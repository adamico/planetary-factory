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
// chassis is `planetaryfactory_core`'s `AssemblingMachine`, which is `SimpleTieredMachine` with
// that predicate flipped and that one slot dropped. It overrides no recipe logic.
const AssemblingMachine = Java.loadClass('com.planetaryfactory.core.machine.AssemblingMachine');

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
    .machine((holder, tier, tankScaling) => new AssemblingMachine(holder, tier, tankScaling))
    .definition((tier, builder) => {
      builder.rotationState(RotationState.NON_Y_AXIS)
        .paintingColor(ASSEMBLING_TINT[tier])
        .recipeType(ASSEMBLING)
        .recipeModifier(GTRecipeModifiers.OC_NON_PERFECT)
        // Set here, not left to KubeJS: the tiered builder only falls back to GregTech's stock
        // UI -- the one carrying the charger slot -- when the definition function leaves this null.
        .editableUI(AssemblingMachine.editableUI(GTCEu.id('assembling_machine'), ASSEMBLING))
        .workableTieredHullModel(GTCEu.id('block/machines/assembler'));
    })
    .addDefaultTooltips(false);
});
