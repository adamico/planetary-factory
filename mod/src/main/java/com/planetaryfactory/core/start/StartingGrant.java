package com.planetaryfactory.core.start;

import java.util.List;

/**
 * Whether this player has already been handed the starting kit (#203).
 *
 * <p>Once per <em>player</em>, not once per join. A grant that re-fires on login is an unlimited
 * iron supply, and it would invalidate every pace reading taken after the first relog -- silently,
 * because eight more plates in an inventory look exactly like eight plates that were never spent.
 *
 * <p>The flag is persisted rather than inferred. An empty inventory is not evidence: a player who
 * has spent the pick, placed the furnace or burned the coal has one too, and inferring from it would
 * re-grant precisely to the player who has played the opening.
 *
 * <p>Minecraft-free on purpose, so the rule is a unit test. See {@link StartingKit}.
 */
public final class StartingGrant {

    private boolean granted;

    public StartingGrant() {
        this(false);
    }

    public StartingGrant(boolean granted) {
        this.granted = granted;
    }

    public boolean granted() {
        return granted;
    }

    /**
     * Take the kit, if it has not been taken.
     *
     * <p>Returns the entries to deliver the first time and an empty list every time after, and
     * marks itself in the same call: there is no way to read the flag, act, and forget to write it.
     */
    public List<StartingKit.Entry> claim() {
        if (granted) {
            return List.of();
        }
        granted = true;
        return StartingKit.ALL;
    }
}
