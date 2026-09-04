# The grid recipe check

`tests/factorio/test_grid_recipes.py` is the static half of `#172` — Create: Power Grid's recipes
re-authored onto the pack's own Assembling Machine, because ADR-0034's sweep removes the mod's own
and 84 of its 112 recipes sit on surfaces no block in this pack executes.

Its check kind, in `docs/testing/what-to-check.md`'s terms, is **cross-file references resolve**
plus one reachability claim the other checks cannot make.

## What it asserts

- **The emitted files are the converter's.** It shells out to
  `scripts/powergrid-recipe-convert.py --check`. Generated output is never hand-edited (ADR-0026),
  and a hand-edited file passes every other assertion here.
- **Counts are preserved.** The total ingredient units in an emitted recipe equal the total in
  Power Grid's own. `#172`'s conversion is "a shape becomes a list": a substitution is one-for-one
  and only ever merges, so a changed total means the substitution table quietly re-costed the grid
  line.
- **Every ingredient bottoms out.** An ingredient is obtainable only if a pack recipe outputs it,
  or `OBTAINABLE` says how the world hands it over, or `KNOWN_BLOCKED` names the ticket that will.
  **This is the assertion the ticket was filed for.** Under a default-deny sweep a vanilla item is
  not obtainable because it is vanilla — the recipe that used to make it is gone — and an
  unobtainable ingredient reaches the player as an EMI entry that cannot be crafted, with nothing
  in any log.
- **One hand recipe per item, and no cycles**, over the union the Personal Assembler actually
  loads. `RuntimeHandRecipes` walks the loaded recipe manager and keeps everything stamped
  `factorio_category: crafting`, so its graph is the Factorio corpus's hand recipes *and* the grid
  ones together. `tests/factorio/test_hand_resolver.py` asserts the same two properties but reads
  only `data/factorio/recipe.json`, so **nothing else sees the union** — and `RecipeGraph` records
  that a second recipe for an item means the first registered wins, "an arbitrary answer, not a
  stable one".
- **No block has two routes at two costs** — one from the Factorio converter and one from this
  one. That is what `grid-substitutions.json`'s `skip` table exists to prevent, and four of its
  rows are `#148`'s recipes.
- The recipes fit the Assembling Machine's five item-input envelope, and their categories route to
  it in `category-map.json`.

## What it cannot prove

- **That the sweep kept them in a running game.** A KubeJS filter is evaluated against a loaded
  `RecipeManager` that exists only in the JVM. `docs/testing/what-to-check.md` puts this on the
  *world-load recipe-manager assertion*, which is still unbuilt, so today it is a human on
  delivery.
- **That the Personal Assembler offers the hand ones.** Reachability on a menu, which a recipe dump
  cannot see either. Also a human on delivery.
- **That a Power Grid version bump did not change a recipe.** The corpus and the emitted files move
  together, so re-extract after a bump. What it *does* catch is a new ingredient, because
  `grid-substitutions.json` will not classify it and an unclassified ingredient is a hard failure.

## When to run it

After editing `data/pack/grid-substitutions.json`, `scripts/powergrid-recipe-convert.py`, or after
re-extracting `data/powergrid/recipe.json` — and after any change to the Factorio converter, since
both write into `kubejs/data/planetaryfactory/recipe/` and each is taught to leave the other's
subtree alone.

```
tests/factorio/test_grid_recipes.py
```
