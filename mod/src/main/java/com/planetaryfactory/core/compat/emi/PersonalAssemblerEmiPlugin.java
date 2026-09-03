package com.planetaryfactory.core.compat.emi;

import com.planetaryfactory.core.PFMenus;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;

/**
 * Registers the Assembler's fill handler against the panel's menu type.
 *
 * <p>A plugin and not a mixin, unlike this package's other resident: {@code addRecipeHandler} is a
 * supported seam that runs unconditionally, where the decorator API behind the lock badge is gated
 * on a config flag that is off for players.
 *
 * <p>{@code EmiRecipeFiller.handlers} is keyed by {@code MenuType}, which is what makes the panel
 * being open the precondition of the only route in (ADR-0038): there is no craft-from-anywhere path
 * because EMI will not offer the button anywhere else.
 */
@EmiEntrypoint
public final class PersonalAssemblerEmiPlugin implements EmiPlugin {

    @Override
    public void register(EmiRegistry registry) {
        registry.addRecipeHandler(PFMenus.ASSEMBLER_PANEL.get(), new PersonalAssemblerEmiHandler());
    }
}
