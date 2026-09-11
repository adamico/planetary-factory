---
status: accepted
supersedes: [74, 76, 79]
---

# Research is server-global, and one force removes the lock apparatus

Researchd scopes research to a team. The pack inherited that, and three tickets were spent making a
team-scoped lock work on machines the pack does not own: #74 (a machine with no owner bypasses every
lock), #76 (completing a research does not wake a machine already sitting on the locked recipe) and
#79 (an idle machine should say which research is stopping it). All three are answers to questions
that only exist because a machine has to know *whose* lock it is under.

**Factorio has one force.** Terra's engineer is alone, and the pack has never modelled rival
factories. FTB Teams is installed for chunk claims, not because two teams research separately.

## The rule

**Research is server-global. There is one force, and Researchd's own `RecipeManagerMixin` is the
whole of the enforcement.**

## Why the pack's mixins existed, and why they stop

GregTech never asked the vanilla `RecipeManager` — every machine walked its own `GTRecipeType`
ingredient trie — so Researchd's filter never saw the call and `RecipeLogicMixin` was the hook into
the foreign finder that `RecipeFilterContext` documents as the remedy.

**Modern Industrialization does not have that problem.** `MachineRecipeType.getRecipesWithCache`
goes through `getManagerRecipes(level)` to the vanilla `RecipeManager`, so Researchd's
`getAllRecipesFor` injection fires on the pack's machines with no hook of the pack's own.

## The reason this is a decision and not a discovery

MI's filter reach is free; its *correctness under teams* is not.

`MachineRecipeType.updateRecipeCache` stores the filtered result in a per-recipe-type
`Map<Item, List<RecipeHolder<MachineRecipe>>>` that is **team-blind** and rebuilt on a **20-second
wall clock** (`:95-116`). Whichever team's machine happens to tick across the interval boundary
builds the cache under its own pushed frame, and every machine on the server then reads that team's
filtered view until the next rebuild. The leak runs in both directions and it is nondeterministic —
strictly worse than no lock, because a lock that holds most of the time teaches the player a rule the
game does not keep.

So the fork is real: **one force and Researchd's built-in, or teams and a `banRecipe` override on
every pack machine.** This ADR takes the first, on the grounds that the second is apparatus in
service of a distinction Factorio does not have.

## What is deleted

`RecipeLogicMixin`, `RecipeLogicStatusMixin`, `LockedRecipeRetry`, `ResearchLocks`,
`PlacedByOwnership`, `MachineLockStatus`, `RecipeLockLookup` and `IdleMachineLockNote`, with
`mixins.json` losing both `gtceu.` entries. #74's fail-open rule, #76's retry list and #79's
derive-per-call tooltip all go with them.

**#76 does not need replacing.** `CrafterComponent.shouldUpdateActiveRecipe` re-searches every 100
ticks on an unchanged inventory, so a machine holding a locked recipe's ingredients starts within
five seconds of the research landing. GregTech's unsubscribe-forever failure has no equivalent here,
and the fix for it is deleted rather than ported.

**#79 is not deleted, it moves.** The reason an idle machine gives is a Jade provider (ADR-0059's
registration work carries it), which also gives #199 the same surface. The machine screen says
nothing, and that is accepted: MI's `MachineProblemsDisplay` carries one boolean with its two lines
hardcoded client-side, so reusing it would print "the inputs match more than one possible recipe"
about a research lock, which is actively false.

## The multiplayer gap, filed rather than ignored

Factorio is a multiplayer game and multiple forces are a real thing in it. This ADR does not argue
that team-scoped research is wrong — it argues that the pack does not model it today and should not
pay for apparatus it is not using. **A defect is filed**, and its content is the finding above rather
than the symptom: a later per-team fix must key `MachineRecipeType`'s cache by team, not add a filter
at `getAllRecipesFor`. A filter alone is the leak.
