package com.planetaryfactory.core.fluid.client;

import java.util.List;

import com.planetaryfactory.core.fluid.BoilerMenu;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * The Boiler's screen (#224): a fuel gauge and two tank gauges, in the shape the furnace ladder set.
 *
 * <p>It draws on vanilla's furnace background for the reason {@code RigScreen} does -- a fuel slot
 * and a bar is a furnace screen whatever the block is -- and paints out the input recess, because
 * a Boiler's other input is a pipe and a clickable slot that swallows nothing reads as broken
 * rather than as absent.
 *
 * <p>The two tank bars are the point of the screen. A stopped Boiler is out of fuel, out of water
 * or backed up, and from outside the block those three are the same still machine.
 */
public class BoilerScreen extends AbstractContainerScreen<BoilerMenu> {

    private static final ResourceLocation BACKGROUND =
            ResourceLocation.withDefaultNamespace("textures/gui/container/furnace.png");

    private static final int FUEL_FULL = 0xFFFFD84D;
    private static final int WATER_FULL = 0xFF3B6FE0;
    private static final int STEAM_FULL = 0xFFD8E4EC;
    private static final int EMPTY = 0xFF3A3A3A;
    private static final int BORDER = 0xFF373737;
    private static final int PANEL = 0xFFC6C6C6;

    private static final int BAR_X = 106;
    private static final int BAR_WIDTH = 62;
    private static final int BAR_HEIGHT = 8;
    private static final int FUEL_Y = 16;
    private static final int WATER_Y = 32;
    private static final int STEAM_Y = 48;

    public BoilerScreen(BoilerMenu menu, Inventory playerInventory, Component title) {
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

        // Vanilla's sheet draws an input recess at 55,16 and an output one at 112,30. The Boiler
        // has neither: water arrives through a pipe and steam leaves through one.
        graphics.fill(left + 55, top + 16, left + 73, top + 34, PANEL);
        graphics.fill(left + 111, top + 29, left + 137, top + 55, PANEL);

        bar(graphics, FUEL_Y, menu.fuelLevel(), FUEL_FULL);
        bar(graphics, WATER_Y, level(menu.water(), menu.waterCapacity()), WATER_FULL);
        bar(graphics, STEAM_Y, level(menu.steam(), menu.steamCapacity()), STEAM_FULL);
    }

    private static float level(int amount, int capacity) {
        return capacity <= 0 ? 0F : Math.min(1F, amount / (float) capacity);
    }

    private void bar(GuiGraphics graphics, int y, float level, int colour) {
        int barLeft = leftPos + BAR_X;
        int barTop = topPos + y;
        int filled = Math.round(level * BAR_WIDTH);
        graphics.fill(barLeft - 1, barTop - 1, barLeft + BAR_WIDTH + 1, barTop + BAR_HEIGHT + 1, BORDER);
        graphics.fill(barLeft, barTop, barLeft + BAR_WIDTH, barTop + BAR_HEIGHT, EMPTY);
        if (filled > 0) {
            graphics.fill(barLeft, barTop, barLeft + filled, barTop + BAR_HEIGHT, colour);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (over(mouseX, mouseY, FUEL_Y)) {
            graphics.renderComponentTooltip(font, fuelTooltip(), mouseX, mouseY);
        } else if (over(mouseX, mouseY, WATER_Y)) {
            graphics.renderComponentTooltip(font, List.of(Component.translatable(
                    "tooltip.planetaryfactory.boiler.water", menu.water(), menu.waterCapacity())),
                    mouseX, mouseY);
        } else if (over(mouseX, mouseY, STEAM_Y)) {
            graphics.renderComponentTooltip(font, List.of(Component.translatable(
                    "tooltip.planetaryfactory.boiler.steam", menu.steam(), menu.steamCapacity())),
                    mouseX, mouseY);
        }
    }

    private boolean over(int mouseX, int mouseY, int y) {
        return mouseX >= leftPos + BAR_X && mouseX < leftPos + BAR_X + BAR_WIDTH
                && mouseY >= topPos + y && mouseY < topPos + y + BAR_HEIGHT;
    }

    /** What is in the fuel buffer, in the ladder's own shape: what is held, and what it costs. */
    private List<Component> fuelTooltip() {
        int joules = menu.fuelStored();
        if (joules <= 0) {
            return List.of(Component.translatable("tooltip.planetaryfactory.boiler.fuel.out")
                    .withStyle(ChatFormatting.GRAY));
        }
        long perTick = menu.joulesPerTick();
        return List.of(
                Component.translatable("tooltip.planetaryfactory.boiler.fuel", joules,
                        menu.fuelCapacity()),
                Component.translatable("tooltip.planetaryfactory.boiler.fuel.seconds",
                                String.format("%.1f", joules / (perTick * 20F)), perTick)
                        .withStyle(ChatFormatting.GRAY));
    }
}
