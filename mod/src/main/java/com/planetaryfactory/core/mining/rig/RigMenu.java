package com.planetaryfactory.core.mining.rig;

import com.planetaryfactory.core.PFMenus;

import net.minecraft.network.RegistryFriendlyByteBuf;
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
 * One tier-aware menu for both rigs (#193, #194).
 *
 * <p>ADR-0043: "plain right-click opens the rig. It has a screen because it has a fuel slot." The
 * electric rig has no fuel slot and the slot is therefore never added rather than merely hidden,
 * so a player cannot shift-click coal into a block with no use for it -- the same rule the furnace
 * ladder keeps for its Electric tier.
 *
 * <p>A no-GUI rig, fuelled by right-clicking with coal in hand and read through Jade, was
 * considered by ADR-0043 and rejected: it would make this the only machine in the pack with no
 * screen.
 */
public class RigMenu extends AbstractContainerMenu {

    private final Container container;
    private final ContainerData data;
    private final RigTier tier;
    private final boolean burnsFuel;

    /** Client side: the block entity is not reachable, so the slots stand over a stub. */
    public RigMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory, new SimpleContainer(RigSlots.SIZE),
                new SimpleContainerData(RigBlockEntity.DATA_COUNT), buf.readEnum(RigTier.class));
    }

    public RigMenu(int containerId, Inventory playerInventory, RigBlockEntity rig, ContainerData data) {
        this(containerId, playerInventory, rig, data, rig.tier());
    }

    private RigMenu(int containerId, Inventory playerInventory, Container container, ContainerData data,
            RigTier tier) {
        super(PFMenus.RIG.get(), containerId);
        this.container = container;
        this.data = data;
        this.tier = tier;
        this.burnsFuel = RigCorpus.get().rowOf(tier).burnsFuel();
        checkContainerSize(container, RigSlots.SIZE);
        checkContainerDataCount(data, RigBlockEntity.DATA_COUNT);

        if (burnsFuel) {
            addSlot(new Slot(container, RigSlots.FUEL, 56, 53));
        }
        addSlot(new OutputSlot(container, RigSlots.OUTPUT, 116, 35));

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

    public RigTier tier() {
        return tier;
    }

    public boolean burnsFuel() {
        return burnsFuel;
    }

    /** How far along the current operation is, 0..1. */
    public float miningProgress() {
        int duration = data.get(RigBlockEntity.DATA_DURATION);
        return duration <= 0 ? 0F : Math.min(1F, data.get(RigBlockEntity.DATA_PROGRESS) / (float) duration);
    }

    /** How full the fuel buffer is, 0..1 -- the gauge fills to the last item the rig lit. */
    public float fuelLevel() {
        int capacity = data.get(RigBlockEntity.DATA_FUEL_CAPACITY);
        return capacity <= 0 ? 0F : Math.min(1F, data.get(RigBlockEntity.DATA_FUEL) / (float) capacity);
    }

    public int fuelStored() {
        return data.get(RigBlockEntity.DATA_FUEL);
    }

    public int fuelCapacity() {
        return data.get(RigBlockEntity.DATA_FUEL_CAPACITY);
    }

    /** What a tick of work costs this rig, in joules -- 150 kW over twenty ticks on the burner. */
    public long joulesPerTick() {
        return RigRate.joulesPerTick(RigCorpus.get().rowOf(tier).energyUsage());
    }

    /** How many machine slots this rig's screen shows, which is where shift-click's boundary is. */
    private int rigSlotCount() {
        return burnsFuel ? 2 : 1;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int machineSlots = rigSlotCount();

        if (index < machineSlots) {
            if (!moveItemStackTo(stack, machineSlots, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, machineSlots, false)) {
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

    /** The output is filled by the rig and emptied by the world; nothing is ever put back in. */
    private static class OutputSlot extends Slot {
        OutputSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
