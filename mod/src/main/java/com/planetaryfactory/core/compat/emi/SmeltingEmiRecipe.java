package com.planetaryfactory.core.compat.emi;

import com.planetaryfactory.core.recipes.SmeltingRecipe;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * One pack smelt as EMI draws it (#155).
 *
 * <p>The count is the whole reason this class exists rather than EMI's own cooking display. EMI
 * builds vanilla's from {@code SmeltingRecipe}, which the pack's type deliberately does not extend,
 * so a pack smelt is in no viewer at all until something puts it in one -- and the number the
 * player most needs from the 5:1 steel recipe is the 5, which vanilla's shape cannot carry.
 *
 * <p>Time is the recipe's own {@code cookingtime}, which is Factorio's {@code energy_required}
 * before any tier divides it (ADR-0029). It is labelled as the Stone tier's, since that is the tier
 * whose {@code crafting_speed} is 1 -- quoting a bare number that no furnace in the pack actually
 * takes would be the misleading half of being accurate.
 */
public class SmeltingEmiRecipe extends BasicEmiRecipe {

    private final int cookingTime;

    public SmeltingEmiRecipe(EmiRecipeCategory category, RecipeHolder<SmeltingRecipe> holder) {
        super(category, holder.id(), 106, 32);
        SmeltingRecipe recipe = holder.value();
        this.cookingTime = recipe.cookingTime();
        this.inputs = List.of(EmiIngredient.of(recipe.ingredient(), recipe.count()));
        this.outputs = List.of(EmiStack.of(recipe.result()));
    }

    @Override
    public void addWidgets(dev.emi.emi.api.widget.WidgetHolder widgets) {
        widgets.addTexture(EmiTexture.EMPTY_ARROW, 24, 5);
        widgets.addFillingArrow(24, 5, cookingTime * 50);
        widgets.addText(seconds(), 26, 24, 0xFF808080, false);
        widgets.addSlot(inputs.get(0), 0, 4);
        widgets.addSlot(outputs.get(0), 58, 4).recipeContext(this);
    }

    private Component seconds() {
        return Component.translatable("emi.planetaryfactory.smelting.seconds",
                String.format("%.1f", cookingTime / 20F));
    }
}
