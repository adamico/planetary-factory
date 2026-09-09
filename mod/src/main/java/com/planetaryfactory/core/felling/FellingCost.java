package com.planetaryfactory.core.felling;

import com.planetaryfactory.core.mining.MiningSpeed;
import com.planetaryfactory.core.mining.PickTier;

/**
 * What a felling gesture costs in time (ADR-0051).
 *
 * <p>{@code amount * seconds_per_log}, where the amount is the tree's own log count and the rate is
 * Factorio's. A four-log tree therefore costs Factorio's own 0.55 seconds exactly, which is the fixed
 * point the pack's divergence on the yield scales around -- without it, "the log count of the tree
 * actually broken" would be a number nobody could check against anything.
 *
 * <p>Delivered to the game as a <em>speed</em> rather than a duration, because that is the only
 * thing Minecraft lets a tool report. {@link MiningSpeed} runs the break-time arithmetic backwards
 * the way ADR-0039 already does for the Pick's stated seconds; the trip through it and back is what
 * {@code FellingCostTest} asserts, since a stated second is easy to lose in that round trip and
 * impossible to see going.
 *
 * <p>Free of Minecraft, and deliberately separate from {@link TreeShape}: one is what the gesture
 * takes and the other is what it costs, and the seam between them is that both read the same
 * amount.
 */
public final class FellingCost {

    private FellingCost() {
    }

    /**
     * Seconds to fell a tree of this many logs with this Pick.
     *
     * <p>The tier scales it the same way it scales everything else the Pick does: `steel-axe`
     * research doubles mining speed, so felling halves. ADR-0039 records that research's outcome as
     * "mining doubles" rather than "ore mining doubles", and Factorio's own
     * {@code character-mining-speed} applies to everything the character mines -- so stopping the
     * boost at wood would be the divergence, not carrying it.
     */
    public static float seconds(int amount, PickTier tier) {
        return amount * TreeCorpus.get().secondsPerLog() * tierFactor(tier);
    }

    /**
     * The speed the Pick must report on this base block for the whole tree to take {@link #seconds}.
     *
     * <p>Charged on the <em>base block only</em>: the rest of the tree comes away with it, so the
     * time for all of it is paid on the one block the player is actually breaking.
     */
    public static float breakSpeed(float hardness, int amount, PickTier tier) {
        return MiningSpeed.forSeconds(hardness, seconds(amount, tier));
    }

    /** How this tier scales a stated duration, relative to the unresearched Pick. */
    private static float tierFactor(PickTier tier) {
        return tier.secondsPerResource() / PickTier.IRON.secondsPerResource();
    }
}
