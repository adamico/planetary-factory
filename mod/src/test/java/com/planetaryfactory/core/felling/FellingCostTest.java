package com.planetaryfactory.core.felling;

import com.planetaryfactory.core.mining.MiningSpeed;
import com.planetaryfactory.core.mining.PickTier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What one felling gesture costs (ADR-0051), and that the number came from Factorio.
 *
 * <p>The arithmetic is small, which is exactly why it is worth asserting: nothing in a running game
 * looks wrong when a tree costs the wrong amount of time. It just feels slightly off, and stays that
 * way. Two claims are held here that no world load would catch --
 *
 * <ul>
 *   <li><b>a four-log tree costs Factorio's own 0.55 seconds.</b> That equality is the whole reason
 *       ADR-0051 could diverge on the yield and still call the result faithful: Factorio's tree is
 *       the calibration point, so the divergence is a scaling around a fixed point rather than a
 *       number somebody liked.
 *   <li><b>the rate is 0.1375 and not 0.125.</b> Those are `0.55 / 4` and `0.5 / 4`, and 0.5 is the
 *       mining time of Factorio's <em>dead</em> trees and of its plants. #205 was written against
 *       the wrong one of the two, so the corpus is asked rather than trusted.
 * </ul>
 */
class FellingCostTest {

    /** Vanilla log hardness, which is what the Pick's speed is solved against. */
    private static final float LOG_HARDNESS = 2.0f;

    private static final float TOLERANCE = 1.0e-4f;

    /** Factorio's own whole-tree mining time for the living Nauvis trees. */
    private static final float FACTORIO_TREE_SECONDS = 0.55f;

    /** The trap: `0.5 / 4`, the dead trees' and the plants' rate. */
    private static final float THE_WRONG_RATE = 0.125f;

    @Test
    void theRateIsTheCorpusAndTheCorpusIsFactorios() {
        assertEquals(0.1375f, TreeCorpus.get().secondsPerLog(), TOLERANCE);
        assertNotEquals(THE_WRONG_RATE, TreeCorpus.get().secondsPerLog(), TOLERANCE);
        assertTrue(TreeCorpus.get().rateFrom().contains("tree-01"),
                   "the rate must still be backed by the prototypes the ADR names");
    }

    @Test
    void aFourLogTreeCostsFactoriosOwnTime() {
        assertEquals(FACTORIO_TREE_SECONDS,
                     FellingCost.seconds(4, PickTier.IRON),
                     TOLERANCE);
    }

    @Test
    void theCostScalesWithTheAmount() {
        // ADR-0051's divergence: the amount is the tree's log count, so a jungle giant costs more
        // than a birch -- which is the reading the flat wood x4 cannot deliver.
        float one = FellingCost.seconds(1, PickTier.IRON);
        for (int amount : new int[] {1, 4, 9, 30}) {
            assertEquals(one * amount, FellingCost.seconds(amount, PickTier.IRON), TOLERANCE,
                         "amount " + amount);
        }
    }

    @Test
    void researchHalvesIt() {
        // One tool, one speed ladder (ADR-0039). A Pick that got faster at ore but not at wood
        // would be a second rule with nothing arguing for it.
        for (int amount : new int[] {1, 4, 12}) {
            assertEquals(FellingCost.seconds(amount, PickTier.IRON) / 2.0f,
                         FellingCost.seconds(amount, PickTier.STEEL),
                         TOLERANCE,
                         "amount " + amount);
        }
    }

    @Test
    void theSpeedSolvesForTheStatedTime() {
        // The seam to the game: the handler reports a *speed*, and Minecraft turns that back into a
        // duration. That round trip is where a stated second is silently lost.
        for (int amount : new int[] {1, 4, 12, 30}) {
            float speed = FellingCost.breakSpeed(LOG_HARDNESS, amount, PickTier.IRON);
            assertEquals(FellingCost.seconds(amount, PickTier.IRON),
                         MiningSpeed.secondsAt(LOG_HARDNESS, speed),
                         TOLERANCE,
                         "amount " + amount);
        }
    }

    @Test
    void aSingleLogIsNotSlowerThanVanilla() {
        // A one-log tree is the floor, and it has to stay a gesture rather than becoming a chore:
        // 0.1375s is well under vanilla's own bare-hand log, so felling never makes wood harder to
        // get than it was before the ticket.
        assertTrue(FellingCost.seconds(1, PickTier.IRON) < 1.0f);
    }
}
