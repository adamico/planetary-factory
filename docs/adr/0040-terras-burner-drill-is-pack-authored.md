---
status: accepted
supersedes: []
---

# Terra's burner drill is pack-authored, and extraction stops being GregTech's end to end

ADR-0017 gave GregTech extraction and said so without qualification: "The extraction ladder is
GregTech's end to end — LP Steam Miner at rung 0, Basic Ore Drilling Rig, Advanced Ore Drilling
Rig, one rig per rung, each granted by a science tier." That sentence is now false at rung 0, on
purpose.

**Factorio's opening machine is a burner mining drill: it burns solid fuel, it is one entity, and
it is the first thing a player builds.** GregTech has no solid-fuel miner. Its extraction line
starts at the LP Steam Miner, which takes steam piped in from a boiler — two blocks, a fluid
connection, and a fuel that is not the coal in your hand. Fidelity to Factorio's opening and
GregTech's ownership of extraction could not both hold, and the opening won.

So: **`planetaryfactory_core` registers a burner mining drill, and it is rung 0's only drill. The
LP Steam Miner leaves the pack.** GregTech keeps the electric ladder — `electric-mining-drill` is
`gtceu:lv_miner` at rung 1 — and keeps the fluid rig.

## Why the Steam Miner goes rather than sitting alongside

Keeping both would give rung 0 two drills and a within-rung upgrade beat: burner first, steam as
the thing that earns the boiler and teaches piping. It is Factorio's own shape and it was the
tempting answer.

It is cut because **rung 0 is budgeted 3–4 hours** and is already the chapter carrying the
Personal Assembler, the furnace, belts, the first circuits and the walk to the Lab. A second drill
whose only lesson is "now with pipes" is the cheapest beat to lose. Cutting it also disposes of the
boiler, which had no other justification left: #37 removed GregTech's power layer and ADR-0036 put
the grid on a pack-authored pole, so the steam drill was the last block in the pack that wanted
steam.

## What the drill is

- **A first-party block in `planetaryfactory_core`.** ADR-0015 puts mechanism in the mod, and a
  fuel-burning miner is mechanism. The mod already registers blocks, block entities and menus — the
  supply-area pole, the furnace ladder, the trees — so this is existing ground.
- **It burns solid fuel.** No steam, no EU, no FE. That is the entire point of the block.
- **It breaks real ore blocks in an area beneath it**, which is not a choice this ADR makes:
  ADR-0020 already settled that depletion *is* physical block removal, and observed that a GT Miner
  scans for ore blocks rather than consulting the vein registry. The pack-authored drill does what
  the GT one does.
- **It carries two numbers, operations per second and yield per operation**, with yield fixed at
  1.0. See "Both rigs ship both numbers" below.
- **It does not output onto a belt.** See the excluded sub-rule below.

## Both rigs ship both numbers, and yield stays dormant

Factorio separates *depletion* from *yield*: an operation takes one unit out of a tile and pays out
an amount that a productivity bonus can raise. Vulcanus's Big Mining Drill is the payoff — 50% more
yield per operation — and it is a body this pack has committed to.

**So the burner drill and `gtceu:lv_miner` both carry an explicit yield-per-operation of 1.0**,
even though nothing on Terra varies it. A dormant field a later body sets is cheap; retrofitting a
second quantity into a mechanism that welded operations to payouts is not.

The amount a tile holds, and the model that makes an operation meaningful at all, is #176 — split
out of #105 because it binds hand-mining and every ore block on every body, not just these two
rigs.

## Terra's ladder is two rigs, not three

ADR-0017's "one rig per rung" reads as three rigs over five rungs, which was never literal. Terra
gets two, matching Factorio's own Nauvis:

| Rung | Drill | Source |
| --- | --- | --- |
| 0 | Burner Mining Drill | `planetaryfactory_core`, this ADR |
| 1 | `gtceu:lv_miner` (Basic Miner) | GregTech, `electric-mining-drill` |

ADR-0017's third rig is Vulcanus's **Big Mining Drill** — planet-locked, that body's puzzle, not
Terra's ladder. Rung 1 is where the electric rig lands because rung 1 is where electricity lands
(ADR-0018: "Electricity, and the first assembler"); the rig is the pole's second customer and
arrives with the thing that powers it.

## Drills do not output onto a belt

Factorio's defining drill behaviour is that output drops straight onto a belt with no inserter.
GT miners mine to an internal inventory and push nothing, and the pack-authored drill does the
same. The player places a Create funnel.

Recorded as `excluded` on the ledger's Mining drills row with the funnel named in the notice. It is
a genuine loss of Factorio feel. The alternative — a rig that auto-pushes into an adjacent belt —
is a mechanism with no home, on the critical path, bought to save one block the player is learning
at rung 0 anyway.

## The burner tier is adapted, not excluded

The ledger's **Burner tier before electric** sub-rule was `unargued`. It closes `adapted`: the tier
exists and is Factorio's own block rather than GregTech's steam stand-in. The fidelity that remains
missing is nothing — this is the one row where authoring bought back full fidelity rather than
approximating it.

## Consequences

- **ADR-0017's Mining automation row is amended**, not struck: GregTech owns the *electric*
  extraction ladder and the fluid rig; rung 0's burner drill is the pack's.
- **`#100`'s pocket swaps** the LP Steam Miner for the burner drill, and
  `docs/spec/terra-progression.md`'s rung 0 grants change with it. Beat 4 — "Place the Furnace and
  the Steam Miner from your pocket" — names the burner drill.
- **`boiler` loses its last justification.** The row is not decided here; what is recorded is that
  the reason it was being kept alive is gone.
- **The player is not expected to hand-mine ore for long.** A burner drill covers four tiles and
  beats hands even at Factorio's 0.25 items/s, which is why it is in the pocket rather than
  crafted. The Engineer's Pick remains the tool for wood, dirt, stone and dismantling machines —
  ADR-0039 stands, but its rung-1 justification for `steel-axe` names the deleted Steam Miner and
  needs rewording (#176).

## Considered alternatives

- **A GT-chassis machine taught to burn coal.** The pack already registers machines on a GregTech
  chassis (ADR-0026), so the shape exists. Rejected: it fights the chassis for the one property
  that makes the block interesting, since GT chassis machines are EU and steam consumers.
- **Another mod's burner miner, admitted past ADR-0034's sweep.** Rejected: it re-opens an
  ownership row ADR-0017 closed, to obtain one block.
- **Keep the LP Steam Miner and call it the burner tier.** This was the recommendation the grilling
  session opened with, and it was overruled. GT spells burner as steam, and the two-block steam pair
  is not the self-fuelled entity Factorio's opening is built around. The pack authors first-party
  when fidelity demands it — ADR-0039's picks are the precedent.
