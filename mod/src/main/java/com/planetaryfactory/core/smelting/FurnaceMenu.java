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

    /**
     * How full the buffer is, 0..1 -- joules on the burners, EU on the Electric tier.
     *
     * <p>One question for all three tiers since ADR-0047: they hold the same kind of thing and
     * the screen draws it with the same gauge. What the gauge fills to differs -- a burner's is
     * the last item it lit, the Electric tier's is the buffer ADR-0036's pole tops up -- and that
     * is the refill economy, not the quantity.
     */
    public float energyLevel() {
        int capacity = data.get(FurnaceBlockEntity.DATA_ENERGY_CAPACITY);
        return capacity <= 0 ? 0F : Math.min(1F, data.get(FurnaceBlockEntity.DATA_ENERGY) / (float) capacity);
    }

    /** What is in the buffer: joules on the burners, EU on the Electric tier. */
    public int energyStored() {
        return data.get(FurnaceBlockEntity.DATA_ENERGY);
    }

    /** What the gauge fills to: the last fuel item's joules, or the Electric tier's capacity. */
    public int energyCapacity() {
        return data.get(FurnaceBlockEntity.DATA_ENERGY_CAPACITY);
    }

    /** What one tick of smelting costs, which is the number that turns a buffer into a duration. */
    public long euPerTick() {
        return tier.euPerTick();
    }

    /** The same number for a burner: 4,500 J, both tiers, from the machine's own 90 kW. */
    public long joulesPerTick() {
        return tier.joulesPerTick();
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
