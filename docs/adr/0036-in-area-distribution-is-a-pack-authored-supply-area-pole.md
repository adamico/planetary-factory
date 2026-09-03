---
status: accepted
supersedes: [46, 104]
---

# In-area distribution is a pack-authored supply-area pole, and the grid mod is Power Grid

ADR-0035 removes Mekanism, and with it the Universal Cables that `#46` made the answer to
distribution *inside* an area. That row cannot be left empty: ADR-0017 teaches the grid as a
boundary — the grid ends where the machines begin — and the cables were the machine side of it.

**In-area distribution becomes a Factorio supply-area pole, registered in `planetaryfactory_core`.**
A block that scans a radius and pushes EU into every `IEnergyContainer` inside it. **The
transmission mod becomes Create: Power Grid**, replacing Create: Electro Energetics, gated on one
bench test.

## Why a pole and not a cable

The pole is the cheaper block to write, which is the opposite of how it first reads.

**There is no network to author.** No graph, no propagation, no merge and split on placement, no
persistence of a topology across chunk unloads — that is where a cable mod's cost actually lives.
A radius scan on a tick interval has none of it. What the pack gets in exchange is Factorio's own
mechanic rather than an approximation of it: the player places a pole and everything inside its
supply area is powered, which is the thing a Factorio-literate player already knows how to reason
about.

**Cables cease to exist as a block class in this pack.** GregTech's stay removed with the rest of
its power layer (`#37`), no voltage-tier ladder is re-admitted, and `enableFEConverters` stays
`false` in `config/gtceu.yaml` so the LV-through-LuV × 1A/4A/8A/16A converter grid never enters JEI.

**All four Factorio pole tiers ship** — small, medium, big, substation. Fidelity over the cheaper
option of shipping two now and one later. It is a **footprint ladder, not a power ladder**: the
tiers differ in the ground they cover and in nothing else, and none of them changes what a machine
receives. That distinction is what keeps the pole off the science spine, where a power
ladder would compete with it.

## Three facts from the jars that decide this, and must not be re-derived

Verified with `javap` against the installed jars. They are the reason the two obvious alternatives
both fail before they reach a trade-off.

- **GregTech machines do not accept FE.** `GTCapability` registers exactly one energy capability,
  `CAPABILITY_ENERGY_CONTAINER`, and machines expose `NotifiableEnergyContainer` — EU only.
- **`nativeEUToFE` is GT learning to push into FE, not to read it.**
  `CommonInit.registerCapabilities` walks the block registry and wraps *third-party FE blocks* as GT
  energy containers via `GTEnergyWrapper`. The arrow points outward.
- **An Electro Connector on a machine does nothing.** Connectors are wire terminals carrying volts.
  The Converter was Electro's only V↔FE block.

So "put a connector on every machine" does not work, and "bring GT cables back" solves a problem
that does not exist while re-admitting the ladder `#37` deleted.

## The bridge is an edge of the pole, not an object

**There is no separate placeable boundary block.** The V↔FE bridge is built into the pole's vertical
extent: the pole exposes a NeoForge `IEnergyStorage` and is fed through the grid mod's own bridge
block, and volts become machine power as part of what a pole *is*.

**This deletes ADR-0017's Converter-as-a-block beat, deliberately.** That beat taught the boundary
by making the player place it. Factorio does not have it — the boundary is where the supply area
ends — and the pack chose Factorio's arrangement. The lesson survives; the block that taught it does
not.

**No mod internals are touched.** Neither Electro nor Power Grid publishes an API, and feeding a
vanilla `IEnergyStorage` through the grid mod's bridge block is the only interop path either author
supports.

## No machine-side energy storage, and no per-area cap

**Storage is grid-side only.** Sag and blown fuses propagating all the way to the machines is the
point of running a physical grid; a buffer sitting behind the bridge would insulate the player from
the exact signal the grid exists to send. If playtesting says this is miserable rather than
instructive, the lever is accumulator size, not a machine buffer.

**The per-area power cap is dropped.** ADR-0017 banked "a real per-area budget" on Electro's
`converterMaxPower = 100.0` kW — verified as the mod's shipped default in
`config/electroenergetics-server.toml`, not a pack invention. The argument was that one Converter per
cluster at a 100 kW ceiling forces the player to partition the factory into power-bounded districts.
**Two of its three premises are gone**: the bridge is no longer a placeable chokepoint, and Power
Grid ships no equivalent single-block kW ceiling.

**The limit becomes emergent** — wire gauge, voltage sag and fuses — which is what a physical grid
should have been doing in the first place, and what the cap was standing in for.

**A live current-vs-Imax overlay teaches it**, built on Power Grid's existing multimeter, plotter and
goggle integration. That is quest-and-config work, not mod work. Letting the first blown fuse teach
it was rejected: the failure is invisible until after it has happened, and voltage sag is a rotten
error message on its own. A scripted brownout in the quest line is held as the fallback if the
overlay proves insufficient in play.

## Electro Energetics becomes Power Grid

The acceptance test was fixed **before** the research was read, so the mod was not chosen by the
features it happened to have: **brownout propagation and a wire-tier ladder are the two hard
requirements**; everything else negotiable. Power Grid passes both, richly.

`powergrid` by patryk3211 — https://modrinth.com/mod/power-grid,
https://github.com/patryk3211/PowerGrid. Not *Create: Powerplantgrid*, which is an addon to it, and
not marvin-roesch/PowerGrid, which is dead and unrelated. **1.21.1 is NeoForge-only**; 0.6.1
published 2026-08-26.

What it brings that Electro did not:

- A **nodal solver** with Ohm's law and real voltage drop under load. Brownouts are not a named
  mechanic — they fall out of the sag, which is the correct way round.
- **Per-material wire gauge as real data**: copper R 0.0015/item, max span 24 m, Imax 80; iron
  0.005 / 64 m / 160; gold 0.003 / 12 m / 160. That is the wire-tier ladder as a physical property
  rather than a config number.
- Grounding rods that scale with buried block count, fuse holders, overheating,
  `explosiveDeconstruction`, entity electrocution.
- **Generation is a Create-kinetic multiblock** — rotor, winding, housing, commutator — plus a
  real-PV Solar Panel. It stands exactly where Electro's Alternator stands in ADR-0017's chain,
  Steam Engine → SU → grid, so the chain that `#101` fixed survives the swap intact.

**Two facts about it are constraints, not features.** Its V↔FE is **two one-way blocks**, not one
bidirectional: the Device Connector is grid→FE (`canReceive() = false`) and the FE Inverter is
FE→grid (`canExtract() = false`, a controlled voltage source). And **it has no pole supply area** —
transmission is point-to-point catenary, 12–64 m by material. The second is survivable only because
the supply area is now the pack's own block; had the pole not been decided first, this swap would
fail on it.

**Sequenced after ADR-0035's removal**, so that a startup failure has one candidate cause rather
than two. **Committed only after the bench test passes**: a pack-authored `IEnergyStorage` block
drawing through a Device Connector in a live world, without the crash in
[issue #1021](https://github.com/patryk3211/PowerGrid/issues/1021). The pole prototype *is* that
bench test.

**The Create pin does not lift.** Power Grid declares `[6.0.9,6.1.0)` as `type = "required"` and its
`powergrid.mixins.json` is `"required": true` with `defaultRequire: 1`, so a 6.0.x patch that
refactors the internals it mixes into still satisfies the range and aborts startup — the same trap
ADR-0017 recorded for Electro. Accepted as a side effect. It was never a goal and never a tiebreaker.

## Amended by #147, on building it

Five things this document either got wrong or never said. They are recorded here rather than in the
code that discovered them, because tickets are the route and ADRs are the state.

**The supply areas are Factorio's own, and they do not increase monotonically.** 5x5, 7x7,
**4x4** and 18x18. The big pole's area really is smaller than the medium pole's: in Factorio it
buys *wire reach* instead, 30 tiles against 9, and a player who knows Factorio knows the big pole
as the one you run a line with rather than the one you cover a base with. This document originally
said "a bigger pole covers more ground and spans further"; **neither clause is true**. The second
was never available at all -- Power Grid owns transmission and does it with catenary whose span is
a material property of the wire, so a pole here has no span to differ in. Fidelity won over the
tidier ladder, and the four values are pinned by `PoleTierTest` so the inversion is not "fixed" by
someone reading it as a typo.

**Even-sided areas are offset, not rounded.** Factorio's big pole and substation are 2x2 entities,
so their even-sided areas centre on a seam between tiles. The pole here is one block. The tile
count is kept exact and the area sits half a block off, taking its extra block on the negative
side -- the same relationship Factorio's 2x2 pole has to any single tile beneath it.

**The supply area has a vertical extent, which Factorio has no answer for.** Two blocks up and
down, the same for every tier. That covers a machine on the pole's own floor, one sunk into it and
one on a platform above, without a pole quietly powering the floor below through the ceiling. It is
deliberately much shallower than the horizontal reach, so the area still reads as a footprint on
the ground.

**A shortfall is shared out, not raced for.** When the area asks for more than the grid is giving,
the pole water-fills: every machine gets an equal cut, and a machine whose demand is below that cut
takes only what it asked for while its spare goes back to the machines that can still use it. The
alternative was first-come-first-served, which would have starved the machines the scan happens to
reach last -- permanently, on an iteration order that is an implementation detail and invisible to
the player. Factorio shares a shortfall out, and this is the one place the pole needed a rule
rather than an arithmetic.

**Energy is inserted directly, not accepted from a network.** GregTech's
`acceptEnergyFromNetwork` enforces the voltage tiers `#37` deleted entire and can overvolt a
machine into exploding. There is no tier for a pole to respect and no face for a wireless supply to
arrive through, so the pole calls `addEnergy` and the question does not arise.

## Considered Options

- **Keep Electro and give in-area distribution to the pole anyway.** The pole does not require the
  swap, and this was the cheap option. Rejected on the two hard requirements: Electro's physics is
  configurable but its wire tiers are voltage ratings in a config file, where Power Grid's are
  material properties in a solver. Having decided to pay for a physical grid, the pack should get
  the physical one.
- **Connectors on every machine**, or **GregTech cables return.** Both fail on the capability
  surface above, before any trade-off.
- **Author a real cable network in `planetaryfactory_core`.** Rejected: it is the expensive half of
  a power mod, and it buys an idiom Factorio does not have.
- **Two pole tiers now, the other two later.** Rejected for fidelity. The tiers are cheap once the
  supply-area scan exists — they differ by one number.
- **Keep the per-area cap by other means.** Rejected: the emergent limit is better teaching, and a
  synthetic ceiling on top of a solver that already models current is two answers to one question.

## Consequences

- **Two unresolved upstream risks, both load-bearing.**
  [#1021](https://github.com/patryk3211/PowerGrid/issues/1021) is an instant crash using the Device
  Connector on other mods' blocks, Mekanism named, closed 2026-08-18 with the fix commit
  unconfirmed — this is what the bench test exists to check.
  [#937](https://github.com/patryk3211/PowerGrid/issues/937) is Device Connector ↔ GTCEu converter
  interop, claimed working, disputed, closed with no stated resolution. The pack's route does not
  depend on the GTCEu converter, which is why #937 is recorded rather than blocking.
- **Maturity is adequate but not settled**: ~1M Modrinth and ~1.7M CurseForge downloads, `release`
  channel through 0.6.1, all hundred most recent commits inside August 2026, 263 open issues, ~80
  blocks and ~55 items. It is actively developed rather than finished, and the pin makes that the
  pack's problem.
- **Manifest churn**: the `electroenergetics` entry is replaced by a `powergrid` one, and
  `config/electroenergetics-*.toml` goes with it. `scripts/pack-check.sh` will report the drift.
- **ADR-0017's transmission, distribution and energy-storage rows are all rewritten**, and its
  Converter paragraph is deleted rather than amended.
- **`#37`'s four-facts doctrine stays spent.** `#46` recorded the cost of installing more electrical
  modelling than GT's removal deleted, and justified it on audience perception rather than literacy
  volume. Power Grid is a further step in the same direction, and the same justification is being
  spent again rather than re-earned — recorded here so the next reader sees the running total.
- **The block-level cut list is still not decided**, and now applies to a different mod's ~80 blocks.
  ADR-0017's "adopt whole, cut as necessary" holds, and the cutting still waits on hands-on play.
