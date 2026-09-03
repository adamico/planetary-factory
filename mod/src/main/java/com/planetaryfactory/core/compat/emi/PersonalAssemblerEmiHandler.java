package com.planetaryfactory.core.compat.emi;

import com.planetaryfactory.core.assembler.AssemblerPanelMenu;
import com.planetaryfactory.core.network.SelectAmountPacket;
import dev.emi.emi.api.recipe.EmiPlayerInventory;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.api.recipe.handler.EmiRecipeHandler;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * EMI's {@code + Fill Recipe}, pointed at the Personal Assembler (ADR-0038, #160).
 *
 * <p><b>{@link EmiRecipeHandler} directly, never {@code StandardRecipeHandler}.</b> That is the whole
 * point of this class. A standard handler moves ingredients into a crafting grid, and its default
 * {@code canCraft} checks the player's inventory against the recipe -- so EMI greys the button out
 * when an ingredient is missing. We have no grid and move nothing, and a missing ingredient is
 * exactly the case the Crafting Plan exists to name. Greying the button there would hide the answer
 * behind the question.
 *
 * <p>Verified against {@code emi-1.1.24+1.21.1+neoforge.jar} rather than assumed:
 * {@code RecipeFillButtonWidget} sets {@code canFill = supportsRecipe(recipe) && canCraft(recipe,
 * ctx)} and that single field drives both the greyed texture and the click gate. {@code canCraft} is
 * ours, so returning true unconditionally keeps the button lit with the ingredients absent.
 *
 * <p>{@code craft()} sends and returns. It opens no screen, because {@code
 * EmiRecipeFiller.performFill} calls {@code Minecraft.setScreen(handledScreen)} the moment it
 * returns true -- anything opened synchronously here loses that race. The server opens the dialog
 * instead, which is what the ADR wanted anyway for reasons that have nothing to do with EMI.
 */
public final class PersonalAssemblerEmiHandler implements EmiRecipeHandler<AssemblerPanelMenu> {

    /**
     * The player's stacks, built here rather than asked for.
     *
     * <p>{@code EmiPlayerInventory.of(player)} would be the obvious call and is a stack overflow:
     * read out of the jar, {@code of} is EMI's <em>dispatcher</em> -- it looks up the handlers
     * registered for the currently open screen and returns {@code handlers.get(0).getInventory(...)},
     * which is this method. It only reaches {@code new EmiPlayerInventory(player)} when no handler is
     * registered at all, so it works everywhere except inside a handler.
     *
     * <p>It crashes the moment the panel opens, not on a button press: EMI builds the inventory to
     * work out what is craftable as soon as a screen with a handler comes up.
     */
    @Override
    public EmiPlayerInventory getInventory(AbstractContainerScreen<AssemblerPanelMenu> screen) {
        Player player = Minecraft.getInstance().player;
        return player == null ? new EmiPlayerInventory(List.of()) : new EmiPlayerInventory(player);
    }

    /**
     * Any recipe EMI can name.
     *
     * <p>Deliberately wide, and the width is the ticket's. The hand-craftable set -- first category
     * {@code crafting}, minus the eleven Factorio withholds (#88) -- is a fact about the Factorio
     * corpus, which lives on the server; a client-side answer would need that set synced, and the
     * thing that knows it is #161's resolver. Until then the refusal happens one screen later and
     * says more: a recipe the Assembler cannot make comes back as an incomplete plan naming what is
     * wrong, rather than as a button that is silently absent.
     *
     * <p>An id is all the server needs to be asked about a recipe, so that is all this checks.
     */
    @Override
    public boolean supportsRecipe(EmiRecipe recipe) {
        return recipe.getId() != null;
    }

    /**
     * Always, and this is the non-standard half of the contract.
     *
     * <p>The button must stay enabled when the player lacks the ingredients, because showing what is
     * missing is the Crafting Plan's entire job.
     */
    @Override
    public boolean canCraft(EmiRecipe recipe, EmiCraftContext<AssemblerPanelMenu> context) {
        return true;
    }

    /**
     * Asks the server for Select Amount and gets out of EMI's way.
     *
     * <p>{@code context.getAmount()} is {@code 1} on a click and {@code Integer.MAX_VALUE} on a
     * shift-click -- Factorio's one-and-all, arriving for free.
     */
    @Override
    public boolean craft(EmiRecipe recipe, EmiCraftContext<AssemblerPanelMenu> context) {
        ResourceLocation id = recipe.getId();
        if (id == null) return false;
        PacketDistributor.sendToServer(new SelectAmountPacket(id, context.getAmount()));
        return true;
    }
}
