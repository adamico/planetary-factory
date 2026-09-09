---
status: provisional
supersedes: []
---

# A tree is one entity holding an amount

`#205` reported that felling a tree log by log is the pack reproducing a mechanic Factorio does not
have. In Factorio a tree is a **single entity**: one mining gesture removes it and yields its wood.
There is no trunk, no canopy and no second gesture. Minecraft's log-by-log felling is therefore not
a missing convenience — it is a mechanic the pack invented by inheriting it.

It is on the critical path at minute zero. `docs/spec/terra-progression.md` puts a log within reach
barehanded, and ADR-0039 leaves exactly one tool in two tiers — no axe, no shovel, no shears — so
whatever felling costs, the player pays it with the Engineer's Pick or with nothing.

## The rule

**A tree is one entity holding an amount, and one gesture takes it whole.**

That is ADR-0041's ore model with one difference. An ore block carries an amount and a break draws
*one unit*, leaving the block standing. A tree carries an amount and a break draws *all of it*,
leaving nothing. Both are the same underlying claim — a resource is an entity with a quantity, not a
pile of blocks — and the difference between them is Factorio's own: an ore patch is mined down over
time, a tree is removed in one gesture.

## The amount is Minecraft's, the rate is Factorio's

Factorio's tree yields a fixed `wood ×4`. The pack does not.

The amount is **the log count of the tree actually broken**, so a jungle giant pays more than a
birch. The alternative — a flat 4 — makes tree height meaningless and, worse, makes a tall tree
strictly worse to fell than a short one, since the player watches nine logs vanish to receive four.

What is taken from Factorio is the **rate**. `tree-01.mining_time` is `0.55` for `wood ×4`, so a log
costs **0.1375 s**, and the gesture costs `amount × 0.1375 s`. A 4-log tree therefore costs Factorio's
own 0.55 s exactly: Factorio's tree is the calibration point rather than a coincidence. The number is
extracted into `data/factorio/tree.json` and never typed, per the repo's standing rule.

The time is delivered as a break-speed modifier on the base block, solved backwards through
`MiningSpeed.forSeconds` the way ADR-0039 already solves the Pick's stated durations. It is halved by
`steel-axe` research with the rest of that ladder: one tool, one speed ladder, and a Pick that got
faster at ore but not at wood would be a second rule to explain.

### Why the divergence is cheap

Wood is terminal in Factorio. Exactly five recipes consume it — `wooden-chest`,
`small-electric-pole`, `shotgun`, `combat-shotgun` and `tree-seed` — and nothing downstream depends
on the rate at which it arrives. Diverging on the yield costs no ratio anywhere else, which is what
makes the player-facing reading ("a bigger tree gives more wood") worth more than the fixed 4.

## What the gesture removes, and what it drops

- **Base only.** The gesture is the tree's own: a log with a log beneath it is mid-trunk, and breaks
  normally. This is also what stops a canopy that touches a neighbour's from being felled from the
  middle of the wrong tree.
- **Leaves go with it**, silently — no drops, no decay ticks. Factorio has no leaves, and vanilla
  leaf decay is a tick storm the fill can pre-empt for free.
- **Drops land at the base block**, one payout, not scattered across a canopy that no longer exists.
- **The fill is bounded** by block count and radius. Over the bound it fells what fits and leaves the
  rest standing as an ordinary tree to break again. The bound exists to cap server work, not to teach
  a rule, so its failure mode is the pre-ticket behaviour rather than a message.
- **A structure never fells.** The fill requires at least one non-persistent leaf, which a placed
  build has none of. Nether stems fall out of this as a consequence rather than by name.

## Saplings are crafted, not dropped

Felling drops no sapling. This is Factorio's answer and not a restriction invented here: a wild tree
yields only wood, and `tree-seed` is a **recipe** costing `wood ×2`. Replanting exists and is paid
for in wood.

Carrying that over keeps felling from being a sapling faucet while leaving the forest renewable, and
it costs one recipe the converter can carry.

## A Factorio `plant` is not this, and is not built here

Yumako and jellystem are not `tree` prototypes. They are `plant`s: `growth_ticks 18000`, grown from a
seed, and one harvest yields **50 fruit and zero wood**, consuming the plant.

The temptation is to read that as the same mechanism with different numbers — flat time instead of
scaled, fruit instead of logs — and it is not, because the two sit in different places in the game.
Wood is terminal. Yumako and jellynut are the **first link of the agricultural science loop**:
`yumako-mash ×15 + jelly ×12 → bioflux ×4`, `bioflux + pentapod-egg → agricultural-science-pack`, and
`yumako-mash ×4 → nutrients ×6` — whose output feeds the towers and biochambers that produce the
input. The real mechanic there is the Agricultural Tower; hand-harvesting is the first turn of a
crank, and the gesture is its least interesting part.

Building the gesture first would therefore not be a partial implementation of that loop. It would be
a different mechanic wearing the same blocks, needing to be unbuilt when the loop lands — and it
would strand `yumako_log` with no source and rewrite two loot tables for a design #23 will revisit.

So Sapros is untouched by this decision: yumako and jellystem stay log-by-log until #23 builds the
loop. The Gleba row in `docs/factorio-mechanics.md` carries the harvest facts as sub-rules, including
that seeds come from `yumako-processing` at 2 % and that the recipe is absent from the pack's corpus
and item map today.

## Consequences

- Felling is mechanism and lives in `planetaryfactory_core`, per ADR-0015. A felling mod was
  considered and declined: every one on 1.21.1 keys off axe tiers and an axe-shaped tool, which this
  pack deliberately does not have, and a new mod arrives with recipes ADR-0034's sweep must handle
  and a survivor decision to argue.
- The bounded fill is a Minecraft-free unit test over a position graph. Whether a tree falls in a
  running game is a world load.
- `yumako_log` and `jellystem_stem` keep their current source until #23, which is the ticket that has
  to say what grants them once the plant model lands.
