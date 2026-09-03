package com.planetaryfactory.core.assembler;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Runs every player's queue, screen open or not.
 *
 * <p>That is the point of a queue rather than a crafting grid: the plan was paid for at Start, so it
 * keeps going while the player walks away, and it is still going when they come back. The panel is a
 * view of it, never the thing driving it.
 *
 * <p>The panel is re-synced on a slow beat rather than every tick. A progress bar does not need
 * twenty updates a second, and the queue's own truth is on the server either way.
 */
public final class AssemblerTicker {

    /** Four times a second, which is smooth enough for a bar and cheap enough for a full server. */
    private static final int SYNC_INTERVAL_TICKS = 5;

    private AssemblerTicker() {
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        AssemblerQueue queue = PersonalAssembler.queueOf(player);
        boolean wasEmpty = queue.isEmpty();
        PersonalAssembler.tick(player);
        if (wasEmpty && queue.isEmpty()) return;
        if (player.tickCount % SYNC_INTERVAL_TICKS == 0 || queue.isEmpty()) {
            PersonalAssembler.sync(player);
        }
    }

    /** A player who logs in gets the queue they left running. */
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PersonalAssembler.sync(player);
        }
    }

    /** A pending plan is an open dialog, and an open dialog does not survive a logout. */
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PersonalAssembler.forget(player);
        }
    }
}
