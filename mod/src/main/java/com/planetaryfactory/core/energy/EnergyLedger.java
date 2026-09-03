package com.planetaryfactory.core.energy;

/**
 * The pole's books, where FE becomes EU.
 *
 * <p>The pole is FE-in and EU-out. Power Grid's Device Connector is a one-way grid-to-FE block, so
 * FE is the only thing the grid can hand a pack block; every machine in the pack takes EU, the
 * Electric Furnace included (ADR-0035, and #155 corrected to EU alongside it). This class is the
 * only place the two currencies meet, and GTCEu's FE converters stay disabled because of it.
 *
 * <p>Its whole job is to not invent energy. The ratio is GTCEu's own {@code feToEuRatio} of 4, and
 * integer division means a partial EU has to be <em>retained</em> rather than rounded away -- a
 * pole fed a trickle would otherwise burn the trickle and emit nothing at all.
 *
 * <p>How big the books are allowed to get is not this class's business -- it takes a capacity and
 * respects it. {@code SupplyAreaPoleBlockEntity.BUFFER_FE} owns that number and the argument for it.
 *
 * <p>Pure: no Minecraft types, so the mod's Minecraft-free test source set can hold it to account.
 */
public final class EnergyLedger {

    /** GTCEu's {@code feToEuRatio} default, and the pack keeps it. */
    public static final long FE_PER_EU = 4L;

    private final long capacityFe;
    private long storedFe;

    public EnergyLedger(long capacityFe) {
        this.capacityFe = Math.max(0L, capacityFe);
    }

    public long capacityFe() {
        return capacityFe;
    }

    public long storedFe() {
        return storedFe;
    }

    /** EU the pole could hand out right now. The remainder below one EU stays on the books. */
    public long availableEu() {
        return storedFe / FE_PER_EU;
    }

    /** How much of an offered amount would be taken, without taking it. */
    public long simulateReceiveFe(long fe) {
        if (fe <= 0L) {
            return 0L;
        }
        return Math.min(fe, capacityFe - storedFe);
    }

    /** Takes what fits and reports it. */
    public long receiveFe(long fe) {
        long accepted = simulateReceiveFe(fe);
        storedFe += accepted;
        return accepted;
    }

    /** Hands out up to {@code maxEu}, debiting exactly the FE that EU was worth. */
    public long drainEu(long maxEu) {
        if (maxEu <= 0L) {
            return 0L;
        }
        long eu = Math.min(maxEu, availableEu());
        storedFe -= eu * FE_PER_EU;
        return eu;
    }

    /** For the block entity's save data. */
    public void setStoredFe(long fe) {
        storedFe = Math.max(0L, Math.min(fe, capacityFe));
    }
}
