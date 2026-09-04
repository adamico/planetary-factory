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
 * The Crafting Plan: what the plan spends, what it makes, what it cannot get, and one commitment
 * (ADR-0038 steps 4 and 5).
 *
 * <p>{@code Locked} is its own column beside {@code Missing} because the two ask different things of
 * the player: research one, mine the other.
 *
 * <p>What fills the three lists is #161's. This screen is the frame, and it is already honest about
 * the empty case -- Start is refused on any incomplete plan, which is the state every plan is in
 * until the resolver lands.
 */
public final class CraftingPlanScreen extends AssemblerScreen<CraftingPlanMenu> {

    /**
     * Consume, To Craft, Missing, Locked.
     *
     * <p>Consume comes first because it is what Start spends. ADR-0038 has Start pay the whole raw
     * cost in one go, so the list of what leaves the inventory is the one the player is actually
     * being asked to agree to -- reading it after the list of what arrives puts the price after the
     * purchase.
     */
    private static final int COLUMN = 4;

    /**
     * How many lines a column shows before it says how many it did not.
     *
     * <p>A flattened plan can be long and the dialog does not scroll. Truncating with a count is the
     * honest failure: a column that silently stopped would tell a player they have everything.
     */
    private static final int MAX_LINES = 6;

    public CraftingPlanScreen(CraftingPlanMenu menu, Inventory inventory, Component title) {
        // Wider than the other two dialogs: four columns of item names at 3-space-per-character do
        // not fit in the panel's width, and a name that elides is a name the player cannot shop for.
        super(menu, inventory, title, 340, 160);
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
        column(graphics, leftPos + 8, "consume", display.consume(), ChatFormatting.AQUA, width);
        column(graphics, leftPos + 8 + width, "to_craft", display.toCraft(), ChatFormatting.WHITE, width);
        column(graphics, leftPos + 8 + 2 * width, "missing", display.missing(), ChatFormatting.RED, width);
        column(graphics, leftPos + 8 + 3 * width, "locked", display.locked(), ChatFormatting.GOLD, width);
        if (!display.complete()) {
            // Its own line above the buttons, not beside them: the reason a plan cannot start is a
            // translated sentence of unknown width, and sharing the row with Start and Back means
            // the longest translation is the one that gets cut in half.
            //
            // Three empty columns is not "you are short of something" -- it is the resolver saying
            // it could not read the recipe at all, which happens to one carrying a tag ingredient,
            // a fluid or a chanced output. Telling the player they are missing nothing while
            // refusing to start would be the worst of both.
            boolean nothingToShow = display.consume().isEmpty()
                    && display.toCraft().isEmpty()
                    && display.missing().isEmpty()
                    && display.locked().isEmpty();
            graphics.drawString(font,
                    Component.translatable(nothingToShow
                                    ? "planetaryfactory_core.assembler.unplannable"
                                    : "planetaryfactory_core.assembler.incomplete")
                            .withStyle(ChatFormatting.RED),
                    leftPos + 8, topPos + imageHeight - 38, 0xFF5555, false);
        }
    }

    private void column(GuiGraphics graphics, int x, String key, List<ItemAmount> amounts, ChatFormatting colour, int width) {
        graphics.drawString(font,
                Component.translatable("planetaryfactory_core.assembler." + key).withStyle(colour),
                x, topPos + 22, 0xFFFFFF, false);
        int y = topPos + 34;
        for (ItemAmount amount : amounts.subList(0, Math.min(MAX_LINES, amounts.size()))) {
            graphics.drawString(font,
                    Component.literal(amount.count() + " ").append(itemName(amount.item())),
                    x, y, 0xCCCCCC, false);
            y += 10;
        }
        if (amounts.size() > MAX_LINES) {
            graphics.drawString(font,
                    Component.translatable("planetaryfactory_core.assembler.and_more",
                            amounts.size() - MAX_LINES),
                    x, y, 0x888888, false);
        }
    }
}
