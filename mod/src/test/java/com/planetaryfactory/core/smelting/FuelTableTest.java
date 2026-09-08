package com.planetaryfactory.core.smelting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * The default-deny fuel alphabet (ADR-0047, #187).
 *
 * <p>The table is generated, so what is asserted here is the *rule* rather than the rows:
 * an item burns iff there is a row for it and that row is {@code chemical}.
 * {@code tests/factorio/test_fuel_convert.py} holds the rows themselves.
 */
class FuelTableTest {

    private static final FuelRow COAL =
            new FuelRow("coal", "minecraft:coal", false, 4_000_000L, "chemical");
    private static final FuelRow WOOD =
            new FuelRow("wood", "minecraft:logs", true, 2_000_000L, "chemical");
    private static final FuelRow CELL = new FuelRow("uranium-fuel-cell",
            "planetaryfactory:uranium_fuel_cell", false, 8_000_000_000L, "nuclear");

    private static final Set<String> NO_TAGS = Set.of();

    @Test
    void anItemWithARowBurnsForItsFuelValue() {
        FuelTable table = new FuelTable(List.of(COAL));
        assertEquals(4_000_000L, table.joules("minecraft:coal", NO_TAGS));
    }

    /** Default-deny: everything vanilla's table would have accepted and the pack does not name. */
    @Test
    void anItemWithNoRowDoesNotBurn() {
        FuelTable table = new FuelTable(List.of(COAL, WOOD));
        assertEquals(0L, table.joules("minecraft:blaze_rod", NO_TAGS));
        assertEquals(0L, table.joules("minecraft:lava_bucket", NO_TAGS));
        assertEquals(0L, table.joules("minecraft:stick", Set.of("minecraft:planks")));
    }

    /** `wood` maps onto `minecraft:logs`, so any log burns at wood's value. */
    @Test
    void aTagRowBurnsEveryMemberOfTheTag() {
        FuelTable table = new FuelTable(List.of(WOOD));
        assertEquals(2_000_000L, table.joules("minecraft:oak_log", Set.of("minecraft:logs")));
        assertEquals(2_000_000L, table.joules("minecraft:jungle_log", Set.of("minecraft:logs")));
        assertEquals(0L, table.joules("minecraft:oak_planks", Set.of("minecraft:planks")));
    }

    /**
     * The category filter, which is the whole reason a row's existence is not sufficient:
     * {@code uranium-fuel-cell} has a {@code fuel_value} and would otherwise become furnace fuel
     * the day #135 gives it an item-map row.
     */
    @Test
    void onlyChemicalFuelBurns() {
        FuelTable table = new FuelTable(List.of(COAL, CELL));
        assertEquals(0L, table.joules("planetaryfactory:uranium_fuel_cell", NO_TAGS));
        assertEquals(1, table.size());
    }

    /** An item row is asked before the tags, so a specific log could not be shadowed by its tag. */
    @Test
    void anItemRowWinsOverATagRow() {
        FuelRow specific =
                new FuelRow("charcoal-log", "minecraft:oak_log", false, 9L, "chemical");
        FuelTable table = new FuelTable(List.of(WOOD, specific));
        assertEquals(9L, table.joules("minecraft:oak_log", Set.of("minecraft:logs")));
    }

    /** A table that failed to load says so, rather than quietly making nothing burn. */
    @Test
    void theEmptyTableIsEmpty() {
        assertTrue(FuelTable.EMPTY.isEmpty());
        assertEquals(0L, FuelTable.EMPTY.joules("minecraft:coal", NO_TAGS));
    }

    /** Two values on one target is a question the table cannot answer by iteration order. */
    @Test
    void twoFuelsOnOneTargetIsRefused() {
        FuelRow other = new FuelRow("carbon", "minecraft:coal", false, 2_000_000L, "chemical");
        assertThrows(IllegalArgumentException.class, () -> new FuelTable(List.of(COAL, other)));
    }

    /** A row with no value or no category is not a row; the generator cannot emit one. */
    @Test
    void aRowNeedsAValueATargetAndACategory() {
        assertThrows(IllegalArgumentException.class,
                () -> new FuelRow("coal", "minecraft:coal", false, 0L, "chemical"));
        assertThrows(IllegalArgumentException.class,
                () -> new FuelRow("coal", "", false, 4L, "chemical"));
        assertThrows(IllegalArgumentException.class,
                () -> new FuelRow("coal", "minecraft:coal", false, 4L, ""));
    }
}
