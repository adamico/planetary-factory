package com.planetaryfactory.core.machine;

import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import com.gregtechceu.gtceu.api.capability.recipe.CWURecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.gui.editor.EditableMachineUI;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.SimpleTieredMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.ui.GTRecipeTypeUI;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;

/**
 * The chassis under Terra's three Assembling Machines (ADR-0026).
 *
 * <p>ADR-0026 authored the GUI at recipe-type level only and said in as many words that a
 * {@code MetaMachine} subclass was not worth writing — with the escape hatch that if the layout
 * still read wrong in game, a follow-up with evidence behind it was cheap. This is that follow-up.
 * Two of the three things wrong with the layout are unreachable from a recipe type, because
 * {@link SimpleTieredMachine} hardcodes them:
 *
 * <ul>
 *   <li>the programmed-circuit configurator, attached whenever {@code isCircuitSlotEnabled()} is
 *       true, which the interface's default implementation returns unconditionally; and</li>
 *   <li>the charger slot, welded into {@code SimpleTieredMachine.EDITABLE_UI_CREATOR} rather than
 *       taken from the recipe type.</li>
 * </ul>
 *
 * <p>Neither belongs on a Factorio assembling machine. There is no circuit anywhere in this pack's
 * assembling corpus — the recipes are generated from Factorio's own prototypes — so the
 * configurator opens onto a setting no recipe reads, and machines here are wired, never
 * battery-fed. This class is the whole answer: one overridden predicate and one UI that is
 * GregTech's own minus the battery slot. It overrides no recipe logic, so nothing here has to
 * track GregTech's internals beyond the two members it names.
 */
public class AssemblingMachine extends SimpleTieredMachine {

    public AssemblingMachine(IMachineBlockEntity holder, int tier, Int2IntFunction tankScalingFunction) {
        super(holder, tier, tankScalingFunction);
    }

    /**
     * No programmed-circuit configurator. The default is {@code true} for every GregTech machine,
     * and it is the only thing gating {@code attachConfigurators}' circuit panel.
     */
    @Override
    public boolean isCircuitSlotEnabled() {
        return false;
    }

    /**
     * GregTech's {@code SimpleTieredMachine} UI without the charger slot.
     *
     * <p>Deliberately a copy of {@code SimpleTieredMachine.EDITABLE_UI_CREATOR} rather than a
     * wrapper around it: that field builds the recipe-type template, builds the battery slot, and
     * puts both into an outer group padded to 78px tall to make room for the slot. There is no seam
     * to remove the slot from afterwards, so the template is taken straight from the recipe type
     * and the padding goes with the slot it existed for.
     *
     * <p>{@code chargerInventory} itself is left alone. It is a plain handler rather than a machine
     * trait, so it is exposed to no capability and no pipe: without a widget it is unreachable, and
     * leaving it in place keeps {@code chargeBattery} and {@code updateBatterySubscription} — both
     * of which index slot 0 unguarded — reading a slot that exists.
     */
    public static EditableMachineUI editableUI(ResourceLocation id, GTRecipeType recipeType) {
        return new EditableMachineUI(
                "simple", id,
                () -> (WidgetGroup) recipeType.getRecipeUI()
                        .createEditableUITemplate(false, false).createDefault(),
                (group, machine) -> {
                    if (!(machine instanceof AssemblingMachine assembling)) {
                        return;
                    }
                    Table<IO, RecipeCapability<?>, Object> storages = Tables.newCustomTable(
                            new EnumMap<>(IO.class), HashMap::new);
                    storages.put(IO.IN, ItemRecipeCapability.CAP, assembling.importItems.storage);
                    storages.put(IO.OUT, ItemRecipeCapability.CAP, assembling.exportItems.storage);
                    storages.put(IO.IN, FluidRecipeCapability.CAP, assembling.importFluids);
                    storages.put(IO.OUT, FluidRecipeCapability.CAP, assembling.exportFluids);
                    storages.put(IO.IN, CWURecipeCapability.CAP, assembling.importComputation);
                    storages.put(IO.OUT, CWURecipeCapability.CAP, assembling.exportComputation);
                    assembling.getRecipeType().getRecipeUI()
                            .createEditableUITemplate(false, false)
                            .setupUI(group, new GTRecipeTypeUI.RecipeHolder(
                                    assembling.recipeLogic::getProgressPercent,
                                    storages,
                                    new CompoundTag(),
                                    Collections.emptyList(),
                                    false, false));
                });
    }
}
