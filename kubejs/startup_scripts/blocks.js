// Custom blocks registered from KubeJS.
//
// REGISTRATION BOUNDARY (ADR-0015). KubeJS, the `planetaryfactory_core` mod and
// datapack JSON all register into the `planetaryfactory` namespace, so a block ID
// does not tell you which one produced it. The ownership rule is:
//
//   flora and multi-block features -> the mod
//   every other custom block/item  -> here
//   biomes, features, loot, tags   -> datapack JSON
//
// Adding a block here that the mod already registers is a startup crash whose
// message will not mention this file. Check `planetaryfactory_core` first.
//
// The two blocks a pickaxe meets on Electro.
//
// Neither is a GregTech ore block, and that is the point: Electro registers no ore
// veins (ADR-0009), so everything hand-mineable there is a plain block placed by a
// feature or a structure. Both drop through a datapack loot table under
// `kubejs/data/planetaryfactory/loot_table/blocks/`, which names GregTech's material
// dust by tag rather than by item id — GregTech registers material items in code, and
// a tag is the handle that does not depend on guessing its naming scheme.
//
// Both textures are placeholders reusing GCyR's Martian blocks, per the art decision:
// scrap wears regolith, fulgorite wears rock. They are meant to be replaced.

StartupEvents.registry('block', (event) => {
  // Loose rubble in and around the ruins, and the whole of a player's early scrap
  // income. Soft and shovel-mineable on purpose: arriving on Electro with nothing is
  // the situation this block exists to rescue.
  event.create('planetaryfactory:scrap_pile')
    .displayName('Scrap Pile')
    .texture('gcyr:block/mars_regolith')
    .gravelSoundType()
    .hardness(0.6)
    .resistance(0.6)
    .requiresTool(false)
    .tagBlock('minecraft:mineable/shovel');

  // Lightning-fused glass on the barren interior plateaus, and the only hand-mined
  // source of holmium in the pack.
  event.create('planetaryfactory:fulgorite')
    .displayName('Fulgorite')
    .texture('gcyr:block/martian_rock')
    .glassSoundType()
    .hardness(1.5)
    .resistance(1.5)
    .requiresTool(false)
    .tagBlock('minecraft:mineable/pickaxe');
});

// Sapros's two trees, minus the two saplings: those are `planetaryfactory_core`'s, because
// a sapling is a `SaplingBlock` backed by a `TreeGrower` and no scripting API here exposes
// one (ADR-0014). Everything else the trees are made of is an ordinary block, so it is here.
//
// The trees' shapes are not here either. Each is one `minecraft:tree` configured feature under
// `kubejs/data/planetaryfactory/worldgen/configured_feature/`, placed by worldgen and grown by
// the sapling from that same definition, so a farmed tree cannot differ from a wild one.
//
// `minecraft:logs` and `minecraft:leaves` are what make Create's saw fell these trees and its
// Deployer treat them as a canopy. They are load-bearing integration, not decoration.

// Both harvests are destructive: a tree yields once and is felled doing it, then replanted
// from a sapling. Yumako's fruit is in the canopy and Jellynut is in the trunk, so the two
// still come off different blocks -- but neither tree is a standing crop you return to.
StartupEvents.registry('block', (event) => {
  event.create('planetaryfactory:yumako_log')
    .displayName('Yumako Log')
    .texture('planetaryfactory:block/yumako_log')
    .soundType('wood')
    .hardness(2.0)
    .resistance(2.0)
    .requiresTool(false)
    .tagBlock('minecraft:logs')
    .tagBlock('minecraft:logs_that_burn')
    .tagBlock('minecraft:mineable/axe')
    .tagItem('minecraft:logs')
    .tagItem('minecraft:logs_that_burn');

  event.create('planetaryfactory:yumako_leaves')
    .displayName('Yumako Leaves')
    .texture('planetaryfactory:block/yumako_leaves')
    .soundType('grass')
    .hardness(0.2)
    .resistance(0.2)
    .requiresTool(false)
    .notSolid()
    .renderType('cutout_mipped')
    .tagBlock('minecraft:leaves')
    .tagBlock('minecraft:mineable/hoe')
    .tagItem('minecraft:leaves');

  event.create('planetaryfactory:jellystem_stem')
    .displayName('Jellystem Stem')
    .texture('planetaryfactory:block/jellystem_stem')
    .soundType('wood')
    .hardness(1.5)
    .resistance(1.5)
    .requiresTool(false)
    .tagBlock('minecraft:logs')
    .tagBlock('minecraft:logs_that_burn')
    .tagBlock('minecraft:mineable/axe')
    .tagItem('minecraft:logs')
    .tagItem('minecraft:logs_that_burn');

  event.create('planetaryfactory:jellystem_leaves')
    .displayName('Jellystem Leaves')
    .texture('planetaryfactory:block/jellystem_leaves')
    .soundType('grass')
    .hardness(0.2)
    .resistance(0.2)
    .requiresTool(false)
    .notSolid()
    .renderType('cutout_mipped')
    .tagBlock('minecraft:leaves')
    .tagBlock('minecraft:mineable/hoe')
    .tagItem('minecraft:leaves');
});
