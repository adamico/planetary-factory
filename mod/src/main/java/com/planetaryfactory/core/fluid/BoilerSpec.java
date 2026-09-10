package com.planetaryfactory.core.fluid;

/**
 * The Boiler's arithmetic, converted once, here (#224, ADR-0048, ADR-0047).
 *
 * <p>Nothing in this class is chosen. Every input is read off the prototype by
 * {@link SteamChainCorpus} -- the 1.8 MW draw, the 165 °C target, water's 15 °C and steam's heat
 * capacity -- and what happens to them is arithmetic. It is written down in one place because
 * there are two ways to get it wrong and both produce a plausible number.
 *
 * <p><b>Trap one: the governing heat capacity is <em>steam's</em>, not water's.</b> Factorio pays
 * for the temperature rise at the capacity of the fluid that comes <em>out</em>: 0.2 kJ per unit
 * per degree, against water's 2 kJ. Water's is ten times larger, and using it would run the Boiler
 * at 6 mB/s -- a tenth of its rate, and entirely believable. {@code OffshorePumpSpec} names this
 * trap from the other end, because ADR-0050's "one pump feeds twenty boilers" is exactly the ratio
 * that breaks if this is wrong.
 *
 * <p><b>Trap two: {@code energy_consumption} is per second, and the buffer is drained per tick.</b>
 * The Boiler's own rate is 60 units a second and Minecraft's tick is a twentieth of one, so the
 * per-tick figures are 90,000 J and 3 mB. A reader who takes the joule figure as already-per-tick
 * gets a Boiler that eats twenty times its fuel and still makes steam, which no output check would
 * catch.
 *
 * <p>One unit is one millibucket, under the same rule the barrel stands on ({@link BarrelSpec}).
 *
 * <p>Pure: no Minecraft types.
 */
public final class BoilerSpec {

    /** Minecraft's tick rate, which is what actually spends the fuel and the water. */
    public static final int MINECRAFT_TICKS_PER_SECOND = 20;

    /** Factorio's own names for the two fluid boxes' roles, as the prototype states them. */
    public static final String INPUT = "input";
    public static final String OUTPUT = "output";

    private BoilerSpec() {
    }

    /**
     * What one tick of boiling costs, in joules: 1.8 MW over twenty ticks is 90,000 J.
     *
     * <p>{@code effectivity} is not applied here. Under ADR-0047 it multiplies the fuel item's
     * value on the way *in* to the buffer, not the draw on the way out --
     * {@link BoilerBlockEntity} is where it is spent.
     */
    public static long joulesPerTick(double energyConsumptionPerSecond) {
        return Math.round(energyConsumptionPerSecond / MINECRAFT_TICKS_PER_SECOND);
    }

    /**
     * What heating one unit of water to the target costs, in joules: 150 degrees at steam's
     * 0.2 kJ is 30,000 J.
     *
     * @param targetTemperature the Boiler's own {@code target_temperature}
     * @param sourceTemperature the water's {@code default_temperature}; nothing in this pack heats
     *     water before it arrives, so the input is always at it
     * @param steamHeatCapacity <em>steam's</em>, never water's -- see this class's trap one
     */
    public static long joulesPerMilliBucket(int targetTemperature, int sourceTemperature,
            double steamHeatCapacity) {
        int rise = targetTemperature - sourceTemperature;
        if (rise <= 0) {
            // A target at or below the water's own temperature is free steam in unlimited
            // quantity. It cannot arise from Factorio's prototype, which is why it is a refusal
            // rather than a branch: a corpus that ever said so is a corpus somebody should read.
            throw new IllegalArgumentException(
                    "a boiler targeting " + targetTemperature + " °C from water at "
                            + sourceTemperature + " °C would make steam for nothing");
        }
        return Math.round(rise * steamHeatCapacity);
    }

    /** Factorio's own figure: 60 mB a second, which is a twentieth of one Offshore Pump. */
    public static int milliBucketsPerSecond(double energyConsumptionPerSecond, long joulesPerUnit) {
        return (int) (Math.round(energyConsumptionPerSecond) / joulesPerUnit);
    }

    /**
     * What one Minecraft tick converts: 3 mB of water into 3 mB of steam.
     *
     * <p>Water in equals steam out, unit for unit. Factorio's boiler is a temperature change and
     * not a reaction, so there is no ratio here to get wrong -- what the two fluids differ in is
     * heat capacity, and that is already spent above.
     */
    public static int milliBucketsPerTick(long joulesPerTick, long joulesPerUnit) {
        if (joulesPerTick % joulesPerUnit != 0) {
            // Refused rather than truncated, the same way a non-positive rise above is. Integer
            // division here would boil at a rate nobody chose and nothing states -- 2 mB a tick
            // instead of 2.9 is a machine running at two thirds of its prototype, with no
            // refusal, no log line and a plausible number on the gauge.
            throw new IllegalArgumentException(
                    joulesPerTick + " J a tick is not a whole number of " + joulesPerUnit
                            + " J units -- the prototype's rate no longer lands on Minecraft's tick");
        }
        return (int) (joulesPerTick / joulesPerUnit);
    }
}
