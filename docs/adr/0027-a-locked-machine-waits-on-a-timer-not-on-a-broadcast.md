---
status: accepted
---

# A locked machine waits on a timer, not on a broadcast

A GregTech machine loaded with the ingredients of a research-locked recipe sat idle, correctly,
and then stayed idle after the research completed. Only breaking and replacing it started the
recipe (issue #76). This ADR records what the stale state actually was and why the fix is a machine
that keeps checking rather than a research that goes and wakes machines.

## The stale state is GregTech's, and it is not a cached refusal

The first guess in the issue was a cached failed-search result on `RecipeLogic`, retried only on an
input change. It is simpler and worse than that: **the machine stops ticking entirely.**

`RecipeLogic.serverTick` (7.0.2) ends with

```java
} else if (lastRecipe == null && isIdle() && !machine.keepSubscribing() && !recipeDirty &&
        lastFailedMatches == null) {
            // No recipes available and the machine wants to unsubscribe until notified
            unsubscribe = true;
        }
```

and `searchRecipe` is

```java
return machine.getRecipeType().searchRecipe(machine, r -> matchRecipe(r).isSuccess());
```

The pack's lock refuses inside `matchRecipe` (`RecipeLogicMixin`), on the `unlock_recipe` effect
that carries ADR-0018's progression spine. So a locked recipe fails the trie's *predicate* — it
never reaches
`handleSearchingRecipes`, and nothing is recorded anywhere. The machine ends the tick with no
recipe, no failed matches, and unsubscribes itself from the server tick. After that it is not
refusing the recipe: it is not looking. Nothing re-subscribes it short of a block update, which is
precisely why break-and-replace was the only remedy a player could find.

The distinction matters because it rules out the tempting fix. There is no cache to invalidate.

## Neither remedy is free, and the cheap-looking one is the expensive one

The issue asks whether a research completing should nudge loaded machines, or whether the machine
should re-check on a timer.

**A broadcast on completion** sounds cheaper — it fires a handful of times per playthrough instead
of every few ticks forever. Its cost is not in the firing. Researchd's completion path
(`ResearchTeamImpl.onCompleteResearch`) knows a team and a level; it does not know which block
entities exist, so the nudge has to enumerate the loaded block entities of every level, per
completion, to find the machines belonging to that team — with no API in either mod that hands
that set over. It also only ever covers *loaded* machines, so the timer's job (a machine in an
unloaded chunk, subscribing again on `onMachineLoad`) still has to be done by something else. It
buys a correct wake-up by adding a cross-mod scan and a second code path.

**A timer** needs no new mechanism at all, because GregTech already has one. A recipe it matched
but cannot currently run goes in `lastFailedMatches`, and that field's emptiness is the last term
of the unsubscribe condition above. Writing the refused recipe there puts the machine in a state
GregTech already understands: it stays subscribed, re-searches on its own, and starts the recipe on
the first search after the research lands — within a quarter of a second. Nothing in Researchd is
touched, and no completion event is subscribed to.

## The decision

**The refusal is remembered on the machine, and the machine finds the research itself.** The lock
wrapper writes the refused recipe into `lastFailedMatches`; `LockedRecipeRetry` holds the rule and
the reasoning, and is unit-tested without Minecraft.

## What it costs

Honestly stated, because it is a real cost and the machine no longer sheds it: a machine sitting on
a locked recipe's ingredients runs a full trie search every fifth tick — `keepSubscribing` defaults
to true and `serverTick` gates the search on `getOffsetTimer() % 5` — for as long as it waits,
which may be hours of play. A machine overriding `keepSubscribing` to false searches every tick.

This is load GregTech's unsubscribe would otherwise have shed, and it is bounded by the number of
machines a player has loaded with the ingredients of a recipe they have not yet unlocked. In a pack
whose research gates a growing share of the tech tree that number is not always small, and if it
ever shows up in a profile the escape is the broadcast rejected above — added *alongside* this, not
instead of it, since the timer is also what covers a machine whose chunk was unloaded when the
research completed.

## Consequences

- The player-visible sequence of #74, #75 and #76 finally terminates: the recipe is visible in EMI
  and marked locked, the machine accepts the ingredients and idles, the research completes, and the
  machine starts.
- Researchd's completion path stays untouched, so nothing here breaks when that fork moves.
- The fix lives entirely behind `matchRecipe`, already the single funnel through which every
  GregTech lock is tested (`RecipeLogicMixin`). There is no second place to keep in sync.
