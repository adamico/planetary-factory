// One GregTech worldgen layer per body, and the dimension marker that makes the body
// visible to GregTech's prospecting and vein tooling.
//
// A GregTech ore vein places ore by *material*, not by block: the block that ends up
// in the ground is the ore variant registered for the stone its layer matches. So a
// body needs a layer of its own or it generates nothing. GregTech's built-in `stone`,
// `deepslate`, `netherrack` and `endstone` layers are bound to vanilla levels and
// never fire on a pack dimension.
//
// Layers cannot be datapack files — they are code objects, and the only handle on
// them outside a mod is this KubeJS startup event. Each body adds one entry to BODIES
// below, matching the stone its noise settings put in the ground.
//
// Ignus's layer is deliberately not GCyR's `venus` layer. That one still exists and
// is scoped to `gcyr:venus`, a dimension this pack no longer offers as a planet.
// See ADR-0008 for why the stone underneath is still GCyR's.
const BlockMatchTest = Java.loadClass(
  'net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest');
const Registries = Java.loadClass('net.minecraft.core.registries.Registries');
const ResourceKey = Java.loadClass('net.minecraft.resources.ResourceKey');
const ResourceLocation = Java.loadClass('net.minecraft.resources.ResourceLocation');
const BuiltInRegistries = Java.loadClass('net.minecraft.core.registries.BuiltInRegistries');
const DimensionMarker = Java.loadClass('com.gregtechceu.gtceu.api.worldgen.DimensionMarker');

const BODIES = [
  {
    layer: 'ignus_rock',
    stone: 'gcyr:venus_rock',
    dimension: 'planetaryfactory:vulcanus',
    // The tier the prospecting tooling sorts this body under: Ignus is a tier-2
    // rocket destination, as its planet definition says.
    tier: 2,
    icon: 'gcyr:venus_rock',
    name: 'level.planetaryfactory.vulcanus',
  },
  {
    // Electro registers no ore veins at all (ADR-0009), so nothing this layer
    // matches ever places an ore. It exists anyway: the layer is what gives the
    // body a tab in GregTech's prospecting tooling, and a player who prospects
    // Electro and is told "nothing here" has learned the design. A body with no
    // layer would instead be told nothing at all, which reads as a bug.
    layer: 'electro_rock',
    stone: 'gcyr:martian_rock',
    dimension: 'planetaryfactory:fulgora',
    tier: 2,
    icon: 'gcyr:martian_rock',
    name: 'level.planetaryfactory.fulgora',
  },
  {
    // Sapros registers no ore veins either (ADR-0016), and for a different reason than
    // Electro's: Electro has no metal at all, where Sapros has metal that arrives by
    // Decaying a bacterium rather than by coming out of the ground. The layer is here for
    // the same reason Electro's is -- a prospecting tab that says "nothing here" teaches
    // the design, where no tab at all reads as a bug.
    layer: 'sapros_rock',
    stone: 'gcyr:mercury_rock',
    dimension: 'planetaryfactory:gleba',
    tier: 3,
    icon: 'gcyr:mercury_rock',
    name: 'level.planetaryfactory.gleba',
  },
];

const levelKey = (id) => ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(id));

GTCEuStartupEvents.worldGenLayers((event) => {
  BODIES.forEach((body) => {
    event.create(body.layer, (builder) => {
      builder.targets(() => new BlockMatchTest(BuiltInRegistries.BLOCK.get(ResourceLocation.parse(body.stone))));
      builder.dimensions([levelKey(body.dimension)]);
    });
  });
});

StartupEvents.postInit(() => {
  BODIES.forEach((body) => {
    // Without a marker GregTech's ore vein tooling has no tab for the body, so its
    // veins exist but cannot be looked up. The registry is GregTech's own rather than
    // a datapack one, hence the direct call — and hence the guard, since a frozen
    // registry is the failure mode to report rather than to crash on.
    try {
      new DimensionMarker(body.tier, ResourceLocation.parse(body.icon), body.name)
        .register(ResourceLocation.parse(body.dimension));
    } catch (error) {
      console.warn(`could not register dimension marker for ${body.dimension}: ${error}`);
    }
  });
});
