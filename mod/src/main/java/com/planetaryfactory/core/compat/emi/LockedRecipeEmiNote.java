package com.planetaryfactory.core.compat.emi;

import com.planetaryfactory.core.research.client.LockedRecipeNote;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.widget.WidgetHolder;
import java.util.List;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Marks a recipe EMI lists that the local team has not researched (issue #75).
 *
 * <p>Applied to every recipe EMI shows -- vanilla, GregTech, Create and IE alike, from one class.
 * That matters: Researchd's lock applies to every recipe source, and marking only one of them would
 * relocate the incoherence rather than fix it.
 *
 * <p>Driven from {@code RecipeDisplayMixin}, not from an {@code EmiRecipeDecorator}: EMI runs
 * registered decorators only under a developer-facing config flag that is off for players. The
 * mixin's javadoc has the evidence.
 *
 * <p>The lock is <em>not</em> read here -- this runs once, when the widgets are built, and both the
 * drawable and the tooltip re-ask per frame. That is what makes the badge vanish the moment the
 * research completes, with the recipe screen still open.
 */
public final class LockedRecipeEmiNote {

    private LockedRecipeEmiNote() {}

    public static void decorate(EmiRecipe recipe, WidgetHolder widgets) {
        ResourceLocation id = recipe.getId();
        if (id == null) return;

        int width = widgets.getWidth();
        widgets.addDrawable(
                LockedRecipeNote.badgeLeft(width),
                LockedRecipeNote.badgeTop(),
                LockedRecipeNote.badgeWidth(),
                LockedRecipeNote.badgeHeight(),
                (graphics, mouseX, mouseY, delta) -> {
                    if (LockedRecipeNote.isLocked(id)) LockedRecipeNote.drawBadge(graphics, width);
                });

        // The tooltip covers the whole recipe rather than just the badge, and that is the safe
        // direction rather than the greedy one. Checked in EMI's bytecode: RecipeScreen walks
        // WidgetGroup.widgets *forward* and stops at the first widget whose bounds hold the cursor
        // and whose getTooltip is non-empty -- so everything added after the recipe's own widgets
        // is behind them and can never take a tooltip away. Slots keep theirs; this fills the gaps.
        //
        // Scoping it to the badge instead would be the unsafe direction: wherever a slot already
        // sits in the top-right corner -- common in GregTech and Create layouts -- that slot wins
        // the hover and the lock lines become unreachable, leaving a red badge naming nothing.
        widgets.addTooltip((mouseX, mouseY) -> tooltip(id), 0, 0, width, widgets.getHeight());
    }

    /**
     * Empty when the team can run the recipe -- which is also how the widget stays out of the way:
     * an empty tooltip fails EMI's non-empty test, so an unlocked recipe's slots and the areas
     * between them behave exactly as they did before this existed.
     */
    private static List<ClientTooltipComponent> tooltip(ResourceLocation id) {
        List<Component> lines = LockedRecipeNote.tooltipFor(id);
        return lines.stream()
                .map(line -> ClientTooltipComponent.create(line.getVisualOrderText()))
                .toList();
    }
}
