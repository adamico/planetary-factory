---
status: accepted
supersedes: [57, 63]
---

# An ore block carries an amount, and mining draws from it one unit at a time

ADR-0020 made depletion physical block removal and built no second mechanism. ADR-0039 then shipped
a table headed **"Seconds per ore"**, a method called `PickTier.secondsPerResource()` and a test
asserting seconds-per-*item*, against a world where breaking an ore block yields exactly one item
and the block is gone. The ledger's Manual mining row carries the correction ADR-0039 quotes in its
own second paragraph: "mining is a Minecraft block break, so it is **per-block** and tool-tiered."

Per-ore is the stated model and per-block is the shipped one. This decision makes the stated one
true.

**An ore block carries an amount, and mining draws from it one unit at a time.** The block stands
until the amount is exhausted, then breaks. Hands and machines draw from the same number: a hand
break cycle yields one ore and decrements the block exactly as a drill operation does, which is
what makes "seconds per ore" literal rather than aspirational.

## This is not the counter ADR-0020 refused

ADR-0020 rejected "a yield counter layered on top of blocks that *also* disappear", because the two
readings can disagree "and the counter always wins the argument while the player believes their
eyes". **Here the count *is* the resource, and the block disappearing is the count reaching zero.**
One event, one reading, nothing to disagree with. ADR-0020 carries the amendment; this ADR is what
it points at.

It also does not collide with ADR-0032. That ADR forbids ore *multiplication* and protects the
ore→plate ratio, which stays 1:1. An amount changes how much ore the ground holds — ADR-0020's
throughput claim, not ADR-0032's.

## Every number is extracted, and none is chosen

ADR-0022 imports Factorio's tech tree as data rather than transcribing it. The same rule applies
here, and it turns out to be cheaper than it looks: **Factorio's resource amounts are closed-form
in the prototype dump**, not buried in map generation.

A fifth extractor, `scripts/factorio-resource-extract.py`, writes `data/factorio/resource.json`
alongside the four existing corpus files, from the same `--dump-data` run.

**The starting patch total.** `resource_autoplace_all_patches` defines it outright:

```
starting_amount = 20000 * base_density * (frequency_multiplier + 1) * size_multiplier
```

At default controls, with `base_density` read from each resource's patch expression:

| resource | `base_density` | starting patch total | starting placement |
| --- | --- | --- | --- |
| iron | 10 | 400,000 | yes |
| copper | 8 | 320,000 | yes |
| coal | 8 | 320,000 | yes |
| stone | 4 | 160,000 | yes |
| uranium | 0.9 | — | `has_starting_area_placement = 0` |

Uranium has no starting patch in Factorio, which is why Terra's starting area never had one.

**The distance law.** Every resource's `richness_expression` carries the same term:

```
max((1000 + distance) / 2600, 1)
```

It is **1.0 everywhere inside 1600 tiles of spawn** and rises linearly beyond. Factorio does not
reward leaving early, and this is the arithmetic saying so. The outfield law — `regular_density_at`,
with `starting_resource_placement_radius = 150`, `regular_patch_fade_in_distance = 300` and
`double_density_distance = 1300` — is closed-form in the same function and ports directly.

**A Factorio tile and a Minecraft block are both one metre**, so the mapping is metre-for-metre and
needs no scale factor: 1600 tiles is 1600 blocks. (A Factorio chunk is 32×32 tiles and a Minecraft
chunk is 16×16 blocks, so one Factorio chunk covers four of ours. That matters to where the delta
is stored, not to any distance.)

**The stage thresholds are ratios, not amounts.** `stage_counts` for iron, copper, coal and stone is
`[15000, 9500, 5500, 2900, 1300, 400, 150, 80]`; uranium's is that list scaled by exactly 2/3 at
every rung. So Factorio's eight sprite stages are a **material-independent fraction set** — 1.0,
.633, .367, .193, .087, .027, .010, .0053 of a tile's own initial amount. That is what makes them
usable against blocks holding a thousand units rather than fifteen thousand.

## Terra's per-block amount falls out of the geometry

Terra's starting fields are a jigsaw structure of about 1150 ore blocks (ADR-0019 as amended), not
Factorio tiles. **The patch total is the invariant and the per-block amount is the quotient**:

```
amount per block = starting_amount(resource) / blocks in that resource's field
```

`scripts/build-terra-start.py` already knows both sides, so the number re-derives whenever patch
sizing changes and is never typed anywhere. At present field sizes that is roughly 1,040 per block
for iron and 835 for copper and coal — figures nobody chose and nobody maintains.

The consequence worth stating plainly: **the burner drill's 2×2 footprint at 0.25 items/s now runs
for hours on one placement instead of sixteen seconds.** That is the entire reason the mechanic
exists. A rung-0 machine you relocate four times a minute is a mining animation, not automation.

## Stone is the fifth resource, and ADR-0021 was wrong about it

ADR-0021 ruled: "Stone is ambient terrain, never a patch. This is the one place fidelity
deliberately loses. A stone patch in a world made of stone reads as a joke." **That is reversed
here, on two grounds.**

**The substitute mechanism does not exist.** ADR-0021 discharged stone's function — "a bulk
feedstock you must build production for" — onto "a cobble generator being something the player
builds". Terra's noise settings carry `aquifers_enabled: false` and place no lava; the `lava` entry
in the router is referenced by nothing. **A cobble generator is unbuildable on Terra.** The ADR
traded fidelity for a mechanism that isn't there.

**And the joke argument is wrong on its own terms.** Stone quarries exist on Earth, a planet made
of stone. What makes a quarry is concentration and accessibility, not the rock being absent
elsewhere. On Terra the same holds twice over: ADR-0019's ground puts a dirt cap over the stone
column, and **no machine in the pack automates digging terrain** — the drills mine ore. Free stone
is neither convenient nor automatable, so a metered patch never competes with it, and ADR-0020's
two-readings failure does not arise.

**A stone ore block is not a stone block.** It is a visually distinct ore, so a patch never reads as
marked-up ground. It drops `minecraft:cobblestone`, which `data/pack/item-map.json` already records
as Factorio's stone ("Factorio's stone is the MINED rock, and Minecraft's mined rock is
cobblestone"), so the `stone-brick` chain — smelt cobble to stone at 1:1 — is untouched.

So **Terra's alphabet is five: iron, copper, coal, uranium and stone**, and its starting area deals
**four fields**, stone the smallest at 160,000. Stone also gets an authored outfield vein: a metered
patch that stops being metered once it is gone is the inconsistency ADR-0021 fell into.

## The ore block is pack-authored

GregTech registers material ore blocks in code and models them at runtime — the jar ships no
blockstate JSON for them, only for machines. Adding a stage property to GT's block means a mixin
into its registration *and* its model provider, across every material and stone type it registers,
to obtain the behaviour for five.

So **`planetaryfactory_core` registers the ore blocks for the alphabet**, per ADR-0015's rule that
mechanism lives in the mod. What makes this affordable is that **the block drops GregTech's raw ore
item**: the block changes and the item does not, so `item-map.json`, every generated recipe,
ADR-0032's 1:1 chain and ADR-0034's sweep are all untouched. The bounded cost is worldgen
references — the structure templates' literal `gtceu:iron_ore`, Terra's vein definitions and
`tests/worldgen/expected.json` — which is a generator re-run already under the worldgen check.

**One risk gates this and must be settled first.** `gtceu:lv_miner` is rung 1's drill (ADR-0040) and
it *scans for ore blocks*. Whether it recognises a pack-registered block — presumably via ore tags —
is unverified, and GT's miner is only in the jar. Read that scan logic before the worldgen
migration, not after.

## The amount is derived, and only the difference is stored

A block's **initial** amount is a pure function of its position: the resource, the patch total, and
the distance law. It is recomputed on demand and never written down.

**What persists is the units already drawn**, as a sparse per-position delta on a **chunk data
attachment** — the idiom the mod already ships and codecs for (ADR-0038). An untouched field of 1150
blocks costs nothing; the delta unloads with its chunk; and the amount stays derived from the world,
with the store recording only what a drill actually did. A block entity per ore block was the
obvious alternative and is not affordable at patch scale.

**An entry retires on any block change away from the ore block** — not only on depletion. A block
destroyed by TNT, a creative break or a structure overwrite must drop its delta, or a later block at
that position inherits a stranger's.

This is also what keeps #57 and #63 honest, and why they are superseded rather than merely amended.
Both derive the map's yield readout by **counting** the ore blocks in a vein's bounds. Under an
amount that is wrong: a fresh patch and a nearly-spent one hold the same block count. **The readout
sums the blocks' amounts instead.** Still computed from the world on demand, still no persisted
counter, still unable to drift — a change to the derivation, not to the decision.

## Reading the remaining amount

**Eight sprite stages, at Factorio's ratios, plus a Jade line for the exact number.** ADR-0020's
refusal of "ore blocks that thin out as they are mined" is amended: the objection was that a stage
competes with the amount, and a stage computed *from* the amount cannot. Factorio itself judged this
worth rendering, over a 15000-unit range, which is a stronger case than the pack's thousand-unit
blocks need.

The starting fields get stages and Jade but no map layer: they are ordinary blocks in a jigsaw
template, carry no `GeneratedVeinMetadata`, and three walkable fields visible from the wreck door do
not need finding.

## Hand-mining, and what stands in the hole

**One break gesture draws one unit and the block stays.** This is Factorio's own behaviour, and it
means the pick is a trickle nobody uses past the first minutes — which is correct: the burner drill
is in the player's pocket at spawn (ADR-0040), and the rung-0 loop is drill, first plates, more
drills. The rejected alternative — a hand break taking one unit and destroying the remainder — hands
the player a way to vandalise a patch for one ore.

**A depleted block becomes stone**, uniformly, including for stone ore. ADR-0019 flattened Terra and
the fields lie flush with the topsoil; breaking to air leaves a pitted field the drill's own
footprint must sit on. The hole argument is served by the eight stages and by the patch visibly
shrinking, which is more legible than a crater.

## Consequences

- **`scripts/factorio-resource-extract.py`** and `data/factorio/resource.json` join the corpus;
  `data/factorio/README.md` gains the file and the regeneration step.
- **`planetaryfactory_core`** registers five ore blocks, the amount derivation, the chunk
  attachment and codec, the stage property and the Jade line.
- **`scripts/build-terra-start.py`** deals a **fourth field** and emits pack ore blocks; the hub
  gains a stone connector and pool.
- **`tests/worldgen/test_start_geometry.py`** must cover four fields against every hub and size
  combination — vanilla drops an overlapping jigsaw child silently, so this is the check that keeps
  a seed from shipping three patches instead of four.
- **Terra's vein definitions** gain a stone vein and point at pack blocks;
  `tests/worldgen/expected.json` follows.
- **ADR-0020** is amended: stages are admitted, and the readout sums rather than counts.
- **ADR-0021** is amended: the stone bullet is struck, and the set becomes five.
- **ADR-0039** is amended: the Steel Pick keeps rung 1, but its justification is no longer ore
  speed — that sentence named the Steam Miner ADR-0040 deleted, and ore does not pass through hands
  past the opening. It is earned by trees, stone, dirt and machine dismantling.
- **`docs/factorio-mechanics.md`**: the Manual mining row loses its per-block notice; *Resource
  richness varies per patch* moves off `unargued`; the stone row moves off ADR-0021's exclusion.
- **#105 unblocks.** Its operations-per-second and yield-per-operation now have something to draw
  from, with yield dormant at 1.0 for both rigs.

## Considered alternatives

- **Make Terra's ore bedrock deposits.** GregTech's bedrock layer already has a real native counter
  — `depleted_yield`, `BedrockOreVeinSavedData`, a depletion percentage on the miner. Rejected: #86
  cut Terra's bedrock ore deliberately, and ADR-0019 makes Terra's ore surface-prospected and dug
  from patches you can see. A bedrock deposit is an invisible tap read off a machine GUI, which is
  the opposite of the patch-and-move loop.
- **Cut the block count instead of raising the ore total** — keep 1150 units and spread them over
  fewer, richer blocks. Rejected: it destroys ADR-0019's legibility requirement and makes the
  footprint problem worse, since a drill would cover a whole patch.
- **Anchor the amount on the `stage_counts` ladder.** This was the working answer until
  `starting_amount` was found. Rejected as inferior once a closed form existed: the ladder is a
  render threshold, not a patch total.
- **Port `resource_autoplace_all_patches` in full.** Rejected: it computes patch *shapes* from spot
  noise, and Terra's patches are a jigsaw structure. The amounts port; the placement does not apply.
- **A client-side overlay instead of blockstate stages.** Rejected once the ore blocks became
  pack-authored — a bespoke renderer to avoid a mixin we no longer need.
- **Stone as ambient terrain** (ADR-0021's position). Rejected above.

## Checks

Per `docs/testing/what-to-check.md`:

- *An amount decrements, the block breaks at zero, and a hand draw and a drill operation take from
  the same number* — **unit test**, `:planetaryfactory_core:test`. The derivation and the delta are
  addressable without a world.
- *The delta round-trips through its codec* — **unit test**, the failure ADR-0038 names: a codec
  that drops a field returns a patch that silently refilled over a logout.
- *An entry retires when the block is destroyed by something other than depletion* — **unit test**.
- *Seconds-per-ore holds across a multi-unit block* — extends `MiningSpeedTest`.
- *The extractor's numbers match the dump* — **static check**, `tests/factorio/`, running the
  extractor's `--check` as the other four do.
- *Four starting fields never overlap* — **static check**, `test_start_geometry.py` extended.
- *The stage ratios render, the Jade line matches what the block pays out, and `gtceu:lv_miner`
  mines a pack ore block* — **human on delivery**, one world load.
- *Whether the amounts pace rung 0 well* — **human on delivery**. This is the tuning number and it
  cannot be settled statically.
