package com.planetaryfactory.core.fluid;

import javax.annotation.Nullable;

import com.planetaryfactory.core.PFBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
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
import net.minecraft.world.phys.BlockHitResult;

/**
 * Terra's Boiler (#224, ADR-0048): the block that turns fuel and water into steam.
 *
 * <p>The shell. The rate is {@link BoilerSpec}'s, the stall is {@link BoilerCycle}'s and the tanks
 * are {@link BoilerBlockEntity}'s; none of the first two touches Minecraft, which is what lets both
 * be asserted without a world.
 *
 * <p><b>One block, one tier.</b> Factorio has one boiler and so does this pack: ADR-0033 has the
 * reactor emitting superheated steam directly with no heat layer, so there is no second rung here
 * for a ladder to climb.
 *
 * <p>Facing is cosmetic, as the pump's is. Fluid and fuel both reach the block on every face --
 * Factorio decides in-or-out by the inserter rather than by the machine -- and the orientation
 * exists so the player can see which side the firebox is on.
 */
public class BoilerBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    /**
     * A block codec is only read by data generation, which this pack does not run -- present
     * because {@link BaseEntityBlock} makes it abstract, the same reason {@code OffshorePumpBlock}
     * carries one.
     */
    public static final com.mojang.serialization.MapCodec<BoilerBlock> CODEC =
            simpleCodec(properties -> new BoilerBlock());

    public BoilerBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(3.5F)
                .requiresCorrectToolForDrops()
                .sound(SoundType.METAL));
        registerDefaultState(getStateDefinition().any().setValue(FACING, Direction.NORTH));
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
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BoilerBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, PFBlockEntities.BOILER.get(),
                (tickLevel, pos, tickState, entity) -> entity.serverTick());
    }

    /** Plain right-click opens the Boiler, because it has a fuel slot -- the rig's own rule. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof BoilerBlockEntity boiler) {
            player.openMenu(boiler);
        }
        return InteractionResult.CONSUME;
    }

    /**
     * Breaking a Boiler pays back the fuel it still holds.
     *
     * <p>Nothing in this pack is a resource sink <em>in items</em>. Coal is finite in the ground
     * under ADR-0041, and voiding a stack of it on a break would make dismantling a machine cost
     * the player ore they had already mined.
     *
     * <p>What does go is the water, the steam and the part-spent joules in the buffer -- none of
     * them is an item to drop, and all three are a tick or two of a machine that is still running
     * somewhere. That is the furnace ladder's own bargain and it is stated here rather than left
     * to be noticed.
     */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState,
            boolean moved) {
        if (!state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof BoilerBlockEntity boiler) {
            Containers.dropContents(level, pos, boiler);
        }
        super.onRemove(state, level, pos, newState, moved);
    }
}
