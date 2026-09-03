package com.planetaryfactory.core.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A pole is a column: a base where the footprint belongs, and extensions above it (ADR-0036).
 *
 * <h2>Why the pole has a height at all</h2>
 *
 * <p>The supply area used to be anchored to the pole block, which meant a player who mounted a pole
 * on a stack of fences -- the obvious way to get a wire attachment point into the air, and what
 * Power Grid's catenary invites -- lifted the supply area off the ground with it. The fault was not
 * that {@link PoleTier#VERTICAL_RADIUS} was too small. It was that the pole block's height is a
 * <em>wiring</em> decision while the supply area is a <em>ground</em> concept, and the two were
 * sharing one coordinate. Giving the pole its own height separates them: the column reaches up to
 * the wire, and the area stays measured at the base.
 *
 * <h2>Being an extension is derived, never stored</h2>
 *
 * <p>There is no {@code part} blockstate property. A segment is an extension if and only if the
 * block below it is the same block -- and since each tier is its own block, that also settles "of
 * the same tier" without asking. Nothing is persisted, so no saved state can disagree with the
 * world it is in.
 *
 * <p>The consequence is that two separate poles of the same tier cannot stand directly on top of
 * one another, because stacking <em>is</em> extension. That is the right reading of the arrangement
 * rather than a limitation of it.
 *
 * <h2>Only the base's block entity means anything</h2>
 *
 * <p>Minecraft builds a block entity from the blockstate alone, with no view of the block below, so
 * an extension gets one whether or not it has any use for it. The column's rule is therefore that
 * only the base's is ticked or consulted: {@link SupplyAreaPoleBlock#getTicker} gates on
 * {@link #isBase}, and the capability provider resolves through {@link #baseOf}. An extension's
 * block entity holds an empty ledger that nothing reads.
 *
 * <p>One consequence is worth knowing rather than defending against: placing a pole directly
 * <em>beneath</em> a standing one turns the standing one into an extension, stranding whatever its
 * ledger had buffered. That is at most one tick of FE, and the alternative -- refusing the
 * placement, or migrating the buffer -- costs more understanding than the energy is worth.
 */
public final class PoleColumn {

    /**
     * How many blocks tall a pole may be, base included.
     *
     * <p>An ergonomics number, not a performance one. Extensions do not tick, so height is free;
     * what is not free is a pole whose top a player cannot reach. Everything the height is <em>for</em>
     * happens up there -- placing Power Grid's Device Connector on the top segment, then clicking it
     * to run wire -- and both are bounded by reach. Five puts the top segment's face at {@code y+4},
     * within reach of a player standing on the ground the base sits on, so a pole is workable
     * without building scaffolding beside it and tearing it down after.
     *
     * <p>Read that as the rule, not the number: <em>the whole pole is workable from the ground it
     * stands on</em>. Five is what that evaluates to.
     */
    public static final int MAX_SEGMENTS = 5;

    private PoleColumn() {
    }

    /** Whether these two positions hold the same pole block, which is also the same tier. */
    private static boolean sameBlock(BlockGetter level, BlockPos pos, Block block) {
        return level.getBlockState(pos).is(block);
    }

    /**
     * The base of the column this position belongs to, or {@code null} if it is not a pole.
     *
     * <p>Walks down while the block below is the same pole. Bounded by {@link #MAX_SEGMENTS}: a
     * column taller than the cap cannot be built through the block's own interactions, and a walk
     * that runs past the cap is answering about a structure this class does not recognise, so it
     * stops rather than following it to bedrock.
     */
    public static BlockPos baseOf(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof SupplyAreaPoleBlock)) {
            return null;
        }
        Block block = state.getBlock();
        BlockPos base = pos;
        for (int i = 1; i < MAX_SEGMENTS; i++) {
            BlockPos below = base.below();
            if (!sameBlock(level, below, block)) {
                return base;
            }
            base = below;
        }
        return base;
    }

    /** Whether this position is the bottom of its column -- the block the supply area is measured at. */
    public static boolean isBase(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof SupplyAreaPoleBlock
                && !sameBlock(level, pos.below(), state.getBlock());
    }

    /**
     * The topmost segment of the column containing this position, or {@code null} if it is not a
     * pole. This is where an extension lands.
     */
    public static BlockPos topOf(BlockGetter level, BlockPos pos) {
        BlockPos base = baseOf(level, pos);
        if (base == null) {
            return null;
        }
        Block block = level.getBlockState(base).getBlock();
        BlockPos top = base;
        for (int i = 1; i < MAX_SEGMENTS; i++) {
            BlockPos above = top.above();
            if (!sameBlock(level, above, block)) {
                return top;
            }
            top = above;
        }
        return top;
    }

    /** How many segments the column containing this position has, or {@code 0} if it is not a pole. */
    public static int height(BlockGetter level, BlockPos pos) {
        BlockPos base = baseOf(level, pos);
        if (base == null) {
            return 0;
        }
        BlockPos top = topOf(level, base);
        return top.getY() - base.getY() + 1;
    }
}
