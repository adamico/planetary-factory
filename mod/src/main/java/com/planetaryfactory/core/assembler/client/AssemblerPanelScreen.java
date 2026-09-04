package com.planetaryfactory.core.assembler.client;

import com.planetaryfactory.core.assembler.AssemblerPanelMenu;
import com.planetaryfactory.core.assembler.AssemblerQueueView;
import com.planetaryfactory.core.network.PlanCancelPacket;
import com.planetaryfactory.core.network.QueueSyncPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Personal Assembler panel: the queue above, the player's own inventory below.
 *
 * <p>Vanilla's {@code AbstractContainerScreen}, not FTB Library -- that is All Rights Reserved and
 * behind a CLA -- and Java rather than KubeJS, which cannot bind a screen on 1.21.1 at all (#96).
 *
 * <p>Each queued plan gets a row and a {@code x} to cancel it. Only the {@code x} cancels, and only
 * on a left click: the plan is the unit of cancellation (ADR-0038), so a stray click anywhere on the
 * row would refund a plan the player was only reading.
 */
public final class AssemblerPanelScreen extends AssemblerScreen<AssemblerPanelMenu> {

    private static final int ROW_HEIGHT = 20;
    private static final int ROWS_TOP = 20;
    private static final int CANCEL_SIZE = 12;
    private static final int BAR = 0xFF4FA84F;
    private static final int BAR_BLOCKED = 0xFFB05030;

    public AssemblerPanelScreen(AssemblerPanelMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 186);
    }

    @Override
    protected void renderPanel(GuiGraphics graphics, int mouseX, int mouseY) {
        renderSlots(graphics);
        int y = topPos + ROWS_TOP;
        int index = 0;
        for (QueueSyncPacket.Entry entry : AssemblerQueueView.entries()) {
            graphics.fill(leftPos + 6, y, leftPos + imageWidth - 6, y + ROW_HEIGHT - 2, ROW);
            int trackWidth = imageWidth - 16 - CANCEL_SIZE - 2;
            int barWidth = (int) (trackWidth * Math.max(0.0f, Math.min(1.0f, entry.progress())));
            boolean isPausedHead = index == 0 && AssemblerQueueView.blocked();
            graphics.fill(leftPos + 8, y + ROW_HEIGHT - 6, leftPos + 8 + barWidth, y + ROW_HEIGHT - 4,
                    isPausedHead ? BAR_BLOCKED : BAR);
            graphics.drawString(font,
                    itemName(entry.rootItem()).copy().append(" x" + entry.amount()),
                    leftPos + 10, y + 2, 0xFFFFFF, false);
            int cancelX = cancelLeft();
            graphics.fill(cancelX, y + 2, cancelX + CANCEL_SIZE, y + 2 + CANCEL_SIZE, 0xFF5A2B2B);
            graphics.drawString(font, "x", cancelX + 4, y + 4, 0xFFDDDD, false);
            y += ROW_HEIGHT;
            index++;
        }
        if (AssemblerQueueView.entries().isEmpty()) {
            graphics.drawString(font,
                    Component.translatable("planetaryfactory_core.assembler.queue_empty").withStyle(ChatFormatting.GRAY),
                    leftPos + 10, topPos + ROWS_TOP + 4, 0xAAAAAA, false);
        } else if (AssemblerQueueView.blocked()) {
            graphics.drawString(font,
                    Component.translatable("planetaryfactory_core.assembler.paused").withStyle(ChatFormatting.GOLD),
                    leftPos + 8, topPos + AssemblerPanelMenu.PLAYER_INVENTORY_Y - 12, 0xFFAA00, false);
        }
    }

    private int cancelLeft() {
        return leftPos + imageWidth - 6 - CANCEL_SIZE - 2;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int index = (int) ((mouseY - topPos - ROWS_TOP) / ROW_HEIGHT);
            int rowTop = topPos + ROWS_TOP + index * ROW_HEIGHT;
            boolean onCancel = mouseX >= cancelLeft() && mouseX <= cancelLeft() + CANCEL_SIZE
                    && mouseY >= rowTop + 2 && mouseY <= rowTop + 2 + CANCEL_SIZE;
            if (onCancel && index >= 0 && index < AssemblerQueueView.entries().size()) {
                PacketDistributor.sendToServer(
                        new PlanCancelPacket(AssemblerQueueView.entries().get(index).planId()));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
