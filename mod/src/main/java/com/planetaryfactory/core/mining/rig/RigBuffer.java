package com.planetaryfactory.core.mining.rig;

import javax.annotation.Nullable;

/**
 * The rig's small internal output buffer, and the stall it produces (#193).
 *
 * <p>ADR-0043 as amended by this ticket: a rig pushes onto the tile it faces, and <b>otherwise
 * fills this buffer and stops</b>. There is no third rule. The ground drop the ADR originally
 * carried is gone: no Factorio machine spills when its output is blocked -- a blocked drill fills
 * its output and halts, and that backpressure is the logistics puzzle -- and under ADR-0041's
 * amount model a spill risks losing a finite resource where a stall preserves it.
 *
 * <p><b>The load-bearing rule is the negative one.</b> {@link #canAccept(String)} is asked
 * <em>before</em> the draw, never after. A rig that mined into a full buffer would take a unit out
 * of the ground, fail to bank it, and have destroyed it -- the one outcome an amount model cannot
 * survive, and the reason this class exists rather than the block entity checking a stack inline.
 *
 * <p>One slot, one item id at a time, because it is a stack. A 2x2 straddling iron and copper
 * therefore stalls on the second resource until the first is taken. That is correct backpressure
 * rather than a case to special-case around, and the alternative -- swapping the held item --
 * would delete what was already banked.
 *
 * <p>Names its item by string and holds no {@code ItemStack}, which is what keeps it on the mod's
 * Minecraft-free test classpath; the block entity is the single place an id becomes a stack.
 */
public final class RigBuffer {

    private final int capacity;

    @Nullable
    private String itemId;

    private int count;

    public RigBuffer(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("a buffer holding " + capacity + " banks nothing");
        }
        this.capacity = capacity;
    }

    /**
     * Whether one more of this item would fit -- asked before an operation starts, so that ore the
     * buffer cannot take is left in the ground rather than drawn and dropped.
     */
    public boolean canAccept(String itemId) {
        if (this.itemId == null) {
            return true;
        }
        return this.itemId.equals(itemId) && count < capacity;
    }

    /** Bank one unit. Refuses rather than replaces, because {@link #canAccept} was the gate. */
    public void add(String itemId) {
        if (!canAccept(itemId)) {
            throw new IllegalStateException(
                    "the buffer holds " + count + " " + this.itemId + " and cannot take " + itemId);
        }
        this.itemId = itemId;
        count++;
    }

    /** Hand out up to {@code max} units, returning how many actually left. */
    public int remove(int max) {
        int taken = Math.min(Math.max(max, 0), count);
        count -= taken;
        if (count == 0) {
            // Losing the identity as well as the count is what lets the next resource in. A
            // buffer that stayed "iron, zero" would refuse copper forever over an empty slot.
            itemId = null;
        }
        return taken;
    }

    @Nullable
    public String itemId() {
        return itemId;
    }

    public int count() {
        return count;
    }

    public int capacity() {
        return capacity;
    }

    /**
     * Restore what was saved. A rig that logs out mid-stall comes back stalled and still holding
     * it; a buffer that came back empty would have voided ore across a logout with nothing in the
     * log.
     */
    public void load(@Nullable String itemId, int count) {
        if (itemId == null || itemId.isEmpty() || count <= 0) {
            this.itemId = null;
            this.count = 0;
            return;
        }
        this.itemId = itemId;
        this.count = Math.min(count, capacity);
    }
}
