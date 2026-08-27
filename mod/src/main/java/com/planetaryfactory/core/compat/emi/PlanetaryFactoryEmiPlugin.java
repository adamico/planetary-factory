package com.planetaryfactory.core.compat.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;

/**
 * The pack's EMI plugin. Loaded by EMI's own annotation scan, so nothing in the mod references this
 * class and it never loads when EMI is absent.
 */
@EmiEntrypoint
public final class PlanetaryFactoryEmiPlugin implements EmiPlugin {

    @Override
    public void register(EmiRegistry registry) {
        registry.addRecipeDecorator(new LockedRecipeEmiDecorator());
    }
}
