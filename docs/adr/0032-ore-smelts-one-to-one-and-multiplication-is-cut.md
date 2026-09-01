---
status: accepted
supersedes: [27, 37]
---

# Ore smelts one to one, and every multiplier is cut

Terra runs three mods that each ship an ore-multiplication ladder. GregTech's is macerate-wash-
centrifuge, Mekanism's is enrichment → purification → injection → dissolution at 2x/3x/4x/5x, and
Create's is a Crushing Wheel pair at 2x. ADR-0017 already recipe-removed GregTech's line and split
the rest between Create at rung 0 and Mekanism from rung 1.

**Factorio has no ore-multiplication ladder.** Ore smelts 1:1 and always has. Every yield gain in
that game comes from research and modules — infinite `Mining productivity`, productivity modules,
the Foundry's built-in bonus in Space Age — never from a tier of machines you unlock and build once.
A ladder of machines that each multiply harder is a *GregTech* idiom that three mods happen to share,
and ADR-0021 already fixed the axis this is decided on: Factorio fidelity over Minecraft fidelity,
modded or vanilla.

## The decision

**Ore smelts one to one, everywhere, on every body. No block in the pack multiplies ore.**

- **Mekanism's ore chain is recipe-removed**: Purification Chamber, Chemical Injection Chamber,
  Chemical Washer, Chemical Crystallizer. They exist only to multiply, and ADR-0017's rule is that a
  losing block is recipe-removed rather than left craftable-but-outclassed.
- **The Enrichment Chamber loses its ore recipes.** The block is Mekanism's own and keeps whatever
  non-ore work the pack gives it; what dies is 2x ore → 2 dust.
- **Create's Crushing Wheels and the Millstone keep their blocks and lose their ore recipes.** They
  remain rung-0 kinetic machines for everything that is not ore.
- **The Chemical Dissolution Chamber is deliberately not decided here** — see below.

**Pack-wide, not Terra-only.** The fidelity claim does not get weaker on Ignus, and every later
body's puzzle is unwritten. Binding them now costs one sentence; binding them after Ignus ships costs
a re-argument nobody will want to have. This is why the decision is its own ADR rather than an
amendment to ADR-0017, which is the Terra ownership table and is read as Terra-scoped.

## Why not half of it

Two smaller cuts were on the table and both are worse than the whole one.

**5x only** — the tier that prompted the question, when sulfuric acid moved to rung 2 (ADR-0025).
Arbitrary: it draws the line where the schedule happened to put it, and the fidelity argument that
kills 5x is word for word the argument that kills 4x, 3x and 2x.

**The Mekanism ladder, keeping Create's rung-0 2x.** Worse than arbitrary. It leaves one mod holding
the sole multiplier, which is the inversion ADR-0017's own rule exists to prevent — except here the
*idiom* is what was judged unfaithful, not the mod. A mechanic cannot be cut for being un-Factorio
and then left in one mod's hands.

## The yield curve is not built, and Terra is not compensated

Factorio's own replacement for a multiplication ladder is a curve you keep paying for: infinite
`Mining productivity` research against the drills. It would sit well beside ADR-0020's depletion arc,
and it is **not built here**, for two reasons.

**The pack cannot express it.** ADR-0022's extractor drops three technology families because
Researchd has no concept that fits them — `max_level = "infinite"`, `count_formula`, and `upgrade`
chains — which is 106 of Factorio's 268 technologies, and `mining-productivity` is the docstring's
own example. Reaching for it means building levelled research in `planetaryfactory_core` first.

**Terra should not be compensated anyway.** ADR-0020 states the answer to *"I need more ore"* and it
is not a multiplier: Terra is deliberately the bad way to get metal, its ore→plate ratio is the
pack's floor, there is no on-planet fix, and the off-ramp is departure. A yield curve on Terra
softens exactly the pressure that ADR spent itself building.

If levelled research is ever built, it should be built where the retrofit tradeoff is the point —
`#120`'s modules and beacons — and not as a patch for a rung that lost a reward it turned out not to
need.

## Rung 1 did not lose anything

`#69` was written expecting this to empty ADR-0018's rung 1, whose whole stated reward was "first
machines and Mekanism enrichment". It does not. `#34`'s beat sheet gives rung 1 **the Alternator, the
FE grid and Assembling Machine I**, and hung the grid on the Assembler rather than on the Enrichment
Chamber specifically so that this decision could not knock it over. Rung 1's row becomes
*"Electricity, and the first assembler"* — a larger reward than enrichment was, and Factorio's own
rung-1 shape.

Rung 3 loses a clause and needs nothing in its place; it already carries advanced oil processing,
cracking, lubricant, the blue circuit and the Electric Furnace.

## The Ore processing row is deleted, not re-owned

With ore at 1:1 there is no step between extraction and smelting. GregTech extracts, on its own row;
the Furnace reduces, on the separate **Smelting (reduction)** row `#91` deliberately created because
reduction is not multiplication. "Ore processing" would name a capability with nothing in it.

An owner named for an empty capability is exactly how ADR-0017's table produced three recorded
failures — the Refining row, the barrel shelf and the Power generation row — where someone read an
owner off the table and shipped it without checking the mod could express the thing. The row is
deleted rather than handed to anyone.

## What is deliberately left open

**The Chemical Dissolution Chamber.** ADR-0021 hangs uranium's sulfuric-acid gate on it, arguing that
"uranium already sits on Mekanism's dissolution tier, so the gate lands out of parts the pack had
already bought". That argument is spent — with the ore chain cut, the gate is the block's *only*
remaining job, and whether the block survives now depends entirely on whether uranium does.

That is `#89`'s question, and it is not answered here. Until it resolves, the Dissolution Chamber is
neither cut nor placed on a row: ADR-0021's uranium recipe stands as written, and `#89` inherits a
clean lever — cut uranium and the block goes with it, keep uranium and the block needs a home under
**Chemistry** with a recorded note that it leaches rather than multiplies.

## Considered Options

- **Keep 5x, cut nothing.** Rejected on fidelity, and on the ledger: `docs/factorio-mechanics.md`
  already carried *"No ore multiplication"* and *"Ore smelts one-to-one straight to plate"* as
  `planned` sub-rules under Smelting, marked `all bodies`. The pack had been leaning here for a while
  without anyone deciding it.
- **Cut 5x only**, or **cut Mekanism's ladder and keep Create's 2x**. Both argued above.
- **Replace the ladder with mining-productivity research.** Deferred, not rejected on merit — the
  lab cannot express levelled research today, and Terra should not be compensated for its scarcity
  in any case.
- **Terra-only, revisited per body.** Rejected: it leaves five Mekanism blocks in the manifest,
  shown-and-refused in EMI (`#45`), and hands every later body an argument nobody has had.

## Consequences

- **Mekanism's Terra footprint shrinks to almost nothing** — the Energized Smelter as `#91`'s
  Electric Furnace, chemistry where the oil chapter does not reach, fluid logistics and storage, the
  upgrade economy `#42` spends in the `production` pack, and the Dissolution Chamber if `#89` keeps
  it. This is a real cost and it is accepted: `#91` already judged the Electric Furnace tier stays
  Mekanism's regardless, on the recipe type rather than on footprint.
- **Ore throughput is now purely a mining problem**, which is what ADR-0020 wanted. The binding
  constraint on a mature Terra factory is raw ore, and there is no machine that eases it.
- **Recipe removal is wide.** Every ore recipe on four Mekanism machines, the Enrichment Chamber's
  ore rows, and Create's Crushing Wheel and Millstone ore rows. The blocks mostly survive; the
  recipes do not.
- **`docs/factorio-mechanics.md`'s two Smelting sub-rules move from `planned, subject to #69` to
  settled**, and the Modules and beacons row stays `blocked` — this ADR does not decide it, and
  `#120` still owns the retrofit-tradeoff argument.
- **AlmostUnified is unaffected.** ADR-0017 restricts it to raw materials; with no multiplication
  chain there are fewer dusts to unify, but the restriction's reasoning is untouched.
