// The two material-level facts Electro needs: scrap, which does not exist anywhere in
// the modpack, and holmium, which exists in GregTech as a name with nothing attached.
//
// Scrap is tier 3 of the substitution policy (docs/gdd.md §9) — a custom material, only
// where the puzzle depends on the thing existing separately from anything that already
// exists. It qualifies on the narrowest reading: scrap's entire role is to be one
// distinct input that recycles into a spread of unrelated outputs, and no GregTech,
// Mekanism or Create material plays that part. Tiers 1 and 2 were checked first and
// have nothing to offer here.
//
// Holmium is the opposite case, and the policy resolves it the other way: GregTech
// already has `gtceu:holmium`, so tier 1 applies and this pack must not register its
// own. What GregTech does *not* give it is any item form at all — no dust, no ingot,
// no ore, just a colour, an icon set and its element — so the material is extended
// rather than replaced.

const PropertyKey = Java.loadClass(
  'com.gregtechceu.gtceu.api.material.material.properties.PropertyKey');
const DustProperty = Java.loadClass(
  'com.gregtechceu.gtceu.api.material.material.properties.DustProperty');
const MaterialIconSet = Java.loadClass(
  'com.gregtechceu.gtceu.api.material.material.info.MaterialIconSet');
// GTMaterials is already a KubeJS global binding; declaring it here is a redeclaration
// error that takes the whole startup script down with it.

StartupEvents.registry('gtceu:material', (event) => {
  // Dust and nothing else. An ore variant would put a scrap *ore block* in the
  // registry, which is the block a future reader finds and assumes is missing its
  // worldgen — on the one body whose defining property is having no ore (ADR-0009).
  // The Bedrock Ore Miner does not need one: its drop chain falls through
  // configured-prefix, crushed, gem and ore to dust before giving up.
  event.create('planetaryfactory:scrap')
    .dust()
    .color(0x8A6A4F)
    .secondaryColor(0x4E3A2A)
    // Rough, not dull: scrap should read as broken debris in an input bus rather
    // than as a processed powder.
    .iconSet(MaterialIconSet.ROUGH);
});

GTCEuStartupEvents.materialModification(() => {
  // Fulgorite is hand-mined for holmium, so holmium needs something to drop as.
  // Guarded because a future GregTech release giving holmium its own dust would
  // otherwise turn this line into a crash on a mod update.
  if (!GTMaterials.Holmium.hasProperty(PropertyKey.DUST)) {
    GTMaterials.Holmium.setProperty(PropertyKey.DUST, new DustProperty());
  }
});
