package com.planetaryfactory.core.research.client;

import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.kind.GTRecipe;
import com.planetaryfactory.core.research.MachineLockStatus;
import com.planetaryfactory.core.research.RecipeLockLookup;
import com.planetaryfactory.core.research.RecipeResearchIndex;
import com.planetaryfactory.core.research.ResearchLocks;
import com.portingdeadmods.researchd.api.ResearchdApi;
import com.portingdeadmods.researchd.api.research.Research;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

/**
 * What an idle GregTech machine says when a research is the only thing stopping it (issue #79).
 *
 * <p>The machine screen already has a place for "why am I not running": {@code IFancyTooltip}, which
 * GregTech answers from {@code waitingReason}. A lock never gets that far -- ADR-0027 -- so the
 * reason is worked out here instead, from the contents the machine holds at the moment of asking.
 *
 * <p><b>The candidates are GregTech's own search, minus the pack's lock.</b>
 * {@code RecipeLogic.matchRecipe} is exactly {@code return RecipeHelper.matchContents(machine,
 * recipe);} -- the whole method body in 7.0.2, read off the shipped jar; the conditions check lives
 * in {@code checkRecipe}, not here -- and the pack's refusal is a wrapper around it -- so searching the trie with {@code matchContents} directly yields
 * the recipes the machine <em>would</em> have matched had nothing been locked. Anything else would
 * be a second, divergent notion of "the machine has the ingredients for this".
 *
 * <p><b>Nothing is stored.</b> The search runs per query, so emptying the machine or completing the
 * research changes the next answer with no invalidation rule to get wrong -- the requirement the
 * issue makes of this fix, and the trap #76 fell into. The cost is a trie search per query -- and
 * the machine screen asks up to three times a frame, see {@code RecipeLogicStatusMixin} -- paid only
 * while a player has an idle machine's screen open; the {@code isIdle} gate keeps a working machine
 * off this path entirely.
 *
 * <p><b>Read as the local player's team.</b> Client-side, so the lock is the one the player looking
 * at the machine is under -- the same frame the recipe viewer annotates in, and for the machine's
 * own owner (the ordinary case) the same lock the machine is refusing on the server.
 */
public final class IdleMachineLockNote {

    private IdleMachineLockNote() {}

    /**
     * The lines to show on an idle {@code machine}, or an empty list when research is not why it is
     * idle -- so an empty result is the "say nothing" signal and the caller needs no second
     * question.
     *
     * <p>Never throws. This is a tooltip on a hot GUI path that reaches into two other mods'
     * registries, and no status line is worth taking the screen down with it.
     */
    public static List<Component> tooltipFor(IRecipeLogicMachine machine) {
        try {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) return List.of();

            RecipeResearchIndex<ResourceLocation, ResourceKey<Research>> index = ResearchLocks.index(player.level());
            RecipeLockLookup<ResourceLocation, ResourceKey<Research>> lookup =
                    RecipeLockLookup.of(index, id -> ResearchdApi.isRecipeBlocked(player, id));

            return MachineLockStatus.lockStopping(() -> candidateIds(machine), lookup)
                    .map(lock -> LockedByResearchLines.of(lock.unlockingResearches(), player.level()))
                    .orElse(List.of());
        } catch (RuntimeException | LinkageError ignored) {
            // A status line is never worth a broken screen.
            return List.of();
        }
    }

    /** The ids of the recipes the machine's current contents match, lazily, lock ignored. */
    private static Iterator<ResourceLocation> candidateIds(IRecipeLogicMachine machine) {
        Iterator<GTRecipe> matching = machine.getRecipeType()
                .searchRecipe(machine, recipe -> RecipeHelper.matchContents(machine, recipe)
                        .isSuccess());
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return matching.hasNext();
            }

            @Override
            public ResourceLocation next() {
                return matching.next().id;
            }
        };
    }
}
