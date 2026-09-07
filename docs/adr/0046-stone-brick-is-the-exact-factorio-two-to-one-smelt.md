---
status: provisional
supersedes: []
---

# Stone brick is the exact Factorio 2:1 smelt, not Minecraft's 1:1 shape

`#87` converted the extracted recipes to pack JSON and hit two smelting recipes that vanilla's
furnace cannot express: `steel-plate` is 5 iron plates to 1, `stone-brick` is 2 stone to 1, and
`SmeltingRecipe` holds a bare `Ingredient` with no count. It resolved the two **as a pair, and
differently** — recorded in `data/pack/recipe-overrides.json` and in `docs/factorio-mechanics.md`:

- `steel-plate` earns a count-bearing `planetaryfactory:smelting` recipe type on the three furnace
  tiers, built by `#155`, read alongside vanilla smelting.
- `stone-brick` takes the vanilla 1:1 shape instead — `stone` is `minecraft:cobblestone`,
  `stone-brick` is `minecraft:stone`, and smelting one to the other is the same move at a
  different ratio. A knowing fidelity loss, logged as `recipe-overrides.json`'s one entry.

This ADR reverses the second half. It **amends** that split; `#155` is unchanged.

## Why the split no longer holds

The split rationed a cost. A count-bearing furnace recipe type is a real thing to design,
register and check, and `#87` judged it worth paying for the only surviving alloy on Terra
(`#72`) and not worth it for a building block.

**But `#155` pays that cost regardless.** The type is on `#155`'s critical path for `steel-plate`
whether or not `stone-brick` uses it. Once it exists, a second recipe on it is one more generated
JSON file — no new machine, no new type, no new check kind. The thing the split was protecting
against is already sunk by the time `stone-brick` could ride it.

And the 1:1 shape was never free either. It forced `stone-brick` onto `minecraft:stone` — the
*smelted* block — because that is the only target for which "smelt cobblestone to it" is a real
vanilla recipe. Factorio's stone brick is a **building material**: walls, floors, the furnaces
themselves. Minecraft's building material of that name is `minecraft:stone_bricks`. The 1:1 shape
bought a working recipe by putting the item on the wrong block.

## The decision

**`stone-brick` is the corpus recipe verbatim: 2 `stone` to 1 `stone-brick`, category
`smelting`, on the count-bearing `planetaryfactory:smelting` type (`#155`), read on all three
furnace tiers.** It is `steel-plate`'s twin, not its counterexample.

**`stone-brick` maps to `minecraft:stone_bricks`.** With the real ratio restored there is no
reason to hold it on the smelted block. `stone` stays `minecraft:cobblestone` (ADR-0045, the
item-map row).

**`data/pack/recipe-overrides.json` loses its only entry and ships empty.** The file and its
mechanism stay: ADR-0031's converter reads it, `tests/factorio/test_recipe_convert.py` asserts
its invariants, and the next genuine departure from Nauvis has somewhere to be written down. An
empty register is not a dead one — it is the honest state when nothing today overrules the corpus.

## What this costs

**No `stone-brick` craft until `#155` lands.** In the interim the converter reports it as a
skip — "no vanilla shape", exactly as it already reports `steel-plate` — instead of emitting a
1:1 recipe. A rung-0 building material with no recipe for a while is acceptable: the pack is
pre-play (ADR-0042), stone brick gates nothing, and `#155` is already required for `steel-plate`
on the same rung. The alternative is shipping a recipe we have decided is wrong.

**`docs/factorio-mechanics.md` and `docs/testing/recipe-conversion-check.md` change.** The
smelting-machine sub-rule that reads "`stone-brick` deliberately does NOT use it" flips, and the
conversion check's "worth reading as a pair" pair becomes two recipes waiting on one ticket
rather than a contrast.

**ADR-0041 loses an incidental clause.** Its aside that "the `stone-brick` chain — smelt cobble
to stone at 1:1 — is untouched" was true only under the shape this ADR removes; it is deleted,
not rewritten, because 0041's subject is ore-block amounts and not this ratio.

## How this is checked

- **The override is gone.** `test_recipe_convert.py` no longer sees a `stone-brick` entry, so it
  no longer asserts a `reason` for one; the converter's `--check` confirms `stone-brick` is a
  reported skip and not an emit, for as long as `#155` is open.
- **After `#155`.** `stone-brick` emits as an ordinary GTRecipe-shaped smelting recipe against
  the count-bearing type, covered by the same corpus assertions as every other converted recipe.
  Whether the recipe *shape* loads is one world load (ADR-0026), which `#155` owns.
