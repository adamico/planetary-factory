package com.planetaryfactory.core.smelting;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * The furnace's one item face (#155), returned for every {@code Direction} and for the null side.
 *
 * <p>Direction never changes what happens here; the item does. A stack matching a loaded
 * {@code planetaryfactory:smelting} ingredient goes to the input slot, a burnable goes to the fuel
 * slot on a burner tier, input wins if an item is both, and nothing else is accepted. Extraction
 * reaches the output slot alone, so a funnel cannot strip a furnace of its own fuel or of the
 * input it has not smelted yet.
 *
 * <p>That is Factorio's arrangement -- there the inserter's direction decides in or out and the
 * furnace has no faces -- and it is also what makes a Create funnel work on whichever face a
 * player puts it on. The rules themselves are in {@link FurnaceSlots}, where they are checkable
 * without a world.
 */
public record FurnaceItemHandler(FurnaceBlockEntity furnace) implements IItemHandler {

    @Override
    public int getSlots() {
        return FurnaceSlots.SIZE;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return furnace.getItem(slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || slot != routeOf(stack)) {
            return stack;
        }
        ItemStack existing = furnace.getItem(slot);
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
                furnace.setItem(slot, stack.copyWithCount(accepted));
            } else {
                existing.grow(accepted);
                furnace.setChanged();
            }
        }
        return stack.getCount() == accepted ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - accepted);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount <= 0 || !FurnaceSlots.canExtract(slot)) {
            return ItemStack.EMPTY;
        }
        ItemStack existing = furnace.getItem(slot);
        if (existing.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int taken = Math.min(amount, existing.getCount());
        if (simulate) {
            return existing.copyWithCount(taken);
        }
        return furnace.removeItem(slot, taken);
    }

    @Override
    public int getSlotLimit(int slot) {
        return furnace.getMaxStackSize();
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return slot == routeOf(stack);
    }

    /** Where this stack is allowed to land, or {@link FurnaceSlots#NONE}. */
    private int routeOf(ItemStack stack) {
        return FurnaceSlots.insertionSlot(
                furnace.isSmeltingIngredient(stack),
                furnace.isFuel(stack),
                furnace.tier().burnsFuel());
    }
}
