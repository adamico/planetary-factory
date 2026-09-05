package com.planetaryfactory.core.ore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The per-block amount, which is a quotient and never a typed constant.
 *
 * <p>ADR-0041: the patch total is the invariant Factorio states, and the per-block amount is that
 * total over the blocks the field happens to hold. A field resized by the generator therefore
 * re-derives rather than drifting, and the failure this guards is the quiet one -- a patch that
 * still says 400,000 in a document while paying out half of it.
 */
class OreFieldTest {

    @Test
    void theAmountIsTheTotalOverTheBlocks() {
        OreField field = new OreField("iron", 400_000, 400);

        assertEquals(1_000, field.amountPerBlock());
    }

    @Test
    void aFieldNeverPaysOutMoreThanItsTotal() {
        OreField field = new OreField("copper", 320_000, 383);

        assertTrue((long) field.amountPerBlock() * 383 <= 320_000,
                "the quotient rounds down, so the field is never richer than Factorio's patch");
    }

    @Test
    void everyBlockCarriesAtLeastOneUnit() {
        OreField field = new OreField("stone", 10, 400);

        assertEquals(1, field.amountPerBlock(),
                "a block that holds nothing is a block that breaks on the first hit for no ore");
    }

    @Test
    void aFieldWithNoBlocksHasNoAmountRatherThanADivisionByZero() {
        OreField field = new OreField("coal", 320_000, 0);

        assertEquals(0, field.amountPerBlock());
    }
}
