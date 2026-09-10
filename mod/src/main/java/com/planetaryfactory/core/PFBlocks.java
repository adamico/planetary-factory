package com.planetaryfactory.core;

import com.planetaryfactory.core.energy.PoleTier;
import com.planetaryfactory.core.mining.rig.RigBlock;
import com.planetaryfactory.core.mining.rig.RigPartBlock;
import com.planetaryfactory.core.mining.rig.RigTier;
import com.planetaryfactory.core.ore.OreBlock;
import com.planetaryfactory.core.ore.OreResource;
import com.planetaryfactory.core.smelting.FurnaceBlock;
import com.planetaryfactory.core.smelting.FurnaceTier;
import com.planetaryfactory.core.energy.SupplyAreaPoleBlock;
import com.planetaryfactory.core.fluid.BoilerBlock;
import com.planetaryfactory.core.fluid.OffshorePumpBlock;
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
     * Factorio's Offshore Pump (#213, ADR-0050): the single point at which water enters the
     * factory. One block rather than a ladder -- Factorio has one pump and so does this pack.
     */
    public static final DeferredHolder<Block, OffshorePumpBlock> OFFSHORE_PUMP =
            BLOCKS.register("offshore_pump", OffshorePumpBlock::new);

    /**
     * Terra's Boiler (#224, ADR-0048): fuel and water in, low-temperature steam out.
     *
     * <p>One block, not a ladder. ADR-0033 has the reactor emitting superheated steam directly
     * with no heat layer, so Factorio's second boiler tier has nothing to be in this pack.
     */
    public static final DeferredHolder<Block, BoilerBlock> BOILER =
            BLOCKS.register("boiler", BoilerBlock::new);

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
     * Factorio's three furnace tiers (#155), keyed the same way the poles are: the id comes from
     * the tier rather than being typed out twice.
     */
    private static final Map<FurnaceTier, DeferredHolder<Block, FurnaceBlock>> FURNACES =
            new EnumMap<>(FurnaceTier.class);

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

    static {
        for (FurnaceTier tier : FurnaceTier.values()) {
            FURNACES.put(tier, BLOCKS.register(tier.blockName(), () -> new FurnaceBlock(tier)));
        }
    }

    /**
     * The two rigs' anchors, and the parts that surround them (#192, ADR-0043). One anchor and one
     * part block per tier, the way the furnace and pole ladders are one class per tier -- the
     * anchor holds the block entity and every part forwards a break to it.
     */
    private static final Map<RigTier, DeferredHolder<Block, RigBlock>> RIGS = new EnumMap<>(RigTier.class);
    private static final Map<RigTier, DeferredHolder<Block, RigPartBlock>> RIG_PARTS =
            new EnumMap<>(RigTier.class);

    static {
        for (RigTier tier : RigTier.values()) {
            RIGS.put(tier, BLOCKS.register(tier.blockName(), () -> new RigBlock(tier)));
            RIG_PARTS.put(tier, BLOCKS.register(tier.partBlockName(), () -> new RigPartBlock(tier)));
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

    public static DeferredHolder<Block, FurnaceBlock> furnace(FurnaceTier tier) {
        return FURNACES.get(tier);
    }

    /** The three furnace blocks, for the block entity type that serves all of them. */
    public static Set<Block> furnaceBlocks() {
        return FURNACES.values().stream().map(DeferredHolder::get).collect(Collectors.toUnmodifiableSet());
    }

    public static DeferredHolder<Block, RigBlock> rig(RigTier tier) {
        return RIGS.get(tier);
    }

    public static DeferredHolder<Block, RigPartBlock> rigPart(RigTier tier) {
        return RIG_PARTS.get(tier);
    }

    /** Both rigs' anchor blocks, for the block entity type that serves both. */
    public static Set<Block> rigBlocks() {
        return RIGS.values().stream().map(DeferredHolder::get).collect(Collectors.toUnmodifiableSet());
    }

    /** Both rigs' part blocks, for the block entity type that serves both. */
    public static Set<Block> rigPartBlocks() {
        return RIG_PARTS.values().stream().map(DeferredHolder::get).collect(Collectors.toUnmodifiableSet());
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
