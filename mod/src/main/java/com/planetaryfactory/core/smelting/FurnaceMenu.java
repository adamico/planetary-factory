package com.planetaryfactory.core.smelting;

import com.planetaryfactory.core.PFMenus;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

/**
 * One tier-aware menu for all three furnaces (#155), rather than one menu per tier.
 *
 * <p>The burner tiers show three slots; the Electric tier shows two and an energy bar where the
 * flame is. The fuel slot is not merely hidden on the Electric tier -- it is never added, so a
 * player cannot shift-click coal into a block that has no use for it.
 */
public class FurnaceMenu extends AbstractContainerMenu {

    private final Container container;
    private final ContainerData data;
    private final FurnaceTier tier;

    /** Client side: the block entity is not reachable, so the slots stand over a stub. */
    public FurnaceMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory, new SimpleContainer(FurnaceSlots.SIZE),
                new SimpleContainerData(FurnaceBlockEntity.DATA_COUNT), buf.readEnum(FurnaceTier.class));
    }

    public FurnaceMenu(int containerId, Inventory playerInventory, FurnaceBlockEntity furnace, ContainerData data) {
        this(containerId, playerInventory, furnace, data, furnace.tier());
    }

    private FurnaceMenu(int containerId, Inventory playerInventory, Container container, ContainerData data,
            FurnaceTier tier) {
        super(PFMenus.FURNACE.get(), containerId);
        this.container = container;
        this.data = data;
        this.tier = tier;
        checkContainerSize(container, FurnaceSlots.SIZE);
        checkContainerDataCount(data, FurnaceBlockEntity.DATA_COUNT);

        addSlot(new Slot(container, FurnaceSlots.INPUT, 56, 17));
        if (tier.burnsFuel()) {
            addSlot(new Slot(container, FurnaceSlots.FUEL, 56, 53));
        }
        addSlot(new OutputSlot(container, FurnaceSlots.OUTPUT, 116, 35));

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

    public FurnaceTier tier() {
        return tier;
    }

    /** How far along the arrow is, 0..1. */
    public float smeltProgress() {
        int duration = data.get(FurnaceBlockEntity.DATA_DURATION);
        return duration <= 0 ? 0F : Math.min(1F, data.get(FurnaceBlockEntity.DATA_PROGRESS) / (float) duration);
    }

    /** How much of the alight fuel item is left, 0..1. Always 0 on the Electric tier. */
    public float fuelLeft() {
        int litDuration = data.get(FurnaceBlockEntity.DATA_LIT_DURATION);
        return litDuration <= 0 ? 0F : Math.min(1F, data.get(FurnaceBlockEntity.DATA_LIT) / (float) litDuration);
    }

    /** How full the EU buffer is, 0..1. Always 0 on the burner tiers. */
    public float energyLevel() {
        int capacity = data.get(FurnaceBlockEntity.DATA_ENERGY_CAPACITY);
        return capacity <= 0 ? 0F : Math.min(1F, data.get(FurnaceBlockEntity.DATA_ENERGY) / (float) capacity);
    }

    /** The EU in the buffer. Always 0 on the burner tiers. */
    public int energyStored() {
        return data.get(FurnaceBlockEntity.DATA_ENERGY);
    }

    /** What the buffer holds when full. Always 0 on the burner tiers. */
    public int energyCapacity() {
        return data.get(FurnaceBlockEntity.DATA_ENERGY_CAPACITY);
    }

    /** What one tick of smelting costs, which is the number that turns a buffer into a duration. */
    public long euPerTick() {
        return tier.euPerTick();
    }

    /** The number of furnace slots this tier actually shows, which the shift-click split needs. */
    private int furnaceSlotCount() {
        return tier.burnsFuel() ? 3 : 2;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int machineSlots = furnaceSlotCount();

        if (index < machineSlots) {
            if (!moveItemStackTo(stack, machineSlots, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(stack, original);
        } else if (!moveItemStackTo(stack, 0, machineSlots - 1, false)) {
            // The output slot is never a destination, which is why the range stops one short.
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

    /** Nothing may be put into the output slot, by hand or by shift-click. */
    private static final class OutputSlot extends Slot {
        private OutputSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
