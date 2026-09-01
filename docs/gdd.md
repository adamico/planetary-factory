# PlanetaryFactory — Game Design Document

A Minecraft 1.21.1 / NeoForge modpack reproducing the progression, logistics puzzles and
interplanetary scope of Factorio's Space Age expansion, built on a curated mod stack and bound
together by KubeJS into a stationary, automation-first loop.

This document describes intended design. Decisions that are hard to reverse are recorded as ADRs in
`docs/adr/`; domain vocabulary is defined in `CONTEXT.md` and used here verbatim.

## 1. Core Technology Stack

- **Create** — Mechanical logistics: bulk transport, item filtering, schedule-based rail networks.
- **GregTech CEu Modern (GT:M) 7.0.2** — Resource generation and custom multiblock machines. Chunk-aligned ore
  veins, bedrock fluid and ore extraction, multiblocks, and the machine and recipe-type registries
  the pack extends.
- **Mekanism** - normal resource processing, spacesuit via MekaSuit
- **Gregicality Rocketry (GCyR)** — Rockets, planets, orbit dimensions, space stations, satellites,
  oxygen and temperature systems. Built from source against GTCEu 7.0.2 (ADR-0001); the released
  jar is permanently incompatible. Stellaris is a disabled fallback, not part of the design
  (ADR-0002).
- **KubeJS 2101.7.1-build.181** — Scripting glue: custom GT machines and recipe types, emission
  tracking, flight simulation, planetary arrival. Pinned to the build GTCEu 7.0.2 compiles against;
  `kubejs-create` is not installed.
- **Pre-AE2 logistics — Create and Mekanism, and nothing else.** Create owns item logistics and
  bulk item storage; Create owns fluid handling too — pipes and pumps for moving, tanks for storing
  (ADR-0017 as amended by #101), and Mekanism has no fluid role. The
  dedicated routing mods — Modular Routers, Integrated Dynamics, LaserIO, XNet, SFM, Pipez, Flux
  Networks, Functional Storage and Sophisticated Storage — are cut from the pack, because one mod
  owns each capability and a substitute routing idiom is a straight bypass of the ladder. Only two
  routing capabilities are gated rather than cut: **AE2**, unlocked at endgame once every planet's
  puzzle is done, and **Create 6's package logistics**, granted at the `logistic` science rung.

## 2. The Solar System

Six bodies, seven destinations — Terra Orbit is not a planet in its own right but Terra's orbit,
as every body here has one. Internal identifiers use Factorio's names; the names players see are
Latin and Greek, supplied by lang files only (ADR-0004).

| Display name | ID | Dimension | Thematic puzzle | Core mechanics |
| --- | --- | --- | --- | --- |
| Terra | `overworld` | `minecraft:overworld` | Standard starter loop | Basic extraction, first automation, first rockets and the Orbital Starter Kit. The only body with Illager raids. |
| Terra Orbit | `overworld_orbit` | GCyR stock | Orbital logistics | Asteroid chunks (ice, carbon) processed into Space Science. Where the first Platform is established. |
| Ignus | `vulcanus` | New, based on GCyR's `venus` | Thermal and fluid processing | Tungsten, infinite lava extraction, molten metal solidification, strict slag management. |
| Electro | `fulgora` | New | Recycling and electricity | No natural ores. Create crushers process generated ruins for scrap. Solar and lightning power. |
| Sapros | `gleba` | New | Organics and spoilage | Agricultural automation under spoilage time limits. Sole source of Cryo-Pods. |
| Gelida | `aquilo` | New | Cryogenics and heat management | Fluids freeze without active heating; ammonia chemistry; every process needs a thermal budget. |
| Atlantis | `shattered_planet` | New, orbit-only | Endgame destination | Reached only via an established Platform. Mechanics deliberately deferred — see §7. |

GCyR ships `luna`, `mars`, `mercury` and `venus`. Only Venus is reused, as the basis for Ignus's
worldgen and sky; the rest are removed rather than reskinned, because dimension effects, sky
rendering and worldgen travel with a body and fighting mismatched data costs more than defining a
new one. Orbit dimensions are GCyR's own and are not built by us.

## 3. Establishing a Presence

### Planetary arrival — the Vanguard Kit

GCyR already models arrival hostility: oxygen and temperature systems are enabled, space is
-270°C, and freezing, overheating and suffocation all deal per-tick damage. What GCyR does not
provide is anywhere safe to stand. The Vanguard Kit does, and no more than that.

- The Kit is crafted on Terra and loaded into an uncrewed rocket.
- A KubeJS script watches for the rocket's arrival in the target dimension.
- On arrival the script consumes the Kit, finds the highest safe solid block, and pastes a minimal
  beachhead: platform floor and a Receiving Terminal.
- The Gateway Flag is set, marking the planet safe to travel to.

Oxygen supply and the return trip are deliberately left to the player — that pressure is the point.
Per-planet Kit variants (heat shielding for Ignus, thermal plant for Gelida) are a later upgrade
tier, not part of the baseline Kit.

**The Kit predates the fidelity standard, and is suspect** (`#100`). Terra's own arrival is now
Factorio's — a crash landing, an engineer stranded, a wreck to loot — and Factorio's *later*
arrivals are Space Age's: a platform overhead and a cargo drop, not a deployable kit. The Kit is
neither. Its fate is not this map's to decide, since `#25` stops at the first launch from Terra;
it is recorded there as out of scope rather than settled.

### Orbital presence — the Orbital Starter Kit

A Platform is a GCyR space station (ADR-0006). The Orbital Starter Kit is GCyR's station package
item, crafted on Terra and launched to orbit, where GCyR's own station creation builds the
foundational Platform. The player then travels up by rocket and expands it by hand, placing GT
machinery shipped from the surface to capture and process asteroid chunks.

Platforms are **static**. They are orbital factories, not vehicles: no thrusters, no navigation
computer, no interplanetary platform transit, and consequently no asteroid defence or hull mass
model. `spaceStationMaxSize` stays at GCyR's default 512, which the config warns cannot change once
a station world exists.

Atlantis, having no surface, is reached only once a Platform is established in its orbit — an
unmanned logistics achievement gates the endgame destination.

## 4. Moving Things

### Player transit

Players fly physically, in a GCyR `RocketEntity`, paying GCyR's tiered fuel costs — 8 buckets to a
moon, 14 within the solar system, 26 within the galaxy, 48 anywhere. The launch is the payoff
moment and is never simulated.

### Cargo

Unattended cargo flights are held as data — a Flight with a remaining travel timer — rather than as
moving entities, and pay their own cheaper fuel curve. A continuous export loop at crewed-rocket
prices would be punishing rather than interesting.

Launch Terminals, Receiving Terminals and Drop Hatches are custom GT machines registered through
GTCEu's KubeJS machine builders, which supply UI, power, JEI integration and recipe types without
bespoke block code, and keep fuel costs in one place.

- **Standard transit** — Cargo and fuel loaded into a Launch Terminal become a Flight subject to a
  travel timer.
- **Instant orbital drops** — Orbit-to-surface delivery is free and immediate: a Drop Hatch moves
  items directly into its linked Receiving Terminal, bypassing timer logic.
- **Drop Pods** — A drop with no Receiving Terminal below generates a temporary container at the
  target coordinates, which vanishes once emptied.

### Export infrastructure

- **Localized assembly** — Planet-specific component blocks are craftable only in machines
  physically located on their planet, forcing on-site factories rather than raw-material shipping.
- **Payload compression** — Materials are pressed into high-density shipping crates before loading,
  maximising throughput per launch.
- **Launch thresholds** — Terminals launch only at full capacity, gated by comparator logic, to
  prevent fuel drain on partial loads.
- **Routing** — A routing computer reads cargo contents and sets the destination accordingly, so
  one terminal serves many routes.

### Simulation Handoff (contingency, not architecture)

An earlier draft made a fully automated Platform hand off to a background throughput calculation
once established. This is **deferred**. The pack is built with real chunk loading; the handoff layer
is added only if measurement shows chunk loading is actually costing TPS. If it lands, it is
player-declared per Platform, not automatic on chunk unload.

## 5. Crafting and Recipe Routing

The crafting grid, workbenches and portable crafting stay intact. Automation pressure comes from
which recipe type each product uses — a recipe is craftable wherever its type is, and that routing
is inherent to the recipe system rather than something to script.

Policy: follow GTCEu's stock recipe-type assignments, re-authoring only for the pack's own items
and where a cross-mod shortcut undercuts GT. Create and Mekanism both offer grid recipes for things
GT expects to be machined, and Almost Unified is installed, so those shortcuts will be live and need
auditing.

### Science and research

Progression is **Factorio's science packs** — four packs plus an unscienced rung 0 (`automation`,
`logistic`, `chemical`, `production`), each rung granting a capability the next rung's production
physically requires. The gate is **Researchd's Research Lab**, fed by pipe and consumed unattended;
FTB Quests keeps the book and the reward surface but does not gate. GregTech is instrumental rather
than the ladder. The spine is recorded in ADR-0018; which mod owns each rung is ADR-0017.

**Terra's science packs are inert items. Sapros's science pack decays** — the buffer-as-liability
puzzle belongs to that body and is specified with it, not here.

### The Personal Assembler

A panel on the inventory screen, covering the bootstrap tier only: the components of the first
machines, which have nowhere to go once the crafting grid is removed and before any machine exists
to make them. It is not an item — there is nothing to craft, nothing to lose and nothing to grant.

- **Trigger** — A tab on the inventory screen, present from the first tick (`#95`).
- **Recipes** — The Assembling Machine's own, filtered to those Factorio marks hand-craftable; it
  has no recipe type of its own (ADR-0029).
- **Rate** — One at a time, at speed 1, so a craft is a request that completes rather than an
  instant.

It is unpowered, works anywhere, and is deliberately slow. Once real machines exist it has nothing
left to make. Because it can never be missing, the opening's job is to **teach** it — Terra's
wreckage and the quest book's tooltip hint, not a grant (`#100`).

## 6. Hazards and the Overseer System

### Emission

GTCEu 7.0.2 has **no pollution system** — the mod contains nothing matching `pollut`. Emission is
therefore ours (ADR-0005): a per-chunk score derived from the EU/t draw of running GT machines,
decaying over time and spreading to neighbouring chunks. Power draw is the one number every GT
machine already exposes, so no per-recipe tagging is needed, and spread plus decay makes outpost
placement a real decision rather than a counter.

### Raids and the Overseer — Terra only

Illager raids are the mechanic emission feeds, and they are Terra's alone. Vanilla raid pathfinding
is the substrate; exporting it to alien worlds would cost work for a less distinct result.

- **The Anchor** — Each outpost needs a Command Center block with a stationary Overseer (a `NoAI`
  villager) deployed on top.
- **The Attack** — High emission triggers raids; Illagers path toward the Overseer.
- **The Kill-Switch** — The Command Center checks for its Overseer continuously. If the Overseer
  dies, the Command Center stops emitting its signal and the outpost halts.
- **Manufacturing** — Overseers are made, not bred: Sapros biomass and nutrient fluids in a GT
  Bio-Vat yield a Cryo-Pod, which deploys into an Overseer on an empty Command Center. Outpost
  defence on Terra therefore depends on an organics chain on another planet.

### Emission elsewhere

Other planets accumulate emission but do not raid. Each converts it into a planet-appropriate
consequence so that emission has teeth everywhere while each planet keeps its own identity. Specific
consequences are open.

### Dormant Siege

Raids are state, not entities, until a player is present.

- **Offline accumulation** — An unloaded outpost's production and emission are computed
  arithmetically, with no entities rendered.
- **The Dormant Siege** — If emission crosses the raid threshold while unloaded, the outpost enters
  `under_siege` and its simulated production halts immediately.
- **Instantiation** — The physical Illagers spawn only when a player loads the chunk.

## 7. Satellites

GCyR ships four satellite types, all adopted:

- **Ore Finder** — The mid-game upgrade over walking and reading surface vein indicators. Both
  exist: rocks early, orbital scanning once you can launch. Gives the first satellite an obvious
  purpose.
- **GPS** — Navigation and mapping support.
- **Laser** — Orbital mining.
- **Dyson Swarm** — Late-game power sink and payoff.

## 8. Open Questions

- **Cross-mod recipe audit.** The specific Create and Mekanism shortcuts that undercut GT routing
  have not been enumerated.
- **Per-planet Vanguard Kit variants.** Deferred to a later upgrade tier.

## 9. Resource substitution policy

Per-body content is drawn from `docs/planets.md`, a transcription of Factorio's own
resource lists organised under Factorio's names. Every resource named there is resolved by this rule,
applied in order:

1. **An existing GregTech material.** Always preferred — the material system supplies dusts, plates,
   fluids, ore variants and recipe integration for free.
2. **A Mekanism or Create equivalent**, where GregTech has none.
3. **A custom KubeJS material**, only where the puzzle depends on the thing existing separately from
   anything that already exists.

GregTech already covers most of the source document: `tungsten`, `scheelite`, `calcite`, `lithium`,
`fluorine`, `ammonia`, `holmium`, `sulfuric_acid`, `apatite`, `rock_salt` and `salt_water` all exist
as materials. That list is recorded here so the next reader does not re-derive it. Scrap on Electro is
the clearest candidate for tier 3, its whole role being to be a distinct thing that recycles into a
spread of outputs.

Each body ticket argues only about its own exceptions to this rule.

Save compatibility is not a design constraint for this pack. Worlds under `saves/` are disposable test
state; where a change is only observable in a fresh world that is a testing fact, not a reason to
defer it.

## Delivery sequence

Specs are cut per slice from this document, in this order. Bodies land one at a time, each finished
before the next, and each body is two tickets: a **`Body:`** ticket delivers terrain, stone, ore
veins, fluid deposits, surface indicators, dimension marker and sky — everything that makes arriving
there complete — and a **`Puzzle:`** ticket, cut after its body ships, delivers that body's processing
chains, machine restrictions and craft gating from `docs/planets.md`.

1. **Planet definitions** — six bodies, IDs, lang files. Blocks the space stack.
2. **Body: Terra** — ore layout reweighted for the early recipes, a bedrock deposit for GregTech's
   common base materials, vanilla ores untouched. Establishes the registry test seam every later body
   extends by fixture.
3. **Body: Ignus** — carries the scaffolding: the first custom stone, worldgen layer and noise
   settings, so every body after it is content against a proven pattern.
4. **Body: Electro** — no natural veins by design; hand-mined surface ruins and a bedrock scrap
   deposit.
5. **Terra flow** — the science spine, the rung-ownership table and the rocket-part chain, ahead of
   Sapros: tech-tree feedback is wanted before the remaining bodies' puzzles are committed. A
   sequence quietly deviated from stops being the plan.
6. **Research: spoilage** — how this pack implements decay on industrial intermediates. Blocks Sapros,
   because specifying it first would be inventing the answer rather than finding it.
7. **Body: Sapros** — cut once the spoilage research closes, and not before.
8. **Body: Gelida** — the last body before the endgame; fluids and no solid ore.
9. **`Puzzle:` tickets** — one per body, each sequenced after its own body ships.
10. **Resource unification** — using AlmostUnified.
11. **Tech and recipe gating** — using KubeJS.
12. **Personal Assembler** — plus the cross-mod recipe audit. Largely independent.
13. **Emission** — per-chunk scoring, decay, diffusion.
14. **Overseer loop** — Command Center, Cryo-Pod, Dormant Siege. Depends on Emission and Sapros.
15. **Cargo terminals** — Launch/Receiving/Drop Hatch as GT machines, plus the Flight timer model.
16. **Vanguard Kit** — arrival intercept, beachhead paste, Gateway Flag.
