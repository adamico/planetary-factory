package com.planetaryfactory.core.assembler;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A multiset of items, which is the only shape the Assembler ever holds one in.
 *
 * <p>A reservation, a plan's buffer and a step's cost are all the same thing counted, and writing
 * that as a bare {@code Map<String, Integer>} meant the same "add, subtract, and drop the key at
 * zero" three times over. A zero count is never stored, so {@link #isEmpty()} means exactly "holds
 * nothing" and never "holds nothing, several times".
 *
 * <p>Insertion order is kept so that what a plan hands back on a cancellation arrives in a stable
 * order rather than a hash one.
 */
public final class ItemBag {

    private final Map<String, Integer> counts = new LinkedHashMap<>();

    public ItemBag() {
    }

    public static ItemBag of(Map<String, Integer> counts) {
        ItemBag bag = new ItemBag();
        counts.forEach(bag::add);
        return bag;
    }

    /** The flattened total of a list that may name the same item more than once. */
    public static ItemBag ofAmounts(List<ItemAmount> amounts) {
        ItemBag bag = new ItemBag();
        for (ItemAmount amount : amounts) bag.add(amount.item(), amount.count());
        return bag;
    }

    public void add(String item, int count) {
        if (count <= 0) return;
        counts.merge(item, count, Integer::sum);
    }

    /** Removes up to {@code count}, and drops the item entirely once it hits zero. */
    public void remove(String item, int count) {
        if (count <= 0) return;
        int left = counts.getOrDefault(item, 0) - count;
        if (left <= 0) counts.remove(item); else counts.put(item, left);
    }

    public int count(String item) {
        return counts.getOrDefault(item, 0);
    }

    public boolean isEmpty() {
        return counts.isEmpty();
    }

    /** A read-only copy, in insertion order -- {@code Map.copyOf} would lose the order. */
    public Map<String, Integer> asMap() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(counts));
    }

    /** Every entry, in insertion order, for iterating while the bag is being changed. */
    public List<ItemAmount> amounts() {
        return counts.entrySet().stream()
                .map(entry -> new ItemAmount(entry.getKey(), entry.getValue()))
                .toList();
    }

    public ItemBag copy() {
        return ItemBag.of(counts);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ItemBag that && counts.equals(that.counts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(counts);
    }

    @Override
    public String toString() {
        return counts.toString();
    }
}
