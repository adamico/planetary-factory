package com.planetaryfactory.core.mining;

import com.planetaryfactory.core.ore.OreDelta;
import com.planetaryfactory.core.ore.OreField;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Engineer's Pick's one checkable claim: that the stated seconds survive the trip through
 * Minecraft's break-time arithmetic (ADR-0039).
 *
 * <p>What this cannot prove is that a player standing in front of a block waits that long -- the
 * mining tick applies enchantments, potion effects, being underwater and being off the ground on
 * top of the tool's speed, and none of those exist here. It proves the speed the item reports is
 * the one that solves for the stated duration, which is the half that is a number rather than a
 * game.
 */
class MiningSpeedTest {

    /** GregTech's ores sit at hardness 3, which is what Terra's three fields are made of. */
    private static final float ORE_HARDNESS = 3.0f;

    private static final float TOLERANCE = 1.0e-4f;

    @Test
    void ironPickTakesOneSecondAnOre() {
        float speed = MiningSpeed.forSeconds(ORE_HARDNESS, PickTier.IRON.secondsPerResource());
        assertEquals(1.0f, MiningSpeed.secondsAt(ORE_HARDNESS, speed), TOLERANCE);
    }

    @Test
    void steelPickHalvesIt() {
        float speed = MiningSpeed.forSeconds(ORE_HARDNESS, PickTier.STEEL.secondsPerResource());
        assertEquals(0.5f, MiningSpeed.secondsAt(ORE_HARDNESS, speed), TOLERANCE);
    }

    @Test
    void theTimeIsFlatAcrossHardness() {
        // The point of the tag: Factorio gives all four resources the same mining_time, so a
        // harder block must not take longer. Vanilla's spread is what the tag excludes.
        for (float hardness : new float[] {0.5f, 3.0f, 30.0f, 50.0f}) {
            float speed = MiningSpeed.forSeconds(hardness, PickTier.IRON.secondsPerResource());
            assertEquals(1.0f, MiningSpeed.secondsAt(hardness, speed), TOLERANCE,
                         "hardness " + hardness);
        }
    }

    @Test
    void theTimeIsThePacksAndTheSpeedsAreFactorios() {
        // seconds per item = mining_time / mining_speed, with Factorio's two speeds and the pack's
        // own halved mining time -- ADR-0039's amendment, after two seconds failed its human check.
        assertEquals(PickTier.MINING_TIME / PickTier.IRON.miningSpeed(),
                     PickTier.IRON.secondsPerResource(), TOLERANCE);
        assertEquals(PickTier.MINING_TIME / PickTier.STEEL.miningSpeed(),
                     PickTier.STEEL.secondsPerResource(), TOLERANCE);
        assertEquals(1.0f, PickTier.IRON.secondsPerResource(), TOLERANCE);
        assertEquals(0.5f, PickTier.STEEL.secondsPerResource(), TOLERANCE);
        // Factorio's ratio between the tiers survives the halving, which is the half that is not
        // ours to change: steel-axe doubles mining speed, so it halves the time.
        assertEquals(2.0f, PickTier.IRON.secondsPerResource() / PickTier.STEEL.secondsPerResource(),
                     TOLERANCE);
    }

    @Test
    void anUnbreakableBlockStaysUnbreakable() {
        // Bedrock's hardness is negative. Solving for a duration there would divide by it and hand
        // back a speed that makes no sense; the guard returns the neutral 1.0 instead.
        assertEquals(1.0f, MiningSpeed.forSeconds(-1.0f, 2.0f), TOLERANCE);
    }

    @Test
    void anInstantDurationAsksForEveryThingAtOnce() {
        // The other guard: a zero or negative duration has no speed that satisfies it, so the
        // answer is the largest one there is rather than a division by zero.
        assertEquals(Float.MAX_VALUE, MiningSpeed.forSeconds(3.0f, 0.0f), TOLERANCE);
        assertEquals(Float.MAX_VALUE, MiningSpeed.forSeconds(3.0f, -1.0f), TOLERANCE);
    }

    @Test
    void theSteelPickIsStrictlyFaster() {
        assertTrue(PickTier.STEEL.miningSpeed() > PickTier.IRON.miningSpeed());
    }

    /**
     * ADR-0041's arithmetic claim, and the one the amendment turns from figurative into literal.
     *
     * <p>Before an ore block carried an amount, "one second per ore" meant one second per
     * <em>block</em>, and how long a field took depended on how many blocks the generator happened
     * to lay down. It now depends on the field's total instead: every unit is one break gesture
     * through {@link OreDelta#draw}, so the seconds a patch costs are its amount times the tier's
     * seconds -- whatever quotient {@link OreField} arrived at, and whether the block gives up its
     * last unit on the first gesture or the five-hundredth.
     */
    @Test
    void secondsPerOreHoldAcrossAMultiUnitBlock() {
        OreField field = new OreField("iron", 40_000L, 100);
        int perBlock = field.amountPerBlock();
        assertEquals(400, perBlock);

        OreDelta delta = new OreDelta();
        int paid = 0;
        for (int gesture = 0; gesture < perBlock * 2; gesture++) {
            OreDelta.Draw draw = delta.draw(1L, perBlock);
            paid += draw.paid();
            if (draw.exhausted()) {
                break;
            }
        }
        // One gesture, one unit: the block pays out exactly what it holds and no gesture is free.
        assertEquals(perBlock, paid);

        float seconds = paid * PickTier.IRON.secondsPerResource();
        assertEquals(perBlock * 1.0f, seconds, TOLERANCE);
        // And the whole field costs its total, not its block count -- the split is arithmetic that
        // cancels, which is exactly what "the amount is the resource" has to mean.
        assertEquals(field.total() * PickTier.IRON.secondsPerResource(),
                     seconds * field.blocks(), TOLERANCE);
    }

    @Test
    void theSteelPickHalvesTheFieldAsWellAsTheBlock() {
        // The tier ratio is a property of a gesture, so it survives multiplication by any amount.
        OreField field = new OreField("copper", 320_000L, 1150);
        float iron = field.total() * PickTier.IRON.secondsPerResource();
        float steel = field.total() * PickTier.STEEL.secondsPerResource();
        assertEquals(2.0f, iron / steel, TOLERANCE);
    }
}
