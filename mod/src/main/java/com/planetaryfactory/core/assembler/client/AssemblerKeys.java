package com.planetaryfactory.core.assembler.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.planetaryfactory.core.network.OpenPanelPacket;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * One key, straight to the panel.
 *
 * <p>The Assembler is this pack's crafting surface once the grid goes (#90, #95), so reaching it
 * through the inventory screen made the thing a player uses constantly two keys deep behind the
 * thing they use occasionally. The tab stays -- it is how a player finds the Assembler exists at
 * all -- and this is how they open it afterwards.
 *
 * <p>Bound to {@code K} by default and declared in the inventory category, so it sits beside the
 * inventory key in Controls rather than under a heading of its own for a single binding. A conflict
 * with another mod is the player's to resolve, which is what the Controls screen is for.
 */
public final class AssemblerKeys {

    private static final String CATEGORY = "key.categories.inventory";

    private static KeyMapping openPanel;

    private AssemblerKeys() {
    }

    public static void register(RegisterKeyMappingsEvent event) {
        openPanel = new KeyMapping(
                "key.planetaryfactory_core.assembler",
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                CATEGORY);
        event.register(openPanel);
    }

    /**
     * Opens the panel on a press, at most one per press.
     *
     * <p>{@code consumeClick} is the drain rather than a held-down test on purpose: the panel is a
     * server-opened menu, and a key read every tick while held would send a packet a tick asking the
     * server to re-open a screen that is already open.
     */
    public static void onClientTick(PlayerTickEvent.Post event) {
        if (openPanel == null) return;
        Minecraft client = Minecraft.getInstance();
        if (client.player != event.getEntity()) return;
        boolean pressed = false;
        while (openPanel.consumeClick()) {
            pressed = true;
        }
        // Only from the world. The key is IN_GAME, but a screen closing on the same tick would
        // otherwise let a queued press through into whatever opens next.
        if (pressed && client.screen == null) {
            PacketDistributor.sendToServer(new OpenPanelPacket());
        }
    }
}
