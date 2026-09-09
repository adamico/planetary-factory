package com.planetaryfactory.core.felling;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The fill: one gesture at the base of a tree, and what it takes with it (ADR-0051).
 *
 * <p>A Factorio tree is a single entity, so felling is not a convenience layered over Minecraft's
 * log-by-log break -- it is the gesture, and this is its shape. Free of Minecraft on purpose: the
 * claims worth checking here are all about a graph of positions, and {@code TreeShapeTest} checks
 * them with no game.
 *
 * <p>Four rules do all the work, and each exists against a specific way a naive flood fill goes
 * wrong in a real world:
 *
 * <ul>
 *   <li><b>Base only.</b> A log with a log beneath it is mid-trunk and breaks normally. This is
 *       Factorio's gesture anyway, and it is also what stops a canopy that touches a neighbour's
 *       from being felled from inside the wrong tree.
 *   <li><b>Never below the base.</b> Descending is how a fill on a slope reaches the roots of the
 *       tree next door, and there is nothing under a base worth reaching.
 *   <li><b>Leaves lead away from the trunk.</b> Vanilla's {@code distance} counts steps to the
 *       nearest log, so stepping only to a leaf that is <em>further</em> out than the one we came
 *       from walks outward and never inward -- and a neighbouring canopy's leaves are closer to
 *       their own trunk, so the fill turns back at the seam rather than crossing it.
 *   <li><b>A grown leaf must be present.</b> Vanilla has no flag for "this tree grew here", but a
 *       placed build has no naturally-grown leaf on it, and every tree does.
 * </ul>
 */
public final class TreeShape {

    /**
     * The 26 offsets to the blocks touching one block, built once.
     *
     * <p>Trees branch diagonally, so a six-way fill drops limbs. The list is constant and the fill
     * walks it for every block it visits, which is the one place in here where allocating would be
     * felt: a survey runs on the break-speed tick.
     */
    private static final int[][] AROUND = around();

    private TreeShape() {
    }

    private static int[][] around() {
        int[][] offsets = new int[26][];
        int next = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx != 0 || dy != 0 || dz != 0) {
                        offsets[next++] = new int[] {dx, dy, dz};
                    }
                }
            }
        }
        return offsets;
    }

    /**
     * Survey the tree standing on {@code base}, or return {@link FellTree#NONE}.
     *
     * <p>The naturalness probe runs before the budgeted fill and is deliberately not budgeted
     * itself: it walks the trunk column, which is a handful of blocks, and asks whether anything
     * beside it grew. Folding it into the fill would make a tight {@code maxBlocks} decide that a
     * twenty-log tree is a building -- the bound is a work cap, and it must not change the answer
     * to a question about what kind of thing this is.
     */
    public static FellTree survey(TreeSurvey world, FellPos base, FellBounds bounds) {
        if (!world.isLog(base) || world.isLog(base.below())) {
            return FellTree.NONE;
        }
        if (!grew(world, base)) {
            return FellTree.NONE;
        }

        Budget budget = new Budget(bounds.maxBlocks());
        Set<FellPos> logs = fillLogs(world, base, bounds, budget);
        Set<FellPos> leaves = fillLeaves(world, logs, base, bounds, budget);
        return new FellTree(logs, leaves, budget.spent());
    }

    /**
     * Did anything here grow?
     *
     * <p>Walks straight up the trunk from the base and asks whether any block touching it is a
     * naturally-grown leaf. A log cabin has none, a decorative pillar of logs and placed leaves has
     * none, and a tree of any species has several -- including the nether stems, which have no
     * leaves at all and therefore fall out of the rule without being named by it.
     *
     * <p>Takes no bounds, and that is the point: the walk stops at the first block that is not a log,
     * so it terminates on its own, and letting {@code maxHeight} truncate it would let a work cap
     * answer a question about what kind of thing is standing here. A tall tree whose crown sits above
     * the cap is still a tree; it just gets felled in two gestures.
     */
    private static boolean grew(TreeSurvey world, FellPos base) {
        for (FellPos trunk = base; world.isLog(trunk); trunk = trunk.above()) {
            for (int[] step : AROUND) {
                FellPos neighbour = trunk.offset(step[0], step[1], step[2]);
                if (world.isLeaf(neighbour) && world.isNaturalLeaf(neighbour)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Every log connected to the base, upward and outward, within the bounds. */
    private static Set<FellPos> fillLogs(
            TreeSurvey world, FellPos base, FellBounds bounds, Budget budget) {
        Set<FellPos> logs = new LinkedHashSet<>();
        Deque<FellPos> queue = new ArrayDeque<>();
        if (!budget.take()) {
            return logs;
        }
        logs.add(base);
        queue.add(base);

        while (!queue.isEmpty()) {
            FellPos log = queue.poll();
            for (int[] step : AROUND) {
                FellPos neighbour = log.offset(step[0], step[1], step[2]);
                if (logs.contains(neighbour) || !world.isLog(neighbour)) {
                    continue;
                }
                if (!inReach(neighbour, base, bounds) || !budget.take()) {
                    continue;
                }
                logs.add(neighbour);
                queue.add(neighbour);
            }
        }
        return logs;
    }

    /**
     * The canopy those logs carry.
     *
     * <p>Seeded from the leaves touching a felled log, then walked outward while {@code distance}
     * keeps increasing. A leaf whose distance is not greater than the one we came from belongs to
     * some trunk that is nearer than ours -- possibly ours, possibly the next tree's -- and either
     * way it is not further out along this branch, so the fill stops.
     */
    private static Set<FellPos> fillLeaves(
            TreeSurvey world, Set<FellPos> logs, FellPos base, FellBounds bounds, Budget budget) {
        Set<FellPos> leaves = new LinkedHashSet<>();
        Deque<FellPos> queue = new ArrayDeque<>();

        for (FellPos log : logs) {
            for (int[] step : AROUND) {
                FellPos neighbour = log.offset(step[0], step[1], step[2]);
                if (accept(world, neighbour, base, bounds, budget, leaves, 0)) {
                    queue.add(neighbour);
                }
            }
        }
        while (!queue.isEmpty()) {
            FellPos leaf = queue.poll();
            int distance = world.leafDistance(leaf);
            for (int[] step : AROUND) {
                FellPos neighbour = leaf.offset(step[0], step[1], step[2]);
                if (accept(world, neighbour, base, bounds, budget, leaves, distance)) {
                    queue.add(neighbour);
                }
            }
        }
        return leaves;
    }

    private static boolean accept(
            TreeSurvey world,
            FellPos leaf,
            FellPos base,
            FellBounds bounds,
            Budget budget,
            Set<FellPos> leaves,
            int from) {
        if (leaves.contains(leaf) || !world.isLeaf(leaf)) {
            return false;
        }
        if (world.leafDistance(leaf) <= from) {
            return false;
        }
        if (!inReach(leaf, base, bounds) || !budget.take()) {
            return false;
        }
        leaves.add(leaf);
        return true;
    }

    /** Inside the shape caps: not below the base, not too far up, not too far out. */
    private static boolean inReach(FellPos pos, FellPos base, FellBounds bounds) {
        int up = pos.y() - base.y();
        return up >= 0
                && up < bounds.maxHeight()
                && Math.abs(pos.x() - base.x()) <= bounds.maxRadius()
                && Math.abs(pos.z() - base.z()) <= bounds.maxRadius();
    }

    /** The work cap, and whether it was ever the reason something was left standing. */
    private static final class Budget {

        private int remaining;
        private boolean spent;

        Budget(int remaining) {
            this.remaining = remaining;
        }

        boolean take() {
            if (remaining <= 0) {
                spent = true;
                return false;
            }
            remaining--;
            return true;
        }

        boolean spent() {
            return spent;
        }
    }
}
