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
        List<String> refused = new ArrayList<>();
        for (RecipeHolder<?> holder : manager.getRecipes()) {
            if (!(holder.value() instanceof GTRecipe recipe)) continue;
            if (!isHandCraftable(recipe)) continue;
            String reason = unreadable(recipe);
            if (reason != null) {
                refused.add(recipe.id + " (" + reason + ")");
                continue;
            }
            builder.add(read(recipe));
        }
        if (!refused.isEmpty()) {
            // Named, not counted. A recipe in the hand category that the Assembler will not plan
            // reaches the player as a Crafting Plan with nothing in it, and the only way to tell
            // which recipe and why is to say so here.
            LOGGER.warn("Personal Assembler: {} hand recipe(s) refused: {}", refused.size(), refused);
        }
        return builder.build();
    }

    /**
     * Why this recipe cannot be planned, or null if it can.
     *
     * <p>Separate from {@link #read} so the refusal has a reason to log. A recipe in the hand
     * category that the Assembler silently ignores is worse than one it refuses out loud: the player
     * gets a plan dialog with three empty columns and no way to find out why.
     */
    private static String unreadable(GTRecipe recipe) {
        if (!recipe.inputs.keySet().stream().allMatch(cap -> cap == ItemRecipeCapability.CAP)) {
            return "a non-item input, which the Assembler has nowhere to hold";
        }
        if (!recipe.outputs.keySet().stream().allMatch(cap -> cap == ItemRecipeCapability.CAP)) {
            return "a non-item output";
        }
        if (recipe.hasTick()) return "a per-tick input or output";
        String inputs = unreadableSide(recipe.getInputContents(ItemRecipeCapability.CAP), false);
        if (inputs != null) return "an input with " + inputs;
        String outputs = unreadableSide(recipe.getOutputContents(ItemRecipeCapability.CAP), true);
        if (outputs != null) return "an output with " + outputs;
        if (recipe.getOutputContents(ItemRecipeCapability.CAP).isEmpty()) return "no item output";
        return null;
    }

    /**
     * Why one side cannot be read, or null.
     *
     * <p>The two sides differ on the one question that matters: an input matching several items is
     * a choice the resolver can make ({@link Ingredient}), while an output matching several is a
     * recipe whose result is unknowable until it runs, and a plan cannot promise an item it cannot
     * name. Tag ingredients and AlmostUnified's unification both land in the first case, which is
     * most of this pack's hand set.
     */
    private static String unreadableSide(List<Content> contents, boolean isOutput) {
        for (Content content : contents) {
            if (isOutput && content.isChanced()) return "a chance attached";
            if (!(content.content instanceof SizedIngredient sized)) return "no fixed count";
            ItemStack[] matches = sized.getItems();
            if (matches.length == 0) return "no item at all";
            if (isOutput && matches.length > 1) {
                return matches.length + " possible items (" + matches[0].getItem() + ", ...)";
            }
            for (ItemStack match : matches) {
                // An item whose identity is partly in its data components cannot be named by its
                // registry id, and naming it by that id anyway would fold Researchd's science packs
                // -- which differ only by a component -- onto one another.
                if (!match.getComponentsPatch().isEmpty()) {
                    return "data components on " + match.getItem();
                }
                if (match.getCount() <= 0) return "a count of zero";
            }
        }
        return null;
    }

    private static boolean isHandCraftable(GTRecipe recipe) {
        return recipe.data != null && HAND_CATEGORY.equals(recipe.data.getString(CATEGORY_KEY));
    }

    /** One GregTech recipe as the resolver sees it. Only ever called after {@link #unreadable}. */
    private static HandRecipe read(GTRecipe recipe) {
        return new HandRecipe(
                recipe.id.toString(),
                ingredients(recipe.getInputContents(ItemRecipeCapability.CAP)),
                amounts(recipe.getOutputContents(ItemRecipeCapability.CAP)),
                recipe.duration);
    }

    private static List<Ingredient> ingredients(List<Content> contents) {
        List<Ingredient> read = new ArrayList<>(contents.size());
        for (Content content : contents) {
            ItemStack[] matches = ((SizedIngredient) content.content).getItems();
            List<String> items = new ArrayList<>(matches.length);
            for (ItemStack match : matches) {
                items.add(BuiltInRegistries.ITEM.getKey(match.getItem()).toString());
            }
            // Every match of one ingredient carries the same count, so the first one's is the
            // ingredient's. getItems() has already applied it -- read out of NeoForge's bytecode,
            // where it maps the ingredient's stacks through copyWithCount(count). Reading count()
            // as well would double every ingredient in the pack.
            read.add(new Ingredient(items, matches[0].getCount()));
        }
        return read;
    }

    private static List<ItemAmount> amounts(List<Content> contents) {
        List<ItemAmount> read = new ArrayList<>(contents.size());
        for (Content content : contents) {
            ItemStack stack = ((SizedIngredient) content.content).getItems()[0];
            read.add(new ItemAmount(
                    BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), stack.getCount()));
        }
        return read;
    }
}
