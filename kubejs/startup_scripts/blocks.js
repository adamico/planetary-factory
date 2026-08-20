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
    .textureAll('gcyr:block/mars_regolith')
    .soundType('gravel')
    .hardness(0.6)
    .resistance(0.6)
    .requiresTool(false)
    .tagBlock('minecraft:mineable/shovel');

  // Lightning-fused glass on the barren interior plateaus, and the only hand-mined
  // source of holmium in the pack.
  event.create('planetaryfactory:fulgorite')
    .displayName('Fulgorite')
    .textureAll('gcyr:block/martian_rock')
    .soundType('glass')
    .hardness(1.5)
    .resistance(1.5)
    .requiresTool(false)
    .tagBlock('minecraft:mineable/pickaxe');
});
