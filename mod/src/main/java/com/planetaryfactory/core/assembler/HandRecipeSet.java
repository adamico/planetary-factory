package com.planetaryfactory.core.assembler;

import java.util.Set;

/**
 * The client's copy of which recipe ids the Assembler can plan.
 *
 * <p>The hand set is a fact about the loaded recipes and therefore server truth -- {@link
 * RuntimeHandRecipes} derives it from the recipe manager, which the client has no filtered view of.
 * EMI's fill button is drawn on the client and has to be right before anything is clicked, so the
 * ids are synced rather than asked for one at a time.
 *
 * <p>Ids only. Not the recipes: what a plan costs, what is missing and what is locked stay entirely
 * on the server, where they are re-derived per player anyway. This answers exactly one question --
 * is there any point offering this recipe to the Assembler -- and nothing else.
 *
 * <p>Empty until the first sync, and an empty set offers nothing. That is the safe direction: a
 * button that has not appeared yet is a moment's wait, and a button that opens a dialog which can
 * only refuse is a dead end the player has to read to discover.
 *
 * <p>Free of {@code net.minecraft.client}, like {@link AssemblerQueueView}, so a dedicated server
 * can load the packet's handler without reaching for a class that is not there.
 */
public final class HandRecipeSet {

    private static volatile Set<String> ids = Set.of();

    private HandRecipeSet() {
    }

    public static void accept(Set<String> synced) {
        ids = Set.copyOf(synced);
    }

    public static boolean contains(String recipeId) {
        return ids.contains(recipeId);
    }

    public static int size() {
        return ids.size();
    }
}
