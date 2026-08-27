package com.planetaryfactory.core.research;

import java.util.UUID;

/**
 * Whether a placed-by team UUID names a real owner.
 *
 * <p>Two callers ask this, and both hinge on the same fact: Researchd's {@code PLACED_BY_UUID}
 * attachment is declared with the zero UUID as its <em>default</em>, so an unowned machine does not
 * read back as absent -- it reads back as owned by a team that does not exist.
 *
 * <ul>
 *   <li>The recipe wrapper, deciding whether the filter frame it was handed represents a team at
 *       all. Because the default is non-null, Researchd pushes a frame even for a machine placed by
 *       {@code /setblock}, and only this test tells that frame apart from a real one.
 *   <li>The placement guard below, deciding whether a machine already has an owner worth keeping.
 *
 * </ul>
 *
 * <h2>The placement guard</h2>
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
