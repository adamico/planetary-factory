package com.planetaryfactory.core.research;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Which researches unlock a given recipe -- the id-to-research direction of Researchd's
 * {@code unlock_recipe} effects.
 *
 * <p>Researchd answers "is this recipe blocked <em>for this team</em>"
 * ({@code ResearchdApi.isRecipeBlocked}). That question needs a team, and the case this index
 * exists for is precisely the one with no team: a machine carrying no placed-by attachment pushes
 * no filter frame, so there is nobody to ask about. The question that can still be answered is the
 * team-independent one -- "is this recipe one that <em>some</em> research unlocks" -- and that is
 * what an index built from the research registry gives.
 *
 * <p>Deliberately free of any Minecraft type. Both parameters are the caller's: the mod passes
 * {@code ResourceLocation} and {@code ResourceKey<Research>}, the tests pass strings, and the class
 * is a plain-JVM unit under the pack's testing policy. {@link ResearchLocks} is the only place that
 * knows how to fill one from a level.
 *
 * <p>Immutable once built, and iteration order follows insertion so that a caller rendering the
 * unlocking researches gets a stable answer -- issue #75 needs that direction to name the research
 * in its annotation.
 *
 * @param <I> the recipe id type
 * @param <R> the research key type
 */
public final class RecipeResearchIndex<I, R> {

    private static final RecipeResearchIndex<?, ?> EMPTY = new RecipeResearchIndex<>(Map.of());

    private final Map<I, Set<R>> byRecipe;

    private RecipeResearchIndex(Map<I, Set<R>> byRecipe) {
        this.byRecipe = byRecipe;
    }

    @SuppressWarnings("unchecked")
    public static <I, R> RecipeResearchIndex<I, R> empty() {
        return (RecipeResearchIndex<I, R>) EMPTY;
    }

    public static <I, R> Builder<I, R> builder() {
        return new Builder<>();
    }

    /**
     * Whether any research unlocks {@code recipeId}. This is the bypass predicate: a recipe no
     * research mentions was never locked, so an unowned machine running it is not a bypass at all.
     */
    public boolean isUnlockedByResearch(I recipeId) {
        return this.byRecipe.containsKey(recipeId);
    }

    /** The researches unlocking {@code recipeId}, in registry order; empty if none do. */
    public Set<R> researchesUnlocking(I recipeId) {
        return this.byRecipe.getOrDefault(recipeId, Set.of());
    }

    /** The number of distinct recipe ids some research unlocks. */
    public int size() {
        return this.byRecipe.size();
    }

    public static final class Builder<I, R> {
        private final Map<I, Set<R>> byRecipe = new LinkedHashMap<>();

        private Builder() {}

        /**
         * Records that {@code research} unlocks every id in {@code recipeIds}. Calling this twice
         * for the same pair is harmless: a recipe named by two researches keeps both, and one named
         * twice by the same research keeps it once.
         */
        public Builder<I, R> add(R research, Iterable<? extends I> recipeIds) {
            for (I recipeId : recipeIds) {
                this.byRecipe
                        .computeIfAbsent(recipeId, id -> new LinkedHashSet<>(1))
                        .add(research);
            }
            return this;
        }

        public RecipeResearchIndex<I, R> build() {
            Map<I, Set<R>> copy = new LinkedHashMap<>(this.byRecipe.size());
            this.byRecipe.forEach((id, researches) -> copy.put(id, Collections.unmodifiableSet(researches)));
            return new RecipeResearchIndex<>(Collections.unmodifiableMap(copy));
        }
    }
}
