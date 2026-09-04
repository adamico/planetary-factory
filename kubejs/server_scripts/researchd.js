// priority: 0
// The pack's research tree. Topology, names and pack costs come from Factorio's extracted
// technology tree (data/factorio/technology.json); this file supplies only what Factorio
// cannot know -- which Minecraft item is the icon, which recipe a research unlocks, and
// which of our bodies gates it. See ADR-0022 and factorio_tech_dsl.js.
//
// A technology absent from this file is absent from the pack, and the DSL logs the ones
// still undeclared on every reload.
//
// The `priority: 0` header above is load order: this file must load AFTER
// factorio_tech_data.js (20) and factorio_tech_dsl.js (10), because fromFactorio() has to
// exist before these calls run. KubeJS does not load scripts alphabetically.

ResearchdEvents.registerResearchPacks(event => {
  event.create('planetary_factory:automation_science_pack')
    .literalName('Automation Science Pack')
    .color(200, 60, 60)
    .sortingValue(100);
  event.create('planetary_factory:logistic_science_pack')
    .literalName('Logistic Science Pack')
    .color(60, 200, 60)
    .sortingValue(101);
  event.create('planetary_factory:chemical_science_pack')
    .literalName('Chemical Science Pack')
    .color(60, 60, 200)
    .sortingValue(102);

  // when using the packs in recipes
  // a data component is needed:
  // Item.of('researchd:research_pack[researchd:research_pack="planetary_factory:automation_science_pack"]')
});

// Steel axe, the pack's first declared technology and ADR-0039's second tier.
//
// Factorio's row is carried exactly where it can be: it is one of the 33 trigger technologies, so
// it costs no science packs at all, and its prerequisite `steel-processing` is still undeclared --
// the DSL resolves through it rather than orphaning this one.
//
// TWO DELIBERATE DIVERGENCES, both ADR-0039's:
//
//   - The trigger. Factorio fires this on CRAFTING 50 steel plates, and Researchd has no
//     craft-triggered method -- its four are consumeItem, consumePack, checkItemPresence and the
//     combinators. `has` is checkItemPresence, which holds the plates rather than eating them, and
//     that is the closer of the two: Factorio's trigger charges nothing. A real craft trigger is
//     mechanism, belongs in `planetaryfactory_core` under ADR-0015, and would serve all seven of
//     #138's trigger technologies rather than this one alone.
//
//   - The effect. Factorio's is `character-mining-speed +1`; here it unlocks the Engineer's Steel
//     Pick recipe and the speed rides on the item. The outcome is the same -- mining doubles, 2.0s
//     to 1.0s -- and this is the first time the pack overrides an extracted effect rather than
//     supplying one, which is why it is written down: a reader diffing this file against
//     `data/factorio/technology.json` would otherwise read it as a bug.
//
// The unlock id is `pack/`, not `assembling/`: the recipe is hand-authored (ADR-0031's exception)
// and lives in the one subtree the converter does not own. `tests/factorio/test_research_unlocks.py`
// asserts this id is a recipe the pack emits, which is the coupling that makes the divergence safe.
fromFactorio('steel-axe', {
  icon: 'planetaryfactory:engineers_steel_pick',
  has: ['gtceu:steel_plate', 50],
  unlocks: ['planetaryfactory:pack/engineers_steel_pick']
});
