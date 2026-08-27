package com.planetaryfactory.core.research;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * The question a recipe viewer asks per recipe (issue #75): is this id locked for the team I am
 * looking at it as, and which research would unlock it? Strings stand in for
 * {@code ResourceLocation} and {@code ResourceKey<Research>}; the lookup never looks inside either.
 */
class RecipeLockLookupTest {

    private static final RecipeResearchIndex<String, String> INDEX = RecipeResearchIndex.<String, String>builder()
            .add("steam_power", List.of("gtceu:steam_turbine", "gtceu:bronze_boiler"))
            .add("alloys", List.of("gtceu:bronze_boiler"))
            .build();

    private static RecipeLockLookup<String, String> lockingNothing() {
        return RecipeLockLookup.of(INDEX, id -> false);
    }

    private static RecipeLockLookup<String, String> lockingEverything() {
        return RecipeLockLookup.of(INDEX, id -> true);
    }

    @Test
    void aRecipeTheTeamCanRunIsNotAnnotated() {
        assertTrue(lockingNothing().lockOn("gtceu:steam_turbine").isEmpty());
    }

    @Test
    void aLockedRecipeNamesTheResearchThatUnlocksIt() {
        Optional<RecipeLockLookup.Lock<String>> lock = lockingEverything().lockOn("gtceu:steam_turbine");

        assertTrue(lock.isPresent());
        assertEquals(Set.of("steam_power"), lock.get().unlockingResearches());
    }

    @Test
    void aLockedRecipeNamesEveryResearchThatUnlocksIt() {
        assertEquals(
                List.of("steam_power", "alloys"),
                List.copyOf(lockingEverything()
                        .lockOn("gtceu:bronze_boiler")
                        .orElseThrow()
                        .unlockingResearches()),
                "in index order, so a redraw does not shuffle the tooltip");
    }

    /**
     * The team's blocked-recipe data is synced from the server and the index is rebuilt from the
     * research registry; nothing makes the two agree at every instant. When they disagree the lock
     * wins -- annotating a recipe the machine will refuse is right even when the research behind it
     * cannot be named, and quietly dropping the mark because the index lagged is not.
     */
    @Test
    void aLockedRecipeTheIndexDoesNotKnowIsStillLockedAndNamesNothing() {
        Optional<RecipeLockLookup.Lock<String>> lock = lockingEverything().lockOn("minecraft:stick");

        assertTrue(lock.isPresent());
        assertTrue(lock.get().unlockingResearches().isEmpty());
    }

    /**
     * Both viewers hand out recipes with no id -- EMI synthesises them, and JEI's
     * {@code getRegistryName} is a nullable default. There is nothing to ask about, and asking
     * Researchd with a null id would be the crash.
     */
    @Test
    void aRecipeWithNoIdIsNotLockedAndIsNeverAskedAbout() {
        List<String> asked = new ArrayList<>();
        RecipeLockLookup<String, String> lookup = RecipeLockLookup.of(INDEX, id -> {
            asked.add(id);
            return true;
        });

        assertTrue(lookup.lockOn(null).isEmpty());
        assertTrue(asked.isEmpty(), "the viewer's null id never reaches Researchd");
    }

    @Test
    void anEmptyIndexStillReportsTheLockItCannotName() {
        RecipeLockLookup<String, String> lookup = RecipeLockLookup.of(RecipeResearchIndex.empty(), id -> true);

        assertTrue(lookup.lockOn("gtceu:steam_turbine").isPresent());
    }

    @Test
    void theResearchesOfALockCannotBeEditedByItsReader() {
        Set<String> researches =
                lockingEverything().lockOn("gtceu:steam_turbine").orElseThrow().unlockingResearches();

        assertThrows(UnsupportedOperationException.class, () -> researches.add("electricity"));
    }

    @Test
    void theLockIsDecidedByTheTeamsDataNotByTheIndex() {
        assertFalse(
                lockingNothing().lockOn("gtceu:bronze_boiler").isPresent(),
                "an id some research unlocks is not locked once that research is complete");
    }
}
