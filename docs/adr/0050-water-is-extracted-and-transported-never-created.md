---
status: provisional
supersedes: [201]
---

# Water is extracted and transported, never created

`#200` reported that Terra's starting structure has no water, and `#201` that no bucket is craftable
— "the easiest SU source right now" being Create's water wheel, which nothing can feed. Both are one
question: **how does water reach the factory**, and the pack had no answer written down anywhere.

ADR-0048 appeared to have settled it and had not:

> **`offshore-pump` becomes `not_emitted`.** Create's Mechanical Pump against a vanilla water source
> is the water half, and there is no Factorio entity to author.

That sentence is wrong on two counts. The Mechanical Pump is a **pipe-network** block — it moves
fluid between pipes and does not extract from the world at all; `mechanicalPumpRange = 16` in
`config/create-server.toml` is a pipe distance. The block that drains world fluid is the **Hose
Pulley**, and it deems a body infinite only past `hosePulleyBlockThreshold = 10000` contiguous
blocks — an invisible flood-fill whose failure mode is a silently drained lake. And there *is* a
Factorio entity to author: `offshore-pump` is in the corpus with a recipe, and
`data/pack/item-map.json` still carries it as `undecided`.

## The rule

**Water is extracted and transported, never created.** Conservation, not confinement. A player may
pump it, pipe it, barrel it and dig channels to it; what nothing in the pack may do is bring a water
source into existence where worldgen did not put one.

That single sentence is what the rest of this ADR implements, and it is the reason the design ended
up small. Every mechanism below either enforces conservation or is deleted by it.

## Source formation is off

`waterSourceConversion` is set to `false`, forced by the mod on level load (ADR-0015 puts mechanism
in the mod; a player toggling it back on re-opens water creation, so it is re-asserted rather than
defaulted).

This is vanilla's own switch, not an invention. `WaterFluid.canConvertToSource` reads
`GameRules.RULE_WATER_SOURCE_CONVERSION`, and `FlowingFluid.getNewLiquid` converts to a source only
under `canConvertToSource(level) && j >= 2` — the two-adjacent-sources rule. With the rule off, the
3×1×1 trench that turns two buckets into unlimited water does not form its middle source, and no
amount of digging multiplies water anywhere.

Everything else about water is untouched: sources still persist, flow still spreads, and a channel
dug from a natural body still turns a wheel.

## The Offshore Pump is the origin

Pack-authored, reversing ADR-0048's `not_emitted`. It is Factorio's own entity and it makes the
constraint a **block the player places** rather than a number in a config file.

- **Predicate**: one adjacent water block for which `FluidState.isSource()` is true. No minimum body
  size and no biome test. Under the rule above, every source block in the world is one worldgen or a
  structure placed, so naturalness needs no tracking — it is guaranteed by construction.
- **Rate**: `pumping_speed: 20` per Factorio tick × 60 = 1,200 units/s = **1,200 mB/s = 60 mB/t**,
  under the converter's two committed rules — one fluid unit is one millibucket (`BarrelSpec`), and
  one Factorio second is one real second (`factorio-recipe-convert.py`'s `energy_required * 20`).
  **The prototype's `20` is per Factorio tick and coincidentally equals the pack's ticks-per-second
  factor; they are unrelated, and anyone re-deriving this will be tempted to multiply once and stop.**
- **Power**: none. `energy_source: {"type": "void"}`. The `energy_usage: "60kW"` sitting beside it in
  the corpus has no consumer and is a display figure; it is named here because a future reader will
  find it and assume otherwise.
- **Failure**: placement is **refused, with a message**, when no valid source adjoins. A pump that
  places and then silently produces nothing reaches the player as a dead factory three machines
  later, which is the failure mode the vein-indicator check exists to prevent.

## The ratio, which is the number that means something

Neither figure is chosen, and the relationship between them is Factorio's:

```
boiler.energy_consumption = 1.8 MW      target_temperature = 165 °C
steam.heat_capacity       = 0.2 kJ/unit/°C    water.default_temperature = 15 °C

energy per unit  = (165 - 15) × 200 J = 30 kJ
boiler water draw = 1.8 MW / 30 kJ    = 60 mB/s
offshore pump                          = 1,200 mB/s
                                       → one pump feeds exactly 20 boilers
```

The pack's Boiler takes **60 mB/s**, extracted, completing the burner model ADR-0047 already commits
it to. Recording the *ratio* rather than two loose figures is deliberate: if either machine later
deviates, the amendment has to say what happened to "one pump feeds twenty boilers".

**`water.heat_capacity` is a red herring** and is written down here because it cost time once already.
It is `2kJ`, six times steam's, and it is *not* the constant in this formula — the conversion is
governed by **steam's** `0.2kJ`. Using water's gives 183 kJ per unit and about 10 units/s, which is
wrong by a factor of six and looks entirely plausible.

## There is no bucket

`#201` asked for one and this ADR refuses it, which is why that ticket is superseded rather than
merely closed.

With source formation off, a bucket would no longer be an infinite-water exploit — it would place one
1,000 mB source, once. It is refused on other grounds: it is a fluid container carrying twenty times
the pack's own barrel (ADR-0037's 50 mB, on the same 1:1 rule), and `BarrelSpec`'s own docstring
already argues that a bucket-parity container "would be a twentyfold dose of every fluid in the corpus
riding in one item". Admitting a 1,000 mB hand container beside a 50 mB automatable one makes the
barrel strictly worse at its only job.

Nothing in the pack references a bucket as an item, so this costs nothing downstream.

**What replaces it for rung 0**: the player digs. A channel cut from a natural water body flows, and
flowing water is what Create's water wheel reads. This is also why the rule is conservation rather
than confinement — digging to water is free and always was.

## Rung 0 gets water in the starting structure

The water wheel is the pack's **only** rotational source before the burner line
(`data/pack/create-substitutions.json:63`, and the emitted Create subtree carries no windmill, sail or
hand crank). So with no bucket, rung 0 power is blocked until the player reaches water — and
ADR-0049 has just budgeted the opening's traversal, which has no room for an unbounded water hunt.

`scripts/build-terra-start.py` therefore places a pool in the hub. No minimum size is required, since
the pump's predicate has none, and the pool is natural by construction. `#200` asked for exactly this
and it turns out to be the whole answer rather than a convenience.

## Placed flowing water, later

A **flowing-only outlet** — a pack block that maintains flowing water beside it while fed from a pipe
— is wanted, for contraptions tidier than a dug channel. It is deferred, not rejected, and it is safe
for a reason worth stating: what it places is never a source, so `isSource()` refuses it at the pump
with no tracking of any kind. Blockstate is a sufficient check here precisely because the pack never
creates a source; it would have been useless as a way to tell a poured source from a natural one,
which is a distinction vanilla does not record — `BucketItem` empties as
`content.defaultFluidState().createLegacyBlock()`, byte-identical to worldgen's.

It costs a block entity with a refresh tick, because vanilla will not keep an unsupported flowing
block alive: `getNewLiquid` recomputes level as `max(neighbour amounts) - dropOff`, so a flowing block
with nothing upstream returns `Fluids.EMPTY` and is removed. A source persists and regenerates its
own flow; that difference is the entire reason the outlet needs mechanism and a bucket did not.

It lands after the pump and pipes exist. Shipping it first would leave rung 0 blocked behind a pump
the player has no power to build.

## What was rejected, and why it is recorded

Two designs were taken a long way before being dropped, and both would look reasonable to a later
reader:

**Tracking natural-vs-placed water per block.** A chunk data attachment marking every placed source,
retiring on removal, with a codec round trip — `core/ore/`'s arrangement. It was chosen and then
abandoned when the rule "never create a source" made it unnecessary. It **fails open**: any placement
path nobody enumerated becomes an exploit, and the paths are many (bucket, dispenser, Create's
open-ended pipe and Hose Pulley fill mode, pistons, commands). A design whose correctness depends on
an exhaustive list is worse than one where the illegal state cannot be constructed.

**Gating the pump by biome.** Cheap, stateless, unfakeable — and it forbids inland lakes, which makes
water a thing you search a biome map for. Rejected on feel: water in Factorio is not a hassle, and the
pump should not turn it into one.

Also rejected: lowering `hosePulleyBlockThreshold` so ponds qualify (that is location-freedom through
the back door), and any minimum body size (same objection as the biome test).

## Consequences

- Create's `pipesPlaceFluidSourceBlocks` and `fluidFillPlaceFluidSourceBlocks` are set to **`false`**.
  Both create sources from piped fluid, which is water creation by another name.
- `data/pack/item-map.json`'s `offshore-pump` row flips from `undecided` to the pack's own block —
  **when the block lands, not before.** An item-map target for a block that does not exist emits a
  recipe naming nothing.
- ADR-0048's water sentence is amended by this ADR. Its steam chain is untouched.
- `docs/factorio-mechanics.md` gains a **Water as a resource** row; it had none, which is the same
  dropped-mechanic failure `#205` names for trees.

## Checks

- references → the pump's rate and the boiler's draw re-derive from the corpus, in the pattern
  `test_resource_extract.py` uses: the ratio is asserted, not the two numbers separately.
- pack logic → the pump's predicate is a Minecraft-free unit test over a source/flowing fluid state.
- emitted → `tests/worldgen/test_start_geometry.py` asserts the hub pool. A structure that silently
  loses it is rung 0 with no power and nothing in any log.
- looks right → human: dig from the hub pool to a water wheel and confirm it turns; place a pump on
  the pool and confirm it feeds; confirm no arrangement of digging produces a new source block.
