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

// Every Java class this script needs is loaded *inside* the callback that uses it.
// KubeJS startup scripts share one JavaScript scope, so a top-level `const` here is a
// global: worldgen_layers.js already declares ResourceLocation, and a second declaration
// is a redeclaration error that stops whichever script loads second — the failure is
// reported against the innocent script, not this one.
//
// `StartupEvents.registry('gtceu:material', ...)` is the supported API on GTCEu 7.0.2 and
// the only one that works: a material can only be finished from inside this event.
// GregTech hides `Material.Builder.buildAndRegister` from scripts with `@HideFromJS`, and
// KubeJS hides `BuilderBase.createObject`, so there is no way to build one earlier by
// hand. The 1.20.1 wiki's `GTCEuStartupEvents.registry('gtceu:material', ...)` does not
// exist here either — 7.0.2's GTCEuStartupEvents group offers only materialIconInfo,
// worldGenLayers, materialModification and craftingComponents.
//
// KNOWN DEFECT: in this pack that event currently fires too late. GregTech closes its
// material registry in `CommonInit.onRegisterEarly` — `MaterialRegistry.close()` followed
// by `PostMaterialEvent` — and that close lands before KubeJS's turn on the
// `gtceu:material` RegisterEvent, so this registration is rejected with
// `IllegalStateException: Materials cannot be registered in the PostMaterialEvent (or
// after)`. That exception kills the whole kubejs mod container, which breaks resource
// loading, which leaves FTB Quests with no theme file and crashes it on an empty shape
// map — a stack trace that points at a mod with nothing to do with the cause. GregTech's
// own mods.toml declares kubejs with `ordering = "AFTER"`, so KubeJS is supposed to get
// this event first; the fix belongs at that ordering, not in this script.
StartupEvents.registry('gtceu:material', (event) => {
  const MaterialIconSet = Java.loadClass(
    'com.gregtechceu.gtceu.api.material.material.info.MaterialIconSet');

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
  const DustProperty = Java.loadClass(
    'com.gregtechceu.gtceu.api.material.material.properties.DustProperty');

  // Fulgorite is hand-mined for holmium, so holmium needs something to drop as.
  // Guarded because a future GregTech release giving holmium its own dust would
  // otherwise turn this line into a crash on a mod update.
  //
  // GTMaterials and PropertyKey are KubeJS global bindings from GTKubeJSPlugin, so they
  // are used directly — declaring either here is the redeclaration error described above.
  if (!GTMaterials.Holmium.hasProperty(PropertyKey.DUST)) {
    GTMaterials.Holmium.setProperty(PropertyKey.DUST, new DustProperty());
  }
});
