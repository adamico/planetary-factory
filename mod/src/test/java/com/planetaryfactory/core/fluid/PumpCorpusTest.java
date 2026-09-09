package com.planetaryfactory.core.fluid;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * That the mod reads the resource the generator writes (#213, ADR-0050).
 *
 * <p>The seam this covers is narrow and nothing else can see it. {@code tests/pack/test_pump_assets.py}
 * asserts the generated resource matches the corpus, and {@code test_resource_extract.py} asserts
 * the corpus states a rate twenty times the boiler's draw -- but both read JSON. If the class that
 * parses it reached for the wrong key, every one of those checks would stay green and the pump
 * would run at no rate at all.
 */
class PumpCorpusTest {

    @Test
    @DisplayName("the resource parses, and carries Factorio's own rate")
    void resourceParses() {
        assertEquals(20, PumpCorpus.get().pumpingSpeed());
    }

    @Test
    @DisplayName("a Minecraft tick may produce 60 mB")
    void perTickIsSixty() {
        assertEquals(60, PumpCorpus.get().milliBucketsPerTick());
    }

    @Test
    @DisplayName("the pump takes no power, because Factorio's energy source is void")
    void takesNoPower() {
        assertFalse(PumpCorpus.get().takesPower(),
                "ADR-0050 makes the pump powerless at rung 0; a corpus row that stopped saying so "
                        + "would have the mod asking for energy the pack never gives it");
    }
}
