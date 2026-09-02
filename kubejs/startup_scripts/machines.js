// Terra's pack-authored GregTech machines: the Assembling Machines (ADR-0026), and the Oil
// Refinery and Chemical Plant of the oil chapter (ADR-0025).
//
// ADR-0017 gave Assembly to GregTech and ADR-0018 refined it to "GT's LV/MV/HV Assemblers,
// renamed". ADR-0026 took that back: the machines are registered here, on a GregTech
// chassis, against a recipe type this pack owns. Binding them to `gtceu:assembler` would
// hand GregTech the capability back silently, by recipe placement, whatever ADR-0017's
// table says -- and would inherit GT's whole stock assembler corpus, circuits included.
//
// Startup scripts share one JavaScript scope (see materials.js), so every name declared at
// top level here is global to every other startup script. Nothing is declared at top level.

// The envelope is read off Factorio's own recipes, not chosen:
// `scripts/factorio-recipe-extract.py` reports the widest recipe in each routed category,
// and `tests/factorio/test_recipe_extract.py` pins the numbers so a regeneration cannot
// widen a registered GUI in silence. Five item inputs is `rocket-control-unit` and
// friends; one fluid in and one out is every `crafting-with-fluid` recipe in the corpus.
const ASSEMBLING_IO = [5, 1, 1, 1];

// Registered through GregTech's own API rather than a KubeJS registry event, because the
// KubeJS `minecraft:recipe_type` event fires AFTER `gtceu:machine` -- measured, not assumed:
// the machine definitions ran at .763 and the recipe-type event at 1.458 of the same second,
// and every machine got GregTech's "Tried to set null recipe type" error. A machine cannot
// name a recipe type that does not exist yet, so the type is created at script-evaluation
// time, which precedes every registry event.
const ASSEMBLING = GTRecipeTypes.register('assembling', GTRecipeTypes.ELECTRIC)
  .setMaxIOSize(ASSEMBLING_IO[0], ASSEMBLING_IO[1], ASSEMBLING_IO[2], ASSEMBLING_IO[3])
  .setEUIO('in')
  // No slot overlay. GregTech's own assembler sets the item-input slots to
  // INT_CIRCUIT_OVERLAY, because a stock GT assembler recipe is selected by a programmed
  // circuit; copying that call printed a circuit behind all five input slots of a machine
  // whose corpus contains no circuit at all. `setSlotOverlay(isOutput, isFluid, texture)`
  // paints every slot of that kind, so the honest overlay here is none.
  .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, FillDirection.LEFT_TO_RIGHT)
  .setSound(GTSoundEntries.ASSEMBLER);

// Three machines, one recipe type, LV/MV/HV.
//
// THE IDS ARE `gtceu:`, NOT `planetaryfactory:`, and the tier is a prefix rather than a
// numeral: `gtceu:lv_assembling_machine`, `_mv_`, `_hv_`. Both halves of ADR-0026's naming
// table are unreachable from here -- KJSTieredMachineBuilder registers through GregTech's
// own registrate, which owns the namespace, and prefixes each tier's short name; the same
// is true of `GTRecipeTypes.register`, which is why the type above is `gtceu:assembling`.
// The name the ADR actually argued about is the display name, and that is authored: see
// `kubejs/assets/gtceu/lang/en_us.json`. Pack-namespaced ids would mean registering from
// `planetaryfactory_core` with a registrate of its own, which is a bigger change than the
// ADR's argument asks for.
//
// ADR-0018 makes the tiers speed-only and says they gate nothing, so there is no per-tier
// fluid restriction -- Factorio's assembling-machine-1-cannot-craft-fluids rule is exactly
// the accidental gate that line exists to prevent. No recipe carries an EUt at all: ADR-0029
// puts energy and speed on the machine, so a recipe cannot encode a tier gate even by accident.
// Two parts of a stock GT machine's GUI are welded into `SimpleTieredMachine` rather than read
// off the recipe type, so no builder call reaches them: the programmed-circuit configurator
// (`isCircuitSlotEnabled()` returns a hardcoded `true`) and the charger slot (built into that
// class's own `EDITABLE_UI_CREATOR`). Neither belongs on an assembling machine here -- there is
// no circuit anywhere in the corpus, and machines are wired rather than battery-fed -- so the
// chassis is `planetaryfactory_core`'s `SimpleMachine`, which is `SimpleTieredMachine` with
// that predicate flipped and that one slot dropped. It overrides no recipe logic.
const SimpleMachine = Java.loadClass('com.planetaryfactory.core.machine.SimpleMachine');

// The tint half of ADR-0026's "speed and tint, nothing else" -- Factorio's three assembling
// machines are told apart at a glance by body colour, and the tier ladder teaches nothing if
// three blocks on a GregTech hull look like three GregTech hulls. Keyed by tier rather than by
// position in `tiers()` so the mapping stays readable if the ladder ever moves.
//
// GregTech tints the hull, not the machine face: `paintingColor` is the machine's *default*
// painting colour, the same value a spray can would overwrite, and the recipe-face overlay is a
// separate untinted layer. So this colours the body and leaves the assembler face legible --
// which is the Factorio read, where the coloured part is the chassis.
const ASSEMBLING_TINT = {};
ASSEMBLING_TINT[GTValues.LV] = 0xc8813c; // 1: brown
ASSEMBLING_TINT[GTValues.MV] = 0x3c7fc8; // 2: blue
ASSEMBLING_TINT[GTValues.HV] = 0x4ca64c; // 3: green

StartupEvents.registry('gtceu:machine', (event) => {
  // The name is passed unqualified: a namespace here is discarded, as the note above says.
  event.create('assembling_machine')
    .tiers(GTValues.LV, GTValues.MV, GTValues.HV)
    .machine((holder, tier, tankScaling) => new SimpleMachine(holder, tier, tankScaling))
    .definition((tier, builder) => {
      builder.rotationState(RotationState.NON_Y_AXIS)
        .paintingColor(ASSEMBLING_TINT[tier])
        .recipeType(ASSEMBLING)
        .recipeModifier(GTRecipeModifiers.OC_NON_PERFECT)
        // Set here, not left to KubeJS: the tiered builder only falls back to GregTech's stock
        // UI -- the one carrying the charger slot -- when the definition function leaves this null.
        .editableUI(SimpleMachine.editableUI(GTCEu.id('assembling_machine'), ASSEMBLING))
        .workableTieredHullModel(GTCEu.id('block/machines/assembler'));
    })
    .addDefaultTooltips(false);
});

// ---------------------------------------------------------------------------------------------
// Terra's oil chapter: the Oil Refinery and the Chemical Plant (ADR-0025).
//
// ADR-0017 gave Refining and Chemistry to Mekanism and ADR-0025 took both back, on one fact:
// advanced oil processing is two fluids in and three out, and NO installed mod has a machine with
// that shape. GregTech's own Distillation Tower takes a single fluid input at every tier, so crude
// plus water cannot enter it at all. These two are registered here for the same reason the
// Assembling Machines above are -- there is nowhere else to put the recipes.
//
// THE IO SIZES ARE THE CORPUS'S, NOT ADR-0025'S ORIGINAL TABLE. That table was read off the 1.1
// wiki and gave the refinery `(2, 0, 2, 3)` for coal liquefaction's coal and calcite; there is no
// liquefaction recipe in the committed corpus, because 2.x puts `coal-liquefaction` behind
// metallurgic and space science and ADR-0022's four-rung filter drops it. The refinery therefore
// crafts two recipes, basic and advanced oil processing, and its item slots would be slots nothing
// can fill. Same for the Chemical Plant's second fluid output: two output fluid boxes is the
// Factorio *entity*, while the widest chemistry *recipe* emits one. `tests/factorio/
// test_recipe_extract.py` pins both numbers against the corpus and `tests/pack/
// test_machine_assets.py` asserts the constants below still equal them -- that second check is
// what makes the numbers here binding, since the first one never opens this file. ADR-0025
// carries the amendment.
const OIL_REFINERY_IO = [0, 0, 2, 3];
const CHEMICAL_PLANT_IO = [2, 1, 2, 1];

// Created at script-evaluation time for the reason ASSEMBLING is: the `minecraft:recipe_type`
// event fires after `gtceu:machine`, and a machine naming a type that does not exist yet gets
// GregTech's "Tried to set null recipe type". The ids are `gtceu:` because GregTech's registrate
// owns the namespace; the authored names are the display names, in the pack's GregTech lang file.
const OIL_REFINERY = GTRecipeTypes.register('oil_refinery', GTRecipeTypes.ELECTRIC)
  .setMaxIOSize(OIL_REFINERY_IO[0], OIL_REFINERY_IO[1], OIL_REFINERY_IO[2], OIL_REFINERY_IO[3])
  .setEUIO('in')
  .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, FillDirection.LEFT_TO_RIGHT)
  .setSound(GTSoundEntries.CHEMICAL);

const CHEMICAL_PLANT = GTRecipeTypes.register('chemical_plant', GTRecipeTypes.ELECTRIC)
  .setMaxIOSize(CHEMICAL_PLANT_IO[0], CHEMICAL_PLANT_IO[1],
    CHEMICAL_PLANT_IO[2], CHEMICAL_PLANT_IO[3])
  .setEUIO('in')
  // No slot overlay, for the reason the assembling type has none: `setSlotOverlay` paints every
  // slot of a kind, and the honest overlay for a corpus with no circuit in it is none.
  .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, FillDirection.LEFT_TO_RIGHT)
  .setSound(GTSoundEntries.CHEMICAL);

// Neither machine has a voltage ladder. ADR-0025: one tier, one recipe set, no per-tier
// rebalancing -- ADR-0017's four-facts doctrine is already spent, and a ladder here would spend it
// again for a speed multiplier. The Chemical Plant is still declared through the *tiered* builder,
// with a single tier: it is the only builder that takes a machine constructor, and LV alone makes
// a ladder of one rather than a ladder. ADR-0029 puts EUt on the machine, so no recipe encodes a
// tier gate even by accident.
StartupEvents.registry('gtceu:machine', (event) => {
  // The Chemical Plant is a single block because Factorio's is: the player spams it, and a
  // multiblock tax on the high-count block is exactly where the tax bites (ADR-0025).
  event.create('chemical_plant')
    .tiers(GTValues.LV)
    .machine((holder, tier, tankScaling) => new SimpleMachine(holder, tier, tankScaling))
    .definition((tier, builder) => {
      builder.rotationState(RotationState.NON_Y_AXIS)
        .recipeType(CHEMICAL_PLANT)
        .recipeModifier(GTRecipeModifiers.OC_NON_PERFECT)
        .editableUI(SimpleMachine.editableUI(GTCEu.id('chemical_plant'), CHEMICAL_PLANT))
        .workableTieredHullModel(GTCEu.id('block/machines/chemical_reactor'));
    })
    .addDefaultTooltips(false);

  // The Oil Refinery is a multiblock because Factorio's refinery is a landmark the player builds a
  // few of and plans around (ADR-0025). Building Gadgets is in the pack for structure copy-paste,
  // which is what makes the shape bearable.
  //
  // `multiblock` is a registered KubeJS builder type on GregTech's machine registry -- checked in
  // the 7.0.2 jar rather than assumed, alongside `tiered_multiblock`, `steam`, `generator`,
  // `primitive` and `custom`. It is a DIFFERENT builder from the tiered one above and shares
  // almost none of its calls: no `.tiers()`, no `.machine()` taking a tier, and the IO is carried
  // by hatch parts rather than by slots on the block, which is why `SimpleMachine`'s two GUI
  // removals have no analogue here -- a multiblock controller never had the circuit configurator
  // or the charger slot to begin with.
  //
  // `enableMaintenance` is false in `config/gtceu.yaml`, so no Maintenance Hatch appears in the
  // pattern and none has to be craftable. ADR-0026 already recorded that maintenance never appears
  // in this pack; this is the first multiblock that would otherwise have asked for it.
  //
  // THE BLOCK ID IS `kubejs:oil_refinery`, NOT `gtceu:`. This is the one place the multiblock path
  // diverges from #73's rule that GregTech's registrate owns the namespace: the tiered builder goes
  // through GT's registrate and the multiblock wrapper goes through KubeJS's own, so the two
  // machines registered in this file land in different namespaces. Read out of a running game, not
  // assumed -- the recipe TYPES are `gtceu:` for both, because `GTRecipeTypes.register` is the same
  // call either way. Two things follow, and both are the pack's to carry:
  //
  //   - the display name is `block.kubejs.oil_refinery`, so it lives in `assets/kubejs/lang`
  //     rather than in the GregTech lang file the other machines use; and
  //   - GregTech's runtime model provider does not generate a blockstate or model for the id, so
  //     the refinery ships authored ones under `assets/kubejs/`. Without them the block renders
  //     untextured and the log carries only a model warning, which no check reads. The authored
  //     blockstate is why `allowExtendedFacing` is off: extended facing adds an `upwards_facing`
  //     property, which quadruples the variants a hand-written blockstate has to cover for a
  //     machine that cannot usefully be built on its side anyway.
  //
  // The authored model is static, so the refinery does not light up while it runs -- a GregTech
  // machine's active overlay comes from the model provider that is not generating here. The
  // block has no `active` blockstate property to cover, so nothing is missing; what is lost is
  // the animation. `tests/pack/test_machine_assets.py` walks blockstate to model to texture so
  // that a broken hop fails there rather than rendering untextured behind a client-side warning.
  event.create('oil_refinery', 'multiblock')
    .rotationState(RotationState.NON_Y_AXIS)
    .allowExtendedFacing(false)
    .recipeType(OIL_REFINERY)
    .recipeModifier(GTRecipeModifiers.OC_NON_PERFECT)
    .appearanceBlock(() => GTBlocks.CASING_STEEL_SOLID.get())
    // 3x3x3 of steel casing with a hollow centre, controller in the middle of the front face. The
    // hatches are the walls: any casing block may be an energy, fluid-input or fluid-output part,
    // which is GregTech's own idiom and keeps the shape one the player can actually plumb three
    // output fluids into.
    .pattern((definition) => FactoryBlockPattern.start()
      .aisle('XXX', 'XXX', 'XXX')
      .aisle('XXX', 'X X', 'XXX')
      .aisle('XXX', 'XSX', 'XXX')
      .where('S', Predicates.controller(Predicates.blocks(definition.get())))
      .where('X', Predicates.blocks(GTBlocks.CASING_STEEL_SOLID.get())
        .setMinGlobalLimited(20)
        .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMinGlobalLimited(1))
        .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS).setMinGlobalLimited(1))
        .or(Predicates.abilities(PartAbility.INPUT_ENERGY)
          .setMinGlobalLimited(1).setMaxGlobalLimited(2)))
      .where(' ', Predicates.any())
      .build())
    // No `.addDefaultTooltips(false)` here: that call is on the TIERED builder and the multiblock
    // wrapper does not have it, so writing it would be a Rhino error at registration rather than a
    // no-op. The tiered machines above still carry it.
    .workableCasingModel(
      GTCEu.id('block/casings/solid/machine_casing_solid_steel'),
      GTCEu.id('block/multiblock/distillation_tower'));
});
