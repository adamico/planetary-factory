package com.planetaryfactory.core.smelting;

import java.util.Locale;

/**
 * Factorio's three furnace tiers (#155), as a speed-and-energy ladder and nothing else.
 *
 * <p>The ladder gates no recipe: all three tiers read {@code planetaryfactory:smelting} and only
 * that, so nothing a Stone Furnace cannot smelt exists. What a tier buys is time, and at the top
 * rung the freedom from carrying fuel to it.
 *
 * <h2>Where the numbers come from</h2>
 *
 * <p>{@code crafting_speed} is Factorio's own, unnormalised, exactly as ADR-0029 takes the
 * Assembling Machines'. The recipe carries {@code energy_required * 20} with no speed divisor in
 * it, and the block divides -- so {@code steel-plate} emits at 320 ticks and is observed at 320 on
 * Stone, 160 on the other two.
 *
 * <p>The Electric tier's draw is ADR-0029's constant applied to its own {@code energy_usage}:
 * 180 kW * 32/420_000, truncated the way that ADR's table truncates. Idle draw is not modelled;
 * ADR-0029 records the 6 kW as {@code excluded}.
 *
 * <p><b>Fuel has no tier rule at all.</b> Both burners consume one burn tick per tick of
 * operation, using Minecraft's own burn values, so the Steel tier's doubled speed is the entire
 * reason it gets twice the items out of one coal. That is Factorio's efficiency story, arrived at
 * without a second scalar to tune. Factorio's absolute rate is not taken: the corpus carries no
 * {@code fuel_value}, and Minecraft's burn times are a thing the player already knows.
 *
 * <p>Pure: no Minecraft types, so the mod's Minecraft-free test source set can hold it to account.
 */
public enum FurnaceTier {
    STONE(1.0f, true),
    STEEL(2.0f, true),
    ELECTRIC(2.0f, false);

    /** The Electric Furnace's {@code energy_usage} in watts, from {@code machine.json}. */
    private static final long ELECTRIC_WATTS = 180_000L;

    /** ADR-0029's scale: LV's 32 EU/t anchored on the Oil Refinery's 420 kW. */
    private static final long EU_PER_TICK_NUMERATOR = 32L;
    private static final long EU_PER_TICK_DENOMINATOR = 420_000L;

    /** The reference craft the buffer is sized on: steel-plate, 16 s in the corpus. */
    private static final int STEEL_PLATE_TICKS = 320;

    private final float craftingSpeed;
    private final boolean burnsFuel;

    FurnaceTier(float craftingSpeed, boolean burnsFuel) {
        this.craftingSpeed = craftingSpeed;
        this.burnsFuel = burnsFuel;
    }

    public float craftingSpeed() {
        return craftingSpeed;
    }

    /** Whether this tier has a fuel slot. The Electric tier does not. */
    public boolean burnsFuel() {
        return burnsFuel;
    }

    /**
     * The recipe's ticks as this tier observes them. Never zero: a craft the speed divisor would
     * round away still costs the tick it takes to run.
     */
    public int durationTicks(int recipeTicks) {
        return Math.max(1, (int) Math.ceil(recipeTicks / craftingSpeed));
    }

    /** EU drawn per tick of operation, and zero on the two burner tiers. */
    public long euPerTick() {
        return burnsFuel ? 0L : ELECTRIC_WATTS * EU_PER_TICK_NUMERATOR / EU_PER_TICK_DENOMINATOR;
    }

    /**
     * The buffer the supply-area pole fills, sized at one whole steel craft.
     *
     * <p>Bigger buys nothing -- the pole tops it up every tick it has EU -- and smaller would stall
     * a craft that had already started against a pole that was merely busy elsewhere.
     */
    public long bufferEu() {
        return euPerTick() * durationTicks(STEEL_PLATE_TICKS);
    }

    /** The registry path, e.g. {@code stone_furnace}. */
    public String blockName() {
        return serializedName() + "_furnace";
    }

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** The inverse of {@link #serializedName()}, which is the spelling every other id path uses. */
    public static FurnaceTier byName(String name) {
        for (FurnaceTier tier : values()) {
            if (tier.serializedName().equals(name)) {
                return tier;
            }
        }
        throw new IllegalArgumentException("no furnace tier named " + name);
    }
}
