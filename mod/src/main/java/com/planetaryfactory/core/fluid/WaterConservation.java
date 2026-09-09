package com.planetaryfactory.core.fluid;

import com.mojang.logging.LogUtils;
import net.minecraft.world.level.GameRules;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;

/**
 * Water is extracted and transported, never created (ADR-0050). This is the vanilla half of that
 * rule: {@code waterSourceConversion} governs {@code WaterFluid.canConvertToSource}, and
 * {@code FlowingFluid.getNewLiquid} consults it before turning two adjacent sources into a third --
 * the 3x1x1 trench that makes a bucket infinite. Off, that conversion never fires, and no amount of
 * digging multiplies water anywhere.
 *
 * <p>Forced rather than defaulted, because the rule is a {@code GameRules} entry and any player with
 * {@code /gamerule} access can flip it back on. Re-asserting it on every {@link ServerStartingEvent}
 * closes that door on the next load rather than trusting it to stay closed.
 *
 * <p>{@code ServerStartingEvent} rather than a per-level load event: the rule lives on
 * {@link GameRules}, which is one instance shared by every dimension on the server, not one per
 * level. Setting it once, as the server comes up and before any level starts ticking, covers every
 * dimension in one call and needs no per-{@code ServerLevel} bookkeeping.
 */
public final class WaterConservation {
    private static final Logger LOGGER = LogUtils.getLogger();

    private WaterConservation() {
    }

    public static void onServerStarting(ServerStartingEvent event) {
        GameRules rules = event.getServer().getGameRules();
        boolean wasSet = rules.getBoolean(GameRules.RULE_WATER_SOURCE_CONVERSION);
        rules.getRule(GameRules.RULE_WATER_SOURCE_CONVERSION).set(false, event.getServer());
        if (wasSet) {
            // Only worth a line when it actually undid something -- a player's own toggle, or a
            // level.dat carried over from before this rule existed.
            LOGGER.info("waterSourceConversion was true; forced back to false (ADR-0050)");
        }
    }
}
