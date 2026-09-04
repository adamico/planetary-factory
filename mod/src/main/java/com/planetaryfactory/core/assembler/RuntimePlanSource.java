package com.planetaryfactory.core.assembler;

import com.portingdeadmods.researchd.api.ResearchdApi;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * The resolver, wired to a running server (#161).
 *
 * <p>It holds no rules. Every decision -- what to chain-craft, what is Missing, what is Locked, what
 * {@code all} comes to -- is {@link PlanResolver}'s and is unit-tested without Minecraft. This class
 * supplies the three things the resolver cannot get for itself: the graph of loaded hand recipes,
 * the player's inventory as a multiset, and the team's Researchd state as a predicate.
 *
 * <p>It is the glue row of the pack's testing policy, checked by a world load rather than by a test.
 */
public final class RuntimePlanSource implements PlanSource {

    @Override
    public ResolvedPlan resolve(ServerPlayer player, ResourceLocation recipe, int amount) {
        PlanResolver.Resolution resolution =
                resolverFor(player).resolve(recipe.toString(), amount, inventoryOf(player));

        if (!resolution.complete()) {
            return new ResolvedPlan(
                    new PlanDisplay(PlanDisplay.NO_PLAN, recipe, amount, resolution.rawCost(),
                            resolution.toCraft(), resolution.missing(), resolution.locked(), false),
                    null);
        }
        UUID id = UUID.randomUUID();
        String root = rootItemOf(player, recipe);
        return new ResolvedPlan(
                new PlanDisplay(id, recipe, amount, resolution.rawCost(), resolution.toCraft(),
                        List.of(), List.of(), true),
                resolution.toPlan(id, root, amount));
    }

    @Override
    public int largestAffordable(ServerPlayer player, ResourceLocation recipe) {
        return resolverFor(player).largestAffordable(recipe.toString(), inventoryOf(player));
    }

    private PlanResolver resolverFor(ServerPlayer player) {
        return new PlanResolver(
                RuntimeHandRecipes.graph(player.level()),
                // Researchd's own question, asked per team. A recipe nothing locks is never blocked,
                // so the common case costs one lookup and no research state at all.
                recipeId -> {
                    ResourceLocation key = ResourceLocation.tryParse(recipeId);
                    return key != null && ResearchdApi.isRecipeBlocked(player, key);
                });
    }

    /**
     * What the plan is for, named for the panel's queue row.
     *
     * <p>Read back off the graph rather than carried through the resolver: the resolver plans by
     * recipe and a recipe knows its own first output, so passing the item alongside would be two
     * facts that can disagree.
     */
    private static String rootItemOf(ServerPlayer player, ResourceLocation recipe) {
        HandRecipe root = RuntimeHandRecipes.graph(player.level()).byId(recipe.toString());
        return root == null ? recipe.toString() : root.primaryOutput();
    }

    /**
     * The player's stock as the resolver counts it: the main inventory and hotbar, the same thirty-six
     * slots {@code InventoryPlayerItems} will take the reservation from.
     *
     * <p>They have to be the same slots. A plan resolved against armour the queue then cannot spend
     * would be complete in the dialog and refused at Start.
     */
    private static ItemBag inventoryOf(ServerPlayer player) {
        ItemBag bag = new ItemBag();
        for (ItemStack stack : player.getInventory().items) {
            if (stack.isEmpty()) continue;
            bag.add(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), stack.getCount());
        }
        return bag;
    }
}
