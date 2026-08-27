package com.planetaryfactory.core.compat.jei;

import com.planetaryfactory.core.research.client.LockedRecipeNote;
import java.util.List;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.category.extensions.IRecipeCategoryDecorator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Marks a recipe JEI lists that the local team has not researched -- the same badge, in the same
 * corner, with the same lines as EMI gets, all of them from {@link LockedRecipeNote} (issue #75).
 *
 * <p>One instance is registered per recipe type, because JEI's
 * {@code addRecipeCategoryDecorator} has no un-categorised overload the way EMI's does. The type
 * parameter is the category's recipe class and is never inspected here; the id comes from
 * {@code IRecipeCategory.getRegistryName}, which every category answers.
 *
 * <p>Both callbacks below run per frame, so the mark is live: it disappears as the research
 * completes, with no screen rebuild.
 *
 * @param <T> the recipe class of the category this decorator was registered against
 */
public final class LockedRecipeJeiDecorator<T> implements IRecipeCategoryDecorator<T> {

    @Override
    public void draw(
            T recipe,
            IRecipeCategory<T> category,
            IRecipeSlotsView slots,
            GuiGraphics graphics,
            double mouseX,
            double mouseY) {
        ResourceLocation id = category.getRegistryName(recipe);
        if (id == null || !LockedRecipeNote.isLocked(id)) return;

        LockedRecipeNote.drawBadge(graphics, category.getWidth());
    }

    /**
     * Adds the lock lines to whatever tooltip JEI is already showing inside the recipe, rather than
     * only to one over the badge. JEI calls this <em>while building a tooltip</em>, so a hover with
     * nothing tooltip-bearing under it never reaches here at all -- gating on the badge's own
     * rectangle would leave the explanation unreachable on every category whose badge corner is
     * empty.
     *
     * <p>So both viewers make the note reachable from anywhere in the recipe, and each does it the
     * only way its API allows: JEI has no hook that shows a tooltip of its own, only this one that
     * appends to another's, and EMI has no hook that appends, only a widget that fills whatever
     * the recipe's own widgets left uncovered. The visible difference -- JEI's lines ride along on
     * an ingredient tooltip, EMI's appear beside it -- is the two APIs, not two decisions.
     */
    @Override
    public void decorateTooltips(
            ITooltipBuilder tooltip,
            T recipe,
            IRecipeCategory<T> category,
            IRecipeSlotsView slots,
            double mouseX,
            double mouseY) {
        ResourceLocation id = category.getRegistryName(recipe);
        if (id == null) return;

        List<Component> lines = LockedRecipeNote.tooltipFor(id);
        if (!lines.isEmpty()) tooltip.addAll(lines);
    }
}
