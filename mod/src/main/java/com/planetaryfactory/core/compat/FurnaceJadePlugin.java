package com.planetaryfactory.core.compat;

import com.planetaryfactory.core.PlanetaryFactoryCore;
import com.planetaryfactory.core.smelting.FurnaceBlock;
import com.planetaryfactory.core.smelting.FurnaceBlockEntity;
import com.planetaryfactory.core.smelting.FurnaceSlots;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
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
 * What a furnace is doing right now, on the HUD (#155).
 *
 * <p>A furnace is the one machine in the pack a player walks a whole line of, and the question at
 * a glance is always the same: is this one working, and if not, which end is at fault. So the
 * tooltip is the input, the output and how far along the smelt is -- the three facts that separate
 * <strong>starved</strong> (no input) from <strong>backed up</strong> (output full, progress
 * frozen) from <strong>unpowered</strong> (both ends fine, progress at zero). Opening the GUI
 * answers this too, and that is exactly the cost: it is one block at a time.
 *
 * <p>The Electric tier adds its buffer, which is the only tier whose "unpowered" has a cause the
 * player cannot see from outside -- a burner's dark front already says it. It is deliberately not
 * shown on the burners, where it is always zero and would read as a fault.
 *
 * <p>The fuel slot is not here. The flame on the front of the block already carries it, and a
 * third stack in the line would push the two that diagnose a stall further from the eye.
 *
 * <p>Like the other plugins in this package, found by Jade's annotation scan and referenced from
 * nowhere else in the mod, so the jar stays a compile-time dependency.
 */
@WailaPlugin
public class FurnaceJadePlugin implements IWailaPlugin {

    private static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath(PlanetaryFactoryCore.NAMESPACE, "furnace");

    private static final String INPUT = "FurnaceInput";
    private static final String OUTPUT = "FurnaceOutput";
    private static final String PROGRESS = "FurnaceProgress";
    private static final String DURATION = "FurnaceDuration";
    private static final String ENERGY = "FurnaceEnergy";
    private static final String CAPACITY = "FurnaceCapacity";

    private static final IServerDataProvider<BlockAccessor> DATA = new IServerDataProvider<>() {
        @Override
        public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof FurnaceBlockEntity furnace)) {
                return;
            }
            put(tag, INPUT, furnace.getItem(FurnaceSlots.INPUT), accessor);
            put(tag, OUTPUT, furnace.getItem(FurnaceSlots.OUTPUT), accessor);
            tag.putInt(PROGRESS, furnace.data().get(FurnaceBlockEntity.DATA_PROGRESS));
            tag.putInt(DURATION, furnace.data().get(FurnaceBlockEntity.DATA_DURATION));
            if (!furnace.tier().burnsFuel()) {
                tag.putInt(ENERGY, furnace.data().get(FurnaceBlockEntity.DATA_ENERGY));
                tag.putInt(CAPACITY, furnace.data().get(FurnaceBlockEntity.DATA_ENERGY_CAPACITY));
            }
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }
    };

    private static void put(CompoundTag tag, String key, ItemStack stack, BlockAccessor accessor) {
        if (!stack.isEmpty()) {
            tag.put(key, stack.save(accessor.getLevel().registryAccess()));
        }
    }

    private static ItemStack read(CompoundTag tag, String key, BlockAccessor accessor) {
        return tag.contains(key)
                ? ItemStack.parse(accessor.getLevel().registryAccess(), tag.getCompound(key))
                        .orElse(ItemStack.EMPTY)
                : ItemStack.EMPTY;
    }

    private static final IBlockComponentProvider TOOLTIP = new IBlockComponentProvider() {
        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if (!data.contains(PROGRESS)) {
                return;
            }
            IElementHelper elements = IElementHelper.get();
            ItemStack input = read(data, INPUT, accessor);
            ItemStack output = read(data, OUTPUT, accessor);

            // One line, read left to right the way the item moves: what goes in, how far along,
            // what has come out. An empty end is stated rather than left blank -- "nothing here"
            // is the diagnosis, and a gap in the line does not say it.
            tooltip.add(end(elements, input));
            tooltip.append(elements.text(progress(data)));
            tooltip.append(end(elements, output));

            if (data.contains(CAPACITY)) {
                tooltip.add(Component.translatable("tooltip.planetaryfactory.furnace.jade.energy",
                        data.getInt(ENERGY), data.getInt(CAPACITY)));
            }
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }
    };

    /**
     * One end of the line. An empty slot is a dash rather than a blank: the whole point of the
     * line is telling starved from backed up, and a gap where an item should be says neither.
     */
    private static snownee.jade.api.ui.IElement end(IElementHelper elements, ItemStack held) {
        return held.isEmpty()
                ? elements.text(Component.translatable("tooltip.planetaryfactory.furnace.jade.empty"))
                : elements.item(held);
    }

    private static Component progress(CompoundTag data) {
        int duration = data.getInt(DURATION);
        int progress = data.getInt(PROGRESS);
        if (duration <= 0) {
            return Component.translatable("tooltip.planetaryfactory.furnace.jade.idle");
        }
        return Component.translatable("tooltip.planetaryfactory.furnace.jade.progress",
                Math.round(progress * 100F / duration));
    }

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(DATA, FurnaceBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(TOOLTIP, FurnaceBlock.class);
    }
}
