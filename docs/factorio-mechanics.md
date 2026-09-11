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

`via` reuses `subgroup-owner.json`'s owner tokens — `gregtech`, `create`, `powergrid`,
`gcyr`, `pack`, `kubejs`, `native_mechanic` — and a value must exist in `index.toml`. **`mekanism` is
no longer one of them** (ADR-0035): the mod is out of the manifest, so a row naming it would fail the
must-exist rule. **`electro` is no longer one of them either** (#148): it named Create: Electro
Energetics, which Create: Power Grid replaced, and it never satisfied the must-exist rule in the
first place — the mod id was `electroenergetics`. The three rows that wrote `electro` (GCyR) meant
GCyR and now say `gcyr`. `candidates` is free
text and commits to no jar; **`pack` is admissible as a candidate only with a named mechanism**
(ADR-0015).

## Summary

### Base game

| mechanic | verdict | where |
| --- | --- | --- |
| [Resource patches and finite ore](#resource-patches-and-finite-ore) | `shipped` | Terra, Ignus, Sapros |
| [Manual mining](#manual-mining) | `adapted` | all bodies |
| [Trees and wood](#trees-and-wood) | `adapted` | all bodies |
| [Mining drills](#mining-drills) | `adapted` | all bodies |
| [Water as a resource](#water-as-a-resource) | `planned` | all bodies |
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
| [Combat: guns, ammo, turrets, walls](#combat-guns-ammo-turrets-walls) | `planned` | Terra |
| [Armor and the equipment grid](#armor-and-the-equipment-grid) | `planned` | Terra |
| [Capsules](#capsules) | `planned` | Terra |
| [Modules and beacons](#modules-and-beacons) | `blocked` | — |
| [Research and science packs](#research-and-science-packs) | `planned` | all bodies |
| [The technology tree](#the-technology-tree) | `shipped` | pack-wide |
| [Rocket silo and rocket launch](#rocket-silo-and-rocket-launch) | `planned` | all bodies |
| [Character movement on foot](#character-movement-on-foot) | `adapted` | all bodies |
| [Personal transport](#personal-transport) | `blocked` | — |
| [Terrain modification](#terrain-modification) | `adapted` | all bodies |
| [Repair and entity damage](#repair-and-entity-damage) | `blocked` | — |
| [Radar and map exploration](#radar-and-map-exploration) | `planned` | Terra |
| [The logistic request and trash system](#the-logistic-request-and-trash-system) | `excluded` | — |
| [Day and night cycle](#day-and-night-cycle) | `shipped` | Terra, Sapros |
| [Controls](#controls) | `planned` | all bodies |

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
- **owner**: ADR-0007, ADR-0019, ADR-0020, ADR-0021, ADR-0041, ADR-0045

Terra deals one ore shape: a filled disc of a single ore block, one deep, flush with the terrain
surface, at Factorio's own spacing. `scripts/worldgen-check.py` asserted it against GregTech's
registries and left with GregTech (ADR-0060), so nothing asserts it on 26.1.2. **ADR-0045 deletes Terra's buried veins entirely** — they were
ADR-0019's leftover default rather than a decision, and ADR-0043's surface-working rig made keeping
them a demand for the digging gesture ADR-0019 removed the caves for. Ignus and Sapros are unaffected.
*This entry described GregTech ore veins in chunk-aligned disc patches, retargeted onto the pack's own
ore blocks by ADR-0041 with the vein shape unchanged.*

Sub-rules:

- **Patches are finite and run out** — `shipped`. ADR-0020: the fix for exhaustion is another planet.
  Since ADR-0041 this is finite in the literal sense as well as the generated one: the patch holds a
  number of units and mining spends them, rather than being finite only because the disc has edges.
- **Stone is a resource patch, not scenery** — `shipped`, ADR-0041. Factorio mines stone out of a
  patch like anything else, and Terra now deals a fourth starting field for it, with its own vein
  beyond. *ADR-0021 ruled stone "ambient terrain, never a patch", on the grounds that
  "a stone patch in a world made of stone reads as a joke". ADR-0041 reverses it: the mechanism
  ADR-0021 discharged stone's bulk-material function onto was never built, and quarries exist on
  Earth because what makes one is concentration, not the rock being absent elsewhere.*
- **Ore is visible where it lies** — `planned`, ADR-0045. Every patch is on the surface, so finding
  one is exploration and the Radar reveals map rather than detecting ore — Factorio's own Radar.
  *This row read "ore is prospected, not stumbled on", `adapted` under ADR-0019: surface indicators
  first, an Ore Finder satellite later. ADR-0045 discharges that prerequisite rather than meeting it,
  and the indicators become dead.*
- **Infinite late-game resource (oil-style yield decay)** — `adapted`. #86: GregTech's bedrock
  fluid deposit decays to a floor rather than to zero, and Terra's crude deposit is one. `adapted`
  rather than `shipped` because the form is wrong in two ways — the deposit is a per-chunk roll
  under the bedrock rather than a patch you can see on the surface, and it is tapped by a Fluid
  Drilling Rig rather than by a pumpjack sat on a visible well. **Oil is the only resource that
  gets this**, which is the point: bedrock *ore* deposits would be infinite ore patches, so Terra
  carries none (ADR-0020 as amended, ADR-0021 as amended).
- **Resource richness varies per patch** — `shipped` at the design level, ADR-0041. An ore block
  carries an **amount** and mining draws one unit at a time, so richness is a real quantity rather
  than a patch size. The numbers are extracted, not chosen:
  `starting_amount = 20000 * base_density * (frequency_multiplier + 1) * size_multiplier`, and the
  per-block amount is that total over the blocks in the patch.
- **Patch spacing is Factorio's spots per km²** — `planned`, ADR-0045. `base_spots_per_km2` is
  extracted, not chosen: 2.5 for coal, copper, iron and stone and 1.25 for uranium, which is ~40 and
  ~56 chunks of mean spacing. An outfield patch is a train ride, not a belt run.
- **Regular patches are suppressed near spawn** — `adapted`, ADR-0045. Factorio's
  `starting_resource_placement_radius` (150) ships as a flat exclusion; the 300-block fade-in beyond
  it does not, because a structure set cannot express a ramp and 150 already keeps a rich patch off
  the tutorial.
- **Patch size rises with distance** — `planned`, ADR-0045. Quantity raises a spot's amplitude until
  `regular_blob_amplitude_at`'s cap and widens its radius past it, which is why a far patch is bigger
  as well as richer. The two split the quantity rather than multiplying it.
- **Richness rises with distance from spawn** — `shipped` at the design level, ADR-0041. Factorio's
  own term, `max((1000 + distance) / 2600, 1)`, ported metre-for-metre: flat inside 1600 blocks,
  linear beyond. This is why leaving the starting area early buys nothing. *ADR-0045 measures it from
  the world origin rather than from spawn — worldgen cannot see spawn, and one mechanic may not have
  two datums — and caps it, because past the amplitude crossover the uncapped term pays Factorio's
  radius growth out as richness.*
- **An ore tile shows its remaining amount** — `adapted`, ADR-0041. Factorio's eight sprite stages
  are kept as a material-independent ratio set (`stage_counts`), computed from the block's own
  amount; the exact number is a Jade line rather than a tooltip. `adapted` because no patch carries
  vein metadata, so they get stages and Jade but no map layer.

### Manual mining

- **verdict**: `adapted`
- **notice**: ~~mining is a Minecraft block break, so it is per-block rather than a hold-to-mine
  timer against a patch total.~~ **Corrected by ADR-0041**: an ore block carries an amount and one
  break gesture draws one unit from it, leaving the block standing until the amount is spent — so
  mining *is* per-ore, and "seconds per ore" is now literal. The player is not expected to do much
  of it: the burner drill is in the pocket at spawn (ADR-0040). It is **not** tool-tiered: one tool in two tiers, the Engineer's Pick,
  mines every block class, and a flat seconds-per-item stands in for vanilla's hardness spread —
  1.0s for Terra's five resources, halved to 0.5s by `steel-axe`. Factorio's two mining
  speeds are kept and so is the ratio between the tiers, but the mining time is **the pack's own,
  half of Factorio's**: 2.0s shipped first and failed ADR-0039's human-on-delivery check, because
  Factorio's number assumes an engineer who hand-mines thirty ore, not ADR-0019's 1150-block
  starting area. *This row read "per-block and tool-tiered";
  ADR-0039 reverses that half. Nothing supplied a tool at all, so the claim described a mechanic
  the pack could not deliver.*
- **where**: all bodies
- **via**: `native_mechanic`
- **owner**: ADR-0039

Sub-rules:

- **Mining speed is doubled by research** — `shipped` at the design level, ADR-0039. Factorio's
  `steel-axe` is a trigger technology costing no packs; the pack declares it as `CheckItemPresence`
  on 50 steel plates, because Researchd has no craft-triggered research method, and swaps its
  `character-mining-speed` effect for an `unlock-recipe` granting the Engineer's Steel Pick.
- **Picking up a placed entity is the same gesture as mining** — `adapted`. The Engineer's Pick
  absorbs GregTech's wrench dismantle verb, which rides the ordinary break path and is delivered by
  the two wrench item tags (ADR-0039). *This entry read "the wrench's rotate and pipe-connection
  verbs are `planned` and unowned"; #168 settled them.* The Pick also declares GregTech's
  `wrench_configure*` abilities, which set a **machine's** auto-output face — which side it pushes
  items or fluids into. *#168 first declined those, reading GTCEu's "Use Wrench to set Connections"
  string as meaning pipes; they are not pipes, and the decline was reversed in the same ticket once
  a machine turned out to have no way to be pointed at a Create belt. Create owning the belts is why
  the verb is needed, not why it is moot.* What stays declined is `wrench_connect`, the actual
  pipe-connection verb on the pipe block's own path: ADR-0017 gives fluid and item logistics to
  Create, GregTech's pipes left with its power layer, and ADR-0034's sweep leaves them unobtainable,
  so it would be declared against blocks Terra does not ship.
- **Rotating a placed entity (`R`)** — `shipped`, #168. The Engineer's Pick declares NeoForge's
  `wrench_rotate` ability, which is what GregTech gates the verb on; the wrench item tags never
  carried it, so until #168 the rotation overlay drew on every machine while the right-click did
  nothing. Declaring the ability was necessary and not sufficient: GregTech only sets a front face
  for a **sneaking** player, while vanilla skips a block's interaction entirely when a sneaking
  player holds a non-empty stack, so the one gesture GregTech accepts was the one that never
  arrived. The Pick answers `doesSneakBypassUse` to get past that, which is the hook NeoForge
  provides where GregTech's own tools use `onItemUseFirst`. Confirmed turning a machine in-game.
  *This entry read "`planned`, no owner yet".*

### Trees and wood

- **verdict**: `adapted`
- **where**: all bodies
- **via**: `pack`
- **owner**: ADR-0051
- **ticket**: #205

A Factorio tree is a **single entity**: one mining gesture removes it and yields its wood, with no
trunk, no canopy and no second gesture. Minecraft's log-by-log felling is a mechanic the pack
inherited rather than one Factorio has, so felling is re-authored in `planetaryfactory_core` — one
gesture at the base removes the connected tree and pays out at the base block.

The **yield diverges from the corpus on purpose**. Factorio's tree gives a flat `wood ×4`; the pack
gives **the log count of the tree actually broken**, so a jungle giant pays more than a birch. What is
kept from Factorio is the **rate**: `tree-01.mining_time 0.55` for `wood ×4` is **0.1375 s per log**,
extracted into `data/factorio/tree.json`, and the gesture costs `amount × 0.1375 s` — so a 4-log tree
costs Factorio's own 0.55 s exactly. The divergence is cheap because wood is terminal in Factorio:
five recipes consume it (`wooden-chest`, `small-electric-pole`, `shotgun`, `combat-shotgun`,
`tree-seed`) and no ratio downstream depends on it.

Sub-rules:

- **Felling is the base gesture only** — `adapted`, ADR-0051. A log with a log beneath it is
  mid-trunk and breaks normally, which is also what stops a touching canopy being felled from the
  wrong tree. The fill is bounded by count and radius; over the bound it fells what fits and leaves
  the rest standing.
- **A placed structure never fells** — `adapted`, ADR-0051. The fill requires at least one
  non-persistent leaf, which a build has none of. Nether stems fall out of this as a consequence
  rather than by name.
- **Leaves are removed with the tree** — `adapted`, ADR-0051. No drops, no decay ticks. Factorio has
  no leaves at all.
- **Felling time is halved by research** — `adapted`, ADR-0051. It rides ADR-0039's `steel-axe`
  ladder rather than declaring a second speed rule.
- **A sapling is crafted, not dropped** — `adapted`, ADR-0051. Factorio's wild tree yields only wood
  and `tree-seed` is a recipe costing `wood ×2`; the pack carries that over, so felling drops no
  sapling and the forest stays renewable through the recipe.
- **Trees are not a fuel or a science input beyond Factorio's own use** — `shipped`. The fuel table
  already carries `wood` as the tag `minecraft:logs` (ADR-0047).

### Mining drills

- **verdict**: `adapted`
- **where**: all bodies
- **via**: `planetaryfactory_core`
- **owner**: ADR-0043
- **ticket**: #105
- **notice**: Terra's two rigs are pack-authored and GregTech owns no drill here. A rig works the
  **layer directly beneath it** — Factorio's tiles, in a game that has a third axis — so a rig is
  placed on a patch rather than scanning downward for one. Its rate is the drill's `mining_speed`
  over the **resource's** `mining_time`, so uranium costs the same rig twice what iron does; its
  area falls out of `resource_searching_radius` and its output tile out of
  `vector_to_place_result`. *This entry read that a buried vein is reached by digging down;
  ADR-0045 deletes the buried veins, and the digging with them.*

Sub-rules:

- **Burner tier before electric** — `adapted`. The tier exists and is Factorio's own block rather
  than GregTech's steam stand-in. ADR-0040.
- **Drills output onto the tile they face** — `shipped` (#193). ADR-0043 reverses ADR-0040's
  `excluded`, which was argued entirely about belts and had deleted the drill-into-furnace pair as
  collateral. A rig pushes into an item handler on its faced tile and **otherwise stalls**, holding
  what it mined and burning nothing. *This entry read that it "else drops one item on the ground
  there and waits for it to be taken"; #182 showed the one-item-per-tile rule governs items already
  on the ground and that no Factorio machine spills when blocked, and #193 deleted the rule. This
  entry also read "Drills output onto a belt directly — `unargued`, no verdict".*
  **Auto-output is a prototype property and not a machine rule** — `vector_to_place_result` is
  carried by the mining drills and the recycler and by nothing else, which is why the pack's furnace
  is emptied rather than pushing.
- **Output onto a moving belt with no intermediate block** — `shipped`. A bare Create belt answers
  `Capabilities.ItemHandler.BLOCK`, so a rig faced at one puts ore on it with nothing in between,
  which is what a Factorio drill does. *This entry read `planned`, blocked on a rig not being able
  to reach Create's funnel. #182 closed that as `wontfix` on its premise: the funnel's inbound
  surface exists for Create's own transport handing over, and the right arrangement for a machine
  with a buffer is the funnel sitting **on** it in extract mode, pulling through the machine's own
  item handler — which needs no `DirectBeltInputBehaviour` call and no Create dependency. The
  entry also read that the item-handler path "ignores the belt's direction", which the bytecode
  refuted: the item lands on the queried segment and travels normally.*
- **A drill shows which tiles it is working** — `adapted`. The rig tints the top face of every ore
  block in its area, when looked at and when held for placement. Factorio shows this on a flat map;
  here it is a render on a surface the player walks on.
- **A body-locked large drill** (Vulcanus's Big Mining Drill) — `planned`, see [Planet-locked buildings](#planet-locked-buildings).

### Water as a resource

- **verdict**: `planned`
- **where**: all bodies
- **via**: `pack` (the Offshore Pump), vanilla water, `create` for pipes
- **owner**: ADR-0050
- **ticket**: #200

Factorio's water is infinite in volume and **fixed in place** — that property is the whole reason the
offshore pump exists and why shoreline is a siting concern. ADR-0050 keeps it with one rule: **water
is extracted and transported, never created.**

Sub-rules:

- **The offshore pump** — `shipped` (#213), pack-authored. ADR-0048 had made it `not_emitted` on the
  reasoning that Create's Mechanical Pump covered the water half; that block is a pipe-network pump
  and does not extract from the world at all, so ADR-0050 reverses the call. One adjacent source
  block, no minimum body size, no power (`energy_source: void`), 1,200 mB/s — which is exactly twenty
  Boilers at their extracted 60 mB/s. Placement is refused with a message where no source adjoins;
  the rate is read from the corpus and never typed, and `tests/pack/test_pump_assets.py` is what
  holds the copy honest.
- **Water source formation** — `excluded`. `waterSourceConversion` is off, forced by the mod on level
  load. Vanilla's 3x1x1 trench turns two buckets into unlimited water anywhere, which is water
  creation and defeats every siting constraint above it.
- **Buckets** — `excluded`. ADR-0050 refuses a 1,000 mB hand container beside ADR-0037's 50 mB barrel.
  Rung 0 reaches water by digging a channel from the hub pool, not by carrying it.
- **Water in the starting area** — `shipped` (#212). `scripts/build-terra-start.py` puts a pool in
  the hub itself — the hub's own blocks, one deep and flush with the ground, not a fifth jigsaw
  child that vanilla could drop silently. Factorio starts the player beside water and the pack has
  no bucket, so without it rung 0's water wheel waits on an unbounded walk ADR-0049's traversal
  budget has no room for. `tests/worldgen/test_start_geometry.py` asserts the pool's presence.
- **Placed flowing water** — `planned`. A pack outlet block maintaining flowing water from a pipe, for
  contraptions tidier than a dug channel. Safe without any tracking because what it places is never a
  source. Lands after the pump and pipes.
- **Water barrelling** — `shipped` via the barrel; see [Fluid handling](#fluid-handling). Hauling
  water in barrels is Factorio's own answer and is not a hole in the rule: the water still came from
  a pump on natural water.

### Fluid handling

- **verdict**: `planned`
- **where**: all bodies
- **via**: `create`
- **owner**: ADR-0017 as amended by #101 (Create owns fluid handling entire — pipes and pumps for
  moving, tanks for storing; Mekanism had no fluid role, and left the pack entirely with ADR-0035)
- **ticket**: #106

Sub-rules:

- **Barrelling and unbarrelling** — `shipped` as `native_mechanic`; `subgroup-owner.json`'s barrel
  shelves emit nothing because the mechanic already works (#93). The container is
  `planetaryfactory:barrel` (ADR-0037), pack-registered at Factorio's 50 units — 50 mB under the
  converter's 1:1 rule — stacking to 10. **Factorio's fluid restriction is not ported**: the barrel
  accepts any fluid, because that list is a content budget for nine items and eighteen recipes, and
  the pack has one container and none.
- **Underground pipes** — `excluded`. The same argument as underground belts, one level up: a
  Create pipe routes freely in three dimensions, so the crossing problem Factorio's pipe-to-ground
  exists to solve does not arise, and `create:encased_fluid_pipe` is decoration rather than a
  buried run. `subgroup-owner.json` marks `pipe-to-ground` `not_emitted` on that reasoning.
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
- **via**: `planetaryfactory_core` — the three tiers are pack blocks (#91, #149, #155)
- **owner**: #91
- **ticket**: #155

Sub-rules:

- **Ore smelts one-to-one straight to plate, with no intermediate step** — `planned`. Recorded in
  `subgroup-owner.json`; the pack does not get to add a hop.
- **A furnace recipe may consume more than one item** — `shipped`, #155. Vanilla's `SmeltingRecipe`
  holds an `Ingredient` with no count, so Factorio's `steel-plate` (5 plates to 1) has no vanilla
  shape at all; the pack's three furnaces read a count-bearing `planetaryfactory:smelting` type
  and **only** that one. `stone-brick` (2 stone to 1) rides the same type — ADR-0046 collapsed
  #87's earlier split, which had `stone-brick` take a vanilla 1:1 shape instead. The vanilla type
  is not read alongside it: under ADR-0034's sweep it carries no live recipe, which also makes
  Minecraft's food cooking gone rather than merely uncraftable (#183).
- **A machine with a blocked output stops** — `shipped`, #155. A furnace whose output slot cannot
  take the result does not start the smelt, burns no fuel and draws no EU; nothing is voided,
  overflowed or dropped. Factorio has no machine that ejects to the ground, and under ADR-0041
  Terra's ore is finite, so backing up is the only answer that does not destroy a resource the
  world cannot re-make.
- **Fuel is rated in joules per item, and a burner drains its own `energy_usage`** — `shipped`,
  #187, ADR-0047 (#185). Lighting an item banks its `fuel_value`; a working tick spends `energy_usage / 20`,
  which is 4,500 J on both burner furnace tiers. There is no burn-time number anywhere in Factorio and
  there is none here: burn time is a quotient, and an idle burner keeps the joules it has not spent.
  #155 shipped the tiers reading Forge's vanilla burn table instead, which is the defect ADR-0047
  closes — both burners drawing the same 90 kW is the actual reason the Steel tier gets twice the
  items from one coal. The burners' flame went with the tick model: they hold the same kind of
  thing the Electric tier holds, so they get the same horizontal gauge.
- **Fuel categories** — `shipped`, #187, ADR-0047 (#185). A furnace burns `chemical` and nothing else, as
  Factorio's do. Every fuel reachable today is `chemical` — coal, wood, solid fuel, rocket fuel and
  `nuclear-fuel`, which despite its name is an ordinary chemical fuel — so the filter currently
  excludes nothing. It is carried anyway: `uranium-fuel-cell` is the `nuclear` one, it has a
  `fuel_value`, and #135 will land it. An item with no row in the fuel table is not fuel, which is
  ADR-0034's default-deny applied to burning. `nuclear-fuel` has no `item-map.json` row, so it is a
  recorded skip in the join rather than a table row — the table names four items today: coal, wood
  (through `minecraft:logs`), solid fuel and rocket fuel.
- **No ore multiplication** — `planned`, settled by ADR-0032: cut pack-wide, Mekanism's ladder and
  Create's rung-0 Crushing Wheels alike. Yield gain by research or module is `blocked`, not
  `excluded` — the lab cannot express levelled research (ADR-0022 prunes 106 such technologies) and
  Terra is deliberately not compensated for its scarcity (ADR-0020). See #120.

### Assembling machines and recipe categories

- **verdict**: `planned`
- **where**: all bodies
- **via**: `pack`
- **owner**: ADR-0026, ADR-0029, ADR-0056
- **ticket**: #87 (the machines are registered; the recipe conversion is not)

Three pack-authored Assembling Machines. Recipe routing follows Factorio's own `category`
(ADR-0021), not the owning mod. ~~On a GT chassis~~ — **ADR-0056 takes GregTech out of the pack and
makes Modern Industrialization the chassis.** The three machines, their tiers and their recipe type
stay pack-authored; what changes underneath them is which mod supplies the block and the recipe
lookup.

Sub-rules:

- **`crafting_speed` as a machine property** — `planned`. ADR-0029 puts it on the machine, at
  Factorio's raw values (0.5 / 0.75 / 1.25), which is what makes `energy_required x 20` produce
  Factorio's own felt durations. It is also the only thing that makes the three tiers differ, since
  overclocking never fires above base tier.
- **`energy_usage` as a machine property** — `planned`. ADR-0029 emits no `EUt` on a recipe at all;
  a machine modifier supplies it, scaled so the Oil Refinery's 420 kW lands on LV's 32 EU/t.
- **Recipe selection in a machine** — `planned`. In Factorio a machine is *told* its recipe: the
  player picks it from a list, the machine displays it, holds it whether or not it is fed, and the
  setting copies to another machine. The pack has **no surface for this at all**, and that is the
  design gap, not the absence of a programmed circuit. GregTech's answer is the circuit, which
  ADR-0026 removed on purpose and #236 measured the cost of: GregTech keys its recipe lookup on the
  ingredient set, so with no circuit a colliding recipe is refused into the lookup at load and 44 of
  139 emitted recipes never reach the machine.

  ADR-0056 removes that mod, and Modern Industrialization's **locked output slot** is the surface.
  One mechanism does three jobs: it selects the recipe (a locked slot refuses a rival recipe's
  product, so that recipe fails its own start simulation), it shows which recipe is selected, and it
  guards against overfill. The lock persists in NBT and survives an empty slot, so the selection
  holds whether or not the machine is fed, and EMI's Fill Recipe sets it with the ingredients absent.
  Machine-configuration copy/paste is not in MI itself and comes from a third-party addon.

  `planned` rather than `shipped`: the mechanism is chosen and proven in play, and nothing is built
  here yet. Its checks are a static assertion that no two emitted recipes of one type share an
  ingredient set (#237) and an in-world test that locking covers every collision group (#238) —
  the candidate filter is the *product*, so it disambiguates a group only where the group's members
  have distinct outputs.
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
- **via**: `planetaryfactory_core`
- **owner**: ADR-0038, `docs/gdd.md` §5
- **ticket**: #160, #161, #99, #100, #140

The crafting grid is removed (#90) and the Personal Assembler replaces it permanently (#95) — it is
the player's only hand-crafting surface, not a bootstrap crutch, and every fluid-free `crafting`
recipe reaches it (#88). The 2x2 inventory grid is removed in fact as of #140, in
`planetaryfactory_core` rather than by recipe removal, because it is a vanilla menu and no recipe
removal reaches it (ADR-0034).

**Chain-crafting is the mechanic, not the timer.** Factorio's wiki names it as what separates the
hand from an assembling machine: request a recipe whose ingredients you lack and the sub-crafts are
queued for you. The pack reproduces it as a resolved **Crafting Plan** on Applied Energistics 2's
autocrafting shape — an amount dialog, then a flattened plan naming every intermediate and every
shortfall, then one commitment that pays the whole cost (ADR-0038). Two departures from Factorio are
deliberate and recorded there: cancellation takes the plan as its unit, and a plan with a missing
ingredient cannot be started.

**The Assembler ships in `planetaryfactory_core`**, not as pack scripting: KubeJS cannot register a
menu or a screen on 1.21.1 (#96, ADR-0015). It has no recipe type of its own — the hand-craftable set
is a predicate over Assembling Machine 1's recipes (#88), so one emitted recipe serves both surfaces.

**The queue's slowness is serial, not a multiplier.** The character prototype sets no `crafting_speed`
at all -- it is not a crafting machine -- so Factorio hand-crafting runs at exactly `energy_required`
seconds. What makes it slow is that the queue is serial: one plan at a time, no modules, no
parallelism. The pack reproduces the mechanism rather than approximating it with a penalty, and
ADR-0029 gives the Assembler speed 1 with durations of `energy_required x 20` unmodified.

### Transport belts

- **verdict**: `adapted`
- **notice**: there is one belt and you buy its speed with RPM, so the belt ladder is a
  power-and-gearing problem rather than three tiers and two research nodes — and the lane-and-tunnel
  patterns a Factorio player has memorised do not transfer, because most of them are answers to
  being flat. Routing in Y replaces them (ADR-0044).
- **where**: all bodies
- **via**: `create`
- **owner**: ADR-0044, ADR-0017, #93

Sub-rules:

- **Three belt tiers** — `adapted`. There is one belt, and its throughput is the RPM you drive it
  at, so belt speed is a power-and-gearing decision made per run rather than three craftable tiers
  bought from the tech tree. Faster belts are therefore never a research unlock here.
- **Throughput as a ratio budget** — `planned`. Factorio's belt has a known items/s *and* a bounded
  researched stack multiplier, which is why a ratio is computable; Create's is `RPM/24` entries per
  second with an items-per-entry that is whatever the upstream inserter handed over, unbounded to 64
  and surfaced nowhere. ADR-0044 defers the target to play — the question is whether a single belt
  ever bottlenecks a line before the machines do — and names the dials: the `getSpeed() / 480f`
  divisor in `BeltBlockEntity.getBeltMovementSpeed()` first, `maxRotationSpeed` second. Not
  `blocked`: the implementation is known, the number is not.
- **Underground belts** — `excluded`. Not for want of a Create block: undergrounds solve a *weaving*
  problem — two lanes past each other in a fixed footprint — that exists only in two dimensions.
  Minecraft has a Y axis and Create has sloped belt runs, so a belt that must cross another goes
  over it. Argued from the medium, not from a mod's shortfall (ADR-0044).
- **Splitters, with filtering and priority** — `adapted`. Create's tunnels are the splitter: a
  tunnel splits a belt's output across the belts beside it and filters what goes where, one filter
  slot per output. Brass Tunnel's seven `SelectionMode` values include `FORCED_SPLIT` and
  `FORCED_ROUND_ROBIN`, which refuse to distribute unless every target can take its share — that is
  a balancer — and `PREFER_NEAREST`, which is positional priority. *This entry read "the balancer
  built out of splitter pairs … is not buildable, and there is no output priority", which was wrong
  on the facts.* What is genuinely absent is the **constructed pattern**: a balancer assembled from
  splitter pairs, rather than the outcome one block delivers. ADR-0044 takes the outcome as what the
  pack promises.
- **Two lanes per belt** — `excluded`. Lane balancing is a compression trick for a conveyor one tile
  wide on a plane — what you do when the only free axis runs along the belt. It goes with the
  undergrounds and for the same reason (ADR-0044). *This entry read `by-consequence` of Create
  having no lane model; the ledger now owns a reason of its own.*
- **Belt as buffer** — `excluded`. A 64-block belt at one item per block holds 64 items where a
  64-tile yellow belt holds 512. Using belts as storage is a real Factorio idiom and ADR-0044 drops
  it knowingly rather than by oversight.

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
row's losses to whatever actually survives. #178 scoped the inserter family alongside the belts and
**ADR-0044 explicitly does not decide it**, handing it back to #102 unblocked: the conveyance is
settled, the swing arm is a separate mechanic with its own fidelity argument. For that argument, the
Arm reaches 5 blocks against an inserter's 1 (2 long-handed), moves up to a full stack per cycle at
roughly 2–2.5 transfers/s at maximum RPM, and does not implement `DirectBeltInputBehaviour` — it uses
the separate `ArmInteractionPointType` registry.

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
  construction network is that decision applied to building. The second half of that argument — "a
  network with nothing to repair" — died with ADR-0055; see
  [Repair and entity damage](#repair-and-entity-damage). ADR-0017 still carries the row on its own.
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
- **via**: `native_mechanic`, `create`, `powergrid`
- **owner**: ADR-0030, and #148 for which block the four device rows name

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
- **Lamps and display panels as readouts** — `adapted`. Nixie Tubes and the Display Link for the
  redstone-driven readout, and, since #148, **Power Grid's own devices for the placed hardware**:
  `small-lamp` is the Light Fixture, `display-panel` the Modular Display, `power-switch` the
  Contactor. They are grid-powered rather than signal-powered, which is the seam — redstone decides,
  the grid drives.
- **An alert that fires on a condition** — `adapted`, and the weakest row on this shelf. Factorio's
  programmable speaker plays a chosen sound and raises a named alert; #148 borrows Power Grid's
  **Alarm Bell**, which rings when powered. The trigger survives and the programmability does not —
  one sound, no alert text, no per-signal selection.
- **Two independent networks on one wire (red and green)** — `excluded`. Redstone has one channel;
  the whole trick of running two circuits down one pole has no analogue.
- **Circuit-controlled inserters and belts** — `unargued`, no verdict, and it depends on #102's
  answer about the Mechanical Arm.

**The supply question is separate and still open.** #58 cut redstone from Terra entirely — no vein,
empty `underground_ores` step — so the mechanic exists while its crafting material does not, and
#62 already records the same problem hitting the authored green circuit. That is a resource question
for #25, not a verdict on the mechanic, and the two were previously conflated in this row.

**The other axis — whether the eight recipes are emitted — was settled by #148, not by this row.**
It was open because ADR-0030 deliberately left it to `subgroup-owner.json`: a mechanic supplied by
vanilla and Create needs no emitted recipe to exist, so `not_emitted` would have contradicted nothing.
The Electro-to-Power-Grid swap is what put candidate blocks on the table, so the swap decided it, and
the shelf now **splits** rather than going one way whole:

- **The four rows naming a physical device the network drives are emitted**, borrowing a Power Grid
  block — Light Fixture, Modular Display, Contactor, Alarm Bell.
- **The four combinators are `not_emitted`.** Their job is arithmetic and decision, and Power Grid
  ships nothing that does either: a potentiometer is a dial, a relay is a switch. Naming one anyway
  would put a Factorio name on a block that does not do the Factorio thing, and ADR-0030 already
  supplies the capability from the comparator and Create's switches — so the player loses a recipe
  and keeps the mechanic. `selector-combinator` is doubly unemittable: its recipe takes five
  `decider-combinator`.

This is the same split the sub-rules above describe, seen from the recipe side: **redstone decides
and the grid drives**, so the deciding half needs no recipe and the driven half does.

### Electric network and transmission

- **verdict**: `adapted`
- **notice**: the grid is a modelled electrical system rather than an abstract pool — poles carry a
  real voltage over wire with a real gauge, the run loses power over distance, and a bad circuit
  damages components instead of merely underfeeding them.
- **where**: all bodies
- **via**: `powergrid`, `pack`
- **owner**: ADR-0017 as amended by ADR-0035 and ADR-0036

**Create: Power Grid owns the grid** — poles, wire and catenary — and GregTech's power layer
was removed entire to make room for it, cables included. *Amended by #148: this read "Create: Electro
Energetics", which the swap replaced. The row did not change hands, only mods — the acceptance test
was brownout propagation and a wire-tier ladder, and both mods were adopted for passing it.* A **pack-authored supply-area pole**
(ADR-0036) distributes inside an area, which is the one seam: the grid moves power between places,
the pole feeds the machines standing in one. *Amended by ADR-0035: this read "Mekanism's Universal
Cables distribute inside an area"; the mod left the pack and in-area distribution became the pack's
own.*

The mod runs at **shipped physics defaults** (ADR-0017), which is the decision this row turns on:
voltage drop, per-material wire gauge, grounding, fuses, brownouts and component damage are all on.
Flattening resistance to the config floor would delete the wire-tier ladder that is the reason to
adopt the mod at all.

An earlier version of this row named GregTech and called brownout `excluded` on the grounds that GT
machines stall rather than derate. Both halves were wrong — GT has no power layer here, and the mod
that replaced it models brownouts natively.

Sub-rules:

- **Brownout: insufficient supply degrades what is running** — `blocked`, and the row has now been
  wrong in both directions. Power Grid models sag on the *grid* natively, which is what the previous
  `shipped` verdict rested on. But the machine side does not derate: `RecipeLogic.regressRecipe`
  takes progress *down* by two per waiting tick (`recipeProgressLowEnergy: false` in
  `config/gtceu.yaml`), so a machine that can afford its full EU/t on a fraction `f` of ticks nets
  `3f - 2` progress per tick and **never completes anything below f = 2/3**. Factorio has a slope
  there; the pack has a cliff, with no signal separating "slow" from "permanently stuck". The
  pole's water-fill makes it worse rather than better: sharing a shortfall evenly puts every machine
  in an area under the cliff at once instead of stalling the hungriest. Verified by disassembly
  against GTCEu 7.0.2 while building #147; the fix is #157, and it is an ADR's worth of argument
  because GT will not derate without touching recipe logic that ADR-0036 forbids reaching into.
- **Voltage tiers** — `adapted`. Factorio steps low to medium to high voltage at the transformer;
  Power Grid's ladder is wire gauge and material — copper, iron and gold, each with its own
  resistance, span and current ceiling as physical data rather than a voltage rating in a `.toml` —
  so upgrading a run means rewiring it rather than swapping a pole tier.
- **Power poles have a supply area** — `shipped`. A pack-authored block in
  `planetaryfactory_core` (ADR-0036, #147) that scans its area on a tick and pushes into every
  machine inside it. Three tiers, 5x5, 7x7 and 18x18, Factorio's own numbers.
- **Power poles have a wire reach** — `excluded`. The two halves used to be one row, which is what
  let Factorio's big pole survive as a candidate: it justifies a *smaller* supply area, 4x4, by
  buying 30 tiles of reach. Here the wire has the span — Power Grid's catenary, as a material
  property — and the pole it hangs from does not enter it, so there is no reach for a pole tier to
  differ in and the big pole is dropped (`not_emitted` in `data/pack/item-map.json`).
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
- **via**: `pack`, `create`, `powergrid`
- **owner**: ADR-0017 as amended by #104, #148 and **ADR-0048, which supersedes #101**. `via`
  is ordered along the chain: the pack's Boiler, the pack's Steam Engine, Power Grid's generator
  assembly, the pack's Steam Turbine. *#101 read "the grid mod owns steam and solar"; ADR-0048 makes
  both steam fluids `planetaryfactory:` and leaves the grid mod owning solar. Power Grid never
  touches steam — its generator takes rotation in and puts volts out.* *#148: the third step was
  Electro's Alternator, a single block; Power Grid's counterpart is a **built assembly** rather than
  a fixed structure — a Stator of Coils on Shafts, an Armature of Rotors, a Commutator and a
  Generator Clutch, coupled to a Create kinetic network and needing an excitation current — standing
  in exactly the same place in the chain.* **`mekanism` was struck by #104** — the pack installs base
  Mekanism, which registers no generator block at all, so the clause naming it never named anything.
  **`gregtech` was struck by ADR-0048**: the boiler is the pack's, and `create` is now on the row for
  the rotation the pack's Steam Engine emits rather than for an engine of Create's own.
- **ticket**: #104, #189, #224

Sub-rules:

- **Boiler and steam engine as the first power** — `adapted`. The chain is **four** steps, not two:
  the **pack's Boiler** burns solid fuel and makes low-temperature steam, the **pack's Steam Engine**
  eats that steam and emits Create rotation, Power Grid's generator assembly turns SU into watts, and
  the grid carries them. A Factorio player's boiler-and-engine pair has a rotational stage wedged in
  the middle of it, and the grid is granted at a rung rather than arriving with the first fire.
  *(#104 corrects "three steps" and "a Create Steam Engine burns fuel": the Steam Engine burns
  nothing — it is the prime mover.)* **ADR-0048 re-cut both of the first two steps.** The first was
  #37's LP Solid Boiler; the boiler is now pack-authored, one tier, under ADR-0047's burner model —
  the third customer of the buffer the Furnace and the Burner Mining Drill already share, and #224
  shipped it: `planetaryfactory:boiler`, fuel and water in, low-temperature steam out at Factorio's
  own 60 mB/s. The second
  was *"a Create Steam Engine turns that steam into rotation"*, and it **was never implementable**:
  Create has no steam fluid at all. Its boiler is a Fluid Tank multiblock holding **water**, heated
  by Blaze Burners, and the Steam Engine mounts on that tank — it cannot consume steam from a pipe,
  from any boiler or from anything else. The pack authors that step. The engine emits rotation and
  not electricity on purpose: an engine that fed a pole directly would route around every mechanic
  ADR-0036 selected Power Grid for.
- **Solar panels and accumulators** — `planned`, and the grid mod's outright: Power Grid ships a
  real-PV Solar Panel and the Battery the pack borrows as Factorio's accumulator (#148). It is also
  the *planet* Electro's identity — see [Day and night cycle](#day-and-night-cycle).
- **Steam as a stored, pipeable intermediate** — `planned` (#189), and **two fluids rather than
  one**. ADR-0048 registers low-temperature steam, which the Boiler makes and the Steam Engine eats,
  and high-temperature steam, which ADR-0033's reactor emits and only the Steam Turbine takes — two
  registry entries rather than one fluid carrying a temperature, because Factorio has exactly two
  temperatures with exactly two consumers. Both are `planetaryfactory:`.
- **The Steam Turbine, on superheated steam** — `planned`, the pack's, and **the pack's only FE-side
  generator**. *Moved here from [Nuclear fission](#nuclear-fission) by #104*: ADR-0033 names the row
  **for the fluid, not for fission**, and the Turbine has two producers on two bodies — Terra's
  Nuclear Reactor and Ignus's acid neutralisation. Filing a cross-body generator under Terra's
  fission chapter hid what it is. Superheated steam is a **pack-owned fluid** — `planetaryfactory:`,
  not GregTech's, since `gtceu:steam` is not inert and GregTech's own steam machines accept it,
  which would re-open the power layer #37 removed (ADR-0048; this corrects an earlier "its own GT
  material") — and **only the Turbine accepts it**; ordinary steam keeps the four-step chain above, which the Turbine will not take, and
  that fluid split — not the Converter — is what stops it retiring the rung-1 generator assembly. **Not
  registered yet** (#107's siblings): ADR-0033's stated design, unbuilt.

### Nuclear fission

- **verdict**: `adapted`
- **notice**: the reactor emits **superheated steam** directly and there is no heat layer, so
  Factorio's reactor-to-exchanger ratio and heat-pipe layout puzzles do not exist here. The chapter
  is fuel chemistry and a steam budget, not a thermal one.
- **where**: Terra
- **via**: `pack`
- **owner**: ADR-0033
- **ticket**: #89 (closed)

**Settled by ADR-0033: the chapter ships, pack-authored.** Factorio's own tech costs place it —
`uranium-mining`, `uranium-processing` and `nuclear-power` are all chemical science, so rung 3;
`nuclear-fuel-reprocessing` adds production, so rung 4; **Kovarex costs space science** and is
post-launch, so Terra runs at raw 0.7% U-235 exactly as Nauvis does. Three pack machines on a GT
chassis — Centrifuge, Nuclear Reactor, Steam Turbine.

**Mekanism was refused, and the earlier `via: mekanism` was a mistake of fact**: the Fission Reactor
lives in **MekanismGenerators**, which this pack does not install. Adopting it would have brought six
further generators onto ADR-0017's Power generation row. **#104 later struck that row's Mekanism
clause on the same fact**, and the refusal here is why: base Mekanism registers no generator block.

**The Steam Turbine is not on this row.** #104 moved it to [Power generation](#power-generation),
where ADR-0033's own framing puts it — the row is named for the fluid, not for fission, and the
Turbine's second producer is Ignus's acid neutralisation, on a body with no reactor. This row keeps
the Reactor, the Centrifuge and the fuel chain.

Sub-rules:

- **Kovarex enrichment** — `blocked`, and correctly so: it costs **space science** in Factorio, so it
  belongs to the post-launch map rather than to Terra. Terra's 0.7% yield is the fidelity, not a gap.
- **Reactor neighbour bonus** — `excluded`. `by-consequence` of adopting a multiblock reactor: there
  is nothing to place next to anything, so the layout puzzle has no board.
- **Heat pipes and heat exchangers as a separate transport network** — `blocked`, **moved to Gelida**
  by ADR-0033. Terra's reactor needs no heat layer, and Aquilo's mechanic is the one that actually
  requires the real thing: buildings freeze by **adjacency** (one tile, orthogonal or diagonal, above
  30 °C) rather than by plumbing, with per-entity draw and an immunity list, while heat pipes buffer
  1 MJ/°C over 500–1000 °C and flow only down a differential. A fluid cannot express coverage.
  Factorio's own two thresholds — **≥500 °C for a heat exchanger, ≥30 °C to keep a building warm** —
  are the seam: the conduction layer is one build, the freezing layer another. Both are Gelida's.

### Pollution

- **verdict**: `planned`
- **where**: all bodies
- **via**: `kubejs`, `pack`
- **owner**: ADR-0055 (supersedes ADR-0005)
- **ticket**: #109, #118

GTCEu 7.0.2 has no pollution system — the mod contains nothing matching `pollut` — so Emission is
ours and none of it is built yet.

**ADR-0005's EU/t proxy was superseded before it shipped.** That ADR scored Emission per chunk off
the **EU/t draw of running GT machines** and rejected per-entity rates as costing "tagging every
recipe in a GregTech pack". Factorio states emission per prototype as
`energy_source.emissions_per_minute`, and that field is in the dump the extractors already read —
extraction is not tagging, and the pipeline that makes it free was built *after* ADR-0005. ADR-0055
takes the corpus rates instead. The proxy also misranks: a boiler pollutes far more per joule than
an assembler, and an idle machine emits its idle rate rather than zero, so under EU/t a coal-fired
base and an electric one of equal draw are equally dirty.

Per-chunk accumulation, decay and diffusion are unchanged from ADR-0005 — they are what make the
score a spatial problem and outpost placement a decision.

Sub-rules:

- **Spread to neighbouring chunks, and decay over time** — `planned`. Both, and they are what makes
  outpost placement a decision.
- **Absorption by terrain and trees** — `unargued`, no verdict.
- **Per-planet consequences** — `blocked`. Named in principle, unspecified everywhere but Terra;
  migrated here out of `docs/gdd.md` §8.

### Enemies and evolution

- **verdict**: `planned`
- **where**: Terra
- **via**: `pack`, `native_mechanic`
- **owner**: ADR-0055
- **ticket**: #118

Nothing here is built. **The shape changed wholesale with ADR-0055**, which reversed the previous
entry in this row: emission was to attract *Illager raids to an Overseer at your outpost*, with no
nest, no expansion and no evolution factor. That design existed because `minecraft:raid` is
village-anchored and needed something to path at. It was never argued against Factorio's own loop,
and `docs/gdd.md` §6 describing it is stale prose with no standing (ADR-0054).

Factorio's loop is one mechanism: nests absorb the pollution that reaches them, and absorbed
pollution is what buys the attack groups. The raid *is* the nest's output. The pack reproduces
that, with nests and waves held as saved data and entities as their rendering — the Dormant Siege's
own idea, applied to the thing it is cheaper to apply it to.

Sub-rules:

- **Pollution triggers attacks** — `planned`. Via nest absorption, not a threshold on the outpost.
- **Attacks are state until a player is present** (the Dormant Siege) — `planned`. Generalised by
  ADR-0055: nests and in-flight waves are both records, and mobs instantiate near the player a wave
  is aimed at. A raider abandons its raid beyond 112 blocks, so a wave cannot walk Factorio's
  distances as entities.
- **Nests, expansion and clearing territory** — `planned`. ADR-0055. Was `excluded`/`by-consequence`
  on the raid design; expansion runs on its own timer rather than on the cloud, capped by the same
  distance-density rule that placed the original nests.
- **Evolution factor rising with pollution and time** — `planned`. Was `blocked` for want of a nest
  to evolve. One global scalar on all three of Factorio's inputs — time, emission produced, and
  nests destroyed — the third being why clearing the map is not a permanent win.
- **Enemies destroy structures** — `adapted`. Factorio's biters eat walls and turrets; Minecraft
  mobs grief nothing, so this is ours to build, and it is bounded to the
  `planetaryfactory:destructible` tag rather than to anything in the way.
- **Gleba's pentapods** — `unargued`, no verdict. `docs/planets.md` marks them TBD.

### Combat: guns, ammo, turrets, walls

- **verdict**: `planned`
- **where**: Terra
- **via**: `pack`
- **owner**: #118
- **ticket**: #118

**This was the canonical `by-consequence` row, and #118 reversed it.** #26 dropped Military science
because its ingredients feed nothing downstream, and seven `combat/*` shelves went `not_emitted`
behind it — turrets, guns, ammo, armor, capsules, equipment and walls. Nobody decided this pack has
no combat; a science-pack pruning decided it for them. The premise is now false: with ADR-0055's
nests on the map, those ingredients feed the thing you defend against and the thing you go and
clear, so **all seven shelves come back together** rather than two of them staying cut for a reason
nobody believes.

Sub-rules:

- **Turrets fed by ammo items** — `planned`. Not powered-and-free: ammo is a production cost with a
  research ladder, and Factorio deliberately shares magazines between the gun turret and the SMG,
  so one ammo line has two customers.
- **Personal firearms** — `planned`. Pistol and SMG, sharing the turrets' ammo. A player expected to
  go and clear a nest needs something to clear it with.
- **Walls** — `planned`. Factorio's wall and gate, and the designated member of the
  `planetaryfactory:destructible` tag — without one, every player picks a different block and the
  mechanic has no shape.
- **Military science returns** — `planned`. #26's pruning is reversed on its own stated reason; the
  Factorio tech tree gates `military-2/3/4`, the laser/rocket turrets, `railgun`, `uranium-ammo`,
  the shields and the power-armor rungs behind it, and ADR-0022 imports that tree as data precisely
  so its prerequisites are not retyped. Reopens #26 and touches ADR-0018.
- **Mechanism is first-party; art and possibly logic are delegated** — `unargued`. A research ticket
  specifies which third-party mods supply models, textures and any borrowed behaviour, and what
  their licenses permit. The `build-pick-textures.py` precedent derives art from a jar the pack
  already depends on, and that reasoning does not transfer to a mod the pack would install only for
  its assets.

Note that `not_emitted` did **not** settle the shelf even while the row read `excluded`:
`combat/defensive-structure` is `not_emitted` and #57 still shipped a Radar. That is the proof case
for the two axes never reading each other, and it is why reversing this row is a ledger edit rather
than a regeneration.

### Armor and the equipment grid

- **verdict**: `planned`
- **where**: Terra
- **via**: `pack`
- **owner**: #118
- **ticket**: #118

Same #26 cascade, reversed with the rest of it. **The old note here was stale twice over**: it read
"MekaSuit is the spacesuit (`docs/gdd.md` §1), and a MekaSuit *is* an equipment grid", but ADR-0035
removed Mekanism from the pack jars and all (#146), and `docs/gdd.md` §1 now says so itself. The
mechanic does not ship under another name; nothing in the pack expresses it.

Factorio's shape is a six-rung ladder — `light-armor`, `heavy-armor`, `modular-armor`,
`power-armor`, `power-armor-mk2`, `mech-armor` — with the grid arriving at `modular-armor` and
seventeen equipment technologies above it. Factorio has **no** space suit; `grep -i
"space-suit|spacesuit|oxygen|pressure|life-support"` over `data/factorio/*.json` returns nothing
across 163 recipes and 162 technologies. Whether GCyR's suit and its `enableOxygen` default survive
contact with the armour ladder is a reconciliation downstream of this row, not an input to it — the
pack builds GCyR from source (ADR-0001), so it is a thing that changes rather than a constraint to
route around.

### Capsules

- **verdict**: `planned`
- **where**: Terra
- **via**: `pack`
- **owner**: #118
- **ticket**: #118

Same #26 cascade, reversed with the rest of it. The seven shelves shared one stated reason and it is
false, so leaving this one behind would keep a shelf cut for an argument nobody holds. `personal-roboport`
in the utility-equipment shelf will collide with ADR-0017's one-mod-owns-each-capability rule, since
Create owns logistics before AE2 — that needs its own argument, and it gets one rather than
inheriting a dead premise.

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
- **Military science** — `planned`. #26 dropped it because its ingredients fed nothing downstream;
  #118 makes them feed the combat line, so the pruning is reversed on its own reason. See
  [Combat](#combat-guns-ammo-turrets-walls). Reopens #26 and touches ADR-0018.
- **Sapros's science pack spoils** — `planned`. The buffer-as-liability puzzle.
- **Research consumes packs continuously while running** — `adapted`. Researchd's Lab consumes on
  completion of a pack batch rather than metering a rate; only `consumePack` reads the Lab.
- **A lab draws power, so research competes with the factory for it** — `planned`, #103. Researchd's
  Lab carried no energy handler when #103 was closed; the maintainer has since accepted a PR adding
  one, so the work is upstream at Porting-Dead-Mods/Researchd#21 and the pack's side lands with the
  ADR that follows it.

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
- **via**: `gcyr`
- **owner**: #41, ADR-0006
- **ticket**: #25 — the map *is* this row's ticket, being Terra's flow to the first rocket launch

Sub-rules:

- **The launch is a physical, watchable event** — `planned`. GCyR's `RocketEntity`, and
  `docs/gdd.md` §4 makes it explicit that the launch is the payoff and is never simulated.
- **Rocket parts are produced continuously and buffer in the silo** — `unargued`, no verdict.
- **Cargo landing pad** — `planned`, the post-launch arc.

### Character movement on foot

- **verdict**: `adapted`
- **where**: all bodies
- **via**: Minecraft's own walk, against Terra's starting-area distances
- **owner**: ADR-0049, #207

Base movement on foot only. Vehicles are [Personal transport](#personal-transport) and #121; the two
do not collide.

Factorio's engineer and Minecraft's player do not walk at the same speed, and the pack does not
change that. The traversal budget has two halves, and both are extracted rather than felt:

| | Factorio | Terra |
| --- | --- | --- |
| speed | `character.running_speed` 0.15 tiles/tick × 60 = **9.0 tiles/s** | Minecraft's walk **4.317 blocks/s** (sprint 5.612) |
| furthest starting resource | `starting_resource_placement_radius` **150 tiles** | `DISTANCES` in `scripts/build-terra-start.py`, furthest field **62 blocks** |
| hub to furthest field | 150 / 9.0 = **16.7 s** | 62 / 4.317 = **14.4 s** |
| two fields, perpendicular | 212 / 9.0 = **23.6 s** | 88 / 4.317 = **20.3 s** |
| two fields, opposite | 300 / 9.0 = **33.3 s** | 124 / 4.317 = **28.7 s** |

A tile and a block are both one metre, so nothing is converted but the tick rate. The speed is read
in `scripts/factorio-resource-extract.py`'s `character_movement()` into
`data/factorio/resource.json` and asserted by `tests/factorio/test_resource_extract.py`; the radius
is the corpus constant the same file already carried.

**Both halves drifted, in opposite directions, and they cancel.** Terra's player walks at 48% of the
engineer's speed and its fields sit at 41% of Factorio's starting radius, so **every** leg comes out
at 0.86× Factorio's time — the ratio is the same whichever pair you measure, which is what keeps the
verdict from resting on a chosen leg. Three legs are tabled rather than one because #170's report is
about moving *between patches*, not out from the hub: Terra's four fields sit on the four cardinal
faces (iron east, copper north, coal west, stone south) at the size variant's distance, so the
traversal a player actually makes is a chord — up to 124 blocks — and not the 62-block radius.
So: **no base speed is set, and `DISTANCES` does not move.** A flat global buff would also have spent
Block Runner's concrete bonus ([Terrain modification](#terrain-modification)), which is `adapted`
precisely so that a built surface is the thing that makes you faster.

**The one soft number is Factorio's side.** `starting_resource_placement_radius` is the bound a
starting patch may be placed within, not where patches typically land. If Factorio's own starting
patches cluster well inside 150, the 0.86 flatters Terra and this row is worth reopening against
measured patch positions rather than the bound.

The playtest report that opened #207 stands as a report — the opening *feels* long, and a measured
patch-to-patch leg is about 100 blocks, some 23 s walked — but the arithmetic says the cause is not
distance or speed relative to Factorio. Factorio lets you zoom out and see all three
patches at once; Minecraft does not. That is legibility, and its surfaces are #116 (radar and surface
indicators) and #158 (pole supply-area overlay), not movement.

Sprinting is not counted above. It burns hunger, and #183 has not decided whether Minecraft's hunger
mechanic stays in the pack at all; a budget that assumed sprinting would be load-bearing on an
undecided mechanic.

### Personal transport

- **verdict**: `blocked`
- **where**: —
- **owner**: `unargued`

`logistics/transport` is `undecided` on one recipe, `car`. Personal transport is not an ADR-0017
capability and no rung grants it. Factorio's car, tank and spidertron have no pack answer, and
Minecraft's own movement options (elytra, horses, boats) are neither gated nor factory-produced.
Follow-on: #121.

### Terrain modification

- **verdict**: `adapted`
- **where**: all bodies
- **via**: Block Runner (walking speed), vanilla concrete (the block)
- **owner**: `by-consequence` for cliffs, ADR-0019 for landfill, #87 for concrete

Sub-rules:

- **Landfill** — `excluded`. ADR-0019 makes Terra flat and sea-bearing; landfill has no meaning there.
  This one *is* argued.
- **Cliffs and cliff explosives** — `excluded`. `by-consequence`: no body generates cliffs as an
  obstacle, so nothing needs removing.
- **Concrete and its speed bonus** — `adapted`. #87 maps the four concretes onto vanilla's own
  coloured concrete (grey, yellow for hazard, light grey and orange for the refined pair) and routes
  their recipes to the Assembling Machine like every other `crafting-with-fluid` craft. **The
  walking-speed bonus does have an analogue**: Block Runner gives a block a configurable
  walk/run speed, which is exactly what Factorio's concrete is for. The earlier `excluded` verdict
  was written before that mod was in the pack and is superseded rather than reversed on argument.

### Repair and entity damage

- **verdict**: `blocked`
- **where**: —
- **owner**: #118

`production/tool` is `undecided` on one recipe, `repair-pack`, and the reason this row carried —
"nothing on Terra takes damage the way a Factorio entity does; with no biters attacking buildings,
the whole repair loop has nothing to repair" — **was falsified by ADR-0055**. Enemies now damage
blocks in the `planetaryfactory:destructible` tag, so there is something to repair.

`blocked` rather than `planned`: the premise is gone but the argument has not been had. It is also
load-bearing in the other direction — ADR-0055 bounded destruction to a tag partly because there is
no repair mechanic underneath it, so a repair loop and the size of that tag are one question, not
two.

### Radar and map exploration

- **verdict**: `planned`
- **where**: Terra
- **via**: `pack`, `powergrid`
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

### Controls

- **verdict**: `planned`
- **where**: all bodies
- **via**: `planetaryfactory_core` (quick transfer), Mouse Tweaks (in-GUI), `native_mechanic` (pipette)
- **owner**: #208
- **ticket**: #208

Factorio's control surface is a mechanic in its own right: the gestures are how the player moves
items without a screen, and a pack that reproduces the production chains while making every transfer
a two-step GUI operation has reproduced the arithmetic and not the game. The rows below are the whole
surface, not only the one #208 asked for — a section admitting a single gesture would be re-argued
the next time one came up.

Sub-rules:

- **Fast entity transfer and fast entity split** — `planned`, #208. The pack calls these **quick
  transfer** and **quick split** (`CONTEXT.md`); Factorio's own names appear here and nowhere else,
  per ADR-0028. Two `KeyMapping`s in `planetaryfactory_core`, defaulting to `CTRL` + left and right
  mouse and declared in Controls so a conflict with Carry On or Building Gadgets is the player's to
  resolve. Magnitude is Factorio's verbatim — the held stack in, everything the target will give up
  out, halved for the split. The target set is every GregTech machine and every pack-authored block
  that holds items, reached through the block's own item handler with no pack-authored slot policy.
  **The mid-recipe question answers itself on both engines**: GregTech consumes inputs at
  `RecipeLogic.setupRecipe`, so a running machine has nothing to take back, and the pack's furnace —
  which consumes at completion instead — already refuses extraction from anything but its output slot
  in `FurnaceItemHandler`. Delegating to the handler is what makes the two timings invisible.
- **In-GUI stack and inventory transfer** (`SHIFT`/`CTRL` + click inside a machine screen) —
  `shipped` via Mouse Tweaks, which is in the manifest and does exactly this. Distinct from the fast
  entity gestures above: those need no screen open.
- **Pipette tool** — `adapted`. Vanilla's pick-block is a near-exact match, already bound, and picks
  from the inventory in survival. No work.
- **Drop item into a machine** (`Z`) — `excluded`. It is a one-item quick transfer, and shipping both
  means two bindings differing only in magnitude.
- **Drag-building** — `excluded`. `by-consequence`: Create's belts are placed endpoint-to-endpoint
  rather than one tile at a time (ADR-0044), so the gesture has nothing to drag across.

---

## Space Age

### Interplanetary travel

- **verdict**: `planned`
- **where**: pack-wide
- **via**: `gcyr`
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
- **via**: `gcyr` (space stations)
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
- **Sulfuric acid geysers, and acid neutralisation to water** — `planned`. Its steam output is the
  second consumer of ADR-0033's **Steam Turbine**, which is why that row is not Terra-only.
- **Demolishers as territorial obstacles** — `blocked`. No enemy model outside Terra, and this is the
  one place Space Age puts a boss between you and a resource.

### Fulgora: scrap and lightning

- **verdict**: `planned`
- **where**: Electro
- **via**: `create`, `powergrid`
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

- **Crops are farmed and replanted, not mined** — `planned`. Yumako and jellystem are Factorio
  `plant` prototypes, not `tree`s: `growth_ticks 18000`, grown from a seed, and one harvest yields
  **50 fruit and zero wood**, consuming the plant. They are deliberately **out of ADR-0051's felling
  rule** — wood is terminal, while these are the first link of the agricultural science loop
  (`yumako-mash ×15 + jelly ×12 → bioflux ×4`; `bioflux + pentapod-egg → agricultural-science-pack`;
  `yumako-mash ×4 → nutrients ×6`, feeding the towers that produce the input). The real mechanic is
  the Agricultural Tower, so building the harvest gesture alone would be a different mechanic wearing
  the same blocks. Until #23, Sapros's trees stay log-by-log and `yumako_leaves` keeps rolling fruit
  and sapling.
- **Seeds come from processing the fruit, never from harvesting** — `planned`. `yumako-processing`
  and `jellynut-processing` return a seed at `independent_probability 0.02` alongside mash or jelly.
  Both are category-less (so hand-craftable in Factorio) but `enabled: false`, and **neither is in
  `data/factorio/recipe.json` nor `data/pack/item-map.json` today** — Space Age pruning dropped them.
  The 2 % output is also the pack's first probabilistic recipe result, which ADR-0038's queue
  contract does not currently admit: a step that can deliver nothing is not a step
  `PlanToQueue` can guarantee runs to the end. #23 owns both problems.
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
- **candidates**: MekanismGenerators
- **owner**: `docs/planets.md`
- **ticket**: #15, then Gelida's `Puzzle:` ticket

Fusion generator and reactor, craftable only on Gelida. **This row has no `via`, and cannot have
  one**: it named MekanismGenerators, which the pack has never installed, and ADR-0035 has since
  taken base Mekanism out of the manifest too — so the jar is now two manifest decisions away, not
  one. Adopting it is a decision Gelida's puzzle must argue, not inherit.

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
  **Answered.** ADR-0054 and ADR-0055: nests absorb emission and send the waves, all seven `combat/*`
  shelves come back, and Military science returns with them.
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
