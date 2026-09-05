package com.planetaryfactory.core.ore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The divisor, and the box a block is later looked up by.
 *
 * <p>What a failure here reaches the player as is what it reached them as once already: four
 * untouched starting patches reading "0 ore left". The field templates are one block tall and go
 * down through a gravity processor, so the piece box is a single y while the blocks follow the
 * terrain -- a census taken over the box counts nothing, {@link OreFields} records nothing, and
 * every block on Terra then derives an initial amount of zero. Nothing logs an error on that path.
 */
class OreCensusTest {

    @Test
    void aColumnIsScannedAroundItsOwnSurfaceRatherThanAtOneY() {
        int surface = 74;

        assertTrue(OreCensus.scanTop(surface) > surface,
                "a block can sit above a heightmap the same stamp outran");
        assertTrue(OreCensus.scanBottom(surface) < surface,
                "terrain-matching drops a flat template onto the ground, which is not one y");
        assertTrue(OreCensus.scanTop(surface) - OreCensus.scanBottom(surface) >= 8,
                "the window has to cover the relief between neighbouring columns");
    }

    @Test
    void theCountIsTheBlocksFoundNotTheColumnsScanned() {
        OreCensus census = new OreCensus();

        census.add(OreResource.IRON, 10, 75, 20);
        census.add(OreResource.IRON, 11, 74, 20);
        census.add(OreResource.IRON, 12, 76, 21);

        assertEquals(3, census.extents().get(OreResource.IRON).blocks());
    }

    @Test
    void theRecordedBoxContainsEveryBlockTheFieldPlaced() {
        OreCensus census = new OreCensus();
        census.add(OreResource.COAL, 10, 75, 20);
        census.add(OreResource.COAL, 40, 68, 55);
        census.add(OreResource.COAL, 25, 81, 33);

        OreCensus.Extent extent = census.extents().get(OreResource.COAL);

        assertEquals(10, extent.minX());
        assertEquals(40, extent.maxX());
        assertEquals(20, extent.minZ());
        assertEquals(55, extent.maxZ());
        assertEquals(68, extent.minY(), "a field spans the heights its terrain gave it");
        assertEquals(81, extent.maxY(),
                "a box one block tall is the bug: isInside would reject the blocks it counted");
    }

    @Test
    void oneBlockIsAnExtentOfItself() {
        OreCensus census = new OreCensus();
        census.add(OreResource.STONE, 5, 70, -3);

        OreCensus.Extent extent = census.extents().get(OreResource.STONE);

        assertEquals(1, extent.blocks());
        assertEquals(70, extent.minY());
        assertEquals(70, extent.maxY());
    }

    @Test
    void twoResourcesInOnePieceAreCountedApart() {
        OreCensus census = new OreCensus();
        census.add(OreResource.IRON, 0, 70, 0);
        census.add(OreResource.IRON, 1, 70, 0);
        census.add(OreResource.STONE, 40, 70, 0);

        assertEquals(2, census.extents().get(OreResource.IRON).blocks());
        assertEquals(1, census.extents().get(OreResource.STONE).blocks());
    }

    @Test
    void aPieceHoldingNoOreIsEmptyRatherThanAFieldOfZero() {
        OreCensus census = new OreCensus();

        assertTrue(census.isEmpty(), "the hub is a piece too, and records no field");
        assertTrue(census.extents().isEmpty());
    }

    @Test
    void aFieldThatWasFoundIsNotEmpty() {
        OreCensus census = new OreCensus();
        census.add(OreResource.COPPER, 3, 71, 4);

        assertFalse(census.isEmpty());
    }
}
