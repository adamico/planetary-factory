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
  // EVERY TEXTURE MUST BE A FILE THAT EXISTS. GregTech generates its MATERIAL items (plates,
  // gears, dusts, most batteries) at runtime from a material set, so `gtceu:item/<material>_plate`
  // and `gtceu:item/max_battery` name no PNG in the jar and render as the missing-texture checker
  // with no error anywhere. Every path below was checked against
  // `assets/gtceu/textures/item/` in `gtceu-1.21.1-7.0.2.jar`, or is vanilla's.
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

  // The oil chapter's two solids and the rocket's two intermediates.
  //
  // `solid-fuel` authors because GregTech ships no solid fuel item and borrowing `minecraft:coal`
  // would have the oil chapter PRINT an ore the pack mines, which ADR-0032's 1:1 stance forbids.
  // The rocket pair authors on Factorio fidelity (#87): the Rocket Silo's cycle consumes
  // Factorio's own intermediates, which revises #41's HDPE-and-circuits triple.
  //
  // `planetaryfactory:rocket_fuel` is NOT `gtceu:rocket_fuel`. This is Factorio's solid item, made
  // from solid fuel and light oil; GregTech's is the FLUID the GCyR rocket entity burns (#41).
  event.create('planetaryfactory:solid_fuel')
    .displayName('Solid Fuel')
    .texture('minecraft:item/charcoal')

  event.create('planetaryfactory:rocket_fuel')
    .displayName('Rocket Fuel')
    .texture('minecraft:item/blaze_powder')

  event.create('planetaryfactory:low_density_structure')
    .displayName('Low Density Structure')
    .texture('gtceu:item/carbon_fiber_plate')

  // Factorio's battery is a crafting INTERMEDIATE, not a placed power store: GregTech's batteries
  // are tiered chargeable hulls and Electro's capacitor is a different thing, so borrowing either
  // would put an EU container inside a recipe that wants lead and acid. No tier suffix -- there is
  // one battery, and a ladder that never arrives costs nothing to leave unnamed.
  event.create('planetaryfactory:battery')
    .displayName('Battery')
    .texture('gtceu:item/lv_lithium_battery')

  // The engine units author because no installed mod ships Factorio's engine as one item, and
  // they feed recipes the pack wants. ADR-0031's author case at its plainest: no borrow candidate.
  event.create('planetaryfactory:engine_unit')
    .displayName('Engine Unit')
    .texture('gtceu:item/lv_electric_motor')

  event.create('planetaryfactory:electric_engine_unit')
    .displayName('Electric Engine Unit')
    .texture('gtceu:item/hv_electric_motor')

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
