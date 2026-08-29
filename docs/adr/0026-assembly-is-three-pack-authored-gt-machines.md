---
status: accepted
---

# Assembly is three pack-authored GT machines, and recipes stop going through the KubeJS builder

ADR-0017 gave **Assembly** to GregTech — "Assembling Machine I/II/III" with no losing block, the one
row where the winner was a stock GT block used as shipped. ADR-0018 refined it: those three are
"GT's LV/MV/HV Assemblers, renamed", granted by a rung, speed-only, gating nothing.

Building against that turned up two costs the rows did not price.

**Authoring.** KubeJS's `GTRecipeBuilder` does not round-trip. It discards the namespace passed to
`.id(...)` and substitutes the recipe type's own, and the JSON it emits decodes to an NPE in
`GTRecipe`'s codec, taking the whole datapack load down. Neither is a property of the recipe
*format* — `kubejs/data/gtceu/gtceu/ore_vein/*.json` are twenty-two hand-authored GT data files that
have never had either problem.

**Interaction.** A stock GT Assembler carries a programmed-circuit slot, a voltage ladder, cover
buttons and several hundred stock recipes the pack does not want. To a Factorio-literate player it
reads as GregTech, which ADR-0018 says the pack explicitly is not.

## The rule

**Terra's Assembly row is three machines this pack registers itself, on a GregTech chassis, against
a recipe type this pack registers. Their recipes are JSON generated from Factorio's own prototypes
and committed to the repo. The KubeJS `GTRecipeBuilder` is not used.**

## The machines

| | |
| --- | --- |
| Ids | `planetaryfactory:assembling_machine_1` / `_2` / `_3` |
| Display | "Assembling Machine 1", "Assembling Machine 2", "Assembling Machine 3" |
| Registration | `KJSTieredMachineBuilder.tiers([LV, MV, HV])` — one builder call, one recipe type |
| Recipe type | `planetaryfactory:assembling`, registered via `GTRecipeTypeBuilder` |
| Differences between tiers | **Speed and tint. Nothing else.** |

**The names are Factorio's on both layers, and this does not lean on ADR-0004.** That ADR governs
celestial bodies and says so in its first line, forbidding its use as precedent. What carries here is
the *argument* it used for the science packs: a body is scenery the pack wants to own, a science pack
is a citation. An Assembling Machine is a citation. A Factorio player has to recognise the block on
sight, or the tier ladder teaches nothing.

**No voltage ladder in the ADR-0025 sense, and no fluid restriction either.** Factorio's assembling
machine 1 cannot craft fluid recipes; 2 and 3 can. That distinction is **dropped**, because ADR-0018
already forbids it in as many words — tiers are "granted by a rung, speed-only, and **gate nothing**"
— and a fluid restriction is exactly the accidental gate that line exists to prevent. Recovering the
beat is an amendment to ADR-0018, argued there.

**ADR-0025's "no voltage ladder" rule does not reach this row.** That rule was argued about machines
Factorio does not tier: there is one refinery and one chemical plant. Factorio tiers the assembler,
three of them, each behind research. `tiers(int[])` costs one array.

## Why not GregTech's own recipe type

Binding the pack's machines to `gtceu:assembler` was considered and declined on ADR-0025's own
reasoning for declining the Large Chemical Reactor: pointing the row at GregTech's generic type hands
GregTech the capability back **silently, by recipe placement**, whatever the table says. It also
inherits GT's `setMaxIOSize` envelope and every stock GT assembler recipe, circuits included.

The cost of a new type is that **twelve GCyR recipes are stranded**.
`gcyr-src/…/data/recipe/MiscRecipes.java` calls `GTRecipeTypes.ASSEMBLER_RECIPES.recipeBuilder(...)`
twelve times — `basic`/`advanced`/`elite` fuel tank and rocket motor, `launch_pad`,
`space_station_package`, `space_fabric`, `casing_atomic`, `seat`, the space-suit smithing template.
That is compiled Java, not data: **every rocket in the pack is an Assembler recipe**. They are
re-authored as JSON on the pack's type. This is deferred work, not a reason to reuse the type — and
the stranding is loud rather than silent, which is the failure mode to prefer.

## The GUI is authored at recipe-type level only

Read out of the 7.0.2 bytecode, `GTRecipeTypeBuilder` exposes `setMaxIOSize`, `setSlotOverlay`,
`setProgressBar`, `setSound`, `setIconSupplier` and `setUiBuilder`; `KJSTieredMachineBuilder` exposes
`tiers`, `editableUI`, `addDefaultTooltips` and `addDefaultModel`.

The pack uses the first group and `addDefaultTooltips(false)`. It does **not** use `setUiBuilder` or
`editableUI` in this decision, and it does **not** write a Java `MetaMachine` subclass.

- **The circuit slot needs no work at all.** A programmed circuit is a recipe *ingredient*, not a
  machine feature. A recipe type fed only recipes this pack authors has no circuit anywhere in it.
- **Cover buttons survive as dead chrome.** Covers live on `MetaMachine`, so removing the buttons
  needs Java — but ADR-0017 already recipe-removed every GT cover with the power layer, so the button
  opens onto nothing craftable. Not worth a `MetaMachine` subclass, which would be unbounded work
  against GT internals: the exact churn this ADR exists to escape.
- **Maintenance never appears.** It is a multiblock hatch ability; these are singleblocks.

If the layout still reads wrong in game, `setUiBuilder` is a cheap follow-up with evidence behind it.

## Recipes are generated data, not builder calls

Two halves, in this order:

1. **Extraction.** Factorio's `recipe` prototypes come out of the same `--dump-data` file
   `scripts/factorio-tech-extract.py` already reads for `technology`. ADR-0022's rule — extract,
   never transcribe — applies unchanged.
2. **Conversion.** A converter reads that dump plus **committed data files** (item map, category map,
   ratio rule) and emits **committed** recipe JSON under `kubejs/data/`. Pack deviations live in an
   overrides file, mirroring `researchd.js`'s `fromFactorio(name, {…})`. Generated output is never
   hand-edited.

The mapping files are committed as *data* rather than buried in the converter because the item map is
a design document — it is where ADR-0021's "Terra's resources are Nauvis's resources" becomes
concrete, and it will be argued over for months. `factorio-tech-extract.py` keeps its pruning rules
readable for the same reason.

The extraction also settles `setMaxIOSize`, which is read off the data — the maximum ingredient count
across the crafting categories — the way ADR-0025 read `(2, 1, 2, 2)` off the wiki rather than
choosing it.

## Amended by #73: the ids are GregTech's, and the recipe type is created before the machines

Building the row turned up two facts about the API this ADR names, both measured in game rather
than reasoned about.

**The ids cannot be `planetaryfactory:`.** `KJSTieredMachineBuilder` registers through GregTech's
own registrate, which owns the namespace and prefixes each tier's short name, so
`event.create('assembling_machine').tiers(LV, MV, HV)` produces
**`gtceu:lv_assembling_machine`**, `gtceu:mv_assembling_machine` and
`gtceu:hv_assembling_machine`. A namespace passed into `create` is discarded. `GTRecipeTypes`
behaves the same way, so the recipe type is **`gtceu:assembling`**, not
`planetaryfactory:assembling`.

The machine table above keeps its display names, because that is what the argument was about: *a
Factorio player has to recognise the block on sight*. "Assembling Machine 1/2/3" is authored in
`kubejs/assets/gtceu/lang/en_us.json` and is what the player reads. The ids are internal, and the
only way to move them into the pack's namespace is to register the machines from
`planetaryfactory_core` with a registrate of its own — a larger change than this ADR's reasoning
asks for, and one that would spend the "no Java" property the row was chosen for.

**The recipe type is created at script-evaluation time, not in a registry event.** KubeJS fires
`gtceu:machine` *before* `minecraft:recipe_type` — the machine definitions ran at `.763` and the
recipe-type event at `1.458` of the same second — so a machine registered in the first event cannot
name a type created in the second, and GregTech reports it as "Tried to set null recipe type on
machine …". The type is therefore built with `GTRecipeTypes.register(...)` at the top level of
`kubejs/startup_scripts/machines.js`, which runs before any registry event fires. The GUI calls
this ADR lists are unaffected; they are the same builder methods either way, except that the
progress bar is `setProgressBar`, not `setProgressBarTexture`, and the model is
`workableTieredHullModel`, not a renderer.

## Considered Options

- **Replace GregTech with a custom-machines mod** (Modular Machinery Reborn, Custom Machinery).
  Rejected on surface area. GT's two rows in ADR-0017's table understate it: GCyR is a GT addon and
  every planet rides on it (ADR-0001, ADR-0003); Terra's veins, prospecting and depletion are GT
  worldgen (ADR-0007, ADR-0019, ADR-0020, ADR-0021); the oil chapter's fluid ids are GT materials and
  ADR-0009 binds Electro's oceans to `gtceu:heavy_oil`; and GT is the chassis ADR-0025 spends. A
  replacement buys one recipe shape and has to re-answer all of that. GT's *membership* is not
  reopened here.
- **Keep the stock GT Assembler and fix only the authoring.** Rejected: it leaves the circuit slot,
  the stock corpus and the GregTech read, all of which were named as costs.
- **Work around `GTRecipeBuilder`'s defects.** Rejected: the ore veins prove raw JSON works, and raw
  JSON makes GT's churn a diff against twenty-two existing files rather than an NPE at load.
- **Generate at build time, output gitignored.** Rejected — the point of leaving the builder was
  files that can be read and diffed.
- **Generate once as a seed, then hand-own.** Rejected: it rots the first time Factorio's data moves.

## Consequences

- **This is the pack's first pack-registered GT machine.** ADR-0025 states that "the pack already
  registers custom GT machines this way — Launch Terminals, Receiving Terminals and Drop Hatches."
  **That is not true of the repo**: no `MACHINE_REGISTRY` or machine-builder call exists anywhere in
  `kubejs/`. Those are GDD design. ADR-0025 is corrected on this point, and its conclusion is
  unaffected — but the Assembler is therefore the **prototype the Oil Refinery and Chemical Plant
  inherit**, and is worth more care than one table row.
- **ADR-0017's Assembly row changes owner** from GregTech to the pack. It becomes the third row whose
  owner is "the pack" rather than a mod.
- **ADR-0018 is untouched** and was the deciding authority twice — for speed-only tiers and for
  dropping the fluid restriction.
- **`gtceu:lv_assembler`'s craft is removed now**, along with the `kubejs:shaped/assembling_machine_1`
  id it was authored under. **This makes the interim pack unplayable past rung 0**, not merely
  rocketless: in stock GT the Drilling Rig controllers and most machine blocks are themselves
  Assembler recipes, so ADR-0017's extraction ladder goes with it. Accepted because the pack is
  pre-release and rocket crafting is being reworked regardless.
- **`recipes.js`'s shaped crafts for `gtceu:lv_machine_hull` and `planetaryfactory:electronic_circuit`
  survive** — they become Assembling Machine 1's ingredients.
- **GregTech's stock assembler corpus is not addressed here.** It is removed wholesale in the wipe
  that follows, not ported: nothing in it survives the re-authoring.
- **Issue #48 is unaffected.** `RecipeLogicMixin` wraps `RecipeLogic.matchRecipe`, and GTCEu ships one
  `RecipeLogic` class with no subclass, so it covers a pack-registered machine for free. Had the row
  moved *off* GregTech, all of #48 would have been thrown away — which is a standing argument for the
  chassis.
- **The work is two tickets, converter first**, because `setMaxIOSize` is read off the converter's
  output. The converter's claim is "cross-file references resolve" (a static data check in `tests/`,
  in the shape of `tests/factorio/test_tech_extract.py`); the registration's claim is "this thing is
  registered", whose check is nothing — but launch plus world creation is the floor for any change
  that touches datapack load.
