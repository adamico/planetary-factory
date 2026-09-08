package com.planetaryfactory.core.smelting;

import java.util.Optional;

import javax.annotation.Nullable;

import com.gregtechceu.gtceu.api.capability.IEnergyContainer;
import com.planetaryfactory.core.PFBlockEntities;
import com.planetaryfactory.core.recipes.PFRecipes;
import com.planetaryfactory.core.recipes.SmeltingRecipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The behaviour behind all three furnace tiers (#155).
 *
 * <p>One block entity type and three blocks pointing at it, the way the four supply-area poles
 * share one. Everything that differs between tiers is on {@link FurnaceTier}: speed, whether there
 * is a fuel slot, and how much EU a tick costs.
 *
 * <p>The arithmetic and the routing rules live in {@link FurnaceTier}, {@link FurnaceSlots},
 * {@link FurnaceCycle} and {@link FurnaceEnergyBuffer}, none of which touch Minecraft -- this
 * class is the shell that gives them a world. That split is the pack's testing policy rather than
 * taste: the mod's test source set has no Minecraft on its classpath.
 */
public class FurnaceBlockEntity extends BlockEntity implements Container, MenuProvider {

    public static final int DATA_PROGRESS = 0;
    public static final int DATA_DURATION = 1;
    // Both burner tiers and the Electric one report through DATA_ENERGY: since ADR-0047 they
    // hold the same kind of thing -- a buffer, in joules or in EU -- and the screen draws it with
    // the same gauge. Two widgets for one quantity would say a burner and an Electric furnace
    // hold interchangeable stuff, which is a worse lie than the flame it replaces.
    public static final int DATA_ENERGY = 2;
    public static final int DATA_ENERGY_CAPACITY = 3;
    public static final int DATA_COUNT = 4;

    private final NonNullList<ItemStack> items = NonNullList.withSize(FurnaceSlots.SIZE, ItemStack.EMPTY);
    private final FurnaceCycle cycle = new FurnaceCycle();
    private final FurnaceEnergyBuffer energy;
    private final FuelBuffer fuel = new FuelBuffer();
    private final FurnaceTier tier;

    /** The duration of the smelt in progress, so the client's arrow has something to scale to. */
    private int duration;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_PROGRESS -> cycle.progress();
                case DATA_DURATION -> duration;
                case DATA_ENERGY -> clampToInt(tier.burnsFuel()
                        ? fuel.storedJoules()
                        : energy.getEnergyStored());
                case DATA_ENERGY_CAPACITY -> clampToInt(tier.burnsFuel()
                        ? fuel.gaugeCapacity()
                        : energy.getEnergyCapacity());
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case DATA_PROGRESS -> cycle.setProgress(value);
                case DATA_DURATION -> duration = value;
                case DATA_ENERGY -> {
                    if (tier.burnsFuel()) {
                        fuel.load(value, fuel.lastLitJoules());
                    } else {
                        energy.setStoredEu(value);
                    }
                }
                case DATA_ENERGY_CAPACITY -> {
                    if (tier.burnsFuel()) {
                        fuel.load(fuel.storedJoules(), value);
                    }
                }
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    /**
     * Vanilla's {@link ContainerData} is a channel of ints and the buffer is joules in a long.
     * Nothing in the table today reaches 2 GJ -- rocket fuel, the largest, is 100 MJ -- but
     * {@code nuclear-fuel} is 1.21 GJ and #135 is the ticket that lands it. Saturating is the only
     * failure a gauge can survive: a wrapped int draws a full bar as an empty one, and a hover
     * that reads negative is the sort of thing nobody traces back to a cast.
     */
    private static int clampToInt(long value) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, value));
    }

    public FurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(PFBlockEntities.FURNACE.get(), pos, state);
        if (!(state.getBlock() instanceof FurnaceBlock furnace)) {
            // Silently defaulting would ship a mis-tiered furnace: an Electric one with a fuel
            // slot and no EU draw, working just well enough that nobody looks at the block.
            throw new IllegalStateException(
                    "furnace block entity on " + state.getBlock() + " at " + pos
                            + ", which is not a furnace");
        }
        this.tier = furnace.tier();
        this.energy = new FurnaceEnergyBuffer(tier.bufferEu());
    }

    public FurnaceTier tier() {
        return tier;
    }

    public ContainerData data() {
        return data;
    }

    // -- the smelt ------------------------------------------------------------------------------

    public void serverTick() {
        if (level == null || level.isClientSide()) {
            return;
        }

        Optional<RecipeHolder<SmeltingRecipe>> found = recipe();
        boolean canSmelt = found.isPresent() && fitsOutput(found.get().value());
        if (!canSmelt) {
            // Held when the recipe is still there and the output is merely full; started over when
            // the input has gone, since progress on a smelt nobody asked for is a free head start.
            //
            // NOTHING BURNS DOWN HERE. A furnace waiting on a full output spends no joules and
            // no EU -- the lit coal is already spent, but what it banked is not, and a stall that
            // quietly drained the buffer would make backpressure cost the player the fuel it was
            // meant to save. That is the same rule the output side keeps under ADR-0041.
            if (cycle.idle(found.isPresent())) {
                setChanged();
            }
            setLit(isLit());
            return;
        }

        SmeltingRecipe smelt = found.get().value();
        duration = tier.durationTicks(smelt.cookingTime());

        boolean powered = pay();
        if (cycle.tick(powered, duration)) {
            complete(smelt);
        }
        // Whether this tick was paid for, not whether the next one can be: reading the buffer
        // here would go dark on the tick the last of a coal is spent, mid-smelt.
        setLit(powered);
        setChanged();
    }

    /**
     * Pays for one tick: 4,500 J from the fuel buffer on the two burner tiers, 13 EU on the
     * Electric one.
     *
     * <p>Both burners draw the same 90 kW, which is Factorio's own arrangement and the reason the
     * Steel tier's doubled speed yields exactly twice the items from one coal (ADR-0047). The
     * buffer is asked first and an item is lit only when it cannot cover the tick, so an item is
     * never consumed to top up a buffer that was already going to pay.
     */
    private boolean pay() {
        if (!tier.burnsFuel()) {
            return energy.drawTick(tier.euPerTick());
        }
        long perTick = tier.joulesPerTick();
        if (fuel.drawTick(perTick)) {
            return true;
        }
        return light() && fuel.drawTick(perTick);
    }

    /**
     * Consumes one fuel item whole and banks its joules.
     *
     * <p>Vanilla's leaves-a-bucket rule is gone with the vanilla table: under a default-deny table
     * no bucket is fuel, and keeping the branch would say a fuel-with-remainder was a case someone
     * had considered.
     */
    private boolean light() {
        ItemStack stack = items.get(FurnaceSlots.FUEL);
        long joules = PFFuel.joules(stack);
        if (joules <= 0L) {
            return false;
        }
        fuel.light(joules);
        stack.shrink(1);
        return true;
    }

    private void complete(SmeltingRecipe smelt) {
        ItemStack result = smelt.assemble(new SingleRecipeInput(items.get(FurnaceSlots.INPUT)),
                level.registryAccess());
        ItemStack output = items.get(FurnaceSlots.OUTPUT);
        if (output.isEmpty()) {
            items.set(FurnaceSlots.OUTPUT, result.copy());
        } else {
            output.grow(result.getCount());
        }
        items.get(FurnaceSlots.INPUT).shrink(smelt.count());
    }

    /** The stall rule: no room for the result means the smelt does not start at all. */
    private boolean fitsOutput(SmeltingRecipe smelt) {
        ItemStack result = smelt.result();
        ItemStack output = items.get(FurnaceSlots.OUTPUT);
        return FurnaceSlots.fitsOutput(
                output.getCount(),
                ItemStack.isSameItemSameComponents(output, result),
                result.getCount(),
                Math.min(getMaxStackSize(), output.isEmpty() ? result.getMaxStackSize() : output.getMaxStackSize()));
    }

    private Optional<RecipeHolder<SmeltingRecipe>> recipe() {
        ItemStack input = items.get(FurnaceSlots.INPUT);
        if (input.isEmpty() || level == null) {
            return Optional.empty();
        }
        return level.getRecipeManager().getRecipeFor(PFRecipes.SMELTING_TYPE.get(),
                new SingleRecipeInput(input), level);
    }

    private void setLit(boolean lit) {
        if (level == null || getBlockState().getValue(FurnaceBlock.LIT) == lit) {
            return;
        }
        level.setBlock(getBlockPos(), getBlockState().setValue(FurnaceBlock.LIT, lit), Block.UPDATE_ALL);
    }

    /**
     * Whether any loaded pack smelt would take this stack, regardless of how many are held.
     *
     * <p>Deliberately not {@code matches}: a hopper handing over one iron plate at a time has to
     * reach the input slot, or the 5:1 steel smelt could never be fed at all.
     */
    public boolean isSmeltingIngredient(ItemStack stack) {
        if (level == null || stack.isEmpty()) {
            return false;
        }
        return level.getRecipeManager().getAllRecipesFor(PFRecipes.SMELTING_TYPE.get()).stream()
                .anyMatch(holder -> holder.value().isIngredient(stack));
    }

    /**
     * Whether the generated fuel table names this stack (ADR-0047). Default-deny: an item with no
     * row is not fuel, and {@link FurnaceSlots#insertionSlot} therefore will not route it to the
     * fuel slot at all.
     */
    public boolean isFuel(ItemStack stack) {
        return PFFuel.joules(stack) > 0L;
    }

    /**
     * Whether the block's front is alight while it is not working: a burner with joules still in
     * its buffer. The Electric tier is never lit this way -- a stocked buffer is not a fire, and
     * ADR-0036's pole keeps it topped up, so a lit front there would never go out.
     */
    private boolean isLit() {
        return tier.burnsFuel() && fuel.isLit();
    }

    // -- the energy face ------------------------------------------------------------------------

    /**
     * GregTech's container face, and the whole of what ADR-0036's pole talks to.
     *
     * <p>Null on the two burner tiers, so a pole does not count a Stone Furnace as a machine it is
     * failing to power.
     */
    @Nullable
    public IEnergyContainer energySide() {
        return tier.burnsFuel() ? null : gtContainer;
    }

    private final IEnergyContainer gtContainer = new IEnergyContainer() {
        @Override
        public long acceptEnergyFromNetwork(Direction side, long voltage, long amperage) {
            // voltage and amperage are GregTech's signature, not ours; nothing here reads them.
            return energy.acceptFromNetwork();
        }

        @Override
        public boolean inputsEnergy(Direction side) {
            return energy.inputsEnergy();
        }

        @Override
        public long changeEnergy(long delta) {
            long moved = energy.changeEnergy(delta);
            if (moved != 0L) {
                setChanged();
            }
            return moved;
        }

        @Override
        public long getEnergyStored() {
            return energy.getEnergyStored();
        }

        @Override
        public long getEnergyCapacity() {
            return energy.getEnergyCapacity();
        }

        @Override
        public long getInputAmperage() {
            return energy.inputAmperage();
        }

        @Override
        public long getInputVoltage() {
            return energy.inputVoltage();
        }
    };

    // -- the container --------------------------------------------------------------------------

    @Override
    public int getContainerSize() {
        return FurnaceSlots.SIZE;
    }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
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
        return slot == FurnaceSlots.insertionSlot(isSmeltingIngredient(stack), isFuel(stack), tier.burnsFuel());
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
        return new FurnaceMenu(containerId, playerInventory, this, data);
    }

    // -- persistence ----------------------------------------------------------------------------

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.clear();
        ContainerHelper.loadAllItems(tag, items, registries);
        cycle.setProgress(tag.getInt("Progress"));
        duration = tag.getInt("Duration");
        fuel.load(tag.getLong("FuelJoules"), tag.getLong("FuelLitJoules"));
        energy.setStoredEu(tag.getLong("Energy"));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putInt("Progress", cycle.progress());
        tag.putInt("Duration", duration);
        tag.putLong("FuelJoules", fuel.storedJoules());
        tag.putLong("FuelLitJoules", fuel.lastLitJoules());
        tag.putLong("Energy", energy.getEnergyStored());
    }
}
