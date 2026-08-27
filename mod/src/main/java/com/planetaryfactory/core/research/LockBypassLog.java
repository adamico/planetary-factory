package com.planetaryfactory.core.research;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Decides whether an unowned machine's locked-recipe bypass is worth a log line.
 *
 * <p>A machine carrying no Researchd placed-by attachment pushes no filter frame, so the pack's
 * recipe wrapper has no team to test against and falls through -- every locked recipe runs. Failing
 * open is the deliberate choice (issue #74): a machine that genuinely belongs to no team has no
 * lock to honour, and refusing would turn a silent bypass into a silent brick with no player
 * remedy. What was wrong was that the case was <em>invisible</em>: a lock that does not apply looked
 * exactly like a lock that was never implemented.
 *
 * <p>Two conditions gate a line, and the order matters.
 *
 * <ol>
 *   <li>The recipe has to be one some research unlocks. The overwhelming majority of GT recipes are
 *       not gated at all, and an unowned machine running one of those is not a bypass -- reporting
 *       it would bury the real case.
 *   <li>The site has to be one not already reported this session. A machine ticks; without this a
 *       single unowned Assembler would fill the log by itself.
 * </ol>
 *
 * <p>The dedupe key is the site alone, not the site and recipe. One line per machine is enough to
 * send a reader to that position, and a multi-recipe machine would otherwise report once per recipe
 * it ever runs.
 *
 * <p>Per session, and deliberately not persisted: the point is to surface the case to whoever is
 * reading this run's log. A datapack reload does not reset it either -- a site already in the log is
 * already known to whoever is reading, and re-reporting every machine on each {@code /reload} is the
 * flood the dedupe exists to prevent. The cost is that the set is never emptied; it is bounded by
 * the number of distinct unowned machines a session ever ticks, which is a handful in any world
 * where this fires at all and zero in a world where it does not.
 *
 * <p>Minecraft-free, so it is a plain-JVM unit under the pack's testing policy.
 */
public final class LockBypassLog {

    /**
     * A machine position, as a plain value. Dimension is carried as its id string so that this
     * class stays free of Minecraft types -- two machines at the same coordinates in different
     * dimensions are different sites.
     */
    public record Site(String dimension, int x, int y, int z) {}

    private final Set<Site> reported = ConcurrentHashMap.newKeySet();

    /**
     * Whether this bypass should produce a log line -- true at most once per site per session, and
     * only for a recipe some research unlocks.
     *
     * <p>A recipe no research unlocks never consumes the site's one slot, so a machine that runs a
     * hundred ungated recipes before its first gated one still reports that one.
     */
    public <I> boolean shouldReport(RecipeResearchIndex<I, ?> index, I recipeId, Site site) {
        if (!index.isUnlockedByResearch(recipeId)) return false;
        return this.reported.add(site);
    }

}
