---
status: provisional
supersedes: [148, 178]
---

# The pack moves to Minecraft 26.1.2, and Oritech is its one tech mod

Two surveys, `docs/research/oritech-coverage.md` and `docs/research/simplebelts-coverage.md`, each
priced a hypothesis without deciding it. The first asked what Oritech could carry if the machines were
its. The second asked what SimpleBelts could carry if Create's belts went. Together they found that the
pack could lose Create entirely. **And once Create goes, nothing ties the pack to Minecraft 1.21.1.**
This ADR adopts both hypotheses and the version change they make possible.

## The rule

**The pack moves to Minecraft 26.1.2 on NeoForge. Its mechanic-bearing palette becomes:**

| role | owner |
| --- | --- |
| machines, the energy layer, fluid logistics | **Oritech**, the one third-party tech mod |
| trains | **Railcraft Reborn** |
| belts, loaders and the splitter | **SimpleBelts, forked by the pack**, which makes it the pack's fourth fork |
| everything no mod carries at Factorio's numbers | `planetaryfactory_core` |

**Leaving:** Create, Create: Power Grid, Modern Industrialization and GCyR. GregTech was already
leaving under ADR-0056.

**Staying:** Researchd and Respoiled, both forks and both ported by the pack. Building Gadgets 2,
FTB Filter System, FTB Quests, KubeJS, Block Runner, and plumbing that has no mechanic of its own.

## Why 26.1.2

**Nothing forces 1.21.1, and 1.21.1 is an old version.** The only thing tying the pack to it was
Create: its GitHub carries `mc1.21.1/*` branches and nothing later. SimpleBelts removes Create's last
job, so the tie is gone.

The two mods the pack now builds on are *developed* on 26.1.2. SimpleBelts' 1.21.1 line ended at
`v0.2.2`, and its work happens on the `26.1.2` branch. Oritech 2.0 and its Space Age addon exist only
there. A fork taken from the 1.21.1 line would start on a frozen branch.

**The probe says the move is real.** `../pf2612` loads Oritech 2.0.0-exp6, SimpleBelts 2.0.0-exp1,
Railcraft Reborn 1.4.3, Building Gadgets 2, FTB Filter System, FTB Quests and KubeJS 8.0.6 together. A
world was created in it on 2026-09-11 (human). The log shows four errors, none fatal: KubeJS's
Architectury plugin, a Configured config provider, EMI's Oritech recipe defaults, and a data map naming
`oritech:fluxite`.

**None of the forks' dependency chains blocks the move:**

- **Researchd** requires only Porting Dead Libs, and PDL's `main` (1.1.16) targets `[26.1,26.2)`.
  Upstream Researchd is still on 1.21.1, and the pack's fork was never submitted upstream, so the
  pack does the port.
- **Respoiled** upstream has a `multi/26.1` branch, which is a reference for porting the Decay fork.
- **`planetaryfactory_core`** requires `gtceu` and `researchd`. The first leaves under ADR-0056.

## Why Oritech, and not Modern Industrialization

**This is a choice, not something the version forced.** MI has a `port/26.1` branch targeting 26.1.2.
Its last commit was 2026-04-29 and it has no release, so MI could plausibly follow the pack. It leaves
anyway.

**One third-party tech mod, not two.** ADR-0017's rule is one owner per capability, with the losing
blocks recipe-removed. ADR-0035 is what that rule looks like applied over time: Mekanism's rows were
taken back one at a time until nothing was left. MI as the chassis plus Oritech as anything else would
restart that attrition.

**Oritech carries more of the ledger than MI's chassis does.** The Oritech survey's tally has Oritech
carrying the energy layer, the module system and most machine bodies, with every Factorio machine one
subclass away (its fact 9). It **unblocks** Modules and beacons, and it reopens the reactor's neighbour
bonus. MI was adopted by ADR-0056 for its recipe lookup, and that lookup is the one thing the Oritech
survey rebuilds in a subclass: first match plus output locking (its fact 3).

## Why a SimpleBelts fork, and not Create's belts

ADR-0044 kept Create because the puzzle Factorio's belt carries is mostly two-dimensional, and that
argument still holds. **Undergrounds and lanes stay `excluded`, argued from the medium.** What changed
is that the other claims the belt makes become *reachable*, which Create's RPM-driven belt never
managed:

- **Throughput becomes a known number.** The fork's belts are set to carry 15 / 30 / 45 / 60 items/s,
  which is Factorio's own figure. ADR-0044 deferred the throughput budget to play because Create's
  items-per-entry was "whatever the upstream inserter happened to hand over". The fork clamps that at
  one item per entry plus the researched bonus.
- **The belt holds 512 items per 64 blocks**, Factorio's number, so the belt as buffer is restored.
- **Splitters build balancers.** The fork adds a two-wide splitter and merger, so a balancer is
  constructed out of splitters, which Create's one-block Brass Tunnel never allowed.
- **`logistics-2`, `logistics-3` and `turbo-transport-belt` buy something again**, namely the belt
  tiers.

These rulings from the SimpleBelts survey are part of this decision:

- **Cost per length is mandatory:** one belt item per block, and a belt that costs one item for any
  length does not ship.
- **There is no inserter.** A belt's ends load and unload it, through the **loader**, and tier-1
  loaders stand in for the burner inserter.
- **Loaders from tier 2 up draw FE per item moved**, anchored on the fast and bulk inserters and derived
  by simulation, never transcribed. This is a balance cost.
- **The splitter draws no power.**
- **The tap is postponed.**

## What this supersedes and amends

- **ADR-0023 has nothing left to pin.** ADR-0056 already expired its constraint, and KubeJS 8 is what
  26.1.2 runs.
- **ADR-0044 is superseded.** Its analysis of the two-dimensional puzzle is kept and cited above. Its
  conclusion, its Create dials and its `maxBeltLength` go.
- **ADR-0056's chassis clause is superseded.** *GregTech leaves* stands, and so does its list of what
  that departure costs. *Modern Industrialization becomes the machine chassis* does not.
- **ADR-0057 is superseded.** Its account of what unification reaches remains correct history. With
  Create and MI both gone it arbitrates between nothing, and **AlmostUnified leaves**. What replaces it
  is ADR-0017's own rule: the item-layer decision names one supplier per part and recipe-removes the
  others.
- **ADR-0017's table is amended row by row.** Item logistics goes to the fork and Railcraft, fluid
  logistics to Oritech, power generation to Oritech plus the pack's engine, and the machine chassis to
  Oritech. The rule itself is unchanged, and it is the reason for this ADR's one-tech-mod choice.
- **ADR-0035's energy reasoning is reversed; its removal of Mekanism is not.** FE was demoted because
  nothing distributed it. With GregTech, Power Grid and Create gone, there is no EU, no volts and no
  rotation, and **FE is the pack's only energy currency**. ADR-0036's pole distributes it once it
  stops asking for GregTech's capability.
- **ADR-0048's rotation clause falls.** Its only argument for a steam engine that emits rotation was
  ADR-0036's choice of Power Grid. The engine emits electricity, which is the Factorio-faithful reading
  ADR-0048 turned down only because of that choice.
- **ADR-0018 rung 2's "movement at scale", Create 6 packages,** loses its mechanism. What `logistic`
  science buys at rung 2 goes back to #25.

`#148` chose Create: Power Grid and `#178` answered "no" to Factorio belts; both are contradicted.
`#102` is open and is left to the frontier: its answer becomes *the loader*.

## What it costs

**Porting three forks is the real cost:**

- **Researchd** needs a port onto PDL's 26.1 line.
- **Respoiled** needs a port with upstream's `multi/26.1` as a reference.
- **The core** has ten classes on the old item, fluid and energy capability API, which NeoForge 26.1
  replaced with the transfer API, plus the pole on GregTech's energy capability, plus Minecraft 26.1's
  renames everywhere else.

**Every generator and every asset check is written against 1.21.1's data formats.** Recipe
ingredients, item model definitions, loot tables and worldgen all changed shape after 1.21.1. The
converters regenerate their output, but the formats they write and the hops the static checks walk
are re-derived, not carried over.

**Several lines are deleted outright, not ported:**

- the Create kinetic recipe line (`create-recipe-convert.py`, `data/pack/create-substitutions.json`,
  `test_create_recipes.py`)
- the Power Grid recipe line (`powergrid-recipe-convert.py`, `data/pack/grid-substitutions.json`,
  `test_grid_recipes.py`)
- the GCyR fork, with its ADR-0001/0003 build and patch

**Four pillars are pre-releases, pinned by exact version:**

- **Oritech 2.0.0-exp6.** The Oritech survey's citations are at 1.2.12, and its *Java on Oritech*
  level couples the core to `block.base.entity`, which is not Oritech's API.
- **SimpleBelts 2.0.0-exp1**, the fork's base.
- **Railcraft Reborn 1.4.3.**
- **EMI**, which is an unofficial, unstable port. The Personal Assembler's Fill Recipe runs through it.
  JEI has an official 26.1.2 build and is already an optional dependency of the core, so it is the
  fallback, not a replacement chosen here.

**Mods that don't make the move:**

- AE2 and Sophisticated Backpacks, which aren't faithful to Factorio
- Tree Harvester, since the pack fells trees itself (ADR-0051)
- AlmostUnified, as above
- ProbeJS, which agent-driven development does not need
- GCyR, together with its rockets, platforms and planets. The pack is still on Terra, so nothing it
  plays today goes with GCyR.

## What is decided here and what is not

**Decided:**

- the version, 26.1.2
- the palette and its owners
- the mods that leave
- FE as the single energy currency
- the steam engine emits electricity
- the belt rulings above

**Not decided. Each needs its own ADR or ticket:**

- **The joules-per-FE constant.** Every Factorio wattage the Oritech survey marks reachable depends on
  it, and so does the loader's charge.
- **The item layer**, meaning which mod supplies each part of ADR-0021's alphabet. Railcraft Reborn
  ships plates and gears, among them `iron_plate`, `copper_plate` and `steel_plate`. Oritech ships
  ingots and dusts but no plates. Both ship `steel_ingot` and `nickel_ingot`. The overlap
  AlmostUnified would have arbitrated is real, and small.
- **Power between areas and energy storage.** These were Power Grid's catenary and Battery.
  ADR-0036's brownout propagation and wire-tier ladder were hard requirements satisfied by Power Grid,
  and whether they survive is open.
- **Recipe selection on Oritech's machines.** It is first match with no lock. The Oritech survey's
  subclass route is the candidate. ADR-0056's slot-locking conclusions described MI.
- **Item bulk storage, barrelling and rung 2's logistics clause**, all of which Create carried and
  which are unowned.
- **Interplanetary travel.** GCyR leaves. Whether Oritech: Space Age (0.1.0, no dimensions) moves the
  row is a survey of its own.
- **The outfield veins' placer**, which ADR-0056 already left open.
- **The re-read of the Oritech survey at 2.0.** Only its item capability was re-read (SimpleBelts
  survey, fact 3).

## Considered alternatives

- **Drop Create and stay on 1.21.1**, with Oritech 1.2.12 and a fork of SimpleBelts 0.2.2. The belt
  decision does not require the version change. Rejected because nothing requires staying either: both
  mods develop on 26.1.2, the belt fork would start from a frozen line, and the pack is pre-release,
  so no world depends on the old version.
- **Keep Create for belts and trains, on 1.21.1** (ADR-0044 and ADR-0056 as written). Rejected on the
  belt argument above. A throughput nobody can compute, no buffer and no constructed balancer are
  defects the fork closes, and Railcraft Reborn carries trains without Create.
- **Move to 26.1.2 with MI as the chassis, if MI's port lands.** Rejected on ADR-0017's rule: a second
  full-stack tech mod beside Oritech restarts the row-by-row attrition ADR-0035 records.

## Status

Provisional under ADR-0042. It is argued from two surveys and one world load, not from a rung played
end to end. It is promoted when Terra's rung 0 has been played on 26.1.2.
