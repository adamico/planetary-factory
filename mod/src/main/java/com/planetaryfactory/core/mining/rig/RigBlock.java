package com.planetaryfactory.core.mining.rig;

import javax.annotation.Nullable;

import com.planetaryfactory.core.PFBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
 * The rig's anchor (#192): the one block of the footprint that holds the block entity, carries
 * {@code FACING} and is the item every part forwards a break to.
 *
 * <p>One class serves both rigs, the way {@code FurnaceBlock} serves all three furnace tiers --
 * {@link #tier} is the only thing that differs between the burner and the electric anchor.
 *
 * <p><b>Facing is fixed at placement and never changes.</b> {@link RigBlockItem} is what actually
 * places this block (and every part around it); this class does not override
 * {@code getStateForPlacement}, because a single-block default would place the anchor without its
 * parts and defeat the whole idiom. Rotation after placement is out of scope (ADR-0043): a
 * mis-faced rig is broken and re-placed.
 */
public class RigBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    /**
     * A block codec is only read by data generation, which this pack does not run -- present
     * because {@link BaseEntityBlock} makes it abstract, the same reason {@code FurnaceBlock}
     * carries one.
     */
    public static final com.mojang.serialization.MapCodec<RigBlock> CODEC =
            com.mojang.serialization.Codec.STRING
                    .xmap(RigTier::byName, RigTier::serializedName)
                    .fieldOf("tier")
                    .xmap(RigBlock::new, RigBlock::tier);

    private final RigTier tier;

    public RigBlock(RigTier tier) {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(3.5F)
                .requiresCorrectToolForDrops()
                .sound(SoundType.METAL));
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
        return new RigBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        // Nothing ticks yet -- #192 places an inert footprint. #193/#194 add a ticker here the way
        // the furnace's serverTick was added once there was fuel to burn.
        return null;
    }

    /**
     * Breaking the anchor -- directly, by a player -- drops the item through the ordinary loot
     * table, so all this has to do is silently clear the parts around it (#192, bed-and-door).
     */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())) {
            RigBreaker.teardown(level, pos, tier, state.getValue(FACING), pos);
        }
        super.onRemove(state, level, pos, newState, moved);
    }
}
