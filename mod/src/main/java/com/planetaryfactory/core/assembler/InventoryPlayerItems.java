package com.planetaryfactory.core.assembler;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The one place the queue's item ids meet Minecraft.
 *
 * <p>Everything above this class counts strings, which is what keeps the queue's rules in a unit
 * test. This class holds no rules of its own -- it is the glue row of the testing policy, checked by
 * a human in-game.
 *
 * <p>Only the main inventory and the hotbar count. Armour and the offhand are not storage the player
 * thinks of as stock, and taking a reservation out of somebody's boots would be a surprise.
 */
public final class InventoryPlayerItems implements PlayerItems {

    private final Inventory inventory;

    public InventoryPlayerItems(Inventory inventory) {
        this.inventory = inventory;
    }

    /**
     * An {@link ItemAmount} as a stack. Here rather than anywhere else because this class is the
     * pack's single crossing point between an item id and an {@code ItemStack}, and a second
     * crossing is a second place for the lookup to disagree.
     */
    public static ItemStack toStack(ItemAmount amount) {
        Item type = itemOf(amount.item());
        return type == null ? ItemStack.EMPTY : new ItemStack(type, amount.count());
    }

    private static Item itemOf(String id) {
        ResourceLocation key = ResourceLocation.tryParse(id);
        return key == null ? null : BuiltInRegistries.ITEM.get(key);
    }

    @Override
    public int count(String item) {
        Item type = itemOf(item);
        if (type == null) return 0;
        int total = 0;
        for (int slot = 0; slot < inventory.items.size(); slot++) {
            ItemStack stack = inventory.items.get(slot);
            if (stack.is(type)) total += stack.getCount();
        }
        return total;
    }

    @Override
    public int take(String item, int count) {
        Item type = itemOf(item);
        if (type == null || count <= 0) return 0;
        int left = count;
        for (int slot = 0; slot < inventory.items.size() && left > 0; slot++) {
            ItemStack stack = inventory.items.get(slot);
            if (!stack.is(type)) continue;
            int taken = Math.min(left, stack.getCount());
            stack.shrink(taken);
            if (stack.isEmpty()) inventory.items.set(slot, ItemStack.EMPTY);
            left -= taken;
        }
        inventory.setChanged();
        return count - left;
    }

    /**
     * All of it or none of it, and the simulation is why: {@code Inventory.add} inserts what fits
     * and reports the remainder, which for a paused craft would leave the queue holding a fraction
     * it has nowhere to record. So the insert happens on a copy of the stacks first.
     */
    @Override
    public boolean give(String item, int count) {
        Item type = itemOf(item);
        if (type == null) return false;
        if (count <= 0) return true;
        if (!fits(type, count)) return false;
        int left = count;
        while (left > 0) {
            ItemStack stack = new ItemStack(type, Math.min(left, type.getDefaultMaxStackSize()));
            left -= stack.getCount();
            if (!inventory.add(stack)) {
                // Cannot happen after fits(), and if it somehow does the item stays with the queue
                // rather than on the ground: pause, never drop.
                return false;
            }
        }
        inventory.setChanged();
        return true;
    }

    /** Room for {@code count}, counted across partial stacks and empty slots alike. */
    private boolean fits(Item type, int count) {
        int max = type.getDefaultMaxStackSize();
        long room = 0;
        for (int slot = 0; slot < inventory.items.size(); slot++) {
            ItemStack stack = inventory.items.get(slot);
            if (stack.isEmpty()) {
                room += max;
            } else if (stack.is(type) && stack.isStackable()) {
                room += Math.max(0, stack.getMaxStackSize() - stack.getCount());
            }
            if (room >= count) return true;
        }
        return false;
    }
}
