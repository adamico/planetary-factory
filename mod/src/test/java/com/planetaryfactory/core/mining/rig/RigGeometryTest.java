package com.planetaryfactory.core.mining.rig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.planetaryfactory.core.mining.rig.RigGeometry.Offset;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * The footprint's geometry: a look direction plus a size, turned into a set of positions (#192).
 *
 * <p>Everything here is arithmetic -- no world, no block, no player -- which is exactly what the
 * ticket's Checks section asks the unit test to cover: "the footprint lands where the preview
 * would draw it, refuses where it does not fit, and the anchor/part forwarding resolves from any
 * part."
 */
class RigGeometryTest {

    @Test
    void aTwoByTwoFacingNorthExtendsAwayFromThePlayerAndToTheirRight() {
        // Facing north, "away from the player" is further north (dz -1) and "right" is east
        // (dx +1) -- the same convention `Direction.getClockWise()` uses.
        List<Offset> footprint = RigGeometry.footprint(2, 2, RigFacing.NORTH);

        assertEquals(new Offset(0, 0), footprint.get(0), "the anchor is always first");
        assertEquals(
                Set.of(new Offset(0, 0), new Offset(0, -1), new Offset(1, 0), new Offset(1, -1)),
                Set.copyOf(footprint));
    }

    @Test
    void aThreeByThreeFacingEastExtendsEastAndToTheSouth() {
        // Facing east: away from the player is +x, and east's right hand is south (+z).
        List<Offset> footprint = RigGeometry.footprint(3, 3, RigFacing.EAST);

        assertEquals(9, footprint.size());
        for (int depth = 0; depth < 3; depth++) {
            for (int lateral = 0; lateral < 3; lateral++) {
                assertTrue(footprint.contains(new Offset(depth, lateral)),
                        "missing (" + depth + "," + lateral + ")");
            }
        }
    }

    @Test
    void everyDirectionProducesADistinctSquareWithNoDuplicatePosition() {
        for (RigFacing facing : RigFacing.values()) {
            List<Offset> footprint = RigGeometry.footprint(2, 2, facing);
            assertEquals(4, footprint.size(), facing + " must place all four blocks");
            assertEquals(4, Set.copyOf(footprint).size(), facing + " must not overlap itself");
        }
    }

    @Test
    void aZeroOrNegativeSizeIsRejectedRatherThanSilentlySkipped() {
        assertThrows(IllegalArgumentException.class, () -> RigGeometry.footprint(0, 2, RigFacing.NORTH));
        assertThrows(IllegalArgumentException.class, () -> RigGeometry.footprint(2, -1, RigFacing.SOUTH));
    }

    @Test
    void fitsIsTrueOnlyWhenEveryOffsetIsClear() {
        List<Offset> footprint = RigGeometry.footprint(2, 2, RigFacing.SOUTH);

        assertTrue(RigGeometry.fits(footprint, offset -> true), "an all-clear site fits");

        Offset blocked = footprint.get(2);
        assertFalse(RigGeometry.fits(footprint, offset -> !offset.equals(blocked)),
                "one occupied tile refuses the whole placement");
    }

    @Test
    void theAnchorResolvesFromAnyPartsOwnOffset() {
        // A part stores the offset it was placed at, relative to the anchor. Negating it and
        // adding to the part's own absolute position recovers the anchor -- the arithmetic the
        // block glue runs when breaking any part has to find the whole structure.
        for (RigFacing facing : RigFacing.values()) {
            List<Offset> footprint = RigGeometry.footprint(3, 3, facing);
            Offset anchorAbsolute = new Offset(10, -4); // an arbitrary world position
            for (Offset partOffset : footprint) {
                Offset partAbsolute = new Offset(
                        anchorAbsolute.dx() + partOffset.dx(),
                        anchorAbsolute.dz() + partOffset.dz());
                Offset recovered = new Offset(
                        partAbsolute.dx() + partOffset.negate().dx(),
                        partAbsolute.dz() + partOffset.negate().dz());
                assertEquals(anchorAbsolute, recovered);
            }
        }
    }
}
