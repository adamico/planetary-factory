# What Oritech could carry: a coverage survey against the mechanic ledger

Read against a clone of `Rearth/Oritech` at tag `v1.2.12` (`bf0d3e9`), beside the pack as
`../oritech-src`, and the matching jar `oritech-neoforge-1.21.1-1.2.12.jar` in the sibling CurseForge
instance `../oritech/mods/` — the recipes are generated at build time, so recipe JSON is read from the
jar, never the source tree. Paths below are relative to `common/src/main/java/rearth/oritech/` unless
they start with `data/`. Oritech is **CC0** (`LICENSE.md`), which matters for any row whose answer is
"subclass it" or "fork it".

**This is a survey, not a decision.** No ADR is proposed here. It answers one question: if the pack's
mechanic-bearing palette were reduced to Create, Oritech and `planetaryfactory_core`, which rows of
`docs/factorio-mechanics.md` could Oritech carry, and at what cost.

## The hypothesis being surveyed

| role | mod |
| --- | --- |
| logistics | Create |
| machines | Oritech |
| everything Oritech does not cover and cannot be customised to | `planetaryfactory_core` |

**Leaves:** GCyR, Create: Power Grid, Modern Industrialization, and GregTech's leftovers.
**Stays:** Researchd, Respoiled (the Decay fork), Building Gadgets 2, Block Runner, and all plumbing
with no mechanic of its own — KubeJS, EMI/JEI, FTB, Jade, libraries, performance mods.

## Method

**Levels.** Each row gets the *cheapest* level that reaches every corpus number in it, tried in this
order:

1. **Native** — Oritech config or datapack JSON only.
2. **KubeJS** — scripting, no Java.
3. **Java** — core-mod code *on Oritech's base classes*: a subclass of `UpgradableMachineBlockEntity`
   and friends, a new `MachineAddonBlock` instance, or a mixin. The cost is coupling to classes in
   `block.base.entity`, which is not Oritech's `api` package; it is paid by pinning the Oritech
   version, as `kubejs-version-is-pinned` already does for KubeJS.
4. **Core** — a standalone core machine that owes Oritech nothing but its energy API.

A row is **Native only if every corpus number in it is reachable** from config or datapack. A shape
match is not enough: one global `speedAddonSpeed` knob can hit one speed module's numbers, not three
tiers.

**Shape-only** is a separate verdict, not a level. It means Oritech has the thing, but using it
contradicts a **mechanic ADR** or the corpus (ADR-0054). Mechanic ADRs still bind in the hypothetical:
0020, 0032, 0033, 0036, 0041, 0049, 0050, 0051, 0054, and the logistic-robot exclusion. **Ownership
ADRs are void by the hypothesis itself** — 0017, 0035, 0056's chassis choice, 0057, and the Power
Grid parts of #148 — and are marked "superseded if adopted" rather than argued.

**Proof.** Every cell cites a class, a config key or a recipe file. A claim about behaviour in play
carries **`world-load (human)`**: it is read from code and wants a person in a running world to
confirm it. No procedural launch is implied.

**Not Oritech.** Rows Oritech has nothing for are listed as `not Oritech → core` with one line and no
sizing.

---

## Cross-cutting facts

These decide most of the rows below, so they are stated once.

### 1. Energy is FE, and the pack has no joule constant yet

Every Oritech block speaks its own `api.energy.EnergyApi`, bridged to NeoForge's FE capability. Draw
is per machine and configurable: `BasicMachineConfig.energyPerTick`, `FurnaceConfig.energyPerTick`,
`CentrifugeConfig.energyPerTick` and so on in `init/OritechConfig.java`, all `long` with no useful
ceiling. So **every Factorio wattage is reachable by config** — given one constant the pack does not
yet have: **joules per FE**. `core/energy/EnergyLedger.java` holds only `FE_PER_EU = 4`, GregTech's
ratio, which the hypothesis deletes. Every "Native at Factorio's kW" cell below is conditional on that
constant being chosen once.

### 2. The recipe schema

`init/recipes/OritechRecipe.java`: a list of `Ingredient` (one unit per entry; a count is written as
repeated entries — `data/oritech/recipe/assembler/*` does exactly this), a list of result
`ItemStack`s, **one** `FluidIngredient`, a list of fluid outputs, and `time` in ticks. There is no
result probability.

Measured over the committed corpus (`data/factorio/recipe.json`, 163 recipes):

| shape | recipes | fits Oritech's schema? |
| --- | --- | --- |
| two fluid inputs | 4 — `advanced-oil-processing`, `heavy-oil-cracking`, `light-oil-cracking`, `sulfur` | **no**: one `fluidInput` |
| more than four distinct item inputs | 2 — `oil-refinery`, `rocket-silo` | not on the Assembler's 4 input slots |
| more than one item output | 4 — the three asteroid crushings, `uranium-processing` | yes: `results` is a list |
| fluid in or out | 26 `crafting-with-fluid`, 11 `chemistry`, 2 `oil-processing` | schema yes; **the Assembler has no fluid tank** |

Two fluid inputs is a schema limit, not a machine limit: a core subclass with two tanks still needs a
recipe type whose serializer carries two fluids. That is Java either way.

### 3. Recipe selection: first match, no lock — and Researchd reaches it for free

`block/base/entity/MachineBlockEntity.java` `getRecipe()` keeps `currentRecipe` while it still
matches, and otherwise asks `level.getRecipeManager().getRecipeFor(type, input, level)` — the **first**
match. If that first match cannot output (`canOutputRecipe`), the machine idles; it does not try the
next candidate. Nothing is dropped at load, unlike GregTech's trie (#236), but ambiguity is settled by
recipe order.

ADR-0056's answer is **output-slot locking**: the lock refuses a rival recipe's product. Oritech has no
slot lock (`grep` for lock/ghost finds only JEI's filter-screen handler). Both methods it would take are
overridable: `getRecipe()` is `protected`, `canOutputRecipe()` is `public`. So **selection is Java,
contained in a subclass** — iterate `getAllRecipesFor`, filter on the locked output, persist the lock.

**Research gating is Native.** Vanilla's three-argument `getRecipeFor` delegates to the four-argument
overload with a `null` hint (disassembled from `neoforge-21.1.248-merged.jar`), and that four-argument
overload is exactly what Researchd's `RecipeManagerMixin` filters, as is `getAllRecipesFor`. Oritech's
`currentRecipe` shortcut bypasses the manager, which is harmless under ADR-0058's one force: research
only unlocks, never re-locks. `world-load (human)`.

### 4. Addons are the module system, and new ones need no mixin

Addons are **blocks** attached to a machine's addon slots or to extenders, found by a spatial search in
`util/MachineAddonController.java`. Each is `new MachineAddonBlock(props, AddonSettings…)`
(`init/BlockContent.java:246-268`). `AddonSettings` is a public record: speed, efficiency, capacity,
insert rate, extra chambers, burst ticks. **There is no productivity field.**

- **Stacking is configurable to Factorio's rule.** `OritechConfig.additiveAddons` switches
  `MachineAddonController:233-260` from multiplying to summing bonuses — Factorio's modules add.
- **A new tier is a new `MachineAddonBlock` instance** with its own settings: Java, no mixin. One
  unverified step: the new block must be a valid block for a `BlockEntityType` whose entity extends
  `AddonBlockEntity`. Oritech builds `ADDON_ENTITY` with a fixed block list
  (`init/BlockEntitiesContent.java:231`), so the core either registers its own type or extends that
  list. `world-load (human)`.
- **Per-machine behaviour has a hook.** `getAdditionalStatFromAddon(AddonBlock)` is overridden by the
  Fragment Forge, the Enderic Laser and the Destroyer to read Yield. A core subclass reads a
  productivity addon the same way.
- **Slot counts are per class.** `getAddonSlots()` (for example `AssemblerBlockEntity:92`, three slots)
  plus extenders bounded by machine-core quality. A subclass fixes them.

### 5. Multiblocks are built, not placed

`getCorePositions()` lists offsets the player must fill with Machine Core blocks before the controller
forms (the Assembler and Foundry are four blocks). ADR-0059 wants footprint = Factorio tile size,
**placed from one item** by the rig idiom. The offsets are a per-subclass override, so the footprint is
reachable; the gesture — one placement — means the core places the cores itself, or skips the Oritech
multiblock and uses its own rig hull. Java either way.

### 6. Burner fuel is Forge's table

`block/entity/generators/BasicGeneratorEntity.java:47` reads Architectury's `FuelRegistry`, the vanilla
burn table. ADR-0047 makes fuel pack JSON with default-deny. Anything that burns items through
Oritech needs Java to read the pack's table.

### 7. Oritech's block-breakers bypass ore amounts

The core draws one unit per break on `BlockEvent.BreakEvent` (`core/ore/OreMining.java:60`) and
retires the delta in `OreBlock#onRemove`. Oritech's Destroyer removes the block with
`setBlockAndUpdate(AIR)` / `destroyBlock` (`block/entity/interaction/DestroyerBlockEntity.java:206,246`),
and the Enderic Laser calls `destroyBlock` too (`LaserArmBlockEntity:261`) — **no `BreakEvent`**. An Oritech machine breaking a
`planetaryfactory:` ore block would take the whole block for one drop and discard the rest of its
amount. That contradicts ADR-0041. `world-load (human)`.

### 8. What Oritech has that Factorio does not costs nothing to remove

ADR-0034's sweep removes every recipe no decision names. An Oritech block with no admitted recipe is
unobtainable, so the Tree Cutter, Particle Accelerator, Enchanter, Spawner Controller, Addon Splicer,
electric tools and the rest vanish by doing nothing. The cost is in the other direction: every Oritech
recipe type the pack *does* use needs a survivor entry in `recipe_survivors.js` (KubeJS). Oritech's
worldgen — nickel and platinum ore, Resource Nodes, oil springs, uranium patches — is nine
`data/oritech/neoforge/biome_modifier/*.json` files; overriding them in a datapack disables it. Native.

---

## The matrix — base game

Ledger verdicts are today's. **Level** is the cheapest that closes every gap in the row.

| mechanic | ledger today | Oritech has | level | gap, and what closes it |
| --- | --- | --- | --- | --- |
| **Resource patches and finite ore** | `shipped` | Resource Nodes + Bedrock Extractor (`DeepDrillEntity`); ore worldgen | **Shape-only** (ADR-0020, 0041) | Nodes are infinite: `serverTick` never depletes. Keep the core's patches; disable Oritech's worldgen (Native, fact 8). |
| **Manual mining** | `adapted` | Hand Drill, Chainsaw, Promethium tools | **Shape-only** (ADR-0039) | Charge-based tools against an indestructible two-tier pick. Core keeps the Engineer's Pick. |
| **Trees and wood** | `adapted` | Tree Cutter (`TreefellerBlockEntity`) | **Shape-only** (ADR-0051) | A felling machine Factorio has none of, harvesting log-by-log; ADR-0051's tree is one entity. Unadmitted (fact 8). |
| **Mining drills** — burner | `adapted` | nothing that burns | **Core** | Stays the pack's rig (ADR-0040, 0043). |
| **Mining drills** — electric | `adapted` | Destroyer frame + Quarry Addon (`range *= 8` per addon, `DestroyerBlockEntity:81`); Bedrock Extractor | **Core**, on Oritech FE | The frame is a gantry the player builds around an area, not a 3x3 drill with a 5x5 reach; and it bypasses ore amounts (fact 7). The core rig stays, drawing through `EnergyApi`. |
| **Mining drills** — pumpjack | `adapted` | Pump on oil springs | **Core** | `PumpBlockEntity` *drains* non-water sources (`:112-120`); Factorio's crude is infinite with a decaying yield. |
| **Water as a resource** | `planned` | Pump | **Shape-only** (ADR-0050) | Water is treated as infinite, which fits, but `ENERGY_USAGE = 512` per bucket and `PUMP_RATE = 5` ticks are hard-coded constants (`:42-43`), against the Offshore Pump's 1,200 mB/s at zero power. Core keeps the Offshore Pump. |
| **Fluid handling** | `planned` | fluid pipes (`fluidPipeInternalStorageBuckets`), Portable Tank | see the boundary section | Create owns it under the hypothesis. |
| **Oil processing** — basic | `planned` | Refinery + Chamber modules; `oil`, `heavy_oil`, `naphtha`, `diesel`, `sulfuric_acid` fluids | **Java** | Schema fits (one fluid in, a list out), but the Refinery is a machine-core multiblock whose output count grows with stacked Chamber modules. It needs ADR-0059's single-placement 5x5 (fact 5) and a fixed three-output shape. Borrowing Oritech's fluids is item-layer work, which is out of scope. |
| **Oil processing** — advanced, cracking, `sulfur` | `planned` | — | **Java** | Two fluid inputs (fact 2): a two-tank subclass *and* a new recipe type. |
| **Smelting** — burner tiers | `planned` | — | **Core** | The existing `FurnaceTier` blocks stay. |
| **Smelting** — electric | `planned` | Powered Furnace | **Java** | `PoweredFurnaceBlockEntity:59` reads vanilla `RecipeType.SMELTING` and `shrink(1)`s — `5 iron_plate → steel_plate` cannot pass. A subclass overriding the lookup to `planetaryfactory:smelting` fixes it. |
| **Assembling machines** | `planned` | Assembler (4 in / 1 out, no fluid) | **Java** | Three subclasses: output lock (fact 3), a base speed per tier (0.5 / 0.75 / 1.25), addon slot counts (0 / 2 / 4), a 3x3 footprint (fact 5), and a fluid tank for the 26 `crafting-with-fluid` recipes. |
| **Chemical plant** | — (in Oil processing) | Refinery, Centrifuge (fluid-capable) | **Java** | 3 of the 11 `chemistry` recipes need two fluids (fact 2). |
| **Centrifuge** | — (in Nuclear fission) | Centrifuge | **Java** | No result probability in the schema; `uranium-processing` needs 0.993/0.007. Override `craftItem`. *See the corpus note at the end.* |
| **Handcrafting and the crafting queue** | `planned` | — | **not Oritech → core** | The Personal Assembler. |
| **Transport belts**, **Inserters** | `adapted` | item pipes, Pipe Booster, Inventory Proxy Addon | **not needed** | Create owns them. Oritech's pipes are unadmitted. |
| **Construction robots and blueprints** | `adapted` | — | **not Oritech** | Building Gadgets 2 stays. |
| **Trains** | `planned` | — | **not Oritech** | Create. |
| **Circuit network** | `adapted` | Control Unit Addon (`RedstoneAddonBlockEntity`): enable/disable plus comparator output; the reactor's Redstone Port | **Native** for Oritech machines | Fits ADR-0030's redstone shape. Core machines get it by subclassing (`RedstoneControllable`). |
| **Electric network** — supply area | `adapted` | energy pipes, Framed Superconductor | **Shape-only** (ADR-0036) | Cables are the distribution mechanism ADR-0036 and ADR-0057 refused. The Supply Area Pole stays core and speaks `EnergyApi` (public package). |
| **Electric network** — wire reach | `adapted` | Energy Transmission Pole (`PowerPoleEntity`) | **Java** | Point-to-point, `poleConfig.minRange = 50` / `maxRange = 1000` (configurable), 1M RF/t, one output face — but a machine-core multiblock and a zipline (fact 5; ADR-0049). The reach is Native; the gesture and zipline are not. |
| **Power generation** — steam engine | `planned` | Steam Engine | **Java** | `steamId` is configurable to `planetaryfactory:steam` and `steamToRfRatio` is configurable. But the engine's rate scales with speed up to `MAX_SPEED = 10`, and it returns **90% of the steam as water** (`WATER_RATIO`, `SteamEngineEntity:47,121`). Factorio's burns 30/s flat and returns nothing. |
| **Power generation** — boiler | `planned` | Steam Boiler Addon on a generator | **Core** | Burns through `FuelRegistry` (fact 6); the core Boiler (ADR-0048) stays. |
| **Power generation** — solar | `planned` | Big Solar Panel | **Java** | `BigSolarPanelEntity`: on/off at sky light 12, scaled by core quality — Factorio's is a daylight curve. Energy per tick is config. |
| **Power generation** — accumulator | `planned` | Portable / Large Energy Storage | **Native** | `smallEnergyStorage` / `largeEnergyStorage` capacity and rates are config; conditional on the joule constant (fact 1). |
| **Nuclear fission** | `adapted` | Reactor multiblock: rods, reflectors, heat pipes, vents, absorbers | **Shape-only** (ADR-0033) | It outputs RF (`ReactorEnergyPortEntity`); ADR-0033's reactor emits Superheated Steam. **But see below**: it restores a sub-rule the ledger dropped by consequence. |
| **Pollution** | `planned` | — | **not Oritech → core** | |
| **Enemies and evolution** | `planned` | — | **not Oritech → core** | |
| **Combat** — laser turret | `planned` | Enderic Laser + Hunter Addon (targets `Enemy`, `LaserArmBlockEntity:358`) | **Java** | Shape-close to a laser turret; damage, range and draw against the enemy corpus need a subclass. It also breaks blocks, so it bypasses ore amounts (fact 7). |
| **Combat** — gun turrets, walls, ammo | `planned` | — | **not Oritech → core** | |
| **Armor and the equipment grid** | `planned` | Exo armor; Augments (`data/oritech/recipe/augment/*`) | **Native** for attribute effects, **Java** for the grid | An augment is datapack: `effect.entityAttributeId` + `amount`, e.g. exoskeleton speed as `generic.movement_speed`. No grid size, no per-tick draw, and its own research track (`researchCost`, `requiredStation`) beside Researchd — gate the station through Researchd instead. |
| **Capsules** | `planned` | `NuclearExplosionEntity` | **not Oritech** | The explosion entity is reusable for an atomic bomb; everything else is core. |
| **Modules and beacons** | `blocked` | Speed, Efficiency, Synergy Matrix, Yield addons | see below | The one `blocked` row Oritech moves. |
| **Research and science packs** | `planned` | Augment research only | **Native** (the gate) | Researchd stays; its filter reaches Oritech machines (fact 3). |
| **The technology tree** | `shipped` | — | **unchanged** | |
| **Rocket silo and rocket launch** | `planned` | — | **not Oritech → core** | GCyR leaves. |
| **Character movement on foot** | `adapted` | speed augments | **unchanged** | Belongs to armor above. |
| **Personal transport** | `blocked` | Jetpacks, Jetpack Elytra, pole zipline | **Shape-only** (ADR-0049) | Flight and ziplining are not Factorio's car or spidertron. |
| **Terrain modification**, **Repair**, **Controls**, **Day and night** | various | — | **not Oritech** | |
| **Radar and map exploration** | `planned` | Ore Vision augment | **not Oritech → core** | ADR-0045's radar detects nothing; ore vision is a prospector. The core Radar draws FE instead of Power Grid's electricity. |
| **Logistic robots** | `excluded` | Drone Port | **Shape-only** | See interplanetary logistics. |
| **Logistic request and trash** | `excluded` | — | — | |

### Modules and beacons, split

| sub-rule | level | reading |
| --- | --- | --- |
| speed and efficiency, tier 1 | **Native** | `speedAddonSpeed` / `speedAddonEfficiency` / `efficiencyAddonEfficiency` in `oritech-startup.toml`, with `additiveAddons` set to Factorio's summing. |
| tiers 2 and 3 | **Java** | Four more `MachineAddonBlock` instances with their own `AddonSettings`; a block entity type to host them (fact 4). |
| productivity | **Java** | No field. Yield means Fortune, Looting or a byproduct toggle (`FragmentForgeBlockEntity:59`), not bonus output. Needs a new addon, a `getAdditionalStatFromAddon` read and a progress bar in `craftItem` on each core subclass. Factorio's intermediates-only restriction is core logic. |
| slot count per machine | **Java** | `getAddonSlots()` per subclass; extenders disabled. |
| slot vs block | **adapted** | A module is a block beside the machine, not an item in it. This is the ledger's `notice` sentence, not a gap. |
| beacon | **Java** (mixin) | Addon discovery is adjacency only. A beacon means either a mixin on `MachineAddonController`'s search or a virtual addon that machines in range register. |

### The reactor restores a sub-rule nobody decided against

The ledger's Nuclear fission row has **Reactor neighbour bonus — `excluded`, `by-consequence`**:
"there is nothing to place next to anything, so the layout puzzle has no board." Oritech's reactor
**is** that board. A rod pulses its neighbours, a reflector sends pulses back, and energy scales with
pulses received — Factorio's adjacency bonus in a different unit (`wiki/docs/reactor/components.mdx`,
`rfPerPulse`). A `by-consequence` row is owned by the ledger, not by an ADR, so nothing binds against
it. What does bind is ADR-0033's steam output: the Energy Port emits RF. A Java subclass that emits
Superheated Steam through a port instead keeps ADR-0033 *and* regains the bonus.

---

## The matrix — Space Age

| mechanic | ledger today | Oritech has | level | reading |
| --- | --- | --- | --- | --- |
| **Interplanetary travel** | `planned` | — | **not Oritech → core** | GCyR leaves. The heaviest core item in the hypothesis, and out of this survey's scope. |
| **Space platforms** | `planned` | — | **not Oritech → core** | |
| **Asteroid mining and reprocessing** | `planned` | Pulverizer, Fragment Forge (multi-result) | **Native** for the crushing recipes | Three `crushing` recipes fit the `grinder` schema, apart from any probabilities (see the corpus note). Collection is core. |
| **Interplanetary logistics** | `planned` | Drone Port | **Java** | `DronePortEntity` holds a payload as data with an arrival time (`DroneTransferData`) — CONTEXT.md's **Flight** in all but name. It is same-level only (`level.getBlockEntity(targetPosition)`), refuses under 50 blocks, and draws `sqrt(dist) × 50`. Crossing dimensions is a subclass overriding `canSend`/`calculateEnergyUsage`. `world-load (human)` for chunk loading at the target. |
| **Spoilage** | `adapted` | — | **unchanged** | Respoiled stays. Whether it ticks inside Oritech inventories is `world-load (human)`. |
| **Quality** | `blocked` | — | **not Oritech** | Oritech's "core quality" is a machine-core tier, not item quality. A name collision to avoid. |
| **Recycling** | `planned` | Pulverizer `grinder/recycle/*` (three tag recipes) | **KubeJS** | Recipes can be generated into the `grinder` type, but the schema has no probability, so Factorio's 25% return must be **batched** (4 gears → 2 plates). That batching is itself an `adapted` notice. |
| **Vulcanus: lava and calcite** | `planned` | Foundry (item alloying), Lava Generator, a Refinery lava recipe | **Core** | Oritech's Foundry has no fluid (`FoundryBlockEntity`, zero fluid references); Factorio's is molten metal. |
| **Fulgora: scrap and lightning** | `planned` | — | **not Oritech → core** | |
| **Gleba: agriculture and nutrients** | `planned` | Frame gantry: Placer, Fertilizer, Destroyer + Crop Filter Addon; Bio Generator | **Java** | Plant-fertilise-harvest in an area is the agricultural tower's shape. The Destroyer path bypasses break events (fact 7) and the frame is built, not placed. The Biochamber is core. |
| **Aquilo: cold and ammonia** | `planned` | Industrial Chiller (fluid → ice/snow/obsidian) | **not Oritech → core** | The freezing layer and heat pipes are ADR-0033's Gelida work. |
| **Planet-locked buildings** | `planned` | — | **KubeJS** | Oritech blocks place anywhere. A cancelled `BlockEvents.placed` per dimension covers them as it covers every other block. |
| **Fusion power** | `planned` | Particle Accelerator | **not Oritech** | The accelerator is a collision mechanic with no Factorio analogue. |
| **Elevated rails**, **The Shattered Planet** | `excluded` / `blocked` | — | — | |

---

## The logistics and energy boundary

Q7 of the grilling that commissioned this survey was left open on purpose. Factorio's inserters draw
from the electric network. Create's belts and arms run on rotation (SU). Oritech runs on FE. Three
options, with costs:

| option | what it means | Oritech cost | core cost |
| --- | --- | --- | --- |
| **(a) FE→SU bridge** | Create keeps belts, arms, trains and fluid pipes. One electric network (Oritech FE). A core block turns FE into rotation, so an inserter is electric in effect. | none | **Java**: one generating kinetic block on Create's API, drawing through `EnergyApi`. Replaces the role Power Grid's generator played in ADR-0048's chain. |
| **(b) Two currencies** | As (a), but rotation comes from Create's own sources and the pack's Steam Engine. Inserters are not electric. | none | none, but a Factorio fidelity loss with an `adapted` notice. |
| **(c) Fluids to Oritech** | Oritech takes pipes, tanks and pumping; Create keeps items and trains. | Native (pipe capacity is config) | Re-plumbing the Offshore Pump and Boiler. Both pipe families speak NeoForge's fluid capability, so this choice is **independent** of (a) versus (b). |

The joule-per-FE constant (fact 1) is a prerequisite of all three.

---

## Tally

Counting the headline rows Oritech touches at all, cheapest level after splits:

- **Native** — circuit network (Oritech machines), accumulator, tier-1 modules, research gating, attribute augments, asteroid crushing recipes, disabling Oritech's worldgen.
- **KubeJS** — recycling (batched), planet-locked placement, survivor entries for every admitted Oritech recipe type.
- **Java on Oritech** — assembling machines, chemical plant, centrifuge, electric furnace, oil processing, steam engine, solar, pole reach, module tiers 2–3, productivity, beacon, laser turret, equipment grid, the drone port across dimensions, the agricultural tower, and the reactor as a steam source.
- **Shape-only** — finite ore, manual mining, trees, water, supply area, nuclear (as is), personal transport, logistic robots.
- **Core, standalone** — burner machines, the electric drill, the pumpjack, the Vulcanus foundry.

**The finding in one line:** Oritech carries the energy layer, the module system and most machine
bodies, but no Factorio machine *as shipped*. It needs output locking, fixed per-tier speeds and a
placement gesture, and each of those is one subclass. The hypothesis trades GregTech/MI's recipe
chassis for Oritech's machine chassis. It does not remove the core's Java; it moves it onto Oritech's
base classes. It **unblocks** Modules and beacons and reopens the reactor neighbour bonus.

## Out of scope, noted

- **Item layer.** 23 of 166 `data/pack/item-map.json` rows target a mod the hypothesis removes (16
  `gtceu:`, 7 `powergrid:`); 95 rows have no target yet. The item layer is cosmetic and delegable to the
  core; mapping it is a follow-on if the pivot is argued.
- **Corpus note, unrelated to Oritech.** `data/factorio/recipe.json` contains no `probability` key
  anywhere, though `scripts/factorio-recipe-extract.py:160-170` says it keeps one. `uranium-processing`
  therefore reads as one U-235 *and* one U-238 per ten ore. That is worth a ticket whatever happens
  to this survey.
