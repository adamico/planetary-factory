package com.planetaryfactory.core.smelting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The Electric tier's buffer, which is what ADR-0036's supply-area pole water-fills against
 * (#147, #155). The pole reads {@code getEnergyCanBeInserted()} and pays with {@code addEnergy}.
 */
class FurnaceEnergyBufferTest {

    @Test
    void anEmptyBufferAsksForOneWholeCraft() {
        FurnaceEnergyBuffer buffer = new FurnaceEnergyBuffer(2080L);
        assertEquals(2080L, buffer.getEnergyCanBeInserted());
        assertEquals(0L, buffer.getEnergyStored());
    }

    @Test
    void itTakesOnlyWhatFits() {
        FurnaceEnergyBuffer buffer = new FurnaceEnergyBuffer(2080L);
        assertEquals(2000L, buffer.addEnergy(2000L));
        assertEquals(80L, buffer.addEnergy(500L));
        assertEquals(2080L, buffer.getEnergyStored());
        assertEquals(0L, buffer.getEnergyCanBeInserted());
    }

    @Test
    void aTickIsDrawnOnlyIfItIsWhollyThere() {
        FurnaceEnergyBuffer buffer = new FurnaceEnergyBuffer(2080L);
        buffer.addEnergy(20L);
        assertTrue(buffer.drawTick(13L));
        assertEquals(7L, buffer.getEnergyStored());
        assertFalse(buffer.drawTick(13L), "a partial tick is not a tick; the smelt waits");
        assertEquals(7L, buffer.getEnergyStored());
    }

    /**
     * The pole is the boundary (ADR-0036), so a GT cable run cannot feed this block: two ways to
     * power one machine is an ambiguity about which drains first, for no mechanic.
     */
    @Test
    void aCableIsRefused() {
        FurnaceEnergyBuffer buffer = new FurnaceEnergyBuffer(2080L);
        assertEquals(0L, buffer.acceptFromNetwork(32L, 4L));
        assertEquals(0L, buffer.getEnergyStored());
        assertFalse(buffer.inputsEnergy());
        assertEquals(0L, buffer.inputVoltage());
        assertEquals(0L, buffer.inputAmperage());
    }

    @Test
    void changeEnergyMovesBothWaysAndReportsWhatMoved() {
        FurnaceEnergyBuffer buffer = new FurnaceEnergyBuffer(100L);
        assertEquals(40L, buffer.changeEnergy(40L));
        assertEquals(-40L, buffer.changeEnergy(-90L));
        assertEquals(0L, buffer.getEnergyStored());
    }
}
