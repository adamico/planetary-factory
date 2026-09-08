package com.planetaryfactory.core.smelting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The ladder's arithmetic (#155). The recipe carries Factorio's {@code energy_required * 20}
 * unmodified and the block applies its own speed, so these numbers are the whole difference
 * between the three tiers.
 */
class FurnaceTierTest {

    /** steel-plate is 16 s in the corpus, so 320 ticks emitted, observed 320/160/160. */
    @Test
    void steelPlateIsObservedAtTheTiersSpeed() {
        assertEquals(320, FurnaceTier.STONE.durationTicks(320));
        assertEquals(160, FurnaceTier.STEEL.durationTicks(320));
        assertEquals(160, FurnaceTier.ELECTRIC.durationTicks(320));
    }

    /** A recipe faster than the speed divisor still takes a tick; nothing is instant. */
    @Test
    void aDurationNeverRoundsToZero() {
        assertEquals(1, FurnaceTier.STEEL.durationTicks(1));
        assertEquals(1, FurnaceTier.STEEL.durationTicks(0));
    }

    /** 180 kW * 32/420_000, ADR-0029's constant, truncated as its table truncates. */
    @Test
    void electricDrawsThirteenEuPerTick() {
        assertEquals(13L, FurnaceTier.ELECTRIC.euPerTick());
        assertEquals(0L, FurnaceTier.STONE.euPerTick());
        assertEquals(0L, FurnaceTier.STEEL.euPerTick());
    }

    /** One steel craft: 13 EU/t for 160 ticks. This is what the pole water-fills against. */
    @Test
    void electricBuffersOneSteelCraft() {
        assertEquals(2080L, FurnaceTier.ELECTRIC.bufferEu());
        assertEquals(0L, FurnaceTier.STONE.bufferEu());
    }

    @Test
    void onlyTheTwoLowerTiersBurnFuel() {
        assertTrue(FurnaceTier.STONE.burnsFuel());
        assertTrue(FurnaceTier.STEEL.burnsFuel());
        assertFalse(FurnaceTier.ELECTRIC.burnsFuel());
    }

    /**
     * Both burners draw the same 90 kW -- 4,500 J a tick -- from {@code machine.json}, which is
     * what makes the Steel tier's doubled speed yield twice the items from one coal (ADR-0047).
     * A per-tier fuel rule here would be a second scalar to keep in step; there is none.
     * {@link FuelBufferTest} counts the crafts.
     */
    @Test
    void bothBurnersDrawTheSameJoulesPerTick() {
        assertEquals(4_500L, FurnaceTier.STONE.joulesPerTick());
        assertEquals(4_500L, FurnaceTier.STEEL.joulesPerTick());
        assertEquals(0L, FurnaceTier.ELECTRIC.joulesPerTick());
    }

    @Test
    void blockNamesAreDerivedFromTheTier() {
        assertEquals("stone_furnace", FurnaceTier.STONE.blockName());
        assertEquals("electric_furnace", FurnaceTier.ELECTRIC.blockName());
    }
}
