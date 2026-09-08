package com.planetaryfactory.core.mining.rig;

import com.planetaryfactory.core.PFBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The anchor's block entity (#192).
 *
 * <p>Empty today, deliberately: #192 delivers "an inert footprint with a facing" and nothing else
 * -- no mining, no fuel, no ejecting. ADR-0043 gives this class its future work: a fuel buffer or a
 * supply-area pole customer, and the operations-per-second logic behind either rig. It exists now
 * because the anchor is where that mechanism will live, and every part already forwards to it
 * rather than to itself, so #193/#194 add fields here rather than re-plumbing the forwarding.
 */
public class RigBlockEntity extends BlockEntity {

    public RigBlockEntity(BlockPos pos, BlockState state) {
        super(PFBlockEntities.RIG.get(), pos, state);
    }
}
