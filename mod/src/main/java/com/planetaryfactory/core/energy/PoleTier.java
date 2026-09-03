package com.planetaryfactory.core.energy;

import java.util.Locale;

/**
 * The pack's three electric poles, as a footprint ladder (ADR-0036).
 *
 * <p>A tier carries geometry and nothing else. It does not change what a machine receives, it
 * grants no voltage tier -- {@code #37} deleted that ladder entire -- and it sits nowhere on the
 * science spine. That is deliberate: a power ladder would compete with the spine, a coverage
 * ladder cannot.
 *
 * <h2>The supply areas are Factorio's own</h2>
 *
 * <p>5x5, 7x7 and 18x18, straight from the prototypes, and {@code PoleTierTest} pins them.
 *
 * <h2>Factorio's fourth tier is not here</h2>
 *
 * <p>The big pole is dropped. Its supply area is 4x4 -- <em>smaller</em> than the medium pole's --
 * because in Factorio it buys wire reach instead, 30 tiles against 9. The pack has no reach to
 * sell: Power Grid owns transmission with catenary whose span is a property of the wire, not of the
 * pole it hangs on, so a big pole here would be a strictly worse medium pole with no compensating
 * axis. Fidelity to the prototype loses to a tier that would only ever be a trap. See
 * {@code data/pack/item-map.json} for the row that records it, and ADR-0036 for the decision.
 *
 * <h2>Even-sided areas on an odd-sized block</h2>
 *
 * <p>The substation is a 2x2 entity in Factorio, so its even-sided area centres on the seam between
 * tiles. This pole is one block. The tile count is kept exact and the area is offset half a block
 * instead, which is the same relationship Factorio's 2x2 pole has to any single tile under it. See
 * {@link SupplyArea}.
 */
public enum PoleTier {
    SMALL(5),
    MEDIUM(7),
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
     * an arbitrary but fixed choice -- it has to go somewhere, and the substation is the only tier
     * it applies to.
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
