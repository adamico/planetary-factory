package com.planetaryfactory.core.assembler.client;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * An item id as the client draws it.
 *
 * <p>The plan and the queue travel as strings, because the resolver and the queue count ids and
 * never learn what an {@code Item} is -- which is what keeps both of them unit-testable with no
 * Minecraft at all. Turning one back into a sprite or a name is a client-side job, and this is the
 * one place it happens: the three screens and the HUD would otherwise each have their own answer for
 * an id nothing is registered under.
 */
final class PlanItems {

    private PlanItems() {}

    /** The item's display name, or the raw id when nothing is registered under it. */
    static Component name(String id) {
        Item item = item(id);
        return item == null ? Component.literal(id) : item.getDescription();
    }

    /**
     * One stack of the item, for drawing and for the vanilla tooltip.
     *
     * <p>Count one, not the plan's: the number beside the icon is the plan's, and a stack count
     * painted into the corner of the sprite would say it twice, differently, the moment a plan asks
     * for more than a stack.
     */
    static ItemStack stack(String id) {
        Item item = item(id);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    private static Item item(String id) {
        ResourceLocation key = ResourceLocation.tryParse(id);
        return key == null ? null : BuiltInRegistries.ITEM.get(key);
    }
}
