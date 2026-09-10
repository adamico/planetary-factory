# How Modern Industrialization resolves an ambiguous recipe

Read against a clone of `AztechMC/Modern-Industrialization` at `1cc3ef3` (2026-09-10), beside the
pack as `../mi-src`. Line numbers are that commit's. Written for #236, where GregTech's answer to
the same question is the programmed circuit and this pack has removed the circuit (ADR-0026).

MI is a GregTech-shaped mod that infers a machine's recipe from its input slots, exactly as GregTech
does. It therefore has #236's problem natively. It does not solve it with a circuit, and — this is
the part worth knowing — **it does not solve it by locking the machine to a recipe either.** It
locks *slots to items*, and lets the item filters make the inference unambiguous.

## The three parts

### 1. Ambiguity is detected and named, not suffered

`CrafterComponent.updateActiveRecipe` (`machines/components/CrafterComponent.java:379`) walks every
candidate recipe. If a second recipe can also start, it asks `overlaps(first, second)` — do both
recipes match the *same* input stack — and if so it sets `matchesMultipleRecipes = true` and
**returns false**. The machine does not start. So MI's failure is GregTech's failure: an idle
machine with the correct items in it.

The difference is entirely in what the player is told. The flag is a GUI component,
`MachineProblemsDisplay(crafter::matchesMultipleRecipes)`
(`machines/blockentities/AbstractCraftingMachineBlockEntity.java:59`), and it renders two lines
(`MIText.java:169`):

> The inputs match more than one possible recipe.
> Lock all output slots with at least one locked to the desired output.

That is the whole of MI's user-facing design: the failure states itself and states its remedy.

### 2. The disambiguator is the locked *output* slot, not the input

`updateActiveRecipe:384` reads `areAllOutputSlotsLocked()` before the walk. If every output slot is
locked, the loop takes the **first** recipe that can start and `break`s — the overlap check never
runs.

This works because `canStartRecipe` (`:462`) is `takeItemInputs && takeFluidInputs &&
putItemOutputs && putFluidOutputs`, all in simulation. A locked slot accepts only its locked
instance (`AbstractConfigurableStack.isResourceAllowedByLock:162` — `lockedInstance == null ||
lockedInstance == instance`), so a competing recipe's product has nowhere to go and the recipe fails
its own simulation. The candidate set is filtered by the **product**, which is how Factorio's recipe
picker is indexed too.

Note what this buys and what it does not. It resolves the ambiguity; it does not make the machine
declare a recipe. An MI machine with locked outputs and no inputs is still idle and still knows
nothing.

### 3. Locking is a slot filter with two owners

`AbstractConfigurableStack` (`inventory/AbstractConfigurableStack.java:50`) holds three fields per
slot: `lockedInstance`, `playerLocked`, `machineLocked`. They persist in the stack's NBT (`:78`).

- `machineLocked` is set by the crafter itself when it starts a recipe and cleared when the recipe
  ends — the running recipe reserves its own output slots.
- `playerLocked` is the player's, and it is what survives.
- `updatedLockedInstance:225` is the rule that makes an **empty** locked slot mean something: when a
  slot is locked while empty, `lockedInstance` becomes the empty instance, and the slot then accepts
  nothing at all. That is the "lock a slot empty so it never overfills" behaviour.

The GUI has a locking mode toggle (`MIText.java:165`, `SetLockingModePacket`), and shift-click
unlocks everything (`ConfigurableScreenHandler.lockAll:251`, `LockAllPacket`).

## The gesture: EMI's Fill Recipe, doing two different things

`client/compat/viewer/impl/emi/MachineRecipeHandler.java` is a `StandardRecipeHandler`, and it
branches on which screen is on top:

```java
if (Minecraft.getInstance().screen == context.getScreen()) {
    return StandardRecipeHandler.super.craft(recipe, context);   // let EMI move items
} else {
    return lockSlots(recipe, context.getScreen(), true);          // lock the slots instead
}
```

`canCraft` branches the same way (`:68`). This matters for #236's requirement that the button work
with the ingredients absent: on the locking branch `canCraft` never consults
`context.getInventory().canCraft(recipe)` — it returns whether locking is *allowed*, which is a
server-supplied flag (`ReiSlotLocking.extractData`). So the button is lit with an empty inventory,
for the locking gesture only.

`lockSlots` sends `ReiLockSlotsPacket(containerId, recipeId)` and re-asserts the screen, the same
race the pack's own `PersonalAssemblerEmiHandler` documents.

## Server side: what `lockRecipe` actually chooses

`CrafterComponent.lockRecipe(recipeId, playerInventory)` (`:782`) resolves the recipe by id, then
for each input and output calls `handleLocking`, which skips slots already locked to a matching
instance and otherwise locks fresh slots via `playerLockNoOverride`. Finally `lockAll` (`:884`)
locks every still-empty, still-unlocked slot **to nothing**, which is what stops a pipe filling the
machine with something the recipe does not want.

The interesting part is how it picks *which* item to lock a tag ingredient to (`:794`), in order:

1. an item the player is carrying that matches the ingredient;
2. **AlmostUnified's target item** for the first candidate (`AlmostUnifiedFacade.INSTANCE.getTargetItem`);
3. the first candidate in the `minecraft:` or MI's own namespace;
4. the first item in the tag.

Rule 2 is worth flagging here: MI treats the unification target as the canonical member of a tag
when it has to choose one. This pack's equivalent choice is ADR-0053, which is accepted and
unapplied.

## What MI has no answer for

There is no copy or paste of a machine's configuration anywhere in the source — no clipboard, no
configuration card, no settings tool. Factorio's shift-click copy of entity settings has no
counterpart in MI, so the third requirement in #236's gesture has no prior art to borrow here.
