package com.planetaryfactory.core;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * The two saplings, and nothing else.
 *
 * <p>Everything else the trees are made of -- logs, leaves, stems, fruit -- is registered by
 * {@code kubejs/startup_scripts/blocks.js}, into this same namespace. The boundary is ADR-0015;
 * a comment at the top of that file restates it, because that is the file someone will have open
 * when they are about to collide with these two ids.
 */
public final class PFBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(PlanetaryFactoryCore.NAMESPACE);

    public static final DeferredHolder<Block, SaplingBlock> YUMAKO_SAPLING =
            sapling("yumako_sapling", PFTrees.YUMAKO);
    public static final DeferredHolder<Block, SaplingBlock> JELLYSTEM_SAPLING =
            sapling("jellystem_sapling", PFTrees.JELLYSTEM);

    private PFBlocks() {
    }

    static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
    }

    private static DeferredHolder<Block, SaplingBlock> sapling(String name, TreeGrower grower) {
        return BLOCKS.register(name, () -> new SaplingBlock(
                grower,
                BlockBehaviour.Properties.of()
                        .mapColor(net.minecraft.world.level.material.MapColor.PLANT)
                        .noCollission()
                        .randomTicks()
                        .instabreak()
                        .sound(SoundType.GRASS)
                        .pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY)));
    }
}
