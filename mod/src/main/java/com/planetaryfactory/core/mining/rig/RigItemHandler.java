package com.planetaryfactory.core.mining.rig;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * The rig's one item face (#193), returned for every {@code Direction} and for the null side.
 *
 * <p>Fuel goes in on a burner rig and ore comes out; nothing else is accepted and nothing else can
 * be taken. The rules are in {@link RigSlots}, where they are checkable without a world.
 *
 * <p>This is the face a hopper, a chest-adjacent funnel or a player's shift-click uses. It is
 * <em>not</em> how the rig delivers what it mines -- for that the rig pushes into the handler on
 * the tile it faces, which is {@link RigOutputTile}'s business and ADR-0043's mechanic.
 */
public record RigItemHandler(RigBlockEntity rig) implements IItemHandler {

    @Override
    public int getSlots() {
        return RigSlots.SIZE;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return rig.getItem(slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || slot != routeOf(stack)) {
            return stack;
        }
        ItemStack existing = rig.getItem(slot);
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
                rig.setItem(slot, stack.copyWithCount(accepted));
            } else {
                existing.grow(accepted);
                rig.setChanged();
            }
        }
        return stack.getCount() == accepted ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - accepted);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount <= 0 || !RigSlots.canExtract(slot)) {
            return ItemStack.EMPTY;
        }
        ItemStack existing = rig.getItem(slot);
        if (existing.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int taken = Math.min(amount, existing.getCount());
        if (simulate) {
            return existing.copyWithCount(taken);
        }
        return rig.removeItem(slot, taken);
    }

    @Override
    public int getSlotLimit(int slot) {
        return rig.getMaxStackSize();
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return slot == routeOf(stack);
    }

    private int routeOf(ItemStack stack) {
        return RigSlots.insertionSlot(rig.isFuel(stack), rig.burnsFuel());
    }
}
