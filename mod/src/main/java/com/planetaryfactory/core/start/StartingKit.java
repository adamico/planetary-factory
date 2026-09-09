package com.planetaryfactory.core.start;

import java.util.ArrayList;
import java.util.List;

/**
 * What a player is holding the first time they wake up on Terra (#203).
 *
 * <p>`docs/spec/terra-progression.md`'s Opening specifies it and, until this class, nothing granted
 * it: the pocket and the hold existed only as prose and a `/give` string the tester had to recall in
 * the right order before starting the clock, which measures the tester rather than the pack.
 *
 * <p>Deliberately free of Minecraft. Items are named by string here so that the once-per-player rule
 * -- the half that can silently hand out an unlimited iron supply -- is an ordinary unit test rather
 * than a world load. {@link StartingKitGrant} is the half that needs a game.
 *
 * <p>The two halves are Factorio's own split (spec, Opening): <b>pocket</b> is tools, and
 * <b>hold</b> is materials from the ship. The hold is single digits, matching freeplay's eight-plate
 * debris chest, and <b>nothing in it is otherwise unobtainable</b> -- it removes the pre-tool grind,
 * it does not seed a tier. The moment it contains a green circuit, rung 0 has stopped being taught,
 * which is why {@code tests/pack/test_starting_kit.py} asserts the list rather than trusting it.
 *
 * <p>The hold lands in the player's inventory alongside the pocket because the wreck's cargo hold
 * (#133) is out of #170's slice. When the wreck arrives these three move into it; the pocket does
 * not.
 */
public final class StartingKit {

    /** One granted stack: an item id and how many of it. */
    public record Entry(String item, int count) {
        public Entry {
            if (item == null || item.isBlank()) {
                throw new IllegalArgumentException("A granted entry needs an item id");
            }
            if (count < 1) {
                throw new IllegalArgumentException("A granted entry needs a positive count: " + count);
            }
        }
    }

    /**
     * The pocket: the tools, one each.
     *
     * <p>The book is here even though its <em>content</em> is a separate ticket -- beat 1 is "the
     * book is in your inventory; its tooltip points at the panel", so a book that arrives later
     * costs the beat rather than merely the content.
     */
    public static final List<Entry> POCKET = List.of(
            new Entry("ftbquests:book", 1),
            new Entry("gtceu:prospector.lv", 1),
            new Entry("planetaryfactory:stone_furnace", 1),
            new Entry("planetaryfactory:burner_mining_drill", 1),
            new Entry("planetaryfactory:engineers_iron_pick", 1));

    /**
     * The hold: iron plate, copper plate, coal, single digits.
     *
     * <p>Eight of each plate is freeplay's debris chest read straight across; the coal is what buys
     * the Stone Furnace and the Burner Mining Drill enough burn to reach beat 6 without a detour
     * back to hand-mining, and is deliberately not more than that.
     */
    public static final List<Entry> HOLD = List.of(
            new Entry("gtceu:iron_plate", 8),
            new Entry("gtceu:copper_plate", 8),
            new Entry("minecraft:coal", 8));

    /** Everything granted, pocket first, in the order it lands in the inventory. */
    public static final List<Entry> ALL = concat(POCKET, HOLD);

    private static List<Entry> concat(List<Entry> pocket, List<Entry> hold) {
        List<Entry> all = new ArrayList<>(pocket);
        all.addAll(hold);
        return List.copyOf(all);
    }

    private StartingKit() {
    }
}
