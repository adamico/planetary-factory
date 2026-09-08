# The Create recipe check

`tests/factorio/test_create_recipes.py` is the static half of the pack's Create kinetic line — the
shaft, the cogwheels, the gearboxes, the water wheels and the item-logistics parts, re-authored
onto the pack's own Assembling Machine because ADR-0034's sweep removes Create's own recipes and
all 653 of its grid recipes sit on surfaces no block in this pack executes.

Its check kind, in `docs/testing/what-to-check.md`'s terms, is **cross-file references resolve**,
plus one closure claim no other check can make.

## Why it is not the grid check again

`scripts/powergrid-recipe-convert.py` converts its whole corpus. `scripts/create-recipe-convert.py`
is **closure-driven**: `data/pack/create-substitutions.json` names WANTED ROOTS and the converter
pulls in whatever their recipes need, transitively. Create ships 653 grid recipes and the pack
wants nine roots, which resolve to nine recipes; the rest are mostly decorative palette variants
and converting them would put hundreds of entries in EMI that nothing needs.

So the property to assert is not "the corpus was converted" but **the closure is closed**. A
conversion that quietly stops one hop short still emits valid JSON, still passes a schema, and
reaches the player as a component whose ingredient cannot be made.

## What it asserts

- **The emitted files are the converter's.** It shells out to
  `scripts/create-recipe-convert.py --check`, and to `scripts/create-recipe-extract.py --check`
  when a Create jar is present. Generated output is never hand-edited (ADR-0026). This matters
  more here than elsewhere: the subtree replaced ten hand-written files that had drifted from each
  other on the same substitution, which is the failure the converter exists to end.
- **Every wanted root arrived.** A root that resolves to nothing is a component the player cannot
  craft, and its only symptom is absence from EMI.
- **Every substitution fired.** No emitted ingredient may be a name the table substitutes away. A
  substitution that silently fails leaves the unobtainable original in place, which under a
  default-deny sweep is a recipe that cannot be crafted.
- **Every tag has a source.** `#create:cogwheel` and `#create:belt_connector` are not tags — both
  are plain items, and neither has a tag file in any installed jar. A `{"tag": ...}` naming a tag
  that does not exist matches **nothing**: the recipe loads, shows in EMI and never fires, with no
  error in any log. Four of the ten hand-written files carried exactly that typo, copied from a
  `#minecraft:logs` ingredient. `KNOWN_TAGS` is spelled out rather than scanned from the jars, for
  the reason the grid check spells out `OBTAINABLE`: a vanilla tag is in no mod jar, so a scan
  would reject `#minecraft:logs` and catch nothing useful.
- **Counts are preserved** against Create's own recipe, so a substitution table edit cannot quietly
  re-cost the kinetic line. Re-costing is a decision, not a conversion.
- **No item has two routes at all.** The Personal Assembler's resolver picks a route with no cost
  model (`test_hand_resolver.py` asserts that property globally), so a second `crafting` recipe for
  one output is ambiguity it cannot resolve. Create ships three second routes here — both gearbox
  conversions and the large cogwheel's from-little route. All three are SKIPPED rather than moved
  to the machine: Create ships the conversions so a player can flip a placed gearbox's orientation
  without re-crafting, and in this pack both orientations craft directly from a casing and four
  cogwheels at the same cost, so the pair buys nothing and costs two duplicate EMI entries. The
  `machine_only` table is empty as a result, and kept because the next component added may need it.
- **The recipes fit the machine that accepts them.** The item-input limit is READ from
  `ASSEMBLING_IO` in `machines.js` rather than restated, so the check and the machine cannot drift
  together.
- **The decision tables still decide something.** A `skip`, `machine_only`, `keep` or `KNOWN_TAGS`
  row that nothing reads is a rule a re-extraction left behind, not a spare part.
- **Nothing here re-makes what another subtree emits.** A second route to one item is a
  progression escape, and both would sit in EMI.

## What it cannot prove

That the sweep kept the recipes in a running game, or that the Personal Assembler offers them.
Both are claims about a loaded `RecipeManager` and a menu, which `what-to-check.md` puts on a human
at delivery.

What it does not TRY to prove, and what no check here should: that the parts work once crafted. A
water wheel turning, a gearbox reversing rotation, a chute moving an item -- that is Create's own
functionality, tested by Create. This pack decides what a component COSTS and whether it is
reachable; it does not re-verify the mod it borrows from.

## When to run it

After editing `data/pack/create-substitutions.json`, after re-running either Create script, and
after a Create version bump. Note that `scripts/factorio-recipe-convert.py` and
`scripts/powergrid-recipe-convert.py` share the output tree with this converter and each leaves the
others' subtree alone — `FOREIGN_SUBTREES` now names `create` in both the Factorio converter and
`tests/factorio/test_recipe_convert.py`. Run all three checks after touching any of them.

## Adding a kinetic component

Add one string to `wanted` in `data/pack/create-substitutions.json` and re-run the converter. If it
drags in an ingredient nothing classifies, the converter fails and names it — classify it as a
`keep` or a `substitute` with its reason, or drop the root. That is the whole workflow, and it is
the point: the alternative was a hand-written recipe file per ingredient the component pulls in.
