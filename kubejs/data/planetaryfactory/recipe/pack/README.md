Pack-authored recipes: the two ADR-0031 names an exception for.

`scripts/factorio-recipe-convert.py` owns every other subtree here and wipes it on each run. This
one it skips (`FOREIGN_SUBTREES`), because nothing in `data/factorio/recipe.json` can ever produce
these: Factorio has no mining-tool prototype, so there is nothing to extract and nothing to convert.
ADR-0039 states the exception and its reason -- it does not generalise, and a third file here needs
its own decision, not this one's precedent.

Both land on the `assembling` surface, which `recipe_survivors.js` already names, and both carry
`factorio_category: crafting`, which is what makes them hand-craftable in the Personal Assembler at
rung 0 with no machine built yet.

`duration: 10` on both is the corpus's own convention, not a new decision: the converter writes
`energy x 20`, and Factorio's cheapest crafting rows -- `wooden-chest` and `small-electric-pole`,
the two that moved with ADR-0039's `wood` row -- land on exactly 10. A pick costs what a chest
costs, which is the closest thing to a corpus price for a recipe the corpus cannot hold.
