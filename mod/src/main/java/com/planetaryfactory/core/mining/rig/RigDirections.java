package com.planetaryfactory.core.mining.rig;

import net.minecraft.core.Direction;

/**
 * The only place {@link net.minecraft.core.Direction} meets {@link RigFacing} (#192). Everything
 * that reasons about the footprint's shape does it in {@link RigGeometry}, which knows nothing of
 * Minecraft; this class is the two-line conversion at the boundary.
 */
public final class RigDirections {

    private RigDirections() {
    }

    public static RigFacing toRigFacing(Direction direction) {
        return switch (direction) {
            case NORTH -> RigFacing.NORTH;
            case EAST -> RigFacing.EAST;
            case SOUTH -> RigFacing.SOUTH;
            case WEST -> RigFacing.WEST;
            default -> throw new IllegalArgumentException(direction + " is not horizontal");
        };
    }

    public static Direction toDirection(RigFacing facing) {
        return switch (facing) {
            case NORTH -> Direction.NORTH;
            case EAST -> Direction.EAST;
            case SOUTH -> Direction.SOUTH;
            case WEST -> Direction.WEST;
        };
    }
}
