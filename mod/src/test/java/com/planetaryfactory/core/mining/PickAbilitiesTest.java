package com.planetaryfactory.core.mining;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Which of GregTech's wrench verbs the Engineer's Pick answers to (#168).
 *
 * <p>Both halves are asserted, and the false half is the one worth having: a blanket
 * {@code return true} passes any check that only asks about rotation, while quietly granting the
 * four configure verbs that ADR-0017 declined. What is declined has to be asserted as declined.
 *
 * <p>This names abilities by string because the test classpath has neither Minecraft nor GregTech
 * on it -- which is the same reason {@link PickAbilities} does. The strings are GTCEu 7.0.2's own,
 * read out of {@code GTItemAbilities}.
 */
class PickAbilitiesTest {

    @Test
    void rotateIsGranted() {
        assertTrue(PickAbilities.grants("wrench_rotate"));
    }

    @Test
    void theConfigureVerbsAreGranted() {
        // A machine's auto-output face, not a pipe's connection. GregTech reads wrench_configure
        // first and the other three only after it, so the gate has to be in the set or the rest
        // perform nothing.
        assertTrue(PickAbilities.grants("wrench_configure"));
        assertTrue(PickAbilities.grants("wrench_configure_all"));
        assertTrue(PickAbilities.grants("wrench_configure_items"));
        assertTrue(PickAbilities.grants("wrench_configure_fluids"));
    }

    @Test
    void theConnectVerbIsDeclined() {
        // This is the actual pipe-connection verb, on the pipe block's own path. ADR-0017 gives
        // fluid and item logistics to Create and GT's pipes went with the power layer, so it would
        // be declared against blocks the pack does not ship.
        assertFalse(PickAbilities.grants("wrench_connect"));
    }

    @Test
    void dismantleIsNotClaimedHere() {
        // Dismantle works, but it does not come from an ability: it rides the ordinary break path
        // that isCorrectToolForDrops already answers, granted by the two wrench item tags in
        // kubejs/data (ADR-0039). Claiming it here would be a second, redundant route to a verb
        // the pack already has -- and a misleading one, since it would suggest the tags are not
        // what delivers it.
        assertFalse(PickAbilities.grants("wrench_dismantle"));
        assertFalse(PickAbilities.grants("wrench_dig"));
    }

    @Test
    void everythingElseIsDeclined() {
        // The Pick is the pack's only tool, so it is asked about every ability any mod defines.
        assertFalse(PickAbilities.grants("axe_strip"));
        assertFalse(PickAbilities.grants("shovel_flatten"));
        assertFalse(PickAbilities.grants("hoe_till"));
        assertFalse(PickAbilities.grants("shears_harvest"));
        assertFalse(PickAbilities.grants("mallet_pause"));
        assertFalse(PickAbilities.grants(""));
    }
}
