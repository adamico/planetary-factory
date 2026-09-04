package com.planetaryfactory.core.mining;

/**
 * Minecraft's break-time arithmetic, run backwards.
 *
 * <p>A held tool has a <em>speed</em>, not a duration: with the correct tool and no other modifier a
 * player breaks a block in {@code hardness * 30 / speed} ticks, so {@code 1.5 * hardness / speed}
 * seconds. ADR-0039 states a duration instead -- Factorio's flat two seconds a resource -- so the
 * speed the item reports is solved for rather than tabulated, and a block whose hardness changes
 * keeps its Factorio time without anything here being edited.
 *
 * <p>Free of every Minecraft type on purpose: this is the one part of the Pick that carries a claim
 * worth checking, and {@code MiningSpeedTest} checks it with no game.
 */
public final class MiningSpeed {

    /** Ticks per second, and the only reason this file knows what a second is. */
    private static final float TICKS_PER_SECOND = 20.0f;

    /** Minecraft's constant: a correct-tool break costs {@code hardness * 30 / speed} ticks. */
    private static final float TICKS_PER_HARDNESS = 30.0f;

    private MiningSpeed() {
    }

    /**
     * The speed a tool must report to break a block of this hardness in exactly this many seconds.
     *
     * <p>An unbreakable block (hardness below zero) and a free one (hardness zero) both fall out of
     * the arithmetic rather than being special cases: zero hardness is already instant at any
     * speed, and a negative one is never broken at all.
     */
    public static float forSeconds(float hardness, float seconds) {
        if (hardness <= 0.0f) return 1.0f;
        if (seconds <= 0.0f) return Float.MAX_VALUE;
        return hardness * TICKS_PER_HARDNESS / (seconds * TICKS_PER_SECOND);
    }

    /** The inverse, which is what a test asserts and a reader checks against Factorio. */
    public static float secondsAt(float hardness, float speed) {
        if (hardness <= 0.0f) return 0.0f;
        return hardness * TICKS_PER_HARDNESS / (speed * TICKS_PER_SECOND);
    }
}
