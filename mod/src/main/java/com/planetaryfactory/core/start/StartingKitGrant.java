package com.planetaryfactory.core.start;

import com.mojang.logging.LogUtils;
import com.planetaryfactory.core.PFAttachments;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.slf4j.Logger;

/**
 * The starting kit, handed over on first join (#203).
 *
 * <p>The spec has specified the pocket and the hold since the Opening was written, and nothing
 * granted them. The stub #170 allowed -- a {@code /give} string the tester types before starting the
 * clock -- is enough for the links pass, which tolerates wrong numbers entirely, and not enough for
 * the pace run: ADR-0040 put the drill in the pocket for a <em>pacing</em> reason, and beat 4 sits at
 * the twenty-minute mark. There is exactly one uncontaminated pace reading, and a run whose opening
 * depends on the tester's memory measures the tester.
 *
 * <p>Once per player rather than once per join, and why, is on {@link StartingGrant}.
 *
 * <p>This is the neighbour of {@link com.planetaryfactory.core.worldgen.TerraStartingArea}: the area
 * is stamped once per world on {@code ServerStartedEvent}, and the kit is granted once per
 * <em>player</em> on login. The two are separate because a world can gain a player later.
 *
 * <p>FTB Quests does not hand out its own book on first join in {@code 2101.1.31} -- the setting
 * that used to is gone, and nothing in the jar grants one -- so the pocket's book is the only book.
 *
 * <p>The wreck itself, spawning inside it and the habitable volume (#100 / #134) stay out of the
 * slice, so the hold's three stacks land in the inventory alongside the pocket rather than in a
 * cargo hold. When #133 arrives, the hold moves into it and the pocket does not.
 */
public final class StartingKitGrant {
    private static final Logger LOGGER = LogUtils.getLogger();

    private StartingKitGrant() {
    }

    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        StartingGrant grant = player.getData(PFAttachments.STARTING_GRANT);
        // claim() marks the attachment as it hands the kit over, and the attachment is the one the
        // player holds -- so the flag is already set by the time anything is delivered. A delivery
        // that throws halfway costs the items rather than handing out a second kit next login,
        // which is the same order TerraStartingArea marks its stamp in and for the same reason.
        List<StartingKit.Entry> kit = grant.claim();
        if (kit.isEmpty()) {
            return;
        }

        for (StartingKit.Entry entry : kit) {
            deliver(player, entry);
        }
        LOGGER.info("Granted the starting kit to {}: {} stacks", player.getGameProfile().getName(),
                kit.size());
    }

    /**
     * One entry into the player's inventory, or onto the floor at their feet if it will not fit.
     *
     * <p>An unresolvable id is logged rather than thrown. It reaches the player as an empty pocket
     * slot and nothing else, which is why {@code tests/pack/test_starting_kit.py} asserts every id
     * resolves before a world is ever loaded -- this log line is the second line of defence.
     */
    private static void deliver(ServerPlayer player, StartingKit.Entry entry) {
        ResourceLocation id = ResourceLocation.tryParse(entry.item());
        Item item = id == null ? null : BuiltInRegistries.ITEM.getOptional(id).orElse(null);
        if (item == null) {
            LOGGER.error("The starting kit names {}, which is not a registered item: the player "
                    + "starts without it", entry.item());
            return;
        }
        ItemStack stack = new ItemStack(item, entry.count());
        // `add` returns true when *any* of the stack moved and shrinks what it took, so the
        // remainder is what decides -- a partial fit returns true and would otherwise be dropped
        // on the floor of the inventory rather than at the player's feet.
        player.getInventory().add(stack);
        if (!stack.isEmpty()) {
            player.drop(stack, false);
        }
    }
}
