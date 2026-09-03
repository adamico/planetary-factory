package com.planetaryfactory.core.assembler.client;

import com.planetaryfactory.core.assembler.CraftingPlanMenu;
import com.planetaryfactory.core.assembler.ItemAmount;
import com.planetaryfactory.core.assembler.PlanDisplay;
import com.planetaryfactory.core.network.OpenPanelPacket;
import com.planetaryfactory.core.network.PlanStartPacket;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Crafting Plan: the flattened tree in three categories, and one commitment (ADR-0038 steps 4
 * and 5).
 *
 * <p>{@code Locked} is its own column beside {@code Missing} because the two ask different things of
 * the player: research one, mine the other.
 *
 * <p>What fills the three lists is #161's. This screen is the frame, and it is already honest about
 * the empty case -- Start is refused on any incomplete plan, which is the state every plan is in
 * until the resolver lands.
 */
public final class CraftingPlanScreen extends AssemblerScreen<CraftingPlanMenu> {

    private static final int COLUMN = 3;

    public CraftingPlanScreen(CraftingPlanMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 260, 160);
    }

    @Override
    protected void init() {
        super.init();
        Button start = Button.builder(Component.translatable("planetaryfactory_core.assembler.start"),
                        b -> PacketDistributor.sendToServer(new PlanStartPacket(menu.display().planId())))
                .bounds(leftPos + imageWidth - 150, topPos + imageHeight - 26, 70, 20).build();
        start.active = menu.display().complete();
        addRenderableWidget(start);
        addRenderableWidget(Button.builder(Component.translatable("planetaryfactory_core.assembler.back"),
                        b -> PacketDistributor.sendToServer(new OpenPanelPacket()))
                .bounds(leftPos + imageWidth - 76, topPos + imageHeight - 26, 70, 20).build());
    }

    @Override
    protected void renderPanel(GuiGraphics graphics, int mouseX, int mouseY) {
        PlanDisplay display = menu.display();
        int width = (imageWidth - 16) / COLUMN;
        column(graphics, leftPos + 8, "to_craft", display.toCraft(), ChatFormatting.WHITE, width);
        column(graphics, leftPos + 8 + width, "missing", display.missing(), ChatFormatting.RED, width);
        column(graphics, leftPos + 8 + 2 * width, "locked", display.locked(), ChatFormatting.GOLD, width);
        if (!display.complete()) {
            // Its own line above the buttons, not beside them: the reason a plan cannot start is a
            // translated sentence of unknown width, and sharing the row with Start and Back means
            // the longest translation is the one that gets cut in half.
            graphics.drawString(font,
                    Component.translatable("planetaryfactory_core.assembler.incomplete")
                            .withStyle(ChatFormatting.RED),
                    leftPos + 8, topPos + imageHeight - 38, 0xFF5555, false);
        }
    }

    private void column(GuiGraphics graphics, int x, String key, List<ItemAmount> amounts, ChatFormatting colour, int width) {
        graphics.drawString(font,
                Component.translatable("planetaryfactory_core.assembler." + key).withStyle(colour),
                x, topPos + 22, 0xFFFFFF, false);
        int y = topPos + 34;
        for (ItemAmount amount : amounts) {
            graphics.drawString(font, amount.count() + " " + amount.item(), x, y, 0xCCCCCC, false);
            y += 10;
        }
    }
}
