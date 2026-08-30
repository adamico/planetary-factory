# The Factorio mechanic ledger

Every mechanic Factorio has — base game and Space Age — and what this pack does about it.

**Row keys are Factorio's own names** (`Gleba`, not Sapros; `Nauvis`, not Terra). This is a declared
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
| [Handcrafting and the crafting queue](#handcrafting-and-the-crafting-queue) | `adapted` | all bodies |
| [Transport belts](#transport-belts) | `planned` | all bodies |
| [Inserters](#inserters) | `adapted` | all bodies |
| [Logistic robots](#logistic-robots) | `excluded` | — |
| [Construction robots and blueprints](#construction-robots-and-blueprints) | `blocked` | — |
| [Trains](#trains) | `planned` | Terra |
| [Circuit network](#circuit-network) | `blocked` | — |
| [Electric network and transmission](#electric-network-and-transmission) | `planned` | all bodies |
| [Power generation](#power-generation) | `planned` | all bodies |
| [Nuclear fission](#nuclear-fission) | `blocked` | Terra |
| [Pollution](#pollution) | `adapted` | all bodies |
| [Enemies and evolution](#enemies-and-evolution) | `adapted` | Terra |
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
| [Radar and map exploration](#radar-and-map-exploration) | `shipped` | Terra |
| [The logistic request and trash system](#the-logistic-request-and-trash-system) | `excluded` | — |
| [Day and night cycle](#day-and-night-cycle) | `shipped` | Terra, Sapros |

### Space Age

| mechanic | verdict | where |
| --- | --- | --- |
| [Interplanetary travel](#interplanetary-travel) | `planned` | pack-wide |
| [Space platforms](#space-platforms) | `adapted` | Terra Orbit and every orbit |
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

Sub-rules:

- **Basic then advanced oil processing** — `planned`.
- **Cracking to resolve the three-output imbalance** — `planned`. The chapter's whole puzzle.
- **Coal liquefaction** — `planned`, on Ignus (`docs/planets.md`).

### Smelting

- **verdict**: `planned`
- **where**: all bodies
- **via**: `undecided` — #91 decides which pack block each of Factorio's three furnace tiers is
- **owner**: #91

Sub-rules:

- **Ore smelts one-to-one straight to plate, with no intermediate step** — `planned`. Recorded in
  `subgroup-owner.json`; the pack does not get to add a hop.
- **No ore multiplication** — `planned`, subject to #69.

### Assembling machines and recipe categories

- **verdict**: `planned`
- **where**: all bodies
- **via**: `pack`
- **owner**: ADR-0026

Three pack-authored Assembling Machines on a GT chassis. Recipe routing follows Factorio's own
`category` (ADR-0021), not the owning mod.

### Handcrafting and the crafting queue

- **verdict**: `adapted`
- **notice**: the crafting grid stays, so early handcrafting is instant rather than queued; the timed
  queue survives only as the Personal Assembler's bootstrap tier, which has nothing left to make once
  real machines exist.
- **where**: all bodies
- **via**: `kubejs`
- **owner**: `docs/gdd.md` §5

### Transport belts

- **verdict**: `planned`
- **where**: all bodies
- **via**: `create`
- **owner**: ADR-0017, #93

Sub-rules:

- **Three belt tiers** — `adapted`. There is one belt, and its throughput is the RPM you drive it
  at, so belt speed is a power-and-gearing decision made per run rather than three craftable tiers
  bought from the tech tree. Faster belts are therefore never a research unlock here.
- **Underground belts** — `unargued`, no verdict.
- **Splitters, with filtering and priority** — `adapted`. Create's item filtering is the substitute;
  priority has no analogue.
- **Two lanes per belt** — `excluded`. `by-consequence`: Create belts have no lane model, and the
  whole lane-balancing idiom goes with it.

### Inserters

- **verdict**: `adapted`
- **notice**: Create funnels and arms move items between inventories, but there is no swing-arm reach
  across a belt, no long-handed tier, and no stack-size bonus research.
- **where**: all bodies
- **via**: `create`
- **owner**: ADR-0017, #93

### Logistic robots

- **verdict**: `excluded`
- **where**: —
- **owner**: ADR-0017

ADR-0017 gives item logistics to Create and cuts the dedicated routing mods, because a substitute
routing idiom is a straight bypass of the ladder. AE2 is the one gated exception, unlocked at endgame
once every planet's puzzle is done — it is not a logistic-robot analogue and is not this row.

### Construction robots and blueprints

- **verdict**: `blocked`
- **where**: —
- **owner**: `unargued`

Wanted — blueprint-and-paste is a large part of what late Factorio *feels* like — and no mod in the
stack provides it. Create's Schematicannon is the nearest thing in the mod ecosystem but is not
installed, and installing it would be an ADR-0017 capability decision.

### Trains

- **verdict**: `planned`
- **where**: Terra
- **via**: `create`
- **owner**: ADR-0017 (Create owns schedule-based rail networks)

Sub-rules:

- **Schedules and stations** — `planned`. Create Trains have both.
- **Rail signals and block-based traffic** — `adapted`. Create resolves train conflicts itself; there
  is no signal to place and no deadlock to debug.
- **Train limits at a station** — `unargued`, no verdict.

### Circuit network

- **verdict**: `blocked`
- **where**: —
- **owner**: `by-consequence`

No ADR-0017 row, no installed mod owns it, and #58 cut redstone from Terra entirely — no vein, empty
`underground_ores` step — so the idiom has no source on the planet. `subgroup-owner.json` parks the
shelf `undecided` and says outright that the decision is not one that table can take.

Filed `blocked` rather than `excluded` deliberately: nobody has argued that this pack should not have
a circuit network. That argument is [a follow-on grilling ticket](#follow-on-tickets).

### Electric network and transmission

- **verdict**: `planned`
- **where**: all bodies
- **via**: `gregtech`
- **owner**: ADR-0017

Sub-rules:

- **Voltage tiers** — `adapted`. GregTech's EU tiers are far more granular than Factorio's flat grid,
  and stepping up a tier is a real gate rather than a wire.
- **Brownout: insufficient supply slows every consumer** — `excluded`. `by-consequence`: GT machines
  stall or explode rather than derate, and the whole read-the-graph-and-add-boilers loop goes with it.
- **The power graph as a diagnostic surface** — `unargued`, no verdict.

### Power generation

- **verdict**: `planned`
- **where**: all bodies
- **via**: `create`, `mekanism`, `electro`
- **owner**: ADR-0017 (a three-way split — Create generates first, Mekanism at scale, Electro carries
  its own)

Sub-rules:

- **Boiler and steam engine as the first power** — `planned`.
- **Solar panels and accumulators** — `planned`, and Electro's identity.
- **Steam as a stored, pipeable intermediate** — `planned`.

### Nuclear fission

- **verdict**: `blocked`
- **where**: Terra
- **owner**: #89

#58 gives uranium's first step an owner — Factorio's sulfuric-acid gate as an authored
`mekanism:dissolution` recipe at rung 3 — but nothing owns enrichment, fuel cells, reactors, heat
pipes or reprocessing, and ADR-0017 has no fission row. Carved out to #89, which decides whether the
nuclear chapter ships at all.

Sub-rules:

- **Kovarex enrichment** — `blocked`.
- **Reactor neighbour bonus** — `blocked`.
- **Heat pipes and heat exchangers as a separate transport network** — `blocked`.

### Pollution

- **verdict**: `adapted`
- **notice**: Emission is scored per chunk off the EU/t draw of running GT machines, not per recipe,
  so a machine idling is free and there is no pollution-per-craft number to read on a recipe tooltip.
- **where**: all bodies
- **via**: `kubejs`
- **owner**: ADR-0005

GTCEu 7.0.2 has no pollution system, so Emission is ours.

Sub-rules:

- **Spread to neighbouring chunks, and decay over time** — `planned`. Both, and they are what makes
  outpost placement a decision.
- **Absorption by terrain and trees** — `unargued`, no verdict.
- **Per-planet consequences** — `blocked`. Named in principle, unspecified everywhere but Terra;
  migrated here out of `docs/gdd.md` §8.

### Enemies and evolution

- **verdict**: `adapted`
- **notice**: pollution attracts Illager raids to an Overseer at your outpost, not biters out of a
  nest you can go and clear; there is no nest to destroy, no expansion, and no evolution factor.
- **where**: Terra
- **via**: `kubejs`, `native_mechanic`
- **owner**: `docs/gdd.md` §6

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

Owned by this ledger, not handed back to #26. Whether the pack has turret defence at all is
[a follow-on grilling ticket](#follow-on-tickets).

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
is a data point and not an answer.

Speed/productivity/efficiency as a three-way tradeoff you retrofit into an existing factory is a large
part of Factorio's mid-game, and nothing in the stack reproduces it. `blocked`, not `excluded` — the
argument has not been had. [Follow-on ticket](#follow-on-tickets).

### Research and science packs

- **verdict**: `planned`
- **where**: all bodies
- **via**: `pack`, `kubejs`
- **owner**: ADR-0018, ADR-0022

Four packs plus an unscienced rung 0, gated by Researchd's Research Lab, fed by pipe and consumed
unattended.

Sub-rules:

- **Each pack rung grants a capability the next rung physically requires** — `planned`. ADR-0018.
- **Military science** — `excluded`. #26; see [Combat](#combat-guns-ammo-turrets-walls) for what went
  with it.
- **Sapros's science pack spoils** — `planned`. The buffer-as-liability puzzle.
- **Research consumes packs continuously while running** — `adapted`. Researchd's Lab consumes on
  completion of a pack batch rather than metering a rate; only `consumePack` reads the Lab.

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
[Follow-on ticket](#follow-on-tickets).

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

- **verdict**: `shipped`
- **where**: Terra
- **via**: `pack`, `electro`
- **owner**: #57

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

Six bodies, seven destinations.

Sub-rules:

- **Each planet is a distinct surface with its own resources and its own puzzle** — `planned`.
- **The player travels physically and pays fuel** — `planned`. GCyR's tiered fuel costs.
- **Arrival is hostile and you must establish a foothold** — `adapted`. The Vanguard Kit pastes a
  beachhead; Factorio drops you into a working platform's cargo pod, so the shape of the first five
  minutes differs entirely.

### Space platforms

- **verdict**: `adapted`
- **notice**: a Platform is a static orbital factory, not a ship — no thrusters, no navigation, no
  interplanetary transit, and therefore no asteroid defence and no hull mass to manage.
- **where**: Terra Orbit, and every body's orbit
- **via**: `electro` (GCyR space stations)
- **owner**: ADR-0006

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
settled inside a row. Open candidates:

- Does the pack have a circuit network?
- Does the pack have combat — biters, turrets, walls — or did Military science take them?
- Modules and beacons, and whether the retrofit-tradeoff mid-game exists here at all.
- Personal transport.
- Quality, and whether an item-quality axis is affordable at all.

## How this stays honest

Convention plus discoverability. A body or puzzle ticket updates its own rows, and the CLAUDE.md skill
entry is what makes an agent find this file. **No automated check** — per `docs/testing/what-to-check.md`
this is a design ledger making no runtime claim, and a test here would be testing prose.
