package com.planetaryfactory.core.fluid;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The pump's rate, from Factorio's figure to the number a Minecraft tick can spend.
 *
 * <p>The corpus check (#210) already asserts that one pump feeds twenty boilers. What it cannot see
 * is this side of the conversion: the same `pumping_speed` read into two different tick rates.
 * Factorio runs at 60 ticks a second and Minecraft at 20, and `pumping_speed` is stated per
 * *Factorio* tick, so the trip is 20/Factorio-tick to 1,200/s to 60/Minecraft-tick. That the first
 * and last figures are the ones ADR-0050 quotes is the whole point of asserting both.
 */
class OffshorePumpSpecTest {

    @Test
    @DisplayName("Factorio's 20 per tick is 1,200 mB a second")
    void perSecondIsFactoriosRate() {
        assertEquals(1_200, OffshorePumpSpec.milliBucketsPerSecond(20));
    }

    @Test
    @DisplayName("a Minecraft tick may spend 60 mB of it")
    void perTickIsSixty() {
        assertEquals(60, OffshorePumpSpec.milliBucketsPerTick(20));
    }

    @Test
    @DisplayName("the two tick rates are not interchangeable")
    void theTwoTickRatesAreDistinct() {
        // If Factorio's 60/s were mistaken for Minecraft's 20/s -- an easy slip, since
        // `pumping_speed` is itself 20 -- the pump would run at a third of its rate and still
        // look plausible. Stated as an assertion so the slip fails rather than ships.
        assertEquals(3, OffshorePumpSpec.milliBucketsPerSecond(20) / (20 * 20));
    }
}
