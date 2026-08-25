package com.planetaryfactory.core.mixin.gtceu;

import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.ActionResult;
import com.gregtechceu.gtceu.api.recipe.kind.GTRecipe;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.portingdeadmods.researchd.api.RecipeFilterContext;
import com.portingdeadmods.researchd.api.ResearchdApi;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Makes GregTech machines respect Researchd's {@code unlock_recipe} locks.
 *
 * <p>GTCEu never asks the vanilla {@code RecipeManager}: every machine walks its own
 * {@code GTRecipeType} ingredient trie, so Researchd's {@code RecipeManagerMixin} never sees the
 * call. {@code RecipeFilterContext} documents the remedy — mixin into the foreign finder and test
 * each result — and this is that hook for GregTech.
 *
 * <p>{@code matchRecipe} is the single funnel. Both of {@code RecipeLogic}'s paths end here: the
 * fresh trie search (its predicate is {@code matchRecipe(...).isSuccess()}) and the
 * {@code lastRecipe} fast path (through {@code checkRecipe}). One mixin therefore covers every
 * machine in the mod — GTCEu ships exactly one {@code RecipeLogic} class, with no subclass to
 * override it — for singleblocks, multiblock controllers and generators alike.
 *
 * <p>No cache key needs scoping: the trie holds recipes rather than lookup results, and it is built
 * once at datapack load, so there is nothing per-team to keep apart.
 *
 * <p>The team frame is already free. GT machines tick through a plain vanilla
 * {@code BlockEntityTicker} ({@code IMachineBlock.getTicker}), so Researchd's
 * {@code BoundTickingBlockEntityMixin} has pushed the owning team's frame before any of this runs.
 * For a multiblock that owner is the controller's, which is the block the player placed.
 *
 * <p>Only the recipe id is tested, not {@code isBlocked}'s item rules: {@code GTRecipe} carries its
 * contents as capabilities, so {@code getResultItem} is always empty and {@code getIngredients}
 * yields nothing. An item-unlock effect can say nothing about a GT recipe. ADR-0018's spine is
 * {@code unlock_recipe} throughout, so this costs the pack nothing.
 */
@Mixin(value = RecipeLogic.class, remap = false)
public abstract class RecipeLogicMixin {

    @WrapMethod(method = "matchRecipe")
    private ActionResult planetaryfactory$refuseLockedRecipe(
            GTRecipe recipe, Operation<ActionResult> original) {
        RecipeFilterContext.Frame frame = RecipeFilterContext.current();
        if (frame != null && ResearchdApi.isRecipeBlocked(frame.level(), frame.teamId(), recipe.id)) {
            // The reason surfaces in the machine's own status readout and in Jade, so a locked
            // recipe reads as locked rather than as a machine that silently does nothing.
            return ActionResult.fail(Component.translatable("planetaryfactory_core.recipe.locked"));
        }
        return original.call(recipe);
    }
}
