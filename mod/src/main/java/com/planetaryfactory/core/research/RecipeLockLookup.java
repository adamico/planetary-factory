package com.planetaryfactory.core.research;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Whether a recipe is locked for the team asking, and which researches would unlock it -- the whole
 * question a recipe viewer needs answered per recipe (issue #75).
 *
 * <p>It takes two collaborators because the answer genuinely has two halves, and they come from
 * different places. "Locked for <em>me</em>" is the team's synced {@code RecipeUnlockEffectData},
 * reached through {@code ResearchdApi.isRecipeBlocked} and passed in here as a predicate. "Which
 * research unlocks it" is team-independent and comes from a {@link RecipeResearchIndex} built off
 * the research registry. Researchd exposes only the first; the annotation needs both, because a
 * mark that cannot name the research to go and complete tells the player no more than the machine
 * already did.
 *
 * <p>The team's data wins. Nothing synchronises a datapack reload's index rebuild with the effect
 * data the server last sent, so the two can disagree for a tick; when they do, a lock the index
 * cannot explain is still reported, unnamed. The reverse -- an id the index knows and the team has
 * already researched -- is simply not locked.
 *
 * <p>Deliberately free of any Minecraft type, like {@link RecipeResearchIndex}: the mod passes
 * {@code ResourceLocation} and {@code ResourceKey<Research>}, the tests pass strings, and this is a
 * plain-JVM unit under the pack's testing policy.
 *
 * @param <I> the recipe id type
 * @param <R> the research key type
 */
public final class RecipeLockLookup<I, R> {

    private final RecipeResearchIndex<I, R> index;
    private final Predicate<I> lockedForViewer;

    private RecipeLockLookup(RecipeResearchIndex<I, R> index, Predicate<I> lockedForViewer) {
        this.index = index;
        this.lockedForViewer = lockedForViewer;
    }

    /**
     * @param index the recipe-to-research direction of the pack's {@code unlock_recipe} effects
     * @param lockedForViewer whether the team being viewed as still has that recipe blocked
     */
    public static <I, R> RecipeLockLookup<I, R> of(RecipeResearchIndex<I, R> index, Predicate<I> lockedForViewer) {
        Objects.requireNonNull(index, "index");
        Objects.requireNonNull(lockedForViewer, "lockedForViewer");
        return new RecipeLockLookup<>(index, lockedForViewer);
    }

    /**
     * The lock on {@code recipeId}, or empty when the team can run it.
     *
     * <p>{@code recipeId} may be null: both viewers hand out recipes without one -- EMI synthesises
     * recipes, and JEI's {@code IRecipeCategory.getRegistryName} is a nullable default -- and such a
     * recipe is no more locked than it is anything else.
     */
    public Optional<Lock<R>> lockOn(I recipeId) {
        if (recipeId == null) return Optional.empty();
        if (!this.lockedForViewer.test(recipeId)) return Optional.empty();

        return Optional.of(new Lock<>(this.index.researchesUnlocking(recipeId)));
    }

    /**
     * A recipe the viewing team cannot run, and the researches that would unlock it -- in index
     * order, so a redraw does not shuffle the tooltip. The set is empty when the lock outran the
     * index; an annotation reading it has to cope with naming nothing.
     */
    public record Lock<R>(Set<R> unlockingResearches) {
        public Lock {
            // Not Set.copyOf: that is unordered, and the order is the point -- the index hands the
            // researches out in registry order so the tooltip reads the same on every redraw.
            unlockingResearches = Collections.unmodifiableSet(new LinkedHashSet<>(unlockingResearches));
        }
    }
}
