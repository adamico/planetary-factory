package com.planetaryfactory.core.compat;

import com.planetaryfactory.core.PlanetaryFactoryCore;
import com.planetaryfactory.core.energy.PoleColumn;
import com.planetaryfactory.core.energy.SupplyAreaPoleBlock;
import com.planetaryfactory.core.energy.SupplyAreaPoleBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/**
 * What a pole is doing right now, on the HUD.
 *
 * <p>The item tooltip states what a pole <em>is</em> -- its footprint, that it is wireless, that the
 * area is measured at the base. This is the other half: the two numbers that can only be read from
 * a running pole, at the moment a player is standing in front of one asking why a machine is dark.
 *
 * <p>They are chosen to separate the only two failure modes a player cannot otherwise tell apart.
 * <strong>Out of range</strong> reads as a machine count that does not include the machine in
 * question. <strong>Underfed</strong> reads as a count that does, with delivery below demand.
 * Without both, those two look identical -- a machine that is not running -- and the pack has no
 * other surface that would ever distinguish them.
 *
 * <p>Deliberately not here: the tier and the footprint, which the item tooltip already carries and
 * Jade prints the block's name above anyway; and which Power Grid circuit the pole is on, which
 * would mean reading that mod's internals for a diagnostic its own multimeter, plotter and goggles
 * already provide (ADR-0036 commits to touching no internals, and the division of teaching labour
 * is the ADR's).
 *
 * <p>Like the EMI and JEI plugins, this class is found by Jade's own annotation scan and is
 * referenced from nowhere else in the mod, so the jar is a compile-time dependency only.
 */
@WailaPlugin
public class PoleJadePlugin implements IWailaPlugin {

    private static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath(PlanetaryFactoryCore.NAMESPACE, "supply_area_pole");

    private static final String MACHINES = "PoleMachines";
    private static final String DELIVERED = "PoleDelivered";
    private static final String DEMANDED = "PoleDemanded";

    /**
     * The numbers live on the server, so they have to be asked for.
     *
     * <p>The block entity Jade hands over is the one at the position being looked at, which for an
     * extension is that segment's inert block entity. So this resolves to the base the same way the
     * capability does -- a column reads as one object on the HUD, exactly as it does to a connector.
     */
    private static final IServerDataProvider<BlockAccessor> DATA = new IServerDataProvider<>() {
        @Override
        public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
            BlockPos base = PoleColumn.baseOf(accessor.getLevel(), accessor.getPosition());
            if (base == null
                    || !(accessor.getLevel().getBlockEntity(base)
                    instanceof SupplyAreaPoleBlockEntity pole)) {
                return;
            }
            tag.putInt(MACHINES, pole.machineCount());
            tag.putLong(DELIVERED, pole.deliveredEuPerTick());
            tag.putLong(DEMANDED, pole.demandedEuPerTick());
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }
    };

    private static final IBlockComponentProvider TOOLTIP = new IBlockComponentProvider() {
        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if (!data.contains(MACHINES)) {
                return;
            }
            int machines = data.getInt(MACHINES);
            tooltip.add(Component.translatable("tooltip.planetaryfactory.pole.jade.machines",
                    machines));
            if (machines == 0) {
                return;
            }
            long demanded = data.getLong(DEMANDED);
            if (demanded == 0L) {
                // Machines are in range and none of them wants anything: they are full, or idle.
                // Reporting "0 / 0 EU/t" here would read as a fault rather than as a quiet factory.
                tooltip.add(Component.translatable("tooltip.planetaryfactory.pole.jade.idle"));
                return;
            }
            tooltip.add(Component.translatable("tooltip.planetaryfactory.pole.jade.supply",
                    data.getLong(DELIVERED), demanded));
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }
    };

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(DATA, SupplyAreaPoleBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(TOOLTIP, SupplyAreaPoleBlock.class);
    }
}
