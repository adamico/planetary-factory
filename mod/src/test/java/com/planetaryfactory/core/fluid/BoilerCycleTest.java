package com.planetaryfactory.core.fluid;

import java.util.concurrent.atomic.AtomicInteger;

import com.planetaryfactory.core.smelting.FuelBuffer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The Boiler's stall (#224), which is the behaviour the ticket watches for as closely as the rate.
 *
 * <p>Three ways a tick can fail to run -- no water, no room, no fuel -- and one thing that must be
 * true of all three: <b>nothing is spent</b>. A boiler that burns coal into a full tank is a leak
 * with no symptom, and one that lights an item to pay for a tick it then refuses to run is the
 * same leak an item at a time.
 */
class BoilerCycleTest {

    private static final int PER_TICK = 3;
    private static final long JOULES_PER_TICK = 90_000L;
    private static final long COAL = 4_000_000L;

    /** A fuel slot holding {@code items} coal, which reports what it hands over. */
    private static final class Slot {
        private final AtomicInteger left;
        private final AtomicInteger lit = new AtomicInteger();

        Slot(int items) {
            this.left = new AtomicInteger(items);
        }

        long ignite() {
            if (left.get() <= 0) {
                return 0L;
            }
            left.decrementAndGet();
            lit.incrementAndGet();
            return COAL;
        }
    }

    @Test
    @DisplayName("a fuelled Boiler with water and room converts a tick's worth")
    void runsATick() {
        Slot slot = new Slot(1);
        FuelBuffer fuel = new FuelBuffer();
        assertEquals(PER_TICK,
                BoilerCycle.tick(200, 200, PER_TICK, JOULES_PER_TICK, fuel, slot::ignite));
        assertEquals(1, slot.lit.get());
        assertEquals(COAL - JOULES_PER_TICK, fuel.storedJoules());
    }

    @Test
    @DisplayName("a full output tank makes no steam, burns no fuel and lights nothing")
    void blockedOutputStalls() {
        Slot slot = new Slot(1);
        FuelBuffer fuel = new FuelBuffer();
        fuel.light(COAL);
        assertEquals(0, BoilerCycle.tick(200, 2, PER_TICK, JOULES_PER_TICK, fuel, slot::ignite));
        assertEquals(COAL, fuel.storedJoules(), "a blocked Boiler spent joules it had banked");
        assertEquals(0, slot.lit.get(), "a blocked Boiler lit an item");
    }

    @Test
    @DisplayName("it resumes the moment the steam is drained")
    void resumesWhenDrained() {
        Slot slot = new Slot(1);
        FuelBuffer fuel = new FuelBuffer();
        fuel.light(COAL);
        assertEquals(0, BoilerCycle.tick(200, 0, PER_TICK, JOULES_PER_TICK, fuel, slot::ignite));
        assertEquals(PER_TICK,
                BoilerCycle.tick(200, 200, PER_TICK, JOULES_PER_TICK, fuel, slot::ignite));
    }

    @Test
    @DisplayName("with no water it burns nothing")
    void noWaterBurnsNothing() {
        Slot slot = new Slot(1);
        FuelBuffer fuel = new FuelBuffer();
        fuel.light(COAL);
        assertEquals(0, BoilerCycle.tick(2, 200, PER_TICK, JOULES_PER_TICK, fuel, slot::ignite));
        assertEquals(COAL, fuel.storedJoules());
        assertEquals(0, slot.lit.get());
    }

    @Test
    @DisplayName("with no fuel it makes no steam and consumes no water")
    void noFuelMakesNoSteam() {
        Slot slot = new Slot(0);
        FuelBuffer fuel = new FuelBuffer();
        assertEquals(0, BoilerCycle.tick(200, 200, PER_TICK, JOULES_PER_TICK, fuel, slot::ignite));
        assertEquals(0L, fuel.storedJoules());
    }

    @Test
    @DisplayName("the buffer is spent before a second item is lit")
    void bufferBeforeItem() {
        Slot slot = new Slot(2);
        FuelBuffer fuel = new FuelBuffer();
        int ticks = (int) (COAL / JOULES_PER_TICK);
        for (int tick = 0; tick < ticks; tick++) {
            assertEquals(PER_TICK,
                    BoilerCycle.tick(200, 200, PER_TICK, JOULES_PER_TICK, fuel, slot::ignite));
        }
        // Coal's 4 MJ buys 44 whole ticks at 90,000 J and leaves 40,000 J banked, so the second
        // item is not lit until the 45th -- the remainder Factorio's burner keeps.
        assertEquals(1, slot.lit.get());
        assertEquals(COAL - ticks * JOULES_PER_TICK, fuel.storedJoules());
        assertEquals(PER_TICK,
                BoilerCycle.tick(200, 200, PER_TICK, JOULES_PER_TICK, fuel, slot::ignite));
        assertEquals(2, slot.lit.get());
    }
}
