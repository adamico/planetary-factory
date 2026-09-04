package com.planetaryfactory.core.crafting;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * A slot that stands where one of the 2x2 inventory grid's slots used to be (#140).
 *
 * <p>It takes nothing, gives nothing, and is not there: {@link #isActive()} is what
 * {@code AbstractContainerScreen} asks before it draws a slot, hovers it or hit-tests a click, so an
 * inactive slot is invisible and unclickable without the screen knowing anything about this pack.
 * The two permission methods are the server's half of the same answer -- a client that sends the
 * click anyway is refused by {@code AbstractContainerMenu.doClick}, which reads {@code mayPlace} and
 * {@code mayPickup} and never reads {@code isActive}.
 *
 * <p>It keeps the container, container index and screen position of the slot it replaces so that
 * nothing downstream shifts: {@code InventoryMenu.quickMoveStack} indexes armour at 5 and the
 * backpack at 9 by constant, and {@code canTakeItemForPickAll} identifies the result slot by its
 * container.
 */
public final class RemovedGridSlot extends Slot {

    public RemovedGridSlot(Container container, int slot, int x, int y) {
        super(container, slot, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }

    @Override
    public boolean mayPickup(Player player) {
        return false;
    }

    @Override
    public boolean isActive() {
        return false;
    }
}
