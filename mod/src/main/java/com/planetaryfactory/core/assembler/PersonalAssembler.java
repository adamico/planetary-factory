package com.planetaryfactory.core.assembler;

import com.planetaryfactory.core.PFAttachments;
import com.planetaryfactory.core.network.PFNetwork;
import com.planetaryfactory.core.network.QueueSyncPacket;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;

/**
 * The server's side of the Personal Assembler: what each packet actually does.
 *
 * <p>Everything here runs on the server, which is the point. A plan is server truth (ADR-0038), so
 * the client never holds one -- it holds a {@link PlanDisplay} and a queue view, and every decision
 * that spends or refunds an item is made on this side of the wire.
 */
public final class PersonalAssembler {

    /**
     * The plan each player is currently looking at, between the Crafting Plan opening and Start.
     *
     * <p>Deliberately not persisted. A pending plan is an open dialog, not a commitment: nothing has
     * been paid for until Start, so a plan that outlived a logout would be a dialog with no screen
     * and a reservation nobody took. The queue is the thing that persists, and it does so on the
     * player's data attachment.
     */
    private static final Map<UUID, CraftingPlan> PENDING = new HashMap<>();

    private PersonalAssembler() {
    }

    public static AssemblerQueue queueOf(Player player) {
        return player.getData(PFAttachments.ASSEMBLER_QUEUE.get());
    }

    /** Opens the panel. This is the tab on the inventory screen, and EMI's precondition. */
    public static void openPanel(ServerPlayer player) {
        PENDING.remove(player.getUUID());
        player.openMenu(new SimpleMenuProvider(
                (id, inventory, who) -> new AssemblerPanelMenu(id, inventory),
                Component.translatable("planetaryfactory_core.assembler.panel")));
        sync(player);
    }

    /**
     * Step 3: Select Amount, with the count EMI's button asked for as its starting value and the
     * resolver's {@code all} beside it.
     */
    public static void openSelectAmount(ServerPlayer player, ResourceLocation recipe, int amount) {
        PENDING.remove(player.getUUID());
        int all = PlanSource.ACTIVE.largestAffordable(player, recipe);
        int initial = Math.max(1, Math.min(amount, Math.max(1, all)));
        player.openMenu(
                new SimpleMenuProvider(
                        (id, inventory, who) -> new SelectAmountMenu(id, inventory, recipe, initial, all),
                        Component.translatable("planetaryfactory_core.assembler.select_amount")),
                buffer -> {
                    ResourceLocation.STREAM_CODEC.encode(buffer, recipe);
                    buffer.writeVarInt(initial);
                    buffer.writeVarInt(all);
                });
    }

    /**
     * Step 4: resolve, and open the Crafting Plan on the result. The plan-result is the dialog's own
     * opening data, so the answer and the screen arrive together.
     */
    public static void openPlan(ServerPlayer player, ResourceLocation recipe, int amount) {
        PlanSource.ResolvedPlan resolved = PlanSource.ACTIVE.resolve(player, recipe, Math.max(1, amount));
        if (resolved.complete()) {
            PENDING.put(player.getUUID(), resolved.plan());
        } else {
            PENDING.remove(player.getUUID());
        }
        player.openMenu(
                new SimpleMenuProvider(
                        (id, inventory, who) -> new CraftingPlanMenu(id, inventory, resolved.display()),
                        Component.translatable("planetaryfactory_core.assembler.plan")),
                buffer -> PlanDisplay.STREAM_CODEC.encode(buffer, resolved.display()));
    }

    /**
     * Step 5: Start, which pays for the whole plan at once and appends it.
     *
     * <p>Refused unless a complete plan is pending, which is the same refusal the dialog makes --
     * asserted again here because a packet is not a button and arrives from wherever it likes.
     */
    public static boolean start(ServerPlayer player, UUID planId) {
        CraftingPlan plan = PENDING.get(player.getUUID());
        if (plan == null || !plan.id().equals(planId)) return false;
        AssemblerQueue queue = queueOf(player);
        boolean started = queue.enqueue(plan, new InventoryPlayerItems(player.getInventory()));
        if (!started) {
            // The inventory changed between the dialog and the click. The plan stays pending and the
            // dialog stays open, so the player sees what they were looking at rather than a screen
            // that closed and a craft that never happened.
            return false;
        }
        PENDING.remove(player.getUUID());
        player.setData(PFAttachments.ASSEMBLER_QUEUE.get(), queue);
        openPanel(player);
        return true;
    }

    /**
     * Cancels a plan and refunds its buffer. Anything that will not fit goes to the player the way
     * a closed container's contents do -- a cancellation is their own action, unlike a finished
     * craft, which pauses rather than drops.
     */
    public static boolean cancel(ServerPlayer player, UUID planId) {
        AssemblerQueue queue = queueOf(player);
        AssemblerQueue.CancelResult result = queue.cancel(planId, new InventoryPlayerItems(player.getInventory()));
        if (!result.cancelled()) return false;
        for (ItemAmount leftover : result.notReturned()) {
            player.getInventory().placeItemBackInInventory(InventoryPlayerItems.toStack(leftover));
        }
        player.setData(PFAttachments.ASSEMBLER_QUEUE.get(), queue);
        sync(player);
        return true;
    }

    /** One tick of one player's queue, run whether or not any screen is open. */
    public static void tick(ServerPlayer player) {
        AssemblerQueue queue = queueOf(player);
        if (queue.isEmpty()) return;
        queue.tick(new InventoryPlayerItems(player.getInventory()));
        player.setData(PFAttachments.ASSEMBLER_QUEUE.get(), queue);
    }

    /** Sends the queue's display view. The plan itself never crosses. */
    public static void sync(ServerPlayer player) {
        PFNetwork.sendToPlayer(player, QueueSyncPacket.of(queueOf(player)));
    }

    /** A pending plan belongs to a session, not to a save. */
    public static void forget(ServerPlayer player) {
        PENDING.remove(player.getUUID());
    }
}
