package com.planetaryfactory.core.mining.rig;

import com.planetaryfactory.core.PFBlockEntities;
import com.planetaryfactory.core.mining.rig.RigGeometry.Offset;
import com.planetaryfactory.core.ore.OreBlock;
import com.planetaryfactory.core.ore.OreCorpus;
import com.planetaryfactory.core.ore.OreDelta;
import com.planetaryfactory.core.ore.OreMining;
import com.planetaryfactory.core.smelting.FuelBuffer;
import com.planetaryfactory.core.smelting.FurnaceCycle;
import com.planetaryfactory.core.smelting.PFFuel;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/**
 * The behaviour behind both mining rigs (#193, #194): it mines the layer beneath it, burns solid
 * fuel for the privilege, and pushes what it mines onto the tile it faces.
 *
 * <p>One block entity type and two blocks pointing at it, the way the three furnace tiers share
 * one. Everything that differs between the rigs is a row in {@link RigCorpus} rather than a branch
 * here -- footprint, speed, wattage, whether there is a fuel slot at all -- which is what lets
 * Vulcanus's Big Mining Drill arrive later as a data row.
 *
 * <p>The arithmetic and the rules live in {@link RigRate}, {@link RigArea}, {@link RigOutputTile},
 * {@link RigBuffer}, {@link RigSlots} and the smelting package's {@link FuelBuffer} and
 * {@link FurnaceCycle}, none of which touch Minecraft. This class is the shell that gives them a
 * world, and that split is the pack's testing policy rather than taste: the mod's test source set
 * has no Minecraft on its classpath.
 *
 * <h2>The three things this class is responsible for getting right</h2>
 *
 * <ol>
 *   <li><b>An operation draws one unit, through {@link OreMining}.</b> Not around it. This is the
 *       specific thing {@code gtceu:lv_miner} gets wrong -- it deletes an ore block whole whatever
 *       amount it held, and takes its drops from the loot table. Terra's ore loot tables are
 *       empty, so that miner would destroy a field and pay out nothing.
 *   <li><b>The buffer is asked before the ground is.</b> A draw that could not be banked would
 *       take a unit out of a finite resource and lose it, which is the one outcome ADR-0041's
 *       amount model cannot survive.
 *   <li><b>A stalled rig burns nothing.</b> No ore under it, or an output it cannot place, and no
 *       joules move. The extracted prototype declares no {@code drain} on a burner source, so
 *       there is not even an idle trickle to argue about -- a rig quietly burning coal while
 *       blocked would be a leak the player cannot see.
 * </ol>
 */
public class RigBlockEntity extends BlockEntity implements Container, MenuProvider {

    public static final int DATA_PROGRESS = 0;
    public static final int DATA_DURATION = 1;
    public static final int DATA_FUEL = 2;
    public static final int DATA_FUEL_CAPACITY = 3;
    public static final int DATA_COUNT = 4;

    /** One stack of ore. ADR-0043 asks for "a small internal buffer" and does not size it. */
    private static final int BUFFER_CAPACITY = 64;

    private final RigTier tier;
    private final RigCorpus.Row row;

    /** Only the fuel slot is a real stack; the output is a view over {@link #buffer}. */
    private final NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);

    private final RigBuffer buffer = new RigBuffer(BUFFER_CAPACITY);
    private final FuelBuffer fuel = new FuelBuffer();
    private final FurnaceCycle cycle = new FurnaceCycle();

    /** The duration of the operation in progress, so the client's gauge has something to scale to. */
    private int duration;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_PROGRESS -> cycle.progress();
                case DATA_DURATION -> duration;
                case DATA_FUEL -> clampToInt(fuel.storedJoules());
                case DATA_FUEL_CAPACITY -> clampToInt(fuel.gaugeCapacity());
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case DATA_PROGRESS -> cycle.setProgress(value);
                case DATA_DURATION -> duration = value;
                case DATA_FUEL -> fuel.load(value, fuel.lastLitJoules());
                case DATA_FUEL_CAPACITY -> fuel.load(fuel.storedJoules(), value);
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    /** Saturating, for the reason {@code FurnaceBlockEntity} gives: a wrapped int draws a full
     * gauge as an empty one, and nobody traces that back to a cast. */
    private static int clampToInt(long value) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, value));
    }

    public RigBlockEntity(BlockPos pos, BlockState state) {
        super(PFBlockEntities.RIG.get(), pos, state);
        if (!(state.getBlock() instanceof RigBlock rig)) {
            // Silently defaulting would ship a mis-tiered rig: a burner one with no fuel slot and
            // no draw, working just well enough that nobody looks at the block.
            throw new IllegalStateException(
                    "rig block entity on " + state.getBlock() + " at " + pos + ", which is not a rig");
        }
        this.tier = rig.tier();
        this.row = RigCorpus.get().rowOf(tier);
    }

    public RigTier tier() {
        return tier;
    }

    public ContainerData data() {
        return data;
    }

    public boolean burnsFuel() {
        return row.burnsFuel();
    }

    // -- the operation --------------------------------------------------------------------------

    public void serverTick() {
        if (!(level instanceof ServerLevel server)) {
            return;
        }

        Target target = nextTarget(server);
        boolean hasOre = target != null;
        boolean canWork = hasOre && buffer.canAccept(target.dropId());
        if (!canWork) {
            // Progress is held while there is still ore and the output is merely full; started
            // over when the ore has gone, since progress towards a block that no longer exists is
            // a free head start on the next one.
            //
            // NOTHING BURNS DOWN HERE, which is ADR-0043's rule verbatim: "a rig that is stalled,
            // unpowered or standing on nothing consumes nothing". The push still runs, because a
            // stall clears the moment the faced tile takes what is banked.
            if (cycle.idle(hasOre)) {
                setChanged();
            }
            push(server);
            return;
        }

        duration = RigRate.operationTicks(row.miningSpeed(), target.miningTime());
        boolean powered = pay();
        if (cycle.tick(powered, duration)) {
            complete(server, target);
        }
        push(server);
        setChanged();
    }

    /**
     * The first ore-bearing tile in the rig's area, or {@code null}.
     *
     * <p>{@link RigArea} fixes the order, so the rig works one block until that block is gone
     * rather than shuffling across the patch and leaving a field of part-mined blocks.
     */
    @Nullable
    private Target nextTarget(ServerLevel server) {
        Direction facing = getBlockState().getValue(RigBlock.FACING);
        for (Offset offset : RigArea.tiles(
                row.width(), row.height(), row.searchingRadius(), RigDirections.toRigFacing(facing))) {
            BlockPos pos = getBlockPos().offset(offset.dx(), offset.dy(), offset.dz());
            if (!(server.getBlockState(pos).getBlock() instanceof OreBlock ore)) {
                continue;
            }
            if (OreMining.remaining(server, pos, ore) <= 0) {
                continue;
            }
            OreCorpus.Resource resource = ore.resource().corpus();
            return new Target(pos, ore, ore.resource().drop(), resource.miningTime());
        }
        return null;
    }

    /**
     * Pay for one tick, at the rig's own wattage.
     *
     * <p>Deliberately not {@code PFFuel.joulesPerTick()}: that is the furnace's 4,500 J/t off its
     * 90 kW, and this rig draws 150. The buffer is asked first and an item is lit only when it
     * cannot cover the tick, so an item is never consumed to top up a buffer that would already
     * have paid (ADR-0047).
     */
    private boolean pay() {
        if (!row.burnsFuel()) {
            // #194: the electric rig is a supply-area pole customer under ADR-0036, and until that
            // ticket lands it has no power source at all. It mines nothing rather than mining free.
            return false;
        }
        long perTick = RigRate.joulesPerTick(row.energyUsage());
        if (fuel.drawTick(perTick)) {
            return true;
        }
        return light() && fuel.drawTick(perTick);
    }

    /** Consume one fuel item whole and bank its joules. */
    private boolean light() {
        ItemStack stack = items.get(RigSlots.FUEL);
        long joules = PFFuel.joules(stack);
        if (joules <= 0L) {
            return false;
        }
        fuel.light(joules);
        stack.shrink(1);
        return true;
    }

    /** One operation: one unit out of the ground, through {@link OreMining} and not around it. */
    private void complete(ServerLevel server, Target target) {
        OreDelta.Draw draw = OreMining.draw(server, target.ore(), target.pos());
        if (draw.paid() > 0) {
            // ADR-0043's yield-per-operation, fixed at 1.0 and dormant on Terra: one unit drawn is
            // one item banked. Vulcanus's Big Mining Drill is what varies it, and when it does the
            // multiplier goes here rather than into a mechanism that welded the two together.
            buffer.add(target.dropId());
        }
    }

    /**
     * Hand what is banked to the tile the rig faces, and hold it if that tile will not take it.
     *
     * <p>ADR-0043's eject, as amended by this ticket: <b>an item handler on the faced tile, and
     * otherwise a stall.</b> There is no ground drop. No Factorio machine spills when its output
     * is blocked -- a blocked drill fills its output and halts, and that backpressure is the
     * logistics puzzle -- and under an amount model a spill risks losing a finite resource where a
     * stall preserves it.
     *
     * <p>Rule 1 covers the pack's furnace, a chest, a vanilla hopper and a bare Create belt, all of
     * which answer {@code Capabilities.ItemHandler.BLOCK}. A Create funnel answers no handler and
     * is reached the other way round -- set to extract, sitting on the rig, pulling through
     * {@link RigItemHandler} -- which is why the mod takes no Create dependency and calls no
     * {@code DirectBeltInputBehaviour} (#182, ADR-0044).
     */
    private void push(ServerLevel server) {
        String itemId = buffer.itemId();
        if (itemId == null || buffer.count() <= 0) {
            return;
        }
        Direction facing = getBlockState().getValue(RigBlock.FACING);
        Offset offset = RigOutputTile.of(
                row.width(), row.height(), row.vectorX(), row.vectorY(),
                RigDirections.toRigFacing(facing));
        BlockPos target = getBlockPos().offset(offset.dx(), offset.dy(), offset.dz());

        IItemHandler handler = server.getCapability(
                // The side the item arrives from, which is the neighbour's face towards the rig.
                Capabilities.ItemHandler.BLOCK, target, facing.getOpposite());
        if (handler == null) {
            return;
        }

        int held = buffer.count();
        ItemStack leftover = ItemHandlerHelper.insertItem(handler, stackOf(itemId, held), false);
        if (leftover.getCount() == held) {
            return;
        }
        // `load` with a count of zero empties the buffer identity and all, which is what lets the
        // next resource in when a 2x2 straddles two fields.
        buffer.load(itemId, leftover.getCount());
        setChanged();
    }

    private static ItemStack stackOf(String itemId, int count) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId));
        return count <= 0 ? ItemStack.EMPTY : new ItemStack(item, count);
    }

    /**
     * Whether the generated fuel table names this stack (ADR-0047). Default-deny, and category
     * filtered: the table only ever answers for {@code chemical}, which is exactly what this rig's
     * corpus row admits.
     */
    public boolean isFuel(ItemStack stack) {
        return PFFuel.joules(stack) > 0L;
    }

    /** What the rig is holding, for the overlay and for anything else that asks (#195). */
    public int bufferedCount() {
        return buffer.count();
    }

    private record Target(BlockPos pos, OreBlock ore, String dropId, double miningTime) {
    }

    // -- the container --------------------------------------------------------------------------

    @Override
    public int getContainerSize() {
        return RigSlots.SIZE;
    }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty) && buffer.count() == 0;
    }

    @Override
    public ItemStack getItem(int slot) {
        if (slot == RigSlots.OUTPUT) {
            // The output is not stored as a stack: the buffer is the truth, and this is its view.
            // Two copies of one quantity is how a machine ends up voiding what it mined.
            String itemId = buffer.itemId();
            return itemId == null ? ItemStack.EMPTY : stackOf(itemId, buffer.count());
        }
        return slot == RigSlots.FUEL ? items.get(RigSlots.FUEL) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot == RigSlots.OUTPUT) {
            String itemId = buffer.itemId();
            int taken = buffer.remove(amount);
            if (taken <= 0) {
                return ItemStack.EMPTY;
            }
            setChanged();
            return stackOf(itemId, taken);
        }
        ItemStack removed = ContainerHelper.removeItem(items, RigSlots.FUEL, amount);
        if (!removed.isEmpty()) {
            setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return removeItem(slot, slot == RigSlots.OUTPUT ? buffer.count() : getMaxStackSize());
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot == RigSlots.OUTPUT) {
            buffer.load(stack.isEmpty() ? null : idOf(stack), stack.getCount());
        } else if (slot == RigSlots.FUEL) {
            items.set(RigSlots.FUEL, stack);
            stack.limitSize(getMaxStackSize());
        }
        setChanged();
    }

    private static String idOf(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == RigSlots.insertionSlot(isFuel(stack), burnsFuel());
    }

    @Override
    public void clearContent() {
        items.clear();
        buffer.load(null, 0);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    @Nullable
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new RigMenu(containerId, playerInventory, this, data);
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
        // A rig that logged out mid-stall comes back stalled and still holding it. A buffer that
        // came back empty would have voided ore across a logout with nothing in the log.
        buffer.load(tag.contains("BufferItem") ? tag.getString("BufferItem") : null,
                tag.getInt("BufferCount"));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putInt("Progress", cycle.progress());
        tag.putInt("Duration", duration);
        tag.putLong("FuelJoules", fuel.storedJoules());
        tag.putLong("FuelLitJoules", fuel.lastLitJoules());
        if (buffer.itemId() != null) {
            tag.putString("BufferItem", buffer.itemId());
            tag.putInt("BufferCount", buffer.count());
        }
    }
}
