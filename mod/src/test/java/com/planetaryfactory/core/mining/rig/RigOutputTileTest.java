package com.planetaryfactory.core.mining.rig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.planetaryfactory.core.mining.rig.RigGeometry.Offset;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Which tile a rig hands its ore to (#193).
 *
 * <p>ADR-0043 says "the tile it faces", and on an odd footprint that is unambiguous. On the burner
 * rig's 2x2 it is not: there are <em>two</em> tiles in front, and the pack would have had to pick
 * one. Factorio picks it, in {@code vector_to_place_result} -- a centre-relative offset in tiles,
 * which for the burner drill is {@code [-0.35, -1.3]} and lands in the <b>left</b> column rather
 * than astride the footprint. This class is the conversion from that vector to a block offset, and
 * these tests are the only place the conversion is checked.
 *
 * <p>Free of Minecraft on purpose: the vector is two doubles and a footprint is two ints, so the
 * derivation belongs where the test source set can reach it rather than inside the generator that
 * copies the corpus.
 */
class RigOutputTileTest {

    @Test
    void theBurnerRigOutputsOneTileBeyondItsFootprintInTheLeftColumn() {
        // The anchor is the near-left corner and lateral runs to the player's right, so the left
        // column is lateral 0 -- the anchor's own. Facing north, "one beyond" is dz -2 against a
        // footprint spanning dz -1..0.
        Offset tile = RigOutputTile.of(2, 2, -0.35, -1.3, RigFacing.NORTH);

        assertEquals(new Offset(0, 0, -2), tile);
    }

    @Test
    void theElectricRigOutputsFromTheMiddleOfItsFrontEdge() {
        // A 3x3's vector is [0, -1.85]: dead centre laterally, one tile beyond a footprint
        // spanning dz -2..0. This is #194's number and it is settled here rather than typed there.
        Offset tile = RigOutputTile.of(3, 3, 0.0, -1.85, RigFacing.NORTH);

        assertEquals(new Offset(1, 0, -3), tile);
    }

    @Test
    void theOutputTileIsNeverInsideTheRigsOwnFootprint() {
        // A rig ejecting into itself would insert into its own item handler forever. Both corpus
        // vectors point past the front edge, and this is the assertion that says so rather than
        // trusting the two literals above.
        for (RigFacing facing : RigFacing.values()) {
            assertTrue(
                    outsideFootprint(2, 2, -0.35, -1.3, facing),
                    "the burner rig ejects into itself facing " + facing);
            assertTrue(
                    outsideFootprint(3, 3, 0.0, -1.85, facing),
                    "the electric rig ejects into itself facing " + facing);
        }
    }

    @Test
    void theOutputTileTurnsWithTheRig() {
        // The vector is stated in the prototype's own north-facing frame, so every other facing is
        // that offset rotated. Facing south, the burner rig's output is dz +2 and the lateral
        // column flips to the other side, because "the player's right" did.
        assertEquals(new Offset(0, 0, 2), RigOutputTile.of(2, 2, -0.35, -1.3, RigFacing.SOUTH));
        assertEquals(new Offset(2, 0, 0), RigOutputTile.of(2, 2, -0.35, -1.3, RigFacing.EAST));
        assertEquals(new Offset(-2, 0, 0), RigOutputTile.of(2, 2, -0.35, -1.3, RigFacing.WEST));
    }

    @Test
    void theOutputTileSitsAtTheRigsBaseAndNotOnTopOfIt() {
        // Factorio is flat, so the vector carries no height. A rig stands two or three blocks tall
        // (ADR-0043's declared exception) and the tile it feeds is at ground level -- a furnace
        // placed in front of it, not on its roof.
        for (RigFacing facing : RigFacing.values()) {
            assertEquals(0, RigOutputTile.of(2, 2, -0.35, -1.3, facing).dy());
            assertEquals(0, RigOutputTile.of(3, 3, 0.0, -1.85, facing).dy());
        }
    }

    private static boolean outsideFootprint(
            int width, int height, double vx, double vy, RigFacing facing) {
        List<Offset> ground =
                RigGeometry.groundLayer(RigGeometry.footprint(width, height, 1, facing));
        return !ground.contains(RigOutputTile.of(width, height, vx, vy, facing));
    }
}
