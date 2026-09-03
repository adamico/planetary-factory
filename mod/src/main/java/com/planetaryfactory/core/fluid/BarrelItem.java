package com.planetaryfactory.core.fluid;

import com.planetaryfactory.core.PFDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;

import java.util.List;

/**
 * The barrel's name and tooltip, which is the whole of what makes a filled and an empty barrel
 * distinguishable in the inventory. Nothing about {@link BarrelSpec} or {@link BarrelFluidHandler}
 * needed this -- the capability round-trips correctly either way, and the model and texture are the
 * same for both. What was missing is what the player reads before they pick one out of a stack.
 */
public class BarrelItem extends Item {

    public BarrelItem(Properties properties) {
        super(properties);
    }

    private FluidStack fluid(ItemStack stack) {
        return stack.getOrDefault(PFDataComponents.FLUID_CONTENT, SimpleFluidContent.EMPTY).copy();
    }

    @Override
    public Component getName(ItemStack stack) {
        FluidStack held = fluid(stack);
        if (held.isEmpty()) {
            return super.getName(stack);
        }
        // "Crude Oil Barrel", matching how the corpus and the item map already name the nine
        // filled-barrel rows (`crude-oil-barrel`, and so on) -- the same pairing, read in the order
        // a player expects a container's name in.
        return Component.translatable("item.planetaryfactory.barrel.filled", held.getHoverName());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        FluidStack held = fluid(stack);
        if (held.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.planetaryfactory.barrel.empty")
                    .withStyle(ChatFormatting.GRAY));
        } else {
            // The capacity is fixed (ADR-0037), so there is only ever one number to show, not a
            // bar: how much of the 50 mB is here. A partial fill is a normal state, not an error --
            // BarrelSpec.fillable accepts less than a full barrel -- so it has to read normally too.
            tooltip.add(Component.translatable("tooltip.planetaryfactory.barrel.amount",
                    held.getAmount(), BarrelSpec.CAPACITY_MB)
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
