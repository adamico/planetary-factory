package com.planetaryfactory.core.assembler.client;

import com.planetaryfactory.core.assembler.AssemblerQueueView;
import com.planetaryfactory.core.network.QueueSyncPacket;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;

/**
 * The queue beside the hotbar, so a plan running while the player is playing is visible without
 * opening anything.
 *
 * <p>The queue keeps going with every screen shut -- that is the point of a queue rather than a
 * crafting grid (ADR-0038) -- and until this existed the only way to see it was to stop playing and
 * open the panel, which is the opposite of what a background queue is for.
 *
 * <p>Read-only, and not because interaction was hard: the cursor is held by the camera while the
 * HUD is up, so a button here would be a button nothing can press. Cancelling stays on the panel,
 * where the pointer is.
 *
 * <p>It draws {@link AssemblerQueueView}, the same client copy the panel draws, which the server
 * re-syncs four times a second whether or not a screen is open.
 */
final class AssemblerHud implements LayeredDraw.Layer {

    /**
     * How many plans the corner shows.
     *
     * <p>A cap rather than a scroll: this is glanceable status beside the hotbar, and a queue tall
     * enough to reach the crosshair would be in the way of the game it is reporting on.
     */
    private static final int MAX_ROWS = 5;

    private static final int ROW_HEIGHT = 20;
    private static final int WIDTH = 96;

    /** Vanilla's hotbar: 182 wide, centred, its top 22 pixels off the bottom. */
    private static final int HOTBAR_HALF_WIDTH = 91;

    private static final int HOTBAR_HEIGHT = 22;

    /**
     * Clear of the offhand slot, which vanilla puts 29 pixels beyond the hotbar's left edge.
     *
     * <p>The queue sits left of the hotbar at the hotbar's own height, so the offhand slot is the
     * one thing it can collide with -- and it appears and disappears with what the player is
     * holding, which is the worst kind of collision to leave to chance.
     */
    private static final int CLEAR_OF_OFFHAND = 32;

    private static final int BACKDROP = 0x90101010;
    private static final int BAR = 0xFF4FA84F;
    private static final int BAR_BLOCKED = 0xFFB05030;

    @Override
    public void render(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft client = Minecraft.getInstance();
        if (client.options.hideGui || client.player == null) return;
        List<QueueSyncPacket.Entry> entries = AssemblerQueueView.entries();
        if (entries.isEmpty()) return;

        int shown = Math.min(MAX_ROWS, entries.size());
        // Left of the hotbar, bottom row level with it, stacked upward from there.
        int left = graphics.guiWidth() / 2 - HOTBAR_HALF_WIDTH - CLEAR_OF_OFFHAND - WIDTH;
        int bottom = graphics.guiHeight() - (HOTBAR_HEIGHT - ROW_HEIGHT) / 2 - ROW_HEIGHT;
        int top = bottom - (shown - 1) * ROW_HEIGHT;
        graphics.fill(left - 3, top - 3, left + WIDTH + 3, bottom + ROW_HEIGHT - 1, BACKDROP);

        Font font = client.font;
        for (int index = 0; index < shown; index++) {
            QueueSyncPacket.Entry entry = entries.get(index);
            int y = top + index * ROW_HEIGHT;
            // What is being made now, falling back to what the plan is for on a plan whose last step
            // has finished and is waiting for room to deliver.
            String item = entry.hasStep() ? entry.stepItem() : entry.rootItem();
            int count = entry.hasStep() ? entry.stepAmount() : entry.amount();
            graphics.renderItem(PlanItems.stack(item), left, y);
            graphics.drawString(font, "x" + count, left + 20, y + 4, 0xFFFFFF, true);
            if (entry.steps() > 1) {
                Component of = Component.literal((entry.step() + 1) + "/" + entry.steps());
                graphics.drawString(font, of, left + WIDTH - font.width(of), y + 4, 0x999999, true);
            }
            int filled = (int) (WIDTH * Math.max(0.0f, Math.min(1.0f, entry.progress())));
            boolean pausedHead = index == 0 && AssemblerQueueView.blocked();
            graphics.fill(left, y + 17, left + WIDTH, y + 18, 0xFF303030);
            graphics.fill(left, y + 17, left + filled, y + 18, pausedHead ? BAR_BLOCKED : BAR);
        }
        if (entries.size() > shown) {
            Component more = Component.translatable(
                    "planetaryfactory_core.assembler.and_more", entries.size() - shown);
            graphics.drawString(font, more, left, top - 12, 0x999999, true);
        }
    }
}
