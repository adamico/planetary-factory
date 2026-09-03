package com.planetaryfactory.core.assembler;

/**
 * The player's inventory, as much of it as the Assembler's queue needs: a multiset that can also be
 * full.
 *
 * <p>Being full is the whole reason this is an interface rather than a map. "A finished craft that
 * cannot fit pauses the head" (ADR-0038) is a rule about slots, and a rule about slots cannot be
 * asserted against something that always has room.
 */
public interface PlayerItems {

    /** How many of an item are held. */
    int count(String item);

    /** Removes up to {@code count} and reports how many actually went. */
    int take(String item, int count);

    /**
     * Inserts all of {@code count}, or none of it.
     *
     * <p>All-or-nothing because a partial delivery has nowhere to record the remainder: the queue's
     * only two states for a finished craft are delivered and paused, and half-delivering would
     * invent a third that the reservation cannot account for.
     */
    boolean give(String item, int count);
}
