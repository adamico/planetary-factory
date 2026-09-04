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
| Each tier has a model, texture and lang key | The missing-texture checkerboard and a raw translation key. Neither is an error. The two picks are dressed from different places, so the texture is resolved per namespace: ours against the file, a mod's against the jar the pack ships, vanilla's against nothing. |
| The Steel Pick's texture is current against the installed GTCEu jar | It is generated (below), and generated output is never hand-edited. A GTCEu update that redrew its tool art would otherwise leave the pack showing the old one silently. |
| Both picks are in `c:tools/wrench` and `gtceu:crafting_tools/wrench` | The Pick stops dismantling machines, and the pack has no other wrench to reach for. |
| The block tag `EngineersPick` names by id exists and is non-empty | An absent tag is an empty one: every block falls back to vanilla hardness and the flat mining time is gone with nothing logged. |

The tier list is parsed out of `PickTier.java`, so a third tier fails this check rather than
shipping without assets or a recipe.

## Where the two textures come from

The **Iron Pick** wears `minecraft:item/iron_pickaxe` directly. Vanilla's sprite needs no tint and
no copy, and the pack is a Factorio pack built in Minecraft — ADR-0039 keeps the opening gesture
recognisable, and nothing reads as "pickaxe" faster than the one the player already knows.

The **Steel Pick** wears GTCEu's Damascus Steel pickaxe, flattened into our namespace by
`scripts/build-pick-textures.py`. It cannot simply reference GT's art: a GT tool sprite is three
greyscale layers — handle, head, overlay — that only become a material when GregTech's item-colour
handler tints them, and that handler never sees an item which is not a GT tool. Referencing them
would render an uncoloured grey pickaxe. So the script bakes the tint, reading Damascus Steel's own
value from what GTCEu registers (`damascus_steel .color(7237230)`, i.e. `0x6E6E6E`) and the layers
from the installed jar. Re-run it after a GTCEu update; the check above fails if it is not re-run.

## What it cannot prove

That the Pick mines a given block class, that GregTech accepts it as a wrench, that Create does, and
that the flat second an ore feels right in the hand (2.0s was tried first, and did not). The first three are a world load — ADR-0039's
GameTests, which #165 names and which this repo has no GameTest harness for yet. The last is a human
on delivery, in the Terra Slice run.

The arithmetic half — that Factorio's stated seconds survive Minecraft's break-time formula — is
`MiningSpeedTest`, under `./gradlew :planetaryfactory_core:test`, with no Minecraft in it.
