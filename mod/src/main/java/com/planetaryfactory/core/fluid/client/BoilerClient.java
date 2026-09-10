package com.planetaryfactory.core.fluid.client;

import com.planetaryfactory.core.PFMenus;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * The client half of the Boiler (#224): one screen for one menu.
 *
 * <p>Called only on the client, from {@code PlanetaryFactoryCore}.
 */
public final class BoilerClient {

    private BoilerClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(BoilerClient::registerScreens);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(PFMenus.BOILER.get(), BoilerScreen::new);
    }
}
