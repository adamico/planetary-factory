package com.planetaryfactory.core.felling;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The fill ADR-0051's gesture is made of, with no Minecraft under it.
 *
 * <p>A tree here is a map from position to what stands there, which is everything {@link TreeShape}
 * is allowed to know: whether a block is a log, whether it is a leaf, and how far that leaf claims
 * to be from a trunk. That last one is vanilla's own {@code distance} property and it is the only
 * thing that keeps one tree's fill out of a neighbour's canopy, so it is the field this test leans
 * on hardest.
 *
 * <p>What is asserted is the five claims the ticket names: it terminates, it respects its bound, it
 * honours base-only, it does not escape into a touching tree, and the count it charges for is the
 * set it removes. The sixth -- that a placed structure never fells -- is the natural-leaf rule, and
 * is here because a log cabin is a shape rather than a game.
 */
class TreeShapeTest {

    /** Bounds roomy enough that nothing in a test hits them unless the test is about them. */
    private static final FellBounds ROOMY = new FellBounds(512, 16, 64);

    /** A world made of what a test places, and nothing else. Absent means air. */
    private static final class Grove implements TreeSurvey {

        private final Map<FellPos, Standing> blocks = new HashMap<>();

        Grove log(int x, int y, int z) {
            blocks.put(new FellPos(x, y, z), new Standing(true, false, 0, false));
            return this;
        }

        Grove leaf(int x, int y, int z, int distance) {
            blocks.put(new FellPos(x, y, z), new Standing(false, true, distance, true));
            return this;
        }

        Grove placedLeaf(int x, int y, int z, int distance) {
            blocks.put(new FellPos(x, y, z), new Standing(false, true, distance, false));
            return this;
        }

        /** A trunk of {@code height} logs rising from the given base, with a 3x3 cap of leaves. */
        Grove tree(int x, int y, int z, int height) {
            for (int i = 0; i < height; i++) {
                log(x, y + i, z);
            }
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx != 0 || dz != 0) {
                        leaf(x + dx, y + height - 1, z + dz, 1);
                    }
                }
            }
            leaf(x, y + height, z, 1);
            return this;
        }

        private Standing at(FellPos pos) {
            return blocks.getOrDefault(pos, new Standing(false, false, 0, false));
        }

        @Override
        public boolean isLog(FellPos pos) {
            return at(pos).log();
        }

        @Override
        public boolean isLeaf(FellPos pos) {
            return at(pos).leaf();
        }

        @Override
        public int leafDistance(FellPos pos) {
            return at(pos).distance();
        }

        @Override
        public boolean isNaturalLeaf(FellPos pos) {
            return at(pos).natural();
        }

        private record Standing(boolean log, boolean leaf, int distance, boolean natural) {
        }
    }

    @Test
    void aWholeTreeIsOneGesture() {
        FellTree felled = TreeShape.survey(new Grove().tree(0, 0, 0, 6), new FellPos(0, 0, 0), ROOMY);

        assertTrue(felled.fells());
        assertEquals(6, felled.logs().size(), "every log of the trunk");
        assertEquals(9, felled.leaves().size(), "the 3x3 cap and the crown");
        assertFalse(felled.bounded(), "a six-log tree is nowhere near the bound");
    }

    @Test
    void theAmountIsTheLogCount() {
        // ADR-0051's divergence from Factorio's flat wood x4: a taller tree pays more.
        for (int height : new int[] {1, 4, 9, 17}) {
            FellTree felled =
                    TreeShape.survey(new Grove().tree(0, 0, 0, height), new FellPos(0, 0, 0), ROOMY);
            assertEquals(height, felled.amount(), "height " + height);
        }
    }

    @Test
    void midTrunkIsNotTheBase() {
        // The gesture is the tree's own. A log with a log beneath it breaks normally, which is what
        // stops a touching canopy being felled from inside the wrong tree.
        FellTree felled = TreeShape.survey(new Grove().tree(0, 0, 0, 6), new FellPos(0, 3, 0), ROOMY);

        assertFalse(felled.fells());
        assertTrue(felled.logs().isEmpty());
    }

    @Test
    void aBuildNeverFells() {
        // A log cabin: logs, no leaves. Nothing here is a tree, and vanilla has no flag saying so --
        // the absence of a naturally-grown leaf is the whole tell.
        Grove cabin = new Grove();
        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 3; y++) {
                cabin.log(x, y, 0);
            }
        }

        assertFalse(TreeShape.survey(cabin, new FellPos(0, 0, 0), ROOMY).fells());
    }

    @Test
    void placedLeavesAreNotGrownOnes() {
        // A decorative build of logs and placed leaves is still a build. `persistent` is vanilla's
        // own bit for "a player put this here".
        Grove folly = new Grove().log(0, 0, 0).log(0, 1, 0).placedLeaf(0, 2, 0, 1);

        assertFalse(TreeShape.survey(folly, new FellPos(0, 0, 0), ROOMY).fells());
    }

    @Test
    void theFillDoesNotCrossIntoATouchingTree() {
        // Two trunks two apart, canopies overlapping. Vanilla's leaf `distance` is the distance to
        // the nearest trunk, so the neighbour's leaves get closer to *it* as ours get further from
        // *us*; refusing to step to a leaf that is nearer some trunk than the one we came from is
        // what stops the fill walking across.
        Grove pair = new Grove().tree(0, 0, 0, 5);
        for (int i = 0; i < 5; i++) {
            pair.log(3, i, 0);
        }
        pair.leaf(2, 4, 0, 2);   // ours, one step out from our crown
        pair.leaf(3, 4, 0, 1);   // theirs, hard against their trunk

        FellTree felled = TreeShape.survey(pair, new FellPos(0, 0, 0), ROOMY);

        assertTrue(felled.fells());
        assertFalse(felled.logs().contains(new FellPos(3, 0, 0)), "their trunk stands");
        assertFalse(felled.leaves().contains(new FellPos(3, 4, 0)), "their leaves stay");
        assertTrue(felled.leaves().contains(new FellPos(2, 4, 0)), "ours goes");
    }

    @Test
    void theFillNeverDescendsBelowTheBase() {
        // A trunk whose neighbour's roots sit lower. Going down from the base is how a fill reaches
        // a second tree on a slope, and there is nothing above a base worth reaching that way.
        Grove slope = new Grove().tree(0, 0, 0, 5);
        slope.log(1, -1, 0).log(1, -2, 0);

        FellTree felled = TreeShape.survey(slope, new FellPos(0, 0, 0), ROOMY);

        assertFalse(felled.logs().contains(new FellPos(1, -1, 0)));
        assertFalse(felled.logs().contains(new FellPos(1, -2, 0)));
    }

    @Test
    void theBoundIsRespectedAndReported() {
        FellBounds tight = new FellBounds(4, 16, 64);
        FellTree felled =
                TreeShape.survey(new Grove().tree(0, 0, 0, 20), new FellPos(0, 0, 0), tight);

        assertTrue(felled.fells());
        assertTrue(felled.bounded(), "the caller has to know it fell only part of the tree");
        assertEquals(4, felled.logs().size() + felled.leaves().size());
    }

    @Test
    void theRadiusBoundsHowFarSidewaysItReaches() {
        Grove sprawl = new Grove().tree(0, 0, 0, 3);
        for (int x = 1; x <= 10; x++) {
            sprawl.log(x, 2, 0);
        }

        FellTree felled =
                TreeShape.survey(sprawl, new FellPos(0, 0, 0), new FellBounds(512, 4, 64));

        assertTrue(felled.logs().contains(new FellPos(4, 2, 0)));
        assertFalse(felled.logs().contains(new FellPos(5, 2, 0)));
    }

    @Test
    void theHeightBoundsHowFarUpItReaches() {
        FellTree felled =
                TreeShape.survey(new Grove().tree(0, 0, 0, 30), new FellPos(0, 0, 0),
                                 new FellBounds(512, 16, 8));

        assertTrue(felled.logs().contains(new FellPos(0, 7, 0)));
        assertFalse(felled.logs().contains(new FellPos(0, 8, 0)));
    }

    @Test
    void whatIsChargedForIsWhatIsRemoved() {
        // The seam between the two halves of the mechanic. The break-speed modifier charges
        // `amount * rate` before the break lands, and the handler removes the fill afterwards; if
        // those two disagree, a player pays for wood that never arrives.
        FellTree felled =
                TreeShape.survey(new Grove().tree(0, 0, 0, 12), new FellPos(0, 0, 0),
                                 new FellBounds(9, 16, 64));

        assertEquals(felled.amount(), felled.logs().size());
        assertTrue(felled.logs().size() + felled.leaves().size() <= 9);
    }

    @Test
    void aLoopOfLogsTerminates() {
        // Nothing in a world guarantees a tree is a tree. A ring of logs is a cycle, and a fill that
        // revisits is a hang on the server thread rather than a wrong answer.
        Grove ring = new Grove().leaf(0, 1, 0, 1);
        for (int x = 0; x < 4; x++) {
            for (int z = 0; z < 4; z++) {
                if (x == 0 || z == 0 || x == 3 || z == 3) {
                    ring.log(x, 0, z);
                }
            }
        }

        FellTree felled = TreeShape.survey(ring, new FellPos(0, 0, 0), ROOMY);

        assertEquals(12, felled.logs().size(), "each log once, and no more");
    }
}
