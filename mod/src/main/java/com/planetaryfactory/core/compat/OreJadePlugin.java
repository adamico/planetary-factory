package com.planetaryfactory.core.compat;

import com.planetaryfactory.core.PlanetaryFactoryCore;
import com.planetaryfactory.core.ore.OreBlock;
import com.planetaryfactory.core.ore.OreMining;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
 * How much ore is left in the block the player is looking at (ADR-0041).
 *
 * <p>The amounts run to hundreds or thousands, and ADR-0041 refused to render them as worn
 * textures alone: eight stages over a thousand units is a lossy picture of a count, and a player
 * planning a drill placement needs the count. So the stages give the survey and this gives the
 * number, and neither can disagree with the other because both are the same reading.
 *
 * <p>The number is server-side -- it is a chunk attachment and a saved field record -- so it has to
 * be asked for. Jade takes a plain {@code Class<?>}, so an ore block needs no block entity to carry
 * a line here, which is the whole point of ADR-0041's storage choice.
 *
 * <p>Found by Jade's own annotation scan and referenced from nowhere else in the mod, exactly like
 * {@link PoleJadePlugin}, so the jar stays a compile-time dependency.
 */
@WailaPlugin
public class OreJadePlugin implements IWailaPlugin {

    private static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath(PlanetaryFactoryCore.NAMESPACE, "ore_amount");

    private static final String REMAINING = "OreRemaining";
    private static final String INITIAL = "OreInitial";

    private static final IServerDataProvider<BlockAccessor> DATA = new IServerDataProvider<>() {
        @Override
        public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
            if (!(accessor.getBlock() instanceof OreBlock ore)
                    || !(accessor.getLevel() instanceof ServerLevel level)) {
                return;
            }
            tag.putInt(REMAINING, OreMining.remaining(level, accessor.getPosition(), ore));
            tag.putInt(INITIAL, OreMining.initialAmount(level, ore, accessor.getPosition()));
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
            if (!data.contains(REMAINING)) {
                return;
            }
            tooltip.add(Component.translatable(
                    "tooltip.planetaryfactory.ore.jade.amount",
                    data.getInt(REMAINING),
                    data.getInt(INITIAL)));
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }
    };

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(DATA, OreBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(TOOLTIP, OreBlock.class);
    }
}
