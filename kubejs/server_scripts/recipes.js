ServerEvents.recipes(event => {
  event.replaceInput(
    { input: 'gtceu:small_bronze_gear' },
    'gtceu:small_bronze_gear',
    'create:cogwheel'
  );

  [
    'gtceu:lp_steam_solid_boiler', 'gtceu:bronze_brick_casing',
    'gtceu:lv_assembler', 'gtceu:lv_machine_hull'
  ].forEach(id => {
    event.remove({ output: id })  
  });

  event.shaped('gtceu:bronze_brick_casing',
    [
      'PPP',
      'P P',
      'BBB'
    ],
    {
      P: 'gtceu:bronze_plate',
      B: 'minecraft:bricks',
    }
  ).id('kubejs:shaped/bronze_brick_casing');

  event.shaped('gtceu:lp_steam_solid_boiler',
    [
      'PPP',
      'P P',
      'BFB'
    ],
    {
      P: 'gtceu:bronze_plate',
      B: 'minecraft:bricks',
      F: 'minecraft:furnace'
    }
  ).id('kubejs:shaped/lp_steam_solid_boiler');

  event.shaped('planetaryfactory:electronic_circuit',
    [
      'RPR',
      'EBE'
    ],
    {
      R: 'electroenergetics:copper_wire',
      P: 'create:iron_sheet',
      E: 'create:electron_tube',
      B: 'gtceu:resin_printed_circuit_board'
    }
  );

  event.shaped('gtceu:lv_machine_hull',
    [
      'SSS',
      'S S',
      'SSS'
    ],
    {
      S: 'create:iron_sheet'
    }
  ).id('kubejs:shaped/lv_machine_hull');

  event.shaped('gtceu:lv_assembler',
    [
      'WEW',
      'BCB',
      'WEW'
    ],
    {
      W: 'create:cogwheel',
      E: 'planetaryfactory:electronic_circuit',
      B: 'create:belt_connector',
      C: 'gtceu:lv_machine_hull'
    }
  ).id('kubejs:shaped/assembling_machine_1');
});

