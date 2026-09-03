package com.planetaryfactory.core;

import com.planetaryfactory.core.energy.PoleColumn;
import com.planetaryfactory.core.energy.PoleTier;
import com.planetaryfactory.core.energy.SupplyAreaPoleBlockEntity;
import net.minecraft.core.BlockPos;
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
     * The pole's FE face, exposed on every segment of every tier.
     *
     * <p>This is the entire V-to-machine boundary. Power Grid's own bridge block feeds it, no mod
     * internals are touched on either side, and there is no separate placeable converter -- which
     * is ADR-0036's arrangement, and the only interop path either grid mod's author supports.
     *
     * <p>Registered against the <em>block</em> rather than the block entity type, because a pole is
     * a column and a connector may be attached to any segment of it. Power Grid's
     * {@code BridgeElectricBehaviourImpl.makeFEHandler} does a plain
     * {@code level.getCapability(Capabilities.EnergyStorage.BLOCK, pos.relative(facing), ...)} and
     * never asks whether the target has a block entity, so an extension answering the lookup is
     * ordinary rather than a trick.
     *
     * <p>What comes back is the <em>base's own</em> {@link
     * com.planetaryfactory.core.energy.PoleEnergyStorage}. Nothing is transported up or down the
     * column: a segment is an address, not a conduit, and once the lookup has resolved the segments
     * are not in the path at all. A segment with no base below it -- an orphan mid-collapse -- has
     * no storage to name, so it answers null and the connector treats it as not connected.
     */
    static void registerCapabilities(RegisterCapabilitiesEvent event) {
        for (PoleTier tier : PoleTier.values()) {
            event.registerBlock(
                    Capabilities.EnergyStorage.BLOCK,
                    (level, pos, state, blockEntity, side) -> {
                        BlockPos base = PoleColumn.baseOf(level, pos);
                        if (base == null) {
                            return null;
                        }
                        return level.getBlockEntity(base)
                                instanceof SupplyAreaPoleBlockEntity pole ? pole.feSide() : null;
                    },
                    PFBlocks.pole(tier).get());
        }
    }
}
