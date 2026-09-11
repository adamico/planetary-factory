// priority: 0
// requires: researchd
// The pack's research tree. Topology, names and pack costs come from Factorio's extracted
// technology tree (data/factorio/technology.json); this file supplies only what Factorio
// cannot know -- which Minecraft item is the icon, which recipe a research unlocks, and
// which of our bodies gates it. See ADR-0022 and factorio_tech_dsl.js.
//
// A technology absent from this file is absent from the pack, and the DSL logs the ones
// still undeclared on every reload.
//
// The `priority: 0` header above is load order: this file must load AFTER
// factorio_tech_data.js (20) and factorio_tech_dsl.js (10), because fromFactorio() has to
// exist before these calls run. KubeJS does not load scripts alphabetically.

ResearchdEvents.registerResearchPacks(event => {
  event.create('planetary_factory:automation_science_pack')
    .literalName('Automation Science Pack')
    .color(200, 60, 60)
    .sortingValue(100);
  event.create('planetary_factory:logistic_science_pack')
    .literalName('Logistic Science Pack')
    .color(60, 200, 60)
    .sortingValue(101);
  event.create('planetary_factory:chemical_science_pack')
    .literalName('Chemical Science Pack')
    .color(60, 60, 200)
    .sortingValue(102);

  // when using the packs in recipes
  // a data component is needed:
  // Item.of('researchd:research_pack[researchd:research_pack="planetary_factory:automation_science_pack"]')
});

// Steel axe, the pack's first declared technology and ADR-0039's second tier.
//
// Factorio's row is carried exactly where it can be: it is one of the 33 trigger technologies, so
// it costs no science packs at all, and its prerequisite `steel-processing` is still undeclared --
// the DSL resolves through it rather than orphaning this one.
//
// TWO DELIBERATE DIVERGENCES, both ADR-0039's:
//
//   - The trigger. Factorio fires this on CRAFTING 50 steel plates, and Researchd has no
//     craft-triggered method -- its four are consumeItem, consumePack, checkItemPresence and the
//     combinators. `has` is checkItemPresence, which holds the plates rather than eating them, and
//     that is the closer of the two: Factorio's trigger charges nothing. A real craft trigger is
//     mechanism, belongs in `planetaryfactory_core` under ADR-0015, and would serve all seven of
//     #138's trigger technologies rather than this one alone.
//
//   - The effect. Factorio's is `character-mining-speed +1`; here it unlocks the Engineer's Steel
//     Pick recipe and the speed rides on the item. The outcome is the same -- mining doubles, 2.0s
//     to 1.0s -- and this is the first time the pack overrides an extracted effect rather than
//     supplying one, which is why it is written down: a reader diffing this file against
//     `data/factorio/technology.json` would otherwise read it as a bug.
//
// The unlock id is `assembling/pack/`: the recipe is hand-authored (ADR-0031's exception) and lives
// in the one subtree no converter owns -- nested INSIDE `assembling/` because GregTech re-registers
// every loaded GTRecipe under its own type path (#87), so a flat `pack/` would put a second id in
// the recipe manager and this gate would name the wrong one of the two.
// `tests/factorio/test_research_unlocks.py` asserts this id is a recipe the pack emits, which is
// the coupling that makes the divergence safe.
fromFactorio('steel-axe', {
  icon: 'planetaryfactory:engineers_steel_pick',
  has: ['gtceu:steel_plate', 50],
  unlocks: ['planetaryfactory:assembling/pack/engineers_steel_pick']
});

// ---------------------------------------------------------------------------------------------
// THE CRITICAL PATH TO PLASTIC (#206, under #170)
//
// Twelve nodes -- eleven declared and one (`oil-processing`) declared absent, for the reason given
// at `oil-gathering` below. They are exactly the ancestor closure of Factorio's `plastics` in
// `data/factorio/technology.json`, computed from the file rather than chosen. Nothing off that path is
// declared here, and nothing on it is invented: every node below is `fromFactorio()`, so its cost,
// its time and its place in the tree are Factorio's own. #170 times the pace, and a hand-cut node
// with a made-up cost would make that reading a lie -- which is why the two places this file
// cannot carry Factorio's shape are written down rather than quietly patched.
//
// UNLOCKS ARE ONLY THE RECIPES THE PACK EMITS. Factorio's effects name recipes this pack does not
// have -- `boiler` and `steam-engine` (item-map `undecided`/`not_emitted`), `inserter` and
// `long-handed-inserter` (`undecided`, which is also why `logistic-science-pack` has no recipe of
// its own: its two ingredients are a belt and an inserter), `pipe-to-ground` (`not_emitted`),
// `pumpjack` (`undecided`, #105) and the twenty barrel fill/empty rows (`native_mechanic`, Create's
// Spout and Item Drain, never recipes). A lock on an id nothing emits unlocks nothing, silently, so
// those effects are dropped here and `tests/factorio/test_research_unlocks.py` holds the rest.
//
// CRAFT-VS-HOLD, the gap #138 will generalise. Factorio's `craft-item` is an EVENT and Researchd's
// `checkItemPresence` reads STATE, so the three craft triggers below fire on HOLDING what Factorio
// fires on CRAFTING. Two properties make that safe rather than merely close: the check latches
// monotonically -- progress does not regress once the items have been held together -- so spending
// the plates afterwards does not re-lock the research; and it charges nothing, which is Factorio's
// own behaviour for a trigger technology. What it does NOT catch is a player who reaches the count
// by looting or trading rather than crafting, and on Terra there is no such route at rung 0.
// `consumeItem` is not used: it is reserved for rung 0 (#42, ADR-0033) and it would destroy the
// plates a trigger technology is not supposed to charge.

// --- Rung 0: the two triggers that open the factory -------------------------------------------

// Factorio: craft 50 iron plate. Unlocks the steam chapter; here that is the pipe and the Offshore
// Pump, the one block water enters the factory through (ADR-0050, #213). `boiler`, `steam-engine`
// and `pipe-to-ground` have no pack recipe, so this node grants two of its five.
//
// The held item is Create's sheet, for the same reason as `electronics` below: AlmostUnified unifies
// `c:plates/iron` and `create` outranks GregTech, so the furnace delivers `create:iron_sheet` however
// `recipe/iron_plate.json` reads. Note that `StartingKit.java` grants `gtceu:iron_plate` x8 directly,
// which no unification touches -- those eight do NOT count toward this fifty, and the same goes for
// the kit's eight copper. #220.
fromFactorio('steam-power', {
  icon: 'planetaryfactory:offshore_pump',
  has: ['create:iron_sheet', 50],
  unlocks: [
    'planetaryfactory:assembling/pipe',
    'planetaryfactory:assembling/offshore_pump'
  ]
});

// Factorio: craft 10 copper plate. The electronics chapter, and the node that grants the Lab --
// without which `automation-science-pack` below can never fire.
//
// THE HELD ITEM IS CREATE'S SHEET, NOT THE PLATE THIS RECIPE'S JSON NAMES. `recipe/copper_plate.json`
// results in `gtceu:copper_plate` on disk, but AlmostUnified unifies `c:plates/{material}` and its
// `mod_priorities` put `create` above GregTech, so the furnace delivers `create:copper_sheet` -- 
// confirmed in a running game, and it is what EMI shows for `planetaryfactory:copper_plate`.
// `checkItemPresence` resolves a literal id through `BuiltInRegistries.ITEM` and holds it as a
// one-item `Ingredient`, so it matches the sheet or the plate but never both; naming the plate here
// is a gate the furnace cannot fill. The pack ought to carry ONE plate item per material rather than
// two that only a unification config keeps apart -- that is #220, and this id follows it when it lands.
//
// CHANGING THIS ID NEEDS A RESTART, NOT `/reload`. Researchd's registry is a
// `SimpleJsonResourceReloadListener` that re-fires the KubeJS event, and on a `/reload` it can read
// the PREVIOUS script evaluation -- the old id, with no warning. The queued `ResearchProgress$Task`
// then holds the ingredient captured when it was queued, so a dequeue/requeue is needed on top. An
// id tested without both looks exactly like a wrong id: this one was reverted once on that evidence.
fromFactorio('electronics', {
  icon: 'planetaryfactory:electronic_circuit',
  has: ['create:copper_sheet', 10],
  unlocks: [
    'planetaryfactory:assembling/copper_cable',
    'planetaryfactory:assembling/electronic_circuit',
    'planetaryfactory:assembling/lab',
    'planetaryfactory:assembling/small_electric_pole'
  ]
});

// --- Rung 1: science begins --------------------------------------------------------------------

// Factorio: craft a Lab. Researchd's Lab is `researchd:research_lab` (item-map, borrowed), so the
// hold is the Lab itself. This is the node that makes every pack-costed research below reachable.
fromFactorio('automation-science-pack', {
  iconPack: 'planetary_factory:automation_science_pack',
  has: ['researchd:research_lab', 1],
  unlocks: ['planetaryfactory:assembling/automation_science_pack']
});

// 50 automation packs. Steel is Terra's only surviving alloy (#72) and its recipe rides the
// count-bearing `planetaryfactory:smelting` type, whose ids are flat rather than under
// `assembling/` -- GregTech does not re-register it, so it is not cloned (#87, FLAT_TYPES).
fromFactorio('steel-processing', {
  icon: 'gtceu:steel_plate',
  unlocks: [
    'planetaryfactory:steel_plate',
    'planetaryfactory:assembling/steel_chest'
  ]
});

// 10 automation packs. `long-handed-inserter` is `undecided`, so this grants the machine only.
fromFactorio('automation', {
  icon: 'gtceu:lv_assembling_machine',
  unlocks: ['planetaryfactory:assembling/assembling_machine_1']
});

// 75 automation packs. THE ONE DECLARED NODE THAT UNLOCKS NOTHING, and deliberately: Factorio's
// logistic science pack is a belt plus an inserter, `inserter` is an `undecided` item-map row, and
// the converter therefore emits no recipe for the pack itself. The NODE still has to exist -- it is
// a prerequisite of `automation-2` and a pack cost of everything from `engine` down, so dropping it
// would take Factorio's rung-2 pacing out of the very run #170 is timing. Its recipe arrives with
// the inserter row; the research does not have to wait for it.
fromFactorio('logistic-science-pack', {
  iconPack: 'planetary_factory:logistic_science_pack',
  unlocks: []
});

// --- Rung 2: fluids, then oil -------------------------------------------------------------------

// 40 automation + logistic.
fromFactorio('automation-2', {
  icon: 'gtceu:mv_assembling_machine',
  unlocks: ['planetaryfactory:assembling/assembling_machine_2']
});

// 100 automation + logistic.
fromFactorio('engine', {
  icon: 'planetaryfactory:engine_unit',
  unlocks: ['planetaryfactory:assembling/engine_unit']
});

// 50 automation + logistic. Three of the twenty-three effects: the barrel ITEM is emitted, while the
// eighteen fill/empty rows are Create's Spout and Item Drain keying on `IFluidHandlerItem` and were
// never recipes at all (`native_mechanic`, ADR-0034 exception class 1).
fromFactorio('fluid-handling', {
  icon: 'create:fluid_tank',
  unlocks: [
    'planetaryfactory:assembling/storage_tank',
    'planetaryfactory:assembling/pump',
    'planetaryfactory:assembling/barrel'
  ]
});

// 100 automation + logistic, 30s -- Factorio's own gate on reaching crude oil, and here it carries
// the whole oil chapter.
//
// DIVERGENCE, the second of this file's two, and the larger. Factorio splits the chapter across two
// nodes: `oil-gathering` (100 packs) grants the pumpjack, and `oil-processing` -- a TRIGGER
// technology firing on MINING crude oil, costing nothing -- grants the Oil Refinery, the Chemical
// Plant, basic oil processing and solid fuel. Neither half survives translation intact:
//
//   - `pumpjack` is an `undecided` item-map row (#105). Terra's crude is a GregTech bedrock fluid
//     deposit tapped by a Fluid Drilling Rig (ADR-0017, docs/factorio-mechanics.md), not a patch you
//     sit a pumpjack on, so this node's own effect has no recipe to grant.
//   - `oil-processing`'s trigger cannot be expressed AT ALL. Researchd's four methods all read
//     items, and crude oil is a fluid with no item form here -- `crude-oil-barrel` is
//     `native_mechanic`, a Spout fill rather than a craft. A node whose method no player can meet is
//     a dead gate, and a dead gate on the Chemical Plant ends the run at rung 2.
//
// So `oil-processing` is declared `skip` below and its four unlocks are granted HERE, by its only
// parent. The pack cost of reaching plastic is unchanged to the pack: `oil-processing` costs nothing
// in Factorio either, so the sum across the pair is the same 100 -- which is what keeps #170's
// reading honest. When the oil extractor lands, `oil-processing` becomes declarable on its own terms
// and these four move back down; nothing here has to be redone to allow that.
fromFactorio('oil-gathering', {
  icon: 'kubejs:oil_refinery',
  unlocks: [
    'planetaryfactory:assembling/oil_refinery',
    'planetaryfactory:assembling/chemical_plant',
    'planetaryfactory:oil_refinery/basic_oil_processing',
    'planetaryfactory:chemical_plant/solid_fuel_from_petroleum_gas'
  ]
});

// Declared absent, with its reason immediately above. `skip` is visible rather than silent: the DSL
// resolves parents THROUGH a skipped node, so `plastics` below parents onto `oil-gathering` and the
// tree stays connected.
fromFactorio('oil-processing', { skip: true });

// 200 automation + logistic, 30s. The end of the path: rung 2's gate (ADR-0025) and what this
// twelve-node slice exists to reach.
fromFactorio('plastics', {
  icon: 'planetaryfactory:plastic_bar',
  unlocks: ['planetaryfactory:chemical_plant/plastic_bar']
});
