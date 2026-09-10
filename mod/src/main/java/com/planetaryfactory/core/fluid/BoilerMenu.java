package com.planetaryfactory.core.fluid;

import com.planetaryfactory.core.PFMenus;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The Boiler's menu (#224): one fuel slot and three gauges.
 *
 * <p>It has a screen for the reason the rig has one -- it has a fuel slot, and a machine a player
 * cannot hand-feed at rung 0 is a machine they cannot start. What it adds over the rig's is the
 * pair of tank readings, because a Boiler that has stopped is either out of fuel, out of water or
 * backed up, and those three look identical from outside the block.
 *
 * <p>No tier travels in the opening packet: there is one Boiler (ADR-0048).
 */
public class BoilerMenu extends AbstractContainerMenu {

    private final Container container;
    private final ContainerData data;

    /** Client side: the block entity is not reachable, so the slot stands over a stub. */
    public BoilerMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(BoilerSlots.SIZE),
                new SimpleContainerData(BoilerBlockEntity.DATA_COUNT));
    }

    public BoilerMenu(int containerId, Inventory playerInventory, Container container,
            ContainerData data) {
        super(PFMenus.BOILER.get(), containerId);
        this.container = container;
        this.data = data;
        checkContainerSize(container, BoilerSlots.SIZE);
        checkContainerDataCount(data, BoilerBlockEntity.DATA_COUNT);

        addSlot(new Slot(container, BoilerSlots.FUEL, 56, 53));

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, 142));
        }
        addDataSlots(data);
    }

    /** How full the fuel buffer is, 0..1 -- the gauge fills to the last item the Boiler lit. */
    public float fuelLevel() {
        int capacity = data.get(BoilerBlockEntity.DATA_FUEL_CAPACITY);
        return capacity <= 0
                ? 0F : Math.min(1F, data.get(BoilerBlockEntity.DATA_FUEL) / (float) capacity);
    }

    public int fuelStored() {
        return data.get(BoilerBlockEntity.DATA_FUEL);
    }

    public int fuelCapacity() {
        return data.get(BoilerBlockEntity.DATA_FUEL_CAPACITY);
    }

    public int water() {
        return data.get(BoilerBlockEntity.DATA_WATER);
    }

    public int steam() {
        return data.get(BoilerBlockEntity.DATA_STEAM);
    }

    public int waterCapacity() {
        return BoilerBlockEntity.WATER_CAPACITY;
    }

    public int steamCapacity() {
        return BoilerBlockEntity.STEAM_CAPACITY;
    }

    /** What a tick of boiling costs, in joules -- 90,000 off Factorio's 1.8 MW. */
    public long joulesPerTick() {
        return BoilerBlockEntity.joulesPerTick();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        if (index < BoilerSlots.SIZE) {
            if (!moveItemStackTo(stack, BoilerSlots.SIZE, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, BoilerSlots.SIZE, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }
}
