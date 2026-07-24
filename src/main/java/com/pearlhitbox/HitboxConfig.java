package com.pearlhitbox;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Typed, cached view over config.yml. Call reload() after the plugin's
 * FileConfiguration has been reloaded to pick up new values.
 */
public class HitboxConfig {

    private final PearlHitboxPlugin plugin;

    public enum HitMode { TELEPORT, BOOST }

    private boolean enabled;
    private double radius;
    private long checkIntervalTicks;
    private long hitCooldownTicks;
    private HitMode mode;
    private boolean consumeProjectile;
    private boolean redirectTriggerToLook;
    private boolean particlesEnabled;
    private boolean soundEnabled;

    private boolean teleportKeepLook;
    private boolean teleportPreserveVelocity;
    private boolean teleportDealDamage;
    private double teleportDamageAmount;
    private boolean teleportRemovePearl;
    private double teleportVerticalOffset;

    private boolean debug;

    private double pushStrength;
    private double verticalBoost;

    private boolean onlyWindCharge;
    private boolean includeWindCharge;
    private boolean includeBreezeWindCharge;
    private final Set<EntityType> extraEntityTypes = new HashSet<>();

    public HitboxConfig(PearlHitboxPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        FileConfiguration cfg = plugin.getConfig();

        this.enabled = cfg.getBoolean("hitbox.enabled", true);
        this.radius = cfg.getDouble("hitbox.radius", 0.6);
        this.checkIntervalTicks = Math.max(1, cfg.getLong("hitbox.check-interval-ticks", 1));
        this.hitCooldownTicks = Math.max(0, cfg.getLong("hitbox.hit-cooldown-ticks", 5));
        this.consumeProjectile = cfg.getBoolean("hitbox.consume-projectile", true);
        this.redirectTriggerToLook = cfg.getBoolean("hitbox.redirect-trigger-to-look", true);
        this.particlesEnabled = cfg.getBoolean("hitbox.effects.particles", true);
        this.soundEnabled = cfg.getBoolean("hitbox.effects.sound", true);

        String modeStr = cfg.getString("hitbox.mode", "TELEPORT");
        HitMode parsedMode;
        try {
            parsedMode = HitMode.valueOf(modeStr.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Unknown hitbox.mode '" + modeStr + "', defaulting to TELEPORT");
            parsedMode = HitMode.TELEPORT;
        }
        this.mode = parsedMode;

        this.teleportKeepLook = cfg.getBoolean("hitbox.teleport.keep-player-look", true);
        this.teleportPreserveVelocity = cfg.getBoolean("hitbox.teleport.preserve-velocity", true);
        this.teleportDealDamage = cfg.getBoolean("hitbox.teleport.deal-damage", true);
        this.teleportDamageAmount = cfg.getDouble("hitbox.teleport.damage-amount", 5.0);
        this.teleportRemovePearl = cfg.getBoolean("hitbox.teleport.remove-pearl", true);
        this.teleportVerticalOffset = cfg.getDouble("hitbox.teleport.vertical-offset", 0.0);

        this.debug = cfg.getBoolean("debug", false);

        this.pushStrength = cfg.getDouble("hitbox.boost.push-strength", 1.6);
        this.verticalBoost = cfg.getDouble("hitbox.boost.vertical-boost", 0.35);

        this.onlyWindCharge = cfg.getBoolean("allowed-triggers.only-windcharge", true);
        this.includeWindCharge = cfg.getBoolean("allowed-triggers.include-windcharge", true);
        this.includeBreezeWindCharge = cfg.getBoolean("allowed-triggers.include-breeze-windcharge", true);

        extraEntityTypes.clear();
        List<String> extra = cfg.getStringList("allowed-triggers.extra-entity-types");
        for (String name : extra) {
            try {
                extraEntityTypes.add(EntityType.valueOf(name.trim().toUpperCase()));
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("Unknown EntityType in extra-entity-types: " + name);
            }
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public HitMode getMode() {
        return mode;
    }

    public double getRadius() {
        return radius;
    }

    public long getCheckIntervalTicks() {
        return checkIntervalTicks;
    }

    public long getHitCooldownTicks() {
        return hitCooldownTicks;
    }

    public double getPushStrength() {
        return pushStrength;
    }

    public double getVerticalBoost() {
        return verticalBoost;
    }

    public boolean isTeleportKeepLook() {
        return teleportKeepLook;
    }

    public boolean isTeleportPreserveVelocity() {
        return teleportPreserveVelocity;
    }

    public boolean isTeleportDealDamage() {
        return teleportDealDamage;
    }

    public double getTeleportDamageAmount() {
        return teleportDamageAmount;
    }

    public boolean isTeleportRemovePearl() {
        return teleportRemovePearl;
    }

    public double getTeleportVerticalOffset() {
        return teleportVerticalOffset;
    }

    public boolean isDebug() {
        return debug;
    }

    public boolean isConsumeProjectile() {
        return consumeProjectile;
    }

    public boolean isRedirectTriggerToLook() {
        return redirectTriggerToLook;
    }

    public boolean isParticlesEnabled() {
        return particlesEnabled;
    }

    public boolean isSoundEnabled() {
        return soundEnabled;
    }

    public boolean isOnlyWindCharge() {
        return onlyWindCharge;
    }

    /**
     * Whether the given entity type is allowed to trigger the virtual pearl hitbox.
     */
    public boolean isAllowedTrigger(EntityType type) {
        boolean isWindCharge = type == EntityType.WIND_CHARGE;
        boolean isBreezeWindCharge = type == EntityType.BREEZE_WIND_CHARGE;

        if (onlyWindCharge) {
            if (isWindCharge) return includeWindCharge;
            if (isBreezeWindCharge) return includeBreezeWindCharge;
            return false;
        }

        if (isWindCharge) return includeWindCharge;
        if (isBreezeWindCharge) return includeBreezeWindCharge;
        return extraEntityTypes.contains(type);
    }
}