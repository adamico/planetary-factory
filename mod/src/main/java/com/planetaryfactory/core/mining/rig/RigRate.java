package com.planetaryfactory.core.mining.rig;

/**
 * A rig's two rates, both of them read off the corpus rather than chosen (#193).
 *
 * <p>ADR-0043 asks for an <b>explicit operations-per-second</b> and gives the reason: Vulcanus's
 * Big Mining Drill varies it, and retrofitting a second quantity into a mechanism that welded them
 * is expensive where a dormant field is not. There is a nearer reason too. Factorio charges an
 * operation the drill's {@code mining_speed} against the <em>resource's</em> {@code mining_time},
 * so one rig already has two rates: uranium's {@code mining_time} of 2 costs the burner rig twice
 * what iron's 1 does. A speed baked into the block cannot say that.
 *
 * <p>The per-tick cost is ADR-0047's model unchanged -- the machine's own {@code energy_usage} over
 * twenty ticks, in joules, with no ADR-0029 conversion because nothing here is EU. The rig is the
 * third customer of that model after the two burner furnaces, and the reason it computes its own
 * figure rather than calling {@code PFFuel.joulesPerTick()} is that the latter is the furnace's
 * 4,500: the rig draws 150 kW where a furnace draws 90.
 *
 * <p>Free of Minecraft, so both derivations are checkable in the mod's ordinary test source set.
 */
public final class RigRate {

    private static final int TICKS_PER_SECOND = 20;

    private RigRate() {
    }

    /**
     * How many ticks one operation takes: the resource's {@code mining_time} over the drill's
     * {@code mining_speed}, in ticks.
     *
     * <p>Floored at one tick. A rig fast enough to finish inside a tick still spends one, because
     * an operation costing zero ticks is an operation nothing was charged for -- free ore rather
     * than a fast rig.
     *
     * @param miningSpeed the drill's own figure, e.g. {@code 0.25} for the burner rig
     * @param miningTime the resource's, e.g. {@code 1} for iron and {@code 2} for uranium
     */
    public static int operationTicks(double miningSpeed, double miningTime) {
        if (!(miningSpeed > 0) || !(miningTime > 0)) {
            // Both arrive from a generated resource, so a regeneration that emitted a null and a
            // reader that defaulted it to zero is the realistic way in. Throwing names the figure;
            // dividing would hand back an infinity that becomes a plausible-looking tick count.
            throw new IllegalArgumentException(
                    "a rig mines at " + miningSpeed + " against a resource costing " + miningTime
                            + " -- both must be positive");
        }
        return Math.max(1, (int) Math.ceil(TICKS_PER_SECOND * miningTime / miningSpeed));
    }

    /** What one tick of work costs, from the machine's {@code energy_usage} in watts. */
    public static long joulesPerTick(double watts) {
        return Math.round(watts / TICKS_PER_SECOND);
    }
}
