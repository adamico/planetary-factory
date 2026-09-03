package com.planetaryfactory.core.assembler;

import java.util.List;

/**
 * One craft in a resolved plan: what it eats, what it makes, and how long it takes.
 *
 * <p>{@code durationTicks} is Factorio's {@code energy_required x 20}, unmodified -- the Assembler
 * runs at speed 1 and ADR-0029 leaves the durations alone, because what makes hand-crafting slow
 * here is that the queue is serial.
 *
 * <p>The inputs are carried on the step rather than derived from the recipe because the queue never
 * looks a recipe up: a plan is resolved once, paid for once, and then is not re-resolved (ADR-0038).
 */
public record CraftStep(String recipe, List<ItemAmount> inputs, List<ItemAmount> outputs, int durationTicks) {

    public CraftStep {
        if (recipe == null || recipe.isBlank()) {
            throw new IllegalArgumentException("a craft step needs a recipe id");
        }
        if (durationTicks < 0) {
            throw new IllegalArgumentException("a craft step cannot take " + durationTicks + " ticks");
        }
        inputs = List.copyOf(inputs);
        outputs = List.copyOf(outputs);
    }
}
