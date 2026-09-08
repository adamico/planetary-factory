package com.planetaryfactory.core;

import com.gregtechceu.gtceu.api.capability.GTCapability;
import com.planetaryfactory.core.energy.PoleColumn;
import com.planetaryfactory.core.energy.PoleTier;
import com.planetaryfactory.core.energy.SupplyAreaPoleBlockEntity;
import com.planetaryfactory.core.mining.rig.RigBlockEntity;
import com.planetaryfactory.core.mining.rig.RigItemHandler;
import com.planetaryfactory.core.mining.rig.RigPartBlockEntity;
import com.planetaryfactory.core.mining.rig.RigTier;
import com.planetaryfactory.core.smelting.FurnaceBlockEntity;
import com.planetaryfactory.core.smelting.FurnaceItemHandler;
import com.planetaryfactory.core.smelting.FurnaceTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Block entities: the supply-area pole and the furnace ladder.
 *
 * <p>All four pole tiers share one {@link BlockEntityType}, and so do all three furnace tiers:
 * each set differs in numbers its tier enum carries and in nothing else, so there is one behaviour
 * and several blocks pointing at it.
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

    /**
     * All three furnace tiers share one type (#155). They differ in speed and in where their
     * energy comes from, both of which are on {@link FurnaceTier}, so there is one behaviour and
     * three blocks pointing at it -- the same arrangement as the pole above.
     */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FurnaceBlockEntity>>
            FURNACE = BLOCK_ENTITIES.register("furnace",
                    () -> new BlockEntityType<>(FurnaceBlockEntity::new, PFBlocks.furnaceBlocks(),
                            null));

    /**
     * Both rigs' anchors share one type (#192, ADR-0043), the same arrangement as the pole and
     * furnace above. It carries no fields yet -- #192 is an inert footprint -- so both tiers are
     * genuinely identical here; #193/#194 are what will need the tier on this entity.
     */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RigBlockEntity>>
            RIG = BLOCK_ENTITIES.register("rig",
                    () -> new BlockEntityType<>(RigBlockEntity::new, PFBlocks.rigBlocks(), null));

    /** Both rigs' parts share one type; each part's only field is its anchor's position. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RigPartBlockEntity>>
            RIG_PART = BLOCK_ENTITIES.register("rig_part",
                    () -> new BlockEntityType<>(RigPartBlockEntity::new, PFBlocks.rigPartBlocks(), null));

    private PFBlockEntities() {
    }

    static void register(IEventBus modBus) {
        BLOCK_ENTITIES.register(modBus);
    }

    static void registerCapabilities(RegisterCapabilitiesEvent event) {
        registerPoleCapabilities(event);
        registerFurnaceCapabilities(event);
        registerRigCapabilities(event);
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
    private static void registerPoleCapabilities(RegisterCapabilitiesEvent event) {
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

    /**
     * The furnace's two faces (#155), both answered on every direction and on the null side.
     *
     * <p><b>The item handler is unsided on purpose.</b> Direction never decides in or out -- the
     * item does, and {@link FurnaceItemHandler} routes it. That is Factorio's arrangement, and it
     * is what makes a Create funnel work on whichever face a player put it on: funnels reach a
     * neighbour through a {@code BlockCapability<IItemHandler, Direction>}, so a nominated-face
     * inventory answers a funnel on any other face with silence and no diagnosis.
     *
     * <p>The energy face is the Electric tier's alone. {@code energySide()} is null on the two
     * burner tiers, so a supply-area pole does not count a Stone Furnace as a machine it is
     * failing to power.
     */
    private static void registerFurnaceCapabilities(RegisterCapabilitiesEvent event) {
        for (FurnaceTier tier : FurnaceTier.values()) {
            Block block = PFBlocks.furnace(tier).get();
            event.registerBlock(
                    Capabilities.ItemHandler.BLOCK,
                    (level, pos, state, blockEntity, side) ->
                            blockEntity instanceof FurnaceBlockEntity furnace
                                    ? new FurnaceItemHandler(furnace) : null,
                    block);
            event.registerBlock(
                    GTCapability.CAPABILITY_ENERGY_CONTAINER,
                    (level, pos, state, blockEntity, side) ->
                            blockEntity instanceof FurnaceBlockEntity furnace
                                    ? furnace.energySide() : null,
                    block);
        }
    }

    /**
     * The rig's item face (#193): fuel in, ore out, on every side and on the null side.
     *
     * <p>Unsided for the reason the furnace's is -- Factorio decides in-or-out by the inserter's
     * direction rather than by the machine's face, and a nominated-face inventory answers a Create
     * funnel on any other face with silence and no diagnosis.
     *
     * <p><b>Registered on the part blocks as well as the anchors.</b> Three quarters of a 2x2 is
     * part, so a hopper under the corner a player happened to build against would otherwise find
     * nothing, and which corner holds the anchor is not visible. The part forwards to its anchor's
     * block entity, the same as its break and its right-click do.
     *
     * <p>No energy face here. The electric rig is a supply-area pole customer under ADR-0036 and
     * gets one with #194; the burner rig never does.
     */
    private static void registerRigCapabilities(RegisterCapabilitiesEvent event) {
        for (RigTier tier : RigTier.values()) {
            event.registerBlock(
                    Capabilities.ItemHandler.BLOCK,
                    (level, pos, state, blockEntity, side) ->
                            blockEntity instanceof RigBlockEntity rig ? new RigItemHandler(rig) : null,
                    PFBlocks.rig(tier).get());
            event.registerBlock(
                    Capabilities.ItemHandler.BLOCK,
                    (level, pos, state, blockEntity, side) -> {
                        if (!(blockEntity instanceof RigPartBlockEntity part)
                                || part.anchorPos() == null
                                || !(level.getBlockEntity(part.anchorPos()) instanceof RigBlockEntity rig)) {
                            return null;
                        }
                        return new RigItemHandler(rig);
                    },
                    PFBlocks.rigPart(tier).get());
        }
    }
}
