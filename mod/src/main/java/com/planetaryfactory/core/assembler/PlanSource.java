package com.planetaryfactory.core.assembler;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Where a Crafting Plan comes from: the resolver seam, which is #161's to fill.
 *
 * <p>#160 owns the panel, the two dialog menus, the packets and the queue, and deliberately not the
 * resolver -- so this interface is the line between the two tickets. {@link #ACTIVE} is what the
 * server calls, and #161's edit is to point it at a real resolver instead of {@link Unresolved};
 * nothing outside this file has to move.
 *
 * <p>Resolving is server-side because a plan is server truth (ADR-0038): it reads the player's
 * inventory <em>and</em> the team's Researchd state, and Start takes the reservation off the back of
 * it. A client-held plan would have to be re-validated at Start, which is exactly the re-validation
 * paying up front was meant to remove.
 */
public interface PlanSource {

    /** The resolver in force. #161 assigns the real one; until then nothing resolves. */
    PlanSource ACTIVE = new Unresolved();

    /**
     * Resolves {@code amount} of a recipe against what the player has and what their team has
     * researched.
     */
    ResolvedPlan resolve(ServerPlayer player, ResourceLocation recipe, int amount);

    /**
     * The largest count whose complete plan the inventory covers -- Select Amount's {@code all}, so
     * that {@code all} can never produce a plan that Start then refuses.
     */
    int largestAffordable(ServerPlayer player, ResourceLocation recipe);

    /**
     * A resolution. {@code plan} is null exactly when the plan is incomplete, which is the one
     * condition Start refuses on.
     */
    record ResolvedPlan(PlanDisplay display, CraftingPlan plan) {

        public boolean complete() {
            return plan != null;
        }
    }

    /**
     * The stand-in until #161 lands: every plan comes back incomplete, so the dialogs open and the
     * round trip is exercisable while Start stays correctly refused.
     *
     * <p>It is not a silent no-op. An incomplete plan is a real state of the real dialog, so what a
     * player sees here is the shape they will see later for a recipe they genuinely cannot afford --
     * which is what makes this a stub rather than a lie.
     */
    final class Unresolved implements PlanSource {

        @Override
        public ResolvedPlan resolve(ServerPlayer player, ResourceLocation recipe, int amount) {
            return new ResolvedPlan(
                    new PlanDisplay(PlanDisplay.NO_PLAN, recipe, amount, List.of(), List.of(), List.of(), false),
                    null);
        }

        @Override
        public int largestAffordable(ServerPlayer player, ResourceLocation recipe) {
            return 0;
        }
    }
}
