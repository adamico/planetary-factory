package com.planetaryfactory.core.assembler;

import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.kind.GTRecipe;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import org.slf4j.Logger;

/**
 * The hand-craftable set, read off what the server actually loaded.
 *
 * <p>ADR-0038 gives the Personal Assembler no recipe type of its own: the hand set is a predicate
 * over Assembling Machine 1's recipes -- Factorio's first category {@code crafting}, which already
 * excludes the eleven withholds (#88). The converter writes that category onto every recipe it
 * emits, so the predicate is a field test and not a second list to keep in step.
 *
 * <p>Reading the recipe manager rather than the corpus is deliberate. The corpus says what the pack
 * intends; the recipe manager says what the player can actually be handed, and a Factorio name whose
 * {@code item-map.json} row is still {@code undecided} is emitted by nothing and must be planned by
 * nothing. {@code tests/factorio/test_hand_resolver.py} is the other direction -- that the corpus
 * admits a terminating plan for all 113.
 *
 * <p>Cached against the {@link RecipeManager}'s identity, the same trick {@code ResearchLocks} uses
 * for Researchd: a datapack reload builds a fresh manager, so its identity is an exact and free
 * invalidation signal with no reload listener to register.
 */
public final class RuntimeHandRecipes {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** The field the converter stamps on every recipe, and the whole definition of the hand set. */
    private static final String CATEGORY_KEY = "factorio_category";

    private static final String HAND_CATEGORY = "crafting";

    private static volatile RecipeManager builtFrom;
    private static volatile RecipeGraph graph = RecipeGraph.empty();

    private RuntimeHandRecipes() {}

    /** The graph for the level's current datapack state, rebuilt when the recipes reload. */
    public static RecipeGraph graph(Level level) {
        RecipeManager manager = level.getRecipeManager();
        if (manager == builtFrom) return graph;
        return rebuild(manager);
    }

    private static synchronized RecipeGraph rebuild(RecipeManager manager) {
        if (manager == builtFrom) return graph;
        RecipeGraph built = build(manager);
        graph = built;
        builtFrom = manager;
        LOGGER.info("Personal Assembler: {} hand-craftable recipe(s) loaded.", built.size());
        return built;
    }

    private static RecipeGraph build(RecipeManager manager) {
        RecipeGraph.Builder builder = RecipeGraph.builder();
        for (RecipeHolder<?> holder : manager.getRecipes()) {
            if (!(holder.value() instanceof GTRecipe recipe)) continue;
            if (!isHandCraftable(recipe)) continue;
            HandRecipe hand = read(recipe);
            if (hand != null) builder.add(hand);
        }
        return builder.build();
    }

    private static boolean isHandCraftable(GTRecipe recipe) {
        return recipe.data != null && HAND_CATEGORY.equals(recipe.data.getString(CATEGORY_KEY));
    }

    /**
     * One GregTech recipe as the resolver sees it, or null when it cannot be read as one.
     *
     * <p>A recipe with a non-item input is refused rather than read partially. The Assembler holds
     * no fluid and has no tank, so a plan that quietly dropped a fluid ingredient would reserve too
     * little and then craft something out of nothing -- the exact duplication
     * {@code QueuedPlan.completeStep} exists to make loud.
     */
    private static HandRecipe read(GTRecipe recipe) {
        if (hasNonItemContents(recipe)) return null;
        List<ItemAmount> inputs = amounts(recipe.getInputContents(ItemRecipeCapability.CAP), false);
        List<ItemAmount> outputs = amounts(recipe.getOutputContents(ItemRecipeCapability.CAP), true);
        if (inputs == null || outputs == null || outputs.isEmpty()) return null;
        return new HandRecipe(recipe.id.toString(), inputs, outputs, recipe.duration);
    }

    private static boolean hasNonItemContents(GTRecipe recipe) {
        return recipe.inputs.keySet().stream().anyMatch(cap -> cap != ItemRecipeCapability.CAP)
                || recipe.outputs.keySet().stream().anyMatch(cap -> cap != ItemRecipeCapability.CAP)
                || recipe.hasTick();
    }

    /**
     * Reads one side of a recipe, or null if any entry is not a plain fixed item.
     *
     * <p>A {@code SizedIngredient} can match several items -- a tag -- and the resolver names one
     * item per amount, so the first match would be a guess about what the player is expected to
     * spend. The converter emits fixed items for everything it routes to the Assembling Machine, so
     * refusing the ambiguous case costs the pack nothing and keeps the reservation exact.
     *
     * <p>A chanced output is refused for the same reason: the plan reserves and delivers exact
     * counts, and an output that might not appear would leave a later step short.
     */
    private static List<ItemAmount> amounts(List<Content> contents, boolean isOutput) {
        List<ItemAmount> read = new ArrayList<>(contents.size());
        for (Content content : contents) {
            if (isOutput && content.isChanced()) return null;
            if (!(content.content instanceof SizedIngredient sized)) return null;
            ItemStack[] matches = sized.getItems();
            if (matches.length != 1) return null;
            // getItems() has already applied the SizedIngredient's count -- read out of NeoForge's
            // bytecode, where it maps the ingredient's stacks through copyWithCount(count). Reading
            // count() as well would double every ingredient in the pack.
            var key = BuiltInRegistries.ITEM.getKey(matches[0].getItem());
            int count = matches[0].getCount();
            if (count <= 0) return null;
            read.add(new ItemAmount(key.toString(), count));
        }
        return read;
    }
}
