package com.planetaryfactory.core.compat;

import com.planetaryfactory.core.PlanetaryFactoryCore;
import com.planetaryfactory.core.mining.rig.RigBlock;
import com.planetaryfactory.core.mining.rig.RigBlockEntity;
import com.planetaryfactory.core.mining.rig.RigPartBlock;
import com.planetaryfactory.core.mining.rig.RigPartBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.IElementHelper;

/**
 * What a mining rig is doing right now, on the HUD (#199).
 *
 * <p>The rig is the pack's hardest machine to diagnose from outside: it is a 2x2 or 3x3 whose work
 * happens <em>under</em> itself and whose output leaves sideways, so every failure looks the same
 * from every angle -- a still block. The tooltip separates the three stills that matter:
 * <strong>backed up</strong> (a banked stack the faced tile will not take), <strong>out of
 * fuel</strong> (the burner's buffer at zero) and <strong>nothing left to mine</strong>, which is
 * the one a furnace has no analogue for and the one a player is most likely to read as a bug.
 *
 * <p>Progress is a percentage rather than a tick count because the duration is per-target here --
 * uranium costs twice what iron does -- so the raw numbers name nothing the player knows.
 *
 * <p>The fuel line is shown on the burner tier only, the same rule {@link FurnaceJadePlugin} uses
 * for its electric buffer: it is stated where "not running" has an invisible cause, and would read
 * as a fault on the tier that never has one.
 *
 * <p>Both blocks carry the component. Three quarters of a 2x2 is a part rather than the anchor, so
 * a crosshair lands on one three times out of four; the menu already forwards to the anchor and a
 * tooltip that did not would be empty from three sides of the same machine.
 *
 * <p>Like the other plugins in this package, found by Jade's annotation scan and referenced from
 * nowhere else in the mod, so the jar stays a compile-time dependency.
 */
@WailaPlugin
public class RigJadePlugin implements IWailaPlugin {

    private static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath(PlanetaryFactoryCore.NAMESPACE, "rig");

    private static final String PRESENT = "RigPresent";
    private static final String BUFFER_ITEM = "RigBufferItem";
    private static final String BUFFER_COUNT = "RigBufferCount";
    private static final String PROGRESS = "RigProgress";
    private static final String DURATION = "RigDuration";
    private static final String FUEL = "RigFuel";
    private static final String FUEL_CAPACITY = "RigFuelCapacity";
    private static final String HAS_ORE = "RigHasOre";

    /**
     * The rig behind whatever the crosshair is on: itself if it is the anchor, and otherwise the
     * anchor the part remembers. A part whose anchor has gone answers nothing rather than throwing
     * -- a half-torn rig is a state a break can be observed in.
     */
    private static RigBlockEntity rigBehind(BlockEntity hit) {
        if (hit instanceof RigBlockEntity rig) {
            return rig;
        }
        if (hit instanceof RigPartBlockEntity part && part.getLevel() != null) {
            BlockPos anchor = part.anchorPos();
            if (anchor != null && part.getLevel().getBlockEntity(anchor) instanceof RigBlockEntity rig) {
                return rig;
            }
        }
        return null;
    }

    private static final IServerDataProvider<BlockAccessor> DATA = new IServerDataProvider<>() {
        @Override
        public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
            RigBlockEntity rig = rigBehind(accessor.getBlockEntity());
            if (rig == null) {
                return;
            }
            tag.putBoolean(PRESENT, true);
            String itemId = rig.bufferedItemId();
            if (itemId != null && rig.bufferedCount() > 0) {
                tag.putString(BUFFER_ITEM, itemId);
                tag.putInt(BUFFER_COUNT, rig.bufferedCount());
            }
            tag.putInt(PROGRESS, rig.data().get(RigBlockEntity.DATA_PROGRESS));
            tag.putInt(DURATION, rig.data().get(RigBlockEntity.DATA_DURATION));
            tag.putBoolean(HAS_ORE, rig.hasOre());
            if (rig.burnsFuel()) {
                tag.putInt(FUEL, rig.data().get(RigBlockEntity.DATA_FUEL));
                tag.putInt(FUEL_CAPACITY, rig.data().get(RigBlockEntity.DATA_FUEL_CAPACITY));
            }
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
            if (!data.getBoolean(PRESENT)) {
                return;
            }
            IElementHelper elements = IElementHelper.get();

            // The banked stack and how far along the rig is, read left to right the way the ore
            // moves: out of the ground, into the buffer. A full buffer against frozen progress is
            // push-or-stall's only visible symptom, and nothing outside the block shows it today.
            tooltip.add(banked(elements, data));
            tooltip.append(elements.text(progress(data)));

            if (!data.getBoolean(HAS_ORE)) {
                // The line the rig needs most. An exhausted footprint is not a fault, and without
                // this it reads as one: same still block, same full buffer, same fuel.
                tooltip.add(Component.translatable("tooltip.planetaryfactory.rig.jade.no_ore"));
            }
            if (data.contains(FUEL_CAPACITY)) {
                tooltip.add(Component.translatable("tooltip.planetaryfactory.rig.jade.fuel",
                        data.getInt(FUEL), data.getInt(FUEL_CAPACITY)));
            }
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }
    };

    /**
     * What the rig is holding. An empty buffer is a dash rather than a blank, for the same reason
     * the furnace's ends are: "nothing banked" is half the diagnosis, and a gap does not say it.
     */
    private static snownee.jade.api.ui.IElement banked(IElementHelper elements, CompoundTag data) {
        if (!data.contains(BUFFER_ITEM)) {
            return elements.text(Component.translatable("tooltip.planetaryfactory.rig.jade.empty"));
        }
        // The buffer names its item by string, so the id has to be resolved here rather than read
        // off a saved stack. An id this client cannot resolve is air, and `elements.item` on an
        // empty stack draws a blank -- which is the one thing this method promises not to do.
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(data.getString(BUFFER_ITEM)));
        ItemStack banked = new ItemStack(item, data.getInt(BUFFER_COUNT));
        return banked.isEmpty()
                ? elements.text(Component.translatable("tooltip.planetaryfactory.rig.jade.empty"))
                : elements.item(banked);
    }

    private static Component progress(CompoundTag data) {
        int duration = data.getInt(DURATION);
        int progress = data.getInt(PROGRESS);
        if (duration <= 0) {
            return Component.translatable("tooltip.planetaryfactory.rig.jade.idle");
        }
        return Component.translatable("tooltip.planetaryfactory.rig.jade.progress",
                Math.round(progress * 100F / duration));
    }

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(DATA, RigBlockEntity.class);
        registration.registerBlockDataProvider(DATA, RigPartBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(TOOLTIP, RigBlock.class);
        registration.registerBlockComponent(TOOLTIP, RigPartBlock.class);
    }
}
