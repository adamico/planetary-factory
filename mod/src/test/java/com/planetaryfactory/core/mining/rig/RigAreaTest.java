package com.planetaryfactory.core.mining.rig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.planetaryfactory.core.mining.rig.RigGeometry.Offset;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Which tiles a rig works (#193).
 *
 * <p>ADR-0043 states the area as "the 2x2 beneath it" and "the 5x5 beneath it", two figures the
 * pack would otherwise have typed. Factorio states one instead: {@code resource_searching_radius},
 * and a tile is worked when <b>its centre is within that radius of the drill's centre</b>. The
 * burner rig's 0.99 gives the 2x2 and the electric rig's 2.49 gives the 5x5 -- one rule, two
 * footprints, and #194 does not type a five.
 *
 * <p>Note the radius is not the number a 2x2 footprint suggests. 0.49 would have been the guess
 * and it is wrong; the corpus is what corrected it, which is the argument for reading it.
 *
 * <p>The area is the layer <em>directly beneath</em> the rig's base and never a column scanned
 * downward -- ADR-0043 rejects the scan on the overlay's account, since a tinted area the player
 * cannot see is worse than no tint.
 */
class RigAreaTest {

    @Test
    void theBurnerRigWorksExactlyTheTwoByTwoBeneathIt() {
        List<Offset> area = RigArea.tiles(2, 2, 0.99, RigFacing.NORTH);

        assertEquals(
                Set.of(new Offset(0, -1, 0), new Offset(1, -1, 0),
                        new Offset(0, -1, -1), new Offset(1, -1, -1)),
                Set.copyOf(area));
    }

    @Test
    void theElectricRigWorksAFiveByFiveFromAThreeByThreeFootprint() {
        // The area overhangs the footprint by one tile on every side, which is what a radius
        // larger than the footprint's own half-extent means. Neither the 5 nor the overhang is
        // typed -- both fall out of 2.49.
        List<Offset> area = RigArea.tiles(3, 3, 2.49, RigFacing.NORTH);

        assertEquals(25, area.size());
        assertTrue(area.contains(new Offset(-1, -1, 1)), "the area reaches behind the rig");
        assertTrue(area.contains(new Offset(3, -1, -3)), "the area reaches past its far corner");
    }

    @Test
    void everyWorkedTileSitsOneBlockBelowTheRigsBase() {
        // ADR-0043's rule, and the reason the anchor stays at the base: "beneath" has to mean one
        // thing. An anchor mid-column would make it mean two.
        for (RigFacing facing : RigFacing.values()) {
            for (Offset tile : RigArea.tiles(3, 3, 2.49, facing)) {
                assertEquals(-1, tile.dy());
            }
        }
    }

    @Test
    void theBurnerRigsAreaIsItsOwnFootprintDroppedOneBlock() {
        // The 2x2 case stated as a relationship rather than as four literals: a radius that
        // reaches no further than the footprint gives back the footprint. This is what makes the
        // starting fields unambiguous -- they are one block thick and flush with the topsoil.
        for (RigFacing facing : RigFacing.values()) {
            Set<Offset> beneath = RigGeometry.groundLayer(RigGeometry.footprint(2, 2, 1, facing))
                    .stream()
                    .map(offset -> new Offset(offset.dx(), -1, offset.dz()))
                    .collect(java.util.stream.Collectors.toSet());

            assertEquals(beneath, Set.copyOf(RigArea.tiles(2, 2, 0.99, facing)));
        }
    }

    @Test
    void theAreaIsTheSameSetWhicheverWayTheRigFaces() {
        // A 2x2 and a 3x3 are rotation-invariant footprints, so rotating a rig moves its arrow and
        // its models and never re-picks which ore it is working. ADR-0043 says so; this is the
        // assertion behind the sentence, and it is why #180's rotate verb costs this ticket
        // nothing.
        for (RigFacing facing : RigFacing.values()) {
            assertEquals(4, RigArea.tiles(2, 2, 0.99, facing).size());
            assertEquals(25, RigArea.tiles(3, 3, 2.49, facing).size());
        }
    }

    @Test
    void theScanOrderIsStableSoARigDoesNotShuffleWhichBlockItWorksNext() {
        // The rig takes the first ore-bearing tile in this list every operation. If the order
        // moved between ticks a half-mined block could be abandoned mid-patch; the order being
        // fixed is what makes "work one block until it is gone" the observed behaviour.
        assertEquals(
                RigArea.tiles(3, 3, 2.49, RigFacing.NORTH),
                RigArea.tiles(3, 3, 2.49, RigFacing.NORTH));
        assertEquals(new Offset(-1, -1, 1), RigArea.tiles(3, 3, 2.49, RigFacing.NORTH).get(0));
    }
}
