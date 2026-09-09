package com.planetaryfactory.core.assembler;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * The queue's slots: the thirty-six the reservation is taken from and delivered into.
 *
 * <p>It is not itself the crossing between a key and an {@code ItemStack} -- {@link ItemKeys} is,
 * and this class goes through it like the other four callers do. ADR-0038 gave that claim to this
 * class and by #222 there were five crossings, so saying it here again is how it drifted.
 *
 * <p>Everything above this class counts strings, which is what keeps the queue's rules in a unit
 * test. This class holds no rules of its own -- it is the glue row of the testing policy, checked by
 * a human in-game.
 *
 * <p>A key is an id plus a data component patch (ADR-0052), and every one of the four methods below
 * takes the whole key: {@code count} and {@code take} match on it rather than on the item type,
 * which is the fold #222 was, and {@code give} builds the delivered stack from it, which is the same
 * fold on the way out -- a plan that resolved and ran correctly and handed back a blank research
 * pack. {@link ItemKeys} does the resolving; this class does the slots.
 *
 * <p>Only the main inventory and the hotbar count. Armour and the offhand are not storage the player
 * thinks of as stock, and taking a reservation out of somebody's boots would be a surprise.
 */
public final class InventoryPlayerItems implements PlayerItems {

    private final Inventory inventory;

    public InventoryPlayerItems(Inventory inventory) {
        this.inventory = inventory;
    }

    private HolderLookup.Provider registries() {
        return inventory.player.level().registryAccess();
    }

    /** The item a key names, resolved once for a whole pass over the slots. */
    private ItemStack prototypeOf(String item) {
        return ItemKeys.toStack(item, 1, registries());
    }

    @Override
    public int count(String item) {
        ItemStack prototype = prototypeOf(item);
        if (prototype.isEmpty()) return 0;
        int total = 0;
        for (int slot = 0; slot < inventory.items.size(); slot++) {
            ItemStack stack = inventory.items.get(slot);
            if (ItemKeys.matches(prototype, stack)) total += stack.getCount();
        }
        return total;
    }

    @Override
    public int take(String item, int count) {
        ItemStack prototype = prototypeOf(item);
        if (prototype.isEmpty() || count <= 0) return 0;
        int left = count;
        for (int slot = 0; slot < inventory.items.size() && left > 0; slot++) {
            ItemStack stack = inventory.items.get(slot);
            if (!ItemKeys.matches(prototype, stack)) continue;
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
     * it has nowhere to record. So the room is counted first.
     */
    @Override
    public boolean give(String item, int count) {
        ItemStack prototype = prototypeOf(item);
        if (prototype.isEmpty()) return false;
        if (count <= 0) return true;
        if (!fits(prototype, count)) return false;
        int left = count;
        while (left > 0) {
            ItemStack stack = prototype.copyWithCount(Math.min(left, prototype.getMaxStackSize()));
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

    /**
     * Room for {@code count}, counted across partial stacks and empty slots alike.
     *
     * <p>Partial stacks are matched on the whole key, not the item type: two research packs
     * differing by component do not stack, so counting room by type would over-count and break
     * {@link #give}'s all-or-nothing guarantee on exactly the items ADR-0052 exists for.
     */
    private boolean fits(ItemStack prototype, int count) {
        int max = prototype.getMaxStackSize();
        long room = 0;
        for (int slot = 0; slot < inventory.items.size(); slot++) {
            ItemStack stack = inventory.items.get(slot);
            if (stack.isEmpty()) {
                room += max;
            } else if (ItemKeys.matches(prototype, stack) && stack.isStackable()) {
                room += Math.max(0, stack.getMaxStackSize() - stack.getCount());
            }
            if (room >= count) return true;
        }
        return false;
    }
}
