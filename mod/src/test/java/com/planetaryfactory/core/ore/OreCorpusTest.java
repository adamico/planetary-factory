package com.planetaryfactory.core.ore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * That the generated slice the mod loads still carries what ADR-0041 reads off it.
 *
 * <p>The file is written by `scripts/factorio-resource-extract.py` from Factorio's dump, and the
 * static check in `tests/factorio/` asserts the corpus it is cut from. What this adds is the other
 * end: the mod can actually parse it, and the numbers survive the trip. A slice that failed to load
 * would take the blockstate property's size with it, which is a crash at registration -- but one
 * that has drifted a total is silent, and it is what a patch paying out the wrong amount looks
 * like from here.
 */
class OreCorpusTest {

    @Test
    void terrasAlphabetIsFive() {
        assertEquals(
                java.util.Set.of("iron", "copper", "coal", "uranium", "stone"),
                OreCorpus.get().resources().keySet());
    }

    @Test
    void thePatchTotalsAreFactoriosOwn() {
        assertEquals(400_000, OreCorpus.get().resource("iron").startingAmount());
        assertEquals(320_000, OreCorpus.get().resource("copper").startingAmount());
        assertEquals(320_000, OreCorpus.get().resource("coal").startingAmount());
        assertEquals(160_000, OreCorpus.get().resource("stone").startingAmount(),
                "stone is the fifth resource and the smallest field (ADR-0041)");
    }

    @Test
    void uraniumHasNoStartingPatch() {
        assertEquals(0, OreCorpus.get().resource("uranium").startingAmount(),
                "Factorio deals uranium no starting patch, which is why Terra's opening has none");
    }

    @Test
    void everyOreRendersThroughFactoriosEightStages() {
        assertEquals(8, OreCorpus.get().stageCount());
        for (OreCorpus.Resource resource : OreCorpus.get().resources().values()) {
            assertEquals(1.0, resource.stageRatios().get(0), 1e-9,
                    resource.name() + "'s first stage is a full block");
            assertTrue(resource.stageRatios().get(7) < 0.01,
                    resource.name() + "'s last stage is nearly gone");
        }
    }

    @Test
    void theDistanceLawIsFlatWithinSixteenHundredBlocks() {
        OreCorpus.DistanceLaw law = OreCorpus.get().distanceLaw();

        assertEquals(1600, law.flatWithin());
        assertEquals(1.0, law.richnessAt(0), 1e-9);
        assertEquals(1.0, law.richnessAt(1599), 1e-9, "leaving early buys nothing");
        assertTrue(law.richnessAt(5200) > 2.0, "and far out is richer, linearly");
    }
}
