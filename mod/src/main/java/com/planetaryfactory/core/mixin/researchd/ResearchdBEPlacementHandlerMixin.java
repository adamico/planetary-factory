package com.planetaryfactory.core.mixin.researchd;

import com.planetaryfactory.core.research.PlacedByOwnership;
import com.portingdeadmods.researchd.data.ResearchdAttachments;
import com.portingdeadmods.researchd.events.common.ResearchdBEPlacementHandler;
import java.util.UUID;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Stops Researchd re-owning a machine that already has an owner.
 *
 * <p>Researchd's handler writes its placed-by attachment on every player placement without reading
 * what is there first, so a block entity that arrives already carrying an owner is re-stamped to
 * whoever set it down. Ordinary placement never hits that -- a freshly placed machine carries
 * nothing -- but this pack ships Carry On and Building Gadgets, and both move placed block entities
 * with their attachments intact. A machine another team built therefore changes hands by being
 * picked up and put down, and a placer with no team replaces a valid owner with the empty UUID,
 * which is strictly worse than no attachment: Researchd still pushes a frame, for a team that does
 * not exist, and every lock passes.
 *
 * <p>Cancelling the handler is the whole fix. Ownership stays Researchd's attachment, written by
 * Researchd's own code -- the pack keeps no second record of who owns a machine, it only declines
 * one overwrite. Issue #74.
 *
 * <p>Cancelling the method rather than the write is deliberate: the write is everything the handler
 * does, and an {@code @Inject} at HEAD does not depend on the shape of its body.
 */
@Mixin(value = ResearchdBEPlacementHandler.class, remap = false)
public class ResearchdBEPlacementHandlerMixin {

    @Inject(method = "entityPlaceEvent", at = @At("HEAD"), cancellable = true)
    private static void planetaryfactory$keepExistingOwner(BlockEvent.EntityPlaceEvent event, CallbackInfo ci) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Player)) return;

        BlockEntity be = entity.level().getBlockEntity(event.getPos());
        if (be == null) return;

        UUID stored = be.getData(ResearchdAttachments.PLACED_BY_UUID);
        if (PlacedByOwnership.isOwned(stored)) {
            ci.cancel();
        }
    }
}
