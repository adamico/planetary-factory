package com.planetaryfactory.core.mining.rig;

/**
 * One of the four horizontal directions, and nothing else -- a stand-in for
 * {@code net.minecraft.core.Direction} so {@link RigGeometry} stays on the mod's Minecraft-free
 * test classpath (#192).
 *
 * <p>The mapping to Minecraft's own direction is the glue's job, not this enum's: the block class
 * converts a real {@link net.minecraft.core.Direction} to one of these four before calling into
 * {@link RigGeometry}, and back again for the world.
 */
public enum RigFacing {
    /** {@code dz -1}, matching {@code net.minecraft.core.Direction.NORTH}. */
    NORTH(0, -1),
    EAST(1, 0),
    SOUTH(0, 1),
    WEST(-1, 0);

    private final int dx;
    private final int dz;

    RigFacing(int dx, int dz) {
        this.dx = dx;
        this.dz = dz;
    }

    public int dx() {
        return dx;
    }

    public int dz() {
        return dz;
    }

    /**
     * The direction a player's right hand points while facing this way -- {@code NORTH}'s right is
     * {@code EAST}, matching {@code Direction.getClockWise()}. This is the axis the footprint's
     * width runs along; {@code this} is the axis its depth runs along.
     */
    public RigFacing rightOf() {
        return switch (this) {
            case NORTH -> EAST;
            case EAST -> SOUTH;
            case SOUTH -> WEST;
            case WEST -> NORTH;
        };
    }
}
