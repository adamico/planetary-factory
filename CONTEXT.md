# PlanetaryFactory

A Minecraft 1.21.1 / NeoForge modpack that reproduces the progression, logistics puzzles and
interplanetary scope of Factorio's Space Age expansion. Create supplies mechanical logistics,
GregTech CEu Modern supplies resource generation and processing, GCyR supplies rockets and
planets, and KubeJS binds them into a stationary, automation-first loop.

## Language

### Naming exceptions

The _Avoid_ lists below govern the pack's own prose. Two places quote Factorio deliberately and are
exempt: `data/factorio/*.json`, which is an extracted dump of Factorio's own prototypes, and
`docs/factorio-mechanics.md`, whose row keys are `Gleba` and `Vulcanus` because the ledger's value is
being diffable against Factorio. Both carry the `factorio-` or `data/factorio/` marker that says so.
Every body row names the pack's own body in its `where` field, so the mapping is never lost. See ADR-0028 —
this is not drift to be tidied up.

### Places

**Terra**:
The Overworld. The starter loop where basic extraction, first automation and the first rockets happen. The only body where Illager raids occur. Internal ID `overworld`.
_Avoid_: Overworld, home planet, spawn, Nauvis

**Terra Orbit**:
GCyR's orbit dimension above Terra, reached by rocket, where asteroid chunks are harvested into Space Science and the first Platform is established. Internal ID `overworld_orbit`.
_Avoid_: space, the void, orbital dimension, Nauvis Orbit

**Ignus**:
The volcanic planet. Thermal and fluid processing; heavy metals and byproduct management. Internal ID `vulcanus`.
_Avoid_: the lava planet, Venus, Vulcanus

**Electro**:
The recycling planet. Has no natural ores; all material comes from recycling generated ruins. Internal ID `fulgora`.
_Avoid_: the scrap planet, Mars, Fulgora

**Sapros**:
The organics planet. Agricultural automation under spoilage time limits; sole source of Cryo-Pods. Internal ID `gleba`.
_Avoid_: the swamp, Glacio, Gleba

**Gelida**:
The ice planet. Cryogenics and heat management — fluids freeze without active heating. Internal ID `aquilo`.
_Avoid_: Aquilo, Glacio, the ice world

**Atlantis**:
The orbit-only endgame destination, reachable only once a Platform is established in its orbit. Its mechanics are deliberately undefined. Internal ID `shattered_planet`.
_Avoid_: the shattered planet, Fragmenta

**Display name**:
The Latin or Greek name a player sees for a **celestial body**, supplied by lang files only. Every body has one, and it is never the identifier. The convention is a body-naming rule and stops there (ADR-0004) — science packs and everything else keep their own names.
_Avoid_: label, alias

**Internal ID**:
The Factorio-derived identifier a body is registered under, never shown to players.
_Avoid_: registry name, dimension key

**Platform**:
A GCyR space station: a player-expanded orbital factory that mines and processes asteroids. Static — it never travels.
_Avoid_: space station, orbital base, ship, vessel

### Sapros terrain

Sapros's five biomes. Identifiers carry Factorio's terms so the mapping to the wiki stays free;
display names are plain English. Transcribed in `docs/research/gleba-worldgen.md`.

**Dark Highlands**:
Sapros's elevated stone biome, and where its stone is found. Internal ID `gleba_dark_highlands`.
_Avoid_: the highlands, mountains, uplands

**Midlands**:
Sapros's other elevated biome, distinguished from the Dark Highlands by carrying no shallow water. Internal ID `gleba_midlands`.
_Avoid_: orange midlands, turquoise midlands, the plateau

**Marshes**:
Sapros's wetland biome, found beside its deep water lakes. Holds neither tree. Internal ID `gleba_marshes`.
_Avoid_: blue marshes, the swamp, wetlands

**Green Marshland**:
The marshland where Yumako trees grow, and one of the two biomes bearing Stromatolites. Internal ID `gleba_green_marshland`.
_Avoid_: the green biome, yumako forest, the swamp

**Red Marshland**:
The marshland where Jellystem grows, and the other biome bearing Stromatolites. Internal ID `gleba_red_marshland`.
_Avoid_: the red biome, jellystem swamp, the swamp

### Sapros flora

**Yumako**:
The fruit harvested from a Yumako tree's leaves. The tree is felled to take it and replanted from a sapling; it is not a standing crop that regrows. A spoilable material, so ultimately four items per Freshness.
_Avoid_: yumako fruit, the orange fruit, fruiting leaves

**Jellystem**:
The tree of Sapros's red marshland, whose stem blocks yield Jellynut. Named for the tree, never for its fruit.
_Avoid_: jelly tree, jelly stem, the jelly plant

**Jellynut**:
The material taken from a Jellystem's stem blocks — from the trunk, not picked from a canopy. A spoilable material. Distinct from Jelly, which is what a Biochamber makes from it.
_Avoid_: jelly, jelly nut, jellyfruit

**Stromatolite**:
The surface-generated block of Sapros's two marshlands, mined by hand for iron or copper bacteria plus stone. It is not an ore and yields no metal directly; the bacteria become metal by Decaying.
_Avoid_: ore patch, bacteria ore, iron ore, copper ore

**Ore Bacteria**:
What a Stromatolite drops — Iron Bacteria or Copper Bacteria. A spoilable material whose Decay product is metal, which is the only way Sapros yields metal at all. Until the Decay engine ships it is inert (ADR-0016).
_Avoid_: bacteria ore, ore culture, iron dust, raw ore

**Saprine**:
The adjective for anything of Sapros — the rock its ground is made of, and the ore variants that rock would carry if the body had any veins. Never Mercurian: the block is GCyR's orphan, the name is not (ADR-0008).
_Avoid_: Mercurian, Gleban, Sapran

### Establishing a presence

**Orbital Starter Kit**:
GCyR's station package item, crafted on Terra and launched to orbit, where GCyR's own station creation builds the foundational Platform.
_Avoid_: platform kit, station seed

**Vanguard Kit**:
The item carried by an uncrewed rocket to a virgin planet, which deploys a minimal beachhead — platform floor and Receiving Terminal — so the player has somewhere to stand on arrival. It supplies no oxygen and no return trip.
_Avoid_: beachhead kit, lander, drop kit

**Gateway Flag**:
The global marker recording that a planet has a deployed landing platform and is therefore safe to travel to.
_Avoid_: unlock, planet flag, safe flag


### Moving things

**Launch Terminal**:
The structure cargo and fuel are loaded into for a journey subject to a travel timer.
_Avoid_: rocket silo, launch pad, cargo bay

**Receiving Terminal**:
The surface structure that accepts arriving cargo on a planet with a deployed landing platform.
_Avoid_: landing pad, receiver, drop point

**Drop Hatch**:
The orbital structure that sends cargo down to a linked Receiving Terminal instantly and without fuel cost.
_Avoid_: cargo drop, chute

**Drop Pod**:
The temporary container generated on a surface when cargo is dropped with no Receiving Terminal present; it disappears once emptied.
_Avoid_: crate, temp chest, cargo pod

**Flight**:
An in-progress journey — cargo or passenger — held as data with a remaining travel timer rather than as a moving entity.
_Avoid_: shipment, trip, transit

**Simulation Handoff**:
The deferred transition of a fully automated Platform from a physically built factory to a background throughput calculation. A contingency held in reserve against measured TPS cost, not a system currently being built.
_Avoid_: abstraction, going virtual

### Making things

**Personal Assembler**:
The portable device, with its own recipe type, that turns a bootstrap-tier craft into a queued request completing after a duration. It supplements the crafting grid rather than replacing it, and covers only the components of the first machines.
_Avoid_: crafting table, hand crafter, personal crafter

**Assembling Machine**:
One of the three machines the pack registers on a GregTech chassis to run Factorio's crafting recipes — `assembling_machine_1`, `_2` and `_3`, differing by speed and tint only, each granted by a science rung and gating nothing (ADR-0026). Not GregTech's Assembler, whose craft the pack removes.
_Avoid_: assembler, GT assembler, crafter, fabricator

**Gated recipe**:
A recipe whose type routes it away from the crafting grid to a machine or the Personal Assembler. Gating is a recipe-authoring choice, not a scripted restriction.
_Avoid_: locked recipe, blocked recipe

**Research lock**:
A Researchd `unlock_recipe` effect withholding a recipe from a team until they research it. Distinct from a **Gated recipe** in both mechanism and meaning: gating is where a recipe is crafted and is permanent, a research lock is whether a team may craft it yet and is lifted by play. A lock is held per team, so the same recipe can be locked for one team and not another.
_Avoid_: recipe unlock, tech lock, gated recipe

**Lock annotation**:
The badge and tooltip a recipe viewer draws on a recipe under a **Research lock** the viewing player's team has not lifted, naming the research that would lift it. The pack annotates rather than hides, in both EMI and JEI and for every recipe source alike: hiding is vanilla's habit and tells the player nothing, and applying either policy to one viewer or one recipe source only relocates the incoherence (issue #75).
_Avoid_: hidden recipe, greyed-out recipe, locked overlay

**Unowned machine**:
A machine carrying no Researchd placed-by attachment, so it belongs to no team and no **Research lock** applies to it — it runs every recipe. Ordinary placement always stamps an owner; this is what `/setblock`, `/clone` and worldgen leave behind. Failing open is deliberate, and the pack logs the first such bypass at each position rather than refusing it (issue #74).
_Avoid_: ownerless machine, orphan machine, teamless machine

### The oil chapter

**Oil Refinery**:
The pack-registered GregTech multiblock that splits crude. It runs basic and advanced oil processing on Terra and coal liquefaction on Ignus, and it is the only machine in the pack that emits three fluids at once (ADR-0025).
_Avoid_: distillation tower, refinery multiblock, cracker

**Chemical Plant**:
The pack-registered GregTech single block carrying Factorio's whole chemical-plant recipe list — both crackings, lubricant, plastic, sulfur, solid fuel, sulfuric acid, battery and explosives (ADR-0025).
_Avoid_: chemical reactor, chem plant, reaction chamber

**Crude Oil**:
The unprocessed fluid a Fluid Drilling Rig extracts, and the sole input to oil processing. `gtceu:raw_oil`.
_Avoid_: raw oil, oil, petroleum

**Petroleum Gas**:
The lightest fraction, and the one that feeds sulfur and plastic. `gtceu:oil`, renamed in lang only.
_Avoid_: refinery gas, natural gas, naphtha

**Heavy Oil** / **Light Oil**:
The two heavier fractions. `gtceu:heavy_oil` and `gtceu:light_oil` — not `heavy_fuel` and `light_fuel`, which are different materials the pack hides. Heavy Oil is also what Electro's oceans are made of (ADR-0009).
_Avoid_: heavy fuel, light fuel, fuel oil, kerosene

**The oil chapter**:
Everything from crude to plastic, lubricant and launch fuel. It spans rungs 2 to 4 rather than sitting in one, because sulfur is petroleum-derived and sulfur gates chemical science.
_Avoid_: the oil rung, rung 4, the petroleum tier

### Spoiling

**Decay**:
The process by which an organic material loses freshness over time and is eventually replaced by
something else. It runs continuously, everywhere, on every body and in flight — it is not a property
of any one machine or dimension.
_Avoid_: spoiling, rotting, decomposition, aging

**Freshness**:
How far through Decay a material has travelled, expressed as one of four named states rather than a
percentage or a timer. Freshness is part of what a material *is*, not a hidden value attached to it.
_Avoid_: spoilage level, staleness, condition, quality, durability

**Fresh**, **Ripe**, **Stale**, **Spoiling**:
The four freshness states, in order. **Spoiling** is the last state before a material is replaced,
not the state of having been replaced — that is Spoilage.
_Avoid_: spoiled (for the fourth state), stage 1-4, tier

**Spoilage**:
The material that organics become at the end of Decay. It is a feedstock in its own right, not
waste — biosulfur is made from it.
_Avoid_: rot, waste, compost, garbage, trash

**Biochamber**:
The Sapros machine that every recipe involving a spoilable material runs in. Processing organics
elsewhere is not an alternative path; there is no alternative path.
_Avoid_: bioreactor, fermenter, organics processor

**Clog**:
The state of a machine holding a material that has Decayed past what its recipe accepts, halting it
until the Spoilage is removed. A stated hazard the player is responsible for designing around, not a
fault. A Clog is an inability to consume, never a refused write — Decay writes slots directly and
cannot be turned away.
_Avoid_: jam, deadlock, stall, blockage

**Drain**:
The player-built route by which Spoilage leaves a Clogged machine. Terminal Spoilage, and only
terminal Spoilage, may be pulled out of a bus that otherwise refuses extraction, so a hopper under an
input bus clears a Clog while leaving every un-Decayed stage locked inside. There is no dedicated
bus and no automatic removal: the Drain is something the player builds, or does not.
_Avoid_: purge, trash, trash slot, reject, waste output, eject, idle draw (Factorio's `drain`, an
unrelated mechanic -- ADR-0029)

### Hazards

**Emission**:
The per-chunk score accumulated from the EU/t draw of running GT machines, which decays over time and spreads to neighbouring chunks. It is the pack's own construct — GTCEu has no pollution system.
_Avoid_: pollution, smog, contamination

**Overseer**:
The stationary villager that anchors an outpost and that raiding Illagers path toward. Manufactured from Sapros organics, not bred or recruited.
_Avoid_: villager, anchor, guard

**Command Center**:
The block an Overseer is deployed onto; it powers the outpost and halts it if its Overseer dies.
_Avoid_: core block, outpost controller

**Cryo-Pod**:
The item produced from Sapros organics that deploys into an Overseer.
_Avoid_: villager egg, pod

**Dormant Siege**:
The state of an unloaded outpost whose accumulated Emission has crossed the raid threshold; its production halts and the raid instantiates only when a player arrives.
_Avoid_: pending raid, queued attack

### Orbit

**Ore Finder Satellite**:
The orbital scanner that reveals ore veins, superseding surface vein indicators as a mid-game upgrade rather than replacing them.
_Avoid_: scanner, prospector

**Dyson Swarm**:
The late-game orbital power infrastructure.
_Avoid_: solar swarm, dyson sphere
