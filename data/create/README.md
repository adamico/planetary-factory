# The Create recipe corpus

`recipe.json` is every recipe Create ships on the three surfaces **no block in this pack
executes**, extracted from the mod jar by `scripts/create-recipe-extract.py` rather than
transcribed.

It exists for the same reason `data/factorio/` and `data/powergrid/` do: the pack re-authors those
recipes, and re-authoring against a jar nobody has committed is not reproducible. A clean clone has
no `mods/` — the manifest is packwiz and the jars are gitignored (ADR-0024) — so a converter that
read the jar directly would work only on a machine that had already installed the pack.

## What is in it, and what is not

| source recipe type | count | why it is here |
| --- | --- | --- |
| `minecraft:crafting_shaped` | 375 | the vanilla grid went with `#90`/`#34`, and `#140` made the 2x2 inert |
| `minecraft:crafting_shapeless` | 274 | as above |
| `create:mechanical_crafting` | 4 | ADR-0017 cuts Create's Mechanical Crafter by name |

The other ~1,230 recipes the mod ships are **not** extracted: milling, crushing, deploying,
splashing, pressing, cutting, haunting, filling, mixing, compacting, emptying, sequenced assembly,
sandpaper polishing, item application, item copying, toolbox dyeing, `minecraft:stonecutting` and
the four vanilla cooking types. Whether the pack keeps any of those surfaces is a separate question
that no ticket has answered, and a corpus is not the place to answer it.

## The one way this differs from `data/powergrid/`

Power Grid's corpus **is** the converted set — 84 recipes, all of them re-authored, and a row
appearing there that nothing converts is a failure rather than a spare.

This corpus is the opposite: it is the mod's whole grid-craftable surface, and the pack wants about
a dozen items out of it. Most of the remainder are decorative palette variants. So the filter is
not here — `data/pack/create-substitutions.json` names the **wanted roots** and
`scripts/create-recipe-convert.py` walks the transitive closure from them. Filtering at extraction
would put the decision in the wrong file and force a re-extraction every time the wanted set grew.

An unconverted row here is therefore normal, and it is the only corpus in the repo where that is
true.

## The shape of a row

A key is the mod's own recipe file stem, prefixed with its directory (`<dir>__<stem>`) only where
Create reuses a stem across directories — the mod does, and a silent collision would drop a recipe.

**Patterns are already flattened**: a shaped recipe's grid is reduced to an unordered ingredient
list where `amount` is the number of cells that ingredient filled. That is the conversion "a shape
becomes a list", and doing it at extraction rather than in the converter keeps the committed data
in the form the decision is actually about.

`ingredient` is an item id, or a tag with a leading `#`. Where Create's own ingredient is a list of
alternatives, **every** member is named, so an alternative nobody has looked at is still a hard
failure in the converter rather than a silent pass here.

`source_type` is kept because it is not decoration: the converter reads it to decide whether a
recipe becomes Factorio's `crafting` (hand-craftable, so the Personal Assembler plans it) or
`advanced-crafting` (the Assembling Machine only). Create drew that line itself by putting a recipe
on the Mechanical Crafter, and the conversion preserves it rather than inventing a new one.

## Provenance

Extracted from `create-1.21.1-6.0.10.jar` — the version pinned in `mods/create.pw.toml`.

**Re-extract after a Create version bump**, with `scripts/create-recipe-extract.py`.
`tests/factorio/test_create_recipes.py` will not catch a recipe the mod changed upstream on its
own: it checks this file against what the converter emits, and both move together. What it does
catch is an ingredient the new version introduces, because `create-substitutions.json` will not
classify it and an unclassified ingredient in the closure is a hard failure. The check also runs
the extractor's `--check` when a jar is present, so a stale corpus fails rather than shipping.
