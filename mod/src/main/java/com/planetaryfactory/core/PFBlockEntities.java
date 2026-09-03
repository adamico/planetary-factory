package com.planetaryfactory.core;

import com.planetaryfactory.core.energy.SupplyAreaPoleBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Block entities, which currently means the supply-area pole and nothing else.
 *
 * <p>All four pole tiers share one {@link BlockEntityType}: they differ in supply area and in
 * nothing else, so there is one behaviour and four blocks pointing at it.
 */
public final class PFBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, PlanetaryFactoryCore.NAMESPACE);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SupplyAreaPoleBlockEntity>>
            SUPPLY_AREA_POLE = BLOCK_ENTITIES.register("supply_area_pole",
                    () -> new BlockEntityType<>(
                            SupplyAreaPoleBlockEntity::new,
                            PFBlocks.poleBlocks(),
                            // No data fixer. The pack is pre-release and carries no world forward,
                            // which is the standing position rather than an oversight here.
                            null));

    private PFBlockEntities() {
    }

    static void register(IEventBus modBus) {
        BLOCK_ENTITIES.register(modBus);
    }

    /**
     * The pole's FE face, exposed on every tier.
     *
     * <p>This is the entire V-to-machine boundary. Power Grid's own bridge block feeds it, no mod
     * internals are touched on either side, and there is no separate placeable converter -- which
     * is ADR-0036's arrangement, and the only interop path either grid mod's author supports.
     */
    static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                SUPPLY_AREA_POLE.get(),
                (pole, side) -> pole.feSide());
    }
}
