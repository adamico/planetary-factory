package com.planetaryfactory.core.smelting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The burner's fuel arithmetic (ADR-0047, #187): joules in, joules out, and nothing in ticks.
 *
 * <p>These are the numbers ADR-0047 is worked in, re-derived here from the tier's own draw rather
 * than typed: 4,500 J a tick on both burners, so one coal is 888 whole ticks of work and the Steel
 * tier gets twice the crafts out of it.
 */
class FuelBufferTest {

    /** Coal's `fuel_value` from the corpus. `test_fuel_extract.py` holds the corpus to it. */
    private static final long COAL = 4_000_000L;

    /** steel-plate: 320 ticks emitted, halved by the Steel tier's speed. */
    private static final int STEEL_PLATE_TICKS = 320;

    @Test
    void oneCoalBuysEightHundredAndEightyEightWholeTicks() {
        FuelBuffer buffer = new FuelBuffer();
        buffer.light(COAL);
        long perTick = FurnaceTier.STONE.joulesPerTick();

        int ticks = 0;
        while (buffer.drawTick(perTick)) {
            ticks++;
        }
        assertEquals(888, ticks);
    }

    /**
     * The remainder is banked, not burnt off. Factorio's buffer keeps what a tick could not use,
     * so the next item lights on top of it -- nine coals buy one more tick than eight do plus 888.
     */
    @Test
    void theRemainderSurvivesTheNextLighting() {
        FuelBuffer buffer = new FuelBuffer();
        long perTick = FurnaceTier.STONE.joulesPerTick();
        int ticks = 0;
        for (int coal = 0; coal < 9; coal++) {
            buffer.light(COAL);
            while (buffer.drawTick(perTick)) {
                ticks++;
            }
        }
        assertEquals(8000, ticks);
        assertEquals(9 * COAL - 8000L * perTick, buffer.storedJoules());
    }

    /**
     * The ratio the ladder claims, as arithmetic rather than as a coincidence of tick accounting:
     * the same 4,500 J against a craft the Steel tier finishes in half the time.
     */
    @Test
    void steelGetsTwiceTheCraftsFromOneCoal() {
        assertEquals(FurnaceTier.STONE.joulesPerTick(), FurnaceTier.STEEL.joulesPerTick());
        long perCraftOnStone =
                FurnaceTier.STONE.joulesPerTick() * FurnaceTier.STONE.durationTicks(STEEL_PLATE_TICKS);
        long perCraftOnSteel =
                FurnaceTier.STEEL.joulesPerTick() * FurnaceTier.STEEL.durationTicks(STEEL_PLATE_TICKS);
        assertEquals(1_440_000L, perCraftOnStone);
        assertEquals(720_000L, perCraftOnSteel);
        assertEquals(2 * (COAL / (double) perCraftOnStone), COAL / (double) perCraftOnSteel);
    }

    /** A part-tick is not a tick: a buffer that cannot cover the whole tick pays nothing. */
    @Test
    void aTickIsPaidInFullOrNotAtAll() {
        FuelBuffer buffer = new FuelBuffer();
        buffer.light(4_000L);
        assertFalse(buffer.drawTick(4_500L));
        assertEquals(4_000L, buffer.storedJoules());
    }

    /** An idle burner keeps its joules -- there is no decay, which is Factorio's own rule. */
    @Test
    void anIdleBufferKeepsWhatItHas() {
        FuelBuffer buffer = new FuelBuffer();
        buffer.light(COAL);
        assertTrue(buffer.isLit());
        assertEquals(COAL, buffer.storedJoules());
    }

    /** The gauge fills to the last item lit, so one log in an empty furnace reads full. */
    @Test
    void theGaugeIsScaledToTheLastItemLit() {
        FuelBuffer buffer = new FuelBuffer();
        buffer.light(2_000_000L);
        assertEquals(2_000_000L, buffer.gaugeCapacity());
        buffer.drawTick(FurnaceTier.STONE.joulesPerTick());
        assertEquals(2_000_000L, buffer.gaugeCapacity());
    }

    /** A buffer holding more than its last item still fits inside its own gauge. */
    @Test
    void theGaugeNeverOverflows() {
        FuelBuffer buffer = new FuelBuffer();
        buffer.light(COAL);
        buffer.light(2_000_000L);
        assertEquals(6_000_000L, buffer.storedJoules());
        assertEquals(6_000_000L, buffer.gaugeCapacity());
    }

    /**
     * Both halves persist. A buffer that came back empty is the silent loss ADR-0038 asks the
     * assembler's codecs to catch; a gauge capacity that did not would redraw a part-spent coal
     * as a full one.
     */
    @Test
    void bothHalvesSurviveALoad() {
        FuelBuffer buffer = new FuelBuffer();
        buffer.load(1_500_000L, COAL);
        assertEquals(1_500_000L, buffer.storedJoules());
        assertEquals(COAL, buffer.gaugeCapacity());
        assertTrue(buffer.isLit());
    }

    @Test
    void anEmptyBufferIsNotLitAndPaysNothing() {
        FuelBuffer buffer = new FuelBuffer();
        assertFalse(buffer.isLit());
        assertFalse(buffer.drawTick(FurnaceTier.STONE.joulesPerTick()));
        assertEquals(0L, buffer.gaugeCapacity());
    }
}
