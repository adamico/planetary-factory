package com.planetaryfactory.core.fluid;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * The Boiler's one item face (#224), returned for every {@code Direction} and for the null side.
 *
 * <p>Fuel in and nothing out, which is {@link BoilerSlots}' whole rule. Direction never changes
 * what happens here; the item does -- the same arrangement the Furnace and the rig keep, and the
 * reason a Create funnel works on whichever face a player puts it on.
 */
public record BoilerItemHandler(BoilerBlockEntity boiler) implements IItemHandler {

    @Override
    public int getSlots() {
        return BoilerSlots.SIZE;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return boiler.getItem(slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || slot != BoilerSlots.insertionSlot(boiler.isFuel(stack))) {
            return stack;
        }
        ItemStack existing = boiler.getItem(slot);
        int limit = Math.min(getSlotLimit(slot), stack.getMaxStackSize());
        if (!existing.isEmpty()) {
            if (!ItemStack.isSameItemSameComponents(existing, stack)) {
                return stack;
            }
            limit -= existing.getCount();
        }
        if (limit <= 0) {
            return stack;
        }

        int accepted = Math.min(limit, stack.getCount());
        if (!simulate) {
            if (existing.isEmpty()) {
                boiler.setItem(slot, stack.copyWithCount(accepted));
            } else {
                existing.grow(accepted);
                boiler.setChanged();
            }
        }
        return stack.getCount() == accepted
                ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - accepted);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (!BoilerSlots.canExtract(slot)) {
            // Which is every slot: a Boiler holds only the fuel it is burning, and letting a
            // funnel take that back is pulling the coal out from under it mid-tick.
            return ItemStack.EMPTY;
        }
        return boiler.removeItem(slot, amount);
    }

    @Override
    public int getSlotLimit(int slot) {
        return boiler.getMaxStackSize();
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return slot == BoilerSlots.insertionSlot(boiler.isFuel(stack));
    }
}
