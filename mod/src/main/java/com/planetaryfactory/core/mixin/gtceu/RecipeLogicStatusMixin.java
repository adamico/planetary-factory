package com.planetaryfactory.core.mixin.gtceu;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.planetaryfactory.core.research.client.IdleMachineLockNote;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Makes an idle machine say that a research is what stops it (issue #79).
 *
 * <p>{@link RecipeLogicMixin} refuses the locked recipe; this is the other half, the answer to "so
 * why is it doing nothing". The refusal cannot carry its own reason on the fresh-search path --
 * there {@code matchRecipe} is the trie iterator's predicate, so a failure means only "this recipe
 * does not match" and the machine ends with no recipe selected and nothing to show a reason against
 * (ADR-0027, issue #48). Without this the player sees a machine indistinguishable from one that is
 * broken, mis-piped or unpowered.
 *
 * <p>{@code IFancyTooltip} is GregTech's own "why am I not running" question, asked of the recipe
 * logic by the machine screen and answered from {@code waitingReason}. The three methods here are
 * that question's three parts -- whether to show the mark, what icon it wears, and what it says --
 * and each defers to GregTech first: a machine that already has a waiting reason keeps it, because
 * a reason GregTech knows is a reason it is better at explaining.
 *
 * <p><b>Derived per call, never stored.</b> Nothing about the refused recipe is kept on the machine
 * and there is no invalidation rule, so emptying the machine or completing the research changes the
 * next answer by itself -- see {@link com.planetaryfactory.core.research.MachineLockStatus}. That is
 * the issue's explicit requirement, and the failure #76 was.
 *
 * <p>Client-side only, guarded at {@link #planetaryfactory$lockLines()} rather than by a client-only
 * mixin config: the enclosing class stays loadable on a dedicated server, and the client-only
 * {@link IdleMachineLockNote} is never resolved there because the call is never reached.
 */
@Mixin(value = RecipeLogic.class, remap = false)
public abstract class RecipeLogicStatusMixin {

    @Shadow
    @Final
    public IRecipeLogicMachine machine;

    @Shadow
    public abstract boolean isIdle();

    @WrapMethod(method = "showFancyTooltip")
    private boolean planetaryfactory$showLockReason(Operation<Boolean> original) {
        return original.call() || !planetaryfactory$lockLines().isEmpty();
    }

    @WrapMethod(method = "getFancyTooltip")
    private List<Component> planetaryfactory$explainLock(Operation<List<Component>> original) {
        List<Component> gregtechs = original.call();
        return gregtechs.isEmpty() ? planetaryfactory$lockLines() : gregtechs;
    }

    /**
     * The same icon GregTech puts on a machine waiting for input. A lock is that same shape of
     * problem to the player -- the machine is fed and still not running -- and inventing a second
     * warning glyph for it would say the difference matters before the tooltip has said what it is.
     */
    @WrapMethod(method = "getFancyTooltipIcon")
    private IGuiTexture planetaryfactory$lockIcon(Operation<IGuiTexture> original) {
        IGuiTexture gregtechs = original.call();
        if (gregtechs != IGuiTexture.EMPTY) return gregtechs;
        return planetaryfactory$lockLines().isEmpty() ? gregtechs : GuiTextures.INSUFFICIENT_INPUT;
    }

    /**
     * Empty unless an incomplete research is the whole of why this machine is idle.
     *
     * <p>The {@code isIdle} gate is what keeps the trie search off every other machine's status
     * query: a working, waiting or suspended machine has nothing to explain here and never reaches
     * the search.
     */
    private List<Component> planetaryfactory$lockLines() {
        if (!this.isIdle()) return List.of();

        MetaMachine self = this.machine.self();
        Level level = self.getLevel();
        if (level == null || !level.isClientSide) return List.of();

        return IdleMachineLockNote.tooltipFor(this.machine);
    }
}
