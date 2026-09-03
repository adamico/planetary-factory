package com.planetaryfactory.core.assembler;

import com.planetaryfactory.core.PFMenus;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The Personal Assembler panel: the surface the queue is watched from, and the only route in.
 *
 * <p>It has no crafting grid and no machine inventory of its own -- the Assembler takes what it
 * needs from the player, whole, at Start (ADR-0038). What it does carry is the <em>player's</em>
 * inventory, all thirty-six slots, because the panel stands in for the inventory screen while it is
 * open: a player planning a craft is looking at what they have, and a panel that hid it would send
 * them back and forth to count.
 *
 * <p>Its other job is simply being a {@code MenuType}. EMI keys its recipe handlers by menu type, so
 * having this open is what makes {@code + Fill Recipe} appear -- and what makes "the panel is open"
 * the precondition of the only route in, with no craft-from-anywhere path.
 */
public final class AssemblerPanelMenu extends AbstractContainerMenu {

    /** Where the player's three rows start, in the screen's own coordinates. */
    public static final int PLAYER_INVENTORY_Y = 104;

    public static final int HOTBAR_Y = 162;

    private static final int HOTBAR_SLOTS = 9;
    private static final int PLAYER_SLOTS = 36;

    public AssemblerPanelMenu(int containerId, Inventory inventory) {
        super(PFMenus.ASSEMBLER_PANEL.get(), containerId);
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, 9 + row * 9 + column,
                        8 + column * 18, PLAYER_INVENTORY_Y + row * 18));
            }
        }
        for (int column = 0; column < HOTBAR_SLOTS; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, HOTBAR_Y));
        }
    }

    /** Always. The panel is the player's own, carried on them, with no block to walk away from. */
    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    /**
     * Shift-click moves between the three rows and the hotbar, and nowhere else.
     *
     * <p>There is no machine side to move into. Keeping the vanilla gesture working within the
     * player's own inventory is what stops the panel feeling like a different kind of screen.
     */
    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = slots.get(slotIndex);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        boolean fromMainRows = slotIndex < PLAYER_SLOTS - HOTBAR_SLOTS;
        boolean moved = fromMainRows
                ? moveItemStackTo(stack, PLAYER_SLOTS - HOTBAR_SLOTS, PLAYER_SLOTS, false)
                : moveItemStackTo(stack, 0, PLAYER_SLOTS - HOTBAR_SLOTS, false);
        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        return original;
    }
}
