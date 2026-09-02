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

## What stops a recipe being emitted

In the order the converter checks: a `!`-routed category; a `not_emitted` shelf; a
`native_mechanic` shelf (in scope, supported by a mod mechanic that needs no recipe — the
eighteen barrel recipes, #93); a process whose machine is not registered yet (the converter names
the ticket); an `undecided` item-map row (the row names what decides it); an override that says
`skip`.

One more, found while building: **`smelting` has no vanilla shape above 1:1.** A vanilla furnace
consumes exactly one item, and Factorio's `steel-plate` is 5 iron plates while `stone-brick` is 2
stone. The two 1:1 smelts emit as `minecraft:smelting`; the other two are reported and wait on a
decision — a departure recorded in the overrides file, or a machine that is not the furnace.

A Factorio name with **no item-map row at all** is none of those. It is a hard failure (#72): a
name nobody has looked at must never be quietly skipped.

## The two checks

**`tests/factorio/test_recipe_convert.py`** — static, no game. The item map covers the corpus and
nothing else; every row is a target or a recorded reason; every target names a namespace the pack
ships and every first-party target is registered in `kubejs/startup_scripts/`; every override has
a `reason` and names a real recipe; every emitted recipe resolves through the map and carries a
registered recipe type; and the emitted files are exactly what the converter emits today, because
generated output is never hand-edited.

**One world load, with the generated recipes in place** — a human. The static check cannot prove
the recipe *shape*: the codec is Java, GregTech's generated material ids exist only in a loaded
registry, and a wrong shape NPEs at datapack load rather than reporting anything readable. That
failure is exactly what ADR-0026 was written about. Load a world and confirm **0 failed recipes**
in the log, and that the emitted recipes appear in the Assembling Machine's JEI page.

That second check cannot be run honestly until #107 and #135 register the remaining machines: a
partial emit reports zero failures for recipes that were never written.

## Re-running it

    scripts/factorio-recipe-convert.py            # rewrite the emitted recipes
    scripts/factorio-recipe-convert.py --check    # fail if what is on disk is stale
    tests/factorio/test_recipe_convert.py         # the static check, which runs --check too
