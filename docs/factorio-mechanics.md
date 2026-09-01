# The Factorio mechanic ledger

Every mechanic Factorio has — base game and Space Age — and what this pack does about it.

**Row keys are Factorio's own names** (`Gleba`, not Sapros; `Vulcanus`, not Ignus). This is a declared
exception to `CONTEXT.md`'s _Avoid_ lists, on the same footing as `data/factorio/*.json`: the ledger's
value is being diffable against Factorio, so it must speak Factorio. The pack's name for the same
thing appears in `where`. See ADR-0028.

**This file is not derived from `data/pack/subgroup-owner.json` and does not derive it.** That file
answers _is a Factorio recipe emitted, and on whose machine_. This one answers _does the mechanic
exist in the pack, by any means_. **`not_emitted` is never evidence for `excluded`** — `combat/defensive-structure`
is `not_emitted` and the pack still ships a Radar (#57).

**This file does not place anything on a progression ladder.** A row of `planned` says the pack has
the mechanic; where on Terra's ladder it lands is #25's call, and it may land nowhere near Terra.

## Verdicts

| verdict | meaning |
| --- | --- |
| `planned` | in, not built |
| `shipped` | in, built — registered *and* its warranted check under `docs/testing/what-to-check.md` passing |
| `adapted` | in, but Minecraft's shape differs. Mandatory `notice` sentence |
| `blocked` | wanted, no known implementation |
| `excluded` | deliberately not reproduced. Requires a written reason |

`owner` is an ADR/issue link where the decision was already made, `unargued` where this ledger is the
first place it has been written down, or `by-consequence` where it fell out of a decision about
something else and was never argued on its own merits. **`by-consequence` rows are owned by this
ledger**, not by the ticket that caused them.

`via` reuses `subgroup-owner.json`'s owner tokens — `gregtech`, `create`, `mekanism`, `electro`,
`pack`, `kubejs`, `native_mechanic` — and a value must exist in `index.toml`. `candidates` is free
text and commits to no jar; **`pack` is admissible as a candidate only with a named mechanism**
(ADR-0015).

## Summary

### Base game

| mechanic | verdict | where |
| --- | --- | --- |
| [Resource patches and finite ore](#resource-patches-and-finite-ore) | `shipped` | Terra, Ignus, Sapros |
| [Manual mining](#manual-mining) | `adapted` | all bodies |
| [Mining drills](#mining-drills) | `planned` | all bodies |
| [Fluid handling](#fluid-handling) | `planned` | all bodies |
| [Oil processing](#oil-processing) | `planned` | Terra, Ignus, Gelida |
| [Smelting](#smelting) | `planned` | all bodies |
| [Assembling machines and recipe categories](#assembling-machines-and-recipe-categories) | `planned` | all bodies |
| [Handcrafting and the crafting queue](#handcrafting-and-the-crafting-queue) | `planned` | all bodies |
| [Transport belts](#transport-belts) | `adapted` | all bodies |
| [Inserters](#inserters) | `adapted` | all bodies |
| [Logistic robots](#logistic-robots) | `excluded` | — |
| [Construction robots and blueprints](#construction-robots-and-blueprints) | `adapted` | all bodies |
| [Trains](#trains) | `planned` | Terra |
| [Circuit network](#circuit-network) | `adapted` | all bodies |
| [Electric network and transmission](#electric-network-and-transmission) | `adapted` | all bodies |
| [Power generation](#power-generation) | `planned` | all bodies |
| [Nuclear fission](#nuclear-fission) | `adapted` | Terra |
| [Pollution](#pollution) | `planned` | all bodies |
| [Enemies and evolution](#enemies-and-evolution) | `planned` | Terra |
| [Combat: guns, ammo, turrets, walls](#combat-guns-ammo-turrets-walls) | `excluded` | — |
| [Armor and the equipment grid](#armor-and-the-equipment-grid) | `excluded` | — |
| [Capsules](#capsules) | `excluded` | — |
| [Modules and beacons](#modules-and-beacons) | `blocked` | — |
| [Research and science packs](#research-and-science-packs) | `planned` | all bodies |
| [The technology tree](#the-technology-tree) | `shipped` | pack-wide |
| [Rocket silo and rocket launch](#rocket-silo-and-rocket-launch) | `planned` | all bodies |
| [Personal transport](#personal-transport) | `blocked` | — |
| [Terrain modification](#terrain-modification) | `excluded` | — |
| [Repair and entity damage](#repair-and-entity-damage) | `excluded` | — |
| [Radar and map exploration](#radar-and-map-exploration) | `planned` | Terra |
| [The logistic request and trash system](#the-logistic-request-and-trash-system) | `excluded` | — |
| [Day and night cycle](#day-and-night-cycle) | `shipped` | Terra, Sapros |

### Space Age

| mechanic | verdict | where |
| --- | --- | --- |
| [Interplanetary travel](#interplanetary-travel) | `planned` | pack-wide |
| [Space platforms](#space-platforms) | `planned` | Terra Orbit and every orbit |
| [Asteroid mining and reprocessing](#asteroid-mining-and-reprocessing) | `planned` | orbits |
| [Interplanetary logistics](#interplanetary-logistics) | `planned` | pack-wide |
| [Spoilage](#spoilage) | `adapted` | Sapros, pack-wide |
| [Quality](#quality) | `blocked` | — |
| [Recycling](#recycling) | `planned` | Electro |
| [Vulcanus: lava and calcite](#vulcanus-lava-and-calcite) | `planned` | Ignus |
| [Fulgora: scrap and lightning](#fulgora-scrap-and-lightning) | `planned` | Electro |
| [Gleba: agriculture and nutrients](#gleba-agriculture-and-nutrients) | `planned` | Sapros |
| [Aquilo: cold and ammonia](#aquilo-cold-and-ammonia) | `planned` | Gelida |
| [Planet-locked buildings](#planet-locked-buildings) | `planned` | all bodies |
| [Elevated rails](#elevated-rails) | `excluded` | — |
| [Fusion power](#fusion-power) | `planned` | Gelida |
| [The Shattered Planet](#the-shattered-planet) | `blocked` | Atlantis |

---

## Base game

### Resource patches and finite ore

- **verdict**: `shipped`
- **where**: Terra, Ignus, Sapros
- **via**: `gregtech`
- **owner**: ADR-0007, ADR-0019, ADR-0020, ADR-0021

GregTech ore veins in chunk-aligned disc patches, asserted by `scripts/worldgen-check.py` against
`tests/worldgen/expected.json`.

Sub-rules:

- **Patches are finite and run out** — `shipped`. ADR-0020: the fix for exhaustion is another planet.
- **Ore is prospected, not stumbled on** — `adapted`. ADR-0019; a Factorio player reads a patch off
  the map, a player here reads surface indicators and later an Ore Finder satellite.
- **Infinite late-game resource (oil-style yield decay)** — `blocked`. No mechanic in the stack
  models a patch that decays to a floor rather than to zero.
- **Resource richness varies per patch** — `unargued`, no verdict. Nobody has thought about it.

### Manual mining

- **verdict**: `adapted`
- **notice**: mining is a Minecraft block break, so it is per-block and tool-tiered rather than a
  hold-to-mine timer against a patch total.
- **where**: all bodies
- **via**: `native_mechanic`
- **owner**: `unargued`

### Mining drills

- **verdict**: `planned`
- **where**: all bodies
- **via**: `gregtech`
- **owner**: ADR-0017
- **ticket**: #105

Sub-rules:

- **Burner tier before electric** — `unargued`, no verdict.
- **Drills output onto a belt directly** — `unargued`, no verdict.
- **A body-locked large drill** (Vulcanus's Big Mining Drill) — `planned`, see [Planet-locked buildings](#planet-locked-buildings).

### Fluid handling

- **verdict**: `planned`
- **where**: all bodies
- **via**: `create`
- **owner**: ADR-0017 as amended by #101 (Create owns fluid handling entire — pipes and pumps for
  moving, tanks for storing; Mekanism has no fluid role)
- **ticket**: #106

Sub-rules:

- **Barrelling and unbarrelling** — `shipped` as `native_mechanic`; `subgroup-owner.json`'s barrel
  shelves emit nothing because the mechanic already works (#93).
- **Fluid mixing is forbidden in a pipe network** — `excluded`. `by-consequence`: no mod in the stack
  enforces single-fluid pipe networks, and adding it would be a pack mechanism nobody asked for.
- **Pumps and flow rate over distance** — `unargued`, no verdict.

### Oil processing

- **verdict**: `planned`
- **where**: Terra, Ignus, Gelida
- **via**: `pack`
- **owner**: ADR-0025 (the Oil Refinery and Chemical Plant are pack-authored GT machines)
- **ticket**: #107

Sub-rules:

- **Basic then advanced oil processing** — `planned`.
- **Cracking to resolve the three-output imbalance** — `planned`. The chapter's whole puzzle.
- **Coal liquefaction** — `planned`, on Ignus (`docs/planets.md`).

### Smelting

- **verdict**: `planned`
- **where**: all bodies
- **via**: `undecided` — #91 decides which pack block each of Factorio's three furnace tiers is
- **owner**: #91
- **ticket**: #91

Sub-rules:

- **Ore smelts one-to-one straight to plate, with no intermediate step** — `planned`. Recorded in
  `subgroup-owner.json`; the pack does not get to add a hop.
- **No ore multiplication** — `planned`, settled by ADR-0032: cut pack-wide, Mekanism's ladder and
  Create's rung-0 Crushing Wheels alike. Yield gain by research or module is `blocked`, not
  `excluded` — the lab cannot express levelled research (ADR-0022 prunes 106 such technologies) and
  Terra is deliberately not compensated for its scarcity (ADR-0020). See #120.

### Assembling machines and recipe categories

- **verdict**: `planned`
- **where**: all bodies
- **via**: `pack`
- **owner**: ADR-0026, ADR-0029
- **ticket**: #87 (the machines are registered; the recipe conversion is not)

Three pack-authored Assembling Machines on a GT chassis. Recipe routing follows Factorio's own
`category` (ADR-0021), not the owning mod.

Sub-rules:

- **`crafting_speed` as a machine property** — `planned`. ADR-0029 puts it on the machine, at
  Factorio's raw values (0.5 / 0.75 / 1.25), which is what makes `energy_required x 20` produce
  Factorio's own felt durations. It is also the only thing that makes the three tiers differ, since
  overclocking never fires above base tier.
- **`energy_usage` as a machine property** — `planned`. ADR-0029 emits no `EUt` on a recipe at all;
  a machine modifier supplies it, scaled so the Oil Refinery's 420 kW lands on LV's 32 EU/t.
- **Machine idle draw** — `excluded`. A Factorio machine consumes power while idle: the
  [Electric system](https://wiki.factorio.com/Electric_system) page notes *"an active assembling
  machine 2 will consume 155 kW (150 kW energy consumption + 5 kW drain)"*, about a thirtieth of the
  draw, and the engine default is `energy_usage / 30` since no crafting machine sets the field.
  GregTech has no equivalent -- an idle GT machine consumes nothing -- and reproducing it means real
  idle draw built in `planetaryfactory_core` for a lesson (*don't over-build*) that ore depletion
  (ADR-0020) and Emission already teach more cheaply. Folding it into `EUt` is worse than either: it
  looks like fidelity and behaves as a flat tax. Called **idle draw** in pack prose, never "drain",
  which `CONTEXT.md` owns for an unrelated Sapros mechanic.

### Handcrafting and the crafting queue

- **verdict**: `planned`
- **where**: all bodies
- **via**: a mod of its own, not yet written
- **owner**: `docs/gdd.md` §5
- **ticket**: #98, #99, #100

The crafting grid stays, so early handcrafting is instant rather than queued. Factorio's *queue* —
select a recipe, wait, collect — survives only as the Personal Assembler's bootstrap tier, covering
the components of the first machines: the things with nowhere to go once their recipes leave the grid
and before any machine exists to make them.

**The Assembler ships as its own mod**, not as pack scripting. Note that `docs/gdd.md` §5 still
describes it as a KubeJS implementation — an FTB Library UI, a `persistentData` queue and a
`PlayerEvents.tick` engine — and ADR-0015's ownership table decides where pack content lives. Moving
it to a mod is a decision neither document records yet.

**The queue's slowness is serial, not a multiplier.** The character prototype sets no
`crafting_speed` at all -- it is not a crafting machine -- so Factorio hand-crafting runs at exactly
`energy_required` seconds. What makes it slow is that the queue is serial: one craft at a time, no
modules, no parallelism. `#95` already gave the Personal Assembler a timed queue, so the pack
reproduces the mechanism rather than approximating it with a penalty, and ADR-0029 gives the
Assembler speed 1 with durations of `energy_required x 20` unmodified.

### Transport belts

- **verdict**: `adapted`
- **notice**: there is one belt and you buy its speed with RPM, so the belt ladder is a
  power-and-gearing problem rather than three tiers and two research nodes — and with no underground
  belts, no lanes and tunnels in place of splitters, none of the routing patterns a Factorio player
  has memorised transfer.
- **where**: all bodies
- **via**: `create`
- **owner**: ADR-0017, #93

Sub-rules:

- **Three belt tiers** — `adapted`. There is one belt, and its throughput is the RPM you drive it
  at, so belt speed is a power-and-gearing decision made per run rather than three craftable tiers
  bought from the tech tree. Faster belts are therefore never a research unlock here.
- **Underground belts** — `excluded`. Create has no belt that runs under an obstacle, and the
  routing puzzle underground belts create — weaving two lanes past each other in a fixed footprint —
  has no substitute here.
- **Splitters, with filtering and priority** — `adapted`. Create's tunnels are the splitter: a
  tunnel splits a belt's output across the belts beside it and filters what goes where. It is
  placed on the belt rather than spliced into it, so the balancer built out of splitter pairs — the
  shape a Factorio player reaches for first — is not buildable, and there is no output priority.
- **Two lanes per belt** — `excluded`. `by-consequence`: Create belts have no lane model, and the
  whole lane-balancing idiom goes with it.

Together these empty out Factorio's belt research. `logistics-2`, `logistics-3` and
`turbo-transport-belt` survive in `data/factorio/technology.json`, and between them they buy exactly
a belt tier, an underground belt and a splitter tier — all three now excluded or bought with RPM
instead. Only `logistics` is declared in `researchd.js` today; the other three are candidates for the
prune, which is #25's call and not this ledger's.

### Inserters

- **verdict**: `adapted`
- **notice**: Create funnels and arms move items between inventories, but there is no swing-arm reach
  across a belt, no long-handed tier, and no stack-size bonus research.
- **where**: all bodies
- **via**: `create`
- **owner**: ADR-0017, #93

The notice above is written against funnels and chutes. **#102 asks whether Create's Mechanical Arm
is the inserter instead** — an Arm is a swing arm, which is a much closer fit — and will rewrite this
row's losses to whatever actually survives.

### Logistic robots

- **verdict**: `excluded`
- **where**: —
- **owner**: ADR-0017

ADR-0017 gives item logistics to Create and cuts the dedicated routing mods, because a substitute
routing idiom is a straight bypass of the ladder. AE2 is the one gated exception, unlocked at endgame
once every planet's puzzle is done — it is not a logistic-robot analogue and is not this row.

### Construction robots and blueprints

- **verdict**: `adapted`
- **notice**: you copy a shape and paste it yourself, paying for it out of your own inventory on the
  spot — there is no ghost to leave behind, nothing builds it while you are elsewhere, and nothing
  rebuilds or repairs it later.
- **where**: all bodies
- **via**: `native_mechanic` (Building Gadgets 2)
- **owner**: `unargued`

Building Gadgets 2 is installed (`mods/building-gadgets.pw.toml`, indexed) and is the pack's closest
thing to a blueprint: copy a region, paste it elsewhere. That covers the *shape* half of a blueprint
and none of the *logistics* half.

**Create's Schematicannon is not this row.** It is vanilla Create and therefore already in the pack,
but a Schematicannon prints a structure block-by-block from a chest at a fixed position — it is a
building tool with a hopper, not a construction network, and the two mechanics are not
interchangeable with Factorio's.

Sub-rules:

- **Copy a built shape and stamp it down again** — `adapted`, via Building Gadgets.
- **A blueprint is an item you can hand to another player, or keep in a library** — `unargued`,
  no verdict.
- **Pasting leaves ghosts that something else fills in** — `excluded`. This is the half that makes
  blueprints a logistics mechanic rather than a building tool, and nothing in the stack has it.
- **Construction robots build, repair and rebuild from a roboport's range** — `excluded`.
  `by-consequence` of [Logistic robots](#logistic-robots): ADR-0017 cuts the routing mods, and a
  construction network is that decision applied to building. It is also, with no biters, a network
  with nothing to repair — see [Repair and entity damage](#repair-and-entity-damage).
- **Deconstruction planner** — `unargued`, no verdict.

### Trains

- **verdict**: `planned`
- **where**: Terra
- **via**: `create`
- **owner**: ADR-0017 (Create owns schedule-based rail networks)
- **ticket**: #108

Sub-rules:

- **Schedules and stations** — `planned`. Create Trains have both.
- **Rail signals and block-based traffic** — `adapted`. Create resolves train conflicts itself; there
  is no signal to place and no deadlock to debug.
- **Train limits at a station** — `unargued`, no verdict.

### Circuit network

- **verdict**: `adapted`
- **notice**: the wires are redstone, so a signal is a strength from 0 to 15 on a block-to-block
  circuit rather than a named channel on a coloured wire — there is no reading a whole belt's contents
  off one wire, and no arithmetic on a signal beyond what a comparator does.
- **where**: all bodies
- **via**: `native_mechanic`, `create`
- **owner**: ADR-0030

**Factorio's circuit network is Minecraft's redstone system**, and this row belongs to redstone
rather than to a missing mod. Vanilla supplies the wire, the comparator, the repeater and the
observer; **Create ships its own redstone line on top** — Redstone Link, Powered Latch, Pulse
Repeater, Threshold and Stockpile Switches, Smart Observer, Display Link and Nixie Tubes — which
between them cover most of what Factorio's combinators, lamps and display panels are for.

An earlier version of this row read `blocked` on the grounds that no installed mod owns a circuit
network. That was a category error: it looked for one mod's capability and missed the mechanic
sitting in the base game. ADR-0030 records the decision and that lesson.

Sub-rules:

- **Read a machine's or container's contents as a signal** — `adapted`. Comparators and Create's
  Stockpile Switch, per container, rather than one wire carrying every item type at once.
- **Combinator logic — arithmetic, decider, constant** — `adapted`. Create's latches, switches and
  gearshifts plus vanilla redstone logic. Arithmetic on a signal is the weakest part of the
  substitution.
- **Wireless signal over distance** — `shipped`, and better than Factorio's: Create's Redstone Link
  needs no wire and no relay, where Factorio needs a wire or a radar-linked circuit.
- **Lamps and display panels as readouts** — `adapted`. Nixie Tubes and the Display Link.
- **Two independent networks on one wire (red and green)** — `excluded`. Redstone has one channel;
  the whole trick of running two circuits down one pole has no analogue.
- **Circuit-controlled inserters and belts** — `unargued`, no verdict, and it depends on #102's
  answer about the Mechanical Arm.

**The supply question is separate and still open.** #58 cut redstone from Terra entirely — no vein,
empty `underground_ores` step — so the mechanic exists while its crafting material does not, and
#62 already records the same problem hitting the authored green circuit. That is a resource question
for #25, not a verdict on the mechanic, and the two were previously conflated in this row.

`subgroup-owner.json` still parks `logistics/circuit-network` `undecided` with a note guessing
`not_emitted`. **That is the other axis and this row does not settle it**: whether Factorio's eight
combinator-and-lamp recipes are emitted is a routing decision, and a mechanic supplied by vanilla and
Create needs no emitted recipe to exist.

### Electric network and transmission

- **verdict**: `adapted`
- **notice**: the grid is a modelled electrical system rather than an abstract pool — poles carry a
  real voltage over wire with a real gauge, the run loses power over distance, and a bad circuit
  damages components instead of merely underfeeding them.
- **where**: all bodies
- **via**: `electro`, `mekanism`
- **owner**: ADR-0017

**Create: Electro Energetics owns the grid** — poles, wire and catenary — and GregTech's power layer
was removed entire to make room for it, cables included. Mekanism's Universal Cables distribute
inside an area, which is the one seam: the grid moves power between places, Mekanism moves it inside
one.

The mod runs at **shipped physics defaults** (ADR-0017), which is the decision this row turns on:
voltage drop, per-material wire gauge, grounding, fuses, brownouts and component damage are all on.
Flattening resistance to the config floor would delete the wire-tier ladder that is the reason to
adopt the mod at all.

An earlier version of this row named GregTech and called brownout `excluded` on the grounds that GT
machines stall rather than derate. Both halves were wrong — GT has no power layer here, and the mod
that replaced it models brownouts natively.

Sub-rules:

- **Brownout: insufficient supply degrades what is running** — `shipped`. On by default, and it is
  the read-the-graph-and-add-generation loop Factorio teaches, arriving with more electrical detail
  rather than less.
- **Voltage tiers** — `adapted`. Factorio steps low to medium to high voltage at the transformer;
  Electro's ladder is wire gauge and material, so upgrading a run means rewiring it rather than
  swapping a pole tier.
- **Power poles have a supply area and a wire reach** — `planned`.
- **Transformers between voltage levels** — `planned`. Kept craftable early because Transformer Oil
  is seed oil and renewable, on the one-way rule that it must never become an input to the oil
  chapter (ADR-0017).
- **A separate FE side, bridged by a Converter** — `adapted`, and a pack addition Factorio has no
  need for: Factorio has one kind of electricity and this pack has two, so the Converter is a
  boundary the player must learn.
- **The power graph as a diagnostic surface** — `unargued`, no verdict.

### Power generation

- **verdict**: `planned`
- **where**: all bodies
- **via**: `electro`, `create`, `mekanism`
- **owner**: ADR-0017 as amended by #101 (Electro owns steam and solar). **`mekanism` appears in
  `via` only on the strength of an unargued clause — #104 decides whether the pack has an at-scale
  generation tier at all, and if not, `via` is `electro` alone.**
- **ticket**: #104

Sub-rules:

- **Boiler and steam engine as the first power** — `adapted`. The chain is three steps, not two:
  a Create Steam Engine burns fuel and emits SU, an Electro Alternator turns SU into watts, and the
  grid carries them. A Factorio player's boiler-and-engine pair has a rotational stage wedged in the
  middle of it, and the grid is granted at a rung rather than arriving with the first fire.
- **Solar panels and accumulators** — `planned`, Electro's outright, and Electro's identity.
- **Steam as a stored, pipeable intermediate** — `planned`.

### Nuclear fission

- **verdict**: `adapted`
- **notice**: fission is Mekanism's reactor, so it is a built multiblock with its own meltdown and
  waste model rather than Factorio's tile-and-neighbour puzzle — you engineer one reactor instead of
  laying out many.
- **where**: Terra
- **via**: `mekanism`
- **owner**: #89

**This verdict is provisional and #89 may retire it.** Mekanism's fission reactor is what the pack
has today, and it is a whole chapter rather than a gap. What has no owner is the chain around it:
#58 gives uranium's first step one — Factorio's sulfuric-acid gate as an authored
`mekanism:dissolution` recipe at rung 3 — but nothing owns enrichment, fuel cells or reprocessing,
ADR-0017 has no fission row, and `subgroup-owner.json` defers four recipes to #89. #89 decides
whether the nuclear chapter ships at all, and if it does not, this row becomes `excluded`.

Sub-rules:

- **Kovarex enrichment** — `blocked`.
- **Reactor neighbour bonus** — `excluded`. `by-consequence` of adopting a multiblock reactor: there
  is nothing to place next to anything, so the layout puzzle has no board.
- **Heat pipes and heat exchangers as a separate transport network** — `blocked`, deferred to #89
  with the rest of the chapter.

### Pollution

- **verdict**: `planned`
- **where**: all bodies
- **via**: `kubejs`
- **owner**: ADR-0005
- **ticket**: #109

GTCEu 7.0.2 has no pollution system — the mod contains nothing matching `pollut` — so Emission is
ours and none of it is built yet.

Its shape already differs from Factorio's in a way worth recording before it lands: Emission is
scored per chunk off the **EU/t draw of running GT machines**, not per recipe, so a machine idling is
free and there is no pollution-per-craft number on a recipe tooltip. Power draw is the one number
every GT machine already exposes, which is why no per-recipe tagging is needed. Expect this row to
become `adapted` with that as its notice once it ships.

Sub-rules:

- **Spread to neighbouring chunks, and decay over time** — `planned`. Both, and they are what makes
  outpost placement a decision.
- **Absorption by terrain and trees** — `unargued`, no verdict.
- **Per-planet consequences** — `blocked`. Named in principle, unspecified everywhere but Terra;
  migrated here out of `docs/gdd.md` §8.

### Enemies and evolution

- **verdict**: `planned`
- **where**: Terra
- **via**: `kubejs`, `native_mechanic`
- **owner**: `docs/gdd.md` §6
- **ticket**: #110

Nothing here is built. The intended shape, for the same reason as [Pollution](#pollution): emission
attracts **Illager raids to an Overseer at your outpost**, not biters out of a nest you can go and
clear — no nest to destroy, no expansion, no evolution factor. Vanilla raid pathfinding is the
substrate, and raids are Terra's alone.

Sub-rules:

- **Pollution triggers attacks** — `planned`.
- **Attacks are state until a player is present** (the Dormant Siege) — `planned`. No Factorio analogue;
  a pack addition that exists because chunks unload.
- **Nests, expansion and clearing territory** — `excluded`. `by-consequence`: raids need no nest, so
  the offensive half of Factorio's enemy loop has nowhere to attach.
- **Evolution factor rising with pollution and time** — `blocked`.
- **Gleba's pentapods** — `unargued`, no verdict. `docs/planets.md` marks them TBD.

### Combat: guns, ammo, turrets, walls

- **verdict**: `excluded`
- **where**: —
- **owner**: `by-consequence`

**The canonical `by-consequence` row.** #26 dropped Military science because its ingredients feed
nothing downstream, and seven `combat/*` shelves went `not_emitted` behind it — turrets, guns, ammo,
armor, capsules, equipment and walls. Nobody decided this pack has no combat; a science-pack pruning
decided it for them.

Owned by this ledger, not handed back to #26. Whether the pack has turret defence at all is #118.

Note that `not_emitted` did **not** settle the shelf: `combat/defensive-structure` is `not_emitted`
and #57 still shipped a Radar. That is the proof case for the two axes never reading each other.

### Armor and the equipment grid

- **verdict**: `excluded`
- **where**: —
- **owner**: `by-consequence`

Same #26 cascade. Partially contradicted already: MekaSuit is the spacesuit (`docs/gdd.md` §1), and a
MekaSuit *is* an equipment grid with modules in it. So the mechanic arguably ships under another
name, which is exactly the kind of thing a ledger is for. Flagged rather than resolved.

### Capsules

- **verdict**: `excluded`
- **where**: —
- **owner**: `by-consequence`

Same #26 cascade.

### Modules and beacons

- **verdict**: `blocked`
- **where**: —
- **owner**: `unargued`

`production/module` is `undecided` in `subgroup-owner.json` on one recipe, `beacon`. Factorio's module
system has no pack analogue; #42 names a Mekanism upgrade in the `production` pack's slot list, which
is a data point and not an answer. Follow-on: #120.

Speed/productivity/efficiency as a three-way tradeoff you retrofit into an existing factory is a large
part of Factorio's mid-game, and nothing in the stack reproduces it. `blocked`, not `excluded` — the
argument has not been had.

### Research and science packs

- **verdict**: `planned`
- **where**: all bodies
- **via**: `pack`, `kubejs`
- **owner**: ADR-0018, ADR-0022
- **ticket**: #66, #82, #103

Four packs plus an unscienced rung 0, gated by Researchd's Research Lab, fed by pipe and consumed
unattended.

Sub-rules:

- **Each pack rung grants a capability the next rung physically requires** — `planned`. ADR-0018.
- **Military science** — `excluded`. #26; see [Combat](#combat-guns-ammo-turrets-walls) for what went
  with it.
- **Sapros's science pack spoils** — `planned`. The buffer-as-liability puzzle.
- **Research consumes packs continuously while running** — `adapted`. Researchd's Lab consumes on
  completion of a pack batch rather than metering a rate; only `consumePack` reads the Lab.
- **A lab draws power, so research competes with the factory for it** — `blocked`, #103. Researchd's
  Lab has no energy handler at all — no class in the jar carries the concept — so research is free of
  the grid, and being free of the grid it also emits nothing (ADR-0005 scores EU/t draw), which makes
  researching the one industrial activity on Terra with no hazard consequence.

### The technology tree

- **verdict**: `shipped`
- **where**: pack-wide
- **via**: `kubejs`
- **owner**: ADR-0022

The tree's topology is extracted from Factorio rather than transcribed —
`data/factorio/technology.json` is committed, `researchd.js` declares each node with
`fromFactorio(...)`, and `tests/factorio/test_tech_extract.py` asserts the pruned tree is still a
valid tree and that every declared name exists. Registered, and the check its claim warrants passes.

Sub-rules:

- **Prerequisites form a DAG the player navigates** — `shipped`.
- **Infinite research tiers with escalating cost** — `excluded`. `by-consequence`: the extraction
  prunes them, and nothing downstream wants them.
- **Research triggers (SA: unlock by doing, not by paying)** — `unargued`, no verdict.

### Rocket silo and rocket launch

- **verdict**: `planned`
- **where**: all bodies
- **via**: `electro` (GCyR)
- **owner**: #41, ADR-0006
- **ticket**: #25 — the map *is* this row's ticket, being Terra's flow to the first rocket launch

Sub-rules:

- **The launch is a physical, watchable event** — `planned`. GCyR's `RocketEntity`, and
  `docs/gdd.md` §4 makes it explicit that the launch is the payoff and is never simulated.
- **Rocket parts are produced continuously and buffer in the silo** — `unargued`, no verdict.
- **Cargo landing pad** — `planned`, the post-launch arc.

### Personal transport

- **verdict**: `blocked`
- **where**: —
- **owner**: `unargued`

`logistics/transport` is `undecided` on one recipe, `car`. Personal transport is not an ADR-0017
capability and no rung grants it. Factorio's car, tank and spidertron have no pack answer, and
Minecraft's own movement options (elytra, horses, boats) are neither gated nor factory-produced.
Follow-on: #121.

### Terrain modification

- **verdict**: `excluded`
- **where**: —
- **owner**: `by-consequence`, and ADR-0019 for landfill specifically

Sub-rules:

- **Landfill** — `excluded`. ADR-0019 makes Terra flat and sea-bearing; landfill has no meaning there.
  This one *is* argued.
- **Cliffs and cliff explosives** — `excluded`. `by-consequence`: no body generates cliffs as an
  obstacle, so nothing needs removing.
- **Concrete and its speed bonus** — `excluded` as a mechanic. The item is on `logistics/terrain`,
  `undecided`, and `create:mixing` fits it by shape; the walking-speed bonus has no analogue.

### Repair and entity damage

- **verdict**: `excluded`
- **where**: —
- **owner**: `by-consequence`

`production/tool` is `undecided` on one recipe, `repair-pack`, with the reason stated plainly:
nothing on Terra takes damage the way a Factorio entity does. With no biters attacking buildings, the
whole repair loop has nothing to repair.

### Radar and map exploration

- **verdict**: `planned`
- **where**: Terra
- **via**: `pack`, `electro`
- **owner**: #57
- **ticket**: #116

#57 decided the Radar — a pack machine on a GT chassis with its own research node — and closing that
ticket is not the same as the mechanic being in a player's hands.

**This row is the proof case for the two axes never reading each other.** `combat/defensive-structure`
is `not_emitted` in `subgroup-owner.json`, and a ledger that read its verdicts out of that file would
have recorded "radar: excluded" — which is wrong whatever this row's verdict turns out to be.

Sub-rules:

- **Radar reveals map, and periodically scans distant chunks** — `adapted`. It finds ore patches;
  there is no fog of war to lift, because Minecraft has no map fog in Factorio's sense.
- **Orbital scanning as the mid-game upgrade** — `planned`. GCyR's Ore Finder satellite.

### The logistic request and trash system

- **verdict**: `excluded`
- **where**: —
- **owner**: `by-consequence`

Follows [Logistic robots](#logistic-robots): personal logistic requests and auto-trash are the bot
network's player-facing half, and they go with it.

### Day and night cycle

- **verdict**: `shipped`
- **where**: Terra, Sapros
- **via**: `native_mechanic`
- **owner**: `unargued`

Sub-rules:

- **Solar output follows the cycle, and accumulators bridge the night** — `planned`, and it is
  Electro's identity. Electro's own cycle is `unargued`.

---

## Space Age

### Interplanetary travel

- **verdict**: `planned`
- **where**: pack-wide
- **via**: `electro` (GCyR)
- **owner**: ADR-0001, ADR-0006, `docs/gdd.md` §2
- **ticket**: #112, #54

Six bodies, seven destinations.

Sub-rules:

- **Each planet is a distinct surface with its own resources and its own puzzle** — `planned`.
- **The player travels physically and pays fuel** — `planned`. GCyR's tiered fuel costs.
- **Arrival is hostile and you must establish a foothold** — `adapted`. The Vanguard Kit pastes a
  beachhead; Factorio drops you into a working platform's cargo pod, so the shape of the first five
  minutes differs entirely.

### Space platforms

- **verdict**: `planned`
- **where**: Terra Orbit, and every body's orbit
- **via**: `electro` (GCyR space stations)
- **owner**: ADR-0006
- **ticket**: #113

A Platform is a static orbital factory, not a ship — no thrusters, no navigation, no interplanetary
transit, and therefore no asteroid defence and no hull mass to manage. That is an argued divergence
(ADR-0006) rather than an unbuilt one, so expect this row to become `adapted` with it as the notice
once Platforms exist.

Sub-rules:

- **A platform is built outward from a starter foundation** — `planned`.
- **Platforms fly between planets** — `excluded`. ADR-0006; this is the argued core of the adaptation.
- **Asteroid collision damages the platform, and it must shoot back** — `excluded`.
  `by-consequence` of static platforms.
- **Cargo travels by platform between planets** — `adapted`. See
  [Interplanetary logistics](#interplanetary-logistics); the pack's cargo is a Flight timer instead.

### Asteroid mining and reprocessing

- **verdict**: `planned`
- **where**: orbits
- **via**: `pack`
- **owner**: `docs/gdd.md` §3, Map #25 (out of scope for the first arc)
- **ticket**: #114

Sub-rules:

- **Collectors harvest passing asteroid chunks** — `planned`. Ice and carbon at Terra Orbit.
- **Crushers break chunks into resources** — `planned`.
- **Reprocessing converts chunk types into one another, closing the loop** — `unargued`, no verdict.
  This is what makes the asteroid economy an economy rather than a drip, and nobody has thought about
  it.
- **Asteroid composition varies by orbit and by route** — `unargued`, no verdict.

### Interplanetary logistics

- **verdict**: `planned`
- **where**: pack-wide
- **via**: `pack`
- **owner**: `docs/gdd.md` §4
- **ticket**: #111

Launch Terminals, Receiving Terminals and Drop Hatches as pack-authored GT machines, with unattended
cargo held as a Flight with a travel timer rather than as a moving entity.

Sub-rules:

- **Requesting from another planet, and cargo arriving unattended** — `planned`.
- **Orbit-to-surface drops are cheap and immediate** — `adapted`. Free and instant here; Factorio
  still pays a pod.
- **A drop with no receiver leaves a container to collect** — `planned`. Drop Pods.
- **Localized assembly forces on-site factories** — `planned`. Factorio's planet-locked buildings,
  generalised to components.

### Spoilage

- **verdict**: `adapted`
- **notice**: freshness is item identity with coarser stages rather than a continuously ticking
  percentage, so a stack does not have one blended freshness value and a conveyor of half-spoiled
  goods does not exist.
- **where**: Sapros, and any body holding its outputs
- **via**: `pack` (the Decay fork)
- **owner**: ADR-0010, ADR-0011

Sub-rules:

- **Spoiled results are themselves an input** — `planned`. Biosulfur from spoilage.
- **Spoilables cannot be parked in digital storage** — `planned`. ADR-0013; a pack addition with no
  Factorio equivalent, because Factorio has no AE2.
- **Freshness survives being processed** — `planned`. ADR-0010.
- **Chunk unload does not pause the clock** — `adapted`. ADR-0012: catch-up is sampled, not replayed.

### Quality

- **verdict**: `blocked`
- **where**: —
- **owner**: `unargued`

Five tiers of every item, quality modules, the recycler-plus-quality loop, and legendary as the
end state. Nothing in the stack has an item-quality axis, and bolting one on would touch every
recipe in the pack. `blocked` and not `excluded`: this is one of Space Age's three headline
mechanics and its absence has never been argued.

### Recycling

- **verdict**: `planned`
- **where**: Electro
- **via**: `create`
- **owner**: `docs/gdd.md` §2, `docs/planets.md`
- **ticket**: #13

Sub-rules:

- **Scrap recycles into a spread of unrelated outputs, and the surplus is the puzzle** — `planned`.
  Create crushers on generated ruins.
- **Any item can be recycled back into a quarter of its ingredients** — `blocked`, following
  [Quality](#quality); without quality the universal recycler has no second purpose.
- **Voiding the surplus is a legitimate answer** — `unargued`, no verdict.

### Vulcanus: lava and calcite

- **verdict**: `planned`
- **where**: Ignus
- **via**: `pack`, `gregtech`
- **owner**: `docs/planets.md`
- **ticket**: #12, then that body's `Puzzle:` ticket

Sub-rules:

- **Lava is an infinite fluid resource** — `planned`. Needs a mechanism to treat lava as infinite.
- **Molten metal as a fluid intermediate, and the foundry** — `planned`.
- **Sulfuric acid geysers, and acid neutralisation to water** — `planned`.
- **Demolishers as territorial obstacles** — `blocked`. No enemy model outside Terra, and this is the
  one place Space Age puts a boss between you and a resource.

### Fulgora: scrap and lightning

- **verdict**: `planned`
- **where**: Electro
- **via**: `create`, `electro`
- **owner**: `docs/planets.md`
- **ticket**: #13, then that body's `Puzzle:` ticket

Sub-rules:

- **No natural ore; everything comes from scrap** — `planned`. ADR-0009, ADR-0016.
- **Lightning damages what is not protected, and can be harvested** — `planned`.
- **Islands constrain buildable space** — `planned`.
- **Holmium and the electromagnetic plant** — `planned`.

### Gleba: agriculture and nutrients

- **verdict**: `planned`
- **where**: Sapros
- **via**: `pack`
- **owner**: `docs/planets.md`, ADR-0016
- **ticket**: #23, then that body's `Puzzle:` ticket

Sub-rules:

- **Crops are farmed and replanted, not mined** — `planned`.
- **Nutrients as a consumable that machines eat** — `unargued`, no verdict. The biochamber's whole
  economy hangs on it.
- **Metal arrives by bacteria that spoil into ore** — `planned`. ADR-0016; no veins on Sapros.
- **Pentapods and eggs that hatch if you stall** — `unargued`, no verdict. TBD in `docs/planets.md`.

### Aquilo: cold and ammonia

- **verdict**: `planned`
- **where**: Gelida
- **via**: `pack`, `gregtech`
- **owner**: `docs/planets.md`
- **ticket**: #15, then that body's `Puzzle:` ticket

Sub-rules:

- **Fluids freeze without active heating, so every process carries a thermal budget** — `planned`.
- **Heating towers and heat distribution** — `planned`.
- **Ammonia chemistry, lithium brine, fluorine** — `planned`.
- **Cryogenic plant and quantum processors** — `planned`.

### Planet-locked buildings

- **verdict**: `planned`
- **where**: all bodies
- **via**: `kubejs`
- **owner**: `docs/gdd.md` §4, `docs/planets.md`
- **ticket**: #115

Foundries and big drills on Ignus, electromagnetic plants on Electro, biochambers on Sapros,
cryogenic plants on Gelida — craftable only where they belong, which is what forces a factory on
every planet instead of one factory and a shipping lane.

### Elevated rails

- **verdict**: `excluded`
- **where**: —
- **owner**: `by-consequence`

Follows [Trains](#trains): Create trains already route in three dimensions without a dedicated
elevated-rail tier, so the mechanic has nothing to add.

### Fusion power

- **verdict**: `planned`
- **where**: Gelida
- **via**: `mekanism`
- **owner**: `docs/planets.md`
- **ticket**: #15, then Gelida's `Puzzle:` ticket

Fusion generator and reactor, craftable only on Gelida.

### The Shattered Planet

- **verdict**: `blocked`
- **where**: Atlantis
- **owner**: `docs/gdd.md` §8, migrated here

A named, orbit-only endgame destination with no defined puzzle, resource or attrition model.
Migrated out of the GDD's Open Questions.

Sub-rules:

- **Promethium science and the final research tier** — `blocked`.
- **A one-way journey of escalating attrition** — `blocked`, and `by-consequence` of static platforms:
  the journey *is* a platform flight, and ADR-0006 has no flying platform.

---

## Follow-on tickets

Load-bearing `by-consequence` and `blocked` rows get their own `Grilling:` issue rather than being
settled inside a row. Filed:

- #119 — where does redstone come from, now that #58 has cut it from Terra and the circuit network
  needs it? A resource question for #25; the row stays `adapted` whatever the answer.
- #118 — does the pack have combat — biters, turrets, walls — or did Military science take them?
- #120 — modules and beacons, and whether the retrofit-tradeoff mid-game exists here at all.
- #121 — personal transport.
- #122 — quality, and whether an item-quality axis is affordable at all.
- #131 — the Shattered Planet: is Atlantis a mechanic, or a name on the map? The attrition half is
  `by-consequence` of ADR-0006's static platforms, so what is open is whether the destination
  survives without the journey.

## Every `planned` row has a ticket

A `planned` row says the pack has the mechanic and has not built it. Without a ticket that is
indistinguishable from having forgotten it, so **every `planned` row carries a `ticket` field** and
that is an invariant of this file: promoting a row to `planned` means filing something, or pointing
at what already exists.

Four rows point at a body ticket *and then* at a `Puzzle:` ticket that does not exist yet. That is
deliberate and not a gap — the GDD's delivery sequence cuts a body's `Puzzle:` ticket only **after
that body ships**, so writing them now would be inventing content for terrain nobody has built.

## How this stays honest

Convention plus discoverability. A body or puzzle ticket updates its own rows, and the CLAUDE.md skill
entry is what makes an agent find this file. **No automated check** — per `docs/testing/what-to-check.md`
this is a design ledger making no runtime claim, and a test here would be testing prose.
