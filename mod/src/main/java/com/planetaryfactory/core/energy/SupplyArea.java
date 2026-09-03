package com.planetaryfactory.core.energy;

/**
 * The shape of a pole's supply area, as pure integer geometry.
 *
 * <p>A square column: a Factorio supply square horizontally, and a shallow band vertically, which
 * is the dimension Factorio does not have. Deliberately free of every Minecraft type -- the mod's
 * test source set has no Minecraft on its classpath, and this is the half of the pole that can be
 * checked without standing a server up.
 */
public final class SupplyArea {

    /** Receives an offset from the pole, in blocks. */
    @FunctionalInterface
    public interface OffsetSink {
        void accept(int dx, int dy, int dz);
    }

    private SupplyArea() {
    }

    /** Whether a block at this offset from the pole is inside its supply area. */
    public static boolean covers(PoleTier tier, int dx, int dy, int dz) {
        return dx >= tier.minOffset() && dx <= tier.maxOffset()
                && dz >= tier.minOffset() && dz <= tier.maxOffset()
                && Math.abs(dy) <= tier.verticalRadius();
    }

    /**
     * Every offset in the area, each exactly once, the pole's own position included.
     *
     * <p>The pole's own block is in the enumeration rather than skipped: the caller is scanning for
     * an energy capability, and the pole does not expose one to itself, so excluding it here would
     * be a special case that buys nothing.
     */
    public static void forEachOffset(PoleTier tier, OffsetSink sink) {
        int v = tier.verticalRadius();
        for (int dx = tier.minOffset(); dx <= tier.maxOffset(); dx++) {
            for (int dz = tier.minOffset(); dz <= tier.maxOffset(); dz++) {
                for (int dy = -v; dy <= v; dy++) {
                    sink.accept(dx, dy, dz);
                }
            }
        }
    }

    /** How many blocks a tier's scan visits. Useful for sizing collections and for the tests. */
    public static int volume(PoleTier tier) {
        int side = tier.supplySize();
        return side * side * (2 * tier.verticalRadius() + 1);
    }
}
