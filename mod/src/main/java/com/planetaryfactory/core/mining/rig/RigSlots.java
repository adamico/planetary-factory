package com.planetaryfactory.core.mining.rig;

/**
 * Which slot an item goes to on a rig, and which one it may be taken from (#193).
 *
 * <p>A rig has two: fuel in, ore out. It is the mirror image of the furnace's arrangement and
 * carries no input slot at all, because a rig's input is the ground under it.
 *
 * <p><b>The rig has no sided inventory</b>, for the reason {@link
 * com.planetaryfactory.core.smelting.FurnaceSlots} gives: Factorio decides in-or-out by the
 * inserter's direction rather than by the machine's face, and a nominated-face inventory answers a
 * Create funnel on the wrong face with silence and no diagnosis. So every face and the null side
 * get the same handler.
 *
 * <p>Note what this does <em>not</em> cover: the rig's own eject, which is not an inventory
 * operation on the rig at all. The rig pushes into the faced tile's handler, and this class governs
 * only what a hopper, funnel or player does to the rig.
 *
 * <p>Pure: no Minecraft types. The item facts arrive already answered.
 */
public final class RigSlots {

    public static final int FUEL = 0;
    public static final int OUTPUT = 1;

    /** Two slots. The electric rig's fuel slot is simply never filled (#194). */
    public static final int SIZE = 2;

    /** No slot will take this stack. */
    public static final int NONE = -1;

    private RigSlots() {
    }

    /**
     * Where an inserted stack lands.
     *
     * <p>Only fuel, and only on a rig that burns it. Nothing may be put into the output: the rig
     * fills it and the world empties it, and letting a hopper stuff ore in would let a player
     * launder items through a machine that is supposed to be producing them.
     */
    public static int insertionSlot(boolean isFuel, boolean burnsFuel) {
        return isFuel && burnsFuel ? FUEL : NONE;
    }

    /**
     * Extraction reaches the output alone, so nothing can strip a rig of the coal it is burning.
     *
     * <p>This is also the pull side of ADR-0044's answer to #182: a Create funnel set to extract,
     * sitting on the rig, empties it through this rule -- which is why the rig takes no Create
     * dependency and calls no {@code DirectBeltInputBehaviour}.
     */
    public static boolean canExtract(int slot) {
        return slot == OUTPUT;
    }
}
