package com.planetaryfactory.core.energy;

import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * The pole's FE face -- the whole of the V-to-machine boundary, as an edge of the pole rather than
 * a block of its own (ADR-0036).
 *
 * <p>ADR-0017 taught that boundary by making the player place a Converter. Factorio has no such
 * object: the boundary is simply where the supply area ends. The pack took Factorio's arrangement,
 * so the lesson survives and the block that taught it does not.
 *
 * <p>Receive-only, deliberately. Power Grid's Device Connector is grid-to-FE and never reads back,
 * and letting anything pull FE out of a pole would make the pole a battery -- which is the
 * machine-side storage ADR-0036 rules out.
 *
 * <p>{@code int} throughout because that is NeoForge's interface. The ledger keeps {@code long}, so
 * the two clamp at the boundary rather than overflowing across it.
 */
public final class PoleEnergyStorage implements IEnergyStorage {

    private final SupplyAreaPoleBlockEntity pole;

    PoleEnergyStorage(SupplyAreaPoleBlockEntity pole) {
        this.pole = pole;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        EnergyLedger ledger = pole.ledger();
        if (simulate) {
            return (int) Math.min(Integer.MAX_VALUE, ledger.simulateReceiveFe(maxReceive));
        }
        long taken = ledger.receiveFe(maxReceive);
        if (taken > 0L) {
            pole.setChanged();
        }
        return (int) Math.min(Integer.MAX_VALUE, taken);
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        return 0;
    }

    @Override
    public int getEnergyStored() {
        return (int) Math.min(Integer.MAX_VALUE, pole.ledger().storedFe());
    }

    @Override
    public int getMaxEnergyStored() {
        return (int) Math.min(Integer.MAX_VALUE, pole.ledger().capacityFe());
    }

    @Override
    public boolean canExtract() {
        return false;
    }

    @Override
    public boolean canReceive() {
        return true;
    }
}
