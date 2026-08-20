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

// SCRAP IS NOT REGISTERED HERE, and cannot be, so do not add it back. A GregTech material
// has to be finished inside GregTech's material window, and every seam a script can reach
// is either hidden or dispatched too late. `Material.Builder.buildAndRegister` carries
// Rhino's `@HideFromJS` and KubeJS hides `BuilderBase.createObject`, so a builder cannot be
// finished by hand; `StartupEvents.registry('gtceu:material', ...)` — the supported API on
// GTCEu 7.0.2, and the 1.20.1 wiki's `GTCEuStartupEvents.registry` does not exist here —
// reaches KubeJS about 340ms after `CommonInit` has closed the registry and generated the
// per-material items, and is rejected with `IllegalStateException: Materials cannot be
// registered in the PostMaterialEvent (or after)`.
//
// That exception used to kill the whole kubejs mod container, which broke resource loading,
// which left FTB Quests with no theme file and crashed it on an empty shape map — a stack
// trace naming a mod with nothing to do with the cause. Mod ordering is not the problem and
// is not worth re-investigating: gtceu declares kubejs `ordering = "AFTER"` and the order is
// already kubejs -> gtceu -> gcyr.
//
// Scrap ships as data instead, read by the GCyR fork from inside the material window:
//
//     kubejs/data/planetaryfactory/gt_materials/scrap.json
//
// Dust and nothing else. An ore variant would put a scrap *ore block* in the registry,
// which is the block a future reader finds and assumes is missing its worldgen — on the
// one body whose defining property is having no ore (ADR-0009). The Bedrock Ore Miner does
// not need one: its drop chain falls through configured-prefix, crushed, gem and ore to
// dust before giving up. The icon set is ROUGH rather than DULL because scrap should read
// as broken debris in an input bus rather than as a processed powder. JSON holds no
// comments, which is why that reasoning lives here.
//
// See ADR-0003 for why the loader belongs to the fork.

// Every Java class this script needs is loaded *inside* the callback that uses it.
// KubeJS startup scripts share one JavaScript scope, so a top-level `const` here is a
// global: worldgen_layers.js already declares ResourceLocation, and a second declaration
// is a redeclaration error that stops whichever script loads second — the failure is
// reported against the innocent script, not this one.
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
