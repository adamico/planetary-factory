package com.planetaryfactory.core.ore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The chunk attachment's round trip, which ADR-0038 asks of every attachment the pack ships.
 *
 * <p>A unit test rather than a world load because the codec is DataFixerUpper's; {@code JsonOps}
 * stands in for NBT, and the delta's values are longs and ints, which both dynamic ops carry the
 * same way. What it catches is the silent one: a dropped field is a patch that refilled over a
 * logout, and the player's only symptom is that the ore lasted longer than it should have.
 */
class OreCodecsTest {

    @Test
    void aPartMinedChunkComesBackPartMined() {
        OreDelta delta = new OreDelta();
        delta.draw(1L, 1000);
        delta.draw(1L, 1000);
        delta.draw(1L << 40, 500);

        OreDelta restored = roundTrip(delta);

        assertEquals(998, restored.remaining(1L, 1000));
        assertEquals(499, restored.remaining(1L << 40, 500));
    }

    @Test
    void anUntouchedChunkRoundTripsEmpty() {
        OreDelta restored = roundTrip(new OreDelta());

        assertTrue(restored.isEmpty());
        assertEquals(1150, restored.remaining(7L, 1150));
    }

    @Test
    void aNegativePositionSurvives() {
        OreDelta delta = new OreDelta();
        delta.draw(-4_000_000_007L, 12);

        assertEquals(11, roundTrip(delta).remaining(-4_000_000_007L, 12),
                "half the world has a negative coordinate");
    }

    @Test
    void theStageLadderFallsWithTheAmount() {
        List<Double> ratios = List.of(1.0, 0.633, 0.367, 0.193, 0.087, 0.027, 0.010, 0.0053);

        assertEquals(0, OreStage.stage(1000, 1000, ratios), "untouched shows the full sprite");
        assertEquals(1, OreStage.stage(500, 1000, ratios));
        assertEquals(4, OreStage.stage(50, 1000, ratios));
        assertEquals(7, OreStage.stage(1, 1000, ratios), "one unit left is the last stage");
        assertEquals(7, OreStage.stage(0, 1000, ratios), "and so is none");
    }

    @Test
    void aStagelessResourceStaysAtOneStage() {
        assertEquals(0, OreStage.stage(5, 10, List.of()));
        assertEquals(1, OreStage.count(List.of()));
    }

    private static OreDelta roundTrip(OreDelta delta) {
        DataResult<JsonElement> written = OreCodecs.DELTA.encodeStart(JsonOps.INSTANCE, delta);
        JsonElement json = written.getOrThrow(error -> new AssertionError("encode: " + error));
        return OreCodecs.DELTA.parse(JsonOps.INSTANCE, json)
                .getOrThrow(error -> new AssertionError("decode: " + error));
    }
}
