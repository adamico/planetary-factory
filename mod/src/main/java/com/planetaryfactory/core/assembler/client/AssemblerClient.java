package com.planetaryfactory.core.assembler.client;

import com.planetaryfactory.core.PFMenus;
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
        NeoForge.EVENT_BUS.addListener(InventoryAssemblerTab::onScreenInit);
        NeoForge.EVENT_BUS.addListener(InventoryAssemblerTab::onScreenRender);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(PFMenus.ASSEMBLER_PANEL.get(), AssemblerPanelScreen::new);
        event.register(PFMenus.SELECT_AMOUNT.get(), SelectAmountScreen::new);
        event.register(PFMenus.CRAFTING_PLAN.get(), CraftingPlanScreen::new);
    }
}
