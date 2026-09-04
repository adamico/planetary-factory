package com.planetaryfactory.core.assembler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Turns "N of this recipe" into a Crafting Plan: the flattened tree, in craft order, with the three
 * categories the dialog draws (#161, ADR-0038).
 *
 * <p>Chain-crafting is the whole job. Requesting a recipe whose ingredients you lack queues the
 * sub-crafts, recursively, and what the walk cannot make it reports rather than silently dropping --
 * as {@code Missing} when nothing hand-makes it, as {@code Locked} when something does and the team
 * has not researched it. The two are separate because one is fixed by mining and the other by
 * research.
 *
 * <p>No Minecraft type appears here, which is what makes the recursion checkable without a game.
 * {@link RecipeGraph} is the seam on the recipe side and {@link ItemBag} on the inventory side;
 * {@code RuntimePlanSource} is the only thing that knows how to fill either from a server.
 *
 * <p><b>Crafts of one node batch into one step.</b> Twenty gears is a single {@link CraftStep} with
 * twenty times the inputs, outputs and duration, not twenty steps. The plan is persisted on a player
 * attachment and a large plan would otherwise be tens of thousands of near-identical records; what
 * is lost is delivery partway through a batch, which ADR-0038 never promised -- the plan is the unit
 * throughout.
 */
public final class PlanResolver {

    /**
     * The most crafts one plan may be resolved for.
     *
     * <p>It bounds {@link #largestAffordable}'s search, which would otherwise run forever against a
     * recipe that consumes nothing, and it keeps every quantity in the walk inside an {@code int}.
     * A real inventory affords far less than this.
     */
    public static final int MAX_CRAFTS = 100_000;

    private final RecipeGraph graph;
    private final Predicate<String> locked;

    /**
     * @param graph the hand-craftable set
     * @param locked whether a recipe id is still blocked for the team asking -- Researchd's question,
     *     passed in as a predicate so this class never learns what a team is
     */
    public PlanResolver(RecipeGraph graph, Predicate<String> locked) {
        this.graph = graph;
        this.locked = locked;
    }

    /**
     * Resolves {@code crafts} runs of {@code recipeId} against what the player has.
     *
     * <p>{@code available} is read, never spent: the reservation is taken at Start by the queue, and
     * a resolver that emptied the bag it was handed could not be asked twice -- which
     * {@link #largestAffordable} does a dozen times.
     */
    public Resolution resolve(String recipeId, int crafts, ItemBag available) {
        Walk walk = new Walk(available.copy());
        HandRecipe root = graph.byId(recipeId);
        if (root == null || crafts <= 0 || crafts > MAX_CRAFTS) {
            return walk.finish();
        }
        if (locked.test(root.id())) {
            for (ItemAmount output : root.outputs()) {
                walk.locked.add(output.item(), output.count() * crafts);
            }
            return walk.finish();
        }
        walk.craft(root, crafts, List.of());
        return walk.finish();
    }

    /**
     * Select Amount's {@code all}: the largest count whose plan is complete, so {@code all} can
     * never produce a plan Start then refuses (ADR-0038).
     *
     * <p>Found by doubling and then bisecting rather than by dividing the inventory through the
     * recipe, because a chain's cost is not linear in the count -- a recipe making two at a time
     * leaves a surplus that the next craft uses, so twice the crafts can cost less than twice the
     * ingredients.
     */
    public int largestAffordable(String recipeId, ItemBag available) {
        if (!resolve(recipeId, 1, available).complete()) return 0;
        int affordable = 1;
        int beyond = 2;
        while (beyond <= MAX_CRAFTS && resolve(recipeId, beyond, available).complete()) {
            affordable = beyond;
            beyond *= 2;
        }
        int high = Math.min(beyond, MAX_CRAFTS + 1);
        while (affordable + 1 < high) {
            int middle = affordable + (high - affordable) / 2;
            if (resolve(recipeId, middle, available).complete()) {
                affordable = middle;
            } else {
                high = middle;
            }
        }
        return affordable;
    }

    /**
     * One resolution in progress: what has been taken, what has been planned, and what could not be.
     *
     * <p>{@code surplus} is what makes a recipe that produces two at a time behave. Outputs land
     * there and later demands draw from it first, so three cables asked for twice is four crafts and
     * not six -- and the leftover is simply part of the plan, delivered when no remaining step wants
     * it.
     */
    private final class Walk {

        private final ItemBag available;
        private final ItemBag surplus = new ItemBag();
        private final ItemBag rawCost = new ItemBag();
        private final ItemBag missing = new ItemBag();
        private final ItemBag locked = new ItemBag();
        private final ItemBag toCraft = new ItemBag();
        private final List<CraftStep> steps = new ArrayList<>();

        private Walk(ItemBag available) {
            this.available = available;
        }

        /**
         * Plans {@code n} runs of {@code recipe}, its ingredients first.
         *
         * <p>The step is appended after the recursion, so the list comes out in dependency order and
         * the queue can run it front to back without ever looking a recipe up again.
         */
        private void craft(HandRecipe recipe, int n, List<String> ancestors) {
            List<String> path = new ArrayList<>(ancestors);
            path.add(recipe.id());
            List<ItemAmount> inputs = new ArrayList<>(recipe.inputs().size());
            for (ItemAmount input : recipe.inputs()) {
                int wanted = input.count() * n;
                need(input.item(), wanted, path);
                inputs.add(new ItemAmount(input.item(), wanted));
            }
            List<ItemAmount> outputs = new ArrayList<>(recipe.outputs().size());
            for (ItemAmount output : recipe.outputs()) {
                int made = output.count() * n;
                outputs.add(new ItemAmount(output.item(), made));
                surplus.add(output.item(), made);
                toCraft.add(output.item(), made);
            }
            steps.add(new CraftStep(recipe.id(), inputs, outputs, recipe.durationTicks() * n));
        }

        /** Finds {@code quantity} of {@code item}, crafting it if that is what it takes. */
        private void need(String item, int quantity, List<String> ancestors) {
            int owed = quantity - drawDown(surplus, item, quantity, false);
            owed -= drawDown(available, item, owed, true);
            if (owed <= 0) return;

            HandRecipe maker = graph.makerOf(item);
            if (maker == null) {
                missing.add(item, owed);
                return;
            }
            if (PlanResolver.this.locked.test(maker.id())) {
                locked.add(item, owed);
                return;
            }
            if (ancestors.contains(maker.id())) {
                // A cycle. The corpus forbids one and `test_hand_resolver.py` asserts that, but the
                // runtime graph is assembled from whatever a pack author loaded, so the resolver
                // must be the thing that stops rather than the thing that hangs.
                missing.add(item, owed);
                return;
            }
            int perCraft = maker.perCraft(item);
            int runs = Math.min(MAX_CRAFTS, ceilDiv(owed, perCraft));
            craft(maker, runs, ancestors);
            surplus.remove(item, Math.min(owed, surplus.count(item)));
        }

        /** Spends up to {@code wanted} of {@code item} out of {@code bag}, and says how much. */
        private int drawDown(ItemBag bag, String item, int wanted, boolean isRawCost) {
            if (wanted <= 0) return 0;
            int taken = Math.min(wanted, bag.count(item));
            if (taken <= 0) return 0;
            bag.remove(item, taken);
            if (isRawCost) rawCost.add(item, taken);
            return taken;
        }

        private Resolution finish() {
            return new Resolution(
                    List.copyOf(steps),
                    rawCost.amounts(),
                    toCraft.amounts(),
                    missing.amounts(),
                    locked.amounts());
        }
    }

    private static int ceilDiv(int quantity, int per) {
        return per <= 0 ? quantity : (quantity + per - 1) / per;
    }

    /**
     * A resolved plan, or the reasons it is not one.
     *
     * <p>{@code rawCost} is everything the plan takes from the inventory -- leaves and any
     * intermediate the player already had, since both leave the inventory at Start and both come
     * back on a cancel. {@code toCraft} is every step's output, root included, because a dialog that
     * hid the thing being made would be a strange dialog.
     */
    public record Resolution(
            List<CraftStep> steps,
            List<ItemAmount> rawCost,
            List<ItemAmount> toCraft,
            List<ItemAmount> missing,
            List<ItemAmount> locked) {

        public Resolution {
            steps = List.copyOf(steps);
            rawCost = List.copyOf(rawCost);
            toCraft = List.copyOf(toCraft);
            missing = List.copyOf(missing);
            locked = List.copyOf(locked);
        }

        /** The one condition Start turns on: nothing to mine and nothing to research. */
        public boolean complete() {
            return missing.isEmpty() && locked.isEmpty() && !steps.isEmpty();
        }

        /**
         * The plan the queue takes, which only a complete resolution has.
         *
         * <p>Only the parts Start needs cross: the ordered steps and the cost. The three display
         * lists stay behind, because the queue never shows anything and a running plan has nothing
         * left to be missing.
         */
        public CraftingPlan toPlan(java.util.UUID id, String rootItem, int amount) {
            return new CraftingPlan(id, rootItem, amount, rawCost, steps);
        }
    }
}
