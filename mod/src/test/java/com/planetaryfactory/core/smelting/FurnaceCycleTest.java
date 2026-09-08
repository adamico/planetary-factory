package com.planetaryfactory.core.smelting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The smelt clock, and the stall rule with it (#155).
 *
 * <p>A furnace whose output cannot take the result backs up rather than voiding it. Under
 * ADR-0041 the ore is finite, so a furnace that dropped or destroyed a result would destroy a
 * resource the world cannot re-make -- which is also Factorio's answer: a machine with a blocked
 * output stops.
 */
class FurnaceCycleTest {

    @Test
    void aPoweredSmeltCompletesAfterItsDuration() {
        FurnaceCycle cycle = new FurnaceCycle();
        for (int tick = 0; tick < 159; tick++) {
            assertFalse(cycle.tick(true, 160));
        }
        assertTrue(cycle.tick(true, 160));
        assertEquals(0, cycle.progress());
    }

    /** No fuel, no coal burned for nothing: an unpowered tick makes no progress. */
    @Test
    void anUnpoweredTickMakesNoProgress() {
        FurnaceCycle cycle = new FurnaceCycle();
        assertFalse(cycle.tick(false, 160));
        assertEquals(0, cycle.progress());
    }

    /** The smelt holds at where it got to and resumes when the output is drained. */
    @Test
    void aBlockedOutputHoldsProgressAndResumes() {
        FurnaceCycle cycle = new FurnaceCycle();
        for (int tick = 0; tick < 100; tick++) {
            cycle.tick(true, 160);
        }
        assertEquals(100, cycle.progress());

        // A stalled furnace does not tick at all -- the caller stops before reaching the clock,
        // which is what keeps the stall from costing fuel.
        for (int tick = 0; tick < 500; tick++) {
            cycle.idle(true);
        }
        assertEquals(100, cycle.progress(), "a stalled furnace loses nothing and voids nothing");

        for (int tick = 0; tick < 59; tick++) {
            assertFalse(cycle.tick(true, 160));
        }
        assertTrue(cycle.tick(true, 160));
    }

    /**
     * Pulling the input mid-smelt is not a stall. There is nothing left to finish, and carrying
     * the progress onto whatever goes in next would be a free head start on a different smelt.
     */
    @Test
    void losingTheRecipeStartsOver() {
        FurnaceCycle cycle = new FurnaceCycle();
        for (int tick = 0; tick < 40; tick++) {
            cycle.tick(true, 160);
        }
        cycle.idle(true);
        assertEquals(40, cycle.progress(), "a full output holds");
        cycle.idle(false);
        assertEquals(0, cycle.progress(), "an empty input starts over");
    }

    /**
     * The reset has to reach disk. A block entity that discards progress without being marked
     * dirty can unload with the old progress still on the save, which is the free head start the
     * reset exists to prevent -- so idle reports whether it actually threw anything away.
     */
    @Test
    void discardingProgressAsksToBeSaved() {
        FurnaceCycle cycle = new FurnaceCycle();
        for (int tick = 0; tick < 40; tick++) {
            cycle.tick(true, 160);
        }
        assertFalse(cycle.idle(true), "holding progress changes nothing to save");
        assertTrue(cycle.idle(false), "discarding progress has to be persisted");
        assertFalse(cycle.idle(false), "an idle furnace at zero does not re-dirty every tick");
    }
}
