// THROWAWAY -- delete this file and `startup_scripts/_throwaway_decay_test_items.js` together.
//
// The four-link ladder over the throwaway items, in `respoiled:spoil_recipe` form.
//
// `spoiltime` counts SWEEP PASSES, not ticks -- `spoilRate` in `config/respoiled-common.toml` is
// the conversion, 30 ticks per pass as shipped. Ten passes is fifteen seconds a rung, so a nominal
// lifetime of one minute; because each rung is a geometric draw and not a timer, an individual item
// may take much longer, which is the Erlang-4 spread the design is built on and is itself the thing
// worth watching.
//
// Every rung shares one `spoiltime` deliberately: `SpoilChain.catchUpDepth` only lets chunk
// catch-up cross more than one rung at a time when the ladder is uniform, so an uneven chain here
// would quietly test a different code path than the pack will ship.
const DECAY_TEST_PASSES = 10;

ServerEvents.recipes((event) => {
  const rung = (from, to) =>
    event.custom({
      type: 'respoiled:spoil_recipe',
      ingredient: { item: `planetaryfactory:${from}` },
      result: { id: `planetaryfactory:${to}`, count: 1 },
      spoiltime: DECAY_TEST_PASSES,
    });

  rung('decaytest_fresh', 'decaytest_ripe');
  rung('decaytest_ripe', 'decaytest_stale');
  rung('decaytest_stale', 'decaytest_spoiling');
  // The terminal rung. `decaytest_spoilage` has no recipe of its own, which is what ends the chain.
  rung('decaytest_spoiling', 'decaytest_spoilage');
});
