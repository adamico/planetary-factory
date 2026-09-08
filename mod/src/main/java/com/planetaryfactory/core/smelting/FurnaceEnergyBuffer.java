package com.planetaryfactory.core.smelting;

/**
 * The Electric Furnace's EU buffer (#155), which is the whole of what the supply-area pole sees.
 *
 * <p>ADR-0036's pole water-fills EU across the machines in its area, reading each one's
 * {@code getEnergyCanBeInserted()} and paying with {@code addEnergy}. This class is that surface,
 * with GregTech's {@code IEnergyContainer} left to the block entity so the arithmetic stays
 * Minecraft-free.
 *
 * <p><b>A cable cannot feed it.</b> {@link #acceptFromNetwork} returns 0 and
 * {@link #inputsEnergy()} is false, so the pole is the only route in. Two ways to power one block
 * is an ambiguity about which drains first for no mechanic in return -- the same reason FE was
 * rejected here in favour of EU.
 *
 * <p>Pure: no Minecraft types.
 */
public final class FurnaceEnergyBuffer {

    private final long capacityEu;
    private long storedEu;

    public FurnaceEnergyBuffer(long capacityEu) {
        this.capacityEu = Math.max(0L, capacityEu);
    }

    public long getEnergyStored() {
        return storedEu;
    }

    public long getEnergyCapacity() {
        return capacityEu;
    }

    public long getEnergyCanBeInserted() {
        return capacityEu - storedEu;
    }

    /** Takes what fits and reports it, which is what the pole debits itself by. */
    public long addEnergy(long eu) {
        return changeEnergy(eu);
    }

    /** Moves the buffer either way, clamped, and reports what actually moved. */
    public long changeEnergy(long delta) {
        long before = storedEu;
        storedEu = Math.max(0L, Math.min(capacityEu, storedEu + delta));
        return storedEu - before;
    }

    /**
     * Pays for one tick of operation, all or nothing.
     *
     * <p>A partial tick is not a tick: spending 7 EU towards a 13 EU tick would make a
     * half-supplied furnace consume power and never finish.
     */
    public boolean drawTick(long euPerTick) {
        if (euPerTick <= 0L) {
            return true;
        }
        if (storedEu < euPerTick) {
            return false;
        }
        storedEu -= euPerTick;
        return true;
    }

    /** The pole is the boundary: a GT cable run gets nothing. */
    public long acceptFromNetwork() {
        return 0L;
    }

    public boolean inputsEnergy() {
        return false;
    }

    public long inputVoltage() {
        return 0L;
    }

    public long inputAmperage() {
        return 0L;
    }

    /** For the block entity's save data. */
    public void setStoredEu(long eu) {
        storedEu = Math.max(0L, Math.min(capacityEu, eu));
    }
}
