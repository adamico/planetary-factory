package com.planetaryfactory.core.mixin.emi;

import com.planetaryfactory.core.compat.emi.LockedRecipeEmiNote;
import dev.emi.emi.screen.RecipeDisplay;
import dev.emi.emi.screen.WidgetGroup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Draws the research-lock note on every recipe EMI builds (issue #75).
 *
 * <p>This exists because EMI's public seam for the job does not work for players. The obvious
 * implementation is {@code EmiRegistry.addRecipeDecorator}, and it registers fine -- but
 * {@code RecipeDisplay.getWidgets} runs the whole decorator list only when
 * {@code EmiConfig.showRecipeDecorators} is true, and that field defaults to
 * {@code EmiAgnos.isDevelopmentEnvironment()}, so it is false in every normal install. EMI
 * describes the setting as "typically developer facing ... not useful for players". A decorator is
 * therefore invisible to everyone the annotation is for.
 *
 * <p>Flipping the setting instead was rejected twice over: {@code config/emi.css} is the player's
 * own GUI configuration, which the pack does not own and EMI rewrites on save, and the flag is
 * global, so setting it would also switch on every other mod's developer widgets.
 *
 * <p>Injecting at RETURN, rather than replacing the config check, keeps the ordering the tooltip
 * logic depends on: the recipe's own widgets and EMI's buttons are already in the group, so
 * everything added here sits behind them and can never take a tooltip from a slot.
 * {@code getWidgets} also returns from a catch block that builds an error group, which carries no
 * recipe -- hence the null guard.
 */
@Mixin(RecipeDisplay.class)
public class RecipeDisplayMixin {

    @Inject(method = "getWidgets", at = @At("RETURN"))
    private void planetaryfactory_core$markLockedRecipe(
            int x, int y, int width, int height, CallbackInfoReturnable<WidgetGroup> cir) {
        WidgetGroup group = cir.getReturnValue();
        if (group == null || group.recipe == null) return;
        LockedRecipeEmiNote.decorate(group.recipe, group);
    }
}
