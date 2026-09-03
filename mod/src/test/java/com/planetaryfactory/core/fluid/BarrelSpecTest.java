package com.planetaryfactory.core.fluid;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * ADR-0037's numbers, asserted where they are decided.
 *
 * <p>Neither constant is a balance knob: the capacity is Factorio's 50 units under the converter's
 * 1 unit = 1 mB rule, and changing it silently decouples a barrel from the recipes it feeds. This
 * check exists so that change cannot be made quietly.
 */
class BarrelSpecTest {
    /** Create's fluid tank, per block. The comparison ADR-0037's ordering claim rests on. */
    private static final int CREATE_TANK_BLOCK_MB = 8_000;

    @Test
    @DisplayName("capacity is Factorio's 50 units at the converter's 1:1")
    void capacityIsFactoriosFifty() {
        assertEquals(50, BarrelSpec.CAPACITY_MB);
    }

    @Test
    @DisplayName("the stack is Factorio's ten, and a slot holds 500 mB")
    void stackIsFactoriosTen() {
        assertEquals(10, BarrelSpec.STACK_SIZE);
        assertEquals(500, BarrelSpec.perSlotMb());
    }

    @Test
    @DisplayName("no quantity of barrels in a slot beats one tank block")
    void barrelsNeverOutstoreTheTank() {
        assertEquals(500, BarrelSpec.perSlotMb());
        org.junit.jupiter.api.Assertions.assertTrue(
                BarrelSpec.perSlotMb() < CREATE_TANK_BLOCK_MB,
                "a slot of barrels must stay under one Create tank block, or ADR-0017's bulk-storage "
                        + "row is owned by an item rather than by the block it names");
    }

    @Test
    @DisplayName("fills are partial rather than all-or-nothing")
    void fillsArePartial() {
        assertEquals(50, BarrelSpec.fillable(0, 1_000), "an offer larger than the barrel fills it");
        assertEquals(20, BarrelSpec.fillable(30, 1_000), "a part-full barrel takes only the room left");
        assertEquals(5, BarrelSpec.fillable(0, 5), "a small pulse is accepted, not refused");
        assertEquals(0, BarrelSpec.fillable(50, 10), "a full barrel takes nothing");
        assertEquals(0, BarrelSpec.fillable(0, 0), "an empty offer is not an error");
    }

    @Test
    @DisplayName("drains give up no more than is held")
    void drainsAreBounded() {
        assertEquals(50, BarrelSpec.drainable(50, 1_000));
        assertEquals(30, BarrelSpec.drainable(30, 1_000));
        assertEquals(10, BarrelSpec.drainable(50, 10));
        assertEquals(0, BarrelSpec.drainable(0, 1_000));
        assertEquals(0, BarrelSpec.drainable(50, 0));
    }

    @Test
    @DisplayName("a fill then a drain round-trips losslessly")
    void roundTripIsLossless() {
        int accepted = BarrelSpec.fillable(0, BarrelSpec.CAPACITY_MB);
        assertEquals(BarrelSpec.CAPACITY_MB, accepted);
        assertEquals(accepted, BarrelSpec.drainable(accepted, BarrelSpec.CAPACITY_MB));
    }

    @Test
    @DisplayName("a held amount outside the barrel's range cannot invent fluid")
    void clampsNonsenseHeldAmounts() {
        assertEquals(0, BarrelSpec.fillable(9_999, 10), "an over-full barrel still takes nothing");
        assertEquals(50, BarrelSpec.fillable(-10, 1_000), "a negative hold is empty, not extra room");
        assertEquals(0, BarrelSpec.drainable(-10, 1_000));
        assertEquals(50, BarrelSpec.drainable(9_999, 1_000));
    }
}
