package com.planetaryfactory.core.recipes;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

/**
 * A smelt whose ingredient carries a count (#155).
 *
 * <p>Vanilla's own {@code SmeltingRecipe} holds a bare {@link Ingredient} with no count field
 * while its result is an {@link ItemStack} that has one, so 1:n smelts fine and <b>m:n cannot be
 * expressed at all</b>. Two of the four smelting recipes in the Factorio corpus need it:
 * {@code steel-plate} is 5 iron plates to 1, and {@code stone-brick} is 2 stone to 1 (ADR-0046).
 *
 * <h2>This deliberately does not extend {@code SmeltingRecipe}</h2>
 *
 * <p>GregTech's {@code GTRecipeType.proxyRecipes} converts {@code SmeltingRecipe} specifically and
 * reads only the single vanilla ingredient, so a subclass would have its count <em>dropped</em>:
 * {@code 5 iron_plate -> 1 steel_plate} would silently become {@code 1 iron_plate -> 1
 * steel_plate}. Wrong output, no error, no log line. Implementing {@link Recipe} directly is what
 * keeps that conversion from ever seeing this type.
 *
 * <p>{@code cookingtime} is Factorio's {@code energy_required * 20} with no speed divisor in it
 * (ADR-0029); the block applies its own {@code crafting_speed}.
 */
public record SmeltingRecipe(Ingredient ingredient, int count, ItemStack result, int cookingTime)
        implements Recipe<SingleRecipeInput> {

    /** Long enough to be visible, short enough not to be a gate. Vanilla's own default. */
    public static final int DEFAULT_COOKING_TIME = 200;

    /**
     * Whether this recipe would take the stack at all, ignoring how many are held.
     *
     * <p>Insertion routing asks this rather than {@link #matches}: a hopper handing over one iron
     * plate at a time must reach the input slot, or a 5:1 recipe could never be fed.
     */
    public boolean isIngredient(ItemStack stack) {
        return ingredient.test(stack);
    }

    /** Matching needs the whole count present, which is what makes the smelt an m:n one. */
    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return ingredient.test(input.item()) && input.item().getCount() >= count;
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result;
    }

    /** The furnace is one slot wide; the dimensions are a crafting-grid notion it never uses. */
    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public RecipeSerializer<SmeltingRecipe> getSerializer() {
        return PFRecipes.SMELTING_SERIALIZER.get();
    }

    @Override
    public RecipeType<SmeltingRecipe> getType() {
        return PFRecipes.SMELTING_TYPE.get();
    }

    public static final class Serializer implements RecipeSerializer<SmeltingRecipe> {

        private static final MapCodec<SmeltingRecipe> CODEC = RecordCodecBuilder.mapCodec(
                instance -> instance.group(
                        Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(SmeltingRecipe::ingredient),
                        ExtraCodecs.POSITIVE_INT.optionalFieldOf("count", 1).forGetter(SmeltingRecipe::count),
                        ItemStack.CODEC.fieldOf("result").forGetter(SmeltingRecipe::result),
                        Codec.INT.optionalFieldOf("cookingtime", DEFAULT_COOKING_TIME)
                                .forGetter(SmeltingRecipe::cookingTime))
                        .apply(instance, SmeltingRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, SmeltingRecipe> STREAM_CODEC =
                StreamCodec.composite(
                        Ingredient.CONTENTS_STREAM_CODEC, SmeltingRecipe::ingredient,
                        ByteBufCodecs.VAR_INT, SmeltingRecipe::count,
                        ItemStack.STREAM_CODEC, SmeltingRecipe::result,
                        ByteBufCodecs.VAR_INT, SmeltingRecipe::cookingTime,
                        SmeltingRecipe::new);

        @Override
        public MapCodec<SmeltingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, SmeltingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
