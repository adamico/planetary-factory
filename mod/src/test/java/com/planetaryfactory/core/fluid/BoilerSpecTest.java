package com.planetaryfactory.core.fluid;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The Boiler's arithmetic (#224): joules per tick, joules per unit, and the rate that falls out.
 *
 * <p>Every figure asserted here is one ADR-0048 or ADR-0050 quotes by name, and each is reachable
 * by a wrong route that produces a plausible number -- water's heat capacity for steam's, or
 * Factorio's per-second draw read as per-tick. The two of them are asserted separately from the
 * rate so a failure says which one moved.
 */
class BoilerSpecTest {

    /** Factorio's own, and the numbers every assertion below is built from. */
    private static final double ENERGY_CONSUMPTION = 1_800_000.0;
    private static final int TARGET = 165;
    private static final int WATER_TEMPERATURE = 15;
    private static final double STEAM_HEAT_CAPACITY = 200.0;
    private static final double WATER_HEAT_CAPACITY = 2_000.0;

    @Test
    @DisplayName("1.8 MW is 90,000 J a Minecraft tick")
    void joulesPerTick() {
        assertEquals(90_000L, BoilerSpec.joulesPerTick(ENERGY_CONSUMPTION));
    }

    @Test
    @DisplayName("heating one unit from 15 C to 165 C costs 30,000 J")
    void joulesPerMilliBucket() {
        assertEquals(30_000L,
                BoilerSpec.joulesPerMilliBucket(TARGET, WATER_TEMPERATURE, STEAM_HEAT_CAPACITY));
    }

    @Test
    @DisplayName("water's heat capacity would be wrong by a factor of ten")
    void waterHeatCapacityIsTheTrap() {
        // Not a rule -- a demonstration. The wrong constant does not throw, does not fail a schema
        // and produces 6 mB/s, which reads like a boiler that is merely slow. Stated here so the
        // next person to reach for `water.heat_capacity` finds the reason written down.
        long wrong = BoilerSpec.joulesPerMilliBucket(TARGET, WATER_TEMPERATURE, WATER_HEAT_CAPACITY);
        assertEquals(6, BoilerSpec.milliBucketsPerSecond(ENERGY_CONSUMPTION, wrong));
    }

    @Test
    @DisplayName("the Boiler consumes 60 mB of water a second -- a twentieth of one pump")
    void ratePerSecondIsOneTwentiethOfAPump() {
        long perUnit =
                BoilerSpec.joulesPerMilliBucket(TARGET, WATER_TEMPERATURE, STEAM_HEAT_CAPACITY);
        int perSecond = BoilerSpec.milliBucketsPerSecond(ENERGY_CONSUMPTION, perUnit);
        assertEquals(60, perSecond);
        // ADR-0050's "one pump feeds twenty boilers", asserted from both ends rather than quoted.
        assertEquals(20, OffshorePumpSpec.milliBucketsPerSecond(20) / perSecond);
    }

    @Test
    @DisplayName("a Minecraft tick converts 3 mB")
    void ratePerTickIsThree() {
        long perUnit =
                BoilerSpec.joulesPerMilliBucket(TARGET, WATER_TEMPERATURE, STEAM_HEAT_CAPACITY);
        assertEquals(3,
                BoilerSpec.milliBucketsPerTick(BoilerSpec.joulesPerTick(ENERGY_CONSUMPTION), perUnit));
    }

    @Test
    @DisplayName("a target at the water's own temperature is refused, not divided by zero")
    void noRiseIsRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> BoilerSpec.joulesPerMilliBucket(15, 15, STEAM_HEAT_CAPACITY));
    }
}
