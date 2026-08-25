StartupEvents.registry('item', event => {
  // Common
  event.create('planetaryfactory:electronic_circuit')
    .displayName('Electronic Circuit')
    .texture('gtceu:item/quantum_processor_assembly')

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
