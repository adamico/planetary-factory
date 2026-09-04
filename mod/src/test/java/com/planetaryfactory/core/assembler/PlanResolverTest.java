package com.planetaryfactory.core.assembler;

import static com.planetaryfactory.core.assembler.TestBags.asMap;
import static com.planetaryfactory.core.assembler.TestBags.have;
import static com.planetaryfactory.core.assembler.TestBags.stocked;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * The resolver's rules (#161, ADR-0038): chain-crafting, what counts as raw cost, and the three
 * categories the plan dialog draws.
 *
 * <p>Minecraft-free, like the queue it feeds. Items are strings and recipes are a hand-built graph,
 * which is the whole reason the recursion is checkable at all -- `tests/factorio/test_hand_resolver.py`
 * asserts the corpus admits these plans, and this asserts the resolver actually builds them.
 */
class PlanResolverTest {

    /** gear = 2 plate; circuit = 3 cable + 1 plate; cable = 1 copper, making 2. */
    private static RecipeGraph graph() {
        return RecipeGraph.builder()
                .add(new HandRecipe("gear", List.of(new ItemAmount("plate", 2)),
                        List.of(new ItemAmount("gear", 1)), 10))
                .add(new HandRecipe("cable", List.of(new ItemAmount("copper", 1)),
                        List.of(new ItemAmount("cable", 2)), 10))
                .add(new HandRecipe("circuit", List.of(new ItemAmount("cable", 3), new ItemAmount("plate", 1)),
                        List.of(new ItemAmount("circuit", 1)), 20))
                .build();
    }

    private static PlanResolver resolver() {
        return new PlanResolver(graph(), Set.of()::contains);
    }



    @Test
    void aRecipeWhoseIngredientsAreOnHandIsOneStep() {
        PlanResolver.Resolution resolution = resolver().resolve("gear", 1, have("plate", 2));

        assertTrue(resolution.complete());
        assertEquals(1, resolution.steps().size());
        assertEquals("gear", resolution.steps().get(0).recipe());
        assertEquals(Map.of("plate", 2), asMap(resolution.rawCost()));
    }

    @Test
    void aMissingIntermediateIsChainCraftedBeforeTheRootThatWantsIt() {
        PlanResolver.Resolution resolution = resolver().resolve("circuit", 1, have("copper", 2, "plate", 1));

        assertTrue(resolution.complete());
        assertEquals(List.of("cable", "circuit"),
                resolution.steps().stream().map(CraftStep::recipe).toList());
        // The leaves, and only the leaves: the cable is made by the plan, not taken from the player.
        assertEquals(Map.of("copper", 2, "plate", 1), asMap(resolution.rawCost()));
    }

    @Test
    void anIntermediateAlreadyInTheInventoryIsUsedRatherThanRemade() {
        PlanResolver.Resolution resolution = resolver().resolve("circuit", 1, have("cable", 3, "plate", 1));

        assertTrue(resolution.complete());
        assertEquals(List.of("circuit"), resolution.steps().stream().map(CraftStep::recipe).toList());
        assertEquals(Map.of("cable", 3, "plate", 1), asMap(resolution.rawCost()));
    }

    @Test
    void aLeafNothingMakesIsMissingAndTheWholePlanIsIncomplete() {
        PlanResolver.Resolution resolution = resolver().resolve("gear", 1, have());

        assertFalse(resolution.complete());
        assertEquals(Map.of("plate", 2), asMap(resolution.missing()));
        assertTrue(resolution.locked().isEmpty());
    }

    @Test
    void aLockedRecipeIsLockedAndNotMissing() {
        PlanResolver locked = new PlanResolver(graph(), Set.of("cable")::contains);

        PlanResolver.Resolution resolution = locked.resolve("circuit", 1, have("copper", 9, "plate", 1));

        assertFalse(resolution.complete());
        assertEquals(Map.of("cable", 3), asMap(resolution.locked()));
        // Not folded into Missing: research fixes one and mining the other, and the player needs to
        // know which errand they are on.
        assertTrue(resolution.missing().isEmpty());
    }

    @Test
    void craftsOfOneRecipeBatchIntoOneStepThatTakesProportionallyLonger() {
        PlanResolver.Resolution resolution = resolver().resolve("gear", 3, have("plate", 6));

        assertEquals(1, resolution.steps().size());
        CraftStep step = resolution.steps().get(0);
        assertEquals(30, step.durationTicks());
        assertEquals(Map.of("plate", 6), asMap(step.inputs()));
        assertEquals(Map.of("gear", 3), asMap(step.outputs()));
    }

    @Test
    void aRecipeMakingTwoAtATimeRoundsUpAndKeepsTheSurplusInThePlan() {
        // One circuit wants 3 cable; cable comes 2 at a time, so 2 crafts make 4 and one is spare.
        PlanResolver.Resolution resolution = resolver().resolve("circuit", 1, have("copper", 2, "plate", 1));

        CraftStep cable = resolution.steps().get(0);
        assertEquals(Map.of("cable", 4), asMap(cable.outputs()));
        assertEquals(Map.of("copper", 2), asMap(cable.inputs()));
    }

    @Test
    void theSurplusOfAnOverProducingStepFeedsTheNextRequestForIt() {
        // Two circuits want 6 cable, which is exactly 3 crafts -- and asking for three shows that
        // the resolver counts cable once across the whole plan rather than per parent craft.
        PlanResolver.Resolution resolution = resolver().resolve("circuit", 2, have("copper", 3, "plate", 2));

        assertTrue(resolution.complete());
        assertEquals(Map.of("cable", 6), asMap(resolution.steps().get(0).outputs()));
        assertEquals(Map.of("copper", 3, "plate", 2), asMap(resolution.rawCost()));
    }

    @Test
    void theInventoryItWasHandedIsNeverSpent() {
        ItemBag inventory = have("plate", 2);

        resolver().resolve("gear", 1, inventory);

        assertEquals(2, inventory.count("plate"));
    }

    @Test
    void everyStepOutputAppearsAsSomethingToCraft() {
        PlanResolver.Resolution resolution = resolver().resolve("circuit", 1, have("copper", 2, "plate", 1));

        assertEquals(Map.of("cable", 4, "circuit", 1), asMap(resolution.toCraft()));
    }

    @Test
    void anUnknownRecipeResolvesToNothingRatherThanThrowing() {
        PlanResolver.Resolution resolution = resolver().resolve("nonesuch", 1, have("plate", 64));

        assertFalse(resolution.complete());
        assertTrue(resolution.steps().isEmpty());
    }

    @Test
    void aCycleInTheGraphTerminatesAsMissingInsteadOfHanging() {
        // The corpus check forbids this, and a runtime graph is assembled from whatever is loaded --
        // so the resolver must not be the thing that hangs when a pack author writes one.
        RecipeGraph cyclic = RecipeGraph.builder()
                .add(new HandRecipe("a", List.of(new ItemAmount("b", 1)), List.of(new ItemAmount("a", 1)), 10))
                .add(new HandRecipe("b", List.of(new ItemAmount("a", 1)), List.of(new ItemAmount("b", 1)), 10))
                .build();

        PlanResolver.Resolution resolution = new PlanResolver(cyclic, Set.of()::contains).resolve("a", 1, have());

        assertFalse(resolution.complete());
        assertEquals(Map.of("a", 1), asMap(resolution.missing()));
    }

    @Test
    void aLockedRootIsLockedRatherThanSilentlyUnplannable() {
        PlanResolver locked = new PlanResolver(graph(), Set.of("gear")::contains);

        PlanResolver.Resolution resolution = locked.resolve("gear", 4, have("plate", 64));

        assertFalse(resolution.complete());
        assertEquals(Map.of("gear", 4), asMap(resolution.locked()));
        assertTrue(resolution.missing().isEmpty());
        assertTrue(resolution.steps().isEmpty());
    }

    @Test
    void aQuantityThatWouldOverflowAnIntIsRefusedRatherThanWrappingNegative() {
        // The cap is on the craft count, not on the product of a count and an ingredient's amount.
        // A wrapped negative demand reads as already satisfied, which would report a complete plan
        // that reserves nothing and then cannot feed its own first step.
        RecipeGraph greedy = RecipeGraph.builder()
                .add(new HandRecipe("greedy", List.of(new ItemAmount("leaf", 1_000_000)),
                        List.of(new ItemAmount("greedy", 1)), 1))
                .build();

        PlanResolver.Resolution resolution = new PlanResolver(greedy, Set.of()::contains)
                .resolve("greedy", PlanResolver.MAX_CRAFTS, have("leaf", Integer.MAX_VALUE));

        assertFalse(resolution.complete());
        for (ItemAmount amount : resolution.rawCost()) {
            assertTrue(amount.count() > 0, "raw cost went negative: " + amount);
        }
    }

    @Test
    void aSubCraftBeyondTheCraftLimitIsMissingRatherThanQuietlyShort() {
        // The cap has to refuse rather than clamp. A clamped sub-craft makes fewer intermediates
        // than the parent step's inputs name, and nothing downstream notices: the plan reports
        // complete, Start takes the reservation, and the queue throws on a step it cannot feed.
        RecipeGraph deep = RecipeGraph.builder()
                .add(new HandRecipe("sub", List.of(new ItemAmount("leaf", 1)),
                        List.of(new ItemAmount("sub", 1)), 1))
                .add(new HandRecipe("bulk", List.of(new ItemAmount("sub", 3)),
                        List.of(new ItemAmount("bulk", 1)), 1))
                .build();
        ItemBag plenty = have("leaf", Integer.MAX_VALUE);

        PlanResolver.Resolution resolution = new PlanResolver(deep, Set.of()::contains)
                .resolve("bulk", PlanResolver.MAX_CRAFTS / 2, plenty);

        assertFalse(resolution.complete());
        assertEquals(Map.of("sub", 3 * (PlanResolver.MAX_CRAFTS / 2)), asMap(resolution.missing()));
    }

    @Test
    void largestAffordableIsTheBiggestCountTheInventoryCovers() {
        assertEquals(3, resolver().largestAffordable("gear", have("plate", 7)));
        assertEquals(0, resolver().largestAffordable("gear", have("plate", 1)));
    }

    @Test
    void largestAffordableCountsThroughTheChain() {
        // 5 copper makes 10 cable, enough for 3 circuits; 3 plate is the binding constraint.
        assertEquals(3, resolver().largestAffordable("circuit", have("copper", 5, "plate", 3)));
    }

    @Test
    void largestAffordableIsZeroWhenTheRecipeIsLocked() {
        PlanResolver locked = new PlanResolver(graph(), Set.of("gear")::contains);

        assertEquals(0, locked.largestAffordable("gear", have("plate", 64)));
    }
}
