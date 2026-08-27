package com.planetaryfactory.core.research;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * The index is what stands in for {@code isRecipeBlocked} when there is no team to ask about
 * (issue #74). Strings stand in for {@code ResourceLocation} and {@code ResourceKey<Research>};
 * the index never looks inside either, so nothing here is weakened by the substitution.
 */
class RecipeResearchIndexTest {

    private static RecipeResearchIndex<String, String> index() {
        return RecipeResearchIndex.<String, String>builder()
                .add("steam_power", List.of("gtceu:steam_turbine", "gtceu:bronze_boiler"))
                .add("electricity", List.of("gtceu:lv_circuit"))
                .build();
    }

    @Test
    void reportsRecipesSomeResearchUnlocks() {
        assertTrue(index().isUnlockedByResearch("gtceu:steam_turbine"));
        assertTrue(index().isUnlockedByResearch("gtceu:lv_circuit"));
    }

    @Test
    void doesNotReportARecipeNoResearchMentions() {
        assertFalse(index().isUnlockedByResearch("minecraft:stick"));
    }

    @Test
    void anEmptyIndexUnlocksNothing() {
        assertFalse(RecipeResearchIndex.<String, String>empty().isUnlockedByResearch("gtceu:lv_circuit"));
        assertEquals(0, RecipeResearchIndex.empty().size());
    }

    @Test
    void namesTheResearchThatUnlocksARecipe() {
        assertEquals(Set.of("steam_power"), index().researchesUnlocking("gtceu:bronze_boiler"));
    }

    @Test
    void namesEveryResearchWhenTwoUnlockTheSameRecipe() {
        RecipeResearchIndex<String, String> shared = RecipeResearchIndex.<String, String>builder()
                .add("steam_power", List.of("gtceu:alloy_smelter"))
                .add("alloys", List.of("gtceu:alloy_smelter"))
                .build();

        assertEquals(Set.of("steam_power", "alloys"), shared.researchesUnlocking("gtceu:alloy_smelter"));
        assertEquals(1, shared.size(), "one recipe id, however many researches name it");
    }

    @Test
    void namesTheResearchOnceWhenItRepeatsARecipe() {
        RecipeResearchIndex<String, String> repeated = RecipeResearchIndex.<String, String>builder()
                .add("steam_power", List.of("gtceu:bronze_boiler", "gtceu:bronze_boiler"))
                .build();

        assertEquals(Set.of("steam_power"), repeated.researchesUnlocking("gtceu:bronze_boiler"));
    }

    @Test
    void yieldsNoResearchesForAnUnlockedRecipe() {
        assertEquals(Set.of(), index().researchesUnlocking("minecraft:stick"));
    }

    @Test
    void keepsRegistryOrderSoAnAnnotationIsStable() {
        RecipeResearchIndex<String, String> shared = RecipeResearchIndex.<String, String>builder()
                .add("steam_power", List.of("gtceu:alloy_smelter"))
                .add("alloys", List.of("gtceu:alloy_smelter"))
                .build();

        assertEquals(List.of("steam_power", "alloys"), List.copyOf(shared.researchesUnlocking("gtceu:alloy_smelter")));
    }

    @Test
    void isImmutableOnceBuilt() {
        RecipeResearchIndex<String, String> built = index();

        assertThrows(
                UnsupportedOperationException.class,
                () -> built.researchesUnlocking("gtceu:lv_circuit").add("smuggled_in"));
    }

    @Test
    void ignoresLaterBuilderWrites() {
        RecipeResearchIndex.Builder<String, String> builder =
                RecipeResearchIndex.<String, String>builder().add("steam_power", List.of("gtceu:bronze_boiler"));
        RecipeResearchIndex<String, String> built = builder.build();

        builder.add("electricity", List.of("gtceu:lv_circuit"));

        assertFalse(built.isUnlockedByResearch("gtceu:lv_circuit"), "a built index is a snapshot");
        assertEquals(1, built.size());
    }
}
