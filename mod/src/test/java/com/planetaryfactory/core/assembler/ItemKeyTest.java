package com.planetaryfactory.core.assembler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The item identity itself (ADR-0052): an item is its registry id together with its data component
 * patch, encoded as one string.
 *
 * <p>Minecraft-free, because the format half is. What a component's value encodes to is vanilla's
 * business and this never asks; what it asserts is the framing and the ordering, which is where the
 * two ways to get #222 wrong both live -- folding two items onto one key, and splitting one item
 * across two.
 */
class ItemKeyTest {

    private static final String PACK = "researchd:research_pack";
    private static final String COMPONENT = "researchd:research_pack";

    private static String scienceKey(String pack) {
        return ItemKey.of(PACK, List.of(ItemKey.Entry.set(COMPONENT, "\"planetary_factory:" + pack + "\"")));
    }

    @Test
    void anItemWithNoComponentsIsExactlyItsRegistryId() {
        // The whole reason nothing else had to change: every key written before ADR-0052 -- every
        // committed queue attachment, every fixture in these tests -- is still the same string.
        assertEquals("gtceu:iron_plate", ItemKey.of("gtceu:iron_plate", List.of()));
        assertEquals("gtceu:iron_plate", ItemKey.of("gtceu:iron_plate", null));
        assertFalse(ItemKey.hasPatch("gtceu:iron_plate"));
    }

    @Test
    void aComponentBearingItemNamesItsComponentInVanillasOwnSyntax() {
        assertEquals(
                "researchd:research_pack[researchd:research_pack=\"planetary_factory:automation_science_pack\"]",
                scienceKey("automation_science_pack"));
        assertTrue(ItemKey.hasPatch(scienceKey("automation_science_pack")));
    }

    @Test
    void twoSciencePacksAreTwoItems() {
        // #222 itself: under a bare-id identity these are one string, a plan for automation science
        // is satisfiable out of a stack of chemical science, and the graph refused both rather than
        // fold them.
        assertFalse(scienceKey("automation_science_pack").equals(scienceKey("chemical_science_pack")));
        assertEquals(PACK, ItemKey.itemId(scienceKey("automation_science_pack")));
        assertEquals(PACK, ItemKey.itemId(scienceKey("chemical_science_pack")));
    }

    @Test
    void aBarePackIsNotAnyOfThem() {
        // Matching is exact, a deliberate divergence from neoforge:components' subset match: a key
        // with no patch names the pristine item and nothing else.
        assertFalse(scienceKey("automation_science_pack").equals(PACK));
    }

    @Test
    void twoDifferentlyOrderedPatchesEncodeIdentically() {
        // DataComponentPatch guarantees no iteration order across loads. Every science pack has
        // exactly one component today, so skipping the sort would work perfectly until the first
        // two-component item and then produce two keys for one item -- #222 in the other direction.
        List<ItemKey.Entry> one = List.of(
                ItemKey.Entry.set("researchd:research_pack", "\"a\""),
                ItemKey.Entry.set("minecraft:custom_name", "'{\"text\":\"b\"}'"));
        List<ItemKey.Entry> other = List.of(one.get(1), one.get(0));

        assertEquals(ItemKey.of(PACK, one), ItemKey.of(PACK, other));
        assertEquals(
                PACK + "[minecraft:custom_name='{\"text\":\"b\"}',researchd:research_pack=\"a\"]",
                ItemKey.of(PACK, one));
    }

    @Test
    void aRemovedComponentIsNamedTheWayVanillaNamesOne() {
        assertEquals(
                PACK + "[!researchd:research_pack]",
                ItemKey.of(PACK, List.of(ItemKey.Entry.removed(COMPONENT))));
    }

    @Test
    void theItemIdIsReadableBackOutOfAnyKey() {
        assertEquals("gtceu:iron_plate", ItemKey.itemId("gtceu:iron_plate"));
        assertEquals(PACK, ItemKey.itemId(PACK + "[!researchd:research_pack]"));
    }
}
