package com.planetaryfactory.core.mixin.minecraft;

import com.planetaryfactory.core.crafting.RemovedGridSlot;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Removes the 2x2 inventory crafting grid (#140, ADR-0034).
 *
 * <p>The Personal Assembler is the pack's only hand-crafting surface (`#90`, `#95`), and until this
 * every other recipe removal was decoration: the 2x2 is part of a vanilla menu rather than a block,
 * so no recipe removal reaches it, and every surviving stock recipe that fits four ingredients was
 * still craftable there. That is ADR-0034's first exception class, and this is where it closes.
 *
 * <p>Two injections, because the grid has two halves and each fails independently.
 *
 * <ul>
 *   <li><b>The slots go.</b> Five of them -- the result and the four inputs -- are replaced in place
 *       by {@link RemovedGridSlot}, which is inactive and refuses both directions. Replacing rather
 *       than removing keeps every index constant, which matters: {@code quickMoveStack} names the
 *       armour and backpack ranges by literal, and the network protocol addresses slots by index.
 *   <li><b>The recipe never resolves.</b> {@code slotsChanged} is the only thing that fills the
 *       result container, so cancelling it means no crafting can happen even where something puts
 *       items into the grid without going through a slot -- the recipe book's
 *       {@code ServerPlaceRecipe} writes to the container directly, and a mod could too.
 * </ul>
 *
 * <p>The grid is also painted on the inventory texture, which no menu change reaches; the cover and
 * the recipe-book button are
 * {@link com.planetaryfactory.core.crafting.client.InventoryGridBlank}'s, client-side. Neither is
 * load-bearing -- this class is what makes the grid unable to craft.
 */
@Mixin(InventoryMenu.class)
public class InventoryMenuMixin {

    @Inject(method = "<init>", at = @At("RETURN"))
    private void planetaryfactory_core$removeCraftingGrid(
            Inventory playerInventory, boolean active, Player owner, CallbackInfo ci) {
        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
        // The result slot and then the four inputs, which vanilla adds in that order and which its
        // own constants describe as one run: RESULT_SLOT, then CRAFT_SLOT_START to CRAFT_SLOT_END.
        planetaryfactory_core$deaden(menu, InventoryMenu.RESULT_SLOT);
        for (int i = InventoryMenu.CRAFT_SLOT_START; i < InventoryMenu.CRAFT_SLOT_END; i++) {
            planetaryfactory_core$deaden(menu, i);
        }
    }

    @Unique
    private static void planetaryfactory_core$deaden(AbstractContainerMenu menu, int index) {
        Slot replaced = menu.slots.get(index);
        Slot dead = new RemovedGridSlot(
                replaced.container, replaced.getContainerSlot(), replaced.x, replaced.y);
        dead.index = replaced.index;
        menu.slots.set(index, dead);
    }

    @Inject(method = "slotsChanged", at = @At("HEAD"), cancellable = true)
    private void planetaryfactory_core$neverCraft(Container container, CallbackInfo ci) {
        ci.cancel();
    }
}
