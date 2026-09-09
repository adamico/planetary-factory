package com.planetaryfactory.core.fluid;

import com.planetaryfactory.core.PFBlocks;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;

/**
 * Placing a pump (#213, ADR-0050), and refusing to when there is nothing to pump.
 *
 * <p><b>Refused with a message, rather than placed and inert.</b> A pump that accepts any position
 * and then quietly produces nothing does not reach the player as a mistake at the pump: it reaches
 * them as a dead factory three machines downstream, with nothing in any log to say why. That is the
 * same failure the vein-indicator check exists to prevent, and the same reason the rig refuses a
 * footprint that does not fit rather than placing part of one.
 *
 * <p>Nothing is consumed on a refusal, because nothing was placed.
 */
public class OffshorePumpItem extends BlockItem {

    /** Named by {@code scripts/build-pump-assets.py}, which writes the string beside the block. */
    private static final String NO_WATER_KEY = "message.planetaryfactory.offshore_pump.no_water";

    public OffshorePumpItem(Item.Properties properties) {
        super(PFBlocks.OFFSHORE_PUMP.get(), properties);
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        if (!context.canPlace()) {
            return InteractionResult.FAIL;
        }
        if (!OffshorePumpBlock.canSit(context.getLevel(), context.getClickedPos())) {
            Player player = context.getPlayer();
            if (player != null && !context.getLevel().isClientSide()) {
                // Above the hotbar rather than in chat: it is feedback on a gesture the player just
                // made, not a log line.
                player.displayClientMessage(Component.translatable(NO_WATER_KEY), true);
            }
            return InteractionResult.FAIL;
        }
        return super.place(context);
    }
}
