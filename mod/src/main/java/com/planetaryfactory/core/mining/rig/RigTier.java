package com.planetaryfactory.core.mining.rig;

import java.util.Locale;

/**
 * ADR-0043's two-rung drill ladder, as identity only -- #192 is the shared footprint idiom both
 * rigs stand on, and carries neither mining speed nor fuel (those are #193/#194).
 *
 * <p>{@link #factorioName()} is the corpus key {@link RigFootprints} reads {@code tile_width} and
 * {@code tile_height} against, so the 2x2 and 3x3 sizes are never typed here.
 */
public enum RigTier {
    BURNER("burner-mining-drill"),
    ELECTRIC("electric-mining-drill");

    private final String factorioName;

    RigTier(String factorioName) {
        this.factorioName = factorioName;
    }

    public String factorioName() {
        return factorioName;
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
