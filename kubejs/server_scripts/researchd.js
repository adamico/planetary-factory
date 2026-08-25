
ResearchdEvents.registerResearchPacks(event => {
  event.create('planetary_factory:automation_science_pack')
    .literalName('Automation Science Pack')
    .color(200, 60, 60)
    .sortingValue(100);
  event.create('planetary_factory:logistics_science_pack')
    .literalName('Logistics Science Pack')
    .color(60, 200, 60)
    .sortingValue(101);
});

ServerEvents.recipes(event => {
  event.shapeless(
    Item.of('researchd:research_pack[researchd:research_pack="planetary_factory:automation_science_pack"]'),
    [
      'create:copper_sheet',
      'create:cogwheel'
    ]
  ).id('planetary_factory:automation_science_pack');
  event.shapeless(
    Item.of('researchd:research_pack[researchd:research_pack="planetary_factory:logistics_science_pack"]'),
    [
      'create:smart_chute',
      'create:brass_funnel'
    ]
  ).id('planetary_factory:logistics_science_pack');
});

// consumeItem should be replaced by Java custom ResearchMethod to detect the presence of the items in the player inventory
// instead of consuming them. Another option could be to refund the consumed items with a quest reward

ResearchdEvents.registerResearches(event => {
  event.create('planetary_factory:steam_power')
    .icon('gtceu:lp_steam_solid_boiler')
    .literalName('Steam Power')
    .consumeItem('mekanism:ingot_bronze', 32)
    .effect(ResearchEffectHelper.unlockRecipe('gtceu:shaped/lp_steam_solid_boiler'));
  event.create('planetary_factory:automation_science_pack')
    .iconPack('planetary_factory:automation_science_pack')
    .literalName('Automation Science Pack')
    .parents('planetary_factory:steam_power')
    .consumeItem('researchd:research_lab', 1)
    .effect(ResearchEffectHelper.unlockRecipe('planetary_factory:automation_science_pack'));
  event.create('planetary_factory:logistics')
    .icon('create:belt_connector')
    .literalName('Logistics')
    .parents('planetary_factory:automation_science_pack')
    .consumePack('planetary_factory:automation_science_pack', 20, 10)
    .effect(ResearchEffectHelper.unlockRecipe('create:crafting/kinetics/belt_connector'));
  event.create('planetary_factory:logistics_science_pack')
    .iconPack('planetary_factory:logistics_science_pack')
    .literalName('Logistics Science Pack')
    .parents('planetary_factory:automation_science_pack')
    .consumePack('planetary_factory:automation_science_pack', 75, 5)
    .effect(ResearchEffectHelper.unlockRecipe('planetary_factory:logistics_science_pack'));
});
