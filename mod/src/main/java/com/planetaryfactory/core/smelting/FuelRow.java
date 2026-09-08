package com.planetaryfactory.core.smelting;

/**
 * One row of the generated fuel table (ADR-0047, #187): what burns, for how many joules, and in
 * which of Factorio's fuel categories.
 *
 * <p>{@code target} is either an item id or a tag id, told apart by {@code tag} -- the lookup is
 * item-then-tag rather than a flat item map, because {@code wood} maps onto {@code minecraft:logs}
 * and every log is Factorio's wood.
 *
 * <p>{@code factorioName} is carried for diagnosis only; nothing routes on it.
 *
 * <p>Pure: no Minecraft types, so ids are strings and the table is testable without a game.
 */
public record FuelRow(String factorioName, String target, boolean tag, long fuelValue,
        String fuelCategory) {

    public FuelRow {
        if (target == null || target.isEmpty()) {
            throw new IllegalArgumentException(factorioName + " has no target");
        }
        if (fuelValue <= 0L) {
            throw new IllegalArgumentException(factorioName + " has no fuel value");
        }
        if (fuelCategory == null || fuelCategory.isEmpty()) {
            throw new IllegalArgumentException(factorioName + " has no fuel category");
        }
    }
}
