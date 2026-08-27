package com.planetaryfactory.core.compat.jei;

import com.planetaryfactory.core.PlanetaryFactoryCore;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IAdvancedRegistration;
import net.minecraft.resources.ResourceLocation;

/**
 * The pack's JEI plugin. Loaded by JEI's own annotation scan, so nothing in the mod references this
 * class and it never loads when JEI is absent.
 */
@JeiPlugin
public final class PlanetaryFactoryJeiPlugin implements IModPlugin {

    private static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath(PlanetaryFactoryCore.MOD_ID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    /**
     * Registers the lock decorator against every recipe type JEI knows. EMI takes one global
     * decorator; JEI's registration is per-type with no global form, so the loop is what makes the
     * two viewers cover the same recipes.
     */
    @Override
    public void registerAdvanced(IAdvancedRegistration registration) {
        registration
                .getJeiHelpers()
                .getAllRecipeTypes()
                .forEach(type -> register(registration, type));
    }

    /** Only exists to capture the wildcard from {@code getAllRecipeTypes}' {@code RecipeType<?>}. */
    private static <T> void register(IAdvancedRegistration registration, RecipeType<T> type) {
        registration.addRecipeCategoryDecorator(type, new LockedRecipeJeiDecorator<>());
    }
}
