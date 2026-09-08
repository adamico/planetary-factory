package com.planetaryfactory.core.mining.rig.client;

import com.planetaryfactory.core.PFMenus;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * The client half of the two mining rigs (#193): one screen for one tier-aware menu.
 *
 * <p>Called only on the client, from {@code PlanetaryFactoryCore}.
 */
public final class RigClient {

    private RigClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(RigClient::registerScreens);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(PFMenus.RIG.get(), RigScreen::new);
    }
}
