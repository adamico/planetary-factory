package com.planetaryfactory.core.fluid;

import com.planetaryfactory.core.PlanetaryFactoryCore;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.block.SoundType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Terra's two pack-owned steam fluids, made real: still, flowing and a block each
 * (#223, ADR-0048).
 *
 * <p>Two registries, kept together because the three objects for one fluid are mutually
 * referential -- the fluid needs the block to convert to on placement, the block needs the fluid's
 * source, and both are resolved lazily through {@link DeferredHolder#get()}, never eagerly.
 * Vanilla's own {@code FLUID} and {@code BLOCK} registries fire their {@code RegisterEvent} in that
 * declared order (see {@code net.minecraft.core.registries.BuiltInRegistries}), which is what makes
 * it safe for the block suppliers below to call {@code .get()} on a fluid holder from inside their
 * own registration lambda.
 *
 * <p><b>Neither fluid has a bucket, deliberately.</b> ADR-0037 already answered portable fluid for
 * this pack: {@code planetaryfactory:barrel}, which takes any fluid at Factorio's own 50 mB. That
 * ADR states the capacity as a rule rather than as a fact about one item -- a portable container
 * holds the Factorio number under the converter's 1:1 unit rule, and "does not get to be re-argued
 * from Minecraft's bucket". A 1 000 mB bucket of steam is exactly the twentyfold dose it rejects,
 * and it would hand the player a hand-carry route around the Boiler-pipe-Engine chain that rung 0
 * exists to teach. A {@link net.neoforged.neoforge.fluids.FluidType} needs no bucket to register.
 *
 * <p>No model or blockstate JSON is generated for either liquid block: {@link LiquidBlock}s are not
 * rendered from one -- {@link com.planetaryfactory.core.fluid.client.SteamFluidClient} supplies the
 * still/flowing textures directly through {@code IClientFluidTypeExtensions}, the same seam GregTech
 * and every other fluid mod uses.
 */
public final class PFFluids {
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(Registries.FLUID, PlanetaryFactoryCore.NAMESPACE);
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(PlanetaryFactoryCore.NAMESPACE);

    // ---- Steam ----------------------------------------------------------------------------

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> STEAM_SOURCE =
            FLUIDS.register("steam", () -> new BaseFlowingFluid.Source(steamProperties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> STEAM_FLOWING =
            FLUIDS.register("flowing_steam", () -> new BaseFlowingFluid.Flowing(steamProperties()));

    public static final DeferredHolder<Block, PFLiquidBlock> STEAM_BLOCK =
            BLOCKS.register("steam", () -> new PFLiquidBlock(STEAM_SOURCE.get(), liquidProperties()));


    // ---- Superheated Steam ------------------------------------------------------------------

    /**
     * ADR-0048: no producer and no consumer, on purpose. The reactor is #135, the Turbine is
     * ADR-0033; registering the fluid now is what stops the second consumer being retrofitted
     * into a mechanism that assumed one.
     */
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> SUPERHEATED_STEAM_SOURCE =
            FLUIDS.register(
                    "superheated_steam", () -> new BaseFlowingFluid.Source(superheatedSteamProperties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> SUPERHEATED_STEAM_FLOWING =
            FLUIDS.register(
                    "flowing_superheated_steam",
                    () -> new BaseFlowingFluid.Flowing(superheatedSteamProperties()));

    public static final DeferredHolder<Block, PFLiquidBlock> SUPERHEATED_STEAM_BLOCK = BLOCKS.register(
            "superheated_steam",
            () -> new PFLiquidBlock(SUPERHEATED_STEAM_SOURCE.get(), liquidProperties()));


    private PFFluids() {
    }

    private static BaseFlowingFluid.Properties steamProperties() {
        return new BaseFlowingFluid.Properties(PFFluidTypes.STEAM, STEAM_SOURCE, STEAM_FLOWING)
                .block(STEAM_BLOCK);
    }

    private static BaseFlowingFluid.Properties superheatedSteamProperties() {
        return new BaseFlowingFluid.Properties(
                PFFluidTypes.SUPERHEATED_STEAM, SUPERHEATED_STEAM_SOURCE, SUPERHEATED_STEAM_FLOWING)
                .block(SUPERHEATED_STEAM_BLOCK);
    }

    /** Vanilla's own water/lava shape: replaceable, no collision, no loot table of its own. */
    private static BlockBehaviour.Properties liquidProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.WATER)
                .replaceable()
                .noCollission()
                .strength(100.0F)
                .pushReaction(PushReaction.DESTROY)
                .noLootTable()
                .liquid()
                .sound(SoundType.EMPTY);
    }

    public static void register(IEventBus modBus) {
        FLUIDS.register(modBus);
        BLOCKS.register(modBus);
    }

}
