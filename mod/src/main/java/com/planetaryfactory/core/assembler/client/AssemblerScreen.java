package com.planetaryfactory.core.assembler.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * What the Assembler's three screens share: a flat panel, a title, and no texture.
 *
 * <p>Drawn from fills rather than from a sprite sheet because the jar's only asset is its lang file
 * (see {@code mod/README.md}) -- and because what the Assembler looks like is #161's to settle, so a
 * texture written now would be a texture drawn twice.
 */
abstract class AssemblerScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {

    protected static final int PANEL = 0xFF2B2B2B;
    protected static final int ROW = 0xFF3C3C3C;
    private static final int SLOT = 0xFF1A1A1A;
    private static final int SLOT_EDGE = 0xFF4A4A4A;

    protected AssemblerScreen(T menu, Inventory inventory, Component title, int width, int height) {
        super(menu, inventory, title);
        this.imageWidth = width;
        this.imageHeight = height;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, PANEL);
        renderPanel(graphics, mouseX, mouseY);
    }

    /**
     * An item id as the player reads it.
     *
     * <p>The plan travels as ids, because the queue and the resolver count strings and never learn
     * what an {@code Item} is. Turning one back into a name is a client-side job, and this is the one
     * place the three screens do it -- {@code gtceu:iron_plate} in a dialog would be the resolver's
     * internals leaking onto the screen.
     */
    protected static Component itemName(String id) {
        ResourceLocation key = ResourceLocation.tryParse(id);
        Item item = key == null ? null : BuiltInRegistries.ITEM.get(key);
        return item == null ? Component.literal(id) : item.getDescription();
    }

    /**
     * An item id as one stack of it, for drawing and for the vanilla tooltip.
     *
     * <p>Count one, not the plan's: the number beside the icon is the plan's and a stack count
     * painted into the corner of the sprite would say it twice, differently, once a plan asks for
     * more than a stack.
     */
    protected static ItemStack itemStack(String id) {
        ResourceLocation key = ResourceLocation.tryParse(id);
        Item item = key == null ? null : BuiltInRegistries.ITEM.get(key);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    /** What this particular screen puts on the panel. */
    protected abstract void renderPanel(GuiGraphics graphics, int mouseX, int mouseY);

    /**
     * A well for every slot the menu carries.
     *
     * <p>{@code AbstractContainerScreen} draws what is <em>in</em> a slot and nothing else -- the
     * empty grid is part of the background texture in vanilla. With no texture, an empty inventory
     * renders as blank panel, so the wells are drawn here from the menu's own slot positions rather
     * than from a second copy of the layout.
     */
    protected void renderSlots(GuiGraphics graphics) {
        for (net.minecraft.world.inventory.Slot slot : menu.slots) {
            int x = leftPos + slot.x;
            int y = topPos + slot.y;
            graphics.fill(x - 1, y - 1, x + 17, y + 17, SLOT_EDGE);
            graphics.fill(x, y, x + 16, y + 16, SLOT);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    /** The title only. The inherited second label names a player inventory these screens draw themselves. */
    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, 0xFFFFFF, false);
    }
}
