package com.planetaryfactory.core.mining.rig;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.MapColor;

/**
 * One block of a rig's footprint that is not the anchor (#192).
 *
 * <p>Never held, never placed on its own -- {@link RigBlockItem} places all of them in one click
 * and nothing else ever should, which is why this block has no {@code BlockItem} and no recipe.
 * {@code FACING} is copied from the anchor purely for the model's orientation; the part carries no
 * behaviour of its own besides remembering the anchor's position ({@link RigPartBlockEntity}) and
 * forwarding a break there.
 *
 * <p>Its loot table is empty: breaking a part pays out the drill item by hand, in {@link
 * #onRemove}, so that exactly one item drops regardless of which block of the footprint the player
 * actually broke. See {@link RigBreaker}.
 */
public class RigPartBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public static final com.mojang.serialization.MapCodec<RigPartBlock> CODEC =
            com.mojang.serialization.Codec.STRING
                    .xmap(RigTier::byName, RigTier::serializedName)
                    .fieldOf("tier")
                    .xmap(RigPartBlock::new, RigPartBlock::tier);

    private final RigTier tier;

    public RigPartBlock(RigTier tier) {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(3.5F)
                .requiresCorrectToolForDrops()
                .sound(SoundType.METAL)
                // A part is never obtained on its own; picking with the wrong tool must not make
                // it drop something -- the item lives in code, not in a loot table roll.
                .noLootTable());
        this.tier = tier;
        registerDefaultState(getStateDefinition().any().setValue(FACING, Direction.NORTH));
    }

    public RigTier tier() {
        return tier;
    }

    @Override
    protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RigPartBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        return null;
    }

    /**
     * Breaking a part -- directly, by a player -- pops one drill item by hand (its loot table gives
     * nothing) and tears the rest of the rig down (#192, bed-and-door).
     */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && !level.isClientSide()
                && level.getBlockEntity(pos) instanceof RigPartBlockEntity part) {
            BlockPos anchorPos = part.anchorPos();
            if (anchorPos != null && level.getBlockState(anchorPos).getBlock() instanceof RigBlock rig) {
                popResource(level, pos, new ItemStack(
                        com.planetaryfactory.core.PFItems.rig(rig.tier()).get()));
                RigBreaker.teardown(level, anchorPos, rig.tier(),
                        level.getBlockState(anchorPos).getValue(RigBlock.FACING), pos);
            }
        }
        super.onRemove(state, level, pos, newState, moved);
    }
}
