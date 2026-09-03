package com.planetaryfactory.core.assembler;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

/**
 * A menu that exists to be a dialog: no slots, and valid wherever the player is.
 *
 * <p>Select Amount and the Crafting Plan are both this. They are menus rather than client screens
 * because a plan is server truth (ADR-0038), not because they hold anything -- so the two overrides
 * {@code AbstractContainerMenu} demands are the same answer twice, and are given here once.
 */
public abstract class DialogMenu extends AbstractContainerMenu {

    protected DialogMenu(MenuType<?> type, int containerId) {
        super(type, containerId);
    }

    /** Always: a dialog belongs to the player, with no block to walk away from. */
    @Override
    public final boolean stillValid(Player player) {
        return true;
    }

    /** Nothing to shift-click into: there are no slots. */
    @Override
    public final ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }
}
