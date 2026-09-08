---
status: provisional
supersedes: [92, 101]
---

# Terra's steam is two pack-owned fluids, and the chain to the grid is pack-authored

Factorio's opening power chain is four entities and one fluid: an offshore pump lifts water, a
boiler burns fuel and heats it to 165 °C steam, a steam engine consumes that steam and emits
electricity, and poles carry it. Steam is a **real, pipeable, buffered fluid** with a temperature,
and that is not decoration — it is what makes power a production chain rather than a slider, and
what lets a nuclear reactor at 500 °C reuse the same pipes and a different consumer.

The ledger has recorded a four-step adaptation of that chain since `#101`: *GregTech's boiler,
Create's Steam Engine, Power Grid's generation multiblock, the pack's Steam Turbine.* Grilled
2026-09-08, **the second step of that chain does not exist**.

## Three facts from the jars

- **Create has no steam fluid.** Its boiler is a Fluid Tank multiblock holding **water**, heated by
  Blaze Burners; `create.boiler.*` is a heat level and a water input rate. The Steam Engine mounts
  on that tank and emits rotation. It cannot consume a steam fluid from a pipe, from GregTech's
  boiler or from anything else. The ledger's "a Create Steam Engine turns that steam into rotation"
  was never implementable.
- **Power Grid never touches steam either.** Its generator is not a fixed multiblock but a built
  assembly — a Stator of Coils strung on Shafts, an Armature of Rotors, a Commutator and a
  Generator Clutch — coupled to a Create kinetic network through the Clutch, needing an excitation
  current or self-excitation with a Rheostat. **Rotation in, volts out**, and nothing else.
- **The gap is therefore steam → rotation**, and no installed mod spans it.

## The decision

**Two pack-owned fluids.** Low-temperature steam, which the Boiler makes and the Steam Engine eats;
high-temperature steam, which ADR-0033's reactor emits and only the Steam Turbine takes. Two fluids
rather than one fluid carrying a temperature, because Factorio has exactly two temperatures with
exactly two consumers, and a registry entry expresses that without inventing per-bucket state.

They are `planetaryfactory:`, not GregTech's. This **corrects the ledger's claim that superheated
steam is "its own GT material"**: `gtceu:steam` is not inert — GregTech's own steam machines accept
it, and admitting it re-opens the power layer `#37` removed. The pack's Steam Turbine is already
pack-authored (ADR-0033); a pack machine is authored against a pack fluid.

**One boiler tier**, pack-authored: solid fuel in, water in, low-temperature steam out, under
ADR-0047's rule — `fuel_value * effectivity` joules into a buffer, drained at the prototype's own
`energy_usage`. It is the third customer of the burner model the Furnace and the Burner Mining Drill
already share, and it replaces the `boiler` row's LP Solid Boiler. There is no second tier: Factorio
reaches 500 °C through the heat exchanger, and ADR-0033 already decided the reactor emits
superheated steam directly with no heat layer, so `heat-exchanger` stays where `#135` has it.

**One steam engine**, pack-authored: low-temperature steam in, **Create rotation out**. Not
electricity.

## Why rotation and not electricity, which is what Factorio does

The Factorio-faithful reading is a steam engine that emits electricity straight into a supply-area
pole — the inverse of the pole's own scan, and exactly Factorio's entity. It is rejected, and the
reason is that **the fidelity price was already paid deliberately and cannot be spent twice.**

ADR-0036 chose Power Grid against an acceptance test fixed before the research: brownout propagation
and a wire-tier ladder, both hard requirements. It bought a nodal solver with real voltage drop,
per-material wire gauge, fuses and grounding, and it dropped the per-area power cap on the argument
that **the limit becomes emergent** — sag and blown fuses — with `#150`'s current-vs-Imax overlay as
the teaching device. It also refused a machine-side buffer so that sag would reach the machines.

A steam engine that emits electricity into a pole routes around every one of those. The player
powers the factory with engines and poles, never places a wire, and the mechanic ADR-0036 selected
the mod for has no subject left. Shipping **both** is worse than either: a wireless engine makes the
wired path a strictly harder route to the same electricity, and nobody builds it twice.

So the chain is **Boiler → low-temperature steam → pack Steam Engine → SU → Power Grid generator
assembly → pole → machines**, and the pack owns its first two steps and its third block.

## What this costs

**The mod takes a Create compile-time dependency.** Rotation has no capability path; a generating
kinetic block extends Create's own class. ADR-0043 avoided this dependency and ADR-0044 kept Create's
belts without needing it. It is taken here knowingly, as the `create-*.jar` line in
`mod/build.gradle`'s `compileOnly fileTree` — the same shape as GTCEu's and Researchd's.

**`offshore-pump` becomes `not_emitted`.** Create's Mechanical Pump against a vanilla water source is
the water half, and there is no Factorio entity to author. `pumpjack` is unaffected — it is oil, and
stays on its own shelf.

**Rung 0 keeps its shape and loses its block identities.** `docs/spec/terra-progression.md` already
says rung 0 is "burner and kinetic, and not one watt anywhere", with steam becoming rotation that
"powers machines, not a grid", and the grid arriving at rung 1 with its first customer. That survives
intact — what changes is that all three of rung 0's named blocks (LP Solid Boiler, LP Steam Miner,
Create's Steam Engine) are now pack-authored or deleted. The player meets Power Grid's excitation
and rheostat at rung 1 with the book in hand, not in the first hour.

**Rung 0's budget is not adjusted on this account.** A player fluent in Factorio reaches electricity
there in an hour or two; the spec puts the grid three to four hours in. ADR-0018's budget is
explicitly for a first-time player of *this pack*, who is learning blocks nobody has shown them, and
the gap is that learning rather than slack to cut. `#170`'s one clean pace reading is what settles
it; it should not read its own budget as a failure before then.

## Provisional

Under ADR-0042: nobody has boiled water here. The chain is argued from the jars and from Factorio's
prototypes, and not one block of it has been placed.
