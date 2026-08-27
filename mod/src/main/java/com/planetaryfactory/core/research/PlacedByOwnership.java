package com.planetaryfactory.core.research;

import java.util.UUID;

/**
 * Whether a block entity being placed already has an owner worth keeping.
 *
 * <p>Researchd stamps its placed-by attachment on every player placement, unconditionally:
 *
 * <pre>{@code
 * UUID teamId = team != null ? team.getId() : PlayerUtils.EmptyUUID;
 * be.setData(ResearchdAttachments.PLACED_BY_UUID, teamId);
 * }</pre>
 *
 * <p>With no read before that write, a machine that already carries an owner is re-owned by whoever
 * next sets it down. That matters in this pack because Carry On and Building Gadgets both move
 * placed block entities, attachments and all: a machine another team built can change hands by being
 * picked up, and a placer who is somehow teamless overwrites a valid owner with the empty UUID --
 * which is worse than no attachment, since Researchd then pushes a frame for a team that does not
 * exist and every lock passes.
 *
 * <p>The remedy is to suppress that one write, not to keep a second record of ownership: the
 * attachment stays Researchd's, and the pack only declines to overwrite it. Issue #74.
 *
 * <p>Minecraft-free, so the rule is a plain-JVM unit under the pack's testing policy.
 */
public final class PlacedByOwnership {

    /**
     * The value Researchd writes when a player has no team, and its attachment's default. It reads
     * back as "unowned" everywhere, so it is never an owner worth keeping.
     */
    public static final UUID NO_OWNER = new UUID(0L, 0L);

    private PlacedByOwnership() {}

    /**
     * Whether {@code stored} -- the placed-by value the block entity already carries, or null if it
     * carries none -- is a real owner that a new placement must not overwrite.
     */
    public static boolean isOwned(UUID stored) {
        return stored != null && !NO_OWNER.equals(stored);
    }
}
