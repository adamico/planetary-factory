package com.planetaryfactory.core.ore;

import java.util.HashMap;
import java.util.Map;

/**
 * What has already been drawn out of the ore blocks in one chunk.
 *
 * <p>ADR-0041 derives a block's <em>initial</em> amount from its position and stores only the
 * difference, because a starting field is around 1150 blocks and a block entity apiece is not
 * affordable at that scale. An untouched field therefore costs nothing: this map is empty until a
 * drill or a pick takes something out of it, and the entry retires again the moment the block is
 * gone.
 *
 * <p>Positions are packed longs rather than {@code BlockPos}, which is what keeps this class free
 * of Minecraft and so checkable in an ordinary unit test. The caller does the packing; nothing here
 * knows what the bits mean.
 *
 * <p><b>One number, two callers.</b> A hand break cycle and a drill operation both come through
 * {@link #draw}, which is the whole of ADR-0041's claim that "seconds per ore" is literal: there is
 * no second path that removes a block outright.
 */
public final class OreDelta {

    private final Map<Long, Integer> drawn;

    public OreDelta() {
        this(new HashMap<>());
    }

    private OreDelta(Map<Long, Integer> drawn) {
        this.drawn = drawn;
    }

    /** Restored from a codec: the map as it was written down. */
    public static OreDelta of(Map<Long, Integer> drawn) {
        return new OreDelta(new HashMap<>(drawn));
    }

    /** What a codec writes. Never contains a position whose block is exhausted or retired. */
    public Map<Long, Integer> drawn() {
        return Map.copyOf(drawn);
    }

    public boolean isEmpty() {
        return drawn.isEmpty();
    }

    /**
     * The units still in the block at {@code pos}, given the initial amount its position derives.
     *
     * <p>Reading does not write: a block nobody has touched stays absent from the map, which is
     * what makes an untouched patch free.
     */
    public int remaining(long pos, int initial) {
        return Math.max(0, initial - drawn.getOrDefault(pos, 0));
    }

    /**
     * Take one unit.
     *
     * <p>The last unit is <em>paid</em> and the block is then exhausted, rather than the break
     * consuming it -- a block that swallowed its last ore would make the field pay out one less
     * than it holds, once per block, which is a percent of the patch nobody could account for.
     *
     * <p>An exhausted position retires its entry here rather than at the caller: the block is about
     * to stop existing, and an entry left behind would be inherited by whatever is placed there
     * next.
     */
    public Draw draw(long pos, int initial) {
        int remaining = remaining(pos, initial);
        if (remaining <= 0) {
            drawn.remove(pos);
            return new Draw(0, 0, true);
        }
        int left = remaining - 1;
        if (left == 0) {
            drawn.remove(pos);
            return new Draw(1, 0, true);
        }
        drawn.put(pos, initial - left);
        return new Draw(1, left, false);
    }

    /**
     * Drop the entry for a position whose block has gone away for some reason other than depletion
     * -- TNT, a creative break, a structure overwriting it.
     *
     * <p>ADR-0041 asks for this by name. Without it a later ore block at the same position inherits
     * a stranger's delta and arrives part-mined, which is invisible until a player mines a fresh
     * patch that pays out half.
     */
    public void retire(long pos) {
        drawn.remove(pos);
    }

    /**
     * The result of a draw: what was paid out, what is left, and whether the block is now gone.
     *
     * @param paid units handed to the caller -- one, or none from an exhausted block
     * @param remaining units still in the block
     * @param exhausted whether the block should now break
     */
    public record Draw(int paid, int remaining, boolean exhausted) {
    }
}
