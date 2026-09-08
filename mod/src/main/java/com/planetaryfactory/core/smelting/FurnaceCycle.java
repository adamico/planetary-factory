package com.planetaryfactory.core.smelting;

/**
 * The smelt clock (#155): progress, and the rule that a furnace which cannot deliver backs up.
 *
 * <p>A blocked output does not void the result, does not overflow and does not drop an item on
 * the ground. It stops the machine, which is Factorio's own answer and, under ADR-0041, the
 * load-bearing one: Terra's ore is finite, so a furnace that destroyed a result would destroy a
 * resource the world cannot re-make. The caller stops before this class is reached, which is what
 * keeps a stalled burner from burning coal for nothing while it waits.
 *
 * <p>Progress is <em>held</em> rather than decayed. Vanilla's furnace winds back down when its
 * input leaves, which is a fairness rule for a block a player stands in front of; a factory floor
 * wants the machine exactly where the belt left it when the belt moves again.
 *
 * <p>Pure: no Minecraft types.
 */
public final class FurnaceCycle {

    private int progress;

    /**
     * Advances one tick of a smelt that can run. A furnace that cannot calls {@link #idle} instead.
     *
     * @param powered whether this tick's fuel or EU was actually paid
     * @return true exactly on the tick the craft completes, at which point progress is back to zero
     */
    public boolean tick(boolean powered, int durationTicks) {
        if (!powered) {
            return false;
        }
        progress++;
        if (progress >= durationTicks) {
            progress = 0;
            return true;
        }
        return false;
    }

    /**
     * What to do with progress on a tick that made none.
     *
     * <p>The two reasons look alike from outside and are not the same. A furnace that still has
     * its recipe and merely cannot deliver <em>holds</em> -- it resumes where the belt left it.
     * One whose input was pulled or swapped has nothing left to finish, and carrying its progress
     * onto whatever is put in next would hand a player a free head start on a different smelt.
     *
     * @return true when progress was discarded, which the caller has to persist -- a reset that
     *     never reached disk is re-read as the progress it threw away
     */
    public boolean idle(boolean hasRecipe) {
        if (hasRecipe || progress == 0) {
            return false;
        }
        reset();
        return true;
    }

    public int progress() {
        return progress;
    }

    public void reset() {
        progress = 0;
    }

    /** For the block entity's save data. */
    public void setProgress(int progress) {
        this.progress = Math.max(0, progress);
    }
}
