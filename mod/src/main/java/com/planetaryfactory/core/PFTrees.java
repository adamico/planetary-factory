package com.planetaryfactory.core;

import java.util.Optional;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

/**
 * The growers for Sapros's two trees.
 *
 * <p>Each names a configured feature and nothing else. The tree's shape lives in
 * {@code kubejs/data/planetaryfactory/worldgen/configured_feature/}, and that single definition is
 * what both worldgen and a bonemealed sapling place -- so a farmed tree cannot drift from a wild one
 * (ADR-0015). If the JSON is missing, the sapling simply fails to grow; it does not crash.
 */
public final class PFTrees {
    public static final TreeGrower YUMAKO = grower("yumako");
    public static final TreeGrower JELLYSTEM = grower("jellystem");

    private PFTrees() {
    }

    private static TreeGrower grower(String name) {
        return new TreeGrower(
                PlanetaryFactoryCore.NAMESPACE + ":" + name,
                Optional.empty(),
                Optional.of(feature(name + "_tree")),
                Optional.empty());
    }

    private static ResourceKey<ConfiguredFeature<?, ?>> feature(String path) {
        return ResourceKey.create(
                Registries.CONFIGURED_FEATURE,
                ResourceLocation.fromNamespaceAndPath(PlanetaryFactoryCore.NAMESPACE, path));
    }
}
