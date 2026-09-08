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

    /** A one-pixel frame, so the bar reads as a gauge on the panel rather than as a painted patch. */
    private static final int ENERGY_BORDER = 0xFF373737;

    /** Vanilla's container panel grey, which is what the fuel slot is painted out with. */
    private static final int PANEL = 0xFFC6C6C6;

    public FurnaceScreen(FurnaceMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    /**
     * The EU bar, in screen-relative coordinates, so the draw and the hover cannot drift apart.
     *
     * <p>Top right, filling left to right. It reads as a supply the machine is drawing from rather
     * than as a fuel item burning down, which is the distinction the tier is: a burner's flame
     * empties and has to be refilled by hand, while a buffer is a level a pole holds up.
     */
    /** The flame, which vanilla's background puts above the fuel slot. */
    private static final int FLAME_X = 56;
    private static final int FLAME_Y = 36;
    private static final int FLAME_SIZE = 14;

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

        if (menu.tier().burnsFuel()) {
            int flame = Math.round(menu.fuelLeft() * 13F);
            if (flame > 0) {
                graphics.blitSprite(LIT_PROGRESS, FLAME_SIZE, FLAME_SIZE, 0, FLAME_SIZE - flame,
                        left + FLAME_X, top + FLAME_Y + FLAME_SIZE - flame, FLAME_SIZE, flame);
            }
        } else {
            // The background is vanilla's, so it draws a fuel slot the Electric tier's menu never
            // adds -- a recess a player can click, drop onto and get nothing back from, which
            // reads as a slot that is broken rather than one that is absent. Painting it out is
            // what makes "this tier has no fuel slot" visible instead of merely true.
            graphics.fill(left + 55, top + 52, left + 73, top + 70, PANEL);

            int filled = Math.round(menu.energyLevel() * BAR_WIDTH);
            int barLeft = left + BAR_X;
            int barTop = top + BAR_Y;
            graphics.fill(barLeft - 1, barTop - 1, barLeft + BAR_WIDTH + 1, barTop + BAR_HEIGHT + 1,
                    ENERGY_BORDER);
            graphics.fill(barLeft, barTop, barLeft + BAR_WIDTH, barTop + BAR_HEIGHT, ENERGY_EMPTY);
            if (filled > 0) {
                graphics.fill(barLeft, barTop, barLeft + filled, barTop + BAR_HEIGHT, ENERGY_FULL);
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
        if (!menu.tier().burnsFuel() && over(mouseX, mouseY, BAR_X, BAR_Y, BAR_WIDTH, BAR_HEIGHT)) {
            graphics.renderComponentTooltip(font, energyTooltip(), mouseX, mouseY);
        }
        if (menu.tier().burnsFuel()
                && over(mouseX, mouseY, FLAME_X, FLAME_Y, FLAME_SIZE, FLAME_SIZE)) {
            graphics.renderComponentTooltip(font, fuelTooltip(), mouseX, mouseY);
        }
    }

    private boolean over(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= leftPos + x && mouseX < leftPos + x + width
                && mouseY >= topPos + y && mouseY < topPos + y + height;
    }

    /**
     * What is left of the lit fuel item, the way the EU bar reads its buffer -- the two tiers ask
     * the same question of the same place on the screen.
     *
     * <p>Ticks and not only seconds: fuel is spent one tick per tick of operation at the same rate
     * on both burners, so the tick count is directly how many more ticks of smelting this item
     * pays for, and the Steel tier getting twice the items out of it is visible as the same number
     * against a halved duration. The number itself is still Minecraft's burn value rather than
     * Factorio's fuel value (#185); this displays it honestly, it does not fix it.
     */
    private List<Component> fuelTooltip() {
        int ticks = menu.fuelTicks();
        if (ticks <= 0) {
            return List.of(Component.translatable("tooltip.planetaryfactory.furnace.fuel.out")
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
        }
        return List.of(
                Component.translatable("tooltip.planetaryfactory.furnace.fuel", ticks),
                Component.translatable("tooltip.planetaryfactory.furnace.fuel.seconds",
                                String.format("%.1f", ticks / 20F))
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
