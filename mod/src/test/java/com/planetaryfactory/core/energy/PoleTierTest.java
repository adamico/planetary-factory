package com.planetaryfactory.core.energy;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The three tiers are a footprint ladder (ADR-0036) and differ in nothing else.
 *
 * <p>The values are Factorio's, and pinning them is the point of this class. Factorio's fourth
 * tier, the big pole, is deliberately absent -- its 4x4 area is smaller than the medium pole's and
 * it pays for that with wire reach the pack does not price. That absence is asserted here too, so
 * re-adding the tier for fidelity has to argue with a failing test first.
 */
class PoleTierTest {

    @Test
    void threeTiersShipAndTheBigPoleIsNotOneOfThem() {
        assertEquals(3, PoleTier.values().length);
        for (PoleTier tier : PoleTier.values()) {
            assertTrue(tier.supplySize() > PoleTier.SMALL.supplySize() || tier == PoleTier.SMALL,
                    "the ladder only grows: " + tier + " is not a big pole in disguise");
        }
    }

    @Test
    void theSupplyAreasAreFactoriosOwn() {
        assertEquals(5, PoleTier.SMALL.supplySize());
        assertEquals(7, PoleTier.MEDIUM.supplySize());
        assertEquals(18, PoleTier.SUBSTATION.supplySize());
    }

    @Test
    void everyTierHasTheSameVerticalBand() {
        for (PoleTier tier : PoleTier.values()) {
            assertEquals(PoleTier.VERTICAL_RADIUS, tier.verticalRadius());
        }
    }

    @Test
    void theOffsetRangeSpansExactlyTheSupplySize() {
        for (PoleTier tier : PoleTier.values()) {
            assertEquals(tier.supplySize(), tier.maxOffset() - tier.minOffset() + 1,
                    tier + " must reach exactly as far as Factorio says it does");
        }
    }

    @Test
    void anOddSizedAreaCentresOnThePole() {
        assertEquals(-2, PoleTier.SMALL.minOffset());
        assertEquals(2, PoleTier.SMALL.maxOffset());
    }

    @Test
    void anEvenSizedAreaTakesItsExtraBlockOnTheNegativeSide() {
        // Factorio's substation is a 2x2 entity, so its even-sided area centres on a seam. This
        // pole is one block: the count is kept exact and the offset goes negative.
        assertEquals(-9, PoleTier.SUBSTATION.minOffset());
        assertEquals(8, PoleTier.SUBSTATION.maxOffset());
    }

    @Test
    void blockNamesAreFactoriosIdsWithMinecraftsSeparator() {
        assertEquals("small_electric_pole", PoleTier.SMALL.blockName());
        assertEquals("substation_electric_pole", PoleTier.SUBSTATION.blockName());
        for (PoleTier tier : PoleTier.values()) {
            assertEquals(tier.blockName(), tier.blockName().toLowerCase(Locale.ROOT));
        }
    }
}
