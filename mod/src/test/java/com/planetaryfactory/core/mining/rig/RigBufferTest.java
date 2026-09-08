package com.planetaryfactory.core.mining.rig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The rig's small internal output buffer, and the stall it produces (#193).
 *
 * <p>ADR-0043 as amended: a rig pushes onto the tile it faces, and <b>otherwise fills its buffer
 * and stops</b>. There is no third rule -- the ground drop is gone, because no Factorio machine
 * spills when its output is blocked and because under ADR-0041's amount model a spill risks losing
 * a finite resource where a stall preserves it.
 *
 * <p>So the buffer is where backpressure lives, and the load-bearing claim is the negative one:
 * <b>an operation that cannot be banked must never start</b>. A rig that mined into a full buffer
 * would draw a unit out of the ground, fail to store it and have destroyed it, which is the
 * one outcome an amount model cannot survive.
 *
 * <p>One slot, one item id at a time -- the buffer is a stack. A 2x2 sitting across iron and copper
 * therefore stalls on the second resource until the first is taken, which is correct backpressure
 * rather than a special case.
 */
class RigBufferTest {

    @Test
    void anEmptyBufferAcceptsAnything() {
        RigBuffer buffer = new RigBuffer(64);

        assertTrue(buffer.canAccept("minecraft:raw_iron"));
        assertNull(buffer.itemId());
        assertEquals(0, buffer.count());
    }

    @Test
    void aFullBufferStartsNoOperation() {
        // The whole point. `canAccept` is asked BEFORE the draw, so the ore stays in the ground.
        RigBuffer buffer = new RigBuffer(2);
        buffer.add("minecraft:raw_iron");
        buffer.add("minecraft:raw_iron");

        assertFalse(buffer.canAccept("minecraft:raw_iron"));
        assertEquals(2, buffer.count());
    }

    @Test
    void aBufferHoldingIronRefusesCopperRatherThanReplacingIt() {
        // A 2x2 can straddle two fields. Refusing is a stall the player can see and fix by
        // clearing the output; silently swapping the held item would delete what was banked.
        RigBuffer buffer = new RigBuffer(64);
        buffer.add("minecraft:raw_iron");

        assertFalse(buffer.canAccept("minecraft:raw_copper"));
        assertTrue(buffer.canAccept("minecraft:raw_iron"));
        assertThrows(IllegalStateException.class, () -> buffer.add("minecraft:raw_copper"));
    }

    @Test
    void takingTheLastItemEmptiesTheBufferBackToAcceptingAnything() {
        RigBuffer buffer = new RigBuffer(64);
        buffer.add("minecraft:raw_iron");

        assertEquals(1, buffer.remove(8));

        assertNull(buffer.itemId());
        assertEquals(0, buffer.count());
        assertTrue(buffer.canAccept("minecraft:raw_copper"), "an emptied buffer is not still iron");
    }

    @Test
    void aPartialTakeLeavesTheRestAndKeepsTheIdentity() {
        RigBuffer buffer = new RigBuffer(64);
        buffer.add("minecraft:raw_iron");
        buffer.add("minecraft:raw_iron");
        buffer.add("minecraft:raw_iron");

        assertEquals(2, buffer.remove(2));

        assertEquals("minecraft:raw_iron", buffer.itemId());
        assertEquals(1, buffer.count());
    }

    @Test
    void takingFromAnEmptyBufferIsNothingRatherThanAThrow() {
        // The push runs every tick whether or not there is anything to push.
        assertEquals(0, new RigBuffer(64).remove(64));
    }

    @Test
    void aBufferRoundTripsThroughASaveAndReload() {
        // A rig that logs out mid-stall comes back stalled, holding what it held. A buffer that
        // came back empty would have voided ore across a logout with nothing in the log --
        // ADR-0038's objection to the Assembler's codec, in a different package.
        RigBuffer buffer = new RigBuffer(64);
        buffer.load("minecraft:raw_iron", 7);

        assertEquals("minecraft:raw_iron", buffer.itemId());
        assertEquals(7, buffer.count());
        assertFalse(buffer.canAccept("minecraft:raw_copper"));
    }

    @Test
    void aReloadedBufferWithNoItemIsEmptyRatherThanHoldingNothingOfSomething() {
        RigBuffer buffer = new RigBuffer(64);
        buffer.load(null, 5);

        assertNull(buffer.itemId());
        assertEquals(0, buffer.count(), "a count with no id is not a hoard of nothing");
    }

    @Test
    void aBufferCannotBeOverfilledPastItsCapacity() {
        RigBuffer buffer = new RigBuffer(1);
        buffer.add("minecraft:raw_iron");

        assertThrows(IllegalStateException.class, () -> buffer.add("minecraft:raw_iron"));
    }
}
