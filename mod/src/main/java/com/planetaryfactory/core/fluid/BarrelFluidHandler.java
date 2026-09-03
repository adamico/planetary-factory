package com.planetaryfactory.core.fluid;

import com.planetaryfactory.core.PFDataComponents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidHandlerItemStack;

/**
 * The barrel's fluid capability: NeoForge's component-backed handler, with one guard added.
 *
 * <p><b>No fluid filter.</b> Factorio bars steam and most Space Age fluids from barrels, and ADR-0037
 * declines to port that: Factorio's list is a content budget for nine items and eighteen recipes,
 * where this pack has one container and none. A filter would have to track the corpus forever, and
 * its failure mode is a barrel that silently refuses a fluid the pack expects it to carry.
 *
 * <p><b>The guard is against duplication, not against fluids.</b> {@link FluidHandlerItemStack} writes
 * the fluid component onto whatever stack it was handed, without looking at the count -- so filling a
 * held stack of ten would set fifty millibuckets on all ten at the cost of fifty. Create never does
 * this: {@code GenericItemFilling} and {@code GenericItemEmptying} both copy the stack and
 * {@code setCount(1)} before touching the capability, which is what makes a stack size above one safe
 * in the first place. This refuses the case anyway, because the barrel is stackable and some other
 * mod's automation is free to be less careful.
 */
public final class BarrelFluidHandler extends FluidHandlerItemStack {
    public BarrelFluidHandler(ItemStack container) {
        super(PFDataComponents.FLUID_CONTENT, container, BarrelSpec.CAPACITY_MB);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        return single() ? super.fill(resource, action) : 0;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        return single() ? super.drain(resource, action) : FluidStack.EMPTY;
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        return single() ? super.drain(maxDrain, action) : FluidStack.EMPTY;
    }

    private boolean single() {
        return container.getCount() == 1;
    }
}
