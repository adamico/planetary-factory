# How does this pack implement spoilage?

**Answer: a freshness *stage* written into `minecraft:custom_data`, advanced lazily from a stored
game-time stamp, never ticked.**

Read against [Mrbysco/Spoiled](https://github.com/Mrbysco/Spoiled), branch `multi/1.21`, HEAD
`214e842` ("Increment version [build] [publish]", 2026-06-10), and against the pack's own
`mods/gtceu-1.21.1-7.0.2.jar` and `mods/kubejs-neoforge-2101.7.1-build.181.jar`.

Spoiled was read for technique. Its storage answer is the right one and we take it. Its *advance*
answer is a tick sweep, and that is the one thing this document rules out.

## 1. How a decaying item is stamped and stored

Spoiled uses a **registered data component**, not NBT and not a capability.

`SpoiledComponents.SPOIL_TIMER` (`registration/SpoiledComponents.java`) is a
`DataComponentType<SpoilTimer>` built `.persistent(CODEC).networkSynchronized(STREAM_CODEC)`, and
`SpoilTimer` (`component/SpoilTimer.java`) is a two-int record:

```java
public record SpoilTimer(int timer, int maxTime) {}
```

`timer` is a **count of elapsed update ticks**, not a timestamp — this is the fact everything in
section 2 turns on. `maxTime` is the number of updates the item survives, copied from the recipe.

Behaviour on the three operations the ticket asks about:

| Operation | What happens | Where |
| --- | --- | --- |
| **Stacking** | Data components participate in `ItemStack.isSameItemSameComponents`, so two stacks with different `timer` values **do not merge**. Every distinct freshness value fragments the stack. | vanilla |
| **Merging by hand** | Spoiled papers over that with a crafting recipe: `StackFoodRecipe` takes exactly two stacks of the same item, checks `totalUnderMax`, and writes the **arithmetic mean** of the two timers onto the combined stack. Gated behind the `mergeSpoilingFood` config via `MergeRecipeCondition`. | `recipe/StackFoodRecipe.java:60-72` |
| **Splitting** | Nothing special. A split copies the whole component map, so both halves carry the same `timer`. Freshness is per-stack, never per-item. | vanilla |
| **Crafting into something else** | Nothing carries over. Vanilla assembles a recipe result fresh, so the output has no spoil component at all until something stamps one. Crafting **resets** freshness. | vanilla |

The stack-fragmentation consequence is severe and is not a Spoiled quirk we can dodge by choosing
a different carrier — it follows from component-based stack equality. **It is the reason this pack
must store a discrete stage, not a continuous number.** See section 4.

### The carrier we can actually use

We cannot copy Spoiled's registered component. KubeJS 2101.7.1 has **no data-component-type
registration API**: `dev.latvian.mods.kubejs.KubeJSComponents` is an empty interface, and nothing
under `dev/latvian/mods/kubejs/registry/` mentions `data_component_type`. KubeJS reads and writes
components (`component/ItemComponentFunctions`, `MutableDataComponentHolderFunctions`) but cannot
create new types. Registering one means a Java mod, and per the ticket adding a mod is ruled out.

`minecraft:custom_data` is the carrier. It is vanilla, always available, persistent, network-synced,
holds arbitrary NBT, is writable from KubeJS, and — critically for section 3 — is matchable by a
`DataComponentPredicate`. Nesting our keys under a single `planetaryfactory` compound inside it
keeps us clear of other mods writing to the same component.

## 2. Ticked or lazy, and what it costs

**Spoiled ticks, and its sweep is exactly the shape that costs a GregTech pack its TPS.** We resolve
lazily instead.

`SpoilHandler.onWorldTick` (`forge/.../handler/SpoilHandler.java:40`) runs on `LevelTickEvent.Post`
every `spoilRate` game ticks — **default 30, i.e. ~40 sweeps per minute** (`SpoiledConfig.java:72`).
Note that the config comment on `spoilRate` describes it as a count of spoiling updates; the
code uses it as a tick interval (`gameTime % spoilRate == 0`). The code is the truth.
Each sweep:

1. `ChunkHelper.getBlockEntityPositions(level)` walks every `ChunkHolder` in the `ChunkMap`, takes
   `getTickingChunk()`, and **allocates a fresh `ArrayList` of every block-entity position in every
   ticking chunk**, then `.filter(isAreaLoaded)` allocates a second list.
2. For each block entity: a block-state lookup, a `BLOCK_ENTITY_TYPE` registry key lookup, a config
   map lookup, and a `Capabilities.ItemHandler.BLOCK` capability resolution.
3. **For every non-empty slot in every one of those handlers**, `SpoilHelper.getSpoilRecipe` calls
   `level.getRecipeManager().getRecipesFor(SPOIL_RECIPE_TYPE, new SingleRecipeInput(stack), level)`
   — a recipe-manager query, per stack, per sweep, plus a `SingleRecipeInput` allocation, plus a
   registry key lookup and a blacklist scan and a stream over the stack's whole component map.
4. Then `level.getAllEntities()` is copied into another `ArrayList` and streamed twice.

The cost is **reasoned, not measured** — the honest statement, since building and profiling a
prototype of a design we are rejecting would not change the conclusion. The arithmetic is what
rules it out. A mid-game GregTech/AE2 base plausibly keeps ~400 chunks loaded (Chunky and FTB Chunks
are both in the roster, and GT multiblocks and AE2 networks force-load aggressively). At even a
modest 15 inventory-bearing block entities per chunk and 20 occupied slots each — an AE2 drive, a
GT multiblock bus, or a 27-slot chest each blow well past that — that is **~120,000 recipe-manager
queries every 1.5 seconds**, ~80,000 per second, before a single item has actually spoiled. Step 1
alone re-allocates a six-figure `BlockPos` list 40 times a minute. This is a per-tick cost
proportional to *total stored items in the world*, in a genre of pack whose defining feature is
storing a great many items. `spark` is in the roster (`mods/spark-1.10.124-neoforge.jar`) if a number
is ever wanted, but the design does not need one to be rejected.

Two further findings from the same handler, both disqualifying on their own:

- **`if (level.dimension() != Level.OVERWORLD) return;`** (`SpoilHandler.java:42`). Spoiled only
  spoils in the Overworld. For a pack whose spoilage planet is Sapros, a different dimension
  entirely, the mod is not merely slow — it is inapplicable as shipped.
- **Unloaded chunks freeze time.** `getTickingChunk()` returns null for a non-ticking chunk, so its
  block entities are skipped and their `timer` never increments. A stack in a chest in an unloaded
  chunk comes back **exactly as fresh as it was left**, for a week or a year. This is precisely the
  outcome the ticket names as unacceptable.

### The lazy design, and what it costs

Store a **stamp**, not a counter: the `level.getGameTime()` at which the item was created or last
resolved. Freshness is then a pure function `f(now - stamp)`, evaluated only when something looks:
on machine input, on container open, on tooltip render, on pickup.

- **Cost:** an integer subtraction and a comparison at the moment of access. There is no per-tick
  cost at all, and no cost proportional to items in storage. Nothing is walked, nothing is
  allocated, and the number of evaluations is bounded by what the player and their machines actually
  touch rather than by what they own.
- **Unloaded chunks are handled correctly and for free.** Game time is world-global and advances
  while a chunk sleeps. A crate sealed for a week of play time reads as a week older the instant it
  is opened — the resource does not come back fresh, *and* the server simulated nothing to achieve
  that. This is the single strongest argument for the lazy design; the tick design cannot express it
  at all without simulating unloaded chunks.
- **The cost that is real:** decay becomes observable only on access. An item's displayed state and
  its stored state diverge until something resolves it. Any effect that must fire *at* the moment of
  spoiling rather than at the next observation of it — a spoiled item bursting out of a sealed crate
  unprompted — is not expressible. This is a genuine loss and we accept it; Factorio's spoilage is
  itself experienced almost entirely at the point of consumption.
- **Use `level.getGameTime()`, never `System.currentTimeMillis()`.** Game time is the world's own
  clock: it pauses in singleplayer, it survives being carried between saves, and it cannot be
  advanced by the player closing the game for a day.

## 3. Can GregTech machine I/O see freshness?

**Yes — through ingredient matching, not through a recipe condition.**

The `RecipeCondition` hierarchy is the wrong place to look, and this is worth stating because it is
the obvious first guess. Every implementation in
`com/gregtechceu/gtceu/common/recipe/condition/` — `BiomeCondition`, `DimensionCondition`,
`ThunderCondition`, `PositionYCondition`, `CleanroomCondition`, `EnvironmentalHazardCondition`,
`ResearchCondition`, `GameStageCondition`, and the rest — tests *world or machine state*. None
receives the input stack. A recipe condition cannot read freshness.

The ingredient layer can. GTCEu 7.0.2 ships
**`com.gregtechceu.gtceu.api.recipe.ingredient.ExDataComponentIngredient`**, which
`extends net.neoforged.neoforge.common.crafting.DataComponentIngredient`:

```java
public ExDataComponentIngredient(HolderSet<Item>, DataComponentPredicate, boolean /* strict */);
public boolean test(ItemStack);
public static Ingredient of(boolean, DataComponentPredicate, TagKey<Item>);
```

and — the part that makes it usable at scale — GT's recipe *lookup* is component-aware:
`com.gregtechceu.gtceu.api.recipe.lookup.ingredient.item.ItemDataComponentMapIngredient`
extends `ItemStackMapIngredient` and holds a `DataComponentIngredient`, so component-discriminated
recipes are indexed in the lookup tree rather than linearly scanned. Registered via
`GTIngredientTypes`.

Three consequences follow, and all three are load-bearing for the design:

1. **A GT recipe can require a specific freshness stage**, by matching a `DataComponentPredicate`
   on `minecraft:custom_data`. Non-strict matching tests the named keys only and ignores the rest of
   the component, which is what we want.
2. **`DataComponentPredicate` is exact-value matching, not a range.** There is no
   "freshness < 0.5" ingredient. A recipe can match `stage: "stale"`; it cannot match "timer between
   200 and 400". **This forces freshness to be a small enum of named stages** — independently of, and
   agreeing with, the stack-fragmentation argument in section 1.
3. **Plain ingredients still ignore components.** A recipe written the ordinary way accepts any
   freshness, so decay is invisible to the entire existing recipe set until we deliberately opt a
   recipe into caring. Nothing in GregTech breaks by our adding the component.

Machine *outputs* carry whatever the recipe declares — GT assembles results fresh, like vanilla. A
machine that should emit a fresh intermediate does so by declaring the fresh stamp on its output;
freshness does not flow through a machine unless we write it through.

## 4. Recommendation

**A discrete freshness stage plus a game-time stamp, both inside `minecraft:custom_data`, resolved
lazily on access, driven entirely from KubeJS.**

Concretely: `custom_data.planetaryfactory = { stage: "fresh" | "stale" | ..., stamped: <gameTime> }`.
On access, compute `now - stamped`, map it to the stage the elapsed time implies, and if the stage
has changed, rewrite both fields — or replace the item outright with its decayed successor, which is
what "iron bacteria decay into ore" means in practice. Stage transitions are the only thing that
mutates the component, so a stack changes identity a bounded handful of times over its life instead
of on every update tick.

The discrete stage is not a simplification, it is a requirement, and two independent findings force
it: `DataComponentPredicate` cannot express a range (§3), and a continuous counter fragments stacks
into one stack per distinct value (§1). With a stage enum, all "fresh" units of an item stack
together and there are only as many stack variants as there are stages.

**Ruled out, and why:**

| Ruled out | Why |
| --- | --- |
| **Shipping Mrbysco/Spoiled** | Overworld-only (`SpoilHandler.java:42`), so it cannot act on Sapros. Food-oriented. And its tick sweep is the cost in §2. |
| **Any per-tick sweep of containers** | Cost scales with items stored, not items used — the wrong axis for a GregTech pack. And it silently freezes decay in unloaded chunks, which the ticket rules out. |
| **A registered data component** | KubeJS 2101.7.1 cannot register `DataComponentType`s; it needs a Java mod, and adding a mod is out of scope. |
| **A continuous freshness counter** | Fragments stacks, and no GT ingredient can range-match it. |
| **`System.currentTimeMillis()` as the clock** | Wall-clock time decays items while the game is closed and ignores singleplayer pause. Use `level.getGameTime()`. |
| **A GT `RecipeCondition`** | No condition in the hierarchy receives the input stack. Freshness lives at the ingredient layer. |

## Consequence for the pack

Spoilage on Sapros is **KubeJS work in this repo**: no new mod, no fork change, no Java. The pieces
the Sapros spec can now assume:

- **Storage** is `minecraft:custom_data`, namespaced under a `planetaryfactory` compound, carrying a
  stage name and a `level.getGameTime()` stamp.
- **Stages are a named enum**, deliberately few. The spec's job is to name them per material
  — bioflux, biosulfur, bioplastic, iron bacteria, copper bacteria — and to state each stage's
  duration in game ticks and what the final stage decays into. Iron and copper bacteria decaying
  into ore is the terminal transition of exactly this mechanism.
- **Decay resolves on access**, and never on a tick handler. Machine input, container interaction
  and tooltip are the resolution points.
- **A stack sitting in an unloaded chunk ages correctly**, because game time advances without it.
- **GregTech machines can demand or refuse a stage** via `ExDataComponentIngredient` /
  NeoForge `DataComponentIngredient` on `custom_data`, non-strict. Recipes that say nothing about
  freshness accept any stage, so no existing recipe changes behaviour.
- **Crafting resets freshness** unless a recipe explicitly stamps its output, so every recipe
  producing a decaying intermediate must declare the stamp on its result. This is a spec obligation,
  not a default.

Nothing above needs re-deriving to write the Sapros spec. What the spec still owes is the material
list, the stage names, the durations, and the decay targets — design decisions, not research.
