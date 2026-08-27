package com.planetaryfactory.core.research;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/** Issue #74's acceptance criteria for the bypass log, as assertions. */
class LockBypassLogTest {

    private static final LockBypassLog.Site ASSEMBLER = new LockBypassLog.Site("minecraft:overworld", 12, 64, -30);

    private static final RecipeResearchIndex<String, String> INDEX = RecipeResearchIndex.<String, String>builder()
            .add("steam_power", List.of("gtceu:bronze_boiler"))
            .build();

    @Test
    void reportsAnUnownedMachineRunningALockedRecipe() {
        assertTrue(new LockBypassLog().shouldReport(INDEX, "gtceu:bronze_boiler", ASSEMBLER));
    }

    @Test
    void reportsASiteOnlyOnce() {
        LockBypassLog log = new LockBypassLog();

        assertTrue(log.shouldReport(INDEX, "gtceu:bronze_boiler", ASSEMBLER));
        assertFalse(log.shouldReport(INDEX, "gtceu:bronze_boiler", ASSEMBLER), "a machine ticks");
        assertFalse(log.shouldReport(INDEX, "gtceu:bronze_boiler", ASSEMBLER));
    }

    @Test
    void saysNothingAboutARecipeNoResearchUnlocks() {
        assertFalse(new LockBypassLog().shouldReport(INDEX, "minecraft:stick", ASSEMBLER));
    }

    @Test
    void anUnlockedRecipeDoesNotConsumeTheSitesOneReport() {
        LockBypassLog log = new LockBypassLog();

        assertFalse(log.shouldReport(INDEX, "minecraft:stick", ASSEMBLER));

        assertTrue(
                log.shouldReport(INDEX, "gtceu:bronze_boiler", ASSEMBLER),
                "an ungated recipe must not swallow the line the gated one deserves");
    }

    @Test
    void reportsASecondLockedRecipeAtAReportedSiteNoFurther() {
        RecipeResearchIndex<String, String> two = RecipeResearchIndex.<String, String>builder()
                .add("steam_power", List.of("gtceu:bronze_boiler", "gtceu:steam_turbine"))
                .build();
        LockBypassLog log = new LockBypassLog();

        assertTrue(log.shouldReport(two, "gtceu:bronze_boiler", ASSEMBLER));

        assertFalse(
                log.shouldReport(two, "gtceu:steam_turbine", ASSEMBLER),
                "the dedupe key is the site, so one line names the machine, not each recipe");
    }

    @Test
    void reportsEachSiteSeparately() {
        LockBypassLog log = new LockBypassLog();
        LockBypassLog.Site other = new LockBypassLog.Site("minecraft:overworld", 12, 64, -31);

        assertTrue(log.shouldReport(INDEX, "gtceu:bronze_boiler", ASSEMBLER));
        assertTrue(log.shouldReport(INDEX, "gtceu:bronze_boiler", other));
    }

    @Test
    void tellsTheSameCoordinatesInTwoDimensionsApart() {
        LockBypassLog log = new LockBypassLog();
        LockBypassLog.Site elsewhere = new LockBypassLog.Site("planetaryfactory:sapros", 12, 64, -30);

        assertTrue(log.shouldReport(INDEX, "gtceu:bronze_boiler", ASSEMBLER));
        assertTrue(log.shouldReport(INDEX, "gtceu:bronze_boiler", elsewhere));
    }

    @Test
    void keepsReportedSitesForTheWholeSession() {
        LockBypassLog log = new LockBypassLog();
        RecipeResearchIndex<String, String> afterReload = RecipeResearchIndex.<String, String>builder()
                .add("steam_power", List.of("gtceu:bronze_boiler"))
                .build();

        assertTrue(log.shouldReport(INDEX, "gtceu:bronze_boiler", ASSEMBLER));

        assertFalse(
                log.shouldReport(afterReload, "gtceu:bronze_boiler", ASSEMBLER),
                "the dedupe is per session, so rebuilding the index does not re-report a known site");
    }

    @Test
    void saysNothingWhenNoResearchUnlocksAnything() {
        assertFalse(new LockBypassLog()
                .shouldReport(RecipeResearchIndex.<String, String>empty(), "gtceu:bronze_boiler", ASSEMBLER));
    }

    /**
     * Machines tick on the server thread, but GT multiblocks and the pack's other mods make it
     * cheap to be wrong about that. One line means one line however many threads race for it.
     */
    @Test
    void reportsOnceUnderConcurrentTicks() throws Exception {
        LockBypassLog log = new LockBypassLog();
        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            List<Callable<Boolean>> ticks = IntStream.range(0, threads)
                    .<Callable<Boolean>>mapToObj(i -> () -> log.shouldReport(INDEX, "gtceu:bronze_boiler", ASSEMBLER))
                    .toList();

            long reported = pool.invokeAll(ticks).stream()
                    .map(LockBypassLogTest::get)
                    .filter(Boolean::booleanValue)
                    .count();

            assertEquals(1, reported);
        } finally {
            pool.shutdownNow();
        }
    }

    private static boolean get(Future<Boolean> future) {
        try {
            return future.get();
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
