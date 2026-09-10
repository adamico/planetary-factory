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
    @DisplayName("both fluid boxes' volumes are read by role, not by name")
    void fluidBoxVolumes() {
        assertEquals(200, SteamChainCorpus.get().boilerFluidBoxVolume(BoilerSpec.INPUT));
        assertEquals(200, SteamChainCorpus.get().boilerFluidBoxVolume(BoilerSpec.OUTPUT));
    }

    @Test
    @DisplayName("the two fluids' rows carry the constants the Boiler's rate is derived from")
    void fluidRows() {
        // Steam's is the one that governs (BoilerSpec's trap one). Water's is asserted beside it
        // so that a resource which silently lost the distinction fails here rather than reaching
        // the player as a Boiler running at a tenth of its rate.
        assertEquals(200.0, SteamChainCorpus.get().fluidHeatCapacity("steam"));
        assertEquals(2_000.0, SteamChainCorpus.get().fluidHeatCapacity("water"));
        assertEquals(15, SteamChainCorpus.get().fluidDefaultTemperature("water"));
    }

    @Test
    @DisplayName("the burner's effectivity is Factorio's 1, and is read rather than assumed")
    void boilerEffectivity() {
        // ADR-0047 banks `fuel_value * effectivity`. The multiplier is 1 here, so a Boiler that
        // ignored it would look identical -- which is exactly why it is read and asserted.
        assertEquals(1.0, SteamChainCorpus.get().boilerEffectivity());
    }

    @Test
    @DisplayName("the whole chain resolves to Factorio's own 60 mB a second")
    void theRateTheCorpusImplies() {
        SteamChainCorpus corpus = SteamChainCorpus.get();
        long perUnit = BoilerSpec.joulesPerMilliBucket(
                corpus.boilerTargetTemperature(),
                corpus.fluidDefaultTemperature("water"),
                corpus.fluidHeatCapacity("steam"));
        assertEquals(60,
                BoilerSpec.milliBucketsPerSecond(corpus.boilerEnergyConsumption(), perUnit));
    }

    @Test
    @DisplayName("the raw rows are still there for #224 and #225 to read from")
    void rawRowsCarryTheirOwnName() {
        assertEquals("boiler", SteamChainCorpus.get().boilerRow().get("name").getAsString());
        assertEquals(
                "steam-engine", SteamChainCorpus.get().steamEngineRow().get("name").getAsString());
    }
}
