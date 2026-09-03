package com.planetaryfactory.core.energy;

import java.util.Locale;

/**
 * Factorio's four electric poles, as a reach-and-footprint ladder (ADR-0036).
 *
 * <p>A tier carries geometry and nothing else. It does not change what a machine receives, it
 * grants no voltage tier -- {@code #37} deleted that ladder entire -- and it sits nowhere on the
 * science spine. That is deliberate: a power ladder would compete with the spine, a coverage
 * ladder cannot.
 *
 * <h2>The supply areas are Factorio's own, inversion included</h2>
 *
 * <p>5x5, 7x7, <strong>4x4</strong> and 18x18, straight from the prototypes. The big pole's area
 * really is smaller than the medium pole's: in Factorio it buys <em>wire reach</em> instead, 30
 * tiles against 9, and a player who knows Factorio knows the big pole as the one you run a line
 * with rather than the one you cover a base with.
 *
 * <p>The pack keeps the number even though it has nowhere to spend the reach it pays for -- Power
 * Grid owns transmission and does it with catenary whose span is a material property of the wire,
 * so a pole here has no span to differ in. Fidelity is the tiebreak, and {@code PoleTierTest}
 * pins the four values so the inversion is not "fixed" by someone reading it as a bug.
 *
 * <p><strong>This contradicts one sentence of ADR-0036</strong>, which reads "a bigger pole covers
 * more ground and spans further". Neither clause survives contact with the prototypes. The ADR
 * needs amending, not the numbers.
 *
 * <h2>Even-sided areas on an odd-sized block</h2>
 *
 * <p>The big pole and the substation are 2x2 entities in Factorio, so their even-sided areas centre
 * on the seam between tiles. This pole is one block. The tile count is kept exact and the area is
 * offset half a block instead, which is the same relationship Factorio's 2x2 pole has to any single
 * tile under it. See {@link SupplyArea}.
 */
public enum PoleTier {
    SMALL(5),
    MEDIUM(7),
    /** Factorio's 4x4. Smaller than {@link #MEDIUM} on purpose -- see the class note. */
    BIG(4),
    SUBSTATION(18);

    /**
     * How far up and down a pole supplies, for every tier.
     *
     * <p>Factorio is two-dimensional and so has no answer to take. Two blocks either way covers a
     * machine standing on the pole's own floor, one sunk into it, and one on a platform above,
     * without a pole quietly powering the floor below through the ceiling. It is deliberately much
     * shallower than the horizontal reach: the supply area should read as a footprint on the
     * ground, which is how a Factorio player already pictures it.
     */
    public static final int VERTICAL_RADIUS = 2;

    private final int supplySize;

    PoleTier(int supplySize) {
        this.supplySize = supplySize;
    }

    /** The side of the supply square in blocks, exactly as Factorio states it. */
    public int supplySize() {
        return supplySize;
    }

    public int verticalRadius() {
        return VERTICAL_RADIUS;
    }

    /**
     * The lowest horizontal offset the area reaches, relative to the pole.
     *
     * <p>Odd sizes centre exactly. Even sizes take the extra block on the negative side, which is
     * an arbitrary but fixed choice -- it has to go somewhere, and putting it on the same side for
     * both even tiers keeps two poles of different tiers aligned when their corners are.
     */
    public int minOffset() {
        return -(supplySize / 2);
    }

    /** The highest horizontal offset the area reaches, relative to the pole. */
    public int maxOffset() {
        return (supplySize - 1) / 2;
    }

    /** The registry path, e.g. {@code small_electric_pole}. */
    public String blockName() {
        return serializedName() + "_electric_pole";
    }

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
