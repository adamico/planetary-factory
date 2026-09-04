package com.planetaryfactory.core.assembler.client;

import com.planetaryfactory.core.PFMenus;
import com.planetaryfactory.core.PlanetaryFactoryCore;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * The client half of the Personal Assembler: which screen each of the three menus opens, and the tab
 * that reaches the first of them.
 *
 * <p>Called only on the client, from {@code PlanetaryFactoryCore}, so nothing here is loaded on a
 * dedicated server.
 */
public final class AssemblerClient {

    private AssemblerClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(AssemblerClient::registerScreens);
        modBus.addListener(AssemblerClient::registerHud);
        modBus.addListener(AssemblerKeys::register);
        NeoForge.EVENT_BUS.addListener(InventoryAssemblerTab::onScreenInit);
        NeoForge.EVENT_BUS.addListener(InventoryAssemblerTab::onScreenRender);
        NeoForge.EVENT_BUS.addListener(AssemblerKeys::onClientTick);
    }

    /** Above the hotbar in draw order, so the queue is not painted under it. */
    private static void registerHud(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR,
                ResourceLocation.fromNamespaceAndPath(PlanetaryFactoryCore.NAMESPACE, "assembler_queue"),
                new AssemblerHud());
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(PFMenus.ASSEMBLER_PANEL.get(), AssemblerPanelScreen::new);
        event.register(PFMenus.SELECT_AMOUNT.get(), SelectAmountScreen::new);
        event.register(PFMenus.CRAFTING_PLAN.get(), CraftingPlanScreen::new);
    }
}
