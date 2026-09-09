package com.planetaryfactory.core.fluid;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.planetaryfactory.core.PFBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
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
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.MapColor;

/**
 * Factorio's Offshore Pump (#213, ADR-0050): where water enters the factory, and the only place it
 * can.
 *
 * <p>This class is the shell. The rule it enforces lives in {@link OffshorePumpSiting} and the rate
 * in {@link OffshorePumpSpec}, neither of which touches Minecraft, which is what lets both be
 * asserted without a world. What is here is the translation between the two: turning real
 * neighbouring blocks into the three cases the predicate understands.
 *
 * <p><b>Sited at placement, not maintained afterwards.</b> {@link OffshorePumpItem} refuses to place
 * where no source adjoins; this block does not then re-check every tick, and a player who drains the
 * pond a pump stands on gets a pump that produces nothing. That is the same bargain Factorio makes,
 * and it is safe here for a reason ADR-0050 spells out: water cannot be created, so a pump can never
 * be talked into a site that was invalid to begin with.
 *
 * <p>Facing is cosmetic. The predicate looks at every neighbour, so a pump works
 * whichever way it points; the orientation exists so the player can see which side is against the
 * water.
 */
public class OffshorePumpBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    /**
     * A block codec is only read by data generation, which this pack does not run -- present
     * because {@link BaseEntityBlock} makes it abstract, the same reason {@code RigBlock} carries
     * one.
     */
    public static final com.mojang.serialization.MapCodec<OffshorePumpBlock> CODEC =
            simpleCodec(properties -> new OffshorePumpBlock());

    public OffshorePumpBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(3.5F)
                .requiresCorrectToolForDrops()
                .sound(SoundType.METAL));
        registerDefaultState(getStateDefinition().any().setValue(FACING, Direction.NORTH));
    }

    /**
     * What surrounds a position, in the terms {@link OffshorePumpSiting} understands.
     *
     * <p>All six faces, which is ADR-0050's "one adjacent water block" taken literally. An earlier
     * pass restricted this to the four horizontal ones, reasoning that a pump standing on a puddle
     * reads as levitating machinery -- but that is a narrowing neither #213 nor the ADR asked for,
     * and a pump sunk into a pond with water above it is a perfectly ordinary thing to build.
     *
     * <p>{@code FluidState.isSource} is the whole test. It is enough here only because nothing in
     * the pack can create a source -- see {@link OffshorePumpSiting} for why that, and not any
     * property of this method, is what makes it sound.
     */
    public static List<OffshorePumpSiting.Neighbour> neighboursOf(BlockGetter level, BlockPos pos) {
        List<OffshorePumpSiting.Neighbour> neighbours = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            FluidState fluid = level.getFluidState(pos.relative(direction));
            if (fluid.isEmpty()) {
                neighbours.add(OffshorePumpSiting.Neighbour.DRY);
            } else if (fluid.isSource()) {
                neighbours.add(OffshorePumpSiting.Neighbour.SOURCE);
            } else {
                neighbours.add(OffshorePumpSiting.Neighbour.FLOWING);
            }
        }
        return neighbours;
    }

    /** Whether a pump may stand here. {@link OffshorePumpItem} is what asks. */
    public static boolean canSit(BlockGetter level, BlockPos pos) {
        return OffshorePumpSiting.accepts(neighboursOf(level, pos));
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
        return new OffshorePumpBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, PFBlockEntities.OFFSHORE_PUMP.get(),
                (tickLevel, pos, tickState, entity) -> entity.serverTick());
    }
}
