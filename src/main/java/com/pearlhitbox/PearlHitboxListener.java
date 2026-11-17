package com.pearlhitbox;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.WindCharge;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Two jobs, both to stop a wind charge from "attacking" the player during the trick:
 *
 * 1. The ORIGINAL wind charge the player threw at the pearl. Vanilla resolves its player
 *    collision on the same tick my polling task detects it, so removing it afterwards is
 *    too late - the hit already landed. Instead we cancel the damage at the moment of
 *    collision IF the charge is currently within the pearl's virtual hitbox radius of an
 *    in-flight pearl owned by the damaged player. That intercepts the hit before it lands.
 *
 * 2. The DEFLECTED charge we spawn (tagged). Belt-and-suspenders so it can never hit the
 *    player either.
 */
public class PearlHitboxListener implements Listener {

    private final PearlHitboxTask task;
    private final HitboxConfig config;

    public PearlHitboxListener(PearlHitboxTask task, HitboxConfig config) {
        this.task = task;
        this.config = config;
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        if (!(damager instanceof WindCharge wc)) {
            return;
        }
        // Only players are protected; other entities can still be hit normally.
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }

        boolean isDeflected = task.isDeflectedCharge(wc.getUniqueId());

        if (isDeflected) {
            // Deflected charges we spawned — cancel damage, remove, and forget the tag
            // so the set doesn't grow forever. These should NOT trigger the virtual hitbox
            // because the player isn't trying to hit their own pearl with them.
            event.setCancelled(true);
            wc.remove();
            task.forgetDeflectedCharge(wc.getUniqueId());
            return;
        }

        // If a wind charge is about to hit the player but it's simultaneously inside the
        // virtual hitbox of the player's own in-flight pearl, cancel the damage but do NOT
        // remove the charge. The scheduled task will detect it on the same tick, trigger
        // the teleport, and remove the charge there — the player teleports safely without
        // taking wind charge damage, and a fresh deflected charge is spawned afterwards.
        if (isWindChargeNearVictimPearl(wc, victim)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(org.bukkit.event.player.PlayerTeleportEvent event) {
        if (event.getCause() == org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.ENDER_PEARL) {
            Player player = event.getPlayer();
            org.bukkit.util.Vector velocity = player.getVelocity().clone();
            Bukkit.getScheduler().runTaskLater(task.getPlugin(), () -> {
                if (player.isOnline()) {
                    player.setVelocity(velocity);
                }
            }, 1L);
        }
    }

    /**
     * True if the wind charge is currently within the configured virtual-hitbox radius of
     * an in-flight ender pearl whose owner is the given player.
     */
    private boolean isWindChargeNearVictimPearl(WindCharge wc, Player victim) {
        double radius = config.getRadius();
        double r2 = radius * radius;
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            for (org.bukkit.entity.EnderPearl pearl : world.getEntitiesByClass(org.bukkit.entity.EnderPearl.class)) {
                if (pearl.isDead() || !pearl.isValid()) {
                    continue;
                }
                java.util.UUID owner = pearl.getOwnerUniqueId();
                if (owner == null || !owner.equals(victim.getUniqueId())) {
                    continue;
                }
                if (wc.getLocation().distanceSquared(pearl.getLocation()) <= r2) {
                    return true;
                }
            }
        }
        return false;
    }
}
