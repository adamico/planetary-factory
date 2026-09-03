package com.planetaryfactory.core;

import com.planetaryfactory.core.assembler.AssemblerPanelMenu;
import com.planetaryfactory.core.assembler.CraftingPlanMenu;
import com.planetaryfactory.core.assembler.SelectAmountMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;

/**
 * The Personal Assembler's three menus (ADR-0038).
 *
 * <p>Three and not one. The panel is the surface EMI's Fill Recipe keys on; Select Amount and the
 * Crafting Plan are dialogs the <em>server</em> opens, because a plan is server truth -- it reads the
 * inventory and the team's research, and Start takes the reservation off the back of it.
 *
 * <p>All Java, and this is ADR-0015's split at its sharpest: KubeJS cannot register a {@code
 * MenuType} or a {@code Screen} on 1.21.1 at all (#96).
 */
public final class PFMenus {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, PlanetaryFactoryCore.NAMESPACE);

    /** No opening data: the panel shows the queue, and the queue is synced separately. */
    public static final Supplier<MenuType<AssemblerPanelMenu>> ASSEMBLER_PANEL =
            MENUS.register("assembler_panel", () -> new MenuType<>(AssemblerPanelMenu::new, net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));

    public static final Supplier<MenuType<SelectAmountMenu>> SELECT_AMOUNT =
            MENUS.register("assembler_select_amount", () -> IMenuTypeExtension.create(SelectAmountMenu::new));

    public static final Supplier<MenuType<CraftingPlanMenu>> CRAFTING_PLAN =
            MENUS.register("assembler_crafting_plan", () -> IMenuTypeExtension.create(CraftingPlanMenu::new));

    private PFMenus() {
    }

    static void register(IEventBus modBus) {
        MENUS.register(modBus);
    }
}
