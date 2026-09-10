package com.planetaryfactory.core.fluid;

import javax.annotation.Nullable;

import com.planetaryfactory.core.PFBlockEntities;
import com.planetaryfactory.core.smelting.FuelBuffer;
import com.planetaryfactory.core.smelting.PFFuel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * The Boiler running (#224, ADR-0048): solid fuel and water in, low-temperature steam out.
 *
 * <p><b>The third customer of the burner model</b> the Furnace and the Burner Mining Drill already
 * share (ADR-0047). Nothing about fuel is re-derived here: {@link PFFuel} says what an item is
 * worth, {@link FuelBuffer} banks it whole and spends it a tick at a time, and the fuel table is
 * generated datapack JSON rather than Forge's burn table -- so what burns in a Boiler is exactly
 * what burns in a Stone Furnace, by construction.
 *
 * <p>The two things this class must get right, both of which are {@link BoilerCycle}'s to decide
 * and this class's to feed honestly:
 *
 * <ol>
 *   <li><b>The stall.</b> A full steam tank makes no steam, burns no fuel and voids none, and it
 *       resumes the moment a pipe drains it. #224 names this as the behaviour it is watching for:
 *       a boiler quietly eating coal into a full tank is a leak with no symptom at all.
 *   <li><b>Water is consumed, never created.</b> Under ADR-0050 every drop comes from an Offshore
 *       Pump. The input tank is fillable from outside and by nothing else.
 * </ol>
 *
 * <p>The rate is read, not chosen -- {@link SteamChainCorpus} into {@link BoilerSpec} -- and the
 * tanks' capacities are the prototype's own fluid boxes, at Factorio's 1:1 unit rule.
 */
public class BoilerBlockEntity extends BlockEntity implements Container, MenuProvider {

    public static final int DATA_FUEL = 0;
    public static final int DATA_FUEL_CAPACITY = 1;
    public static final int DATA_WATER = 2;
    public static final int DATA_STEAM = 3;
    public static final int DATA_COUNT = 4;

    /** Every figure below is the prototype's, resolved once at class-init. */
    private static final SteamChainCorpus CORPUS = SteamChainCorpus.get();

    public static final int WATER_CAPACITY = CORPUS.boilerFluidBoxVolume(BoilerSpec.INPUT);
    public static final int STEAM_CAPACITY = CORPUS.boilerFluidBoxVolume(BoilerSpec.OUTPUT);

    private static final long JOULES_PER_TICK =
            BoilerSpec.joulesPerTick(CORPUS.boilerEnergyConsumption());
    private static final long JOULES_PER_MILLIBUCKET = BoilerSpec.joulesPerMilliBucket(
            CORPUS.boilerTargetTemperature(),
            CORPUS.fluidDefaultTemperature("water"),
            // Steam's, not water's. BoilerSpec is where that trap is written down.
            CORPUS.fluidHeatCapacity("steam"));
    /** ADR-0047's multiplier on the way *in* to the buffer. Factorio's is 1. */
    private static final double EFFECTIVITY = CORPUS.boilerEffectivity();

    private static final int MILLIBUCKETS_PER_TICK =
            BoilerSpec.milliBucketsPerTick(JOULES_PER_TICK, JOULES_PER_MILLIBUCKET);

    private final NonNullList<ItemStack> items = NonNullList.withSize(BoilerSlots.SIZE, ItemStack.EMPTY);
    private final FuelBuffer fuel = new FuelBuffer();

    private final FluidTank water = new FluidTank(WATER_CAPACITY, stack -> stack.getFluid() == Fluids.WATER) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    private final FluidTank steam = new FluidTank(STEAM_CAPACITY) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_FUEL -> clampToInt(fuel.storedJoules());
                case DATA_FUEL_CAPACITY -> clampToInt(fuel.gaugeCapacity());
                case DATA_WATER -> water.getFluidAmount();
                case DATA_STEAM -> steam.getFluidAmount();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case DATA_FUEL -> fuel.load(value, fuel.lastLitJoules());
                case DATA_FUEL_CAPACITY -> fuel.load(fuel.storedJoules(), value);
                case DATA_WATER -> water.setFluid(new FluidStack(Fluids.WATER, value));
                case DATA_STEAM -> steam.setFluid(new FluidStack(PFFluids.STEAM_SOURCE.get(), value));
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    /** Saturating, for the reason the furnace's is: a wrapped int draws a full gauge as an empty
     * one, and nobody traces that back to a cast. */
    private static int clampToInt(long value) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, value));
    }

    public BoilerBlockEntity(BlockPos pos, BlockState state) {
        super(PFBlockEntities.BOILER.get(), pos, state);
    }

    public ContainerData data() {
        return data;
    }

    /** What a tick of boiling costs, for the screen's fuel tooltip. */
    public static long joulesPerTick() {
        return JOULES_PER_TICK;
    }

    // -- the operation --------------------------------------------------------------------------

    public void serverTick() {
        if (!(level instanceof ServerLevel)) {
            return;
        }
        int converted = BoilerCycle.tick(
                water.getFluidAmount(),
                steam.getCapacity() - steam.getFluidAmount(),
                MILLIBUCKETS_PER_TICK,
                JOULES_PER_TICK,
                fuel,
                this::light);
        if (converted <= 0) {
            return;
        }
        water.drain(converted, IFluidHandler.FluidAction.EXECUTE);
        // Unit for unit: Factorio's boiler is a temperature change, not a reaction.
        steam.fill(new FluidStack(PFFluids.STEAM_SOURCE.get(), converted),
                IFluidHandler.FluidAction.EXECUTE);
        setChanged();
    }

    /**
     * Consume one fuel item whole and report what it was worth, the rig's own idiom.
     *
     * <p>ADR-0047's rule in full: {@code fuel_value * effectivity} joules into the buffer. The
     * Boiler's effectivity is 1, so the multiplication changes nothing today -- it is here so that
     * the rule is executed rather than assumed, and a prototype that ever stated otherwise would
     * be obeyed instead of silently ignored.
     */
    private long light() {
        ItemStack stack = items.get(BoilerSlots.FUEL);
        long joules = PFFuel.joules(stack);
        if (joules <= 0L) {
            return 0L;
        }
        stack.shrink(1);
        return Math.round(joules * EFFECTIVITY);
    }

    /** Whether the generated fuel table names this stack (ADR-0047). Default-deny. */
    public boolean isFuel(ItemStack stack) {
        return PFFuel.joules(stack) > 0L;
    }

    // -- the faces ------------------------------------------------------------------------------

    /**
     * Water in, steam out, on every side.
     *
     * <p>Two tanks behind one handler, and the direction never decides which: the <em>fluid</em>
     * does. A pipe pushing water reaches tank 0 and nothing else; a pipe pulling reaches the steam
     * and can never drain the water back out, which would otherwise let a player launder water
     * through a machine that is supposed to be consuming it.
     */
    public IFluidHandler fluidHandler() {
        return new IFluidHandler() {
            @Override
            public int getTanks() {
                return 2;
            }

            @Override
            public FluidStack getFluidInTank(int tank) {
                return tank == 0 ? water.getFluid() : steam.getFluid();
            }

            @Override
            public int getTankCapacity(int tank) {
                return tank == 0 ? water.getCapacity() : steam.getCapacity();
            }

            @Override
            public boolean isFluidValid(int tank, FluidStack stack) {
                return tank == 0 && water.isFluidValid(stack);
            }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
                return water.isFluidValid(resource) ? water.fill(resource, action) : 0;
            }

            @Override
            public FluidStack drain(FluidStack resource, FluidAction action) {
                return steam.drain(resource, action);
            }

            @Override
            public FluidStack drain(int maxDrain, FluidAction action) {
                return steam.drain(maxDrain, action);
            }
        };
    }

    // -- the container --------------------------------------------------------------------------

    @Override
    public int getContainerSize() {
        return BoilerSlots.SIZE;
    }

    @Override
    public boolean isEmpty() {
        return items.get(BoilerSlots.FUEL).isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot == BoilerSlots.FUEL ? items.get(BoilerSlots.FUEL) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) {
            setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        stack.limitSize(getMaxStackSize());
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == BoilerSlots.insertionSlot(isFuel(stack));
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    @Nullable
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new BoilerMenu(containerId, playerInventory, this, data);
    }

    // -- persistence ----------------------------------------------------------------------------

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.clear();
        ContainerHelper.loadAllItems(tag, items, registries);
        fuel.load(tag.getLong("FuelJoules"), tag.getLong("FuelLitJoules"));
        // Both tanks persist. A Boiler that came back empty over a logout would have destroyed
        // water an Offshore Pump had to lift, and steam a whole fuel item paid for.
        water.readFromNBT(registries, tag.getCompound("Water"));
        steam.readFromNBT(registries, tag.getCompound("Steam"));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putLong("FuelJoules", fuel.storedJoules());
        tag.putLong("FuelLitJoules", fuel.lastLitJoules());
        tag.put("Water", water.writeToNBT(registries, new CompoundTag()));
        tag.put("Steam", steam.writeToNBT(registries, new CompoundTag()));
    }
}
