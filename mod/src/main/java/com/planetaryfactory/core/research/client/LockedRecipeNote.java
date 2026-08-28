package com.planetaryfactory.core.research.client;

import com.planetaryfactory.core.research.RecipeLockLookup;
import com.planetaryfactory.core.research.RecipeResearchIndex;
import com.planetaryfactory.core.research.ResearchLocks;
import com.portingdeadmods.researchd.api.ResearchdApi;
import com.portingdeadmods.researchd.api.research.Research;
import java.util.List;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * What a recipe viewer draws on a recipe the local player's team has not researched: a badge and
 * the tooltip lines behind it (issue #75).
 *
 * <p>Viewer-neutral on purpose. EMI and JEI reach it from their own plugin packages and neither
 * type appears here, so the pack answers "is this locked, and what does it say" once rather than
 * twice -- the two viewers were shipping the same gap and must not now diverge on the fix.
 *
 * <p><b>Annotate, never hide.</b> Hiding is vanilla's habit and it tells the player nothing; the
 * whole point of the mark is to name the research to go and complete. Both viewers therefore keep
 * listing the recipe.
 *
 * <p>Client-only: it reads {@code Minecraft.getInstance()} for the local player, whose team is the
 * one being viewed as. Nothing on the server should call it.
 */
public final class LockedRecipeNote {

    /**
     * Drawn in the recipe's top-right corner. A translation key rather than a literal so a
     * resource pack can change it, and one glyph so it fits the smallest recipe either viewer
     * lays out.
     */
    public static final Component BADGE = Component.translatable("planetaryfactory_core.recipe_viewer.locked_badge");

    /** Vanilla's {@link ChatFormatting#RED}, as the packed ARGB that {@code drawString} wants. */
    private static final int BADGE_COLOR = 0xFFFF5555;

    /** Inset of the badge from the recipe's top-right corner, in pixels. */
    private static final int BADGE_INSET = 1;

    private LockedRecipeNote() {}

    /**
     * Whether the local player's team is locked out of {@code recipeId} -- the same question
     * {@link #tooltipFor} answers, for the callers that only need the yes or no.
     *
     * <p>Both viewers ask this per frame per visible recipe, so it exists to keep the badge from
     * building and discarding a tooltip's worth of {@link Component}s to decide whether to draw
     * one glyph.
     */
    public static boolean isLocked(ResourceLocation recipeId) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || recipeId == null) return false;

        // Deliberately not through RecipeLockLookup: naming the research needs the index, and the
        // badge does not. Keeping the index off this path means a client-side index that is empty
        // or late cannot swallow the mark -- the team's own blocked set is the whole answer to
        // "will the machine refuse this", and that is what the badge claims.
        return ResearchdApi.isRecipeBlocked(player, recipeId);
    }

    /**
     * Draws the badge in the top-right corner of a recipe {@code containerWidth} pixels wide.
     *
     * <p>Both viewers draw it through here rather than each placing it themselves. The badge is the
     * pack's mark, not EMI's or JEI's, and the two agreeing on where it sits and what colour it is
     * should not depend on two files being edited together.
     */
    public static void drawBadge(GuiGraphics graphics, int containerWidth) {
        Font font = Minecraft.getInstance().font;
        graphics.drawString(font, BADGE, badgeLeft(containerWidth), BADGE_INSET, BADGE_COLOR, true);
    }

    /** The badge's left edge inside a recipe {@code containerWidth} pixels wide. */
    public static int badgeLeft(int containerWidth) {
        return containerWidth - badgeWidth() - BADGE_INSET;
    }

    /** The badge's top edge. */
    public static int badgeTop() {
        return BADGE_INSET;
    }

    public static int badgeWidth() {
        return Minecraft.getInstance().font.width(BADGE);
    }

    public static int badgeHeight() {
        return Minecraft.getInstance().font.lineHeight;
    }

    /**
     * The tooltip for {@code recipeId}, or an empty list when the local player's team can run it --
     * so an empty result is the "draw nothing" signal, and callers need no second question.
     *
     * <p>Answered from scratch on every call, which is what makes the mark live: both viewers ask
     * per frame, so a recipe un-annotates itself the moment the research completes, with no screen
     * rebuild and no world reload. There is no state here to forget to invalidate -- the exact
     * failure mode of JEI's imperative {@code hideRecipes} alternative.
     */
    public static List<Component> tooltipFor(ResourceLocation recipeId) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return List.of();

        return lookupFor(player)
                .lockOn(recipeId)
                .map(lock -> lines(lock.unlockingResearches(), player.level()))
                .orElse(List.of());
    }

    /**
     * The lookup as {@code player}'s team sees it. Rebuilt per call rather than cached:
     * {@link ResearchLocks#index} already caches the expensive half against the research manager's
     * identity, and what is left is two field reads -- so there is no state here to forget to
     * invalidate, which is the whole reason the mark stays live.
     */
    private static RecipeLockLookup<ResourceLocation, ResourceKey<Research>> lookupFor(LocalPlayer player) {
        RecipeResearchIndex<ResourceLocation, ResourceKey<Research>> index = ResearchLocks.index(player.level());
        return RecipeLockLookup.of(index, id -> ResearchdApi.isRecipeBlocked(player, id));
    }

    private static List<Component> lines(Set<ResourceKey<Research>> researches, Level level) {
        return LockedByResearchLines.of(researches, level);
    }
}
