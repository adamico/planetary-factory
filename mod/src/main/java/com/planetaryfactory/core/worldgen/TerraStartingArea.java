package com.planetaryfactory.core.worldgen;

import com.mojang.logging.LogUtils;
import com.planetaryfactory.core.PlanetaryFactoryCore;
import com.planetaryfactory.core.ore.OreBlock;
import com.planetaryfactory.core.ore.OreCensus;
import com.planetaryfactory.core.ore.OreFields;
import com.planetaryfactory.core.ore.OreResource;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.slf4j.Logger;

/**
 * Terra's opening: the hub and its three ore fields, stamped at world spawn.
 *
 * <p>ADR-0019 asks for a <em>spawn-anchored</em> starting area, and vanilla cannot express that.
 * A {@code StructurePlacement} decides which chunks a structure occupies from
 * {@link net.minecraft.world.level.chunk.ChunkGeneratorStructureState} alone -- a biome source, a
 * random state and the seeds. It is deliberately isolated from level state so that worldgen stays
 * deterministic and thread-safe, which means <em>no placement type can see world spawn</em>, custom
 * ones included. The nearest vanilla placement is {@code concentric_rings} at distance 0, and it
 * anchors to the world origin rather than to spawn: it pins the ring to chunk (0,0) and searches a
 * hardcoded 112 blocks for a preferred biome. On a seed whose origin is open ocean -- Terra has a
 * sea, so that is common -- the search fails, the ring falls back to (0,0), the structure's
 * land-biome predicate refuses to start there, and the player gets no opening at all. Silently.
 *
 * <p>So the placement happens here instead, which is also what the modpacks that do this well do:
 * vanilla chooses a spawn, which is on land by construction, and the pack stamps the jigsaw onto
 * it. The structure is no longer part of worldgen -- {@code /locate} will not find it -- so the
 * coordinates are logged. Everything above the placement is unchanged: the same template pools,
 * the same hub variants, the same size and rotation randomisation, run through vanilla's own
 * {@link JigsawPlacement#generateJigsaw}.
 *
 * <p>The work happens on {@link ServerStartedEvent} rather than when spawn is chosen. Vanilla picks
 * the spawn position before the spawn chunks exist, and stamping needs blocks to write into; by the
 * time the server reports started, it has prepared the spawn area. A {@link SavedData} flag makes it
 * once-per-world rather than once-per-load.
 *
 * <p>The prepared spawn area is not big enough, though, and that is why the placement below is
 * vanilla's {@code generateJigsaw} inlined rather than called: the fields reach past it, and a
 * {@code terrain_matching} piece over an unloaded chunk is written at the bottom of the world rather
 * than on the ground. See {@link #stamp}.
 */
public final class TerraStartingArea {
    private static final Logger LOGGER = LogUtils.getLogger();

    /** The hub pool. Its three connectors are what deal the ore fields; see ADR-0019. */
    private static final ResourceKey<StructureTemplatePool> START_POOL = ResourceKey.create(
            Registries.TEMPLATE_POOL,
            ResourceLocation.fromNamespaceAndPath(PlanetaryFactoryCore.NAMESPACE, "terra_start"));

    /**
     * The name every hub connector carries. Vanilla offsets the hub so that the connector it picks
     * lands on the given position, so the player spawns on the hub edge with the fields radiating
     * away -- which is the reading ADR-0019 wants.
     */
    private static final ResourceLocation START_JIGSAW =
            ResourceLocation.fromNamespaceAndPath(PlanetaryFactoryCore.NAMESPACE, "terra_start_hub");

    /** One level of children: the hub, then its three patches. Matches the structure's own size. */
    private static final int MAX_DEPTH = 1;

    /**
     * The radius the children must fit inside, in blocks. This is the value vanilla's own
     * {@code generateJigsaw} hardcodes; the placement below is that method inlined, so it keeps it.
     * The templates are laid out well inside it -- the furthest patch centre is 62 from the hub
     * face plus its own 17-block half-width.
     */
    private static final int MAX_DISTANCE_FROM_CENTER = 128;

    private TerraStartingArea() {
    }

    public static void onServerStarted(ServerStartedEvent event) {
        ServerLevel level = event.getServer().overworld();
        Stamped stamped = level.getDataStorage().computeIfAbsent(Stamped.FACTORY, Stamped.NAME);
        if (stamped.done) {
            return;
        }

        // Marked before the attempt, not after. A stamp that throws half-placed would otherwise be
        // retried on every load, each retry writing another hub over the wreckage of the last.
        stamped.done = true;
        stamped.setDirty();

        Holder<StructureTemplatePool> pool = level.registryAccess()
                .registryOrThrow(Registries.TEMPLATE_POOL)
                .getHolder(START_POOL)
                .map(holder -> (Holder<StructureTemplatePool>) holder)
                .orElse(null);
        if (pool == null) {
            LOGGER.error("No template pool {}: Terra has no starting area. Is the datapack loaded?",
                    START_POOL.location());
            return;
        }

        BlockPos spawn = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG,
                level.getSharedSpawnPos());
        stamp(level, pool, spawn);
    }

    /**
     * Vanilla's {@link JigsawPlacement#generateJigsaw} with two things added: the chunks the pieces
     * land in are loaded before anything is written, and the pieces are logged.
     *
     * <p>The chunk loading is the whole reason this is inlined rather than called. The patches use
     * the {@code terrain_matching} projection, which places every column through a
     * {@link net.minecraft.world.level.levelgen.structure.templatesystem.GravityProcessor} that asks
     * the <em>level</em> -- not the chunk generator -- for the surface height. {@code Level.getHeight}
     * does not generate: on a chunk that is not loaded it returns {@code getMinBuildHeight()}. So a
     * field reaching past the loaded spawn area is not dropped, it is written at y=-64, buried in
     * bedrock, and the player finds part of a patch or none of it depending on how the spawn area
     * happened to fall. Nothing is logged, and both the pieces and the blocks are "placed" as far as
     * vanilla is concerned. Loading the chunks first is the fix; there is no hook inside
     * {@code generateJigsaw} to do it from, hence the copy.
     */
    private static void stamp(ServerLevel level, Holder<StructureTemplatePool> pool, BlockPos spawn) {
        var chunkGenerator = level.getChunkSource().getGenerator();
        var context = new Structure.GenerationContext(
                level.registryAccess(),
                chunkGenerator,
                chunkGenerator.getBiomeSource(),
                level.getChunkSource().randomState(),
                level.getStructureManager(),
                level.getSeed(),
                new ChunkPos(spawn),
                level,
                biome -> true);
        Optional<Structure.GenerationStub> stub = JigsawPlacement.addPieces(
                context,
                pool,
                Optional.of(START_JIGSAW),
                MAX_DEPTH,
                spawn,
                false,
                Optional.empty(),
                MAX_DISTANCE_FROM_CENTER,
                PoolAliasLookup.EMPTY,
                JigsawStructure.DEFAULT_DIMENSION_PADDING,
                JigsawStructure.DEFAULT_LIQUID_SETTINGS);
        if (stub.isEmpty()) {
            LOGGER.error("Terra's starting area could not be placed at {}, {}, {}",
                    spawn.getX(), spawn.getY(), spawn.getZ());
            return;
        }

        List<StructurePiece> pieces = stub.get().getPiecesBuilder().build().pieces();
        // One line per piece. A jigsaw child that is rejected -- for overlapping a sibling, or for
        // reaching outside MAX_DISTANCE_FROM_CENTER -- is dropped silently, so the piece count is
        // the only signal that the hub dealt fewer than its three fields.
        for (StructurePiece piece : pieces) {
            LOGGER.info("Terra's starting area piece: {} at {}", describe(piece), piece.getBoundingBox());
        }

        loadChunks(level, pieces);

        RandomSource random = level.getRandom();
        for (StructurePiece piece : pieces) {
            if (piece instanceof PoolElementStructurePiece poolPiece) {
                poolPiece.place(level, level.structureManager(), chunkGenerator, random,
                        BoundingBox.infinite(), spawn, false);
            }
        }
        recordFields(level, pieces);
        LOGGER.info("Terra's starting area placed at {}, {}, {}: {} pieces",
                spawn.getX(), spawn.getY(), spawn.getZ(), pieces.size());
    }

    /**
     * Count each field's ore blocks, so a block's amount can be the patch total divided by them.
     *
     * <p>ADR-0041 keeps Factorio's patch total as the invariant and derives the per-block amount
     * from it. Only the placed world knows the divisor: the hub deals one of three size variants
     * per resource, and vanilla drops an overlapping child silently, so counting what is actually
     * there is the only reading that cannot be wrong.
     *
     * <p>Counted once, here, and written down as one record per field. That is four records, not a
     * counter per ore block -- and it is the *initial* amount's derivation, fixed at placement,
     * rather than the parallel remaining-count ADR-0020 refused.
     */
    private static void recordFields(ServerLevel level, List<StructurePiece> pieces) {
        OreFields fields = level.getDataStorage().computeIfAbsent(OreFields.FACTORY, OreFields.NAME);
        for (StructurePiece piece : pieces) {
            BoundingBox box = piece.getBoundingBox();
            OreCensus census = census(level, box);
            if (census.isEmpty()) {
                // The hub is a piece and holds no ore, so this is not always wrong -- but a field
                // piece that counts nothing is the silence that shipped every block at zero.
                LOGGER.info("Terra's piece at {} holds no ore blocks", box);
                continue;
            }
            for (Map.Entry<OreResource, OreCensus.Extent> entry : census.extents().entrySet()) {
                OreCensus.Extent extent = entry.getValue();
                OreFields.Field placed = fields.record(entry.getKey(), boxOf(extent), extent.blocks());
                LOGGER.info("Terra's {} field: {} blocks over {}, {} units each",
                        placed.resource(), extent.blocks(), placed.box(), placed.amountPerBlock());
            }
        }
    }

    /**
     * Read the ore blocks a piece actually placed.
     *
     * <p>Column by column rather than over the piece's box, because the box is one block tall: the
     * fields are flat templates put down through a gravity processor, so the piece reports the y it
     * was anchored at while its blocks sit on the terrain. Each column is scanned through
     * {@link OreCensus}'s window around its own surface height, which is a handful of reads per
     * column and finds the block wherever the ground put it.
     */
    private static OreCensus census(ServerLevel level, BoundingBox box) {
        OreCensus census = new OreCensus();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                int top = Math.min(OreCensus.scanTop(surface), level.getMaxBuildHeight() - 1);
                int bottom = Math.max(OreCensus.scanBottom(surface), level.getMinBuildHeight());
                for (int y = top; y >= bottom; y--) {
                    if (level.getBlockState(cursor.set(x, y, z)).getBlock() instanceof OreBlock ore) {
                        census.add(ore.resource(), x, y, z);
                    }
                }
            }
        }
        return census;
    }

    /** The census's own inclusive extent, as the box the field is recorded and looked up by. */
    private static BoundingBox boxOf(OreCensus.Extent extent) {
        return new BoundingBox(
                extent.minX(), extent.minY(), extent.minZ(),
                extent.maxX(), extent.maxY(), extent.maxZ());
    }

    /**
     * Load every chunk the pieces touch, so that the gravity processor can see a real heightmap.
     *
     * <p>{@link ServerLevel#getChunk(int, int)} generates the chunk if it does not exist yet and
     * blocks until it is at {@code FULL}, which is what makes the heightmap readable. The area is
     * bounded by {@link #MAX_DISTANCE_FROM_CENTER}, so this is at most a few hundred chunks, once
     * per world, at a moment when the server is already generating the spawn area.
     */
    private static void loadChunks(ServerLevel level, List<StructurePiece> pieces) {
        BoundingBox bounds = null;
        for (StructurePiece piece : pieces) {
            bounds = bounds == null ? piece.getBoundingBox()
                    : BoundingBox.encapsulatingBoxes(List.of(bounds, piece.getBoundingBox())).orElseThrow();
        }
        if (bounds == null) {
            return;
        }

        int minChunkX = SectionPos.blockToSectionCoord(bounds.minX());
        int maxChunkX = SectionPos.blockToSectionCoord(bounds.maxX());
        int minChunkZ = SectionPos.blockToSectionCoord(bounds.minZ());
        int maxChunkZ = SectionPos.blockToSectionCoord(bounds.maxZ());
        for (int x = minChunkX; x <= maxChunkX; x++) {
            for (int z = minChunkZ; z <= maxChunkZ; z++) {
                level.getChunk(x, z);
                if (!level.hasChunk(x, z)) {
                    // Not fatal, but it means the fields in this chunk will be written at bedrock.
                    // Worth a line rather than the silence that made this bug take a day to find.
                    LOGGER.warn("Chunk {}, {} would not load: Terra's starting area may be buried there",
                            x, z);
                }
            }
        }
        LOGGER.info("Terra's starting area loaded {} chunks over {}",
                (maxChunkX - minChunkX + 1) * (maxChunkZ - minChunkZ + 1), bounds);
    }

    /** The template a piece came from, for the log line. */
    private static String describe(StructurePiece piece) {
        return piece instanceof PoolElementStructurePiece poolPiece
                ? poolPiece.getElement().toString()
                : piece.getClass().getSimpleName();
    }

    /** Once per world, not once per load. Presence is the whole state; the flag survives a reload. */
    public static final class Stamped extends SavedData {
        static final String NAME = "planetaryfactory_terra_starting_area";
        static final Factory<Stamped> FACTORY = new Factory<>(Stamped::new, Stamped::load, null);

        private boolean done;

        private static Stamped load(CompoundTag tag, HolderLookup.Provider registries) {
            Stamped stamped = new Stamped();
            stamped.done = tag.getBoolean("done");
            return stamped;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            tag.putBoolean("done", this.done);
            return tag;
        }
    }
}
