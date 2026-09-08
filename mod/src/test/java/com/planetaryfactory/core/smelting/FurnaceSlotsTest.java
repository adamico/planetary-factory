package com.planetaryfactory.core.smelting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The furnace has no sided inventory (#155): what decides where an inserted stack lands is the
 * item, not the face it arrived through. This is that routing, with the item facts already
 * answered so the rule itself can be checked without Minecraft.
 */
class FurnaceSlotsTest {

    @Test
    void anIngredientReachesTheInputSlot() {
        assertEquals(FurnaceSlots.INPUT, FurnaceSlots.insertionSlot(true, false, true));
        assertEquals(FurnaceSlots.INPUT, FurnaceSlots.insertionSlot(true, false, false));
    }

    @Test
    void aBurnableReachesTheFuelSlotOnBurnerTiersOnly() {
        assertEquals(FurnaceSlots.FUEL, FurnaceSlots.insertionSlot(false, true, true));
        assertEquals(FurnaceSlots.NONE, FurnaceSlots.insertionSlot(false, true, false));
    }

    /** Charcoal smelts into nothing, but coal blocks and logs are both. Input wins. */
    @Test
    void inputWinsWhenAnItemIsBoth() {
        assertEquals(FurnaceSlots.INPUT, FurnaceSlots.insertionSlot(true, true, true));
    }

    @Test
    void anItemThatIsNeitherIsRejected() {
        assertEquals(FurnaceSlots.NONE, FurnaceSlots.insertionSlot(false, false, true));
    }

    /** So a funnel on any face cannot strip a furnace of its own fuel or its unsmelted input. */
    @Test
    void extractionTakesOnlyFromTheOutputSlot() {
        assertTrue(FurnaceSlots.canExtract(FurnaceSlots.OUTPUT));
        assertFalse(FurnaceSlots.canExtract(FurnaceSlots.INPUT));
        assertFalse(FurnaceSlots.canExtract(FurnaceSlots.FUEL));
    }

    @Test
    void anEmptyOutputTakesAnything() {
        assertTrue(FurnaceSlots.fitsOutput(0, false, 1, 64));
    }

    @Test
    void aDifferentItemInTheOutputBlocks() {
        assertFalse(FurnaceSlots.fitsOutput(1, false, 1, 64));
    }

    @Test
    void theSameItemStacksUpToTheStackLimit() {
        assertTrue(FurnaceSlots.fitsOutput(63, true, 1, 64));
        assertFalse(FurnaceSlots.fitsOutput(64, true, 1, 64));
        assertFalse(FurnaceSlots.fitsOutput(63, true, 2, 64));
    }
}
