// THROWAWAY -- delete this file and `server_scripts/_throwaway_decay_test_chain.js` together.
//
// Not pack content. This exists only so the Decay engine (#17) has a real four-link ladder to
// exercise in-game, since the pack itself ships only the `*_fresh` half of each spoilable and no
// spoil recipes at all. Nothing here may be referenced by anything that ships.
//
// Five items: four freshness stages plus the terminal Spoilage. Textures are borrowed from
// existing pack art purely so the stages are distinguishable at a glance in an inventory -- the
// point is to SEE a 64-stack split across rungs, and identical icons would hide exactly that.
StartupEvents.registry('item', (event) => {
  const stage = (id, name, texture) =>
    event.create(`planetaryfactory:${id}`).displayName(name).texture(texture);

  stage('decaytest_fresh', 'Decay Test (Fresh)', 'planetaryfactory:item/yumako');
  stage('decaytest_ripe', 'Decay Test (Ripe)', 'planetaryfactory:item/jellynut');
  stage('decaytest_stale', 'Decay Test (Stale)', 'planetaryfactory:item/iron_bacteria');
  stage('decaytest_spoiling', 'Decay Test (Spoiling)', 'planetaryfactory:item/copper_bacteria');
  stage('decaytest_spoilage', 'Decay Test Spoilage', 'minecraft:item/rotten_flesh');
});
