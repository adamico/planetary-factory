package com.planetaryfactory.core.start;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.JsonOps;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The once-per-player rule (#203), which is the half of the starting kit that can go wrong quietly.
 *
 * <p>What a failure here reaches the player as: an unlimited iron supply, one relog at a time, and
 * every pace reading taken after the first login measuring a player who is eight plates richer than
 * the spec says. Nothing is logged either way, and an inventory holding the kit twice looks like an
 * inventory holding a kit that was never spent.
 */
class StartingGrantTest {

    @Test
    void aFreshPlayerIsGrantedTheWholeKit() {
        StartingGrant grant = new StartingGrant();

        assertFalse(grant.granted());
        assertEquals(StartingKit.ALL, grant.claim());
        assertTrue(grant.granted());
    }

    @Test
    void aSecondJoinIsGrantedNothing() {
        StartingGrant grant = new StartingGrant();
        grant.claim();

        assertEquals(List.of(), grant.claim());
    }

    /**
     * That a player who has <em>spent</em> the kit earns no second one is not asserted here, and
     * saying so is the point: the distinguishing case is an empty inventory, and this class cannot
     * hold one. What is asserted instead is the property that makes the spent case follow -- the
     * grant's only state is the flag, and no inventory is reachable from it. The spent case itself
     * is the human-on-delivery step #203 names: relog after placing the furnace, and be granted
     * nothing.
     */
    @Test
    void theOnlyStateIsTheFlag() {
        StartingGrant grant = new StartingGrant();
        grant.claim();

        assertTrue(grant.granted());
        assertEquals(List.of(), grant.claim());
        assertEquals(1, StartingGrant.class.getDeclaredFields().length,
                "a second field here is a second thing the grant could consult -- an inventory "
                        + "among them, which is what re-grants to the player who played the opening");
    }

    @Test
    void theFlagSurvivesTheRoundTrip() {
        StartingGrant granted = new StartingGrant();
        granted.claim();

        var written = StartingCodecs.GRANT.encodeStart(JsonOps.INSTANCE, granted)
                .getOrThrow(error -> new AssertionError("the grant would not encode: " + error));
        StartingGrant read = StartingCodecs.GRANT.parse(JsonOps.INSTANCE, written)
                .getOrThrow(error -> new AssertionError("the grant would not decode: " + error));

        assertTrue(read.granted(), "a flag lost over a logout is a second starting kit");
        assertEquals(List.of(), read.claim());
    }

    @Test
    void anUngrantedFlagAlsoSurvivesTheRoundTrip() {
        var written = StartingCodecs.GRANT.encodeStart(JsonOps.INSTANCE, new StartingGrant())
                .getOrThrow(error -> new AssertionError("the grant would not encode: " + error));
        StartingGrant read = StartingCodecs.GRANT.parse(JsonOps.INSTANCE, written)
                .getOrThrow(error -> new AssertionError("the grant would not decode: " + error));

        assertFalse(read.granted());
        assertEquals(StartingKit.ALL, read.claim());
    }

    @Test
    void theKitIsThePocketThenTheHold() {
        assertEquals(StartingKit.POCKET.size() + StartingKit.HOLD.size(), StartingKit.ALL.size());
        assertEquals(StartingKit.POCKET, StartingKit.ALL.subList(0, StartingKit.POCKET.size()));
    }
}
