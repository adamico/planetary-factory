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
 * The rig's geometry: a look direction plus a size, turned into a set of positions (#192).
 *
 * <p>Everything here is arithmetic -- no world, no block, no player -- which is exactly what the
 * ticket's Checks section asks the unit test to cover: "the footprint lands where the preview
 * would draw it, refuses where it does not fit, and the anchor/part forwarding resolves from any
 * part."
 *
 * <p>The vertical extent is asserted here too. It is the pack's own figure rather than the corpus's
 * -- Factorio states two ground sizes and no third -- so nothing upstream can catch a rig that
 * silently went back to being one block tall, which is the thing that read as a platform rather
 * than a machine.
 */
class RigGeometryTest {

    @Test
    void aTwoByTwoFacingNorthExtendsAwayFromThePlayerAndToTheirRight() {
        // Facing north, "away from the player" is further north (dz -1) and "right" is east
        // (dx +1) -- the same convention `Direction.getClockWise()` uses. One layer only here,
        // so the ground plane can be asserted on its own.
        List<Offset> footprint = RigGeometry.footprint(2, 2, 1, RigFacing.NORTH);

        assertEquals(new Offset(0, 0, 0), footprint.get(0), "the anchor is always first");
        assertEquals(
                Set.of(new Offset(0, 0, 0), new Offset(0, 0, -1),
                        new Offset(1, 0, 0), new Offset(1, 0, -1)),
                Set.copyOf(footprint));
    }

    @Test
    void aThreeByThreeFacingEastExtendsEastAndToTheSouth() {
        // Facing east: away from the player is +x, and east's right hand is south (+z).
        List<Offset> footprint = RigGeometry.footprint(3, 3, 1, RigFacing.EAST);

        assertEquals(9, footprint.size());
        for (int depth = 0; depth < 3; depth++) {
            for (int lateral = 0; lateral < 3; lateral++) {
                assertTrue(footprint.contains(new Offset(depth, 0, lateral)),
                        "missing (" + depth + "," + lateral + ")");
            }
        }
    }

    @Test
    void everyDirectionProducesADistinctSquareWithNoDuplicatePosition() {
        for (RigFacing facing : RigFacing.values()) {
            List<Offset> footprint = RigGeometry.footprint(2, 2, 2, facing);
            assertEquals(8, footprint.size(), facing + " must place all eight blocks");
            assertEquals(8, Set.copyOf(footprint).size(), facing + " must not overlap itself");
        }
    }

    @Test
    void theBurnerRigIsACubeTwoTallAndTheElectricOneThree() {
        // The vertical extent is chosen rather than extracted (RigTier#blocksTall), so this is the
        // only place it is asserted at all. A rig one block tall reads as a platform.
        List<Offset> burner = RigGeometry.footprint(2, 2, RigTier.BURNER.blocksTall(), RigFacing.NORTH);
        List<Offset> electric = RigGeometry.footprint(3, 3, RigTier.ELECTRIC.blocksTall(), RigFacing.NORTH);

        assertEquals(8, burner.size(), "a 2x2 rig two blocks tall is eight blocks");
        assertEquals(27, electric.size(), "a 3x3 rig three blocks tall is twenty-seven blocks");
        assertEquals(Set.of(0, 1), burner.stream().map(Offset::dy).collect(java.util.stream.Collectors.toSet()));
        assertEquals(Set.of(0, 1, 2), electric.stream().map(Offset::dy).collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void everyLayerRepeatsTheGroundPlaneExactly() {
        // The rig is a prism, not a taper: the layer a break has to clear is the same square at
        // every height, which is what lets teardown walk one offset list.
        List<Offset> footprint = RigGeometry.footprint(3, 3, 3, RigFacing.SOUTH);
        Set<Offset> ground = Set.copyOf(RigGeometry.groundLayer(footprint));

        assertEquals(9, ground.size());
        for (int layer = 1; layer < 3; layer++) {
            final int dy = layer;
            Set<Offset> flattened = footprint.stream()
                    .filter(offset -> offset.dy() == dy)
                    .map(offset -> new Offset(offset.dx(), 0, offset.dz()))
                    .collect(java.util.stream.Collectors.toSet());
            assertEquals(ground, flattened, "layer " + dy + " differs from the ground plane");
        }
    }

    @Test
    void theAnchorSitsAtTheBaseSoNoBlockIsBelowIt() {
        // The mining area is the layer directly beneath the rig (#193/#194). If the anchor were
        // in the middle of the column, "beneath" would mean two different things.
        for (RigFacing facing : RigFacing.values()) {
            for (Offset offset : RigGeometry.footprint(2, 2, 2, facing)) {
                assertTrue(offset.dy() >= 0, facing + " placed a block below the anchor: " + offset);
            }
        }
    }

    @Test
    void aZeroOrNegativeSizeIsRejectedRatherThanSilentlySkipped() {
        assertThrows(IllegalArgumentException.class, () -> RigGeometry.footprint(0, 2, 2, RigFacing.NORTH));
        assertThrows(IllegalArgumentException.class, () -> RigGeometry.footprint(2, -1, 2, RigFacing.SOUTH));
        assertThrows(IllegalArgumentException.class, () -> RigGeometry.footprint(2, 2, 0, RigFacing.EAST));
    }

    @Test
    void fitsIsTrueOnlyWhenEveryOffsetIsClear() {
        List<Offset> footprint = RigGeometry.footprint(2, 2, 2, RigFacing.SOUTH);

        assertTrue(RigGeometry.fits(footprint, offset -> true), "an all-clear site fits");

        Offset blocked = footprint.get(2);
        assertFalse(RigGeometry.fits(footprint, offset -> !offset.equals(blocked)),
                "one occupied tile refuses the whole placement");
    }

    @Test
    void aCeilingOneBlockAboveRefusesATwoTallRig() {
        // The failure the vertical extent introduces: a site whose ground plane is clear and whose
        // upper layer is not. Nothing above the anchor is visible from where the player clicks.
        List<Offset> footprint = RigGeometry.footprint(2, 2, 2, RigFacing.WEST);

        assertTrue(RigGeometry.fits(RigGeometry.groundLayer(footprint), offset -> offset.dy() == 0),
                "the ground plane alone is clear");
        assertFalse(RigGeometry.fits(footprint, offset -> offset.dy() == 0),
                "a ceiling one block up must refuse the whole rig");
    }

    @Test
    void theAnchorResolvesFromAnyPartsOwnOffset() {
        // A part stores the offset it was placed at, relative to the anchor. Negating it and
        // adding to the part's own absolute position recovers the anchor -- the arithmetic the
        // block glue runs when breaking any part has to find the whole structure.
        for (RigFacing facing : RigFacing.values()) {
            List<Offset> footprint = RigGeometry.footprint(3, 3, 3, facing);
            Offset anchorAbsolute = new Offset(10, 64, -4); // an arbitrary world position
            for (Offset partOffset : footprint) {
                Offset partAbsolute = new Offset(
                        anchorAbsolute.dx() + partOffset.dx(),
                        anchorAbsolute.dy() + partOffset.dy(),
                        anchorAbsolute.dz() + partOffset.dz());
                Offset recovered = new Offset(
                        partAbsolute.dx() + partOffset.negate().dx(),
                        partAbsolute.dy() + partOffset.negate().dy(),
                        partAbsolute.dz() + partOffset.negate().dz());
                assertEquals(anchorAbsolute, recovered);
            }
        }
    }
}
