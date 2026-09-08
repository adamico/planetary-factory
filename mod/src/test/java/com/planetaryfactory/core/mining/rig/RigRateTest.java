package com.planetaryfactory.core.mining.rig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * A rig's two rates: how long one operation takes, and what it costs per tick (#193).
 *
 * <p>ADR-0043 asks for an <em>explicit</em> operations-per-second rather than a constant welded
 * into the mining loop, and the reason it has to be explicit is here: the figure is the drill's
 * {@code mining_speed} over the <em>resource's</em> {@code mining_time}, so one rig has two rates
 * the moment it stands over uranium instead of iron. A speed baked into the block cannot express
 * that.
 *
 * <p>The per-tick cost is ADR-0047's, unchanged: joules over ticks, with no EU anywhere and no
 * MJ-to-ticks constant. Both numbers come off the corpus, so nothing here asserts a value the
 * pack chose -- it asserts the arithmetic that turns two corpus figures into a tick count.
 */
class RigRateTest {

    @Test
    void theBurnerRigTakesFourSecondsOverTerrasOreAtAQuarterOfAnOperationPerSecond() {
        // 0.25 operations/second against `mining_time` 1 is one operation every four seconds,
        // which is 80 ticks. This is the number ADR-0040 justified the rig's place in the
        // starting pocket with -- "beats hands even at 0.25 items/s".
        assertEquals(80, RigRate.operationTicks(0.25, 1));
    }

    @Test
    void uraniumCostsTheSameRigTwiceAsLongBecauseTheResourceSaysSo() {
        // The whole reason the rate cannot live on the drill: `mining_time` 2 against the same
        // rig. Factorio charges the resource's hardness, not the machine's.
        assertEquals(160, RigRate.operationTicks(0.25, 2));
    }

    @Test
    void theElectricRigIsTwiceTheBurnersOverTheSameOre() {
        assertEquals(40, RigRate.operationTicks(0.5, 1));
    }

    @Test
    void anOperationNeverRoundsAwayToNothing() {
        // A drill fast enough to finish inside a tick still takes one. A zero here would be an
        // operation per tick with no fuel drawn for it, which is a free-ore bug rather than a
        // fast rig.
        assertEquals(1, RigRate.operationTicks(1000, 1));
    }

    @Test
    void aRigWithNoSpeedOrTheOreWithNoTimeIsARefusalRatherThanADivideByZero() {
        // Both arrive from the corpus, so a regeneration that emits a null and a reader that
        // defaults it to zero is the realistic path in. Better a named throw at class-init than
        // an infinity turned into a tick count.
        assertThrows(IllegalArgumentException.class, () -> RigRate.operationTicks(0, 1));
        assertThrows(IllegalArgumentException.class, () -> RigRate.operationTicks(0.25, 0));
        assertThrows(IllegalArgumentException.class, () -> RigRate.operationTicks(-1, 1));
    }

    @Test
    void theBurnerRigSpendsItsOwnWattageAndNotTheFurnaces() {
        // 150 kW over 20 ticks. The furnace's 90 kW gives 4,500 -- the rig must not inherit that
        // constant, which is exactly what calling `PFFuel.joulesPerTick()` would have done.
        assertEquals(7_500L, RigRate.joulesPerTick(150_000.0));
        assertEquals(4_500L, RigRate.joulesPerTick(90_000.0));
    }

    @Test
    void aRigThatDrawsNothingCostsNothingPerTick() {
        assertEquals(0L, RigRate.joulesPerTick(0.0));
    }
}
