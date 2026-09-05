package com.planetaryfactory.core.ore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The draw: an amount decrements, and the block breaks when it reaches zero.
 *
 * <p>ADR-0041's whole claim is that hands and machines take from the same number, so the seam
 * they share is the one worth checking -- and it is checkable without a game because
 * {@link OreDelta} names a position by a packed long and holds nothing else of Minecraft's.
 *
 * <p>What a failure here reaches the player as: an ore block that pays out forever, or one that
 * vanishes on the first hit while claiming to hold a thousand. Neither logs anything.
 */
class OreAmountTest {

    private static final long POS = 42L;

    @Test
    void aDrawPaysOneUnitAndLeavesTheBlockStanding() {
        OreDelta delta = new OreDelta();

        OreDelta.Draw draw = delta.draw(POS, 3);

        assertEquals(1, draw.paid());
        assertEquals(2, draw.remaining());
        assertFalse(draw.exhausted(), "a block with units left stands");
    }

    @Test
    void theLastDrawPaysOutAndExhaustsTheBlock() {
        OreDelta delta = new OreDelta();
        delta.draw(POS, 3);
        delta.draw(POS, 3);

        OreDelta.Draw last = delta.draw(POS, 3);

        assertEquals(1, last.paid(), "the last unit is paid, not swallowed by the break");
        assertEquals(0, last.remaining());
        assertTrue(last.exhausted(), "at zero the block breaks");
    }

    @Test
    void aBlockPaysOutExactlyWhatItHolds() {
        OreDelta delta = new OreDelta();
        int paid = 0;

        for (int cycle = 0; cycle < 500; cycle++) {
            paid += delta.draw(POS, 500).paid();
        }

        assertEquals(500, paid,
                "no unit is swallowed by the break and none is dealt twice");
    }

    @Test
    void aDrawAgainstAnAmountOfZeroPaysNothing() {
        OreDelta delta = new OreDelta();

        OreDelta.Draw draw = delta.draw(POS, 0);

        assertEquals(0, draw.paid(), "a field that placed no blocks deals no ore");
        assertTrue(draw.exhausted());
    }

    @Test
    void handAndDrillDrawFromTheSameNumber() {
        OreDelta delta = new OreDelta();

        delta.draw(POS, 10);        // a hand break cycle
        delta.draw(POS, 10);        // a drill operation
        OreDelta.Draw third = delta.draw(POS, 10);

        assertEquals(7, third.remaining(),
                "there is one count, and both callers decrement it");
    }

    @Test
    void anUntouchedBlockCostsNothingToStore() {
        OreDelta delta = new OreDelta();

        assertEquals(9, delta.remaining(POS, 9));
        assertTrue(delta.isEmpty(), "reading a full block must not write an entry");
    }

    @Test
    void aRetiredPositionDoesNotHandItsDeltaToTheNextBlock() {
        OreDelta delta = new OreDelta();
        delta.draw(POS, 5);
        delta.draw(POS, 5);

        delta.retire(POS);

        assertEquals(5, delta.remaining(POS, 5),
                "a block destroyed by anything else drops its delta");
        assertTrue(delta.isEmpty());
    }

    @Test
    void exhaustionRetiresTheEntryRatherThanLeavingItBehind() {
        OreDelta delta = new OreDelta();

        delta.draw(POS, 1);

        assertTrue(delta.isEmpty(),
                "the block is gone, so the entry that tracked it is too");
    }
}
