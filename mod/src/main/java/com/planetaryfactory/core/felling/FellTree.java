package com.planetaryfactory.core.felling;

import java.util.Set;

/**
 * One tree, as the gesture found it.
 *
 * <p>{@link #amount} is ADR-0051's amount -- the log count of the tree actually broken, which is the
 * pack's deliberate divergence from Factorio's flat {@code wood x4}. It is the log set's size and
 * not a second number, because the charge and the payout have to be the same count: the break-speed
 * modifier charges {@code amount * rate} before the break lands and this set is removed after it, so
 * a tree that costs more than it gives is what a second number would eventually produce.
 *
 * @param logs    the logs to remove, and the amount
 * @param leaves  the leaves to remove with them -- no drops, no decay ticks (ADR-0051)
 * @param bounded whether a bound cut the fill short, leaving part of the tree standing
 */
public record FellTree(Set<FellPos> logs, Set<FellPos> leaves, boolean bounded) {

    /** The gesture found nothing to fell: not a base, not a tree, or a build. */
    public static final FellTree NONE = new FellTree(Set.of(), Set.of(), false);

    public boolean fells() {
        return !logs.isEmpty();
    }

    /** ADR-0051's amount: the log count of this tree, which is also its yield. */
    public int amount() {
        return logs.size();
    }
}
