package com.planetaryfactory.core.research;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * The question an idle machine answers when something asks why it is doing nothing (issue #79):
 * given the recipes its current contents match, is an incomplete research the only thing stopping
 * it, and which research is it? Strings stand in for {@code ResourceLocation} and
 * {@code ResourceKey<Research>}, exactly as in {@link RecipeLockLookupTest}.
 */
class MachineLockStatusTest {

    private static final RecipeResearchIndex<String, String> INDEX = RecipeResearchIndex.<String, String>builder()
            .add("steam_power", List.of("gtceu:steam_turbine", "gtceu:bronze_boiler"))
            .add("alloys", List.of("gtceu:bronze_boiler"))
            .add("electricity", List.of("gtceu:lv_macerator"))
            .build();

    private static RecipeLockLookup<String, String> locking(String... lockedIds) {
        Set<String> locked = Set.of(lockedIds);
        return RecipeLockLookup.of(INDEX, locked::contains);
    }

    /** Nothing in the machine, nothing to explain -- an empty machine is idle for the obvious reason. */
    @Test
    void anEmptyMachineReportsNothing() {
        assertEquals(Optional.empty(), MachineLockStatus.lockStopping(List.of(), locking("gtceu:steam_turbine")));
    }

    @Test
    void aLockedCandidateNamesTheResearchThatUnlocksIt() {
        Optional<RecipeLockLookup.Lock<String>> lock =
                MachineLockStatus.lockStopping(List.of("gtceu:steam_turbine"), locking("gtceu:steam_turbine"));

        assertEquals(Set.of("steam_power"), lock.orElseThrow().unlockingResearches());
    }

    /**
     * A machine whose contents match a recipe it may already run is idle for some other reason --
     * unpowered, output full, disabled. Claiming research is what stops it would be a lie, and a
     * worse one than saying nothing.
     */
    @Test
    void anUnlockedCandidateMeansResearchIsNotWhatStopsIt() {
        assertEquals(
                Optional.empty(),
                MachineLockStatus.lockStopping(List.of("gtceu:steam_turbine"), locking()),
                "the recipe is runnable, so the lock is not the reason");
    }

    @Test
    void oneRunnableCandidateAmongLockedOnesStillMeansResearchIsNotWhatStopsIt() {
        assertEquals(
                Optional.empty(),
                MachineLockStatus.lockStopping(
                        List.of("gtceu:steam_turbine", "gtceu:lv_macerator"), locking("gtceu:steam_turbine")));
    }

    /**
     * Every candidate locked, so research really is the whole reason -- and completing any one of
     * the named researches starts the machine, so all of them are named.
     */
    @Test
    void severalLockedCandidatesNameEveryResearchBetweenThem() {
        Optional<RecipeLockLookup.Lock<String>> lock = MachineLockStatus.lockStopping(
                List.of("gtceu:steam_turbine", "gtceu:lv_macerator"),
                locking("gtceu:steam_turbine", "gtceu:lv_macerator"));

        assertEquals(
                List.of("steam_power", "electricity"),
                List.copyOf(lock.orElseThrow().unlockingResearches()),
                "in candidate order then index order, so a redraw does not shuffle the tooltip");
    }

    @Test
    void aResearchUnlockingTwoLockedCandidatesIsNamedOnce() {
        Optional<RecipeLockLookup.Lock<String>> lock = MachineLockStatus.lockStopping(
                List.of("gtceu:steam_turbine", "gtceu:bronze_boiler"),
                locking("gtceu:steam_turbine", "gtceu:bronze_boiler"));

        assertEquals(
                List.of("steam_power", "alloys"),
                List.copyOf(lock.orElseThrow().unlockingResearches()));
    }

    /** The index lagging the team's data must not swallow the report -- see {@link RecipeLockLookup}. */
    @Test
    void aLockedCandidateTheIndexDoesNotKnowIsStillReportedAndNamesNothing() {
        Optional<RecipeLockLookup.Lock<String>> lock =
                MachineLockStatus.lockStopping(List.of("minecraft:stick"), locking("minecraft:stick"));

        assertTrue(lock.isPresent());
        assertTrue(lock.orElseThrow().unlockingResearches().isEmpty());
    }

    /**
     * The candidates arrive as a lazily-searched iterator over the machine's recipe trie, and the
     * first runnable one settles the answer. Walking the rest would be a full trie search per
     * status query for nothing.
     */
    @Test
    void theSearchStopsAtTheFirstRunnableCandidate() {
        List<String> walked = new ArrayList<>();
        List<String> candidates = List.of("gtceu:steam_turbine", "gtceu:lv_macerator", "gtceu:bronze_boiler");

        MachineLockStatus.lockStopping(
                () -> candidates.stream().peek(walked::add).iterator(), locking("gtceu:steam_turbine"));

        assertEquals(List.of("gtceu:steam_turbine", "gtceu:lv_macerator"), walked);
    }

    @Test
    void theResearchesOfAReportCannotBeEditedByItsReader() {
        Set<String> researches = MachineLockStatus.lockStopping(
                        List.of("gtceu:steam_turbine"), locking("gtceu:steam_turbine"))
                .orElseThrow()
                .unlockingResearches();

        assertThrows(UnsupportedOperationException.class, () -> researches.add("electricity"));
    }
}
