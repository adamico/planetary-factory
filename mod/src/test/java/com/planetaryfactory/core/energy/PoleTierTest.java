package com.planetaryfactory.core.energy;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The four tiers are a footprint ladder (ADR-0036) and differ in nothing else.
 *
 * <p>The values are Factorio's, and pinning them is the point of this class. The big pole's supply
 * area is genuinely <em>smaller</em> than the medium pole's -- 4x4 against 7x7 -- because in
 * Factorio it buys wire reach instead. It reads like a typo and is not, so it is asserted
 * explicitly rather than left to be "corrected" by the next person to open the enum.
 */
class PoleTierTest {

    @Test
    void allFourTiersShip() {
        assertEquals(4, PoleTier.values().length);
    }

    @Test
    void theSupplyAreasAreFactoriosOwn() {
        assertEquals(5, PoleTier.SMALL.supplySize());
        assertEquals(7, PoleTier.MEDIUM.supplySize());
        assertEquals(4, PoleTier.BIG.supplySize());
        assertEquals(18, PoleTier.SUBSTATION.supplySize());
    }

    @Test
    void theBigPoleCoversLessGroundThanTheMediumOne() {
        // Not a bug. Factorio's big pole trades supply area for 30 tiles of wire reach. This pack
        // has no span to trade for, and keeps the number anyway on fidelity -- see PoleTier.
        assertTrue(PoleTier.BIG.supplySize() < PoleTier.MEDIUM.supplySize());
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
        // Factorio's big pole and substation are 2x2 entities, so their even-sided areas centre on
        // a seam. This pole is one block: the count is kept exact and the offset goes negative.
        assertEquals(-2, PoleTier.BIG.minOffset());
        assertEquals(1, PoleTier.BIG.maxOffset());
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
