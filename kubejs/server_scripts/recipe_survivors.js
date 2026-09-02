// priority: 10
// The priority header is load order: KubeJS sorts scripts by it, descending, and does NOT load
// them alphabetically (ADR-0022). This has to load before recipes.js reads RECIPE_SURVIVORS,
// and it is `var` for the same reason -- a `const` is not visible to the other file.
// The survivor allowlist for ADR-0034's default-deny sweep (#143).
//
// The rule: a stock recipe ships only if a decision names it AND names the surface it is crafted
// on. This file is the "names it" half -- everything not matched here is removed by
// `recipes.js`, including the ~100 manifest entries nobody has decided yet. Deciding which of
// those earn a survivor is the follow-on ticket, not a prerequisite: deny is the default.
//
// An entry is a surface, not a recipe. `surface` is the key in `data/pack/category-map.json`'s
// `machines` table that crafts it, `type` is that machine's registered recipe type, and `why`
// names the decision. The pack's own namespace is not repeated per row -- `recipes.js` applies
// it once, because every survivor is by definition a recipe the pack authored.
// `tests/factorio/test_recipe_sweep.py` asserts the two agree -- every machine with a registered
// `recipe_type` has an entry here, and every type the converter emits is covered by one -- so a
// machine landing later (#107's Chemical Plant, #135's Centrifuge) fails the check until its
// survivor is written, rather than having its recipes swept in silence.
//
// Four of ADR-0034's seven exception classes need no entry, and that is not an oversight:
//
//   - `native_mechanic` (#93): Create's Spout and Item Drain key on `IFluidHandlerItem`, so the
//     eighteen barrel fill/empty rows were never recipes. Nothing to keep, nothing to remove,
//     and authoring one would duplicate a free mechanic.
//   - The 2x2 inventory grid: not recipe-removable, and #140's, not this sweep's.
//   - GCyR's twelve compiled Java recipes (ADR-0026): expressed in a form no removal call
//     reaches. The pack's answer there is re-authoring, not removal.
//   - Gated-not-removed (ADR-0034 §4): the Energized Smelter was its only live case and ADR-0035
//     takes Mekanism out of the pack.
var RECIPE_SURVIVORS = [
  {
    surface: 'assembling',
    type: 'gtceu:assembling',
    why: "ADR-0026's Assembling Machine 1/2/3, one recipe type across the three tiers. ADR-0031: the corpus authors every recipe it contains, and #87 emits them."
  },
  {
    surface: 'oil_refinery',
    type: 'gtceu:oil_refinery',
    why: "ADR-0025's Oil Refinery, the only machine in the pack that emits three fluids at once. Basic and advanced oil processing; #87 emits them, ADR-0031 says the corpus authors them."
  },
  {
    surface: 'chemical_plant',
    type: 'gtceu:chemical_plant',
    why: "ADR-0025's Chemical Plant -- cracking, lubricant, plastic, sulfur, sulfuric acid, solid fuel, battery and explosives. ADR-0025 moved sulfur onto petroleum gas and nothing else, so this surface is the pack's only route to it."
  },
  {
    surface: 'smelting',
    type: 'minecraft:smelting',
    why: "#91 puts the smelting categories on vanilla `minecraft:smelting`. ADR-0034 §2: the TYPE survives and carries pack content -- vanilla smelting is curated, not deleted. What is curated is the pack's own rows; #91's two named cuts (log to charcoal, ore to ingot) plus every stock smelt nobody has decided go by default-deny, not by a per-recipe decision, and re-admitting any of them is the follow-on ticket's call."
  }
]
