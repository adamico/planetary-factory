package com.planetaryfactory.core.research;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Why an idle machine is idle, when the answer is a research (issue #79).
 *
 * <p>A GregTech machine loaded with the ingredients of a locked recipe sits there saying nothing.
 * ADR-0027 explains why the refusal cannot speak for itself: on the fresh-search path the pack's
 * lock wrapper is the trie iterator's <em>predicate</em>, so refusing only means "this recipe does
 * not match". The machine ends the search with no recipe selected and therefore nothing to show a
 * reason against. To the player that is indistinguishable from broken, mis-piped or unpowered.
 *
 * <p>So the reason is worked out again from scratch whenever something asks: take the recipes the
 * machine's <em>current</em> contents match, ask {@link RecipeLockLookup} about each, and report
 * only when a research is the whole of what stands in the way.
 *
 * <p><b>Derived, never stored.</b> Nothing here is remembered on the machine and there is no
 * invalidation rule to get wrong -- pulling the ingredients out, or completing the research, changes
 * what the next query returns because the next query recomputes it. #76 is what a stored conclusion
 * re-checked on the wrong trigger costs, and this must not reproduce that. It also means the report
 * does not depend on whether GregTech re-runs its search when a machine empties.
 *
 * <p><b>One runnable candidate and there is nothing to say.</b> If the contents match a recipe the
 * team may already run, the machine is stopped by something else -- no power, full output, disabled
 * -- and blaming research would be a worse answer than none. Only when <em>every</em> matching
 * candidate is locked is an incomplete research the whole reason, which is exactly the claim the
 * message makes.
 *
 * <p>Minecraft-free, like {@link RecipeLockLookup}: the mod passes {@code ResourceLocation} and
 * {@code ResourceKey<Research>}, the tests pass strings, and this stays a plain-JVM unit under the
 * pack's testing policy.
 */
public final class MachineLockStatus {

    private MachineLockStatus() {}

    /**
     * The lock that is the whole reason an idle machine is idle, or empty when research is not the
     * reason -- because the machine matches nothing at all, or because it matches something it may
     * already run.
     *
     * @param candidateIds the recipes the machine's current contents match, ignoring the pack's
     *     lock. Consumed lazily and only as far as the answer needs: the caller hands over the
     *     machine's recipe-trie search, and walking all of it per status query would be a real cost
     *     for a machine sitting in a player's crosshair.
     * @param lookup whether the machine's team is locked out of an id, and which researches unlock
     *     it
     * @return the merged researches of every locked candidate, in the order the candidates came and
     *     then index order, each named once. Empty of researches -- but present -- when the lock
     *     outran the index; a caller reading it has to cope with naming nothing.
     */
    public static <I, R> Optional<RecipeLockLookup.Lock<R>> lockStopping(
            Iterable<I> candidateIds, RecipeLockLookup<I, R> lookup) {
        Objects.requireNonNull(candidateIds, "candidateIds");
        Objects.requireNonNull(lookup, "lookup");

        Set<R> researches = new LinkedHashSet<>();
        boolean anyLocked = false;
        for (I candidate : candidateIds) {
            Optional<RecipeLockLookup.Lock<R>> lock = lookup.lockOn(candidate);
            if (lock.isEmpty()) return Optional.empty();
            anyLocked = true;
            researches.addAll(lock.get().unlockingResearches());
        }
        return anyLocked ? Optional.of(new RecipeLockLookup.Lock<>(researches)) : Optional.empty();
    }
}
