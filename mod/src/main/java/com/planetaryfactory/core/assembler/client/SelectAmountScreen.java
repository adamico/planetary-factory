package com.planetaryfactory.core.assembler.client;

import com.planetaryfactory.core.assembler.SelectAmountMenu;
import com.planetaryfactory.core.network.PlanRequestPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Select Amount: {@code x1}, {@code x5}, {@code all}, and a typed field (ADR-0038 step 3).
 *
 * <p>{@code all} is not computed here. It arrives with the menu, from the resolver, as the largest
 * count whose complete plan the inventory covers -- so {@code all} can never produce a plan that
 * Start then refuses.
 */
public final class SelectAmountScreen extends AssemblerScreen<SelectAmountMenu> {

    private EditBox typed;

    public SelectAmountScreen(SelectAmountMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 80);
    }

    @Override
    protected void init() {
        super.init();
        int y = topPos + 26;
        addRenderableWidget(Button.builder(Component.literal("x1"), b -> request(1))
                .bounds(leftPos + 8, y, 32, 20).build());
        addRenderableWidget(Button.builder(Component.literal("x5"), b -> request(5))
                .bounds(leftPos + 44, y, 32, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("planetaryfactory_core.assembler.all"),
                        b -> request(menu.largestAffordable()))
                .bounds(leftPos + 80, y, 40, 20).build());
        typed = new EditBox(font, leftPos + 8, y + 26, 80, 18,
                Component.translatable("planetaryfactory_core.assembler.amount"));
        typed.setValue(Integer.toString(Math.max(1, menu.amount())));
        typed.setFilter(value -> value.isEmpty() || value.chars().allMatch(Character::isDigit));
        addRenderableWidget(typed);
        addRenderableWidget(Button.builder(Component.translatable("planetaryfactory_core.assembler.show_plan"),
                        b -> request(parsed()))
                .bounds(leftPos + 92, y + 26, 76, 18).build());
    }

    private int parsed() {
        try {
            return Math.max(1, Integer.parseInt(typed.getValue()));
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    /** Plan-request. The answer is the Crafting Plan the server opens next. */
    private void request(int amount) {
        PacketDistributor.sendToServer(new PlanRequestPacket(menu.recipe(), Math.max(1, amount)));
    }

    /** Nothing but the buttons, which are widgets and draw themselves. */
    @Override
    protected void renderPanel(GuiGraphics graphics, int mouseX, int mouseY) {
    }
}
