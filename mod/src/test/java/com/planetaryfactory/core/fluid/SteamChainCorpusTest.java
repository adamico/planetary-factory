package com.planetaryfactory.core.fluid;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * That the mod reads the resource the generator writes (#223, ADR-0048).
 *
 * <p>The seam this covers is narrow, the same way {@code PumpCorpusTest} is: this asserts today's
 * corpus parses into today's known values, hardcoded here the way {@code PumpCorpusTest} hardcodes
 * 20. {@code tests/pack/test_steam_assets.py} is what holds the *generator* against the corpus field
 * by field -- if this class read the wrong key off the resource, that check would stay green and
 * the number the mod actually sees would be wrong anyway.
 */
class SteamChainCorpusTest {

    @Test
    @DisplayName("the resource parses, and the Boiler's target is Factorio's 165 °C")
    void boilerTargetTemperature() {
        assertEquals(165, SteamChainCorpus.get().boilerTargetTemperature());
    }

    @Test
    @DisplayName("the Boiler's energy consumption survives the copy")
    void boilerEnergyConsumption() {
        assertEquals(1_800_000.0, SteamChainCorpus.get().boilerEnergyConsumption());
    }

    @Test
    @DisplayName("the Steam Engine's ceiling is the same 165 °C -- one boiler tier (ADR-0048)")
    void steamEngineMaximumTemperature() {
        assertEquals(165, SteamChainCorpus.get().steamEngineMaximumTemperature());
    }

    @Test
    @DisplayName("the Steam Engine's fluid draw survives the copy")
    void steamEngineFluidUsagePerTick() {
        assertEquals(0.5, SteamChainCorpus.get().steamEngineFluidUsagePerTick());
    }

    @Test
    @DisplayName("the raw rows are still there for #224 and #225 to read from")
    void rawRowsCarryTheirOwnName() {
        assertEquals("boiler", SteamChainCorpus.get().boilerRow().get("name").getAsString());
        assertEquals(
                "steam-engine", SteamChainCorpus.get().steamEngineRow().get("name").getAsString());
    }
}
