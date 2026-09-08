package com.planetaryfactory.core.mining.rig;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * That the mod reads the two rigs' footprint sizes off the corpus rather than carrying them as
 * typed integers (#192, ADR-0043).
 *
 * <p>{@code data/factorio/machine.json}'s {@code drills} rows are the source of truth (#188); the
 * static check in {@code tests/} asserts {@code footprint.json} against that file. What this adds
 * is that the mod can actually parse the generated slice and gets the numbers ADR-0043 states:
 * 2x2 for the burner rig, 3x3 for the electric one.
 */
class RigFootprintsTest {

    @Test
    void theBurnerRigIsTwoByTwo() {
        RigFootprints.Size size = RigFootprints.get().sizeOf(RigTier.BURNER);
        assertEquals(2, size.width());
        assertEquals(2, size.height());
    }

    @Test
    void theElectricRigIsThreeByThree() {
        RigFootprints.Size size = RigFootprints.get().sizeOf(RigTier.ELECTRIC);
        assertEquals(3, size.width());
        assertEquals(3, size.height());
    }
}
