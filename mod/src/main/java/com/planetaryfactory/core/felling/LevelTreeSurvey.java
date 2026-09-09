package com.planetaryfactory.core.felling;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The bridge between {@link TreeShape}'s graph and an actual world.
 *
 * <p>Everything Minecraft-shaped about felling is in this class and in {@link TreeFelling}; the rule
 * itself is neither, which is what lets it be a unit test. Three vanilla facts are read here and
 * nowhere else:
 *
 * <ul>
 *   <li><b>what counts as a log</b> is a block tag, so a later tree is a tag entry rather than a
 *       code path (ADR-0051);
 *   <li><b>{@link LeavesBlock#DISTANCE}</b> is how far a leaf is from the nearest log, which is the
 *       only thing that keeps the fill out of a touching canopy;
 *   <li><b>{@link LeavesBlock#PERSISTENT}</b> is vanilla's own bit for "a player placed this", so
 *       its negation is the closest thing the game has to "this grew here".
 * </ul>
 *
 * <p>A leaf block from another mod that does not carry those two properties reads as distance 1 and
 * placed -- so it is felled when it touches a log and never treated as evidence the thing grew.
 * That is the safe direction of both defaults: it will not eat a canopy it cannot reason about, and
 * it will not decide somebody's build is a tree.
 */
public final class LevelTreeSurvey implements TreeSurvey {

    private final BlockGetter level;

    public LevelTreeSurvey(BlockGetter level) {
        this.level = level;
    }

    /** The world position a graph position stands for. */
    public static BlockPos toBlockPos(FellPos pos) {
        return new BlockPos(pos.x(), pos.y(), pos.z());
    }

    public static FellPos toFellPos(BlockPos pos) {
        return new FellPos(pos.getX(), pos.getY(), pos.getZ());
    }

    private BlockState at(FellPos pos) {
        return level.getBlockState(toBlockPos(pos));
    }

    @Override
    public boolean isLog(FellPos pos) {
        return at(pos).is(TreeFelling.FELLABLE);
    }

    @Override
    public boolean isLeaf(FellPos pos) {
        return at(pos).is(BlockTags.LEAVES);
    }

    @Override
    public int leafDistance(FellPos pos) {
        BlockState state = at(pos);
        return state.hasProperty(LeavesBlock.DISTANCE) ? state.getValue(LeavesBlock.DISTANCE) : 1;
    }

    @Override
    public boolean isNaturalLeaf(FellPos pos) {
        BlockState state = at(pos);
        return state.hasProperty(LeavesBlock.PERSISTENT) && !state.getValue(LeavesBlock.PERSISTENT);
    }
}
