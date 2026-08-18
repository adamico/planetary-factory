# PlanetaryFactory

A Minecraft 1.21.1 / NeoForge modpack that reproduces the progression, logistics puzzles and
interplanetary scope of Factorio's Space Age expansion. Create supplies mechanical logistics,
GregTech CEu Modern supplies resource generation and processing, GCyR supplies rockets and
planets, and KubeJS binds them into a stationary, automation-first loop.

## Language

### Places

**Nauvis**:
The Overworld. The starter loop where basic extraction, first automation and the first rockets happen.
_Avoid_: Overworld, home planet, spawn

**Nauvis Orbit**:
The void dimension above Nauvis, reached by rocket, where asteroid chunks are harvested into Space Science.
_Avoid_: space, the void, orbital dimension

**Vulcanus**:
The Venus-like planet. Thermal and fluid processing; heavy metals and byproduct management.
_Avoid_: the lava planet, Venus

**Fulgora**:
The Mars-like planet. Has no natural ores; all material comes from recycling generated ruins.
_Avoid_: the scrap planet, Mars

**Gleba**:
The organics planet. Agricultural automation under spoilage time limits.
_Avoid_: the swamp, Glacio

**Platform**:
A player-built structure in a void dimension, expanded from a delivered starter kit, that mines and processes asteroids and can later carry the player between planets.
_Avoid_: space station, orbital base, ship

### Establishing a presence

**Orbital Starter Kit**:
The item crafted on Nauvis and launched to orbit; on arrival it becomes the foundational Platform.
_Avoid_: platform kit, station seed

**Vanguard Kit**:
The item carried by an uncrewed rocket to a virgin planet, which deploys a landing platform and Receiving Terminal so the player can arrive safely.
_Avoid_: beachhead kit, lander, drop kit

**Gateway Flag**:
The global marker recording that a planet has a deployed landing platform and is therefore safe to travel to.
_Avoid_: unlock, planet flag, safe flag

**Navigation Computer**:
The multiblock that verifies a Platform's thrusters and defences and a destination's Gateway Flag before permitting a journey.
_Avoid_: nav computer, flight controller

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
The transition of a fully automated Platform from a physically built factory to a background throughput calculation.
_Avoid_: abstraction, going virtual

### Making things

**Personal Assembler**:
The item that replaces vanilla 2x2 and 3x3 handcrafting, turning a hand-craft into a queued request that completes after a duration.
_Avoid_: crafting table, hand crafter, personal crafter

### Hazards

**Overseer**:
The stationary villager that anchors an outpost and that raiding Illagers path toward. Manufactured from Gleba organics, not bred or recruited.
_Avoid_: villager, anchor, guard

**Command Center**:
The block an Overseer is deployed onto; it powers the outpost and halts it if its Overseer dies.
_Avoid_: core block, outpost controller

**Cryo-Pod**:
The item produced from Gleba organics that deploys into an Overseer.
_Avoid_: villager egg, pod

**Dormant Siege**:
The state of an unloaded outpost whose accumulated pollution has crossed the raid threshold; its production halts and the raid instantiates only when a player arrives.
_Avoid_: pending raid, queued attack
