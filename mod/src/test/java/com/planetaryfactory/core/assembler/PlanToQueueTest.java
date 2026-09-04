package com.planetaryfactory.core.assembler;

import static com.planetaryfactory.core.assembler.TestBags.asMap;
import static com.planetaryfactory.core.assembler.TestBags.have;
import static com.planetaryfactory.core.assembler.TestBags.stocked;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * The seam between #161's resolver and #160's queue: a plan the resolver calls complete must be one
 * the queue can actually run to the end.
 *
 * <p>It is asserted here because neither side can assert it alone, and because the failure is
 * invisible from either. {@code QueuedPlan.completeStep} throws when the buffer cannot feed a step,
 * which is the right answer to a resolver bug and a terrible thing to discover from a crash report:
 * the reservation would already have been taken and the player's items would be inside a plan that
 * cannot finish.
 */
class PlanToQueueTest {

    private static final RecipeGraph GRAPH = RecipeGraph.builder()
            .add(new HandRecipe("cable", List.of(new ItemAmount("copper", 1)),
                    List.of(new ItemAmount("cable", 2)), 5))
            .add(new HandRecipe("circuit", List.of(new ItemAmount("cable", 3), new ItemAmount("plate", 1)),
                    List.of(new ItemAmount("circuit", 1)), 10))
            .add(new HandRecipe("inserter", List.of(new ItemAmount("circuit", 1), new ItemAmount("gear", 1),
                    new ItemAmount("plate", 1)), List.of(new ItemAmount("inserter", 1)), 10))
            .add(new HandRecipe("gear", List.of(new ItemAmount("plate", 2)),
                    List.of(new ItemAmount("gear", 1)), 5))
            .build();



    /** Runs a plan to the end, and says how many ticks it took. Fails loudly if it never finishes. */
    private static int runToCompletion(AssemblerQueue queue, TestPlayerItems items) {
        for (int tick = 0; tick < 100_000; tick++) {
            if (queue.isEmpty()) return tick;
            queue.tick(items);
        }
        throw new AssertionError("the queue never emptied");
    }

    @Test
    void aThreeDeepChainResolvesAndThenRunsToTheEnd() {
        ItemBag inventory = have("copper", 2, "plate", 4);
        PlanResolver.Resolution resolution = new PlanResolver(GRAPH, Set.of()::contains)
                .resolve("inserter", 1, inventory);
        assertTrue(resolution.complete());

        TestPlayerItems items = stocked(inventory);
        AssemblerQueue queue = new AssemblerQueue();
        assertTrue(queue.enqueue(resolution.toPlan(UUID.randomUUID(), "inserter", 1), items));

        runToCompletion(queue, items);

        assertEquals(1, items.count("inserter"));
        // The cable recipe makes two and the circuit wants three, so the fourth is spare -- and it
        // reaches the player rather than vanishing with the plan.
        assertEquals(1, items.count("cable"));
        assertEquals(0, items.count("copper"));
        assertEquals(0, items.count("plate"));
    }

    @Test
    void everyStepOfEveryAffordablePlanIsFedByWhatCameBeforeIt() {
        // largestAffordable is the count Select Amount's `all` puts in front of the player, so it is
        // the count most likely to sit exactly on the edge of the reservation. Walking every one of
        // them is what shows the resolver's rounding never leaves a step short.
        PlanResolver resolver = new PlanResolver(GRAPH, Set.of()::contains);
        ItemBag inventory = have("copper", 17, "plate", 40);

        int all = resolver.largestAffordable("inserter", inventory);
        assertTrue(all > 1, "the fixture should afford several, got " + all);

        for (int amount = 1; amount <= all; amount++) {
            PlanResolver.Resolution resolution = resolver.resolve("inserter", amount, inventory);
            assertTrue(resolution.complete(), "plan for " + amount + " should be complete");

            TestPlayerItems items = stocked(inventory);
            AssemblerQueue queue = new AssemblerQueue();
            assertTrue(queue.enqueue(resolution.toPlan(UUID.randomUUID(), "inserter", amount), items));

            runToCompletion(queue, items);

            assertEquals(amount, items.count("inserter"), "plan for " + amount + " delivered short");
        }
    }

    @Test
    void aPlanCancelledPartWayThroughGivesBackEverythingItHeld() {
        ItemBag inventory = have("copper", 2, "plate", 4);
        PlanResolver.Resolution resolution = new PlanResolver(GRAPH, Set.of()::contains)
                .resolve("inserter", 1, inventory);
        UUID id = UUID.randomUUID();

        TestPlayerItems items = stocked(inventory);
        AssemblerQueue queue = new AssemblerQueue();
        queue.enqueue(resolution.toPlan(id, "inserter", 1), items);
        for (int tick = 0; tick < 12; tick++) queue.tick(items);

        assertTrue(queue.cancel(id, items).cancelled());

        // Whatever it is holding comes back -- as raw copper still unspent, or as the cable it had
        // already made. What must never happen is that the reservation simply disappears.
        assertTrue(items.count("copper") > 0 || items.count("cable") > 0,
                "cancelling mid-plan returned nothing at all");
        assertEquals(0, items.count("inserter"));
    }
}
