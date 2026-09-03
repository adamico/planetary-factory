package com.planetaryfactory.core.assembler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * The data attachment's round trip, which ADR-0038 asks for by name.
 *
 * <p>It is a unit test and not a world load because the codec is DataFixerUpper's rather than
 * Minecraft's, so nothing here needs a game. That matters for what the check can catch: a queue
 * whose codec drops a field does not crash, it comes back short -- a plan that was paid for and is
 * now simply gone, with no log line and nothing for a player to report but missing items.
 *
 * <p>{@code JsonOps} stands in for NBT. The queue's values are strings, ints and lists, all of which
 * both dynamic ops carry identically, so what this asserts about one holds for the other.
 */
class AssemblerCodecsTest {

    private static AssemblerQueue midPlanQueue() {
        CraftStep gear = new CraftStep(
                "gear", List.of(new ItemAmount("iron_plate", 2)), List.of(new ItemAmount("iron_gear", 1)), 40);
        CraftStep belt = new CraftStep(
                "belt",
                List.of(new ItemAmount("iron_gear", 1), new ItemAmount("iron_plate", 1)),
                List.of(new ItemAmount("transport_belt", 2)),
                10);
        CraftingPlan plan = new CraftingPlan(
                UUID.fromString("6f1b1e5e-0000-4000-8000-00000000abcd"),
                "transport_belt",
                2,
                List.of(new ItemAmount("iron_plate", 3)),
                List.of(gear, belt));
        TestPlayerItems items = new TestPlayerItems().with("iron_plate", 3);
        AssemblerQueue queue = new AssemblerQueue();
        queue.enqueue(plan, items);
        for (int i = 0; i < 41; i++) queue.tick(items); // the gear is made; the belt is part-way in
        return queue;
    }

    private static <T> T decode(com.mojang.serialization.Codec<T> codec, JsonElement json) {
        DataResult<com.mojang.datafixers.util.Pair<T, JsonElement>> result =
                codec.decode(JsonOps.INSTANCE, json);
        assertTrue(result.result().isPresent(), () -> "decode failed: " + result.error().orElseThrow().message());
        return result.result().orElseThrow().getFirst();
    }

    @Test
    void aQueueMidPlanSurvivesTheRoundTrip() {
        AssemblerQueue queue = midPlanQueue();

        JsonElement written = AssemblerCodecs.QUEUE
                .encodeStart(JsonOps.INSTANCE, queue)
                .result()
                .orElseThrow();
        AssemblerQueue restored = decode(AssemblerCodecs.QUEUE, written);

        assertEquals(queue.entries(), restored.entries());
    }

    @Test
    void theBufferSurvivesWithIt() {
        AssemblerQueue queue = midPlanQueue();

        JsonElement written = AssemblerCodecs.QUEUE
                .encodeStart(JsonOps.INSTANCE, queue)
                .result()
                .orElseThrow();
        AssemblerQueue restored = decode(AssemblerCodecs.QUEUE, written);

        assertEquals(
                Map.of("iron_gear", 1, "iron_plate", 1),
                restored.entries().get(0).buffer(),
                "the intermediate already made and the unspent reservation are both the player's");
        assertEquals(1, restored.entries().get(0).stepIndex());
    }

    @Test
    void aRestoredQueueGoesOnFromWhereItStopped() {
        AssemblerQueue queue = midPlanQueue();
        JsonElement written = AssemblerCodecs.QUEUE
                .encodeStart(JsonOps.INSTANCE, queue)
                .result()
                .orElseThrow();
        AssemblerQueue restored = decode(AssemblerCodecs.QUEUE, written);
        TestPlayerItems items = new TestPlayerItems();

        for (int i = 0; i < 11; i++) restored.tick(items);

        assertEquals(2, items.count("transport_belt"), "the plan finishes on the other side of a logout");
        assertTrue(restored.isEmpty());
    }

    @Test
    void anEmptyQueueRoundTripsToAnEmptyQueue() {
        JsonElement written = AssemblerCodecs.QUEUE
                .encodeStart(JsonOps.INSTANCE, new AssemblerQueue())
                .result()
                .orElseThrow();

        assertTrue(decode(AssemblerCodecs.QUEUE, written).isEmpty());
    }
}
