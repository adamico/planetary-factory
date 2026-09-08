package com.planetaryfactory.core.smelting.client;

import com.planetaryfactory.core.smelting.PFFuel;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * What an item is worth in a burner furnace, on its own tooltip (ADR-0047).
 *
 * <p>Fuel is default-deny here, so "does this burn" is a question about a table a player cannot
 * see: vanilla's intuitions are wrong in both directions -- planks and blaze rods do not burn, and
 * a log does. The tooltip is where the table stops being invisible, and it says nothing at all on
 * an item with no row, which is the answer to the other half of the question.
 *
 * <p>Two lines, because joules alone are not legible. The seconds are the tier-independent half:
 * both burners spend 4,500 J a tick, so the same item is the same number of seconds of furnace
 * work on Stone and on Steel -- what the Steel tier buys is twice the crafts inside them, which is
 * the furnace's story to tell and not the item's.
 */
public final class FuelTooltip {

    private FuelTooltip() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(FuelTooltip::onTooltip);
    }

    private static void onTooltip(ItemTooltipEvent event) {
        long joules = PFFuel.joules(event.getItemStack());
        if (joules <= 0L) {
            return;
        }
        long perTick = PFFuel.joulesPerTick();
        event.getToolTip().add(Component.translatable("tooltip.planetaryfactory.fuel.joules",
                        String.format("%,d", joules))
                .withStyle(ChatFormatting.GRAY));
        event.getToolTip().add(Component.translatable("tooltip.planetaryfactory.fuel.burn",
                        String.format("%.1f", joules / (perTick * 20F)))
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
