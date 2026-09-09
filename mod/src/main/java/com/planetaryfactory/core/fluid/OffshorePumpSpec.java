package com.planetaryfactory.core.fluid;

/**
 * The pump's rate, converted once, here (ADR-0050).
 *
 * <p>Nothing in this class is chosen. {@code pumping_speed} is read from the corpus by
 * {@link PumpCorpus}; what happens to it is arithmetic against two tick rates, and it is written
 * down in one place because there are two ways to get it wrong and both produce a plausible number.
 *
 * <p><b>Trap one: {@code pumping_speed} is per Factorio tick, and its value is 20.</b> Factorio runs
 * at 60 ticks a second, so the pump is 1,200 units a second -- but 20 is also Minecraft's tick rate,
 * so a reader who takes the figure as already-per-second, or who divides by the wrong 20, lands on a
 * number that looks like it was derived. The corpus check (#210) holds the other end of this: 1,200
 * mB/s against the boiler's 60 mB/s is ADR-0050's "one pump feeds twenty boilers".
 *
 * <p><b>Trap two, for anyone re-deriving the boiler's side of that ratio:</b> the governing constant
 * is <em>steam's</em> heat capacity of 0.2 kJ, not water's 2 kJ. Water's is six times larger and
 * yields about 10 units a second, which is wrong by a factor of six and entirely believable. That
 * arithmetic lives in {@code tests/factorio/test_resource_extract.py}; it is named here because this
 * is the class someone will have open when they go looking for it.
 *
 * <p>One unit is one millibucket, under the same rule the barrel stands on ({@link BarrelSpec}).
 */
public final class OffshorePumpSpec {

    /** Factorio's tick rate. An engine constant, not a property of any prototype. */
    public static final int FACTORIO_TICKS_PER_SECOND = 60;

    /** Minecraft's tick rate, which is what actually spends the fluid. */
    public static final int MINECRAFT_TICKS_PER_SECOND = 20;

    private OffshorePumpSpec() {
    }

    /** Factorio's per-tick figure as millibuckets a second: 20 becomes 1,200. */
    public static int milliBucketsPerSecond(int pumpingSpeedPerFactorioTick) {
        return pumpingSpeedPerFactorioTick * FACTORIO_TICKS_PER_SECOND;
    }

    /** What one Minecraft tick may produce: 60 mB, at Factorio's own rate. */
    public static int milliBucketsPerTick(int pumpingSpeedPerFactorioTick) {
        return milliBucketsPerSecond(pumpingSpeedPerFactorioTick) / MINECRAFT_TICKS_PER_SECOND;
    }
}
