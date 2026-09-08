package com.planetaryfactory.core.smelting.client;

import com.planetaryfactory.core.PFMenus;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * The client half of the three furnaces (#155): one screen for one tier-aware menu.
 *
 * <p>Called only on the client, from {@code PlanetaryFactoryCore}.
 */
public final class FurnaceClient {

    private FurnaceClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(FurnaceClient::registerScreens);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(PFMenus.FURNACE.get(), FurnaceScreen::new);
    }
}
