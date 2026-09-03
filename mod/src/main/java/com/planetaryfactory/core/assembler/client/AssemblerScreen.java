package com.planetaryfactory.core.assembler.client;

import net.minecraft.client.gui.GuiGraphics;
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

    /** What this particular screen puts on the panel. */
    protected abstract void renderPanel(GuiGraphics graphics, int mouseX, int mouseY);

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
