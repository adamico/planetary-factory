package com.planetaryfactory.core.smelting;

/**
 * Which slot an item goes to, and which one it may be taken from (#155).
 *
 * <p><b>The furnace has no sided inventory.</b> Every face and the null side return the same
 * handler, and what decides the outcome is the item and the operation rather than the direction.
 * That is Factorio's arrangement -- there the <em>inserter's</em> direction decides in or out and
 * the furnace itself has no faces -- and it is also what makes a Create funnel work wherever a
 * player puts it: funnels reach a neighbour through {@code InvManipulationBehaviour}, whose
 * capability is a {@code BlockCapability<IItemHandler, Direction>}, so a nominated-face inventory
 * answers a funnel on the wrong face with silence and no diagnosis.
 *
 * <p>Pure: no Minecraft types. The item facts arrive already answered.
 */
public final class FurnaceSlots {

    public static final int INPUT = 0;
    public static final int FUEL = 1;
    public static final int OUTPUT = 2;

    /** Three slots on a burner tier; the Electric tier's fuel slot is simply never filled. */
    public static final int SIZE = 3;

    /** No slot will take this stack. */
    public static final int NONE = -1;

    private FurnaceSlots() {
    }

    /**
     * Where an inserted stack lands.
     *
     * <p>Input wins when an item is both, which is the case that matters: a coal block and a log
     * are each a fuel and a smelting ingredient, and routing them to the fuel slot would make a
     * furnace burn the thing it was asked to smelt.
     */
    public static int insertionSlot(boolean isIngredient, boolean isFuel, boolean burnsFuel) {
        if (isIngredient) {
            return INPUT;
        }
        if (isFuel && burnsFuel) {
            return FUEL;
        }
        return NONE;
    }

    /** Extraction reaches the output and nothing else, so nothing can strip fuel or input. */
    public static boolean canExtract(int slot) {
        return slot == OUTPUT;
    }

    /**
     * Whether a result fits in the output slot as it stands. A false here is the stall: the smelt
     * does not start, nothing is voided and nothing is dropped.
     */
    public static boolean fitsOutput(int outputCount, boolean sameItem, int resultCount, int maxStackSize) {
        if (outputCount <= 0) {
            return true;
        }
        return sameItem && outputCount + resultCount <= maxStackSize;
    }
}
