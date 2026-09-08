package com.planetaryfactory.core.compat.emi;

import com.planetaryfactory.core.PlanetaryFactoryCore;
import com.planetaryfactory.core.recipes.PFRecipes;
import com.planetaryfactory.core.smelting.FurnaceTier;
import com.planetaryfactory.core.PFBlocks;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.resources.ResourceLocation;

/**
 * Puts the pack's smelts in EMI (#155 follow-up).
 *
 * <p>Without this they are in no recipe viewer at all. {@code planetaryfactory:smelting} is a
 * recipe type EMI has never heard of, and it cannot fall back on vanilla's cooking category
 * because the recipe class does not extend {@code SmeltingRecipe} -- the same separation that
 * stops GregTech's {@code proxyRecipes} from dropping the count also hides the recipe from every
 * viewer until a category exists. The furnace still smelted; only the teaching was missing.
 *
 * <p>All three tiers are workstations for the one category. They read the same recipe type and
 * differ only in speed and energy source, so a per-tier category would be three identical lists.
 */
@EmiEntrypoint
public final class SmeltingEmiPlugin implements EmiPlugin {

    private static final ResourceLocation ICON_ID =
            ResourceLocation.fromNamespaceAndPath(PlanetaryFactoryCore.NAMESPACE, "smelting");

    public static final EmiRecipeCategory SMELTING = new EmiRecipeCategory(
            ICON_ID, EmiStack.of(PFBlocks.furnace(FurnaceTier.STONE).get()));

    @Override
    public void register(EmiRegistry registry) {
        registry.addCategory(SMELTING);
        for (FurnaceTier tier : FurnaceTier.values()) {
            registry.addWorkstation(SMELTING, EmiStack.of(PFBlocks.furnace(tier).get()));
        }
        registry.getRecipeManager()
                .getAllRecipesFor(PFRecipes.SMELTING_TYPE.get())
                .forEach(holder -> registry.addRecipe(new SmeltingEmiRecipe(SMELTING, holder)));
    }
}
