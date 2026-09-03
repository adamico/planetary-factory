package com.planetaryfactory.core.assembler;

import com.planetaryfactory.core.PFMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Select Amount, the first of the two server-opened dialogs.
 *
 * <p>A menu and not a client screen, for the reason ADR-0038 gives: {@code all} is the resolver's
 * answer, and the resolver reads the player's inventory and their team's research. It also sidesteps
 * EMI entirely -- {@code craft()} returns true and EMI restores the panel screen, and the server
 * opens this a moment later, so nothing races EMI's own {@code setScreen}.
 *
 * <p>{@code amount} arrives from {@code EmiCraftContext.getAmount()}: {@code 1} on a click and
 * {@code Integer.MAX_VALUE} on a shift-click, which is Factorio's one-and-all for free.
 */
public final class SelectAmountMenu extends DialogMenu {

    private final ResourceLocation recipe;
    private final int amount;
    private final int largestAffordable;

    public SelectAmountMenu(int containerId, Inventory inventory, ResourceLocation recipe, int amount, int largestAffordable) {
        super(PFMenus.SELECT_AMOUNT.get(), containerId);
        this.recipe = recipe;
        this.amount = amount;
        this.largestAffordable = largestAffordable;
    }

    /** The client's side of the open, reading what the server wrote. */
    public SelectAmountMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory, ResourceLocation.STREAM_CODEC.decode(buffer), buffer.readVarInt(), buffer.readVarInt());
    }

    public ResourceLocation recipe() {
        return recipe;
    }

    /** The amount the button asked for, which is Select Amount's starting value. */
    public int amount() {
        return amount;
    }

    /** {@code all}: the largest count whose complete plan the inventory covers. */
    public int largestAffordable() {
        return largestAffordable;
    }
}
