ServerEvents.recipes(event => {
  event.replaceInput(
    { input: 'gtceu:small_bronze_gear' },
    'gtceu:small_bronze_gear',
    'create:cogwheel'
  );

  [
    'gtceu:lp_steam_solid_boiler', 'gtceu:bronze_brick_casing',
    'gtceu:lv_machine_hull'
  ].forEach(id => {
    event.remove({ output: id })
  });

  // GregTech's own Assemblers lose their crafts entirely (ADR-0026). Every tier, not just
  // LV: the pack's Assembling Machines are the Assembly row now, and a stock Assembler
  // reachable at any tier is GregTech taking the capability back by recipe placement --
  // which is the failure ADR-0025 named when it declined the Large Chemical Reactor.
  ['lv', 'mv', 'hv', 'ev', 'iv', 'luv', 'zpm', 'uv'].forEach(tier => {
    event.remove({ output: `gtceu:${tier}_assembler` })
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

  // Assembling Machine 1, the pack's own (ADR-0026). The id is GregTech-namespaced and
  // tier-prefixed because GregTech's registrate owns both; the display name is authored in
  // `kubejs/assets/gtceu/lang/en_us.json`. Machines 2 and 3 are the same build on the next
  // hull up -- ADR-0018 makes the tiers speed-only, so the ingredients climb with the hull
  // and nothing else changes.
  event.shaped('gtceu:lv_assembling_machine',
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

  event.shaped('gtceu:mv_assembling_machine',
    [
      'WEW',
      'BCB',
      'WEW'
    ],
    {
      W: 'create:cogwheel',
      E: 'planetaryfactory:electronic_circuit',
      B: 'create:belt_connector',
      C: 'gtceu:mv_machine_hull'
    }
  ).id('kubejs:shaped/assembling_machine_2');

  event.shaped('gtceu:hv_assembling_machine',
    [
      'WEW',
      'BCB',
      'WEW'
    ],
    {
      W: 'create:cogwheel',
      E: 'planetaryfactory:electronic_circuit',
      B: 'create:belt_connector',
      C: 'gtceu:hv_machine_hull'
    }
  ).id('kubejs:shaped/assembling_machine_3');
});
