package com.planetaryfactory.core.mining.rig.client;

import com.planetaryfactory.core.mining.rig.RigMenu;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * The mining rig's screen (#193): a fuel gauge and a progress arrow, in the shape the furnace
 * ladder already set.
 *
 * <p>It draws on vanilla's furnace background for the same reason {@code FurnaceScreen} does --
 * a fuel slot, an output slot and a bar is a furnace screen whatever the block is, and reusing the
 * sheet keeps the mod's assets to the one thing that is genuinely different. The input recess
 * vanilla draws is painted out on every rig, because a rig's input is the ground under it and a
 * clickable slot that swallows nothing reads as broken rather than as absent.
 */
public class RigScreen extends AbstractContainerScreen<RigMenu> {

    private static final ResourceLocation BACKGROUND =
            ResourceLocation.withDefaultNamespace("textures/gui/container/furnace.png");
    private static final ResourceLocation BURN_PROGRESS =
            ResourceLocation.withDefaultNamespace("container/furnace/burn_progress");

    private static final int FUEL_FULL = 0xFFFFD84D;
    private static final int FUEL_EMPTY = 0xFF3A3A3A;
    private static final int FUEL_BORDER = 0xFF373737;
    private static final int PANEL = 0xFFC6C6C6;

    /** The gauge, screen-relative, so the draw and the hover cannot drift apart. */
    private static final int BAR_X = 106;
    private static final int BAR_Y = 16;
    private static final int BAR_WIDTH = 62;
    private static final int BAR_HEIGHT = 8;

    public RigScreen(RigMenu menu, Inventory playerInventory, Component title) {
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

        // Vanilla's sheet draws an input recess at 55,16 and a fuel recess at 55,52. A rig has no
        // input -- it mines the ground -- so that one always goes, and the fuel one goes too on
        // the electric rig, whose menu never adds the slot (#194).
        graphics.fill(left + 55, top + 16, left + 73, top + 34, PANEL);
        if (!menu.burnsFuel()) {
            graphics.fill(left + 55, top + 52, left + 73, top + 70, PANEL);
        }

        if (menu.burnsFuel()) {
            int filled = Math.round(menu.fuelLevel() * BAR_WIDTH);
            int barLeft = left + BAR_X;
            int barTop = top + BAR_Y;
            graphics.fill(barLeft - 1, barTop - 1, barLeft + BAR_WIDTH + 1, barTop + BAR_HEIGHT + 1,
                    FUEL_BORDER);
            graphics.fill(barLeft, barTop, barLeft + BAR_WIDTH, barTop + BAR_HEIGHT, FUEL_EMPTY);
            if (filled > 0) {
                graphics.fill(barLeft, barTop, barLeft + filled, barTop + BAR_HEIGHT, FUEL_FULL);
            }
        }

        int arrow = Math.round(menu.miningProgress() * 24F);
        if (arrow > 0) {
            graphics.blitSprite(BURN_PROGRESS, 24, 16, 0, 0, left + 79, top + 34, arrow, 16);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (menu.burnsFuel() && over(mouseX, mouseY, BAR_X, BAR_Y, BAR_WIDTH, BAR_HEIGHT)) {
            graphics.renderComponentTooltip(font, fuelTooltip(), mouseX, mouseY);
        }
    }

    private boolean over(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= leftPos + x && mouseX < leftPos + x + width
                && mouseY >= topPos + y && mouseY < topPos + y + height;
    }

    /**
     * What is in the fuel buffer, in the furnace's own shape: what is held, and what it costs to
     * run. The seconds are what make joules legible, and they are the rig's own -- 7,500 J a tick
     * off 150 kW, against the furnace's 4,500 off 90.
     */
    private List<Component> fuelTooltip() {
        int joules = menu.fuelStored();
        if (joules <= 0) {
            return List.of(Component.translatable("tooltip.planetaryfactory.rig.fuel.out")
                    .withStyle(ChatFormatting.GRAY));
        }
        long perTick = menu.joulesPerTick();
        return List.of(
                Component.translatable("tooltip.planetaryfactory.rig.fuel", joules, menu.fuelCapacity()),
                Component.translatable("tooltip.planetaryfactory.rig.fuel.seconds",
                                String.format("%.1f", joules / (perTick * 20F)), perTick)
                        .withStyle(ChatFormatting.GRAY));
    }
}
