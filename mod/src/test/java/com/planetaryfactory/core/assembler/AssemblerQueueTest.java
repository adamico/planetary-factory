package com.planetaryfactory.core.assembler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * The Personal Assembler's queue, ADR-0038.
 *
 * <p>Everything the ADR decided about a running plan is asserted here rather than in a world: the
 * reservation is taken whole at Start, the plan is the unit of cancellation, and a craft that will
 * not fit pauses the head instead of dropping on the ground. The queue is plain state with no
 * Minecraft type in it, which is why this is a unit test and not a GameTest.
 */
class AssemblerQueueTest {

    private static final String IRON = "iron_plate";
    private static final String GEAR = "iron_gear";
    private static final String BELT = "transport_belt";

    /** One gear: two plates in, one gear out. */
    private static CraftStep gearStep() {
        return new CraftStep("gear", List.of(new ItemAmount(IRON, 2)), List.of(new ItemAmount(GEAR, 1)), 2);
    }

    /** One belt: a gear and a plate in, two belts out -- Factorio's own numbers. */
    private static CraftStep beltStep() {
        return new CraftStep(
                "belt",
                List.of(new ItemAmount(GEAR, 1), new ItemAmount(IRON, 1)),
                List.of(new ItemAmount(BELT, 2)),
                2);
    }

    /** A two-step chain: make the gear, then the belt. Raw cost is three plates. */
    private static CraftingPlan beltPlan() {
        return new CraftingPlan(
                UUID.nameUUIDFromBytes("belt".getBytes()),
                BELT,
                2,
                List.of(new ItemAmount(IRON, 3)),
                List.of(gearStep(), beltStep()));
    }

    private static CraftingPlan gearPlan() {
        return new CraftingPlan(
                UUID.nameUUIDFromBytes("gear".getBytes()),
                GEAR,
                1,
                List.of(new ItemAmount(IRON, 2)),
                List.of(gearStep()));
    }

    private static void tick(AssemblerQueue queue, PlayerItems items, int times) {
        for (int i = 0; i < times; i++) queue.tick(items);
    }

    @Test
    void startTakesTheWholeRawCostAtOnce() {
        TestPlayerItems items = new TestPlayerItems().with(IRON, 10);
        AssemblerQueue queue = new AssemblerQueue();

        assertTrue(queue.enqueue(beltPlan(), items));

        assertEquals(7, items.count(IRON), "the plan's entire raw cost leaves the inventory at Start");
        assertEquals(1, queue.entries().size());
    }

    @Test
    void startIsRefusedWhenTheInventoryCannotCoverTheCost() {
        TestPlayerItems items = new TestPlayerItems().with(IRON, 2);
        AssemblerQueue queue = new AssemblerQueue();

        assertFalse(queue.enqueue(beltPlan(), items));

        assertEquals(2, items.count(IRON), "a refused Start takes nothing");
        assertTrue(queue.isEmpty());
    }

    @Test
    void aChainRunsItsStepsInOrderAndDeliversOnlyTheRoot() {
        TestPlayerItems items = new TestPlayerItems().with(IRON, 3);
        AssemblerQueue queue = new AssemblerQueue();
        queue.enqueue(beltPlan(), items);

        tick(queue, items, 2);
        assertEquals(0, items.count(GEAR), "the intermediate is the next step's input, not a delivery");

        tick(queue, items, 2);
        assertEquals(2, items.count(BELT));
        assertEquals(0, items.count(GEAR));

        queue.tick(items);
        assertTrue(queue.isEmpty(), "a plan whose last step delivered leaves the queue");
    }

    @Test
    void surplusFromAnIntermediateIsDelivered() {
        // The gear step makes two, the belt step wants one: the spare is the player's.
        CraftStep pairOfGears =
                new CraftStep("gear", List.of(new ItemAmount(IRON, 2)), List.of(new ItemAmount(GEAR, 2)), 1);
        CraftingPlan plan = new CraftingPlan(
                UUID.randomUUID(), BELT, 2, List.of(new ItemAmount(IRON, 3)), List.of(pairOfGears, beltStep()));
        TestPlayerItems items = new TestPlayerItems().with(IRON, 3);
        AssemblerQueue queue = new AssemblerQueue();
        queue.enqueue(plan, items);

        queue.tick(items);

        assertEquals(1, items.count(GEAR), "what no remaining step needs goes to the player at once");
    }

    @Test
    void cancellingRefundsTheRemainingReservationAndTheIntermediatesAlreadyMade() {
        TestPlayerItems items = new TestPlayerItems().with(IRON, 3);
        AssemblerQueue queue = new AssemblerQueue();
        CraftingPlan plan = beltPlan();
        queue.enqueue(plan, items);
        tick(queue, items, 2); // the gear is made and held; one plate of the reservation is unspent

        AssemblerQueue.CancelResult result = queue.cancel(plan.id(), items);

        assertTrue(result.cancelled());
        assertTrue(result.notReturned().isEmpty());
        assertEquals(1, items.count(IRON), "the unspent reservation comes back");
        assertEquals(1, items.count(GEAR), "so does the intermediate already produced");
        assertTrue(queue.isEmpty());
    }

    @Test
    void cancellingAPlanThatHasNotStartedRefundsItsWholeReservation() {
        TestPlayerItems items = new TestPlayerItems().with(IRON, 5);
        AssemblerQueue queue = new AssemblerQueue();
        CraftingPlan first = beltPlan();
        CraftingPlan second = gearPlan();
        queue.enqueue(first, items);
        queue.enqueue(second, items);
        assertEquals(0, items.count(IRON));

        AssemblerQueue.CancelResult result = queue.cancel(second.id(), items);

        assertTrue(result.cancelled());
        assertEquals(2, items.count(IRON));
        assertEquals(List.of(first.id()), queue.entries().stream().map(e -> e.plan().id()).toList());
    }

    @Test
    void cancellingAnUnknownPlanChangesNothing() {
        TestPlayerItems items = new TestPlayerItems().with(IRON, 3);
        AssemblerQueue queue = new AssemblerQueue();
        queue.enqueue(beltPlan(), items);

        AssemblerQueue.CancelResult result = queue.cancel(UUID.randomUUID(), items);

        assertFalse(result.cancelled());
        assertEquals(1, queue.entries().size());
        assertEquals(0, items.count(IRON));
    }

    @Test
    void aCraftThatWillNotFitPausesTheHeadAndDropsNothing() {
        // One slot, and the reservation vacates it, so the finished gear has nowhere to land.
        TestPlayerItems items = new TestPlayerItems(1, 64).with(IRON, 2);
        AssemblerQueue queue = new AssemblerQueue();
        queue.enqueue(gearPlan(), items);
        items.give("stone", 64); // the freed slot fills up while the craft runs

        tick(queue, items, 5);

        assertTrue(queue.isBlocked(), "the head pauses rather than dropping the craft");
        assertEquals(0, items.count(GEAR));
        assertEquals(1, queue.entries().size(), "and the plan stays on the queue, holding the gear");
    }

    @Test
    void aPausedHeadStopsTheWholeQueue() {
        TestPlayerItems items = new TestPlayerItems(1, 64).with(IRON, 4);
        AssemblerQueue queue = new AssemblerQueue();
        CraftingPlan first = gearPlan();
        CraftingPlan second = new CraftingPlan(
                UUID.randomUUID(), GEAR, 1, List.of(new ItemAmount(IRON, 2)), List.of(gearStep()));
        queue.enqueue(first, items);
        queue.enqueue(second, items);
        items.give("stone", 64);

        tick(queue, items, 20);

        assertEquals(0, items.count(GEAR));
        assertEquals(2, queue.entries().size(), "a blocked head does not step aside");
        assertEquals(0, queue.entries().get(1).stepIndex());
        assertEquals(0, queue.entries().get(1).progressTicks(), "the plan behind it never starts");
    }

    @Test
    void theHeadResumesWhenRoomAppears() {
        TestPlayerItems items = new TestPlayerItems(1, 64).with(IRON, 2);
        AssemblerQueue queue = new AssemblerQueue();
        queue.enqueue(gearPlan(), items);
        items.give("stone", 64);
        tick(queue, items, 5);
        assertTrue(queue.isBlocked());

        items.take("stone", 64);
        queue.tick(items);

        assertEquals(1, items.count(GEAR));
        assertFalse(queue.isBlocked());
    }

    @Test
    void aQueueRoundTripsThroughItsEntries() {
        TestPlayerItems items = new TestPlayerItems().with(IRON, 3);
        AssemblerQueue queue = new AssemblerQueue();
        queue.enqueue(beltPlan(), items);
        tick(queue, items, 3); // mid-plan: one step done, the next part-way through

        AssemblerQueue restored = AssemblerQueue.of(queue.entries());

        assertEquals(queue.entries(), restored.entries());
        assertEquals(1, restored.entries().get(0).stepIndex());
        assertEquals(Map.of(GEAR, 1, IRON, 1), restored.entries().get(0).buffer());

        // And it goes on running from where it stopped, rather than from the start.
        tick(restored, items, 1);
        assertEquals(2, items.count(BELT));
    }

    @Test
    void aStepTheReservationCannotFeedFailsLoudly() {
        // A plan whose raw cost does not cover its own first step: only a resolver can produce this,
        // and the alternative to throwing is crafting a gear out of nothing.
        CraftingPlan underpaid = new CraftingPlan(
                UUID.randomUUID(), GEAR, 1, List.of(new ItemAmount(IRON, 1)), List.of(gearStep()));
        TestPlayerItems items = new TestPlayerItems().with(IRON, 1);
        AssemblerQueue queue = new AssemblerQueue();
        queue.enqueue(underpaid, items);

        assertThrows(IllegalStateException.class, () -> tick(queue, items, 2));
    }

    @Test
    void anEmptyQueueIsNeitherBlockedNorBusy() {
        AssemblerQueue queue = new AssemblerQueue();
        TestPlayerItems items = new TestPlayerItems();

        queue.tick(items);

        assertTrue(queue.isEmpty());
        assertFalse(queue.isBlocked());
        assertNotNull(queue.head());
        assertTrue(queue.head().isEmpty());
    }
}
