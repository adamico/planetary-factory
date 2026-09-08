package com.planetaryfactory.core.mining.rig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * That the generated rig corpus parses, and that what it parses to is what the rest of the package
 * reads (#192, #193).
 *
 * <p>This is deliberately not the check that the numbers are <em>right</em>: {@code drills.json} is
 * generated from {@code data/factorio/machine.json}, and {@code tests/pack/test_rig_assets.py}
 * asserts it against that corpus rather than against literals -- which is what catches a generator
 * patched to emit numbers somebody chose. What this adds is that the resource is on the classpath
 * at all, that every registered tier has a row, and that the fields land in the right slots. A
 * transposed {@code vectorX}/{@code vectorY} parses perfectly and ejects sideways.
 */
class RigCorpusTest {

    @Test
    void theBurnerRigIsTwoByTwoAndBurnsChemicalFuelAtOneHundredAndFiftyKilowatts() {
        RigCorpus.Row row = RigCorpus.get().rowOf(RigTier.BURNER);

        assertEquals(2, row.width());
        assertEquals(2, row.height());
        assertEquals(0.25, row.miningSpeed());
        assertEquals(150_000.0, row.energyUsage());
        assertEquals("burner", row.energyType());
        assertTrue(row.burnsFuel());
        assertEquals(List.of("chemical"), row.fuelCategories());
    }

    @Test
    void theElectricRigIsThreeByThreeAndTakesNoFuelAtAll() {
        // Not "has an empty fuel list by accident": ADR-0036 makes it a supply-area pole customer,
        // so a fuel slot on it would be the wrong machine (#194).
        RigCorpus.Row row = RigCorpus.get().rowOf(RigTier.ELECTRIC);

        assertEquals(3, row.width());
        assertEquals(3, row.height());
        assertEquals(0.5, row.miningSpeed());
        assertEquals("electric", row.energyType());
        assertFalse(row.burnsFuel());
        assertEquals(List.of(), row.fuelCategories());
    }

    @Test
    void theOutputVectorPointsForwardAndNotSideways() {
        // Factorio's y runs south, so a drill's own output vector is NEGATIVE in y -- it points at
        // the entity's front. A transposed pair would parse and would eject out of the rig's side,
        // which is why this is asserted here rather than left to the offset arithmetic.
        for (RigTier tier : RigTier.values()) {
            RigCorpus.Row row = RigCorpus.get().rowOf(tier);

            assertTrue(row.vectorY() < 0,
                    tier + "'s output vector does not point at its front: " + row.vectorY());
            assertTrue(Math.abs(row.vectorX()) < Math.abs(row.vectorY()),
                    tier + "'s output vector is more sideways than forward");
        }
    }

    @Test
    void everyRigReachesFarEnoughToWorkTheGroundItStandsOn() {
        // A radius smaller than the footprint's own half-extent would give a rig that stands on
        // ore and mines none of it. The burner rig's 0.99 against a half-extent of 0.5 is the
        // tight case, and it is the one a guessed 0.49 would have broken.
        for (RigTier tier : RigTier.values()) {
            RigCorpus.Row row = RigCorpus.get().rowOf(tier);

            assertTrue(row.searchingRadius() >= (row.width() - 1) / 2.0,
                    tier + " cannot reach its own footprint");
            assertEquals(
                    row.width() * row.height(),
                    Math.min(
                            row.width() * row.height(),
                            RigArea.tiles(row.width(), row.height(), row.searchingRadius(),
                                    RigFacing.NORTH).size()),
                    tier + " works fewer tiles than it stands on");
        }
    }
}
