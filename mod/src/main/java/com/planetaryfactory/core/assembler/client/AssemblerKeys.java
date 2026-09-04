package com.planetaryfactory.core.assembler.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.planetaryfactory.core.network.OpenPanelPacket;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
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
 *
 * <p>It is a toggle wherever it is pressed, the way the inventory key is: from the world it opens
 * the panel, from the panel it closes back to the world, and from the inventory screen it swaps one
 * for the other. A key that only opened would be a key a player presses twice and ends up where
 * they started.
 */
public final class AssemblerKeys {

    private static final String CATEGORY = "key.categories.inventory";

    private static KeyMapping openPanel;

    private AssemblerKeys() {
    }

    public static void register(RegisterKeyMappingsEvent event) {
        openPanel = new KeyMapping(
                "key.planetaryfactory_core.assembler",
                KeyConflictContext.UNIVERSAL,
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
        // Only from the world. A screen closing on the same tick would otherwise let a queued
        // press through into whatever opens next; presses made with a screen up are handled below,
        // where the screen itself says what the key should do.
        if (pressed && client.screen == null) {
            PacketDistributor.sendToServer(new OpenPanelPacket());
        }
    }

    /**
     * The same key, pressed with a screen up.
     *
     * <p>A {@link KeyMapping} is only polled from the world, so a binding that has to work inside a
     * screen has to be matched here as well. Two screens answer to it and every other one is left
     * alone: the panel closes, and the inventory swaps to the panel.
     */
    public static void onScreenKey(ScreenEvent.KeyPressed.Pre event) {
        if (openPanel == null || !openPanel.matches(event.getKeyCode(), event.getScanCode())) return;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        if (event.getScreen() instanceof AssemblerPanelScreen panel) {
            // Closed here rather than by falling through to the screen, which has no idea this key
            // means anything. onClose is what tells the server the menu is shut.
            panel.onClose();
            event.setCanceled(true);
        } else if (event.getScreen() instanceof InventoryScreen) {
            PacketDistributor.sendToServer(new OpenPanelPacket());
            event.setCanceled(true);
        }
    }
}
