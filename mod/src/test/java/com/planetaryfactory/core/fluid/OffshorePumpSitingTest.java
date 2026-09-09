package com.planetaryfactory.core.fluid;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.planetaryfactory.core.fluid.OffshorePumpSiting.Neighbour.DRY;
import static com.planetaryfactory.core.fluid.OffshorePumpSiting.Neighbour.FLOWING;
import static com.planetaryfactory.core.fluid.OffshorePumpSiting.Neighbour.SOURCE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ADR-0050's predicate, asserted away from any world.
 *
 * <p>The rule the whole design rests on is one sentence -- one adjacent block whose fluid state is
 * a source -- and the reason it is safe is not in this class at all: it is that the pack never
 * creates a source, so every source in the world is one worldgen or a structure placed. What these
 * tests hold is that the predicate stays that sentence. A size test or a biome test creeping in
 * here is the rejected design of ADR-0050 arriving by the back door, and both were rejected for
 * making water something the player hunts for.
 */
class OffshorePumpSitingTest {

    @Test
    @DisplayName("one adjacent source is enough, whatever else surrounds the pump")
    void oneSourceIsEnough() {
        assertTrue(OffshorePumpSiting.accepts(List.of(SOURCE)));
        assertTrue(OffshorePumpSiting.accepts(List.of(DRY, DRY, SOURCE, DRY)));
        assertTrue(OffshorePumpSiting.accepts(List.of(FLOWING, SOURCE)));
    }

    @Test
    @DisplayName("flowing water is refused -- it is what a placed outlet makes")
    void flowingIsRefused() {
        assertFalse(OffshorePumpSiting.accepts(List.of(FLOWING)),
                "flowing water is the one state ADR-0050's deferred outlet block may create, and "
                        + "admitting it here would make that block a source of water rather than a "
                        + "way to move it");
        assertFalse(OffshorePumpSiting.accepts(List.of(FLOWING, FLOWING, FLOWING, FLOWING)));
    }

    @Test
    @DisplayName("dry ground is refused, and so is nothing at all")
    void dryIsRefused() {
        assertFalse(OffshorePumpSiting.accepts(List.of(DRY)));
        assertFalse(OffshorePumpSiting.accepts(List.of()));
    }

    @Test
    @DisplayName("no minimum body size: a single source block is a valid site")
    void noMinimumBodySize() {
        assertTrue(OffshorePumpSiting.accepts(List.of(SOURCE)),
                "ADR-0050 rejected every minimum-size rule; one block is a site");
    }
}
