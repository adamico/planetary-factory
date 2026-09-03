package com.planetaryfactory.core.assembler;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A player inventory that is a multiset and a slot count, which is all {@link AssemblerQueue} ever
 * asks of one.
 *
 * <p>The slot count is the only reason this is not a bare map: "the inventory is full" is the
 * condition the queue has to pause on, and a multiset alone can never be full.
 */
final class TestPlayerItems implements PlayerItems {

    private final Map<String, Integer> items = new LinkedHashMap<>();
    private final int slots;
    private final int stackSize;

    TestPlayerItems() {
        this(36, 64);
    }

    TestPlayerItems(int slots, int stackSize) {
        this.slots = slots;
        this.stackSize = stackSize;
    }

    TestPlayerItems with(String item, int count) {
        items.merge(item, count, Integer::sum);
        return this;
    }

    @Override
    public int count(String item) {
        return items.getOrDefault(item, 0);
    }

    @Override
    public int take(String item, int count) {
        int taken = Math.min(count, count(item));
        if (taken <= 0) return 0;
        int left = count(item) - taken;
        if (left == 0) items.remove(item); else items.put(item, left);
        return taken;
    }

    @Override
    public boolean give(String item, int count) {
        if (count <= 0) return true;
        int free = (stackSize - count(item) % stackSize) % stackSize;
        int spareSlots = slots - usedSlots();
        if (count > free + (long) spareSlots * stackSize) return false;
        items.merge(item, count, Integer::sum);
        return true;
    }

    private int usedSlots() {
        int used = 0;
        for (int held : items.values()) used += (held + stackSize - 1) / stackSize;
        return used;
    }

    Map<String, Integer> contents() {
        return Map.copyOf(items);
    }
}
