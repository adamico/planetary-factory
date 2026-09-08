package com.planetaryfactory.core.mining.rig;

import javax.annotation.Nullable;

import com.planetaryfactory.core.PFBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, PFBlockEntities.RIG.get(),
                (tickLevel, pos, tickState, entity) -> entity.serverTick());
    }

    /**
     * Plain right-click opens the rig, because it has a fuel slot (ADR-0043).
     *
     * <p>The tier travels with the opening packet: the client has no block entity to ask, and the
     * tier is what decides whether the screen carries a fuel slot at all.
     */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof RigBlockEntity rig) {
            player.openMenu(rig, buf -> buf.writeEnum(rig.tier()));
        }
        return InteractionResult.CONSUME;
    }

    /**
     * Breaking the anchor -- directly, by a player -- drops the item through the ordinary loot
     * table, so this clears the parts around it (#192, bed-and-door) and pays back what it held.
     *
     * <p>The payback is not optional. A rig holds coal and, when its output is blocked, ore it has
     * already drawn out of the ground -- and under ADR-0041 that ore is a finite resource with the
     * ground's own count already decremented for it. Voiding it on a break would make breaking a
     * stalled rig destroy the very thing the stall existed to preserve. Nothing in this pack is a
     * resource sink.
     *
     * <p>This runs on the anchor whichever block the player actually hit: breaking a part calls
     * {@link RigBreaker#teardown}, which removes the anchor, which arrives here.
     */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof RigBlockEntity rig) {
                Containers.dropContents(level, pos, rig);
            }
            RigBreaker.teardown(level, pos, tier, state.getValue(FACING), pos);
        }
        super.onRemove(state, level, pos, newState, moved);
    }
}
