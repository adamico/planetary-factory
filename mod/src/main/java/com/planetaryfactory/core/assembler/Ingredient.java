package com.planetaryfactory.core.assembler;

import java.util.List;

/**
 * One of a recipe's demands: {@code count} of any one of {@code items}.
 *
 * <p>A recipe's ingredient is a choice, not an item. Two things in this pack make it one, and both
 * are ordinary rather than exceptional: the converter emits tag ingredients where Factorio's name
 * has no single Minecraft answer ({@code wooden_chest} eats {@code #minecraft:planks}), and
 * AlmostUnified rewrites plain item ingredients into unified tags at load, so a recipe whose emitted
 * JSON says {@code gtceu:iron_plate} accepts {@code create:iron_sheet} in the running game. A
 * resolver that read only the first match would refuse a plan the crafting grid would have accepted,
 * which is exactly what it did before this type existed.
 *
 * <p>Outputs stay {@link ItemAmount}: a recipe result is one concrete item, and there is nothing to
 * choose between.
 *
 * <p>The order of {@code items} is the order the resolver prefers them in. It comes from the
 * ingredient's own match list, which is an arbitrary order rather than a ranked one -- so it decides
 * which of two interchangeable items a plan spends, and nothing more.
 */
public record Ingredient(List<String> items, int count) {

    public Ingredient {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("an ingredient nothing satisfies is not an ingredient");
        }
        for (String item : items) {
            if (item == null || item.isBlank()) {
                throw new IllegalArgumentException("an ingredient needs item ids");
            }
        }
        if (count <= 0) {
            throw new IllegalArgumentException("an ingredient count of " + count + " is not a count");
        }
        items = List.copyOf(items);
    }

    /** The single-item case, which is most of them. */
    public static Ingredient of(String item, int count) {
        return new Ingredient(List.of(item), count);
    }

    /** What a shortfall of this ingredient is reported as: the first thing that would satisfy it. */
    public String preferred() {
        return items.get(0);
    }
}
