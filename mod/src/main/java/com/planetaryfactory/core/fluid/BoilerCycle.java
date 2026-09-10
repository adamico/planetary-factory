package com.planetaryfactory.core.fluid;

import java.util.function.LongSupplier;

import com.planetaryfactory.core.smelting.FuelBuffer;

/**
 * One tick of the Boiler (#224, ADR-0048), and the order the three questions are asked in.
 *
 * <p>The rate is {@link BoilerSpec}'s. What is here is the <b>stall</b>, which #224 names as the
 * behaviour that matters as much as the rate: a Boiler whose steam has backed up makes no steam,
 * burns no fuel and voids none. A boiler that quietly eats coal into a full tank is a leak the
 * player cannot see, and it is the same rule {@code FurnaceCycle} and {@code RigBlockEntity} keep
 * for the two burners that came before this one.
 *
 * <p><b>Order is the whole mechanism.</b> Water and room are asked <em>before</em> the fuel buffer
 * is, so a blocked Boiler cannot light an item to pay for a tick it will not run. Asking them in
 * the other order still stalls -- and burns one item per stalled tick doing it.
 *
 * <p><b>All or nothing.</b> A tick that cannot convert its whole {@code perTick} converts none of
 * it, the same rule {@link FuelBuffer#drawTick} keeps for a part-paid tick. A part-filled tick
 * would spend a whole tick's joules on part of a tick's steam, which is a rate that drifts with
 * how full the neighbouring pipe happens to be.
 *
 * <p>Pure: no Minecraft types. The caller turns tanks into three ints and lighting an item into a
 * supplier.
 */
public final class BoilerCycle {

    private BoilerCycle() {
    }

    /**
     * Runs one tick.
     *
     * @param waterAvailable how much water is in the input tank, in millibuckets
     * @param outputRoom how much room the steam tank has left, in millibuckets
     * @param perTick what a whole tick converts -- {@link BoilerSpec#milliBucketsPerTick}
     * @param joulesPerTick what a whole tick costs -- {@link BoilerSpec#joulesPerTick}
     * @param fuel the burner model's buffer, shared with the Furnace and the rig (ADR-0047)
     * @param ignite consumes one fuel item and returns what it was worth, or 0 when there is
     *     nothing to light. Called at most once, and only on a tick that has already established
     *     it has both water and somewhere to put the steam.
     * @return how many millibuckets of water became steam: {@code perTick}, or zero
     */
    public static int tick(int waterAvailable, int outputRoom, int perTick, long joulesPerTick,
            FuelBuffer fuel, LongSupplier ignite) {
        if (perTick <= 0 || waterAvailable < perTick || outputRoom < perTick) {
            return 0;
        }
        if (!pay(joulesPerTick, fuel, ignite)) {
            return 0;
        }
        return perTick;
    }

    /**
     * The buffer first, an item only when it cannot cover the tick.
     *
     * <p>This is ADR-0047's rule and not an optimisation: lighting an item to top up a buffer that
     * would already have paid throws away the remainder Factorio's burner keeps, and coal's
     * 4 MJ would stop being 44 whole ticks of boiling.
     */
    private static boolean pay(long joulesPerTick, FuelBuffer fuel, LongSupplier ignite) {
        if (fuel.drawTick(joulesPerTick)) {
            return true;
        }
        long lit = ignite.getAsLong();
        if (lit <= 0L) {
            return false;
        }
        fuel.light(lit);
        return fuel.drawTick(joulesPerTick);
    }
}
