package com.planetaryfactory.core.smelting.client;

import com.planetaryfactory.core.smelting.FurnaceMenu;

import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * The furnace screen (#155), tier-aware: a flame under the fuel slot on the burner tiers, and an
 * EU bar in the same place on the Electric one.
 *
 * <p>It draws on vanilla's own furnace background. The pack's furnaces are Factorio's, but the
 * screen a player reads is a furnace screen either way, and reusing the sprite sheet keeps the
 * mod's assets to the one thing that is genuinely different -- the bar.
 */
public class FurnaceScreen extends AbstractContainerScreen<FurnaceMenu> {

    private static final ResourceLocation BACKGROUND =
            ResourceLocation.withDefaultNamespace("textures/gui/container/furnace.png");
    private static final ResourceLocation LIT_PROGRESS =
            ResourceLocation.withDefaultNamespace("container/furnace/lit_progress");
    private static final ResourceLocation BURN_PROGRESS =
            ResourceLocation.withDefaultNamespace("container/furnace/burn_progress");

    /** The EU bar's colours: GregTech's own energy yellow over an empty slate. */
    private static final int ENERGY_FULL = 0xFFFFD84D;
    private static final int ENERGY_EMPTY = 0xFF3A3A3A;

    /** Vanilla's container panel grey, which is what the fuel slot is painted out with. */
    private static final int PANEL = 0xFFC6C6C6;

    public FurnaceScreen(FurnaceMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    /** The EU bar, in screen-relative coordinates, so the draw and the hover cannot drift apart. */
    private static final int BAR_X = 57;
    private static final int BAR_Y = 37;
    private static final int BAR_WIDTH = 12;
    private static final int BAR_HEIGHT = 14;

    @Override
    protected void init() {
        super.init();
        titleLabelX = (imageWidth - font.width(title)) / 2;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int left = leftPos;
        int top = topPos;
        graphics.blit(BACKGROUND, left, top, 0, 0, imageWidth, imageHeight);

        if (menu.tier().burnsFuel()) {
            int flame = Math.round(menu.fuelLeft() * 13F);
            if (flame > 0) {
                graphics.blitSprite(LIT_PROGRESS, 14, 14, 0, 14 - flame,
                        left + 56, top + 36 + 14 - flame, 14, flame);
            }
        } else {
            // The background is vanilla's, so it draws a fuel slot the Electric tier's menu never
            // adds -- a recess a player can click, drop onto and get nothing back from, which
            // reads as a slot that is broken rather than one that is absent. Painting it out is
            // what makes "this tier has no fuel slot" visible instead of merely true.
            graphics.fill(left + 55, top + 52, left + 73, top + 70, PANEL);

            // The energy bar stands where the flame would be, so the two tiers read the same way.
            int height = Math.round(menu.energyLevel() * BAR_HEIGHT);
            int bottom = top + BAR_Y + BAR_HEIGHT;
            graphics.fill(left + BAR_X, top + BAR_Y, left + BAR_X + BAR_WIDTH, bottom, ENERGY_EMPTY);
            if (height > 0) {
                graphics.fill(left + BAR_X, bottom - height, left + BAR_X + BAR_WIDTH, bottom,
                        ENERGY_FULL);
            }
        }

        int arrow = Math.round(menu.smeltProgress() * 24F);
        if (arrow > 0) {
            graphics.blitSprite(BURN_PROGRESS, 24, 16, 0, 0, left + 79, top + 34, arrow, 16);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (!menu.tier().burnsFuel() && overEnergyBar(mouseX, mouseY)) {
            graphics.renderComponentTooltip(font, energyTooltip(), mouseX, mouseY);
        }
    }

    private boolean overEnergyBar(int mouseX, int mouseY) {
        return mouseX >= leftPos + BAR_X && mouseX < leftPos + BAR_X + BAR_WIDTH
                && mouseY >= topPos + BAR_Y && mouseY < topPos + BAR_Y + BAR_HEIGHT;
    }

    /**
     * What GregTech's own machines put on the same hover, in the same order: what is in the buffer,
     * and what it costs to run.
     *
     * <p>The draw rate is the half that makes the buffer legible. 2080 EU means nothing on its own;
     * 2080 EU at 13 EU/t is the length of one steel plate, which is the number the buffer was
     * actually sized to (ADR-0036) -- a pole that goes quiet mid-smelt still finishes the item.
     */
    private List<Component> energyTooltip() {
        return List.of(
                Component.translatable("tooltip.planetaryfactory.furnace.energy",
                        menu.energyStored(), menu.energyCapacity()),
                Component.translatable("tooltip.planetaryfactory.furnace.usage", menu.euPerTick())
                        .withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
