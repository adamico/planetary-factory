package com.planetaryfactory.core.worldgen;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jetbrains.annotations.Nullable;

/**
 * Drop every block of a template onto the ground, where "ground" means the terrain and not what
 * happens to be growing on it.
 *
 * <p>This exists because vanilla's own {@code minecraft:gravity} processor -- the one the
 * {@code terrain_matching} projection applies -- cannot express that. It reads a heightmap, and on
 * a {@code ServerLevel} it rewrites {@code WORLD_SURFACE_WG} to {@code WORLD_SURFACE}, whose
 * definition is "the highest block that is not air". A tree is not air. So an ore field crossing a
 * wood landed on the canopy: ore blocks in place of leaves, twenty blocks up, with the field split
 * between treetop and ground wherever the wood ended. No vanilla heightmap helps here --
 * {@code OCEAN_FLOOR} and {@code MOTION_BLOCKING} both stop at leaves and logs too.
 *
 * <p>So the column is walked down instead, from the surface through everything that grew there, and
 * the block lands on the first thing that is actually terrain. A field under a wood therefore lies
 * *under* the trees rather than on top of them, which is the reading ADR-0019 wants: the ore
 * replaces the topsoil block, the trunk that stood on that block still stands on it, and the patch
 * is something the player walks onto rather than climbs.
 *
 * <p>Applying this makes the element's projection {@code rigid} rather than {@code terrain_matching}:
 * the projection is what would add the gravity processor, and it runs after the element's own
 * processors, so leaving it on would simply put the ore back on the canopy. The piece's bounding box
 * is therefore flat at the hub's y while the blocks are not -- which costs nothing here, because the
 * boxes are only used for the sibling-overlap test and no two of them overlap in x/z anyway
 * ({@code tests/worldgen/test_start_geometry.py}).
 */
public final class GroundProcessor extends StructureProcessor {
    public static final GroundProcessor INSTANCE = new GroundProcessor();
    public static final MapCodec<GroundProcessor> CODEC = MapCodec.unit(() -> INSTANCE);

    private GroundProcessor() {
    }

    @Nullable
    @Override
    public StructureTemplate.StructureBlockInfo process(
            LevelReader level,
            BlockPos offset,
            BlockPos pos,
            StructureTemplate.StructureBlockInfo blockInfo,
            StructureTemplate.StructureBlockInfo relativeBlockInfo,
            StructurePlaceSettings settings,
            @Nullable StructureTemplate template) {
        BlockPos placed = relativeBlockInfo.pos();
        // `blockInfo` is the template-space original and `relativeBlockInfo` the world-space one
        // -- the same split vanilla's gravity processor relies on. The template's own y is kept as
        // an offset from the ground, so a template can still be more than one block thick.
        int y = ground(level, placed.getX(), placed.getZ()) + blockInfo.pos().getY();
        return new StructureTemplate.StructureBlockInfo(
                new BlockPos(placed.getX(), y, placed.getZ()),
                relativeBlockInfo.state(),
                relativeBlockInfo.nbt());
    }

    /** The y of the topmost terrain block in a column: the one a patch replaces. */
    private static int ground(LevelReader level, int x, int z) {
        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(x, y, z);
        while (y > level.getMinBuildHeight() && !isTerrain(level.getBlockState(cursor.setY(y)))) {
            y--;
        }
        return y;
    }

    /**
     * Terrain is what a patch may sit on. Everything else -- the wood, the undergrowth, the snow
     * that settled on both -- is passed through on the way down.
     *
     * <p>Water is not terrain either, so a field that clips a pond lies on its floor rather than
     * floating. The fields are on land biomes by construction, so that is an edge, not the case.
     */
    private static boolean isTerrain(BlockState state) {
        return state.blocksMotion()
                && !state.is(BlockTags.LOGS)
                && !state.is(BlockTags.LEAVES);
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return PFWorldgen.GROUND_PROCESSOR.get();
    }
}
