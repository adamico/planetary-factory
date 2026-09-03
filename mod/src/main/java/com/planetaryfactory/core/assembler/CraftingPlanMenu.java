package com.planetaryfactory.core.assembler;

import com.planetaryfactory.core.PFMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

/**
 * The Crafting Plan, the second server-opened dialog and the plan-result's carrier.
 *
 * <p>The plan itself never comes here. What the client gets is a {@link PlanDisplay} -- the flattened
 * tree in three categories -- and the {@link CraftingPlan} that Start will actually queue stays on the
 * server, held against this player until they Start or walk away. That is what makes Start a bare
 * "yes": there is nothing for the client to send back and therefore nothing to re-validate.
 */
public final class CraftingPlanMenu extends DialogMenu {

    private final PlanDisplay display;

    public CraftingPlanMenu(int containerId, Inventory inventory, PlanDisplay display) {
        super(PFMenus.CRAFTING_PLAN.get(), containerId);
        this.display = display;
    }

    public CraftingPlanMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory, PlanDisplay.STREAM_CODEC.decode(buffer));
    }

    public PlanDisplay display() {
        return display;
    }
}
