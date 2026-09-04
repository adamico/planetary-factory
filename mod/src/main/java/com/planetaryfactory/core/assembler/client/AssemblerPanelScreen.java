package com.planetaryfactory.core.assembler.client;

import com.planetaryfactory.core.assembler.AssemblerPanelMenu;
import com.planetaryfactory.core.assembler.AssemblerQueueView;
import com.planetaryfactory.core.network.PlanCancelPacket;
import com.planetaryfactory.core.network.QueueSyncPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
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

    private static final int ROW_HEIGHT = 22;
    private static final int ROWS_TOP = 20;
    private static final int CANCEL_SIZE = 12;
    private static final int BAR = 0xFF4FA84F;
    private static final int BAR_BLOCKED = 0xFFB05030;

    public AssemblerPanelScreen(AssemblerPanelMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 186);
    }

    /** The title, plus which key leaves for the inventory -- a way back nothing else advertises. */
    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        if (minecraft == null) return;
        Component hint = Component.translatable("planetaryfactory_core.assembler.to_inventory",
                minecraft.options.keyInventory.getTranslatedKeyMessage());
        graphics.drawString(font, hint, imageWidth - 6 - font.width(hint), 6, 0x808080, false);
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
            // The plan on the left, the step under way on the right. A row naming only the plan
            // says nothing is happening for as long as a transport belt spends crafting iron gears,
            // which is most of its life.
            int textY = y + 5;
            graphics.renderItem(itemStack(entry.rootItem()), leftPos + 9, y + 2);
            int after = leftPos + 27;
            graphics.drawString(font, "x" + entry.amount(), after, textY, 0xFFFFFF, false);
            after += font.width("x" + entry.amount()) + 6;
            if (entry.hasStep()) {
                graphics.drawString(font, ">", after, textY, 0x777777, false);
                graphics.renderItem(itemStack(entry.stepItem()), after + 8, y + 2);
                graphics.drawString(font, "x" + entry.stepAmount(), after + 26, textY, 0xCCCCCC, false);
            }
            if (entry.steps() > 1) {
                Component of = Component.literal((entry.step() + 1) + "/" + entry.steps());
                graphics.drawString(font, of, cancelLeft() - font.width(of) - 4, textY, 0x999999, false);
            }
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

    /**
     * The inventory key goes to the inventory, not to the world.
     *
     * <p>The panel is opened from the inventory screen and stands in for it while it is up, so
     * vanilla's "the inventory key closes this" left a player who wanted their inventory back
     * pressing it twice, with a frame of world in between. Escape is untouched and still leaves for
     * the game, which is what every other screen does with it.
     */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (minecraft != null && minecraft.player != null
                && minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            onClose();
            minecraft.setScreen(new InventoryScreen(minecraft.player));
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
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
