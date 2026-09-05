package com.planetaryfactory.core.ore;

import java.util.List;

/**
 * Which of Factorio's eight sprite stages a block is showing.
 *
 * <p>ADR-0020 refused "ore blocks that thin out as they are mined" because a worn texture would
 * compete with the amount and the player would believe their eyes. ADR-0041 amends that: a stage
 * <em>computed from</em> the amount cannot compete with it, because it is the same reading rendered
 * twice. This class is that computation and nothing else.
 *
 * <p><b>The thresholds are fractions of a block's own initial amount</b>, not unit counts. Factorio
 * renders its stages against tiles holding fifteen thousand; Terra's blocks hold about a thousand,
 * so the counts do not port and the ratios do. They come from the corpus --
 * {@code data/factorio/resource.json}, {@code stage_ratios} -- and are passed in rather than
 * tabulated here, so no number in this file needs to agree with a document.
 */
public final class OreStage {

    private OreStage() {
    }

    /**
     * The stage index for a block, {@code 0} being untouched and the last being nearly gone.
     *
     * <p>Descending thresholds: a block is at stage {@code i} when its remaining fraction has
     * fallen below the {@code i}-th ratio. A block above the first threshold is at stage 0, and an
     * exhausted one is at the last stage rather than off the end of the list -- it is about to
     * break, and a missing sprite would be the more visible bug.
     */
    public static int stage(int remaining, int initial, List<Double> ratios) {
        if (ratios.isEmpty()) {
            return 0;
        }
        if (initial <= 0 || remaining <= 0) {
            return ratios.size() - 1;
        }
        double fraction = (double) remaining / initial;
        int stage = 0;
        for (int index = 1; index < ratios.size(); index++) {
            if (fraction < ratios.get(index)) {
                stage = index;
            }
        }
        return stage;
    }

    /** How many stages a ratio set carries, which is what the blockstate property is sized to. */
    public static int count(List<Double> ratios) {
        return Math.max(1, ratios.size());
    }
}
