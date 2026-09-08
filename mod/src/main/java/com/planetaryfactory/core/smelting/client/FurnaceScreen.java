package com.planetaryfactory.core.smelting.client;

import com.planetaryfactory.core.smelting.FurnaceMenu;

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

    public FurnaceScreen(FurnaceMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

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
            // The energy bar stands where the flame would be, so the two tiers read the same way.
            int height = Math.round(menu.energyLevel() * 14F);
            graphics.fill(left + 57, top + 37, left + 69, top + 51, ENERGY_EMPTY);
            if (height > 0) {
                graphics.fill(left + 57, top + 51 - height, left + 69, top + 51, ENERGY_FULL);
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
    }
}
