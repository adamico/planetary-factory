---
status: accepted
---

# Stock recipes go, and a survivor must name its surface

"The pack removes stock recipe sets wholesale and authors its own" is the most load-bearing sentence
in this design that is written down nowhere. It is the reason ADR-0007's cost estimate for closing
the Nether was wrong — *"blaze powder, netherite, ender pearls and every recipe in the stack that
assumes them"* is an inventory of recipes that do not ship — and it mis-scoped
[`#127`](https://github.com/adamico/planetary-factory/issues/127), which was filed to enumerate that
cost and closed unstarted once someone said the sentence out loud.
[`#124`](https://github.com/adamico/planetary-factory/issues/124) flagged the gap in its own answer
and declined to ticket it.

What exists instead is scattered and partial. ADR-0017 records removals per capability row, in a
column headed *the losing blocks*. [`#97`](https://github.com/adamico/planetary-factory/issues/97)
forbids the pack *emitting* a vanilla shaped recipe. ADR-0031 says the corpus authors every recipe it
contains. None of the three states the general case, and none of them covers a recipe the pack never
had an opinion about — which is most of them.

## The rule

**A stock recipe ships only if a decision names it and names the surface it is crafted on. Everything
else goes.**

The default is removal, and the burden is on the survivor. This is ADR-0017's *losing block* rule
generalised off the capability table: that table names about twenty blocks across four tech mods, and
the manifest is 111 entries.

## Why "wholesale" was never quite the right word

The premise is usually said as though removal were a policy the pack chose. For one recipe type it is
not a choice at all.

[`#90`](https://github.com/adamico/planetary-factory/issues/90) removed the vanilla crafting grid, and
`#34` finished the job on ADR-0017's Hand-crafting surface row: the Crafting Table, Crafting on a
Stick, CraftingTweaks, Sophisticated Backpacks' Crafting Upgrade, AE2's terminals, Create's Mechanical
Crafter and Mekanism's Formulaic Assemblicator are all cut. **After that there is no block in the pack
that executes a `minecraft:crafting_shaped` recipe of more than four ingredients.** A surviving stock
shaped recipe is therefore not "kept" — it is unreachable, exactly as `#97` says of one the pack
emits. `#97` is the rule for the emitter; this is the same fact read backwards, and it is the reason
the removal is not optional.

So the interesting question is not *are stock recipes removed*. It is **which recipes escape the
consequence**, and there are more of those than anyone has written down.

## The exceptions, enumerated

Seven classes. Six are visible in decisions already taken; the seventh is a hole.

### 1. The 2x2 inventory grid, which is not removable

`#90` named this and deferred it to `#95`, which dissolved it by making the Personal Assembler a
screen rather than an item — and in dissolving the *bootstrap* question left the *execution* question
open. The 2x2 is part of the inventory screen. It cannot be recipe-removed, and **every surviving
stock recipe that fits 2x2 is still craftable on it**: logs to planks, planks to sticks, and whatever
the QoL tail ships in that shape.

`#90` recorded that the 2x2 "goes empty only as a *consequence* — every recipe is authored to another
surface" and that this "closes by accident rather than by decision". That is true of the recipes the
pack authors and false of the ones it merely fails to remove. **This is the exception with the
sharpest teeth, because it is the one nobody chose.**

**Closed by [`#140`](https://github.com/adamico/planetary-factory/issues/140).** The grid is removed
in `planetaryfactory_core`, where it always had to be: an `InventoryMenu` mixin replaces the result
and the four input slots with slots that are inactive and refuse both directions, and cancels
`slotsChanged` so the result container is never filled by anything — including the recipe book,
which writes to the container without going through a slot and whose button is removed client-side
with the painted grid. The exception is not dissolved, only defanged: **every stock recipe that fits
2x2 is still loaded, and is now unreachable rather than craftable**, which is exactly this ADR's
reading of the rest of the shaped set.

### 2. Recipe types that are not the grid

The grid is gone; the furnace is not. [`#91`](https://github.com/adamico/planetary-factory/issues/91)
puts the four `smelting` recipes on **vanilla `minecraft:smelting`** — `data/pack/category-map.json`
records it — with vanilla's Furnace renamed as the rung-0 tier. Vanilla's *recipe type* survives and
carries pack-authored content.

The removal there is per-recipe rather than per-type, and `#91` names two: `log → charcoal` and
`ore → ingot`. Both are cut for progression reasons, not because the surface disappeared. **Vanilla
smelting is curated, not deleted** — and by the same argument so is any other executor the pack keeps
(smithing, brewing, stonecutting) if it keeps one, which nothing currently decides.

### 3. `native_mechanic` — a capability that needs no recipe

[`#93`](https://github.com/adamico/planetary-factory/issues/93) verified in
`create-1.21.1-6.0.10.jar` that `GenericItemFilling.canItemBeFilled` and
`GenericItemEmptying.canItemBeEmptied` key on the item's `IFluidHandlerItem` capability: the Spout
fills and the Item Drain drains any fluid-holding item **with no recipe at all**. Create's shipped
`filling`/`emptying` files exist for items that are *not* fluid handlers.

Eighteen `fill-barrel` / `empty-barrel` rows are therefore `native_mechanic` in
`data/pack/subgroup-owner.json` — in scope, fully supported, no recipe emitted — and the value is
deliberately distinct from `not_emitted`, which means cut. **Removing a stock recipe here would remove
nothing, and authoring one would duplicate a free mechanic.** The exception is not "this recipe
survives"; it is "this capability was never a recipe", and a removal sweep that reasons over recipe
files cannot see it.

### 4. Gated is a third state, not a shade of kept

`#91` keeps Mekanism's Energized Smelter craftable **only from rung 3**. ADR-0018's spine is built on
Researchd holding recipe locks, and `docs/research/recipe-locking-mechanisms.md` records that
`ServerEvents.recipes` removal and a research lock are different mechanisms with different failure
modes. ADR-0017's table has a two-valued column — owner, losing blocks — and cannot say *kept, locked
until rung N*, which is what several of its rows actually mean.

### 5. Almost Unified needs survivors to work on

ADR-0017 keeps Almost Unified, restricted to raw materials: *"Ores, ingots, dusts, plates and gems
unify. Recipe types do not."* Unification **rewrites ingredients in recipes that exist**. A pack that
had genuinely removed every stock recipe would have nothing for it to do, and keeping it is a standing
admission that a large body of stock recipes ships. Which body, nobody has said.

### 6. Compiled recipes, which are not data to remove

ADR-0026 found twelve GCyR recipes built in Java —
`gcyr-src/…/data/recipe/MiscRecipes.java` calling `GTRecipeTypes.ASSEMBLER_RECIPES.recipeBuilder(...)`
for the three fuel tanks, three rocket motors, `launch_pad`, `space_station_package`, `space_fabric`,
`casing_atomic`, `seat` and the space-suit template. **Every rocket in the pack is an Assembler
recipe**, and they were stranded — not removed — when ADR-0026 declined GT's recipe type. They are
being re-authored as JSON on the pack's type.

The general point: a jar can express a recipe in a form no removal call reaches, and the pack's
answer is re-authoring rather than removal. The GCyR fork is the pack's to edit, which is why this one
is loud rather than silent.

### 7. The tail, which is not decided at all

The manifest is 111 entries. Across every ADR and every closed ticket, the mods whose recipes are
decided are GregTech, Create, Mekanism, Electro, GCyR, AE2 (terminals), Sophisticated Backpacks (the
Crafting Upgrade), Crafting on a Stick and CraftingTweaks — nine. **Building Gadgets is explicitly
kept** (ADR-0025 leans on it for the refinery), and its recipe is a shaped one. So are Akashic Tome's,
Carry On's, the gravestone's, the elevator's, the backpacks' own.

Under the rule stated at the top these all go, and with them the mods. Under the reading that the
premise only ever meant *the four tech mods*, they stay and are uncraftable, because §"Why wholesale
was never the right word" applies to them too. **Neither reading is a decision anyone has taken**, and
the tail is where the great majority of the pack's surviving recipes actually live.

## What the rule does not reach

**Recipe removal binds recipes.** It does not bind loot tables, villager and wandering-trader trades,
mob drops or structure chests. `#124`'s input-alphabet rule — *the four-ore set plus what Terra's
surface grows* — is stated at the recipe, following ADR-0017's *"every new recipe is checked against
this table"* precedent, so a trader selling a cut material is outside it by construction.

Nothing in `docs/adr/`, `docs/gdd.md` or `CONTEXT.md` takes a position on non-recipe acquisition; the
grep is empty apart from ADR-0016's Sapros loot tables, which are about the opposite problem.
**Recorded here as unsettled, not decided.**

## Considered Options

- **Say nothing and keep relying on the premise.** The status quo, and it has already cost one
  research ticket (`#127`) filed against a question that could not be answered and one ADR consequence
  (ADR-0007's) that was wrong for four ADRs. The failure mode is not that people disagree with the
  premise — it is that they cannot tell which recipes it covers, so they price it as either free or
  infinite depending on which half of the pack they happen to be looking at.
- **Amend ADR-0017 instead.** Rejected on scope. That table is Terra's capability ownership, read
  every time anyone adds a recipe, and it names about twenty blocks. This rule is pack-wide and its
  interesting cases — the 2x2, the QoL tail, GCyR's compiled Java — are not capability disputes at
  all. It is the same argument ADR-0032 used for splitting out of the same table.
- **State the premise unqualified: everything stock goes.** The clean version, and false on delivery.
  Vanilla smelting survives and carries pack content (`#91`); Create's Spout survives by carrying no
  recipe (`#93`); the 2x2 survives because it cannot be removed. An unqualified rule with three known
  violations on the day it is written teaches the next reader to ignore it.
- **Enumerate the surviving recipes rather than the classes.** Rejected as unmaintainable: it is a
  list against 111 jars that goes stale on every version bump, and `#127` is the precedent for what
  happens when a decision rests on an enumeration nobody has done. Classes are checkable; a list is a
  chore.
- **Keep one shaped-recipe executor after all, to give survivors a surface.** This is `#90`'s original
  position and `#34` reversed it. Reopening it would make the tail tractable at the price of the
  pack's central claim — that there is one place you craft by hand. Recorded because if the tail turns
  out to be unaffordable, this is the lever, and it should be pulled deliberately rather than
  discovered.

## Consequences

- **The tail needs a ticket, and it is the expensive one.** Roughly a hundred manifest entries have
  undecided recipe sets, and the default stated here deletes them. Either each kept mod's items get a
  pack-authored route onto the Personal Assembler or an Assembling Machine, or the mod is cut from the
  manifest, or the executor decision above is reopened. This is the enumeration `#127` was mis-scoped
  out of, asked at the right boundary.
- **The 2x2 wants the static check `#97` already specifies, widened.** `#97` asserts no *pack-emitted*
  recipe uses a vanilla shaped or shapeless type. That check cannot see a *surviving stock* one, which
  is the live hazard. Widening it means reading the loaded recipe manager rather than the pack's own
  files, so it is a world-load check and not a `tests/` static assertion — a different check kind, and
  `docs/testing/what-to-check.md` should say which.
  **`#140` named the kind and declined to build it**: `docs/testing/what-to-check.md` now carries the
  *world-load recipe-manager assertion* as a kind, and records that the 2x2's own removal is not one
  of its claims — a recipe dump sees recipes, not surfaces, so it could neither have caught the grid
  working nor catch it coming back. That removal's check is a human on delivery; the widened sweep
  stays `#97`'s.
- **ADR-0017's losing-blocks column gains a third value.** *Gated* is not *removed*, and at least the
  Energized Smelter row means it. Recording it is a one-line edit; leaving it means the table
  continues to read as a binary it is not.
- **`#127` gets a courtesy comment, not a supersede.** Its conclusion — the question is void — still
  holds, because `#124` removed the dimension outright. What no longer holds is the unqualified
  premise its closure cited. Per `docs/agents/domain.md` that is a re-scope rather than a
  contradiction, so no frontmatter entry; the comment should point here.
- **ADR-0007 should carry the amendment `#124` decided and has not received.** `#124` answered *the
  Nether does not exist* and recorded it as an amendment to ADR-0007. As of this ADR,
  `docs/adr/0007-gregtech-worldgen-belongs-to-planets.md` still reads *"the Nether and the End keep
  their dimensions"*, and `kubejs/data/minecraft/world_preset/normal.json:101` still declares
  `minecraft:the_nether`. Noted here because this ADR quotes that cost estimate as its motivating
  failure and the estimate is still standing in the file.
- **Nothing in this ADR is implemented.** ~~There is no recipe removal anywhere in the repo~~ —
  **no longer true as of [`#143`](https://github.com/adamico/planetary-factory/issues/143)**, which
  ships the sweep and its survivor allowlist. See the section below, amended. As written this was a
  rule for work that had not started, which is the cheapest moment to write it and the reason it was
  worth writing then.

## The state on the ground, recorded because it is not what the docs imply

**Amended by [`#143`](https://github.com/adamico/planetary-factory/issues/143): the first two
bullets below are no longer true.** `kubejs/server_scripts/recipes.js` is the default-deny sweep
and `kubejs/server_scripts/recipe_survivors.js` is the allowlist it re-admits by name; the
corpus emits into `kubejs/data/planetaryfactory/recipe/` (`#87`). The rest of the section stands,
and it is left as written because it is this ADR's motivating evidence.

Verified across `kubejs/`, `mod/`, `data/` and `scripts/`:

- **No recipe is removed anywhere in the pack.** `kubejs/server_scripts/recipes.js` is three lines —
  an empty `ServerEvents.recipes` handler with the comment *"the Factorio export script will fill up
  this"*. It is the only `ServerEvents.recipes` call in the repo, and no `.remove(`, `removeByOutput`,
  `removeByInput` or `removeById` call exists in any script.
- **No recipe is emitted anywhere either.** There is no `recipe/` or `recipes/` directory under
  `kubejs/data/` or in the mod. The three datapacks in `kubejs/data/*.zip` are `pack.mcmeta`-only
  filter packs, and all three filter worldgen files.
- Every removal in ADR-0017, ADR-0025, ADR-0026, ADR-0031, ADR-0032, ADR-0033, `#37`, `#91` and `#93`
  is therefore **a decision, not a shipped state**. The docs are consistent with each other and none
  of them is yet true of the jars. That is the pack's recorded failure class (ADR-0017's own
  *"an owner named off this table without checking that the mod can express the recipe"*, three times)
  pointed at the removal column instead of the owner column, and it is worth stating once so that a
  later session does not read a removal row as done.

Two documents also still assert the opposite of `#90`, and both are tracked files a session is
expected to trust:

- `docs/gdd.md` §5: *"The crafting grid, workbenches and portable crafting stay intact"*, and
  *"Policy: follow GTCEu's stock recipe-type assignments, re-authoring only for the pack's own items"*
  — which is the premise this ADR reverses, stated as policy. The same section says nineteen lines
  later that the Assembler covers the components *"which have nowhere to go once the crafting grid is
  removed"*. §5 contradicts itself.
- `docs/factorio-mechanics.md`, *Handcrafting and the crafting queue*: *"The crafting grid stays, so
  early handcrafting is instant rather than queued."* The ledger is the tracked list this pack treats
  as authoritative for what it does about a mechanic, and on this row it is wrong.
