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

### 9. Subclass the abstract base, not the concrete machine — and the model comes free

Every abstract base in `block/base/entity/` takes a `BlockEntityType` in its constructor
(`MachineBlockEntity:82`, `MultiblockMachineEntity:27`, `MultiblockGeneratorBlockEntity:26`,
`UpgradableGeneratorBlockEntity:56`, `FluidMultiblockGeneratorBlockEntity:40`). Most *concrete*
machines do not: `SteamEngineEntity:74`, `FoundryBlockEntity:20`, `CentrifugeBlockEntity:48` and
`PumpBlockEntity:64` hard-code their own type in a `(pos, state)` constructor. A core subclass of one of
those cannot have a type of its own. Vanilla's `BlockEntity` constructor calls `validateBlockState`, and
a save reloads through the type's factory. Reusing Oritech's type would therefore bring the block back
as Oritech's class. **`DeepDrillEntity:82` is the exception.** It has a second constructor that takes a
type, with the comment "to allow addons to create custom deep drill entities with special logic".

So the route for a fixed-type machine is to **extend its abstract base and copy the concrete logic**.
CC0 makes copying free; the concrete classes run 70–300 lines. That changes the cost of a Java cell,
but it never makes a Java cell impossible.

**The visuals are one line.** Oritech registers each renderer as
`new MachineRenderer<>("models/foundry_block")` (`client/init/ModRenderers.java:24,40,45`). Both
`MachineRenderer` and `MachineModel` are public, and the path resolves into Oritech's own jar. A core
block entity that implements `GeoBlockEntity` and names the same path renders with Oritech's model,
texture and animations, and the pack ships no asset. A footprint larger than the model is a
`poseStack.scale` in a renderer subclass. Whether a scaled model reads well is `world-load (human)`.

---

## The matrix — base game

Ledger verdicts are today's. **Level** is the cheapest that closes every gap in the row.

| mechanic | ledger today | Oritech has | level | gap, and what closes it |
| --- | --- | --- | --- | --- |
| **Resource patches and finite ore** — the patch | `shipped` | Resource Nodes; `ResourceNodeFeature` | **Core** (ADR-0020, 0041, 0045) | There is nothing in the nodes to reuse. Each one is a bare `new Block(ofFullCopy(BEDROCK))` (`init/BlockContent.java:328-358`) with no block entity and no state, so it cannot hold an amount. The amount is the core's `OreBlock` plus `OreDelta`, and that stays. `ResourceNodeFeature` places a bowl on bedrock under a surface boulder, with each block replaced at random `nodeOreChance`. ADR-0045 wants a solid surface disc, one block deep, at Factorio's spacing, so the feature is the wrong shape and no config fixes that. **GregTech's departure orphans a layer here.** The outfield patches are GregTech ore veins today (`kubejs/data/gtceu/gtceu/ore_vein/*.json`), so a core feature has to replace them. `core/worldgen/TerraStartingArea.java` already places the starting area. |
| **Resource patches and finite ore** — the extractor on it | `shipped` | Deep Drill (`DeepDrillEntity`) | **Java** (see the electric drill) | The drill is reusable where the node is not: it *reads* the blocks under it and never breaks them, so fact 7 does not apply. As shipped it never depletes anything, because `craftResult` is private and only runs a recipe. A subclass draws each unit from the core's ore amount instead. |
| **Manual mining** | `adapted` | Hand Drill, Chainsaw, Promethium tools | **Shape-only** (ADR-0039) | Charge-based tools against an indestructible two-tier pick. Core keeps the Engineer's Pick. |
| **Trees and wood** | `adapted` | Tree Cutter (`TreefellerBlockEntity`) | **Shape-only** (ADR-0051) | A felling machine Factorio has none of, harvesting log-by-log; ADR-0051's tree is one entity. Unadmitted (fact 8). |
| **Mining drills** — burner | `adapted` | nothing that burns | **Core** | Stays the pack's rig (ADR-0040, 0043). |
| **Mining drills** — electric | `adapted` | Deep Drill (`DeepDrillEntity`); Destroyer frame + Quarry Addon | **Java**, on `DeepDrillEntity` | The Destroyer frame is out: it is a gantry that breaks blocks (fact 7). The Deep Drill fits nearly as shipped. It has a 3x3 footprint, which is Factorio's `tile_width`/`tile_height` 3 (`data/factorio/machine.json`). It is the one concrete class with a type-taking constructor (fact 9). Every method that needs changing is public. `loadOreBlocks` scans a 3x3; the subclass makes it a 5x5 (`resource_searching_radius` 2.49) and stores **positions**, where `targetedOre` stores `Block`s. `serverTick` changes to draw one unit through the core's amount every `mining_time / 0.5` s, at 90 kW. `getMaxRfInput()` returns 0 because the Enderic Laser is its only power source (`LaserArmBlockEntity:829`); the subclass returns a real rate and exposes energy on its cores through `getEnergyStorageForMultiblock` (null today). It is built from 26 machine cores, so single placement is fact 5. It is 3 blocks tall. The model is free (fact 9). This supersedes the core rig for the electric tier; the burner tier stays core. |
| **Mining drills** — pumpjack | `adapted` | Pump on oil springs (`OilSpringFeature`) | **Native** — finite oil, a departure ruled acceptable | **Ruling (2026-09-11):** finite crude is an accepted departure. You need several oil fields in Factorio anyway, and the infinite trickle is never enough on its own, so oil is finite like ore and the decaying-yield well goes. On those terms the pair is Native. *Placement* is two datapack files: `worldgen/configured_feature/oil_spring.json` (`number` 7, `blockId` `oritech:still_oil_block`) and `neoforge/biome_modifier/oil_spring{,_desert}.json`, retargeted at the pack's biomes. The spring is a sphere of oil source blocks with a column to the surface, and a fountain above ground when `easyFindFeatures` is on, so it is a visible well. Pool size is `max(number + variation, 13)` with variation in `[-number/2, number]` (`OilSpringFeature.placeStructure`). At the default 7, every spring is 13–14, and only values above about 9 change anything. *Extraction* is the Pump. It flood-fills the pool (up to `MAX_SEARCH_COUNT` 100,000) and drains one source block per bucket (`PumpBlockEntity:112-120`). **What stays non-Factorio, and is not config:** the rate is one bucket every `PUMP_RATE` 5 ticks at `ENERGY_USAGE` 512 FE per bucket (`:42-43`, `private static final`), a 16-bucket tank, and a 1x1 block where Factorio's pumpjack is 3x3. So Factorio's 10/s at 90 kW is not reachable without Java, and under the ruling that is part of the departure. The fluid becomes crude by retargeting the item layer (out of scope). Terra's GregTech crude deposit and the Fluid Drilling Rig leave with GregTech either way (`researchd.js:215`). If adopted, the ledger's *Infinite late-game resource (oil-style yield decay)* sub-rule becomes `excluded`. The pump reaching the pool through the column is **verified in a world** (human, 2026-09-11). |
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
| **Power generation** — steam engine | `planned` | Steam Engine | **Java**, on `MultiblockGeneratorBlockEntity` | Oritech's engine is the wrong shape for Factorio's. It burns steam at a rate that follows how full its tank is, up to `MAX_SPEED = 10`, on an efficiency curve. It returns **90% of the steam as water** (`WATER_RATIO`, `SteamEngineEntity:47,121`). It chains up to 20 engines onto one master. Factorio's burns `fluid_usage_per_tick` 0.5 flat, for 900 kW, and returns nothing. `SteamEngineEntity` has a fixed type, so the core copies `tickMaster` onto `MultiblockGeneratorBlockEntity` (fact 9). The copy takes the flat rate, no water, no chaining, and a 3x5 footprint, and keeps `steamId` = `planetaryfactory:steam`. The model is free. **The ledger's four-step chain collapses to Factorio's two.** ADR-0048 gave the engine a **rotation** output instead of electricity, and argued it purely from ADR-0036's choice of Power Grid: an engine that fed a pole would route around the wire solver. Power Grid leaves under the hypothesis, so that argument goes with it. The engine emits FE into the Supply Area Pole, which is Factorio's own entity. That is an ADR-0048 amendment if the pivot is argued. |
| **Power generation** — boiler | `planned` | `UpgradableGeneratorBlockEntity`'s steam mode | **Java**, optional | The Steam Boiler Addon only sets a flag. `getAdditionalStatFromAddon` sets `isProducingSteam`, and the steam logic lives in the generator base itself: a water-in/steam-out `boilerStorage`, and `produceEnergy` converting burn into steam (`UpgradableGeneratorBlockEntity:44,170-183`). A subclass sets the flag permanently and needs no addon. It needs three overrides. `tryConsumeInput` reads the pack's fuel table instead of `FuelRegistry` (fact 6). `produceEnergy` runs at Factorio's 60/s. `canFitEnergy` checks the steam tank, because as shipped steam mode returns `true` and voids steam into a full tank ("by design", `:163,174`), which breaks `BoilerCycleTest`'s stall rule. That is all reachable, but the core Boiler has already shipped with its checks (#224). Rebasing it buys Oritech's generator model and nothing mechanical. |
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
| **Vulcanus: lava and calcite** — the foundry | `planned` | Foundry (2 in, 1 out, no fluid); Centrifuge as the fluid pattern | **Java**, on `MultiblockMachineEntity` | Oritech's Foundry is an 80-line class with no fluid (`FoundryBlockEntity`). Its fluidless-ness is not a limit of the base class. `CentrifugeBlockEntity:40-175` puts a fluid-capable machine on the same `MultiblockMachineEntity` base: a `SimpleInOutFluidStorage`, `FluidApi.BlockProvider`, and fluid-aware `canProceed`, `getRecipe` and `craftItem`. A core foundry copies that pattern, with the tank always present instead of addon-gated, on a recipe type of its own. Casting (fluid in, items out) and melting (items in, fluid out) fit the one-fluid schema. A recipe with two fluid inputs does not (fact 2). The corpus has no `metallurgy` recipe to count, because the extract is pruned to Terra and only `machine.json`'s category map names the foundry, so re-extract before sizing. **The model is free**: `models/foundry_block` (fact 9), scaled from Oritech's 2x2 to Factorio's 5x5. Whether a model scaled 2.5 times reads as a foundry is `world-load (human)`. |
| **Vulcanus: lava and calcite** — lava as infinite | `planned` | Pump (only water counts as infinite) | **Java** (a two-point mixin) for the exact rule; **KubeJS** for a looser one | Oritech decides "infinite" in exactly two places, both hard-coded as `isSame(Fluids.WATER)` with no config or tag. `PumpBlockEntity:119` skips `drainSourceBlock` for water, and `FloodFillSearch.checkForEarlyStop` (`:344`) ends the search early on water. **Java:** a mixin (`@WrapOperation`) on those two calls that also accepts lava when the level is Ignus. The Pump then pumps lava exactly as it pumps water: it never drains, and it draws 512 FE per bucket. That is Factorio's lava, pumped by an electric pump where Factorio uses the unpowered offshore pump. **KubeJS:** set vanilla's `lavaSourceConversion` gamerule (`GameRules.RULE_LAVA_SOURCE_CONVERSION`, read by `LavaFluid.canConvertToSource`). The Pump still drains, but a lake refills itself the way an infinite water source does. Ignus is `ultrawarm` (`kubejs/data/planetaryfactory/dimension_type/ignus.json`), so its lava spreads every 10 ticks instead of 30 (`LavaFluid.getTickDelay`). The costs: the gamerule is **server-wide**, so lava is infinite on every body. Refill speed depends on the lake's shape and caps sustained throughput. Throughput is `world-load (human)`. The Offshore Pump accepting lava (ADR-0050) is still a third route. |
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
| **(b) Two currencies** | As (a), but rotation comes from Create's own sources, such as water wheels and windmills. Inserters are not electric. | none | none, but a Factorio fidelity loss with an `adapted` notice. If the Steam Engine emits FE (see its row), it is no longer a rotation source, and (b) rests on Create's generators alone. |
| **(c) Fluids to Oritech** | Oritech takes pipes, tanks and pumping; Create keeps items and trains. | Native (pipe capacity is config) | Re-plumbing the Offshore Pump and Boiler. Both pipe families speak NeoForge's fluid capability, so this choice is **independent** of (a) versus (b). |

The joule-per-FE constant (fact 1) is a prerequisite of all three.

---

## Tally

Counting the headline rows Oritech touches at all, cheapest level after splits:

- **Native** — the pumpjack and crude oil (finite, by ruling), circuit network (Oritech machines), accumulator, tier-1 modules, research gating, attribute augments, asteroid crushing recipes, disabling Oritech's worldgen.
- **KubeJS** — recycling (batched), planet-locked placement, survivor entries for every admitted Oritech recipe type.
- **Java on Oritech** — assembling machines, chemical plant, centrifuge, electric furnace, oil processing, the electric drill (on `DeepDrillEntity`), steam engine, boiler (optional), solar, pole reach, module tiers 2–3, productivity, beacon, laser turret, equipment grid, the drone port across dimensions, the agricultural tower, the Vulcanus foundry, lava as infinite (a Pump mixin), and the reactor as a steam source.
- **Shape-only** — manual mining, trees, water, supply area, nuclear (as is), personal transport, logistic robots.
- **Core, standalone** — burner machines, the ore patch itself (amount and placement).

**The finding in one line:** Oritech carries the energy layer, the module system and most machine
bodies, but no Factorio machine *as shipped*. It needs output locking, fixed per-tier speeds and a
placement gesture, and each of those is one subclass. The hypothesis trades GregTech/MI's recipe
chassis for Oritech's machine chassis. It does not remove the core's Java; it moves it onto Oritech's
base classes. It **unblocks** Modules and beacons and reopens the reactor neighbour bonus.

**Second pass, 2026-09-11.** Four rows were first marked Core without anyone reading the base classes:
the electric drill, the pumpjack, the boiler and steam engine, and the foundry. Reading them moved
three to Java, and fact 9 is why. A ruling then moved the pumpjack to Native, with finite oil.
What stays core is **data a block must hold**: an ore amount. Lava as infinite turned out to be a
two-point mixin on the Pump. Oritech has no block that holds a quantity. What moves is the
**machine body**, which Oritech has for every one of the four. Two consequences sit outside Oritech:
- GregTech's departure orphans the outfield ore veins and Terra's crude deposit. The veins need a
  core placer. Crude goes to Oritech's springs.
- Power Grid's departure takes ADR-0048's only argument for a rotation-emitting steam engine with it.

## Out of scope, noted

- **Item layer.** 23 of 166 `data/pack/item-map.json` rows target a mod the hypothesis removes (16
  `gtceu:`, 7 `powergrid:`); 95 rows have no target yet. The item layer is cosmetic and delegable to the
  core; mapping it is a follow-on if the pivot is argued.
- **Corpus note, unrelated to Oritech.** `data/factorio/recipe.json` contains no `probability` key
  anywhere, though `scripts/factorio-recipe-extract.py:160-170` says it keeps one. `uranium-processing`
  therefore reads as one U-235 *and* one U-238 per ten ore. That is worth a ticket whatever happens
  to this survey.
