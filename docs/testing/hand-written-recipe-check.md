# The hand-written recipe check

`tests/factorio/test_pack_recipes.py`. A **cross-file static check** in
[`what-to-check.md`](what-to-check.md)'s terms: no game launch, no corpus, and nothing about how the
Engineer's Pick feels to swing.

## Why this subtree needs a check of its own

Every other recipe under `kubejs/data/planetaryfactory/recipe/` is generated and is checked against
the thing that generated it — the Factorio corpus via
[`recipe-conversion-check.md`](recipe-conversion-check.md), Power Grid's via
[`grid-recipe-check.md`](grid-recipe-check.md). `recipe/pack/` is neither. It is ADR-0031's single
stated exception, taken by ADR-0039: the corpus authors every recipe it contains, Factorio has no
mining-tool prototype, and so the two Engineer's Pick recipes cannot be extracted, converted or
regenerated. They are written by hand, and without this file nothing checks them at all.

Both recipes land on the `assembling` surface, which `recipe_survivors.js` already names, and both
carry `factorio_category: crafting`, which is what makes them hand-craftable in the Personal
Assembler at rung 0 with no machine built yet.

`duration: 10` on both is the corpus's own convention rather than a new decision: the converter
writes `energy x 20`, and Factorio's cheapest crafting rows — `wooden-chest` and
`small-electric-pole`, the two that moved with ADR-0039's `wood` row — land on exactly 10. A pick
costs what a chest costs, which is the closest thing to a corpus price for a recipe the corpus
cannot hold.

**Nothing may document that subtree in place.** KubeJS validates every file name under `kubejs/`
and rejects an uppercase letter outright — `Invalid file name: Uppercase 'R' in
kubejs/data/planetaryfactory/recipe/pack/README.md`, logged as an ERROR that stops a world from
loading. A README beside the recipes is not an option, which is why this page carries what would
otherwise sit next to them.

## What it asserts, and what each failure would look like in a game

| Assertion | The silent failure it catches |
| --- | --- |
| Both converters and `test_recipe_convert.py` still list `pack` as foreign | A converter run wipes the subtree. The sweep leaves no stock pickaxe behind it, so the pack returns to #165's opening state: nothing can be mined at all. |
| Each recipe's type is one `recipe_survivors.js` admits | ADR-0034's sweep removes it on load, with no error and no log line. |
| Each carries `factorio_category: crafting` | `RuntimeHandRecipes` is a predicate on exactly that field, so the recipe survives but the Personal Assembler will not plan it — and rung 0 has no machine to craft it in either. |
| The subtree is exactly the registered tiers | The exception is narrow on purpose. A third file here is a decision ADR-0039 did not make. |
| The steel recipe consumes the iron pick | ADR-0039 states it in one line, and nothing else in the repo would notice both tiers being holdable at once. |
| Each tier has a model, texture and lang key | The missing-texture checkerboard and a raw translation key. Neither is an error. |
| Both picks are in `c:tools/wrench` and `gtceu:crafting_tools/wrench` | The Pick stops dismantling machines, and the pack has no other wrench to reach for. |
| The block tag `EngineersPick` names by id exists and is non-empty | An absent tag is an empty one: every block falls back to vanilla hardness and Factorio's flat 2.0s is gone with nothing logged. |

The tier list is parsed out of `PickTier.java`, so a third tier fails this check rather than
shipping without assets or a recipe.

## What it cannot prove

That the Pick mines a given block class, that GregTech accepts it as a wrench, that Create does, and
that two seconds feels like Factorio's two seconds. The first three are a world load — ADR-0039's
GameTests, which #165 names and which this repo has no GameTest harness for yet. The last is a human
on delivery, in the Terra Slice run.

The arithmetic half — that Factorio's stated seconds survive Minecraft's break-time formula — is
`MiningSpeedTest`, under `./gradlew :planetaryfactory_core:test`, with no Minecraft in it.
