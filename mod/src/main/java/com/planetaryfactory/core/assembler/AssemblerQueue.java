package com.planetaryfactory.core.assembler;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The Personal Assembler's queue: a serial list of Crafting Plans, ticked whether or not anybody is
 * looking at it (ADR-0038).
 *
 * <p>Three rules, and they are the whole class:
 *
 * <ul>
 *   <li><b>Start pays for everything.</b> A plan's entire raw cost leaves the inventory when it is
 *       enqueued, so nothing can change underneath a running plan and no re-validation is ever
 *       needed.
 *   <li><b>One at a time, and a blocked head does not step aside.</b> Serial execution is the only
 *       thing making hand-crafting slow once ADR-0029 removed the multiplier, so letting a stalled
 *       plan be overtaken would quietly hand the player parallel hand-crafting.
 *   <li><b>Pause, never drop.</b> A finished craft with nowhere to go stays in the plan's buffer and
 *       the whole queue waits, which is Factorio's own 0.15 behaviour.
 * </ul>
 *
 * <p>No Minecraft type appears here, which is what lets the ticket's real risk be checked by an
 * ordinary unit test. {@link PlayerItems} is the seam, and {@code InventoryPlayerItems} is the only
 * implementation that knows what an {@code ItemStack} is.
 */
public final class AssemblerQueue {

    private final List<QueuedPlan> entries = new ArrayList<>();
    private boolean blocked;

    public AssemblerQueue() {
    }

    /** Restores a queue from what was persisted, mid-plan and all. */
    public static AssemblerQueue of(List<QueuedPlan> entries) {
        AssemblerQueue queue = new AssemblerQueue();
        for (QueuedPlan entry : entries) {
            queue.entries.add(QueuedPlan.restored(
                    entry.plan(), entry.buffer(), entry.stepIndex(), entry.progressTicks()));
        }
        return queue;
    }

    /** The queue in order, head first. */
    public List<QueuedPlan> entries() {
        return List.copyOf(entries);
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /** The plan being crafted, if any. */
    public Optional<QueuedPlan> head() {
        return entries.isEmpty() ? Optional.empty() : Optional.of(entries.get(0));
    }

    /**
     * Whether the head is paused on a craft it cannot deliver.
     *
     * <p>Derived from the last tick and never persisted: a queue restored into an inventory with
     * room in it must not come back paused because it was paused when the player logged out.
     */
    public boolean isBlocked() {
        return blocked;
    }

    /**
     * Start: takes the plan's whole raw cost and appends it.
     *
     * <p>Returns false and takes nothing when the inventory does not cover the cost. That is a
     * belt-and-braces refusal rather than a case the player can reach -- the plan dialog refuses an
     * incomplete plan before it gets here -- but the check is what makes the reservation honest if
     * the inventory changed between the dialog and the click.
     */
    public boolean enqueue(CraftingPlan plan, PlayerItems items) {
        ItemBag cost = ItemBag.ofAmounts(plan.rawCost());
        for (ItemAmount owed : cost.amounts()) {
            if (items.count(owed.item()) < owed.count()) return false;
        }
        for (ItemAmount owed : cost.amounts()) {
            items.take(owed.item(), owed.count());
        }
        entries.add(new QueuedPlan(plan, cost, 0, 0));
        return true;
    }

    /**
     * One server tick of the head plan.
     *
     * <p>Delivery is attempted before any work is done, which is what makes a pause recover on its
     * own the moment the player frees a slot.
     */
    public void tick(PlayerItems items) {
        if (entries.isEmpty()) {
            blocked = false;
            return;
        }
        QueuedPlan head = entries.get(0);
        if (!deliver(head, items)) {
            blocked = true;
            return;
        }
        blocked = false;
        if (head.finished()) {
            entries.remove(0);
            return;
        }
        head.advanceTick();
        if (head.progressTicks() >= head.currentStep().durationTicks()) {
            head.completeStep();
            if (!deliver(head, items)) {
                blocked = true;
                return;
            }
            if (head.finished()) entries.remove(0);
        }
    }

    /**
     * Cancels a plan wherever it sits, refunding its buffer -- the unspent reservation and every
     * intermediate already made.
     *
     * <p>The plan is the unit, never an item inside it: cancelling one intermediate out of a
     * resolved plan would orphan everything downstream of it and leave a reservation matching
     * nothing (ADR-0038).
     */
    public CancelResult cancel(UUID planId, PlayerItems items) {
        for (int i = 0; i < entries.size(); i++) {
            QueuedPlan entry = entries.get(i);
            if (!entry.plan().id().equals(planId)) continue;
            List<ItemAmount> notReturned = new ArrayList<>();
            for (ItemAmount held : entry.held()) {
                if (!items.give(held.item(), held.count())) notReturned.add(held);
            }
            entries.remove(i);
            if (i == 0) blocked = false;
            return new CancelResult(true, List.copyOf(notReturned));
        }
        return new CancelResult(false, List.of());
    }

    /**
     * Hands the player everything in the buffer that no remaining step needs, and reports whether
     * all of it went.
     *
     * <p>Computing what is spare from the steps that are left, rather than marking outputs when the
     * plan was resolved, is what makes a recipe that over-produces work for free: two gears made for
     * a step that wants one leave the spare with the player on the tick it was crafted.
     */
    private boolean deliver(QueuedPlan entry, PlayerItems items) {
        ItemBag needed = new ItemBag();
        List<CraftStep> steps = entry.plan().steps();
        for (int i = entry.stepIndex(); i < steps.size(); i++) {
            for (ItemAmount input : steps.get(i).inputs()) {
                needed.add(input.item(), input.count());
            }
        }
        boolean allDelivered = true;
        for (ItemAmount held : entry.held()) {
            int spare = held.count() - needed.count(held.item());
            if (spare <= 0) continue;
            if (items.give(held.item(), spare)) {
                entry.takeFromBuffer(held.item(), spare);
            } else {
                allDelivered = false;
            }
        }
        return allDelivered;
    }

    /**
     * What a cancellation did. {@code notReturned} is what would not fit -- the caller's to deal
     * with, since a cancellation is the player's own action and dropping at their feet is a fair
     * answer there in a way it never is for a finished craft.
     */
    public record CancelResult(boolean cancelled, List<ItemAmount> notReturned) {
    }
}
