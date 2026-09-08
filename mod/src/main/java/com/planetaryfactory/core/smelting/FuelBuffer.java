package com.planetaryfactory.core.smelting;

/**
 * A burner furnace's fuel, held as joules rather than as burn ticks (ADR-0047, #187).
 *
 * <p>Lighting an item banks its whole {@code fuel_value}; a tick of work spends the tier's
 * {@code energy_usage / 20}. Burn time is the quotient and is never stored, which is why there is
 * no MJ-to-ticks constant anywhere in the ladder and so no rounding rule to defend.
 *
 * <p><b>A part-tick is not a tick.</b> {@link #drawTick} is all or nothing, the same rule
 * {@link FurnaceEnergyBuffer} keeps: a furnace that spent 4,000 J towards a 4,500 J tick would
 * consume fuel and make no progress. So coal's 4 MJ buys 888 whole ticks of Stone-tier work and
 * leaves 4,000 J <em>banked</em> -- Factorio's buffer keeps its remainder, and the next item
 * lights on top of it rather than replacing it.
 *
 * <p>The buffer is uncapped. Factorio's burner accepts one item at a time and this one does too,
 * so what it can hold is bounded by the largest fuel value plus the sliver left over from the
 * last -- there is no capacity a player could overfill and none to defend.
 *
 * <p>Pure: no Minecraft types.
 */
public final class FuelBuffer {

    private long storedJoules;

    /**
     * What the last item lit was worth. Not part of the arithmetic: it is the denominator the
     * gauge is drawn against, so an empty furnace fed one log shows a full bar rather than a
     * sliver of some absolute scale nothing in the world corresponds to.
     */
    private long lastLitJoules;

    public long storedJoules() {
        return storedJoules;
    }

    /** What the gauge fills to: the last item's worth, or the buffer itself if that is larger. */
    public long gaugeCapacity() {
        return Math.max(lastLitJoules, storedJoules);
    }

    /** Banks one fuel item, consumed whole. */
    public void light(long fuelValue) {
        if (fuelValue <= 0L) {
            return;
        }
        storedJoules += fuelValue;
        lastLitJoules = fuelValue;
    }

    /**
     * Pays for one tick of operation, all or nothing.
     *
     * @return false when the buffer cannot cover the whole tick, which is the caller's cue to
     *     light another item -- and, if there is none, to stall without spending anything
     */
    public boolean drawTick(long joulesPerTick) {
        if (joulesPerTick <= 0L) {
            return true;
        }
        if (storedJoules < joulesPerTick) {
            return false;
        }
        storedJoules -= joulesPerTick;
        return true;
    }

    /** Whether anything is alight, which is what the block's {@code lit} state reads. */
    public boolean isLit() {
        return storedJoules > 0L;
    }

    /**
     * For the block entity's save data.
     *
     * <p>Both fields persist. A buffer that came back empty over a logout is the silent loss
     * ADR-0038 asks the assembler's codecs to catch, and a gauge capacity that did not come back
     * would redraw a part-spent coal as a full one.
     */
    public void load(long storedJoules, long lastLitJoules) {
        this.storedJoules = Math.max(0L, storedJoules);
        this.lastLitJoules = Math.max(0L, lastLitJoules);
    }

    public long lastLitJoules() {
        return lastLitJoules;
    }
}
