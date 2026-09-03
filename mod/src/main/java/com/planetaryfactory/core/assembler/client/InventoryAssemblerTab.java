package com.planetaryfactory.core.assembler.client;

import com.planetaryfactory.core.network.OpenPanelPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Personal Assembler's tab on the vanilla inventory screen.
 *
 * <p>Present from the first tick and with no item to obtain, because the Assembler is the player's
 * only hand-crafting surface once the crafting grid goes (#90, #95) -- a tab that had to be unlocked
 * would be a world with no way to craft anything in it.
 *
 * <p>Added as a listener on {@code Init.Post} and drawn on {@code Render.Post}, which is two events
 * for one button because they are the halves NeoForge exposes: {@code addListener} reaches the
 * screen's children, where clicks are dispatched, and nothing public reaches its renderables. The
 * button is a real {@link Button} either way, so hit-testing, sounds and narration are vanilla's.
 */
public final class InventoryAssemblerTab {

    private static Button tab;

    private InventoryAssemblerTab() {
    }

    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen screen)) {
            tab = null;
            return;
        }
        tab = Button.builder(
                        Component.translatable("planetaryfactory_core.assembler.tab"),
                        button -> PacketDistributor.sendToServer(new OpenPanelPacket()))
                .bounds(screen.getGuiLeft(), screen.getGuiTop() - 22, 62, 20)
                .tooltip(Tooltip.create(Component.translatable("planetaryfactory_core.assembler.tab.tooltip")))
                .build();
        event.addListener(tab);
    }

    public static void onScreenRender(ScreenEvent.Render.Post event) {
        if (tab == null || !(event.getScreen() instanceof InventoryScreen)) return;
        GuiGraphics graphics = event.getGuiGraphics();
        tab.render(graphics, event.getMouseX(), event.getMouseY(), event.getPartialTick());
    }
}
