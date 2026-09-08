package com.planetaryfactory.core.mining.rig;

import javax.annotation.Nullable;

import com.planetaryfactory.core.PFBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A part's only job: remember where its anchor is (#192). It carries no rules of its own --
 * everything a rig does lives on {@link RigBlockEntity}; this is the pointer the bed-and-door
 * idiom needs to find it from any part, which is what lets breaking any part break the rig.
 */
public class RigPartBlockEntity extends BlockEntity {

    private static final String TAG_ANCHOR_X = "anchor_x";
    private static final String TAG_ANCHOR_Y = "anchor_y";
    private static final String TAG_ANCHOR_Z = "anchor_z";

    @Nullable
    private BlockPos anchorPos;

    public RigPartBlockEntity(BlockPos pos, BlockState state) {
        super(PFBlockEntities.RIG_PART.get(), pos, state);
    }

    /** Set once, at placement, by the item that placed the whole footprint in one click. */
    public void setAnchorPos(BlockPos anchorPos) {
        this.anchorPos = anchorPos;
        setChanged();
    }

    @Nullable
    public BlockPos anchorPos() {
        return anchorPos;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (anchorPos != null) {
            tag.putInt(TAG_ANCHOR_X, anchorPos.getX());
            tag.putInt(TAG_ANCHOR_Y, anchorPos.getY());
            tag.putInt(TAG_ANCHOR_Z, anchorPos.getZ());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        anchorPos = tag.contains(TAG_ANCHOR_X)
                ? new BlockPos(tag.getInt(TAG_ANCHOR_X), tag.getInt(TAG_ANCHOR_Y), tag.getInt(TAG_ANCHOR_Z))
                : null;
    }
}
