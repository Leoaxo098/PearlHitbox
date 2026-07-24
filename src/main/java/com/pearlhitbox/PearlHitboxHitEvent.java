package com.pearlhitbox;

import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired whenever a trigger entity (by default, a wind charge) registers a hit
 * against an ender pearl's virtual/expanded hitbox. Cancel this to stop the
 * velocity transfer / projectile consumption for that specific hit.
 *
 * Other plugins/datapack bridges can listen to this to add custom effects,
 * scoring, cooldowns, etc.
 */
public class PearlHitboxHitEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final EnderPearl pearl;
    private final Projectile trigger;
    private boolean cancelled;

    public PearlHitboxHitEvent(EnderPearl pearl, Projectile trigger) {
        this.pearl = pearl;
        this.trigger = trigger;
    }

    public EnderPearl getPearl() {
        return pearl;
    }

    public Projectile getTrigger() {
        return trigger;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
