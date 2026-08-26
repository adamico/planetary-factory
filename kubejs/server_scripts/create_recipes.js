ServerEvents.recipes(event => {
   event.replaceInput({ input: 'create:andesite_alloy' },
    'create:andesite_alloy',
    'minecraft:iron_ingot'
  );

  [
    'andesite_funnel', 'brass_funnel', 
    'smart_chute', 'andesite_tunnel',
    'brass_tunnel', 'basin',
    'mechanical_arm'
  ].forEach(item => {
    event.remove({ output: `create:${item}`});
  });
  event.shaped(Item.of('create:andesite_funnel', 2),
    [
      ' I ',
      ' S ',
      ' S '
    ],
    {
      I: 'minecraft:iron_ingot',
      S: 'create:iron_sheet'
    }
  );
  event.shaped('create:brass_funnel',
    [
      ' T ',
      ' X ',
      ' C '
    ],
    {
      T: 'create:electron_tube',
      X: 'create:andesite_funnel',
      C: 'planetaryfactory:electronic_circuit'
    }
  );
  event.shaped('create:smart_chute',
    [
      ' T ',
      ' X ',
      ' C '
    ],
    {
      T: 'create:electron_tube',
      X: 'create:chute',
      C: 'planetaryfactory:electronic_circuit'
    }
  );
  event.shaped(Item.of('create:andesite_tunnel', 2),
    [
      'II ',
      'SS ',
      'SS '
    ],
    {
      I: 'minecraft:iron_ingot',
      S: 'create:iron_sheet'
    }
  );
  event.shaped('create:brass_tunnel',
    [
      ' T ',
      ' X ',
      ' C '
    ],
    {
      T: 'create:electron_tube',
      X: 'create:andesite_tunnel',
      C: 'planetaryfactory:electronic_circuit'
    }
  );
  event.shaped('create:basin',
    [
      'S S',
      'SSS'
    ],
    {
      S: 'create:iron_sheet'
    }
  );
});
