package com.planetaryfactory.core;

import com.planetaryfactory.core.energy.PoleTier;
import com.planetaryfactory.core.ore.OreBlock;
import com.planetaryfactory.core.ore.OreResource;
import com.planetaryfactory.core.energy.SupplyAreaPoleBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The blocks the mod itself registers: the two saplings and the four supply-area poles.
 *
 * <p>The four supply-area poles are here too (ADR-0036). They are mechanism -- a block entity
 * that scans and pushes energy -- so ADR-0015 puts them in the mod rather than in KubeJS, while
 * their models, textures and names stay data in the pack like everything else.
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

    /**
     * One block per {@link OreResource}: Terra's five ore blocks (ADR-0041).
     *
     * <p>Pack-authored rather than GregTech's because they carry an amount and a sprite stage, and
     * GregTech models its ore blocks at runtime -- the ADR has the cost comparison. They still drop
     * GregTech's raw ore, so nothing downstream of the item can tell.
     */
    private static final Map<OreResource, DeferredHolder<Block, OreBlock>> ORES =
            new EnumMap<>(OreResource.class);

    static {
        for (OreResource resource : OreResource.values()) {
            ORES.put(resource, BLOCKS.register(resource.blockName(), () -> new OreBlock(resource)));
        }
    }

    /**
     * One block per {@link PoleTier}, in declaration order, so the four ids are derived from the
     * tier rather than typed out twice.
     */
    private static final Map<PoleTier, DeferredHolder<Block, SupplyAreaPoleBlock>> POLES =
            new EnumMap<>(PoleTier.class);

    static {
        for (PoleTier tier : PoleTier.values()) {
            POLES.put(tier, BLOCKS.register(tier.blockName(), () -> new SupplyAreaPoleBlock(tier)));
        }
    }

    private PFBlocks() {
    }

    public static DeferredHolder<Block, OreBlock> ore(OreResource resource) {
        return ORES.get(resource);
    }

    public static DeferredHolder<Block, SupplyAreaPoleBlock> pole(PoleTier tier) {
        return POLES.get(tier);
    }

    /** The four pole blocks, for the block entity type that serves all of them. */
    public static Set<Block> poleBlocks() {
        return POLES.values().stream().map(DeferredHolder::get).collect(Collectors.toUnmodifiableSet());
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
