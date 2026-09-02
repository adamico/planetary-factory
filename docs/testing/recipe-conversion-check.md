# The recipe conversion, and the two checks on it

`scripts/factorio-recipe-convert.py` turns `data/factorio/recipe.json` into GregTech recipe JSON
under `kubejs/data/planetaryfactory/recipe/`. ADR-0026 is the decision; #87 is the build.

## What decides what

Nothing is decided in the script. Five committed files hold the judgements:

| File | Answers |
| --- | --- |
| `data/factorio/recipe.json` | the corpus — 163 Nauvis pre-launch recipes, extracted (#72) |
| `data/pack/category-map.json` | which pack machine crafts a Factorio *category*, and which are `!`-routed |
| `data/pack/subgroup-owner.json` | which process crafts a *recipe*, per shelf and per recipe (#88) |
| `data/pack/item-map.json` | Factorio name → pack item, tag or fluid |
| `data/pack/recipe-overrides.json` | every knowing departure from Factorio, with its reason (ADR-0031) |

So a decision lands as a diff to a design document, never as a diff to the converter.

## The conversion rule

Nothing is scaled (#126, which rewrote ADR-0025's table). Item counts transfer 1:1, one Factorio
fluid unit is one millibucket, and `energy_required` seconds become ticks at ×20. `crafting_speed`
and `EUt` belong to the machine at registration (ADR-0029), so neither appears in a recipe.

Two shapes come out: GregTech's `GTRecipe` — read off `GTRecipeSerializer`'s codec in GTCEu 7.0.2,
where `type` and `duration` are the only required fields and every capability map is optional — and
vanilla's furnace recipe for the 1:1 smelts (#91).

Factorio's source category rides on the emitted recipe, in GregTech's `data` compound, because
`category-map.json` collapses three crafting categories into one machine and the Personal
Assembler needs the distinction back — it is a filtered view of the Assembling Machine's recipes,
not a machine of its own (#125).

An item-map row may carry `components`, and the converter emits a `neoforge:components`
ingredient for it. The science packs are the case: they are Researchd research packs, so the
player holds one `researchd:research_pack` item told apart by a data component, not four items.
On the OUTPUT side that leans on GregTech resolving a `SizedIngredient` back into stacks, which
is one more thing the world load has to confirm.

## What stops a recipe being emitted

In the order the converter checks: a `!`-routed category; a `not_emitted` shelf; a
`native_mechanic` shelf (in scope, supported by a mod mechanic that needs no recipe — the
eighteen barrel recipes, #93); a process whose machine is not registered yet (the converter names
the ticket); an `undecided` item-map row (the row names what decides it); an override that says
`skip`.

One more, found while building: **`smelting` has no vanilla shape above 1:1.** Vanilla's
`SmeltingRecipe` holds a bare `Ingredient` with no count field, while its result is an `ItemStack`
that has one — so 1:n emits and m:n cannot be written at all. Factorio's `steel-plate` is 5 iron
plates and `stone-brick` is 2 stone. Both were reported here until #87 resolved them differently,
and the split is worth reading as a pair:

- **`stone-brick` takes Minecraft's shape.** 1 cobblestone to 1 stone is the same move at a
  different ratio, so `stone` is `minecraft:cobblestone` (the mined rock) and `stone-brick` is
  `minecraft:stone`. A knowing fidelity loss, recorded in `recipe-overrides.json` — which is what
  that file is for.
- **`steel-plate` is worth paying for.** It is the only surviving alloy on Terra (#72), so it gets
  a count-bearing `planetaryfactory:smelting` recipe type on the pack's three furnaces (#155),
  read alongside vanilla smelting. Until that lands it stays a reported skip.

A Factorio name with **no item-map row at all** is none of those. It is a hard failure (#72): a
name nobody has looked at must never be quietly skipped.

## The two checks

**`tests/factorio/test_recipe_convert.py`** — static, no game. The item map covers the corpus and
nothing else; every row is a target or a recorded reason; every target names a namespace the pack
ships and every first-party target is registered in `kubejs/startup_scripts/` (items and blocks
both) or carries `blocked_by`, the ticket that will register it; every `undecided` row names the
ticket that decides it; every override has
a `reason` and names a real recipe; every emitted recipe resolves through the map and carries a
registered recipe type; and the emitted files are exactly what the converter emits today, because
generated output is never hand-edited.

**One world load, with the generated recipes in place** — a human. The static check cannot prove
the recipe *shape*: the codec is Java, GregTech's generated material ids exist only in a loaded
registry, and a wrong shape NPEs at datapack load rather than reporting anything readable. That
failure is exactly what ADR-0026 was written about. Load a world and confirm **0 failed recipes**
in the log, and that the emitted recipes appear in the Assembling Machine's JEI page.

That second check cannot be run honestly until the remaining machines are registered: a partial
emit reports zero failures for recipes that were never written. #107 has landed; #135's Centrifuge
and the Rocket Silo (#41) have not.

### Two rules the map earns by having been wrong

**`undecided` must name a ticket.** #87 found 40 rows — a quarter of the map — sitting `undecided`
with a note pointing at prose rather than at an owner. Eight of them cited ADR-0030 as the thing
that *might* decide them, and ADR-0030 was already accepted and had decided them. A status that
means "someone will decide this" needs to say who.

**`blocked_by` is the narrow escape for a decided row whose item does not exist yet.** KubeJS
cannot register a furnace with a fuel slot or a chunk-charting block, so those belong to
`planetaryfactory_core` and arrive with their ticket. The field names that ticket, and the check
fails once the item *is* registered — so the map cannot keep pointing at a ticket that closed.

## Re-running it

    scripts/factorio-recipe-convert.py            # rewrite the emitted recipes
    scripts/factorio-recipe-convert.py --check    # fail if what is on disk is stale
    tests/factorio/test_recipe_convert.py         # the static check, which runs --check too
