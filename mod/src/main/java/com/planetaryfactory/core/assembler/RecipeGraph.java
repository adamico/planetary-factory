package com.planetaryfactory.core.assembler;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The hand-craftable set, in the two directions the resolver walks it: by recipe id, which is what
 * EMI hands over, and by the item a recipe makes, which is what a chain-craft needs.
 *
 * <p>ADR-0038 gives the Assembler no recipe type of its own -- the hand set is a predicate over
 * Assembling Machine 1's recipes -- so this graph is built from whatever the server has loaded and
 * filtered, not from a registry of its own. {@code RuntimePlanSource} is the only place that knows
 * how to fill one.
 *
 * <p><b>One recipe per item.</b> The corpus admits no item with two hand recipes, asserted by
 * {@code tests/factorio/test_hand_resolver.py} -- which is what lets the resolver plan without a
 * cost model to choose between routes. If a second one ever appears here the first registered wins,
 * silently and stably, because the alternative is a resolver whose answer depends on load order.
 */
public final class RecipeGraph {

    private static final RecipeGraph EMPTY = new RecipeGraph(Map.of(), Map.of());

    private final Map<String, HandRecipe> byId;
    private final Map<String, HandRecipe> byItem;

    private RecipeGraph(Map<String, HandRecipe> byId, Map<String, HandRecipe> byItem) {
        this.byId = byId;
        this.byItem = byItem;
    }

    public static RecipeGraph empty() {
        return EMPTY;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** The recipe with this id, or null. EMI's fill button names a recipe, never an item. */
    public HandRecipe byId(String recipeId) {
        return byId.get(recipeId);
    }

    /** The recipe that makes this item, or null when nothing hand-makes it -- a plan's leaf. */
    public HandRecipe makerOf(String item) {
        return byItem.get(item);
    }

    public int size() {
        return byId.size();
    }

    public Collection<HandRecipe> recipes() {
        return byId.values();
    }

    public static final class Builder {

        private final Map<String, HandRecipe> byId = new LinkedHashMap<>();
        private final Map<String, HandRecipe> byItem = new LinkedHashMap<>();

        private Builder() {}

        public Builder add(HandRecipe recipe) {
            if (byId.putIfAbsent(recipe.id(), recipe) != null) return this;
            for (ItemAmount output : recipe.outputs()) {
                byItem.putIfAbsent(output.item(), recipe);
            }
            return this;
        }

        public RecipeGraph build() {
            return new RecipeGraph(Map.copyOf(byId), Map.copyOf(byItem));
        }
    }
}
