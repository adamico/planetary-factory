// Terra's Assembly row: the pack's own Assembling Machines (ADR-0026).
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
  .setSlotOverlay(false, false, GuiTextures.INT_CIRCUIT_OVERLAY)
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
// the accidental gate that line exists to prevent -- and every recipe is authored at LV EUt.
StartupEvents.registry('gtceu:machine', (event) => {
  // The name is passed unqualified: a namespace here is discarded, as the note above says.
  event.create('assembling_machine')
    .tiers(GTValues.LV, GTValues.MV, GTValues.HV)
    .definition((tier, builder) => {
      builder.rotationState(RotationState.NON_Y_AXIS)
        .recipeType(ASSEMBLING)
        .recipeModifier(GTRecipeModifiers.OC_NON_PERFECT)
        .workableTieredHullModel(GTCEu.id('block/machines/assembler'));
    })
    .addDefaultTooltips(false);
});
