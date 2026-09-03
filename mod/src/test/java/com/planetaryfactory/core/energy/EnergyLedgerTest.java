package com.planetaryfactory.core.energy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The pole is FE-in and EU-out: Power Grid's Device Connector hands out FE, and every machine in
 * the pack takes EU (ADR-0035, and #155's Electric Furnace corrected to EU alongside it). The
 * ledger is the only place the two meet, and its whole job is to not invent energy while doing it.
 *
 * <p>The ratio is GTCEu's own {@code feToEuRatio}, 4 FE to 1 EU. Integer division means a partial
 * EU has to be <em>retained</em> rather than rounded away, or a pole fed a trickle would burn it
 * and emit nothing.
 */
class EnergyLedgerTest {

    private static final long CAP = 1_000_000L;

    @Test
    void fourFeBuysOneEu() {
        EnergyLedger ledger = new EnergyLedger(CAP);
        ledger.receiveFe(4);
        assertEquals(1, ledger.availableEu());
    }

    @Test
    void aPartialEuIsRetainedRatherThanRoundedAway() {
        EnergyLedger ledger = new EnergyLedger(CAP);
        ledger.receiveFe(3);
        assertEquals(0, ledger.availableEu());
        ledger.receiveFe(1);
        assertEquals(1, ledger.availableEu(), "the three FE must still have been there");
    }

    @Test
    void drainingRemovesExactlyTheFeItWasWorth() {
        EnergyLedger ledger = new EnergyLedger(CAP);
        ledger.receiveFe(10);
        assertEquals(2, ledger.drainEu(2));
        assertEquals(2, ledger.storedFe(), "the leftover 2 FE stays on the books");
    }

    @Test
    void drainingMoreThanIsStoredYieldsOnlyWhatIsStored() {
        EnergyLedger ledger = new EnergyLedger(CAP);
        ledger.receiveFe(8);
        assertEquals(2, ledger.drainEu(50));
        assertEquals(0, ledger.storedFe());
    }

    @Test
    void receiveIsCappedAndReportsWhatItTook() {
        EnergyLedger ledger = new EnergyLedger(10);
        assertEquals(10, ledger.receiveFe(25));
        assertEquals(0, ledger.receiveFe(25));
        assertEquals(10, ledger.storedFe());
    }

    @Test
    void simulatingChangesNothing() {
        EnergyLedger ledger = new EnergyLedger(CAP);
        assertEquals(7, ledger.simulateReceiveFe(7));
        assertEquals(0, ledger.storedFe());
    }

    @Test
    void energyIsNeverCreated() {
        EnergyLedger ledger = new EnergyLedger(CAP);
        long fed = 0;
        for (int i = 0; i < 1000; i++) {
            fed += ledger.receiveFe(7);
        }
        long drained = ledger.drainEu(Long.MAX_VALUE);
        assertEquals(fed, drained * EnergyLedger.FE_PER_EU + ledger.storedFe());
    }

    @Test
    void negativeAmountsAreRefused() {
        EnergyLedger ledger = new EnergyLedger(CAP);
        assertEquals(0, ledger.receiveFe(-5));
        assertEquals(0, ledger.drainEu(-5));
    }
}
