// Custom items registered from KubeJS.
//
// REGISTRATION BOUNDARY (ADR-0015): see the header of `blocks.js`. The same rule applies
// here, and the same crash awaits anyone who registers an id `planetaryfactory_core`
// already owns.

// Sapros's two harvested materials.
//
// Both are spoilable (ADR-0010), which means each is ultimately FOUR registered items --
// Fresh, Ripe, Stale, Spoiling -- chained by the Decay engine (#17). That engine does not
// exist yet, so only the Fresh item of each ships here, under the id it will keep:
// `<material>_fresh`, matching the `jelly_fresh -> jelly_ripe -> ...` chain in
// `docs/research/spoilage.md`. #17 adds the other three beside them and renames nothing.
//
// Nothing here decays, and nothing here may assume Decay is running. The stage badge
// ADR-0010 describes as `layer1` is likewise the Decay engine's to add: these models are
// single-layer until the shared badge set exists.
StartupEvents.registry('item', (event) => {
  // The fruit picked from a Yumako tree's fruiting leaves. Display name is the plain
  // material name -- the freshness stage lives in the id, not in what a player reads,
  // until #17 gives the four stages distinct names.
  event.create('planetaryfactory:yumako_fresh')
    .displayName('Yumako')
    .texture('planetaryfactory:item/yumako');

  // Taken from a Jellystem's stem blocks. Never called Jelly: Jelly is what a Biochamber
  // makes from this, and it ships with `Puzzle: Sapros`, not here.
  event.create('planetaryfactory:jellynut_fresh')
    .displayName('Jellynut')
    .texture('planetaryfactory:item/jellynut');
});
