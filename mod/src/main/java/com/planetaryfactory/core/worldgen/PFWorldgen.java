package com.planetaryfactory.core.worldgen;

import com.planetaryfactory.core.PlanetaryFactoryCore;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The worldgen pieces the pack's data refers to by id.
 *
 * <p>Only one so far: the processor that puts Terra's starting patches on the ground rather than on
 * the canopy. It is registered here rather than in the datapack because a processor is code --
 * ADR-0015's line -- and named here rather than inline because
 * `worldgen/processor_list/terra_start_ground.json` has to be able to name it.
 */
public final class PFWorldgen {
    public static final DeferredRegister<StructureProcessorType<?>> PROCESSORS =
            DeferredRegister.create(Registries.STRUCTURE_PROCESSOR, PlanetaryFactoryCore.NAMESPACE);

    public static final DeferredHolder<StructureProcessorType<?>, StructureProcessorType<GroundProcessor>>
            GROUND_PROCESSOR = PROCESSORS.register("ground", () -> () -> GroundProcessor.CODEC);

    private PFWorldgen() {
    }

    public static void register(IEventBus modBus) {
        PROCESSORS.register(modBus);
    }
}
