package com.planetaryfactory.core.mining;

/**
 * The two rungs of the Engineer's Pick, and Factorio's two mining speeds (ADR-0039).
 *
 * <p>Factorio's character mines at {@code 0.5}, and {@code steel-axe} adds {@code 1} to it. Those
 * two speeds are Factorio's and are kept; a resource's seconds-per-item is
 * {@code mining_time / mining_speed}, so the ratio between the tiers is Factorio's too.
 *
 * <p><b>The mining time is the pack's, not Factorio's.</b> Factorio gives Terra's four resources
 * {@code mining_time: 1}, which would be two seconds by hand -- and ADR-0039 shipped exactly that,
 * then failed its own human-on-delivery check. Two seconds is Factorio's number inside Factorio's
 * economy, where the engineer hand-mines perhaps thirty ore before a burner drill takes over; here
 * the starting area holds around 1150 ore blocks and every other block in the game breaks in well
 * under a second, so the ore alone felt three to five times heavier than the world around it. The
 * amendment halves the time and keeps everything else: still flat across the four resources, still
 * halved by {@code steel-axe}, still checkable as one number rather than vanilla's hardness spread.
 *
 * <p>The speeds are <em>transcribed from the wiki, not extracted</em>: {@code data/factorio/} holds
 * no resource dump, so unlike the technology tree they cannot be checked against the repo. ADR-0039
 * labels that weakness and names the extractor as the follow-on; a reader who finds these numbers
 * wrong should suspect the transcription first.
 */
public enum PickTier {
    /** Factorio's bare character: {@code mining_speed 0.5}, so one second an ore. */
    IRON("engineers_iron_pick", 0.5f),
    /** After {@code steel-axe}: {@code mining_speed 1.0}, so half of one. */
    STEEL("engineers_steel_pick", 1.0f);

    /**
     * The mining time every one of Terra's resources carries -- the pack's number, not Factorio's.
     *
     * <p>Factorio's is {@code 1}. This is half of it, and the halving is the whole of ADR-0039's
     * amendment: flat across the four resources as before, and still the only mining number in the
     * pack that is stated rather than inherited from a hardness table.
     */
    public static final float MINING_TIME = 0.5f;

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
        return MINING_TIME / miningSpeed;
    }
}
