package com.planetaryfactory.core;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Block items for the two saplings, so they can be held, planted by hand and placed by a Create
 * Deployer through the normal use-on path.
 */
public final class PFItems {
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(PlanetaryFactoryCore.NAMESPACE);

    static {
        ITEMS.registerSimpleBlockItem(PFBlocks.YUMAKO_SAPLING);
        ITEMS.registerSimpleBlockItem(PFBlocks.JELLYSTEM_SAPLING);
    }

    private PFItems() {
    }

    static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }

    static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() != CreativeModeTabs.NATURAL_BLOCKS) {
            return;
        }
        ITEMS.getEntries().forEach(entry -> {
            Item item = entry.get();
            if (item instanceof BlockItem) {
                event.accept(item);
            }
        });
    }
}
