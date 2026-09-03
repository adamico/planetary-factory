package com.planetaryfactory.core.assembler;

import java.util.List;
import java.util.UUID;

/**
 * A resolved Crafting Plan: the flattened tree, in the order it will be crafted, and the raw cost
 * that pays for all of it.
 *
 * <p>This is what Start hands the queue, and it is the unit of cancellation (ADR-0038). By the time
 * one exists it is complete -- an incomplete plan is refused at the dialog and never reaches here,
 * which is why nothing in the queue asks whether a plan can be finished.
 *
 * <p>{@code rawCost} is the leaves only. The intermediates are made by the steps and never touched
 * by the player, so they are not part of what Start takes.
 *
 * <p>Resolving one is #161's; this record is the shape it produces.
 */
public record CraftingPlan(UUID id, String rootItem, int amount, List<ItemAmount> rawCost, List<CraftStep> steps) {

    public CraftingPlan {
        if (id == null) throw new IllegalArgumentException("a plan needs an id to be cancelled by");
        if (rootItem == null || rootItem.isBlank()) {
            throw new IllegalArgumentException("a plan needs the item it is for");
        }
        rawCost = List.copyOf(rawCost);
        steps = List.copyOf(steps);
    }
}
