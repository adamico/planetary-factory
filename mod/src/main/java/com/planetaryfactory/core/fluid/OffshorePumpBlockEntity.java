package com.planetaryfactory.core.fluid;

import com.planetaryfactory.core.PFBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * The pump running (#213, ADR-0050): 60 mB of water into a small buffer every tick, for whatever
 * pipe cares to take it.
 *
 * <p><b>The buffer holds nothing between ticks that matters.</b> It exists because a fluid
 * capability needs somewhere to hand fluid out of, not as storage -- it is one tick's production
 * over, so a pump left unconnected does not silently accumulate a tank's worth of water to dump the
 * moment a pipe arrives. Water is unlimited at the source anyway; what a buffer would add is a
 * burst, and a burst is a thing a player would have to plan around.
 *
 * <p><b>Nothing here checks the water is still there.</b> The site was settled at placement
 * ({@link OffshorePumpItem}) and is not re-tested, which is Factorio's own bargain and safe under
 * ADR-0050 for the reason {@link OffshorePumpSiting} states.
 *
 * <p>No energy. {@link PumpCorpus#takesPower()} reads Factorio's {@code void} energy source, so the
 * absence of a power connection is a read fact rather than something nobody wired up.
 */
public class OffshorePumpBlockEntity extends BlockEntity {

    /**
     * One tick's production. Not a tank: a pump that banks water while unconnected would deliver a
     * burst on connection, which is a behaviour a player would have to learn.
     */
    private final FluidTank buffer = new FluidTank(PumpCorpus.get().milliBucketsPerTick()) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    public OffshorePumpBlockEntity(BlockPos pos, BlockState state) {
        super(PFBlockEntities.OFFSHORE_PUMP.get(), pos, state);
    }

    /**
     * Refill to the tick's worth. Filling rather than setting, so a pipe that took only part of
     * last tick's water is topped up rather than handed a second full measure -- the rate is
     * 60 mB a tick delivered, not 60 mB a tick offered.
     */
    public void serverTick() {
        int room = buffer.getSpace();
        if (room > 0) {
            buffer.fill(new FluidStack(Fluids.WATER, room), IFluidHandler.FluidAction.EXECUTE);
        }
    }

    /**
     * Extract-only. A pump is an origin, and letting something push fluid back into it would make
     * it a pipe junction that happens to make water.
     */
    public IFluidHandler fluidHandler() {
        return new IFluidHandler() {
            @Override
            public int getTanks() {
                return buffer.getTanks();
            }

            @Override
            public FluidStack getFluidInTank(int tank) {
                return buffer.getFluidInTank(tank);
            }

            @Override
            public int getTankCapacity(int tank) {
                return buffer.getTankCapacity(tank);
            }

            @Override
            public boolean isFluidValid(int tank, FluidStack stack) {
                return false;
            }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
                return 0;
            }

            @Override
            public FluidStack drain(FluidStack resource, FluidAction action) {
                return buffer.drain(resource, action);
            }

            @Override
            public FluidStack drain(int maxDrain, FluidAction action) {
                return buffer.drain(maxDrain, action);
            }
        };
    }
}
