package com.planetaryfactory.core.energy;

import com.planetaryfactory.core.PFBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A Factorio electric pole (ADR-0036): it supplies every machine standing in its area, and there is
 * no network behind it.
 *
 * <p>That absence is the whole design. No graph, no propagation, no merge and split on placement,
 * no topology persisted across chunk unloads -- which is where a cable mod's cost actually lives.
 * A pole is a position, a radius and a tick. What the pack gets for that is Factorio's own
 * mechanic rather than an approximation: place a pole, and everything inside its supply area is
 * powered.
 *
 * <p>One class serves all three tiers; they differ by the {@link PoleTier} handed to the
 * constructor and by nothing else.
 *
 * <h2>The pole is a column</h2>
 *
 * <p>A pole stands as tall as the player builds it, up to {@link PoleColumn#MAX_SEGMENTS}, and the
 * supply area is measured at the base whatever the height. {@link PoleColumn} has the why. This
 * class owns the three world-facing consequences: extending a column, breaking one, and keeping the
 * FE capability answering correctly while either happens.
 */
public class SupplyAreaPoleBlock extends Block implements EntityBlock {

    /** A pole is a post, not a cube -- 6/16 square and full height. */
    private static final VoxelShape SHAPE = Block.box(5.0D, 0.0D, 5.0D, 11.0D, 16.0D, 11.0D);

    private final PoleTier tier;

    public SupplyAreaPoleBlock(PoleTier tier) {
        super(BlockBehaviour.Properties.of()
                .strength(1.5F)
                .sound(SoundType.COPPER)
                // No tool requirement: the pack registers no mining-tool tags for its own blocks,
                // and requiring one here would make a pole break to nothing by hand.
                .noOcclusion());
        this.tier = tier;
    }

    public PoleTier tier() {
        return tier;
    }

    @Override
    protected VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level,
                                  BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /**
     * Right-clicking any segment with this tier's own pole grows the column by one.
     *
     * <p>Clicking <em>any</em> segment rather than only the top is the point: the new block lands on
     * top of the column, so a player standing on the ground raises a pole past their own reach by
     * clicking the base repeatedly. That matters because {@link PoleColumn#MAX_SEGMENTS} is chosen
     * so the top stays reachable -- if extending also demanded reaching the top, the cap would have
     * had to be smaller still.
     *
     * <p>A pole of a <em>different</em> tier does nothing at all, rather than falling through to
     * ordinary placement. Falling through would set a second, separate pole against the side of this
     * column, with its own supply area and its own block entity, and it would look exactly like the
     * extension the player was asking for.
     */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                              BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
        if (!(stack.getItem() instanceof BlockItem item) || !item.getBlock().equals(this)) {
            // Includes the empty hand. Removing the top segment bare-handed would be the natural
            // inverse of this, and is deliberately absent: breaking is already how blocks come off,
            // and a bare-hand interaction that deletes part of a build loses substations to
            // misclicks.
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        BlockPos top = PoleColumn.topOf(level, pos);
        if (top == null || PoleColumn.height(level, pos) >= PoleColumn.MAX_SEGMENTS) {
            return ItemInteractionResult.CONSUME;
        }
        BlockPos next = top.above();
        if (!level.getBlockState(next).canBeReplaced()) {
            return ItemInteractionResult.CONSUME;
        }

        if (level.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }
        level.setBlockAndUpdate(next, defaultBlockState());
        level.playSound(null, next, getSoundType(state, level, next, player).getPlaceSound(),
                net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        invalidateColumn(level, next);
        return ItemInteractionResult.SUCCESS;
    }

    /**
     * Breaking any segment drops the column above it.
     *
     * <p>Chains, scaffolding and Create's belts all do this, so the muscle memory is already there,
     * and the alternative -- leaving segments floating where their base was -- is a lie about a
     * structure the player thinks of as one object. Recursion is via {@code destroyBlock}, which
     * re-enters here for the block above, so the column unwinds one segment at a time.
     */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState,
                            boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockPos above = pos.above();
            if (level.getBlockState(above).is(this)) {
                level.destroyBlock(above, true);
            }
            // Everything that was standing on this base is now standing on nothing, so whatever a
            // connector resolved through it has to be looked up again.
            invalidateColumn(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /**
     * Re-resolve the FE capability for every segment of this column.
     *
     * <p>NeoForge caches block capabilities per position, and invalidates them automatically only
     * where a block entity changes. An extension has no block entity of its own worth reading -- its
     * capability is the <em>base's</em>, handed out by the provider in {@code PFBlockEntities} --
     * so nothing invalidates it when the base appears or goes. Without this, a Device Connector goes
     * on holding a handler into a block entity that is no longer there, and does it silently.
     */
    static void invalidateColumn(Level level, BlockPos pos) {
        if (level.isClientSide()) {
            return;
        }
        BlockPos base = PoleColumn.baseOf(level, pos);
        BlockPos from = base != null ? base : pos;
        for (int i = 0; i < PoleColumn.MAX_SEGMENTS; i++) {
            BlockPos segment = from.above(i);
            level.invalidateCapabilities(segment);
        }
        // The block below a base may have been one a moment ago.
        level.invalidateCapabilities(from.below());
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState,
                           boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        // Placing a pole directly beneath a standing one makes that one an extension, which changes
        // where its capability resolves. See PoleColumn for why the stranded buffer is accepted.
        invalidateColumn(level, pos);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SupplyAreaPoleBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                 BlockEntityType<T> type) {
        // Server only: the pole has nothing to animate, and pushing energy on the client would be
        // pushing it into a copy of the world.
        if (level.isClientSide() || type != PFBlockEntities.SUPPLY_AREA_POLE.get()) {
            return null;
        }
        return (lvl, pos, st, be) -> {
            // Only the base's block entity is ticked. An extension is given one by Minecraft --
            // a block entity is built from the blockstate, which cannot see the block below -- and
            // it stays inert. This is the gate that makes a five-tall pole cost what a one-tall
            // pole costs.
            if (be instanceof SupplyAreaPoleBlockEntity pole && PoleColumn.isBase(lvl, pos)) {
                pole.serverTick();
            }
        };
    }
}
