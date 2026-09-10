package com.planetaryfactory.core.fluid;

/**
 * Which slot an item goes to on a Boiler (#224), and which it may be taken from.
 *
 * <p>One slot: fuel in. Water and steam are fluids and reach the block through its fluid face, and
 * there is no output item at all, so this is the smallest of the three burners' slot rules.
 *
 * <p><b>Nothing may be extracted.</b> The one thing in the Boiler is the fuel it is burning, and a
 * funnel that could take it back out would let a player pull the coal out from under a machine
 * mid-tick -- the rule the Furnace and the rig both keep, which here leaves no extractable slot at
 * all.
 *
 * <p><b>No sided inventory</b>, for the reason {@code FurnaceSlots} gives: Factorio decides
 * in-or-out by the inserter's direction rather than by the machine's face, and a nominated-face
 * inventory answers a Create funnel on the wrong face with silence and no diagnosis.
 *
 * <p>Pure: no Minecraft types. The item facts arrive already answered.
 */
public final class BoilerSlots {

    public static final int FUEL = 0;

    public static final int SIZE = 1;

    /** No slot will take this stack. */
    public static final int NONE = -1;

    private BoilerSlots() {
    }

    /** Where an inserted stack lands: the fuel slot, if the fuel table names it, and nowhere else. */
    public static int insertionSlot(boolean isFuel) {
        return isFuel ? FUEL : NONE;
    }

    /** Nothing comes back out of a Boiler as an item. */
    public static boolean canExtract(int slot) {
        return false;
    }
}
