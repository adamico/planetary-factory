package com.planetaryfactory.core.fluid.client;

import com.planetaryfactory.core.fluid.PFFluidTypes;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/**
 * The client half of Terra's two steam fluids: what texture and tint a tank or a pipe renders them
 * with (#223, ADR-0048).
 *
 * <p><b>This is the trap the ticket named.</b> Registering a {@link net.neoforged.neoforge.fluids.FluidType}
 * with no client extension does not fail the build -- it ships a fluid that is invisible in a tank,
 * or renders as the black-and-magenta missing-texture checker if something downstream assumes one
 * exists. There is no in-repo precedent for this seam, so it is wired explicitly here rather than
 * folded into {@link PFFluidTypes} itself, and {@code PlanetaryFactoryCore} calls
 * {@link #register(IEventBus)} only inside its {@code Dist.CLIENT} branch -- registering it
 * unconditionally would pull client-only classes onto a dedicated server's classpath.
 *
 * <p>Both fluids reuse vanilla's own water still/flow textures, tinted rather than redrawn -- the
 * same reuse-by-reference the Offshore Pump makes of vanilla's block textures, and the bucket items'
 * models make of vanilla's bucket icons. Steam keeps close to water's own pale tint; Superheated
 * Steam is tinted toward the orange end, so the two read as visibly different fluids in a tank
 * without either needing a hand-drawn texture.
 */
public final class SteamFluidClient {

    private static final ResourceLocation WATER_STILL =
            ResourceLocation.withDefaultNamespace("block/water_still");
    private static final ResourceLocation WATER_FLOW =
            ResourceLocation.withDefaultNamespace("block/water_flow");

    /** A pale, slightly translucent grey-blue -- steam rather than water, but visibly kin to it. */
    private static final int STEAM_TINT = 0xB0DCE6EC;

    /** An orange-red tint on the same water texture, reading as hot rather than merely wet. */
    private static final int SUPERHEATED_STEAM_TINT = 0xC0FF8A3D;

    private SteamFluidClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(SteamFluidClient::registerExtensions);
    }

    private static void registerExtensions(RegisterClientExtensionsEvent event) {
        event.registerFluidType(new SteamExtensions(STEAM_TINT), PFFluidTypes.STEAM.get());
        event.registerFluidType(
                new SteamExtensions(SUPERHEATED_STEAM_TINT), PFFluidTypes.SUPERHEATED_STEAM.get());
    }

    private static final class SteamExtensions implements IClientFluidTypeExtensions {
        private final int tint;

        private SteamExtensions(int tint) {
            this.tint = tint;
        }

        @Override
        public ResourceLocation getStillTexture() {
            return WATER_STILL;
        }

        @Override
        public ResourceLocation getFlowingTexture() {
            return WATER_FLOW;
        }

        @Override
        public int getTintColor() {
            return tint;
        }
    }
}
