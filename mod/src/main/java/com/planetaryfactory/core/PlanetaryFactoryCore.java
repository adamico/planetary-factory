package com.planetaryfactory.core;

import com.planetaryfactory.core.worldgen.PFWorldgen;
import com.planetaryfactory.core.worldgen.TerraStartingArea;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

/**
 * The pack's first-party mod (ADR-0014).
 *
 * <p>Its remit is the mechanism, never the content (ADR-0015). What lives here is what no
 * scripting API in the pack reaches: right now that is {@link net.minecraft.world.level.block.SaplingBlock}
 * backed by a {@link net.minecraft.world.level.block.grower.TreeGrower}. Every value a designer would
 * tune -- tree shape, drop counts, growth chance, display names, models, textures -- is data in the
 * pack, not a constant in this jar.
 *
 * <p>Note the deliberate split between the mod id and the registry namespace: this mod is
 * {@code planetaryfactory_core}, and it registers into {@code planetaryfactory} alongside KubeJS.
 */
@Mod(PlanetaryFactoryCore.MOD_ID)
public final class PlanetaryFactoryCore {
    public static final String MOD_ID = "planetaryfactory_core";

    /** The shared registry namespace. Not the mod id. See ADR-0014. */
    public static final String NAMESPACE = "planetaryfactory";

    public PlanetaryFactoryCore(IEventBus modBus) {
        PFBlocks.register(modBus);
        PFDataComponents.register(modBus);
        PFItems.register(modBus);
        PFBlockEntities.register(modBus);
        PFWorldgen.register(modBus);
        modBus.addListener(PFItems::addToCreativeTabs);
        modBus.addListener(PFBlockEntities::registerCapabilities);
        modBus.addListener(PFItems::registerCapabilities);
        // Game bus, not the mod bus: this one fires per running server, not per mod load.
        NeoForge.EVENT_BUS.addListener(TerraStartingArea::onServerStarted);
    }
}
