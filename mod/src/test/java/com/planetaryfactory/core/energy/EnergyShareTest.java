package com.planetaryfactory.core.energy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What a pole does when its area asks for more than the grid is giving it. The argument for
 * water-filling over first-come-first-served is on {@link EnergyShare} and in ADR-0036.
 */
class EnergyShareTest {

    @Test
    void everyoneIsSatisfiedWhenThereIsEnough() {
        assertArrayEquals(new long[] {10, 20, 30}, EnergyShare.waterFill(100, new long[] {10, 20, 30}));
    }

    @Test
    void anEqualShortfallIsSplitEqually() {
        assertArrayEquals(new long[] {30, 30, 30}, EnergyShare.waterFill(90, new long[] {100, 100, 100}));
    }

    @Test
    void aModestDemandIsMetInFullAndTheRestGoesToTheOthers() {
        // 100 EU, three machines wanting 10 / 100 / 100. An equal cut would be 33 each, but the
        // first only wants 10, so its spare 23 is redistributed rather than wasted.
        long[] grants = EnergyShare.waterFill(100, new long[] {10, 100, 100});
        assertEquals(10, grants[0]);
        assertEquals(100, grants[0] + grants[1] + grants[2]);
        assertTrue(Math.abs(grants[1] - grants[2]) <= 1, "the two large demands split the remainder");
    }

    @Test
    void nothingIsGrantedBeyondWhatIsAvailable() {
        long[] grants = EnergyShare.waterFill(7, new long[] {100, 100, 100});
        assertEquals(7, grants[0] + grants[1] + grants[2]);
    }

    @Test
    void noMachineIsGrantedMoreThanItAsked() {
        long[] demands = {5, 0, 17, 3};
        long[] grants = EnergyShare.waterFill(1000, demands);
        for (int i = 0; i < demands.length; i++) {
            assertTrue(grants[i] <= demands[i]);
        }
    }

    @Test
    void aZeroDemandIsGrantedNothing() {
        assertArrayEquals(new long[] {0, 50}, EnergyShare.waterFill(50, new long[] {0, 100}));
    }

    @Test
    void noReceiversGrantsNothing() {
        assertArrayEquals(new long[0], EnergyShare.waterFill(100, new long[0]));
    }

    @Test
    void nothingAvailableGrantsNothing() {
        assertArrayEquals(new long[] {0, 0}, EnergyShare.waterFill(0, new long[] {10, 10}));
    }

    @Test
    void theIndivisibleRemainderIsStillHandedOut() {
        // 10 EU across three equal demands: 3 each leaves 1 that must not evaporate.
        long[] grants = EnergyShare.waterFill(10, new long[] {100, 100, 100});
        assertEquals(10, grants[0] + grants[1] + grants[2]);
    }
}
