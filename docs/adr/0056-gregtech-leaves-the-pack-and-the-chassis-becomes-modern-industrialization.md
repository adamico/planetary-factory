---
status: accepted
supersedes: [73, 237, 238]
---

# GregTech leaves the pack, and the machine chassis becomes Modern Industrialization

ADR-0017 gave GregTech four rows of the ownership table and ADR-0026 built the Assembly row on a GT
chassis: three pack-registered machines, a pack-registered recipe type, GT supplying only the block
behind them. ADR-0025 did the same for the oil chapter. The bet was that GregTech could be used as a
*chassis* — machinery with the GregTech taken out.

`#236` is where that bet failed, and it failed on the one thing the chassis is for.

## The immediate cause

Terra's **Assembling Machine** sits idle holding the correct ingredients and shows no error. Reading
`GTRecipeLookup` out of `mods/gtceu-1.21.1-7.0.2.jar` says why. GregTech keys its recipe lookup on
the ingredient *set*, and when two recipes share a key it **refuses the second one into the trie at
load** and logs a warning:

```
Recipe duplicate or conflict found in GTRecipeType {} and was not added. See next lines for details
Attempted to add GTRecipe: {}
Which conflicts with: {}
```

The refused recipe stays in the vanilla `RecipeManager`, so it is registered, it passes every static
check and it is visible in EMI. It is simply not in the structure the machine searches. Two facts
make this invisible rather than merely broken: the warning is gated behind `dev.debug`, which
`config/gtceu.yaml:623-626` sets to `false`; and on the search path a refusal only means "does not
match", so the machine has nothing to display.

GregTech's own answer to a shared ingredient set is the **programmed circuit**. That is the idiom
ADR-0026 removed on purpose — `SimpleMachine.isCircuitSlotEnabled()` returns `false`, because a
circuit is a GregTech gesture and Factorio resolves the same ambiguity in the recipe picker.

**So the chassis was never separable from the mod.** The circuit is not a UI element bolted onto
GregTech's machines; it is how GregTech's recipe lookup is *addressed*. Take it out and the lookup
loses the ability to hold two recipes that share ingredients. Measured over the emitted corpus by
sorted ingredient set with counts ignored — the trie's actual key — **44 of 139 `gtceu:assembling`
recipes fall into 16 collision groups**, not the 13 in 3 that `#236` first reported.

## The rule

**GregTech CEu Modern leaves the pack. Modern Industrialization becomes the machine chassis.**

The pack keeps registering its own machines against its own recipe types, generated from the corpus
and committed. What changes is which mod supplies the block, the recipe lookup and the energy
capability underneath.

## Why Modern Industrialization

Not "a better GregTech". Three specific properties, each verified in the clone at `1cc3ef3`.

**Its recipe lookup keeps colliding recipes.** `CrafterComponent.getRecipes()`
(`machines/components/CrafterComponent.java:448-456`) is a flat list. Nothing is dropped at load.
`updateActiveRecipe` (`:379-400`) discovers the ambiguity at run time instead, and this is the
difference that matters: the recipes are all still there to choose between.

**It names its own failure.** `matchesMultipleRecipes` drives a GUI component
(`machines/blockentities/AbstractCraftingMachineBlockEntity.java:59`) that renders two lines
(`MIText.java:169-170`):

> The inputs match more than one possible recipe.
> Lock all output slots with at least one locked to the desired output.

A stated failure with a stated remedy. `#236`'s core complaint is silence, and MI treats breaking
that silence as part of the mechanism rather than as a debug flag.

**Its pack-facing API is wider than GregTech's.** `RegisterMachinesEventJS.craftingSingleBlock`
takes the slot counts, slot positions, bars and capacities as arguments, so a pack-authored machine
needs no Java subclass at all. `RegisterRecipeTypesEventJS`, `RegisterCasingsEventJS`,
`RegisterHatchesEventJS`, `RegisterCableTiersEventJS`, `AddMaterialsEventJS` and
`RegisterFluidsEventJS` are all callable from KubeJS.

It is NeoForge-native at this commit, which is worth stating because MI's history says otherwise:
`gradle.properties:13-16` is MC 1.21.1 and NeoForge `21.1.219+`, `build.gradle:401` declares
`modLoaders = ["neoforge"]`, and the Fabric transfer API is vendored in-tree rather than depended
on. The pack is on NeoForge `21.1.248`.

## What this costs, stated honestly

**Eight Java files in `planetaryfactory_core` import `com.gregtechceu.*`.** Two `RecipeLogic` mixins
retarget to `CrafterComponent`; `RuntimeHandRecipes` and `IdleMachineLockNote` retype from `GTRecipe`
to MI's `MachineRecipe`; three energy touchpoints move from `GTCapability.CAPABILITY_ENERGY_CONTAINER`
to `MIEnergyStorage`. `mixins.json` declares `"required": true`, so a stale entry is a crash and not
a warning.

**153 emitted recipes and 16 `data/pack/item-map.json` rows change namespace.** Free in effort — the
converters regenerate them — but every substitution table, the category map and `recipe_survivors.js`
carry `gtceu:` strings that have to be re-judged rather than rewritten mechanically.

**Nine of the pack's static checks are touched and two are deleted.** `tests/worldgen/` is the deepest
hit; that is covered below. `tests/pack/test_vein_indicators.py` exists only because GregTech's
indicator field is an `Either<BlockState, Material>` that fails at world creation, and it goes with
the mod.

**One texture loses its source.** `scripts/build-pick-textures.py` flattens the Steel Pick's sprite
out of the GTCEu jar.

**`guideme` is a required MI dependency and is not jar-in-jarred.** One new packwiz entry.

## What this does not cost, and why the first analysis said otherwise

Three things were argued as blockers and are not.

**The KubeJS pin is not a constraint, it is a dividend.** MI wants KubeJS
`>= 2101.7.2-build.290`; ADR-0023 pins the pack *below* `2101.7.2` because gtceu 7.0.2 holds a
compiled reference to the relocated `ServerEvents`. That makes MI and GTCEu unable to share an
instance — which is only an argument against *coexistence*, and coexistence was never wanted.
Removing GregTech removes the only thing holding the pin. **ADR-0023's constraint expires with this
ADR**; its explanation of the failure remains correct history.

**GregTech's ore veins were leaving anyway.** MI has no vein registry, no bedrock ore, no bedrock
fluid and no worldgen layers; it places ore with vanilla `Feature.ORE` and a biome modifier. But
Terra's veins are generated by `scripts/build-terra-ore.py` from pack-authored data, ADR-0045
already cut the buried veins, and what replaces the placement is an open decision rather than a
capability lost. The `tests/worldgen/` fixtures are re-derived, not abandoned.

**The prospector is not canon.** `gtceu:prospector.lv` is in the starting kit
(`core/start/StartingKit.java:50`) and MI has no equivalent — grep for `prospect` over MI's source
returns nothing. It was never an ADR's answer to anything, and the gesture it serves is open.

## What the pack does not take from MI

**Voltage tiers, cables, pipes and logistics.** MI ships `CableTier` LV/MV/HV/EV/SV
(`api/energy/CableTier.java:51-55`) with the same names GregTech uses. The pack does not use
GregTech's and will not use MI's. ADR-0036's Supply Area Pole is the distribution mechanism and
ADR-0025's rider still holds: a recipe carries no `EUt` and a tier gates nothing.

MI's machine parts are taken **only where there is no pack-authored equivalent and reinventing one
is disproportionate.** That is the same test ADR-0017 applied, and the reason this ADR does not
simply re-run ADR-0017's ownership table: the pack of 2026 authors most of its own mechanism.

## What is decided here and what is not

Decided: GregTech leaves; MI is the chassis; the KubeJS pin lifts with it; MI's cables, pipes,
voltage ladder and logistics are not adopted.

**Not decided, and each needs its own ADR or ticket:**

- ~~**How a machine is locked to a recipe.**~~ **Settled: the locked output slot is the surface.**
  This entry originally read that MI's slot-level lock was not the recipe lock the pack wants, on
  the grounds that a machine with locked outputs and empty inputs stores no recipe and displays
  none. That was a reading of the server-side code with no client evidence behind it, and it is
  **wrong in play**. One mechanism does three jobs: `updateActiveRecipe:384` takes the first
  startable recipe once `areAllOutputSlotsLocked()`, which works because
  `AbstractConfigurableStack.isResourceAllowedByLock:163` refuses a rival recipe's product and that
  recipe then fails its own start simulation; the lock persists in NBT (`:78`) and survives an empty
  slot, so it is the *display* as well as the selection; and `MIItemStorage:130-141` builds a pipe
  insert whitelist from the same locked instance, which is the overfill guard. EMI's Fill Recipe
  sets it with the ingredients absent. Copy/paste of machine configuration is not in MI; the
  reference implementation is Extended Industrialization's Machine Config Card, which copies each
  slot's locked instance and adjusted capacity, matches on the same block and identical slot counts,
  and applies on a simulate-then-act pass — read in
  `docs/research/modern-industrialization-slot-locking.md`. **The addon is prior art, not a
  dependency**: it arrives with 227 generated recipes and `tesseract_api`, all of which ADR-0034's
  sweep would have to be argued against, and the pack's own carrier is ADR-0039's Engineer's Pick,
  which is being reopened anyway because its GregTech wrench strings do not survive this ADR. What remains is coverage, not design — the candidate filter is the
  *product*, so it disambiguates a group only where the members have distinct outputs (`#238`).
- **What replaces GregTech's ore placement.**
- **What the Supply Area Pole and the Electric furnace tier speak.** Both currently speak GT EU, and
  `core/energy/EnergyLedger.java:22` hard-codes GregTech's 4 FE/EU. ADR-0035's argument that FE
  stops being a currency is unaffected; its arithmetic is not.
- **Whether ADR-0026's three-tier Assembly ladder keeps its shape.** MI's single-block machines are
  registered one at a time with their own capacities, so three tiers are three registrations and the
  ladder is reproducible — but ADR-0026's `KJSTieredMachineBuilder.tiers([LV, MV, HV])` and the ids
  `gtceu:{lv,mv,hv}_assembling_machine` that `#73` settled do not survive, which is what this ADR
  supersedes.

## Two consequences worth recording

**`#87`'s file-path invariant disappears.** GregTech re-registers every loaded `GTRecipe` —
`RecipeManagerLateMixin` strips everything before the first `/` of the id and `GTRecipeBuilder.save`
puts the recipe type's path back on — which is why `grid/`, `create/` and `pack/` all sit *inside*
`assembling/`. That is GregTech behaviour. With it gone the nesting is unnecessary and `FLAT_TYPES`
in `tests/factorio/test_recipe_duplication.py` becomes every type. The invariant must not be deleted
until the mod is actually gone from the instance.

**LDLib leaves the pack**, and `mod/build.gradle:37-56`'s requirement that exactly one `gtceu-*.jar`
sit in `mods/` so LDLib can be unpacked from it goes with it.

**`SimpleMachine.java` is deleted rather than ported.** It exists only to strip the circuit slot and
the charger slot from `SimpleTieredMachine`. MI's crafting machines have neither, so the class has
nothing left to do — which is the shortest available statement of why this ADR exists.
