package com.planetaryfactory.core.mixin.gtceu;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.ActionResult;
import com.gregtechceu.gtceu.api.recipe.kind.GTRecipe;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.planetaryfactory.core.research.LockedRecipeRetry;
import com.planetaryfactory.core.research.PlacedByOwnership;
import com.planetaryfactory.core.research.ResearchLocks;
import com.portingdeadmods.researchd.api.RecipeFilterContext;
import com.portingdeadmods.researchd.api.ResearchdApi;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

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
 * <p>The team frame is already free -- for a machine that has an owner. GT machines tick through a
 * plain vanilla {@code BlockEntityTicker} ({@code IMachineBlock.getTicker}), so Researchd's
 * {@code BoundTickingBlockEntityMixin} has pushed the owning team's frame before any of this runs.
 * For a multiblock that owner is the controller's, which is the block the player placed.
 *
 * <p>A machine that reached the world by anything other than player placement -- {@code /setblock},
 * {@code /clone}, worldgen -- carries no owner, and every lock passes for it. Failing open is
 * deliberate: it belongs to no team and has no lock to honour, and refusing would turn a silent
 * bypass into a silent brick the player cannot fix. Issue #74 kept that and added
 * {@link ResearchLocks}, which logs the first such bypass at each position so the case is legible
 * from a log rather than only from an in-game A/B.
 *
 * <p><b>An unowned machine still gets a frame.</b> Issue #74 describes the bypass as
 * {@code current()} returning null, and that is not what happens -- checked in-game, where a
 * {@code /setblock} Assembler crafted a locked recipe and logged nothing at all. Researchd's
 * {@code PLACED_BY_UUID} attachment is declared with the zero UUID as its <em>default</em>, so
 * {@code getData} never returns null; {@code getOrMigratePlacedByTeam} hands that zero UUID straight
 * back, {@code BoundTickingBlockEntityMixin} finds it non-null and pushes a frame for a team that
 * does not exist. The lock then passes not because there is no frame but because no effect data is
 * stored against that team.
 *
 * <p>So the bypass test is the team, not the frame: a null frame <em>or</em> a frame whose team is
 * the empty UUID. The null case is kept because nothing guarantees Researchd will always push.
 *
 * <p>Only the recipe id is tested, not {@code isBlocked}'s item rules: {@code GTRecipe} carries its
 * contents as capabilities, so {@code getResultItem} is always empty and {@code getIngredients}
 * yields nothing. An item-unlock effect can say nothing about a GT recipe. ADR-0018's spine is
 * {@code unlock_recipe} throughout, so this costs the pack nothing.
 */
@Mixin(value = RecipeLogic.class, remap = false)
public abstract class RecipeLogicMixin {

    @Shadow
    @Final
    public IRecipeLogicMachine machine;

    /**
     * GregTech's memory of recipes it matched but could not run. Its emptiness is the last term of
     * {@code serverTick}'s unsubscribe condition, which makes it the whole of the fix for #76 --
     * see {@link LockedRecipeRetry}.
     */
    @Shadow
    public List<GTRecipe> lastFailedMatches;

    @WrapMethod(method = "matchRecipe")
    private ActionResult planetaryfactory$refuseLockedRecipe(
            GTRecipe recipe, Operation<ActionResult> original) {
        RecipeFilterContext.Frame frame = RecipeFilterContext.current();
        if (frame == null || !PlacedByOwnership.isOwned(frame.teamId())) {
            // No owner, so the recipe runs. This is the pinned decision of issue #74, not an
            // oversight: a machine reaching the world by /setblock, /clone or worldgen belongs to
            // no team and has no lock to honour, and refusing would turn a silent bypass into a
            // silent brick the player cannot fix. The report is the whole change -- the outcome is
            // deliberately identical to what it always was.
            planetaryfactory$noteUnownedBypass(recipe);
            return original.call(recipe);
        }
        if (ResearchdApi.isRecipeBlocked(frame.level(), frame.teamId(), recipe.id)) {
            // Remember the refusal so the machine stays on the tick list and re-searches once the
            // research lands. Without this a machine loaded with a locked recipe's ingredients
            // unsubscribes itself and only a break-and-replace ever starts it -- issue #76.
            this.lastFailedMatches = LockedRecipeRetry.remember(this.lastFailedMatches, recipe, r -> r.id);
            // The reason is carried but not shown on the search path: there matchRecipe is
            // the iterator's predicate, so a failure only means "this recipe does not
            // match" and the machine ends with no recipe selected and nothing to display a
            // reason against. It surfaces only on the lastRecipe fast path, where a recipe
            // was already selected. A locked recipe therefore reads to the player as a
            // machine that silently does nothing -- verified in-game, issue #48.
            return ActionResult.fail(Component.translatable("planetaryfactory_core.recipe.locked"));
        }
        return original.call(recipe);
    }

    /**
     * Reports the bypass, never letting it break the machine. This sits on the hot path of every
     * unowned machine's recipe match, so it is guarded rather than trusted: {@code ResearchLocks}
     * reaches into Researchd's registry, and a machine ticking in a half-loaded world must run its
     * recipe whatever that lookup does.
     */
    private void planetaryfactory$noteUnownedBypass(GTRecipe recipe) {
        try {
            MetaMachine self = this.machine.self();
            Level level = self.getLevel();
            if (level != null && !level.isClientSide) {
                ResearchLocks.noteUnownedBypass(level, self.getPos(), recipe.id);
            }
        } catch (RuntimeException | LinkageError ignored) {
            // A log line is never worth a crash.
        }
    }
}
