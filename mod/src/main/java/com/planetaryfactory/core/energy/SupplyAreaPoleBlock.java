package com.planetaryfactory.core.energy;

import com.planetaryfactory.core.PFBlockEntities;
import net.minecraft.core.BlockPos;
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
 * <p>One class serves all four tiers; they differ by the {@link PoleTier} handed to the
 * constructor and by nothing else.
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
            if (be instanceof SupplyAreaPoleBlockEntity pole) {
                pole.serverTick();
            }
        };
    }
}
