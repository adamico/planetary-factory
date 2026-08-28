package com.planetaryfactory.core.research;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

/** Issue #76's acceptance criteria for the retry memory, as assertions. */
class LockedRecipeRetryTest {

    /** Stands in for a {@code GTRecipe}: an id, and an object identity distinct from it. */
    private record Recipe(String id) {}

    private static final Function<Recipe, String> ID = Recipe::id;

    private static final Recipe STICKY_PISTON = new Recipe("gtceu:assembler/sticky_piston_slime");
    private static final Recipe CONVEYOR = new Recipe("gtceu:assembler/conveyor_module_lv");

    @Test
    void remembersARefusedRecipeSoTheMachineKeepsTicking() {
        List<Recipe> remembered = LockedRecipeRetry.remember(null, STICKY_PISTON, ID);

        assertEquals(List.of(STICKY_PISTON), remembered, "an empty memory is what unsubscribes the machine");
    }

    @Test
    void keepsWhatWasAlreadyRemembered() {
        List<Recipe> first = LockedRecipeRetry.remember(null, STICKY_PISTON, ID);

        assertEquals(List.of(STICKY_PISTON, CONVEYOR), LockedRecipeRetry.remember(first, CONVEYOR, ID));
    }

    @Test
    void remembersARecipeOnlyOnce() {
        List<Recipe> once = LockedRecipeRetry.remember(null, STICKY_PISTON, ID);

        assertEquals(List.of(STICKY_PISTON), LockedRecipeRetry.remember(once, STICKY_PISTON, ID));
    }

    /**
     * The re-check path runs the refused recipe through {@code fullModifyRecipe} first, so what
     * comes back each tick is a new object carrying the same id. Identity dedup would grow the list
     * for as long as the machine waits for its research.
     */
    @Test
    void treatsAModifiedCopyAsTheSameRecipe() {
        Recipe modified = new Recipe(STICKY_PISTON.id());
        assertNotSame(STICKY_PISTON, modified);

        List<Recipe> remembered =
                LockedRecipeRetry.remember(LockedRecipeRetry.remember(null, STICKY_PISTON, ID), modified, ID);

        assertEquals(1, remembered.size(), "one waiting recipe is one entry, however many ticks pass");
    }

    /**
     * GregTech iterates this list on the same tick that the pack writes to it -- the iteration calls
     * back into the refusal. Mutating in place would throw on the machine's own tick.
     */
    @Test
    void leavesTheListItWasGivenUntouched() {
        List<Recipe> held = LockedRecipeRetry.remember(null, STICKY_PISTON, ID);

        List<Recipe> next = LockedRecipeRetry.remember(held, CONVEYOR, ID);

        assertNotSame(held, next);
        assertEquals(List.of(STICKY_PISTON), held, "an in-flight iteration must see what it started with");
    }

    /** A no-op remember still hands back a copy, so the same guarantee holds on the dedup path. */
    @Test
    void copiesEvenWhenNothingIsAdded() {
        List<Recipe> held = LockedRecipeRetry.remember(null, STICKY_PISTON, ID);

        assertNotSame(held, LockedRecipeRetry.remember(held, STICKY_PISTON, ID));
    }

    /** {@code handleSearchingRecipes} adds to this field itself, so what is left there must take it. */
    @Test
    void handsBackAListGregTechCanAddTo() {
        List<Recipe> remembered = LockedRecipeRetry.remember(null, STICKY_PISTON, ID);

        remembered.add(CONVEYOR);

        assertTrue(remembered.contains(CONVEYOR));
    }

    @Test
    void takesOverAListGregTechBuilt() {
        List<Recipe> gregtechs = new ArrayList<>(List.of(CONVEYOR));

        List<Recipe> remembered = LockedRecipeRetry.remember(gregtechs, STICKY_PISTON, ID);

        assertEquals(List.of(CONVEYOR, STICKY_PISTON), remembered);
        assertSame(CONVEYOR, remembered.get(0));
    }
}
