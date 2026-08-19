# PlanetaryFactory — Game Design Document

A Minecraft 1.21.1 / NeoForge modpack reproducing the progression, logistics puzzles and
interplanetary scope of Factorio's Space Age expansion, built on a curated mod stack and bound
together by KubeJS into a stationary, automation-first loop.

This document describes intended design. Decisions that are hard to reverse are recorded as ADRs in
`docs/adr/`; domain vocabulary is defined in `CONTEXT.md` and used here verbatim.

## 1. Core Technology Stack

- **Create** — Mechanical logistics: bulk transport, item filtering, schedule-based rail networks.
- **GregTech CEu Modern (GT:M) 7.0.2** — Resource generation and processing. Chunk-aligned ore
  veins, bedrock fluid and ore extraction, multiblocks, and the machine and recipe-type registries
  the pack extends.
- **Gregicality Rocketry (GCyR)** — Rockets, planets, orbit dimensions, space stations, satellites,
  oxygen and temperature systems. Built from source against GTCEu 7.0.2 (ADR-0001); the released
  jar is permanently incompatible. Stellaris is a disabled fallback, not part of the design
  (ADR-0002).
- **KubeJS 2101.7.1-build.181** — Scripting glue: custom GT machines and recipe types, emission
  tracking, flight simulation, planetary arrival. Pinned to the build GTCEu 7.0.2 compiles against;
  `kubejs-create` is not installed.
- **Pre-AE2 logistics** — Modular Routers, Integrated Dynamics and LaserIO cover routing and
  circuit-network roles before digital storage is unlocked.

## 2. The Solar System

Seven destinations. Internal identifiers use Factorio's names; the names players see are Latin and
Greek, supplied by lang files only (ADR-0004).

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

### The Personal Assembler

A portable device with its own recipe type, covering the bootstrap tier only: the components of the
first machines, which have nowhere to go once their recipes leave the grid and before any machine
exists to make them.

- **Trigger** — Right-clicking opens an FTB Library UI.
- **State** — A selected recipe pushes a queue entry to the player's `persistentData`.
- **Engine** — A `PlayerEvents.tick` script decrements the timer and delivers the item at zero.

It is unpowered, works anywhere, and is deliberately slow. Once real machines exist it has nothing
left to make.

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
consequence — accelerated spoilage on Sapros, equipment or thermal stress on Ignus and Gelida —
so that emission has teeth everywhere while each planet keeps its own identity. Specific
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

- **Atlantis mechanics.** Deferred pending a closer look at Factorio's Shattered Planet. It is
  currently a named, orbit-only endgame destination with no defined puzzle, resource or attrition
  model.
- **Per-planet emission consequences.** Named in principle (§6), unspecified in detail.
- **Cross-mod recipe audit.** The specific Create and Mekanism shortcuts that undercut GT routing
  have not been enumerated.
- **Per-planet Vanguard Kit variants.** Deferred to a later upgrade tier.

## Delivery sequence

Specs are cut per slice from this document, in this order:

1. **Planet definitions** — seven bodies, IDs, lang files. Blocks the space stack.
2. **Personal Assembler** — plus the cross-mod recipe audit. Largely independent.
3. **Emission** — per-chunk scoring, decay, diffusion.
4. **Overseer loop** — Command Center, Cryo-Pod, Dormant Siege. Depends on Emission and Sapros.
5. **Cargo terminals** — Launch/Receiving/Drop Hatch as GT machines, plus the Flight timer model.
6. **Vanguard Kit** — arrival intercept, beachhead paste, Gateway Flag.
