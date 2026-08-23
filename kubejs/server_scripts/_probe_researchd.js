// THROWAWAY PROBE — issue #45 (go/no-go: does Researchd replace the whole science system?).
// Delete once #45 resolves. Registers one pack and three researches whose only job is to
// answer facts 2, 3 and 4 of that ticket.

ResearchdEvents.registerResearchPacks((event) => {
  event
    .create('pf_probe:probe_pack')
    .literalName('Probe Pack')
    .literalDescription('Throwaway pack for issue #45. Delete me.')
    .color(200, 60, 60)
    .sortingValue(0)
})

ResearchdEvents.registerResearches((event) => {
  // H1 vs H2 discriminator (see #45 thread). Same shape as create_lock but a vanilla
  // recipe, declared FIRST -- ahead of vanilla_lock.
  //   cake blocked + golden_apple still open  => golden_apple's id is the problem (H1)
  //   cake open    + golden_apple now blocked => the first-declared research is dropped (H2)
  //   both blocked                            => something about golden_apple alone (H1)
  event
    .create('pf_probe:cake_lock')
    .icon('minecraft:cake')
    .literalName('Probe: cake lock')
    .method(ResearchMethodHelper.consumeItem('minecraft:dirt', 1))
    .effect(ResearchEffectHelper.unlockRecipe('minecraft:cake'))

  // Fact 4, vanilla reach: RecipeManagerMixin covers the crafting grid directly.
  // Expected: golden_apple is uncraftable and hidden in JEI until this completes.
  event
    .create('pf_probe:vanilla_lock')
    .icon('minecraft:golden_apple')
    .literalName('Probe: vanilla lock')
    .method(ResearchMethodHelper.consumeItem('minecraft:dirt', 1))
    .effect(ResearchEffectHelper.unlockRecipe('minecraft:golden_apple'))

  // Fact 2, the shipped-mixin baseline: Create goes through RecipeTrieFinderMixin.
  // Expected: a Mechanical Press refuses to make brass ingots until this completes.
  // If this fails, the finder-mixin pattern does not work in this pack and GT is moot.
  event
    .create('pf_probe:create_lock')
    .icon('create:brass_ingot')
    .literalName('Probe: Create lock')
    .method(ResearchMethodHelper.consumeItem('minecraft:dirt', 1))
    .effect(ResearchEffectHelper.unlockRecipe('create:pressing/brass_ingot'))

  // Fact 1, piped pack input: the only research that needs the Lab fed.
  // Lab part blocks expose Capabilities.ItemHandler.BLOCK, so point any pipe at one.
  event
    .create('pf_probe:pack_feed')
    .icon('minecraft:diamond')
    .literalName('Probe: piped pack feed')
    .parents('pf_probe:vanilla_lock')
    .method(ResearchMethodHelper.consumePack('pf_probe:probe_pack', 10, 20))
    .effect(ResearchEffectHelper.unlockRecipe('minecraft:diamond_block'))
})
