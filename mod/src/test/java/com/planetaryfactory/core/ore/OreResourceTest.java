package com.planetaryfactory.core.ore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * That the five blocks and the five sets of amounts are the same five.
 *
 * <p>The failure this catches is an ore block registered with no amounts behind it, which is a
 * block that breaks on the first hit -- and the reverse, an extracted resource nothing places.
 */
class OreResourceTest {

    @Test
    void everyOreBlockHasAmountsBehindIt() {
        for (OreResource resource : OreResource.values()) {
            assertNotNull(resource.corpus(), resource.key() + " has no extracted amounts");
        }
    }

    @Test
    void everyExtractedResourceHasABlock() {
        for (String key : OreCorpus.get().resources().keySet()) {
            assertNotNull(OreResource.of(key), key + " is extracted but nothing places it");
        }
    }

    @Test
    void stoneDropsTheRockTheItemMapAlreadyCallsFactoriosStone() {
        assertEquals("minecraft:cobblestone", OreResource.STONE.drop());
    }

    @Test
    void theBlockNameIsDerivedFromTheCorpusKey() {
        assertEquals("iron_ore", OreResource.IRON.blockName());
        assertEquals("stone_ore", OreResource.STONE.blockName());
    }
}
