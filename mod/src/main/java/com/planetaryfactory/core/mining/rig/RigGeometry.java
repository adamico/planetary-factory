package com.planetaryfactory.core.mining.rig;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * The rig footprint's arithmetic: a look direction plus a size, turned into a set of horizontal
 * positions (#192).
 *
 * <p><b>The square extends away from the player, and upward.</b> The anchor -- the tile the player
 * targets -- is the corner nearest them at the rig's base, and the structure grows along three axes
 * from there: {@code facing} itself (depth, away from the player), {@code facing.rightOf()} (width,
 * to their right) and straight up ({@code blocksTall}). That is one deterministic rule, not a
 * preview to consult, which is what lets breaking any part recompute the whole structure with
 * nothing stored but the anchor's own facing.

 * <p>The vertical extent is the pack's own and not Factorio's -- the corpus states two ground
 * figures and nothing else, because Factorio is played on a plane. See {@link RigTier#blocksTall()}
 * and ADR-0043's declared exception.
 *
 * <p>Free of Minecraft, per the mod's testing policy (ADR-0043 / #192's Checks): this is arithmetic
 * over a direction and two integers, and it is exercised in
 * {@code planetaryfactory_core}'s Minecraft-free test source set. The block glue that turns an
 * {@link Offset} into a real {@code BlockPos} lives beside the block classes, not here.
 */
public final class RigGeometry {

    private RigGeometry() {
    }

    /**
     * Every position the rig occupies, anchor included and always first.
     *
     * @param width the size along {@code facing.rightOf()} -- Factorio's {@code tile_width}
     * @param height the size along {@code facing} -- Factorio's {@code tile_height}
     * @param blocksTall how many blocks the rig stands, the pack's own figure
     */
    public static List<Offset> footprint(int width, int height, int blocksTall, RigFacing facing) {
        if (width <= 0 || height <= 0 || blocksTall <= 0) {
            throw new IllegalArgumentException("a rig must be at least 1x1x1, got "
                    + width + "x" + height + "x" + blocksTall);
        }
        RigFacing right = facing.rightOf();
        List<Offset> offsets = new ArrayList<>(width * height * blocksTall);
        // Anchor first, deliberately: callers that only want "the anchor's own offset" can take
        // index 0 rather than searching for (0, 0, 0).
        offsets.add(new Offset(0, 0, 0));
        for (int layer = 0; layer < blocksTall; layer++) {
            for (int depth = 0; depth < height; depth++) {
                for (int lateral = 0; lateral < width; lateral++) {
                    if (layer == 0 && depth == 0 && lateral == 0) {
                        continue;
                    }
                    offsets.add(new Offset(
                            facing.dx() * depth + right.dx() * lateral,
                            layer,
                            facing.dz() * depth + right.dz() * lateral));
                }
            }
        }
        return List.copyOf(offsets);
    }

    /** The rig's ground layer only -- the tiles it stands on, which is what a mining area is measured from. */
    public static List<Offset> groundLayer(List<Offset> footprint) {
        return footprint.stream().filter(offset -> offset.dy() == 0).toList();
    }

    /**
     * Whether every offset in a footprint is clear, per the caller's own notion of "clear". Pure
     * plumbing over {@code isClear} -- the actual world check (air or replaceable, in bounds) is
     * the block item's, not this class's.
     */
    public static boolean fits(List<Offset> footprint, Predicate<Offset> isClear) {
        return footprint.stream().allMatch(isClear);
    }

    /** One position, relative to the anchor -- which sits at the rig's base, so {@code dy} is never negative. */
    public record Offset(int dx, int dy, int dz) {

        /**
         * The offset back from a part to its anchor. A part stores its own {@code Offset} from the
         * anchor at placement; subtracting it from the part's absolute position -- i.e. adding this
         * negated offset -- recovers the anchor regardless of which part is asked.
         */
        public Offset negate() {
            return new Offset(-dx, -dy, -dz);
        }
    }
}
