package com.planetaryfactory.core.mining.rig;

import java.util.Locale;

/**
 * ADR-0043's two-rung drill ladder, as identity only -- #192 is the shared footprint idiom both
 * rigs stand on, and carries neither mining speed nor fuel (those are #193/#194).
 *
 * <p>{@link #factorioName()} is the corpus key {@link RigFootprints} reads {@code tile_width} and
 * {@code tile_height} against, so the 2x2 and 3x3 ground sizes are never typed here.
 * {@link #blocksTall()} is the exception, and says why on itself: Factorio is played on a plane and
 * states no third figure.
 */
public enum RigTier {
    BURNER("burner-mining-drill", 2),
    ELECTRIC("electric-mining-drill", 3);

    private final String factorioName;
    private final int blocksTall;

    RigTier(String factorioName, int blocksTall) {
        this.factorioName = factorioName;
        this.blocksTall = blocksTall;
    }

    public String factorioName() {
        return factorioName;
    }

    /**
     * How many blocks tall the rig stands. <b>This is the one number here that is chosen rather
     * than extracted, and it has to be.</b>
     *
     * <p>Factorio is played on a plane: a prototype states {@code tile_width} and
     * {@code tile_height}, both of them ground extent, and there is no third figure to read. A rig
     * one block tall reads as a platform rather than as a machine, so the pack picks a vertical
     * extent the corpus cannot supply. ADR-0041's "every number is extracted, and none is chosen"
     * is not being bent quietly -- this is a declared exception in ADR-0043, in the shape ADR-0028
     * established, and it is deliberately the only one in this class.
     *
     * <p>It scales with the tier rather than being one constant: the burner rig is a 2-cube and
     * the electric one a 3-cube, so the upgrade reads as a visibly bigger machine and not merely a
     * wider one.
     */
    public int blocksTall() {
        return blocksTall;
    }

    /** The registry path, e.g. {@code burner_mining_drill}. */
    public String blockName() {
        return serializedName() + "_mining_drill";
    }

    /** The part block's registry path, e.g. {@code burner_mining_drill_part}. */
    public String partBlockName() {
        return blockName() + "_part";
    }

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static RigTier byName(String name) {
        for (RigTier tier : values()) {
            if (tier.serializedName().equals(name)) {
                return tier;
            }
        }
        throw new IllegalArgumentException("no rig tier named " + name);
    }
}
