package com.planetaryfactory.core.mining.rig;

import java.util.List;

import com.planetaryfactory.core.PFBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * One click places all four or nine blocks (#192, ADR-0043): the anchor at the targeted tile and
 * its parts extending away from the player by their horizontal look direction.
 *
 * <p>This entirely replaces {@link BlockItem#place}, rather than overriding {@code getStateForPlacement}
 * on the anchor block, because the default single-block flow has no hook for "and also place these
 * other N blocks" -- the door and bed idiom this mirrors does the same thing in vanilla.
 *
 * <p><b>Placement is refused with nothing consumed where the footprint does not fit.</b> Every
 * target tile is checked before any block is placed and before the stack is shrunk; a partial
 * footprint that ate the item on a failed 2x2 is exactly ADR-0043's "bad first machine".
 */
public class RigBlockItem extends BlockItem {

    private final RigTier tier;

    public RigBlockItem(RigTier tier, Item.Properties properties) {
        super(PFBlocks.rig(tier).get(), properties);
        this.tier = tier;
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        if (!context.canPlace()) {
            return InteractionResult.FAIL;
        }
        Level level = context.getLevel();
        BlockPos anchorPos = context.getClickedPos();
        Direction facing = context.getHorizontalDirection();
        RigFacing rigFacing = RigDirections.toRigFacing(facing);
        RigFootprints.Size size = RigFootprints.get().sizeOf(tier);
        List<RigGeometry.Offset> offsets = RigGeometry.footprint(size.width(), size.height(), rigFacing);

        boolean fits = RigGeometry.fits(offsets, offset -> {
            BlockPos pos = anchorPos.offset(offset.dx(), 0, offset.dz());
            return level.isInWorldBounds(pos) && level.getBlockState(pos).canBeReplaced();
        });
        if (!fits) {
            return InteractionResult.FAIL;
        }

        Block anchorBlock = PFBlocks.rig(tier).get();
        Block partBlock = PFBlocks.rigPart(tier).get();
        BlockState anchorState = anchorBlock.defaultBlockState().setValue(RigBlock.FACING, facing);
        BlockState partState = partBlock.defaultBlockState().setValue(RigPartBlock.FACING, facing);

        level.setBlock(anchorPos, anchorState, Block.UPDATE_ALL);
        for (RigGeometry.Offset offset : offsets) {
            if (offset.dx() == 0 && offset.dz() == 0) {
                continue;
            }
            BlockPos pos = anchorPos.offset(offset.dx(), 0, offset.dz());
            level.setBlock(pos, partState, Block.UPDATE_ALL);
            if (level.getBlockEntity(pos) instanceof RigPartBlockEntity part) {
                part.setAnchorPos(anchorPos);
            }
        }

        Player player = context.getPlayer();
        SoundType sound = anchorState.getSoundType();
        level.playSound(player, anchorPos, sound.getPlaceSound(), SoundSource.BLOCKS,
                (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
        level.gameEvent(GameEvent.BLOCK_PLACE, anchorPos, GameEvent.Context.of(player, anchorState));

        context.getItemInHand().consume(1, player);
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
