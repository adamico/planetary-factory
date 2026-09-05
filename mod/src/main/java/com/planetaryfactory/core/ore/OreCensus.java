package com.planetaryfactory.core.ore;

import java.util.EnumMap;
import java.util.Map;

/**
 * The count of ore blocks a placed field actually holds, and where they actually landed.
 *
 * <p>ADR-0041 divides Factorio's patch total by the blocks in the field, so this count is the
 * divisor the whole mechanic rests on. Getting it from the piece's own bounding box does not work,
 * and that is the bug this class exists to make impossible to write again: the field templates are
 * <b>one block tall</b> and placed through a gravity processor, so a piece box reads
 * {@code minY=74, maxY=74} while the blocks it placed sit at whatever height the terrain has under
 * each column. A scan of the box finds nothing, four fields record nothing, and every block on the
 * planet then derives an initial amount of zero -- which reaches the player as an untouched patch
 * reading "0 ore left".
 *
 * <p>So a column is scanned through a window around its own surface, and the field's record takes
 * the extent of the blocks that were found rather than the extent of the template. That second half
 * matters as much as the first: {@link OreFields#initialAmount} looks a block up by
 * {@code box.isInside(pos)}, and a box one block tall contains none of the blocks it placed.
 *
 * <p>Free of Minecraft, so the arithmetic is checkable in an ordinary unit test. The caller reads
 * the blocks; nothing here knows what a block is.
 */
public final class OreCensus {

    /**
     * How far above a column's surface height an ore block can still be found.
     *
     * <p>The heightmap reading is the first free space above the ground, so an ore block flush with
     * the topsoil sits at {@code surface - 1}. The margin covers a column whose heightmap has not
     * caught up with a block the same stamp wrote.
     */
    public static final int ABOVE_SURFACE = 2;

    /**
     * How far below a column's surface an ore block can still be found. The fields are one block
     * tall, so this is slack for terrain, not for depth: it covers a column whose surface reading
     * includes something standing on the ore -- snow, a plant -- and the relief a terrain-matching
     * projection leaves between neighbouring columns.
     */
    public static final int BELOW_SURFACE = 8;

    private final Map<OreResource, Extent> extents = new EnumMap<>(OreResource.class);

    /** The highest y worth reading in a column whose surface heightmap reads {@code surfaceY}. */
    public static int scanTop(int surfaceY) {
        return surfaceY + ABOVE_SURFACE;
    }

    /** The lowest y worth reading in that column. */
    public static int scanBottom(int surfaceY) {
        return surfaceY - BELOW_SURFACE;
    }

    /** Count one ore block, at the position it was actually found. */
    public void add(OreResource resource, int x, int y, int z) {
        extents.merge(resource, Extent.of(x, y, z), Extent::union);
    }

    public boolean isEmpty() {
        return extents.isEmpty();
    }

    /** What was found, per resource. A field that placed nothing is absent rather than zero. */
    public Map<OreResource, Extent> extents() {
        return Map.copyOf(extents);
    }

    /**
     * The blocks of one resource found in one piece, and the box that contains them.
     *
     * <p>The box is inclusive on both ends, like Minecraft's own, so it is handed straight to a
     * {@code BoundingBox} by the caller without a fencepost decision being taken twice.
     */
    public record Extent(int blocks, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {

        static Extent of(int x, int y, int z) {
            return new Extent(1, x, y, z, x, y, z);
        }

        Extent union(Extent other) {
            return new Extent(
                    blocks + other.blocks,
                    Math.min(minX, other.minX),
                    Math.min(minY, other.minY),
                    Math.min(minZ, other.minZ),
                    Math.max(maxX, other.maxX),
                    Math.max(maxY, other.maxY),
                    Math.max(maxZ, other.maxZ));
        }
    }
}
