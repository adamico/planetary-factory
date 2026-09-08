package com.planetaryfactory.core.smelting;

import javax.annotation.Nullable;

import com.planetaryfactory.core.PFBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;

/**
 * One of Factorio's three furnaces (#155). The tier is the block's, so the three ids point at one
 * block entity and one behaviour.
 *
 * <p>{@code FACING} is for the front texture and nothing else -- the inventory is unsided, so
 * which way the block looks never changes what an inserter or a funnel gets. {@code LIT} drives
 * the lit front and the glow, on all three tiers: the Electric Furnace lights when it is running,
 * which is the only thing distinguishing "powered and working" from "waiting for the pole".
 */
public class FurnaceBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    /**
     * A block codec is only ever read by data generation, which this pack does not run -- but
     * {@link BaseEntityBlock} makes it abstract, so it carries the one field that distinguishes
     * the three registrations rather than a stand-in tier that would be wrong for two of them.
     */
    public static final com.mojang.serialization.MapCodec<FurnaceBlock> CODEC =
            com.mojang.serialization.Codec.STRING
                    .xmap(FurnaceTier::valueOf, FurnaceTier::name)
                    .fieldOf("tier")
                    .xmap(FurnaceBlock::new, FurnaceBlock::tier);

    private final FurnaceTier tier;

    public FurnaceBlock(FurnaceTier tier) {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .strength(3.5F)
                .requiresCorrectToolForDrops()
                .sound(SoundType.STONE)
                .lightLevel(state -> state.getValue(LIT) ? 13 : 0));
        this.tier = tier;
        registerDefaultState(getStateDefinition().any()
                .setValue(FACING, net.minecraft.core.Direction.NORTH)
                .setValue(LIT, false));
    }

    public FurnaceTier tier() {
        return tier;
    }

    @Override
    protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
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
        return new FurnaceBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, PFBlockEntities.FURNACE.get(),
                (tickLevel, pos, tickState, entity) -> entity.serverTick());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof FurnaceBlockEntity furnace) {
            // The tier travels with the opening packet: the client has no block entity to ask, and
            // the tier is what decides whether the screen has a fuel slot or an energy bar.
            player.openMenu(furnace, buf -> buf.writeEnum(furnace.tier()));
        }
        return InteractionResult.CONSUME;
    }

    /** A broken furnace pays back what it held. Nothing here is a resource sink (ADR-0041). */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof FurnaceBlockEntity furnace) {
            net.minecraft.world.Containers.dropContents(level, pos, furnace);
        }
        super.onRemove(state, level, pos, newState, moved);
    }

}
