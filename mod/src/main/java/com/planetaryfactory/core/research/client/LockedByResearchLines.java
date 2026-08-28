package com.planetaryfactory.core.research.client;

import com.portingdeadmods.researchd.api.ResearchdApi;
import com.portingdeadmods.researchd.api.research.RegistryDisplay;
import com.portingdeadmods.researchd.api.research.Research;
import com.portingdeadmods.researchd.api.research.ResearchManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * "Locked by research", and the researches that would unlock it, written once.
 *
 * <p>Two places say this: the recipe-viewer annotation (issue #75) and an idle machine explaining
 * itself (issue #79). They are the same sentence about the same lock, and the issue asks for all of
 * it to read as one system alongside Researchd's own item-blocked tooltip -- which two copies of the
 * wording, in two packages, would not stay.
 *
 * <p>Client-only: the research names come from the local level's registries.
 */
public final class LockedByResearchLines {

    private LockedByResearchLines() {}

    /**
     * The tooltip for a lock: the header, then one line per research.
     *
     * <p>The header is not a parameter. Both callers want the same sentence in the same colour, and
     * a parameter with one value is the drift this class exists to prevent -- the wording is the
     * point, so it is written here once.
     *
     * @param researches the researches that would unlock it, in the order they should be read. May
     *     be empty -- the lock outran the index, see {@code RecipeLockLookup} -- in which case the
     *     lines say so rather than naming a research that may since have been renamed or removed.
     */
    public static List<Component> of(Set<ResourceKey<Research>> researches, Level level) {
        List<Component> lines = new ArrayList<>(researches.size() + 1);
        lines.add(Component.translatable("planetaryfactory_core.recipe_viewer.locked").withStyle(ChatFormatting.RED));

        if (researches.isEmpty()) {
            lines.add(Component.translatable("planetaryfactory_core.recipe_viewer.locked_by_unknown")
                    .withStyle(ChatFormatting.GRAY));
            return lines;
        }

        ResearchManager manager = ResearchdApi.getResearchManager();
        for (ResourceKey<Research> research : researches) {
            lines.add(Component.translatable(
                            "planetaryfactory_core.recipe_viewer.locked_by", nameOf(research, manager, level))
                    .withStyle(ChatFormatting.GRAY));
        }
        return lines;
    }

    /**
     * The name the research screen shows. The pack's researches are built by
     * {@code fromFactorio(...)}, which calls the KubeJS builder's {@code literalName} -- so they
     * carry a display name and no lang key at all, and {@code Research.getLangName} would render a
     * raw {@code research.planetary_factory.*_name} string. {@link RegistryDisplay} is the branch
     * that finds the literal one; the lang key stays as the fallback for a research declared
     * without it.
     */
    private static Component nameOf(ResourceKey<Research> research, ResearchManager manager, Level level) {
        Research declared = manager == null ? null : manager.lookupResearch(research, level);
        if (declared instanceof RegistryDisplay<?> display) {
            return display.getDisplayNameUnsafe(research);
        }
        return Research.getLangName(research);
    }
}
