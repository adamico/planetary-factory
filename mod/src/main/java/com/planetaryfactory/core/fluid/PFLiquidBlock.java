package com.planetaryfactory.core.fluid;

import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;

/**
 * Vanilla's {@link LiquidBlock} with its constructor opened up.
 *
 * <p>{@code LiquidBlock}'s own constructor is {@code protected} -- vanilla expects a subclass, the
 * way {@code Blocks.WATER} is a plain {@code LiquidBlock} constructed from inside vanilla's own
 * package. This is that subclass, shared by both of Terra's steam fluids rather than written twice.
 */
public final class PFLiquidBlock extends LiquidBlock {
    public PFLiquidBlock(FlowingFluid fluid, BlockBehaviour.Properties properties) {
        super(fluid, properties);
    }
}
