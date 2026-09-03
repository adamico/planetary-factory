package com.planetaryfactory.core.energy;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The supply area is a Factorio supply square in the horizontal plane and a band in the vertical,
 * which is Factorio's 2D square given the one dimension Factorio does not have.
 *
 * <p>Pure integer geometry on purpose: it is the half of the pole that can be checked without a
 * server, which is what {@code docs/testing/what-to-check.md} asks a pack-logic check to be.
 */
class SupplyAreaTest {

    @Test
    void theCentreIsCovered() {
        for (PoleTier tier : PoleTier.values()) {
            assertTrue(SupplyArea.covers(tier, 0, 0, 0), tier + " must power what it stands on");
        }
    }

    @Test
    void theCornerOfTheSquareIsCovered() {
        assertTrue(SupplyArea.covers(PoleTier.SMALL,
                        PoleTier.SMALL.maxOffset(), 0, PoleTier.SMALL.maxOffset()),
                "the area is a square, not a disc -- its corners are inside it");
    }

    @Test
    void oneBlockBeyondEitherEdgeIsNotCovered() {
        for (PoleTier tier : PoleTier.values()) {
            assertFalse(SupplyArea.covers(tier, tier.maxOffset() + 1, 0, 0));
            assertFalse(SupplyArea.covers(tier, tier.minOffset() - 1, 0, 0));
            assertFalse(SupplyArea.covers(tier, 0, 0, tier.maxOffset() + 1));
            assertFalse(SupplyArea.covers(tier, 0, 0, tier.minOffset() - 1));
        }
    }

    @Test
    void theVerticalBandIsShallowerThanTheHorizontalReach() {
        assertFalse(SupplyArea.covers(PoleTier.SUBSTATION, 0, PoleTier.VERTICAL_RADIUS + 1, 0));
        assertTrue(SupplyArea.covers(PoleTier.SUBSTATION, 0, PoleTier.VERTICAL_RADIUS, 0));
        assertFalse(SupplyArea.covers(PoleTier.SUBSTATION, 0, -(PoleTier.VERTICAL_RADIUS + 1), 0));
    }

    @Test
    void offsetsEnumerateExactlyTheCoveredVolume() {
        for (PoleTier tier : PoleTier.values()) {
            List<int[]> seen = new ArrayList<>();
            SupplyArea.forEachOffset(tier, (dx, dy, dz) -> seen.add(new int[] {dx, dy, dz}));

            assertEquals(SupplyArea.volume(tier), seen.size(), tier + " enumerated the wrong volume");
            for (int[] o : seen) {
                assertTrue(SupplyArea.covers(tier, o[0], o[1], o[2]),
                        tier + " enumerated an offset it does not cover");
            }
        }
    }

    @Test
    void theVolumeIsTheSupplySquareTimesTheBand() {
        assertEquals(5 * 5 * 5, SupplyArea.volume(PoleTier.SMALL));
        assertEquals(7 * 7 * 5, SupplyArea.volume(PoleTier.MEDIUM));
        assertEquals(18 * 18 * 5, SupplyArea.volume(PoleTier.SUBSTATION));
    }

    @Test
    void theEnumerationHasNoDuplicates() {
        List<Long> seen = new ArrayList<>();
        SupplyArea.forEachOffset(PoleTier.MEDIUM,
                (dx, dy, dz) -> seen.add((long) dx * 1_000_000 + dy * 1_000 + dz));
        assertEquals(seen.size(), seen.stream().distinct().count());
    }

    @Test
    void theSubstationCoversEverythingASmallPoleDoes() {
        SupplyArea.forEachOffset(PoleTier.SMALL, (dx, dy, dz) ->
                assertTrue(SupplyArea.covers(PoleTier.SUBSTATION, dx, dy, dz)));
    }

    @Test
    void theLadderOnlyGrows() {
        // With the big pole gone (see PoleTier) every tier covers everything the one below does.
        SupplyArea.forEachOffset(PoleTier.SMALL, (dx, dy, dz) ->
                assertTrue(SupplyArea.covers(PoleTier.MEDIUM, dx, dy, dz)));
        assertTrue(SupplyArea.covers(PoleTier.MEDIUM, 3, 0, 0));
        assertFalse(SupplyArea.covers(PoleTier.SMALL, 3, 0, 0));
    }
}
