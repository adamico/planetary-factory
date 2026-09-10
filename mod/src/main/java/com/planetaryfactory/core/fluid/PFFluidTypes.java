package com.planetaryfactory.core.fluid;

import com.planetaryfactory.core.PlanetaryFactoryCore;

import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * The {@link FluidType}s behind Terra's two pack-owned steam fluids (#223, ADR-0048).
 *
 * <p>Two fluids rather than one carrying a temperature, because Factorio has exactly two
 * temperatures with exactly two consumers -- see the ADR. Both are {@code planetaryfactory:}, never
 * {@code gtceu:steam}: GregTech's own steam machines accept its material, and admitting it would
 * re-open the power layer #37 removed.
 *
 * <p>{@link #temperature(int)} converts ADR-0048's own Celsius figures (165 °C, 500 °C) to the
 * Kelvin-flavoured scale {@link FluidType.Properties#temperature(int)} documents -- a read of the
 * ADR, not a new number invented here. Every other property (density, viscosity, light level) is
 * flavour: nothing in the chain reads them back, and nothing here claims they are derived from
 * anything.
 */
public final class PFFluidTypes {
    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, PlanetaryFactoryCore.NAMESPACE);

    /** What the Boiler makes and the Steam Engine eats. Factorio's 165 °C. */
    public static final DeferredHolder<FluidType, FluidType> STEAM = FLUID_TYPES.register(
            "steam",
            () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("fluid_type." + PlanetaryFactoryCore.NAMESPACE + ".steam")
                    .lightLevel(0)
                    .density(-10)
                    .temperature(temperature(165))
                    .viscosity(800)
                    .rarity(Rarity.COMMON)));

    /**
     * What ADR-0033's reactor emits and only the Steam Turbine takes. Factorio's 500 °C.
     *
     * <p>Registered with no producer and no consumer, deliberately: the reactor is #135 and the
     * Turbine is ADR-0033. Nothing here or anywhere else reaches for either.
     */
    public static final DeferredHolder<FluidType, FluidType> SUPERHEATED_STEAM = FLUID_TYPES.register(
            "superheated_steam",
            () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("fluid_type." + PlanetaryFactoryCore.NAMESPACE + ".superheated_steam")
                    .lightLevel(4)
                    .density(-15)
                    .temperature(temperature(500))
                    .viscosity(600)
                    .rarity(Rarity.COMMON)));

    private PFFluidTypes() {
    }

    /** Celsius to {@link FluidType}'s Kelvin-flavoured scale. Documentation, not physics. */
    private static int temperature(int celsius) {
        return celsius + 273;
    }

    public static void register(IEventBus modBus) {
        FLUID_TYPES.register(modBus);
    }
}
