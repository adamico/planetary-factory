package com.planetaryfactory.core.mining;

/**
 * The two rungs of the Engineer's Pick, and Factorio's two mining speeds (ADR-0039).
 *
 * <p>Factorio's character mines at {@code 0.5}, and {@code steel-axe} adds {@code 1} to it. A
 * resource's seconds-per-item is {@code mining_time / mining_speed}, and Terra's four resources --
 * iron ore, copper ore, coal and stone -- all carry {@code mining_time: 1}. Hence two seconds by
 * hand and one after the research.
 *
 * <p>These two numbers are <em>transcribed from the wiki, not extracted</em>: {@code data/factorio/}
 * holds no resource dump, so unlike the technology tree they cannot be checked against the repo.
 * ADR-0039 labels that weakness and names the extractor as the follow-on; a reader who finds these
 * numbers wrong should suspect the transcription first.
 */
public enum PickTier {
    /** Factorio's bare character: {@code mining_speed 0.5}, so two seconds an ore. */
    IRON("engineers_iron_pick", 0.5f),
    /** After {@code steel-axe}: {@code mining_speed 1.0}, so one. */
    STEEL("engineers_steel_pick", 1.0f);

    /** Factorio gives all four of Terra's resources the same {@code mining_time}, and it is 1. */
    public static final float FACTORIO_MINING_TIME = 1.0f;

    private final String id;
    private final float miningSpeed;

    PickTier(String id, float miningSpeed) {
        this.id = id;
        this.miningSpeed = miningSpeed;
    }

    public String id() {
        return id;
    }

    public float miningSpeed() {
        return miningSpeed;
    }

    /** Seconds to take one of Terra's resources, which is the whole number the player feels. */
    public float secondsPerResource() {
        return FACTORIO_MINING_TIME / miningSpeed;
    }
}
