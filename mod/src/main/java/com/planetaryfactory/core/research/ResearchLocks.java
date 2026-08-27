package com.planetaryfactory.core.research;

import com.mojang.logging.LogUtils;
import com.portingdeadmods.researchd.api.ResearchdApi;
import com.portingdeadmods.researchd.api.research.Research;
import com.portingdeadmods.researchd.api.research.ResearchManager;
import com.portingdeadmods.researchd.api.research.effects.ResearchEffect;
import com.portingdeadmods.researchd.api.research.effects.ResearchEffectList;
import com.portingdeadmods.researchd.impl.research.effect.RecipeUnlockEffect;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

/**
 * The pack's view of Researchd's recipe locks: which recipes any research unlocks, and one log line
 * the first time a machine that owns no team runs one of them.
 *
 * <p>This is the only place that knows how to fill a {@link RecipeResearchIndex} from a level.
 * {@link RecipeResearchIndex} and {@link LockBypassLog} carry the logic and are Minecraft-free; this
 * class is the glue and holds no rules of its own.
 *
 * <p>Issue #74. A block entity carrying no Researchd placed-by attachment pushes no filter frame,
 * so {@code RecipeLogicMixin} has no team to test and every locked recipe runs. That stays true --
 * failing open is the decision, not the bug. The bug was that it was silent, and this makes it
 * legible from a log rather than only from an in-game A/B.
 */
public final class ResearchLocks {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final LockBypassLog BYPASSES = new LockBypassLog();

    /**
     * The manager Researchd hands out is a fresh object on every datapack reload
     * ({@code ResearchManagerImpl.setNewInstance}), so its identity is an exact and free
     * invalidation signal -- no reload listener to register and nothing that can drift out of sync
     * with the registry the index was built from.
     */
    private static volatile ResearchManager builtFrom;

    private static volatile RecipeResearchIndex<ResourceLocation, ResourceKey<Research>> index =
            RecipeResearchIndex.empty();

    private ResearchLocks() {}

    /**
     * Records that an unowned machine at {@code pos} ran {@code recipeId}, logging it at most once
     * per position per session and only when some research unlocks that recipe.
     */
    public static void noteUnownedBypass(Level level, BlockPos pos, ResourceLocation recipeId) {
        RecipeResearchIndex<ResourceLocation, ResourceKey<Research>> current = index(level);
        LockBypassLog.Site site = new LockBypassLog.Site(
                level.dimension().location().toString(), pos.getX(), pos.getY(), pos.getZ());

        if (!BYPASSES.shouldReport(current, recipeId, site)) return;

        LOGGER.debug(
                "Recipe {} at {} {} ran unfiltered: the machine carries no Researchd owner, so no team lock applies."
                        + " Researches that unlock it: {}. See issue #74.",
                recipeId,
                site.dimension(),
                pos.toShortString(),
                current.researchesUnlocking(recipeId));
    }

    /**
     * The recipe-to-research index for the current datapack state, rebuilt when Researchd reloads.
     *
     * <p>Every unowned machine consults this on every recipe match, so the steady state -- the index
     * is current -- is two volatile reads and no lock. Only the reload rebuild is serialised, and it
     * is re-checked inside the lock so a reload cannot set two threads building the same index.
     */
    public static RecipeResearchIndex<ResourceLocation, ResourceKey<Research>> index(Level level) {
        ResearchManager manager = ResearchdApi.getResearchManager();
        if (manager == null) return RecipeResearchIndex.empty();
        if (manager == builtFrom) return index;

        return rebuild(manager, level);
    }

    private static synchronized RecipeResearchIndex<ResourceLocation, ResourceKey<Research>> rebuild(
            ResearchManager manager, Level level) {
        if (manager == builtFrom) return index;

        RecipeResearchIndex<ResourceLocation, ResourceKey<Research>> built = build(manager, level);
        index = built;
        builtFrom = manager;
        return built;
    }

    private static RecipeResearchIndex<ResourceLocation, ResourceKey<Research>> build(
            ResearchManager manager, Level level) {
        RecipeResearchIndex.Builder<ResourceLocation, ResourceKey<Research>> builder = RecipeResearchIndex.builder();
        for (ResourceKey<Research> key : manager.getResearches()) {
            Research research = manager.lookupResearch(key, level);
            if (research != null) {
                collect(research.researchEffect(), key, builder);
            }
        }
        return builder.build();
    }

    /**
     * Walks one research's effect. An effect is a tree, not a single value: {@code and} is a
     * {@link ResearchEffectList} holding others, and a research that unlocks a recipe alongside an
     * item lock is written that way, so a flat instanceof check would miss it.
     */
    private static void collect(
            ResearchEffect effect,
            ResourceKey<Research> research,
            RecipeResearchIndex.Builder<ResourceLocation, ResourceKey<Research>> builder) {
        if (effect instanceof RecipeUnlockEffect unlock) {
            builder.add(research, unlock.recipes());
        } else if (effect instanceof ResearchEffectList list) {
            for (ResearchEffect child : list.effects()) {
                collect(child, research, builder);
            }
        }
    }
}
