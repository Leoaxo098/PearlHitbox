package com.pearlhitbox;

import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.WindCharge;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Periodically scans all in-flight ender pearls and looks for allowed "trigger"
 * entities (by default: wind charges) within HitboxConfig#getRadius(). When one
 * is found, it's treated as a direct hit on the pearl.
 *
 * This does NOT modify the pearl's real vanilla collision box (Bukkit doesn't
 * expose that) - it implements an equivalent virtual hitbox on top of vanilla
 * physics, which is both larger/smaller than vanilla depending on config and
 * restrictable to specific entity types.
 */
public class PearlHitboxTask {

    private final PearlHitboxPlugin plugin;
    private final HitboxConfig config;

    private BukkitTask bukkitTask;
    private long tickCounter = 0L;
    private final Map<UUID, Long> lastHitTick = new HashMap<>();

    // UUIDs of wind charges we spawned as "deflected" charges. These are exempt from
    // hitting the player (see PearlHitboxListener) so they fly past instead of attacking.
    private final Set<UUID> deflectedCharges = java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());

    public boolean isDeflectedCharge(UUID uuid) {
        return deflectedCharges.contains(uuid);
    }

    public void forgetDeflectedCharge(UUID uuid) {
        deflectedCharges.remove(uuid);
    }

    public PearlHitboxPlugin getPlugin() {
        return plugin;
    }

    public PearlHitboxTask(PearlHitboxPlugin plugin, HitboxConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void start() {
        stop();
        this.bukkitTask = Bukkit.getScheduler().runTaskTimer(
                plugin, this::tick, 0L, config.getCheckIntervalTicks());
    }

    public void stop() {
        if (bukkitTask != null) {
            bukkitTask.cancel();
            bukkitTask = null;
        }
    }

    public void restart() {
        start();
    }

    private void tick() {
        tickCounter++;

        if (!config.isEnabled()) {
            return;
        }

        double radius = config.getRadius();

        for (World world : Bukkit.getWorlds()) {
            for (EnderPearl pearl : world.getEntitiesByClass(EnderPearl.class)) {
                if (pearl.isDead() || !pearl.isValid()) {
                    continue;
                }

                Long lastHit = lastHitTick.get(pearl.getUniqueId());
                if (lastHit != null && (tickCounter - lastHit) < config.getHitCooldownTicks()) {
                    continue;
                }

                Projectile trigger = findTrigger(pearl, radius);
                if (trigger != null) {
                    handleHit(pearl, trigger);
                }
            }
        }

        // Light cleanup so the cooldown map doesn't grow forever.
        if (tickCounter % 200 == 0) {
            lastHitTick.entrySet().removeIf(e -> (tickCounter - e.getValue()) > 400);
        }
    }

    private Projectile findTrigger(EnderPearl pearl, double radius) {
        for (Entity nearby : pearl.getNearbyEntities(radius, radius, radius)) {
            if (!(nearby instanceof Projectile projectile)) {
                continue;
            }
            if (projectile.isDead() || !projectile.isValid()) {
                continue;
            }
            if (config.isAllowedTrigger(projectile.getType())) {
                return projectile;
            }
        }
        return null;
    }

    private void handleHit(EnderPearl pearl, Projectile trigger) {
        PearlHitboxHitEvent event = new PearlHitboxHitEvent(pearl, trigger);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }

        switch (config.getMode()) {
            case TELEPORT -> teleportHit(pearl, trigger);
            case BOOST -> boostHit(pearl, trigger);
        }

        lastHitTick.put(pearl.getUniqueId(), tickCounter);
    }

    /**
     * TELEPORT mode: mirrors vanilla pearl landing — the pearl's owner is warped to
     * the pearl's current location the moment a valid trigger hits it, then the pearl
     * is consumed right there instead of continuing to fly.
     */
    private void teleportHit(EnderPearl pearl, Projectile trigger) {
        if (config.isDebug()) {
            plugin.getLogger().info("[debug] teleportHit: pearl=" + pearl.getUniqueId()
                    + " trigger=" + trigger.getType() + " @ " + pearl.getLocation().toVector());
        }

        if (config.isParticlesEnabled()) {
            pearl.getWorld().spawnParticle(Particle.GUST, pearl.getLocation(), 12, 0.15, 0.15, 0.15, 0.02);
        }
        if (config.isSoundEnabled()) {
            pearl.getWorld().playSound(pearl.getLocation(), Sound.ENTITY_BREEZE_WIND_BURST, 1.0f, 1.0f);
        }

        // Resolve the owner. Prefer the persistent owner UUID (most reliable for
        // player-thrown pearls); fall back to the shooter source.
        Player player = null;
        java.util.UUID ownerId = pearl.getOwnerUniqueId();
        if (ownerId != null) {
            player = Bukkit.getPlayer(ownerId);
        }
        if (player == null && pearl.getShooter() instanceof Player p) {
            player = p;
        }

        if (player == null) {
            if (config.isDebug()) {
                plugin.getLogger().info("[debug] teleportHit: no owner player found, pearl keeps flying");
            }
            return;
        }

        // IMPORTANT timing fix: the player is about to teleport ONTO the pearl, and the
        // wind charge is right there too. If we let the original wind charge live until
        // after the teleport, vanilla resolves its player collision on the next tick and
        // the charge "hits" the now-coincident player. So kill the original trigger BEFORE
        // the teleport. The deflect (below) spawns a fresh, safely-offset charge after the
        // player has moved.
        trigger.remove();

        // Build the destination from the pearl's position, but keep the player's OWN
        // current look angle (vanilla pearl landing preserves your facing - it does not
        // rotate you to match the pearl). Only override direction when keep-player-look
        // is disabled.
        org.bukkit.Location target = pearl.getLocation().clone();
        if (config.getTeleportVerticalOffset() != 0.0) {
            target.add(0, config.getTeleportVerticalOffset(), 0);
        }
        if (config.isTeleportKeepLook()) {
            target.setYaw(player.getLocation().getYaw());
            target.setPitch(player.getLocation().getPitch());
        } else {
            Vector dir = pearl.getVelocity().clone();
            if (dir.lengthSquared() > 0.0001) {
                target.setDirection(dir);
            }
        }

        Vector currentVelocity = player.getVelocity().clone();

        // Use the same teleport cause vanilla ender pearls use so portals, plugins and
        // other mechanics treat this exactly like a real pearl landing.
        player.teleport(target, TeleportCause.ENDER_PEARL);

        if (config.isTeleportPreserveVelocity()) {
            player.setVelocity(currentVelocity);
            final Player targetPlayer = player;
            final Vector preservedVelocity = currentVelocity;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (targetPlayer.isOnline()) {
                    targetPlayer.setVelocity(preservedVelocity);
                }
            }, 1L);
        }

        if (config.isTeleportDealDamage()) {
            player.damage(config.getTeleportDamageAmount());
        }

        if (config.isTeleportRemovePearl()) {
            pearl.remove();
        }

        // If the wind charge is meant to survive, spawn a FRESH one in front of the player
        // (now at the pearl) travelling along the look direction. Spawning fresh + offset
        // forward avoids both the same-tick collision race and spawning on top of the
        // player. (The original was already removed above.)
        if (!config.isConsumeProjectile() && config.isRedirectTriggerToLook()) {
            spawnDeflectedWindCharge(trigger, player);
        }
    }

    /**
     * Spawn a fresh wind charge in front of the player, travelling along their current
     * look direction. The original trigger has already been removed before the player
     * teleported onto the pearl, so this new charge starts safely away from the player
     * and flies cleanly past (vanilla "deflect" feel) instead of hitting them.
     */
    private void spawnDeflectedWindCharge(Projectile trigger, Player player) {
        double speed = trigger.getVelocity().length();
        if (speed < 0.01) {
            speed = 1.5; // sensible default if it had no velocity
        }
        Vector dir = player.getLocation().getDirection().normalize();
        Vector velocity = dir.clone().multiply(speed);
        // Spawn ~2.5 blocks in front of the player's eyes (at eye level, not lowered) so
        // the new charge clears the player's collision box even when looking down/up.
        org.bukkit.Location spawnAt = player.getEyeLocation().add(dir.clone().multiply(2.5));
        WindCharge wc = trigger.getWorld().spawn(spawnAt, WindCharge.class, c -> c.setVelocity(velocity));
        // Tag it so PearlHitboxListener cancels any hit it would deal to the player - this
        // is the guaranteed safety net in case it still ends up close to the player.
        deflectedCharges.add(wc.getUniqueId());
        // Forget the tag shortly after so the set doesn't grow forever.
        Bukkit.getScheduler().runTaskLater(plugin, () -> deflectedCharges.remove(wc.getUniqueId()), 120L);
    }

    /**
     * BOOST mode: does NOT teleport. Redirects/boosts the pearl's velocity in the
     * trigger's travel direction so it keeps flying, and the normal vanilla teleport
     * only happens later once the (now faster/higher) pearl naturally hits a block.
     */
    private void boostHit(EnderPearl pearl, Projectile trigger) {
        Vector triggerVelocity = trigger.getVelocity();
        Vector direction = triggerVelocity.lengthSquared() > 0
                ? triggerVelocity.clone().normalize()
                : pearl.getVelocity().clone().normalize();

        Vector kick = direction.multiply(config.getPushStrength());
        kick.setY(kick.getY() + config.getVerticalBoost());

        pearl.setVelocity(pearl.getVelocity().add(kick));

        if (config.isConsumeProjectile()) {
            trigger.remove();
        }

        if (config.isParticlesEnabled()) {
            pearl.getWorld().spawnParticle(Particle.GUST, pearl.getLocation(), 12, 0.15, 0.15, 0.15, 0.02);
        }
        if (config.isSoundEnabled()) {
            pearl.getWorld().playSound(pearl.getLocation(), Sound.ENTITY_BREEZE_WIND_BURST, 1.0f, 1.0f);
        }
    }
}
