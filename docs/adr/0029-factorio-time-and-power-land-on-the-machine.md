---
status: accepted
---

# Factorio's time and power land on the machine, and no recipe carries `EUt`

`#87`'s converter has to turn Factorio's recipe corpus into pack recipes, and Factorio states four
facts a recipe's execution depends on: the recipe's `energy_required`, and the crafting machine's
`crafting_speed`, `energy_usage` and `drain`. `#126` settled the *quantities* half — the ratio rule —
and found this to be a separate decision, because quantities are a property of the recipe while
throughput and energy are properties of the machine.

This ADR records where each of the four facts lands.

## The rule

| Factorio fact | Where it lands | Rule |
| --- | --- | --- |
| recipe `energy_required` (s) | recipe `duration` | × 20, seconds to ticks. Nothing else |
| machine `crafting_speed` | the machine's duration modifier | `durationModifier = multiplier(1 / crafting_speed)` |
| machine `energy_usage` (W) | the machine's EU modifier | `eutModifier = ContentModifier(0, energy_usage × 32/420_000)` |
| machine `drain` | — | `excluded`, see below |

**No emitted recipe carries an `EUt` field.** This is the load-bearing half. `EUt` is a property of
the machine in Factorio and it stays one here; the recipe carries a duration and nothing else.

## Why `EUt` is not on the recipe

The alternative was the obvious one: emit a per-recipe `EUt` scaled from the host machine's power,
capped at LV's 32. It was rejected for three reasons.

**It makes ADR-0018's rider structural rather than remembered.** That rider — *"every GT recipe is
authored at LV `EUt`, or machine tier becomes a recipe gate by accident"* — is a rule a recipe author
can forget. A recipe that carries no `EUt` at all **cannot** encode a tier gate. The rider's intent is
satisfied by construction, and the rider itself is superseded: see the amendment in ADR-0018.

**It duplicates a machine fact 163 times.** `energy_usage` is constant across every recipe a machine
runs. Writing it into each emitted recipe stores one number in a hundred places and makes re-anchoring
the scale a regeneration instead of a constant change.

**It is not how Factorio models it.** Factorio's `power × time / speed` and the pack's
`EUt × duration / speed` are the same expression only when the machine's speed multiplier scales
duration alone. With `EUt` on the machine and `crafting_speed` on the machine, energy per craft comes
out right automatically, with no third scalar to tune.

## The mechanism

GregTech supports this directly. `ModifierFunction.FunctionBuilder` (verified in
`gtceu-1.21.1-7.0.2.jar`) exposes `durationModifier` and `eutModifier` as **independent** knobs, each
taking a `ContentModifier(multiplier, addition)`. `ContentModifier(0, n)` sets a value absolutely,
which is what makes a recipe with no authored `EUt` work: the modifier supplies it from the machine.
The recipe type stays `ELECTRIC` with `setEUIO('in')` — the field exists structurally, the modifier
fills it.

Separating speed from energy is therefore not the hard part. GT's *overclocking* couples them, but
overclocking only fires when machine voltage exceeds recipe `EUt`, and under this rule a machine's
tier always equals its own recipe tier. `GTRecipeModifiers.OC_NON_PERFECT` on `machines.js` is inert,
not wrong, and is left in place.

## The scale constant

**`P_max` is the Oil Refinery at 420 kW, anchored exactly on LV's 32 EU/t** — a factor of
`32 / 420_000` EU/t per watt.

`P_max` is the maximum over the machines that *receive* a number, which is the intersection of
`data/factorio/machine.json` and `data/pack/category-map.json`, not either one alone. Five machines
qualify:

| Pack machine | Factorio source | kW | `crafting_speed` | derived `EUt` |
| --- | --- | --- | --- | --- |
| `oil_refinery` | `oil-refinery` | 420 | 1 | 32 |
| `chemical_plant` | `chemical-plant` | 210 | 1 | 16 |
| `assembling` (HV) | `assembling-machine-3` | 375 | 1.25 | 28 |
| `assembling` (MV) | `assembling-machine-2` | 150 | 0.75 | 11 |
| `assembling` (LV) | `assembling-machine-1` | 75 | 0.5 | 5 |

Everything else in `machine.json` receives nothing, and for reasons already recorded elsewhere:
`smelting` routes to Create/Mekanism per ADR-0017 and has no `EUt` field at all; the Rocket Silo is
GCyR's; the Research Lab has no energy handler; `crushing` and `centrifuging` are `!`-routed.

**The two files disagree by design and the converter reconciles them.** `machine.json`'s scope rule is
*"keep a machine whose own item recipe is in the corpus"*, which is a Factorio scope — it is why the
space-platform `crusher` is in the file at 540 kW despite `crushing` being `!asteroids-are-post-launch`.
Reading `machine.json` for a maximum without intersecting it against the route map picks the crusher
and is wrong. The extraction is not filtered: its scope rule is honest and a later rung widens it. The
pack-scope view is a projection, and `tests/factorio/` asserts the intersection is total.

## Tiers differ by speed, and the speeds are Factorio's raw values

ADR-0026 promised the three Assembling Machines differ by "speed and tint, nothing else". Tint shipped
in `#73`; speed did not, and with no overclocking above base tier the three machines were identical.
This ADR supplies the missing half: **`crafting_speed` 0.5 / 0.75 / 1.25, Factorio's values
unnormalised.**

Normalising them so tier 1 sits at 1.0 was considered and rejected, because the raw values are what
make `× 20` produce Factorio's own felt durations. Factorio's fastest emitted recipes are 0.5 s — iron
gear, copper cable, electronic circuit, transport belt — and they are never *observed* at 0.5 s,
because the machine crafting them runs at speed 0.5. A player's first assembler takes one second.
Normalising discards exactly that, and then needs a global stretch constant to put it back.

The emitted corpus under this rule: 0.25 s → 10 ticks on tier 1 (`hazard-concrete`, two recipes),
0.5 s → 20 ticks (47 recipes), the 2 s median → 80 ticks, and `rocket-part` at 60 s → 1200 ticks in the
silo. No floor, no stretch constant, no per-recipe exception.

## Idle draw is excluded

Factorio machines consume power while idle. The [Electric system](https://wiki.factorio.com/Electric_system)
page: *"an active assembling machine 2 will consume 155 kW (150 kW energy consumption + 5 kW drain)"*.
Not one crafting machine sets the field — the engine default is `energy_usage / 30` on an electric
source, which is where `machine.json`'s figures come from.

GregTech has no equivalent; an idle GT machine consumes nothing. Reproducing it means real idle draw
built in `planetaryfactory_core` and taught to the player, for a lesson — *don't over-build* — that
ore depletion (ADR-0020) and Emission already teach more cheaply. Folding it into `EUt` is worse than
either: it looks like fidelity and behaves as a flat tax.

Recorded as an `excluded` row in `docs/factorio-mechanics.md`, with the figure quoted.

**It is called "idle draw" in pack-facing prose, never "drain".** `CONTEXT.md` already owns **Drain**
for the route by which Spoilage leaves a Clogged machine on Sapros. The field name stays `drain` inside
`data/factorio/machine.json`, which is extracted data and covered by ADR-0028's declared exception for
Factorio's own names.

## The Personal Assembler takes speed 1

`category-map.json` routes `hand-crafting` to the Personal Assembler, and `hand-crafting` has no
machine — it belongs to the character. The dump settles it: the character prototype sets no
`crafting_speed` at all, because it is not a crafting machine. Hand-crafting runs at exactly
`energy_required` seconds.

Factorio's hand-crafting is slow, but not because of a multiplier. It is slow because the queue is
**serial** — one craft at a time, no modules, no parallelism — and `#95` already gave the Personal
Assembler a timed queue. The pack reproduces the mechanism rather than approximating it with a
penalty, so the Personal Assembler is speed 1 and its durations are `energy_required × 20` unmodified.

## Consequences

- `#87`'s converter emits `duration` and no `EUt`, and reconciles `machine.json` against
  `category-map.json` at read time.
- ADR-0018's second rider is superseded; see the amendment there.
- `kubejs/startup_scripts/machines.js` gains a per-tier `crafting_speed` and a pack recipe modifier.
- Re-anchoring the EU scale is one constant in the converter. Nothing persists `EUt`.
- Emission (ADR-0005) scores off EU/t draw, so this ADR sets the pollution ratios of the GT corpus as
  a side effect. It is a five-machine decision rather than a 163-recipe one, which is the reason to
  prefer this shape and not merely a consequence of it.
