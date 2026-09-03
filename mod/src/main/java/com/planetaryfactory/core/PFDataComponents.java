package com.planetaryfactory.core;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Data components, which currently means the one the barrel carries its fluid in.
 *
 * <p>NeoForge supplies the content type and both codecs; what it does not supply is a registered
 * component to hang them on, so every mod with a fluid-holding item registers its own. This is that
 * one item's, and it is the whole reason a single barrel can stand in for Factorio's nine distinct
 * filled-barrel items (#93).
 */
public final class PFDataComponents {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, PlanetaryFactoryCore.NAMESPACE);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SimpleFluidContent>>
            FLUID_CONTENT = DATA_COMPONENTS.register("fluid_content",
                    () -> DataComponentType.<SimpleFluidContent>builder()
                            .persistent(SimpleFluidContent.CODEC)
                            .networkSynchronized(SimpleFluidContent.STREAM_CODEC)
                            .build());

    private PFDataComponents() {
    }

    static void register(IEventBus modBus) {
        DATA_COMPONENTS.register(modBus);
    }
}
