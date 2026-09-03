package com.planetaryfactory.core;

import com.planetaryfactory.core.energy.PoleTier;
import com.planetaryfactory.core.energy.SupplyAreaPoleItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;

/**
 * Block items for what {@link PFBlocks} registers.
 *
 * <p>The saplings need one so they can be held, planted by hand and placed by a Create Deployer
 * through the normal use-on path. The poles need one to be placed at all.
 */
public final class PFItems {
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(PlanetaryFactoryCore.NAMESPACE);

    private static final List<DeferredHolder<Item, ? extends Item>> NATURAL = new ArrayList<>();
    private static final List<DeferredHolder<Item, ? extends Item>> FUNCTIONAL = new ArrayList<>();

    static {
        NATURAL.add(ITEMS.registerSimpleBlockItem(PFBlocks.YUMAKO_SAPLING));
        NATURAL.add(ITEMS.registerSimpleBlockItem(PFBlocks.JELLYSTEM_SAPLING));
        for (PoleTier tier : PoleTier.values()) {
            // Not registerSimpleBlockItem: the pole carries the only description of itself the
            // pack has, so its item is a SupplyAreaPoleItem for the tooltip alone.
            FUNCTIONAL.add(ITEMS.register(PFBlocks.pole(tier).getId().getPath(),
                    () -> new SupplyAreaPoleItem(PFBlocks.pole(tier).get(), new Item.Properties())));
        }
    }

    private PFItems() {
    }

    static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }

    static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        // A pole is not a natural block. It goes where a player already looks for something that
        // moves power around, next to the rest of the machinery.
        if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
            NATURAL.forEach(entry -> event.accept(entry.get()));
        } else if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            FUNCTIONAL.forEach(entry -> event.accept(entry.get()));
        }
    }
}
