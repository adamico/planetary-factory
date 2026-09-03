package com.planetaryfactory.core;

import com.planetaryfactory.core.energy.PoleTier;
import com.planetaryfactory.core.energy.SupplyAreaPoleItem;
import com.planetaryfactory.core.fluid.BarrelFluidHandler;
import com.planetaryfactory.core.fluid.BarrelSpec;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
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
 *
 * <p>The barrel is the exception: an item with no block behind it, and the only thing here that is a
 * mechanism rather than a way to hold a block. It is Factorio's barrel (ADR-0037), and it exists in
 * this jar rather than in KubeJS because a fluid capability is not something any scripting API in the
 * pack reaches.
 */
public final class PFItems {
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(PlanetaryFactoryCore.NAMESPACE);

    private static final List<DeferredHolder<Item, ? extends Item>> NATURAL = new ArrayList<>();
    private static final List<DeferredHolder<Item, ? extends Item>> FUNCTIONAL = new ArrayList<>();

    /**
     * Factorio's barrel: 50 mB, stacking to ten, holding any fluid.
     *
     * <p>Both numbers are Factorio's and neither is tunable here -- see {@link BarrelSpec}. Filling
     * and emptying are Create's Spout and Item Drain, natively and with no recipes at all, because
     * both key on the fluid capability this item carries (#93).
     */
    public static final DeferredHolder<Item, Item> BARREL = ITEMS.registerSimpleItem(
            "barrel", new Item.Properties().stacksTo(BarrelSpec.STACK_SIZE));

    static {
        NATURAL.add(ITEMS.registerSimpleBlockItem(PFBlocks.YUMAKO_SAPLING));
        NATURAL.add(ITEMS.registerSimpleBlockItem(PFBlocks.JELLYSTEM_SAPLING));
        for (PoleTier tier : PoleTier.values()) {
            // Not registerSimpleBlockItem: the pole carries the only description of itself the
            // pack has, so its item is a SupplyAreaPoleItem for the tooltip alone.
            FUNCTIONAL.add(ITEMS.register(PFBlocks.pole(tier).getId().getPath(),
                    () -> new SupplyAreaPoleItem(PFBlocks.pole(tier).get(), new Item.Properties())));
        }
        FUNCTIONAL.add(BARREL);
    }

    private PFItems() {
    }

    static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }

    /**
     * The barrel's fluid face.
     *
     * <p>Registered against the item rather than built into it, which is how an {@code ItemStack}
     * capability works in NeoForge: the handler is constructed per stack, over the component that
     * stack carries.
     */
    static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerItem(
                Capabilities.FluidHandler.ITEM,
                (stack, context) -> new BarrelFluidHandler(stack),
                BARREL.get());
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
