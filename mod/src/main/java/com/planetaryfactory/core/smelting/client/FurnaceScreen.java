package com.planetaryfactory.core.smelting.client;

import com.planetaryfactory.core.smelting.FurnaceMenu;

import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * The furnace screen (#155): one horizontal gauge, top right, on all three tiers.
 *
 * <p>The flame the burners used to draw is gone with ADR-0047. A burner holds a buffer in joules
 * exactly as the Electric tier holds one in EU, so it gets the same widget; two widgets for one
 * quantity would say the two tiers hold interchangeable stuff. What differs is the refill
 * economy -- a burner's gauge is filled by hand, one item at a time -- and that is legible from
 * the fuel slot beside it rather than from a second kind of picture.
 *
 * <p>It draws on vanilla's own furnace background. The pack's furnaces are Factorio's, but the
 * screen a player reads is a furnace screen either way, and reusing the sprite sheet keeps the
 * mod's assets to the one thing that is genuinely different -- the bar.
 */
public class FurnaceScreen extends AbstractContainerScreen<FurnaceMenu> {

    private static final ResourceLocation BACKGROUND =
            ResourceLocation.withDefaultNamespace("textures/gui/container/furnace.png");
    private static final ResourceLocation BURN_PROGRESS =
            ResourceLocation.withDefaultNamespace("container/furnace/burn_progress");

    /** The gauge's colours: GregTech's own energy yellow over an empty slate. */
    private static final int ENERGY_FULL = 0xFFFFD84D;
    private static final int ENERGY_EMPTY = 0xFF3A3A3A;

    /** A one-pixel frame, so the bar reads as a gauge on the panel rather than as a painted patch. */
    private static final int ENERGY_BORDER = 0xFF373737;

    /** Vanilla's container panel grey, which is what the fuel slot is painted out with. */
    private static final int PANEL = 0xFFC6C6C6;

    public FurnaceScreen(FurnaceMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    /**
     * The gauge, in screen-relative coordinates, so the draw and the hover cannot drift apart.
     * Top right, filling left to right, on every tier.
     */
    private static final int BAR_X = 106;
    private static final int BAR_Y = 16;
    private static final int BAR_WIDTH = 62;
    private static final int BAR_HEIGHT = 8;

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

        if (!menu.tier().burnsFuel()) {
            // The background is vanilla's, so it draws a fuel slot the Electric tier's menu never
            // adds -- a recess a player can click, drop onto and get nothing back from, which
            // reads as a slot that is broken rather than one that is absent. Painting it out is
            // what makes "this tier has no fuel slot" visible instead of merely true.
            graphics.fill(left + 55, top + 52, left + 73, top + 70, PANEL);
        }

        int filled = Math.round(menu.energyLevel() * BAR_WIDTH);
        int barLeft = left + BAR_X;
        int barTop = top + BAR_Y;
        graphics.fill(barLeft - 1, barTop - 1, barLeft + BAR_WIDTH + 1, barTop + BAR_HEIGHT + 1,
                ENERGY_BORDER);
        graphics.fill(barLeft, barTop, barLeft + BAR_WIDTH, barTop + BAR_HEIGHT, ENERGY_EMPTY);
        if (filled > 0) {
            graphics.fill(barLeft, barTop, barLeft + filled, barTop + BAR_HEIGHT, ENERGY_FULL);
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
        if (over(mouseX, mouseY, BAR_X, BAR_Y, BAR_WIDTH, BAR_HEIGHT)) {
            graphics.renderComponentTooltip(font,
                    menu.tier().burnsFuel() ? fuelTooltip() : energyTooltip(), mouseX, mouseY);
        }
    }

    private boolean over(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= leftPos + x && mouseX < leftPos + x + width
                && mouseY >= topPos + y && mouseY < topPos + y + height;
    }

    /**
     * What is in the fuel buffer, in the same shape the Electric tier's hover uses: what is held,
     * and what it costs to run.
     *
     * <p>The seconds are the half that makes joules legible, and they are the tier's own: 4,500 J
     * a tick on both burners, against a craft the Steel tier finishes in half the time. That is
     * ADR-0047's ratio, on the screen, as two numbers a player can divide.
     */
    private List<Component> fuelTooltip() {
        int joules = menu.energyStored();
        if (joules <= 0) {
            return List.of(Component.translatable("tooltip.planetaryfactory.furnace.fuel.out")
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
        }
        long perTick = menu.joulesPerTick();
        return List.of(
                Component.translatable("tooltip.planetaryfactory.furnace.fuel", joules,
                        menu.energyCapacity()),
                Component.translatable("tooltip.planetaryfactory.furnace.fuel.seconds",
                                String.format("%.1f", joules / (perTick * 20F)), perTick)
                        .withStyle(net.minecraft.ChatFormatting.GRAY));
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
