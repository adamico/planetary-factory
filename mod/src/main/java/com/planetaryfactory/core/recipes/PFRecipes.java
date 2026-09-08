package com.planetaryfactory.core.recipes;

import com.planetaryfactory.core.PlanetaryFactoryCore;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The pack's own recipe types, which currently means {@code planetaryfactory:smelting} and
 * nothing else (#155).
 *
 * <p>It is the only type the three furnace tiers read. Vanilla's {@code minecraft:smelting} is not
 * read alongside it: ADR-0034's sweep removes every vanilla smelting recipe, so a dual read would
 * have no live consumer and would mean a recipe carrying vanilla's cook time and getting no tier
 * scaling. All four corpus smelting recipes are on this type, and anything re-admitted later is
 * authored on it with {@code count: 1}.
 */
public final class PFRecipes {

    public static final DeferredRegister<RecipeType<?>> TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, PlanetaryFactoryCore.NAMESPACE);

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, PlanetaryFactoryCore.NAMESPACE);

    public static final String SMELTING = "smelting";

    public static final DeferredHolder<RecipeType<?>, RecipeType<SmeltingRecipe>> SMELTING_TYPE =
            TYPES.register(SMELTING, () -> RecipeType.simple(
                    ResourceLocation.fromNamespaceAndPath(PlanetaryFactoryCore.NAMESPACE, SMELTING)));

    public static final DeferredHolder<RecipeSerializer<?>, SmeltingRecipe.Serializer> SMELTING_SERIALIZER =
            SERIALIZERS.register(SMELTING, SmeltingRecipe.Serializer::new);

    private PFRecipes() {
    }

    public static void register(IEventBus modBus) {
        TYPES.register(modBus);
        SERIALIZERS.register(modBus);
    }
}
