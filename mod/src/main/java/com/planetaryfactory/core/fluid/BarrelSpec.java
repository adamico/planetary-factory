package com.planetaryfactory.core.fluid;

/**
 * The barrel's numbers and the arithmetic over them, with no Minecraft on the classpath (ADR-0037).
 *
 * <p>Both constants are Factorio's, and neither is a tuning knob. `scripts/factorio-recipe-convert.py`
 * fixes one Factorio fluid unit to one millibucket, and every emitted recipe is built on that rule --
 * so a barrel holding 50 mB is a barrel holding Factorio's 50 units, and it stays a known fraction of
 * the recipe that consumes it. A bucket-parity 1 000 mB would be a twentyfold dose of every fluid in
 * the corpus riding in one item.
 *
 * <p>The barrel is deliberately bad at storage. 50 mB against {@code create:fluid_tank}'s 8 000 mB per
 * block is 1:160, where Factorio's own barrel-to-storage-tank ratio is 1:500 -- a different number
 * with the same ordering, which is the part that matters: no quantity of barrels is a cheaper tank.
 * Even a full vanilla stack of 64 would come to 3 200 mB, still under one tank block, so the stack
 * size is free to be Factorio's ten rather than a defence against anything.
 */
public final class BarrelSpec {
    /** Factorio's 50 units, at the converter's 1 unit = 1 mB. */
    public static final int CAPACITY_MB = 50;

    /** Factorio's stack of ten. 500 mB per inventory slot. */
    public static final int STACK_SIZE = 10;

    private BarrelSpec() {
    }

    /**
     * How much of {@code offeredMb} a barrel already holding {@code heldMb} can take.
     *
     * <p>Partial fills are accepted rather than refused. Create's Spout offers what its own tank has
     * and expects to be told how much was taken; refusing anything short of a full 50 mB would stall
     * a Spout fed by a pipe that delivers in smaller pulses.
     */
    public static int fillable(int heldMb, int offeredMb) {
        if (offeredMb <= 0) {
            return 0;
        }
        return Math.min(offeredMb, CAPACITY_MB - clampHeld(heldMb));
    }

    /** How much a barrel holding {@code heldMb} can give up against a request of {@code requestedMb}. */
    public static int drainable(int heldMb, int requestedMb) {
        if (requestedMb <= 0) {
            return 0;
        }
        return Math.min(requestedMb, clampHeld(heldMb));
    }

    /** What a full inventory slot of barrels holds: the number the logistics claim rests on. */
    public static int perSlotMb() {
        return CAPACITY_MB * STACK_SIZE;
    }

    private static int clampHeld(int heldMb) {
        return Math.max(0, Math.min(CAPACITY_MB, heldMb));
    }
}
