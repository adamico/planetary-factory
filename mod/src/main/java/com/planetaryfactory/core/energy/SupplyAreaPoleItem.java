package com.planetaryfactory.core.energy;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * The pole in the hand, and the only thing in the pack that says what a pole does.
 *
 * <h2>Why a pole ships its own explanation</h2>
 *
 * <p>Every other way a player learns a machine is missing here. There is no cable to trace, so the
 * shape of the network cannot be read off the world. There is no GUI, so nothing can be inspected.
 * The recipe says nothing about reach. A pole's supply area is invisible by construction -- that
 * invisibility is the mechanic -- which leaves a block whose entire behaviour has to be taken on
 * trust unless it is stated somewhere.
 *
 * <p>So it is stated here, in three lines, always shown rather than hidden behind Shift. This is
 * not detail a player goes looking for; it is the block's basic contract, and a tooltip nobody
 * opens teaches nobody.
 *
 * <p>The live half -- how many machines are actually in the area, and whether they are being fed --
 * cannot come from an item and belongs to the Jade line instead.
 */
public class SupplyAreaPoleItem extends BlockItem {

    public SupplyAreaPoleItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    private PoleTier tier() {
        return ((SupplyAreaPoleBlock) getBlock()).tier();
    }

    @Override
    public void appendHoverText(net.minecraft.world.item.ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        PoleTier tier = tier();

        // What it covers. Stated as Factorio states it -- a square of tiles -- plus the vertical
        // band, which is the dimension Factorio has no answer for and a player cannot guess.
        tooltip.add(Component.translatable("tooltip.planetaryfactory.pole.area",
                tier.supplySize(), tier.supplySize(), tier.verticalRadius())
                .withStyle(ChatFormatting.GRAY));

        // That it is wireless. The load-bearing line: a Minecraft player who is not told this will
        // go looking for the cable, fail to find one, and conclude the pole is broken.
        tooltip.add(Component.translatable("tooltip.planetaryfactory.pole.wireless")
                .withStyle(ChatFormatting.GRAY));

        // That height does not move the area, and how to add height. One line for both, because
        // they are the same fact from two sides: the column exists so the wire can go up, and the
        // footprint stays on the ground while it does. Saying only the first would replace an
        // invisible bug with an invisible rule.
        tooltip.add(Component.translatable("tooltip.planetaryfactory.pole.column",
                Component.translatable(getBlock().getDescriptionId()), PoleColumn.MAX_SEGMENTS)
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
