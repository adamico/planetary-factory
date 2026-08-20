# How does this pack implement spoilage?

**Answer: four sibling items per material, advanced by a forked Mrbysco/Spoiled whose per-stack
timer has been deleted. No data component, no NBT, no per-stack state of any kind.**

Read against [Mrbysco/Spoiled](https://github.com/Mrbysco/Spoiled), branch `multi/1.21`, HEAD
`214e842` ("Increment version [build] [publish]", 2026-06-10); against the pack's own
`mods/gtceu-1.21.1-7.0.2.jar`, `mods/Mekanism-1.21.1-10.7.19.85.jar`, `mods/create-1.21.1-6.0.10.jar`,
`mods/integrateddynamics-1.21.1-neoforge-1.34.0.jar` and `mods/kubejs-neoforge-2101.7.1-build.181.jar`;
and against NeoForge `21.1.248` and vanilla `1.21.1` under `Install/libraries`.

Shelf lives are taken verbatim from [Factorio's spoilage mechanics](https://wiki.factorio.com/Spoilage_mechanics).
Spoiled was read for technique; we take its sweep and datapack recipe format, and delete its
storage model.

## Summary of the design

A spoilable material is **four registered items**, not one item with a freshness value:

```
jelly_fresh --> jelly_ripe --> jelly_stale --> jelly_spoiling --> spoilage
```

Each arrow is a `spoiled:spoil_recipe`. Advancement is a probability roll per sweep pass, so a stack
carries no state at all. Recipes that consume jelly reference a **tag** spanning all four items.

Everything below is why.

## 1. How a decaying item is stamped and stored

**It isn't. The item's identity *is* its freshness.**

Spoiled's own answer is a registered data component — `SpoiledComponents.SPOIL_TIMER`
(`registration/SpoiledComponents.java`), a `DataComponentType<SpoilTimer>` built
`.persistent(CODEC).networkSynchronized(STREAM_CODEC)`, where

```java
public record SpoilTimer(int timer, int maxTime) {}
```

`timer` counts elapsed update passes; `maxTime` is copied from the recipe. **We reject this**, and
the reason is stack fragmentation.

### Why any per-stack freshness value fragments stacks

Data components participate in `ItemStack.isSameItemSameComponents`, so two stacks whose freshness
differs **do not merge**. A bioflux with a 1200-pass lifetime has 1200 distinct timer values, so a
chest accumulates a smear of unmergeable partial stacks. This is not a Spoiled quirk — it follows
directly from component-keyed stack equality, and it afflicts every design that writes a number onto
the stack.

Factorio does not have this problem because it averages freshness whenever items combine
("*If you take a stack of 10 items with freshness 50% and add one item of freshness 100%, the result
is a stack of 11 items with a freshness of 54.5%*"). Spoiled reimplements that rule by hand for
exactly one case — `StackFoodRecipe` (`recipe/StackFoodRecipe.java:60-72`) is a crafting recipe that
takes two stacks and writes the arithmetic mean of their timers.

### Averaging on merge is not implementable in this pack

This was investigated directly, because it is the only thing that would rescue a per-stack value.

- **One predicate, not overridable.** `ItemStack.isSameItemSameComponents(ItemStack, ItemStack)` is
  `static`, compares the two `PatchedDataComponentMap` fields directly, and has no `Item` dispatch
  and no NeoForge hook. `IItemStackExtension` exposes 41 methods and **none** affects stacking.
  NeoForge 21.1 has no per-item stacking override.
- **No event.** No merge or insertion event exists under `net/neoforged/neoforge/event/`.
  `ItemStackedOnOtherEvent` is the bundle-click GUI hook and never fires for a hopper or a pipe.
  `ItemHandlerHelper.canItemStacksStack` **no longer exists** — it was removed in the component
  rewrite.
- **The mutation sites are scattered, and mods duplicate them.** Averaging must run where counts
  change, and `grow()`/`setCount()` do not know the donor stack, so every call site needs patching.
  Vanilla has ~19 across `AbstractContainerMenu`, `Inventory`, `HopperBlockEntity`, `SimpleContainer`
  and others. **GTCEu alone reimplements the merge arithmetic in 24 classes**, including
  `ItemNetHandler`, `GTTransferUtils`, `NotifiableItemStackHandler`, `QuantumChestMachine$ItemCache`
  and `ConveyorCover`. AE2 and Create have their own storage layers again.
- **Two further hazards.** `ItemStack.hashItemAndComponents` backs `ItemStackLinkedSet` and
  `RecipeCache`; making differing-freshness stacks compare equal breaks the equals/hashCode contract
  they rely on. And `recipeessentials-1.21.1-4.7.jar` already `@Overwrite`s `DataComponentMap.equals`
  with a cached hash, sitting on the exact bytecode a stacking Mixin would need.

A Mixin fork would therefore buy a mechanic that averages correctly in a vanilla chest and silently
resets the moment a GT pipe touches the stack — a rule the player cannot learn because it holds only
sometimes. **Averaging is ruled out, and with it every per-stack freshness value.**

### What we store instead

Nothing. Four separate registered items per material, one per freshness state. Fragmentation is
therefore exactly four, and it is *legible* fragmentation — the variants have names, textures and
tooltips, so a player reads them as information rather than as inventory noise.

| Operation | What happens |
| --- | --- |
| **Stacking** | Two `jelly_stale` stacks merge normally. They are the same item with no components. |
| **Splitting** | Both halves are the same item. Nothing to copy or divide. |
| **Crafting** | The product is whatever the recipe declares. Freshness does not propagate (see §3). |
| **Advancing** | The stack is replaced with the next item in the chain. |

The KubeJS constraint that shaped the earlier draft is now irrelevant, but is recorded because it
rules out the obvious alternative: KubeJS 2101.7.1 **cannot register `DataComponentType`s** —
`dev.latvian.mods.kubejs.KubeJSComponents` is an empty interface and nothing under
`dev/latvian/mods/kubejs/registry/` mentions `data_component_type`. Registering a component needs
Java. Registering four items does not.

## 2. Ticked or lazy, and what it costs

**Ticked — and the earlier draft's rejection of tick sweeps was wrong about the cause.**

Spoiled's `SpoilHandler.onWorldTick` (`forge/.../handler/SpoilHandler.java:40`) runs on
`LevelTickEvent.Post` every `spoilRate` game ticks, default 30 (`SpoiledConfig.java:72`). Each sweep
walks every ticking chunk's block entities, resolves `Capabilities.ItemHandler.BLOCK` on each, and
then — the expensive part — for **every non-empty slot** calls `SpoilHelper.getSpoilRecipe`, which is
`level.getRecipeManager().getRecipesFor(SPOIL_RECIPE_TYPE, new SingleRecipeInput(stack), level)`.

That is a recipe-manager query per stack per pass, plus a `SingleRecipeInput` allocation, plus a
registry lookup and a stream over the stack's component map. A mid-game GregTech/AE2 base plausibly
keeps several hundred chunks loaded, which puts this in the six-figures-per-second range.

**The cost driver is that query, not the walking.** Our spoilable set is a handful of items known at
datapack load; the fork replaces the per-stack query with a lookup in an `Item ->
SpoilRecipe` map built once on reload. What remains is a hash lookup per occupied slot per pass,
which is affordable at the 30-tick default.

This matters because the fastest material is **iron and copper bacteria at 60 seconds**, spoiling
*into ore*. That is a production step on the critical path of a resource chain, not a loss — a
low-frequency sweep would throttle ore throughput to its own period. The sweep has to stay frequent,
and after the caching fix it can be.

Cost is **reasoned, not measured**. `spark` is in the roster (`mods/spark-1.10.124-neoforge.jar`) and
the fork should be profiled once it exists; the number that matters is the per-pass cost after
caching, which no reasoning here can substitute for.

### Unloaded chunks

`ChunkHelper.getBlockEntityPositions` takes `getTickingChunk()`, so a non-ticking chunk's block
entities are skipped entirely. **As shipped, Spoiled freezes decay in unloaded chunks** — a stack
sealed in a chest for a week returns exactly as fresh as it was left. The ticket names this outcome
unacceptable, and dropping the per-stack timer does not fix it: with no timestamp anywhere, there is
nothing to compute elapsed time from.

The fork closes this with a **per-chunk** last-swept game tick held in a `SavedData`, not a per-stack
one. On chunk load, the number of missed passes is `(now - lastSwept) / spoilRate`, and the
accumulated advancement is applied as a single draw rather than by simulating each pass. Per-chunk
state costs one long per chunk and — critically — introduces **no per-stack data**, so the
fragmentation argument in §1 is untouched.

### Why probability, and why four stages

With no stored timer, advancement between stages must be a per-pass probability `p = 1/spoiltime`.
A single such stage is memoryless: lifetime is exponentially distributed, half the items die before
69% of nominal, and the tail is unbounded. That is unacceptably random for a mechanic whose whole
tension is time pressure.

**Four sequential stages sum to an Erlang-4 distribution.** Variance falls with the square root of
the stage count: the coefficient of variation drops from 1.0 to 0.5. Jelly's nominal 240 seconds
lands roughly between 120 and 400 rather than between zero and forever. The four freshness states
therefore do double duty — they are the player-facing legibility *and* the variance reduction, from
the same structure. More stages would tighten it further, converging on deterministic, at the cost of
more item variants.

The residual cost is honest and worth stating: an item's remaining life is **not** exactly knowable,
only bounded. Factorio's spoilage is deterministic; ours is not.

## 3. Can machine I/O see freshness?

**Yes, natively, in every mod — because freshness is item identity.**

This question was originally scoped to GregTech. Scoping it there was a mistake: resource processing
in this pack happens across Create, Mekanism, GregTech and Integrated Dynamics, with multiple
optional paths, so any answer that works only in GT would silently bias which path a player picks.

Four sibling items answer it for all of them at once. A recipe that accepts any freshness references
a **tag** containing all four. A recipe that demands a specific state references the item directly.
Tags and item IDs work identically in every mod's recipe system, with no integration code anywhere.

For the record, the GT-specific mechanism does exist and is **not needed**: GTCEu 7.0.2 ships
`com.gregtechceu.gtceu.api.recipe.ingredient.ExDataComponentIngredient extends
net.neoforged.neoforge.common.crafting.DataComponentIngredient`, and its lookup tree is
component-aware via `ItemDataComponentMapIngredient`. The `RecipeCondition` hierarchy is *not* the
place — every implementation in `common/recipe/condition/` (`BiomeCondition`, `DimensionCondition`,
`CleanroomCondition`, `EnvironmentalHazardCondition`, …) tests world or machine state and none
receives the input stack. Duration modulation via `recipeModifier` / `beforeWorking` exists only on
`MultiblockMachineBuilderWrapper`, not on the single-block or KubeJS builders — another reason not to
build the mechanic on GT-only affordances.

**Freshness does not propagate through crafting.** Factorio's products inherit input freshness, but
inheritance would have to be implemented separately in every mod's recipe system, which is exactly
the cross-mod uniformity failure above. A recipe's output freshness is whatever the recipe declares.

## 4. What happens when a spoilable item is inside a machine

Investigated because it determines whether spoilage can deadlock a factory.

| Mod | Extract from an input slot? | Evidence |
| --- | --- | --- |
| **GregTech** | **Never** | `NotifiableItemStackHandler.extractItem` → `canCapOutput() ? … : ItemStack.EMPTY`; input handlers built with `IO.IN` |
| **Mekanism** | **Never** externally | `InputInventorySlot` passes `ConstantPredicates.notExternal()` as its `canExtract` |
| **Create** | Per-machine | Basin, Depot, Deployer yes; Millstone, Saw, Crushing Wheel, Mechanical Crafter no |
| **Integrated Dynamics** | Always | plain `InvWrapper`, no override |

For GregTech this is absolute: pipes, AE2, SFM, GT's own Conveyor Modules and **even an Item Voiding
Cover** all route through `getItemHandlerCap` and receive `EMPTY`. Mekanism's `INPUT_OUTPUT` side
setting does not help, because the gate is at the slot, not the side. In both, the only recovery is a
human opening the GUI or breaking the machine.

There *is* a generic escape hatch, and it needs no Mixins:

- **GregTech:** `NotifiableItemStackHandler.extractItemInternal` / `insertItemInternal` are **public**
  and delegate straight to `storage` with no IO check; the backing `storage` is a public final field.
  The whole path is public — `MetaMachine.getMachine(level, pos)` → `getTraits()` → filter on the
  public `handlerIO`.
- **Mekanism:** `ConstantPredicates.notExternal()` is `type != EXTERNAL`, so `MANUAL` and `INTERNAL`
  both pass. `TileEntityMekanism.getInventorySlots(null)` is public and returns the full slot list;
  `((IMekanismInventory) be).extractItem(i, n, null, Action.EXECUTE)` resolves to `INTERNAL`.

**We deliberately do not use it.** Clogging is a stated, documented hazard that the player is
responsible for avoiding, in keeping with how modded processing chains work generally. The escape
hatch is recorded here because it is the recovery mechanism available if that stance ever changes.

Two containment measures make the stance fair rather than punitive. Spoilable recipes are gated to
the **Biochamber**, a machine the pack authors, so no spoilable item routinely enters a machine we do
not control. And the Biochamber's clogging behaviour is surfaced in-game — Jade and the machine
tooltip — rather than left for the player to discover by deadlock.

## 5. Recommendation

**Fork Mrbysco/Spoiled (MIT). Delete `SpoilTimer`. Register four items per spoilable material and
chain them with `spoiled:spoil_recipe`.**

The fork's diff is subtractive except for two additions:

1. **Remove the Overworld gate.** `SpoilHandler.java:42` is `if (level.dimension() != Level.OVERWORLD)
   return;`, hardcoded with no config. Sapros is a different dimension, so **Spoiled as shipped does
   nothing in this pack.** This alone forces a fork.
2. **Delete the `SpoilTimer` component** and its registration; replace `SpoilHelper.updateSpoilingStack`
   with a per-pass probability roll.
3. **Cache `getSpoilRecipe`** by item ID at reload instead of querying the recipe manager per stack.
4. **Add per-chunk last-swept tracking** in a `SavedData`, with catch-up on chunk load.

`spoiled:spoil_recipe` is a plain datapack recipe and needs no CraftTweaker — the `.zs` files in
`EXAMPLE_SCRIPTS/` are a convenience wrapper. The JSON form (`EXAMPLES/data/spoiled/recipes/bread_to_stone.json`):

```json
{
  "type": "spoiled:spoil_recipe",
  "ingredient": { "item": "minecraft:bread" },
  "spoiltime": 20,
  "priority": 5,
  "result": { "id": "minecraft:stone" }
}
```

`spoiltime` counts **update passes, not ticks** — at the default `spoilRate` of 30 ticks per pass,
Create's `bar_of_chocolate` at 800 works out to one Minecraft day, matching the day-based headings in
the example scripts.

### Shelf lives

Factorio's published values, converted at 20 TPS and 30 ticks per pass (seconds × ⅔), then divided by
four for the per-stage `spoiltime`:

| Material | Factorio | Total passes | Per-stage `spoiltime` | Spoils into |
| --- | --- | --- | --- | --- |
| Bioflux | 2 h | 4800 | 1200 | Spoilage |
| Yumako / Jellynut | 1 h | 2400 | 600 | Spoilage |
| Nutrients | 5 min | 200 | 50 | Spoilage |
| Jelly | 4 min | 160 | 40 | Spoilage |
| Yumako mash | 3 min | 120 | 30 | Spoilage |
| Iron / Copper bacteria | 1 min | 40 | 10 | Iron / Copper ore |

The 120× spread between bacteria and bioflux is what forces a frequent sweep, and is why the
`getSpoilRecipe` caching in the fork is load-bearing rather than an optimisation.

### Ruled out, and why

| Ruled out | Why |
| --- | --- |
| **Shipping Mrbysco/Spoiled unmodified** | Overworld-only (`SpoilHandler.java:42`), so it cannot act on Sapros at all. |
| **Forking Food Spoilage instead** | **All Rights Reserved**, no public source. Also food-oriented (everything decays to rotten flesh), config-driven rather than datapack-driven, and built on a continuous per-item freshness percentage — the model §1 rules out. Its container-preservation multipliers, the feature that makes it attractive, already exist in Spoiled as `containerModifier` / `itemContainerModifier`. |
| **Any per-stack freshness value** (component, NBT, timestamp) | Fragments stacks unboundedly, and averaging-on-merge is not implementable (§1). |
| **A Mixin fork to intercept merging** | ~19 vanilla mutation sites, 24 more in GTCEu alone, plus AE2 and Create; breaks the `hashItemAndComponents` contract; collides with `recipeessentials`. Would work only sometimes. |
| **A registered `DataComponentType` from KubeJS** | KubeJS 2101.7.1 cannot register component types. Moot now — we store nothing. |
| **Lazy resolution on access** | The earlier recommendation. There is no machine-boundary hook that generalises beyond GT multiblocks, so a lazily-resolved item would enter a Create or Mekanism recipe unresolved. |
| **A single probabilistic stage** | Exponential lifetime: unbounded tail, ~63% of items dead before nominal. Four stages give Erlang-4 and halve the spread. |
| **Freshness inheritance through crafting** | Would need separate implementation in each of four mods' recipe systems; uneven enforcement would bias which processing path players choose. |
| **Duration modulation on stale input** | `recipeModifier` / `beforeWorking` exist only on GT multiblocks. Same cross-mod objection. |
| **Trash slots à la Factorio** | We can add them to machines we author, not to Mekanism's or Create's. Uniformity fails. Clogging is a documented hazard instead (§4). |
| **A GT `RecipeCondition`** | No condition in the hierarchy receives the input stack. |

## Consequence for the pack

- **Spoilage needs a Java fork**, not KubeJS scripting. This overturns the ticket's framing: adding a
  *third-party* mod is ruled out, but the mechanic cannot be built from KubeJS alone, and Spoiled is
  MIT-licensed with a NeoForge 1.21.1 build. The fork is tracked separately.
- **Every spoilable material is four registered items plus one tag.** The Sapros spec owes the
  material list, the four state names per material, and the decay target of the final state.
- **Recipes consuming a spoilable reference the tag**, so they work identically in Create, Mekanism,
  GregTech and Integrated Dynamics with no integration code.
- **Spoilable recipes are gated to the Biochamber**, a GT multiblock this pack authors.
- **Clogging is a documented hazard.** A spoiled stack in a machine input jams it, recoverable only
  by hand. This is stated to players, not engineered away.
- **Decay runs in every dimension**, including in flight, once the Overworld gate is removed.
- **Iron and copper bacteria decaying into ore is the terminal transition of this same mechanism**,
  not a special case.

What the Sapros spec still owes is the material list, the state names, and the decay targets —
design decisions, not research.
