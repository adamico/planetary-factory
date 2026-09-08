package com.planetaryfactory.core.mining.rig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Where an item may land on a rig, and where it may be taken from (#193).
 *
 * <p>The mirror of {@code FurnaceSlotsTest}, and it matters for the same reason: both
 * {@link RigItemHandler} and the block entity's {@code canPlaceItem} route through this one class,
 * so a wrong answer here is a hopper stripping a rig of the coal it is burning, or a player
 * laundering items into a machine that is supposed to be producing them.
 */
class RigSlotsTest {

    @Test
    void aBurnableGoesToTheFuelSlotOnARigThatBurns() {
        assertEquals(RigSlots.FUEL, RigSlots.insertionSlot(true, true));
    }

    @Test
    void aBurnableGoesNowhereOnARigThatDoesNotBurn() {
        // The electric rig is a supply-area pole customer (ADR-0036), so coal offered to it is
        // refused rather than banked in a slot it will never spend.
        assertEquals(RigSlots.NONE, RigSlots.insertionSlot(true, false));
    }

    @Test
    void anythingThatIsNotFuelIsRefused() {
        // A rig has no input slot: its input is the ground under it. So unlike the furnace there
        // is no "is it an ingredient" branch to lose to, and everything else is simply refused.
        assertEquals(RigSlots.NONE, RigSlots.insertionSlot(false, true));
        assertEquals(RigSlots.NONE, RigSlots.insertionSlot(false, false));
    }

    @Test
    void extractionReachesTheOutputAndNothingElse() {
        assertTrue(RigSlots.canExtract(RigSlots.OUTPUT));
        assertFalse(RigSlots.canExtract(RigSlots.FUEL), "a funnel could strip the rig of its coal");
        assertFalse(RigSlots.canExtract(RigSlots.NONE));
    }

    @Test
    void nothingMayBeInsertedIntoTheOutput() {
        // Asserted as a property of the routing rather than of the menu's OutputSlot, because the
        // item handler is the face a hopper uses and it never sees the menu.
        for (boolean isFuel : new boolean[] {true, false}) {
            for (boolean burnsFuel : new boolean[] {true, false}) {
                assertFalse(RigSlots.insertionSlot(isFuel, burnsFuel) == RigSlots.OUTPUT,
                        "ore can be inserted into the output slot");
            }
        }
    }
}
