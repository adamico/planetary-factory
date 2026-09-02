StartupEvents.registry('item', event => {
  // The circuit ladder, and plastic.
  //
  // First-party by ADR-0031's borrow/author rule: borrow an existing item unless the row sits on
  // a rung boundary or a mod's competing line would give a parallel escape. The circuits carry
  // progression and #62 removed GregTech's and Mekanism's competing lines; plastic gates rung 2
  // (ADR-0025). `copper-cable` is the counter-example and borrows
  // (`electroenergetics:copper_wire`) -- see `data/pack/item-map.json`.
  //
  // THE SCIENCE PACKS ARE NOT HERE. They are Researchd research packs, not plain items:
  // `kubejs/server_scripts/researchd.js` declares them with `registerResearchPacks` under
  // `planetary_factory:` (an underscore, and not this pack's item namespace), and they are held
  // as `researchd:research_pack` carrying a `researchd:research_pack` data component. Registering
  // an item of the same name here would have shipped a second, inert pack the Lab cannot read.
  //
  // EVERY TEXTURE HERE IS A PLACEHOLDER -- one borrowed GregTech icon on all four items. Art is a
  // "looks or feels right" check (docs/testing/what-to-check.md) and lands with the quest book,
  // not with the converter.
  //
  // Their recipes are not written here: they are the corpus's, emitted by
  // `scripts/factorio-recipe-convert.py` into `kubejs/data/planetaryfactory/recipe/`.

  // Common
  event.create('planetaryfactory:electronic_circuit')
    .displayName('Electronic Circuit')
    .texture('gtceu:item/quantum_processor_assembly')

  event.create('planetaryfactory:advanced_circuit')
    .displayName('Advanced Circuit')
    .texture('gtceu:item/wetware_processor_assembly')

  event.create('planetaryfactory:processing_unit')
    .displayName('Processing Unit')
    .texture('gtceu:item/crystal_processor_assembly')

  // Plastic authors for the same reason: it gates rung 2 (ADR-0025), and a rung-boundary row
  // authors rather than borrows. Its recipe is the Chemical Plant's and arrives with #107.
  event.create('planetaryfactory:plastic_bar')
    .displayName('Plastic Bar')
    .texture('gtceu:item/plastic_circuit_board')

  // Sapros
  event.create('planetaryfactory:yumako_fresh')
    .displayName('Yumako')
    .texture('planetaryfactory:item/yumako');

  event.create('planetaryfactory:jellynut_fresh')
    .displayName('Jellynut')
    .texture('planetaryfactory:item/jellynut');

  event.create('planetaryfactory:iron_bacteria_fresh')
    .displayName('Iron Bacteria')
    .texture('planetaryfactory:item/iron_bacteria');

  event.create('planetaryfactory:copper_bacteria_fresh')
    .displayName('Copper Bacteria')
    .texture('planetaryfactory:item/copper_bacteria');
});
