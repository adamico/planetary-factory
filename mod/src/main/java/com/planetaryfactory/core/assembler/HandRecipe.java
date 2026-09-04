package com.planetaryfactory.core.assembler;

import java.util.List;

/**
 * One recipe as the resolver sees it: what it eats, what it makes, and how long one craft takes.
 *
 * <p>Deliberately not a {@code GTRecipe} and deliberately not a corpus row. The resolver plans over
 * a graph of these, and keeping the graph free of any Minecraft type is what lets the recursion --
 * the only genuinely hard thing in #161 -- be checked by an ordinary unit test.
 *
 * <p>{@code durationTicks} is one craft's, Factorio's {@code energy_required x 20} unmodified: the
 * Assembler runs at speed 1 and ADR-0029 leaves durations alone.
 */
public record HandRecipe(String id, List<ItemAmount> inputs, List<ItemAmount> outputs, int durationTicks) {

    public HandRecipe {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("a recipe needs an id");
        if (outputs == null || outputs.isEmpty()) {
            throw new IllegalArgumentException("recipe " + id + " makes nothing");
        }
        if (durationTicks < 0) {
            throw new IllegalArgumentException("recipe " + id + " cannot take " + durationTicks + " ticks");
        }
        inputs = List.copyOf(inputs);
        outputs = List.copyOf(outputs);
    }

    /** How many of {@code item} one craft makes, or zero if this recipe does not make it. */
    public int perCraft(String item) {
        for (ItemAmount output : outputs) {
            if (output.item().equals(item)) return output.count();
        }
        return 0;
    }

    /** The item a plan rooted at this recipe is for: its first output. */
    public String primaryOutput() {
        return outputs.get(0).item();
    }
}
