package com.planetaryfactory.core.energy;

import com.gregtechceu.gtceu.api.capability.GTCapability;
import com.gregtechceu.gtceu.api.capability.IEnergyContainer;
import com.planetaryfactory.core.PFBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * The pole's tick: rescan the area now and then, push EU into everything found, every tick.
 *
 * <h2>FE in, EU out</h2>
 *
 * <p>Power Grid's Device Connector is one-way grid-to-FE ({@code canReceive() == false}), so FE is
 * the only currency the grid can hand a pack block. Every machine in the pack takes EU. The
 * conversion happens once, here, in {@link EnergyLedger} -- which is why GTCEu's FE converters stay
 * disabled and why no converter block appears anywhere in the pack.
 *
 * <p>Energy is inserted with {@link IEnergyContainer#addEnergy(long)} rather than
 * {@code acceptEnergyFromNetwork}. The latter enforces GregTech's voltage tiers and can overvolt a
 * machine into exploding; {@code #37} deleted that ladder entire, so there is no tier for a pole to
 * respect and direct insertion is the honest operation. A pole supplies wirelessly, so it also has
 * no face to be accepted through.
 *
 * <h2>Why the receiver list is cached</h2>
 *
 * <p>A substation's area is 18x18x5 = 1620 blocks. A capability lookup per block per tick is not
 * affordable, and it is also pointless: machines do not appear and vanish every tick. The area is
 * rescanned on {@link #RESCAN_INTERVAL} and energy is pushed to the cached list in between, so a
 * newly placed machine waits at most two seconds to be picked up and the steady state costs one
 * lookup per actual receiver.
 */
public class SupplyAreaPoleBlockEntity extends BlockEntity {

    /** Ticks between rescans of the supply area. Two seconds. */
    private static final int RESCAN_INTERVAL = 40;

    /**
     * The FE buffer, sized at one tick of a busy area and derived rather than picked.
     *
     * <p>A GregTech LV machine draws 32 EU/t. A substation area packed with 32 of them is
     * 1024 EU/t, which is {@code 1024 * }{@link EnergyLedger#FE_PER_EU}{@code  = 4096 FE} for a
     * single tick. That is the number: enough that a full area can be served from one push, and
     * not one tick more.
     *
     * <p>Sizing it that way is what keeps it clear of the machine-side storage ADR-0036 forbids.
     * That prohibition exists so sag and blown fuses reach the machines instead of being absorbed,
     * and a buffer holding one tick cannot absorb anything a player could perceive -- the moment
     * the grid gives less, the very next tick gives less. The buffer exists at all only because a
     * capability push and a block tick do not happen at the same instant.
     */
    private static final long BUFFER_FE = 4_096L;

    private final EnergyLedger ledger = new EnergyLedger(BUFFER_FE);
    private final PoleEnergyStorage feSide = new PoleEnergyStorage(this);

    private List<BlockPos> receivers = List.of();
    private int sinceRescan = RESCAN_INTERVAL;

    public SupplyAreaPoleBlockEntity(BlockPos pos, BlockState state) {
        super(PFBlockEntities.SUPPLY_AREA_POLE.get(), pos, state);
    }

    public EnergyLedger ledger() {
        return ledger;
    }

    /** The FE face the grid mod's bridge block feeds. There is no separate boundary block. */
    public PoleEnergyStorage feSide() {
        return feSide;
    }

    public PoleTier tier() {
        // The block entity type is registered against the four pole blocks and nothing else, so
        // this cannot fail. Saying so loudly beats defaulting to SMALL, which would answer a
        // question wrongly rather than reveal that the registration had come apart.
        if (getBlockState().getBlock() instanceof SupplyAreaPoleBlock pole) {
            return pole.tier();
        }
        throw new IllegalStateException(
                "supply-area pole block entity on " + getBlockState().getBlock()
                        + " at " + getBlockPos() + ", which is not a pole");
    }

    void serverTick() {
        if (level == null || level.isClientSide()) {
            return;
        }

        if (++sinceRescan >= RESCAN_INTERVAL) {
            sinceRescan = 0;
            receivers = scan(level);
        }
        if (receivers.isEmpty() || ledger.availableEu() <= 0L) {
            return;
        }
        distribute(level);
    }

    /** Every position in the area that currently answers with a GregTech energy container. */
    private List<BlockPos> scan(Level level) {
        List<BlockPos> found = new ArrayList<>();
        BlockPos origin = getBlockPos();
        SupplyArea.forEachOffset(tier(), (dx, dy, dz) -> {
            BlockPos pos = origin.offset(dx, dy, dz);
            if (pos.equals(origin) || !level.isLoaded(pos)) {
                return;
            }
            if (container(level, pos) != null) {
                found.add(pos.immutable());
            }
        });
        return found;
    }

    /** A machine that answered this tick, and the room it has. */
    private record Receiver(IEnergyContainer container, long demand) {
    }

    private void distribute(Level level) {
        List<Receiver> hungry = new ArrayList<>(receivers.size());
        for (BlockPos pos : receivers) {
            IEnergyContainer container = container(level, pos);
            if (container == null) {
                continue;
            }
            long demand = container.getEnergyCanBeInserted();
            if (demand > 0L) {
                hungry.add(new Receiver(container, demand));
            }
        }
        if (hungry.isEmpty()) {
            return;
        }

        long[] demands = hungry.stream().mapToLong(Receiver::demand).toArray();
        long[] grants = EnergyShare.waterFill(ledger.availableEu(), demands);

        long spent = 0L;
        for (int i = 0; i < grants.length; i++) {
            if (grants[i] > 0L) {
                spent += hungry.get(i).container().addEnergy(grants[i]);
            }
        }
        if (spent > 0L) {
            ledger.drainEu(spent);
            setChanged();
        }
    }

    private static IEnergyContainer container(Level level, BlockPos pos) {
        // A pole supplies wirelessly, so it has no natural side to ask through. GregTech machines
        // answer on a null context; the faces are a fallback for anything that insists on one.
        IEnergyContainer container =
                level.getCapability(GTCapability.CAPABILITY_ENERGY_CONTAINER, pos, null);
        if (container != null) {
            return container;
        }
        for (Direction side : Direction.values()) {
            container = level.getCapability(GTCapability.CAPABILITY_ENERGY_CONTAINER, pos, side);
            if (container != null) {
                return container;
            }
        }
        return null;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ledger.setStoredFe(tag.getLong("StoredFe"));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("StoredFe", ledger.storedFe());
    }
}
