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
