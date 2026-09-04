# The Create: Power Grid recipe corpus

`recipe.json` is the 84 recipes Create: Power Grid ships on surfaces **no block in this pack
executes**, extracted from the mod jar rather than transcribed (#172).

It exists for the same reason `data/factorio/` does: the pack re-authors those recipes, and
re-authoring against a jar nobody has committed is not reproducible. A clean clone has no
`mods/` — the manifest is packwiz and the jars are gitignored (ADR-0024) — so a converter that
read the jar directly would work only on a machine that had already installed the pack.

## What is in it, and what is not

| source recipe type | count | why it is here |
| --- | --- | --- |
| `minecraft:crafting_shaped` | 43 | the vanilla grid went with `#90`/`#34`, and `#140` made the 2x2 inert |
| `minecraft:crafting_shapeless` | 28 | as above |
| `create:mechanical_crafting` | 13 | ADR-0017 cuts Create's Mechanical Crafter by name |

The other 28 recipes the mod ships are **not** extracted: `create:sequenced_assembly`,
`deploying`, `cutting`, `mixing`, `pressing`, `item_application`, `minecraft:stonecutting` and
Power Grid's own `boost_recipe` and `magnetization`. Whether the pack keeps any of those surfaces
is a separate question and `#172` did not answer it — so the corpus holds only what that ticket
converted, and a recipe appearing here that nothing converts is a failure rather than a spare.

## The shape of a row

A key is the mod's own recipe file stem, which is unique across the three surfaces. **Patterns are
already flattened**: a shaped recipe's grid is reduced to an unordered ingredient list where
`amount` is the number of cells that ingredient filled. That is the conversion `#172` asked for —
"a shape becomes a list" — and doing it at extraction rather than in the converter keeps the
committed data in the form the decision is actually about.

`ingredient` is an item id, or a tag with a leading `#`. `source_type` is kept because it is not
decoration: `scripts/powergrid-recipe-convert.py` reads it to decide whether a recipe becomes
Factorio's `crafting` (hand-craftable, so the Personal Assembler plans it) or `advanced-crafting`
(the Assembling Machine only). Power Grid drew that line itself by putting a recipe on the
Mechanical Crafter, and the conversion preserves it rather than inventing a new one.

## One block that looks missing and is not

`powergrid:winding` — which `#172` lists as a step of the generation chain — has **no recipe in
the jar at all**, on any surface. It is a block with no item form (`block.powergrid.winding` in
the mod's lang, and no `item.powergrid.winding`), placed by the `copper_coil` item, which this
corpus does convert. So the generation multiblock is fully craftable without a `winding` row, and
its absence here is not a gap to fill.

## Provenance

Extracted from `powergrid-mc1.21.1-0.6.1.jar` — the version pinned in `mods/power-grid.pw.toml` —
by reading `data/powergrid/recipe/**/*.json` out of the jar and keeping the three types above.

**Re-extract after a Power Grid version bump.** `tests/factorio/test_grid_recipes.py` will not
catch a recipe the mod changed upstream: it checks this file against what the converter emits, and
both move together. What it does catch is an ingredient the new version introduces, because
`data/pack/grid-substitutions.json` will not classify it and an unclassified ingredient is a hard
failure.
