package com.planetaryfactory.core.research;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.jetbrains.annotations.Nullable;

/**
 * Keeps a machine that refused a locked recipe on the tick list, so that completing the research
 * starts it.
 *
 * <p>Issue #76: a machine loaded with a locked recipe's ingredients does not start when the research
 * completes -- only breaking and replacing it does. The stale state is GregTech's, not Researchd's,
 * and it is not a cached refusal. {@code RecipeLogic.serverTick} ends by unsubscribing itself
 * outright when {@code lastRecipe == null && isIdle() && !keepSubscribing && !recipeDirty &&
 * lastFailedMatches == null}. A locked recipe fails the trie's predicate -- {@code searchRecipe} is
 * {@code searchRecipe(machine, r -> matchRecipe(r).isSuccess())}, and the pack's wrapper refuses
 * inside {@code matchRecipe} -- so the recipe never reaches {@code handleSearchingRecipes}, nothing
 * is recorded, and the machine stops ticking. Nothing short of a block update subscribes it again,
 * which is exactly why break-and-replace was the only remedy a player found.
 *
 * <p>The remedy is the one GregTech already uses for a recipe it matched but cannot currently run:
 * remember it in {@code lastFailedMatches}. That single field is the whole mechanism -- it is the
 * last term of the unsubscribe condition, so a machine holding a remembered match keeps ticking and
 * re-searches on its own (every tick, or every fifth for a machine that keeps subscribing). The
 * research completing therefore needs no nudge at all: the machine finds the recipe on its next
 * search, within a fraction of a second. That is the answer to the issue's second question -- a
 * timer, not a broadcast -- and it is why no hook into Researchd's completion path is needed.
 *
 * <p><b>The cost, stated honestly.</b> Only a machine <em>loaded with the ingredients of a recipe it
 * may not yet run</em> ticks this way, and it stops the moment the research lands or the ingredients
 * leave -- but "the research lands" can be hours of play away, so this is bounded per machine rather
 * than short-lived. What it costs meanwhile is one {@code findAndHandleRecipe} -- a full trie search
 * -- every fifth tick, since {@code IRecipeLogicMachine.keepSubscribing} defaults to true and
 * {@code serverTick} gates the search on {@code getOffsetTimer() % 5}. A machine that overrides
 * {@code keepSubscribing} to false searches every tick instead. This is load GregTech's unsubscribe
 * would otherwise have shed, and it is the price of the machine noticing the research at all.
 *
 * <p><b>A new list, never the old one mutated.</b> {@code serverTick} iterates
 * {@code lastFailedMatches} calling {@code checkMatchedRecipeAvailable}, which runs
 * {@code matchRecipe} again -- so the pack's wrapper refuses, and remembers, <em>while GregTech is
 * iterating the very list being written</em>. Appending in place would be a
 * {@link java.util.ConcurrentModificationException} on the machine's own tick. Copying leaves the
 * in-flight iteration on the list it started with. The copy stays a mutable {@link ArrayList}
 * because {@code handleSearchingRecipes} adds to this field itself.
 *
 * <p><b>Nothing clobbers the write.</b> Checked against 7.0.2's {@code RecipeLogic}: the only
 * assignment to {@code lastFailedMatches} after the search is the {@code null} at the top of the
 * <em>next</em> {@code findAndHandleRecipe}, which runs before the pack's wrapper repopulates it on
 * that same tick. Past the re-check loop {@code serverTick} only reads the field, in the unsubscribe
 * condition. So a refusal recorded on one tick is still there when that condition is evaluated,
 * which is the whole point.
 *
 * <p>Recipes are deduplicated by id rather than by identity: the re-check path hands back
 * {@code fullModifyRecipe}'s output, a distinct object for the same recipe, which would otherwise
 * make the list grow once per tick for as long as the machine waits.
 *
 * <p>Minecraft-free, so it is a plain-JVM unit under the pack's testing policy.
 */
public final class LockedRecipeRetry {

    private LockedRecipeRetry() {}

    /**
     * The refused recipe remembered alongside whatever was already remembered this search.
     *
     * @param remembered what the machine holds now, null before anything was remembered this tick
     * @param recipe the recipe just refused for being locked
     * @param id the recipe's identity for deduplication -- its recipe id, not the object. The key
     *     type is a parameter rather than {@code Object} so that a key compared by identity cannot
     *     be passed in silently: that would defeat the dedup and let the list grow every tick.
     * @return a fresh mutable list; never the one passed in, and never empty
     */
    public static <R, K> List<R> remember(@Nullable List<R> remembered, R recipe, Function<R, K> id) {
        List<R> next = remembered == null ? new ArrayList<>(1) : new ArrayList<>(remembered);
        K key = id.apply(recipe);
        for (R held : next) {
            if (id.apply(held).equals(key)) return next;
        }
        next.add(recipe);
        return next;
    }
}
