// Worldgen registry dump — the read half of the automated worldgen check.
//
// GregTech's ore veins, bedrock ore deposits and worldgen layers are datapack
// registries, so nothing about them is true until a world has loaded them. This writes
// what actually loaded, for scripts/worldgen-check.py to compare against
// tests/worldgen/expected.json. See docs/testing/worldgen-registry-check.md.
//
// The plan for this seam named two handles for reading the registries and neither
// survived contact: `/gtceu dump_data` does not exist in GTCEu 7.0.2 (its commands are
// cape, place_vein and ui_editor), and KubeJS's web server serves registry *keys*
// only, which cannot answer what a vein's dimension filter or layer is. So the dump
// reads the live registry itself.
//
// It is inert in normal play: it does nothing unless the harness has written the
// request file, which lives under local/ and never ships.
const REQUEST_FILE = 'local/registry-dump.request.json';
const DUMP_FILE = 'local/registry-dump.json';
// The harness also accepts the dump from the log, so a sandbox where KubeJS cannot
// reach the filesystem still produces a result rather than a mystery.
const LOG_MARKER = 'WORLDGEN_DUMP ';

// Server scripts share one scope with KubeJS's own bindings, and `GTRegistries` is
// already one of them — redeclaring that name is a hard load error. Note also that
// java.nio is blocked by KubeJS's class filter, so all file access goes through
// KubeJS's own path constants and JSON helper.
const GTRegistryKeys = Java.loadClass('com.gregtechceu.gtceu.api.registry.GTRegistries');
const KubeJSPathConstants = Java.loadClass('dev.latvian.mods.kubejs.KubeJSPaths');
const KubeJsonIO = Java.loadClass('dev.latvian.mods.kubejs.util.JsonIO');
const BuiltInRegistryKeys = Java.loadClass('net.minecraft.core.registries.BuiltInRegistries');
// Every worldgen layer, not only the ones a vein happens to name. A body that registers no
// veins at all still has a layer, and whether that layer covers the body is a claim only this
// map can answer.
const WorldGeneratorUtilities = Java.loadClass('com.gregtechceu.gtceu.api.worldgen.WorldGeneratorUtils');
const RegistryKeys = Java.loadClass('net.minecraft.core.registries.Registries');

// Rhino cannot choose between Path.resolve(String) and Path.resolve(Path) from a JS
// string, and the InternalError it raises is swallowed by the request guard -- so the
// overload is named explicitly rather than left to overload resolution.
const gamePath = (relative) => KubeJSPathConstants.GAMEDIR['resolve(java.lang.String)'](relative);

// Sorted ids, so the fixture can be compared literally rather than order-sensitively.
const dimensionIds = (levelKeys) => {
  const ids = [];
  levelKeys.forEach((key) => ids.push(key.location().toString()));
  return ids.sort();
};

const eachEntry = (registryAccess, registryKey, fn) => {
  registryAccess.registryOrThrow(registryKey).entrySet().forEach((entry) => {
    fn(entry.getKey().location().toString(), entry.getValue());
  });
};

const dumpRegistries = (server) => {
  const registryAccess = server.registryAccess();
  const oreVeins = {};
  const worldGenLayers = {};
  const bedrockOres = {};
  const bedrockFluids = {};

  WorldGeneratorUtilities.WORLD_GEN_LAYERS.forEach((name, layer) => {
    worldGenLayers[name] = { dimensions: dimensionIds(layer.getLevels()) };
  });

  eachEntry(registryAccess, GTRegistryKeys.ORE_VEIN_REGISTRY, (id, vein) => {
    const layer = vein.layer();
    const layerName = layer.getSerializedName();
    oreVeins[id] = {
      weight: vein.weight(),
      layer: layerName,
      dimensions: dimensionIds(vein.dimensionFilter()),
    };
    // Layers are shared between veins; a layer that covers no dimension a vein filters
    // to is exactly the failure this dump exists to make visible. Layers are enumerated
    // above, but a vein may name one the map does not hold, and that has to show up too.
    worldGenLayers[layerName] = { dimensions: dimensionIds(layer.getLevels()) };
  });

  eachEntry(registryAccess, GTRegistryKeys.BEDROCK_ORE_REGISTRY, (id, deposit) => {
    const materials = [];
    deposit.materials().forEach((weighted) => {
      materials.push({
        material: weighted.material().getResourceLocation().toString(),
        weight: weighted.weight(),
      });
    });
    bedrockOres[id] = {
      weight: deposit.weight(),
      size: deposit.size(),
      depleted_yield: deposit.depletedYield(),
      depletion_amount: deposit.depletionAmount(),
      depletion_chance: deposit.depletionChance(),
      materials: materials,
      dimensions: dimensionIds(deposit.dimensionFilter()),
    };
  });

  eachEntry(registryAccess, GTRegistryKeys.BEDROCK_FLUID_REGISTRY, (id, deposit) => {
    bedrockFluids[id] = {
      weight: deposit.getWeight(),
      fluid: BuiltInRegistryKeys.FLUID.getKey(deposit.getStoredFluid()).toString(),
      depleted_yield: deposit.getDepletedYield(),
      // Bedrock fluids expose their dimension filter as a field, where veins and
      // bedrock ores expose an accessor.
      dimensions: dimensionIds(deposit.dimensionFilter),
    };
  });

  return {
    biomes: sampleBiomes(server),
    dimension_bounds: dimensionBounds(server),
    structures: dumpStructures(registryAccess),
    structure_sets: dumpStructureSets(registryAccess),
    blocks: dumpOreBlocks(),
    ore_veins: oreVeins,
    bedrock_ores: bedrockOres,
    bedrock_fluids: bedrockFluids,
    worldgen_layers: worldGenLayers,
  };
};

// The structure half of the dump, added for Terra's starting area (#84).
//
// A structure fails in three ways a codec cannot see, and all three are silent: it can load
// but list a biome the body never emits, so it generates nowhere; its structure set can carry
// a placement that scatters it instead of anchoring it; and its template palette can name a
// block id that does not exist, which Minecraft resolves to air rather than to an error. The
// first two are read here from the registries. The third is read as `blocks` -- the ore block
// ids the game actually registered -- because a template's palette is already resolved by the
// time anything can look at it, so the only way to catch a typo'd ore id is to check the id
// against the registry before trusting the template that names it.

// The whole structure and structure-set registries would be mostly vanilla noise; only the
// pack's own entries are worth dumping, and they are the only ones any fixture asserts.
const PACK_NAMESPACE = 'planetaryfactory:';

const holderSetIds = (holders) => {
  const ids = [];
  holders.forEach((holder) => {
    const key = holder.unwrapKey();
    if (key.isPresent()) ids.push(key.get().location().toString());
  });
  return ids.sort();
};

const dumpStructures = (registryAccess) => {
  const structures = {};
  eachEntry(registryAccess, RegistryKeys.STRUCTURE, (id, structure) => {
    if (!id.startsWith(PACK_NAMESPACE)) return;
    structures[id] = { biomes: holderSetIds(structure.biomes()) };
  });
  return structures;
};

// A placement is read through its own codec rather than through its accessors.
//
// The interesting fields -- `count` and `distance` on concentric_rings, `spacing` on
// random_spread -- live on the subclasses, and Rhino wraps the placement as the declared type
// of `StructureSet.placement()`, which is the base `StructurePlacement`. So `type()` and
// `salt()` are reachable and every subclass field is invisible, in all three of the property,
// bean-getter and explicit-call forms. The codec has no such problem: it dispatches on the
// placement's own type and hands back exactly the JSON the datapack file would carry, which
// is also the shape a fixture can be written against without knowing which subclass it is.
// Registry-aware ops, not plain JsonOps: `concentric_rings` carries `preferred_biomes`, which
// is a HolderSet, and a HolderSet cannot be written without a registry to resolve its holders
// against. Plain JsonOps does not throw on one -- it returns an empty DataResult, which read
// as "this placement has no fields" and passed a fixture asserting nothing.
const JsonOpsClass = Java.loadClass('com.mojang.serialization.JsonOps');
const RegistryOpsClass = Java.loadClass('net.minecraft.resources.RegistryOps');
const StructurePlacementClass = Java.loadClass(
  'net.minecraft.world.level.levelgen.structure.placement.StructurePlacement');

const encodePlacement = (placement, registryAccess) => {
  try {
    // `var`, not `const`. Rhino hoists a `const` declared inside a `try` out of the block,
    // so the second call to this function -- the pack has two structure sets -- raised
    // "redeclaration of var" from inside the guard below and turned every placement into a
    // null that read exactly like a placement with no such field.
    var encodedPlacement = StructurePlacementClass.CODEC
      .encodeStart(RegistryOpsClass.create(JsonOpsClass.INSTANCE, registryAccess), placement)
      .result()
      .orElse(null);
    return encodedPlacement === null ? null : JSON.parse(String(encodedPlacement));
  } catch (error) {
    // A placement that cannot be encoded is worth saying out loud: the fixture that asserts
    // its fields would otherwise read as "no such field" and pass on a typo.
    console.warn(`structure placement could not be encoded: ${error}`);
    return null;
  }
};

const dumpStructureSets = (registryAccess) => {
  const sets = {};
  eachEntry(registryAccess, RegistryKeys.STRUCTURE_SET, (id, set) => {
    if (!id.startsWith(PACK_NAMESPACE)) return;
    const placement = set.placement();
    const members = [];
    set.structures().forEach((entry) => {
      const key = entry.structure().unwrapKey();
      if (key.isPresent()) members.push(key.get().location().toString());
    });
    sets[id] = {
      placement: encodePlacement(placement, registryAccess),
      structures: members.sort(),
    };
  });
  return sets;
};

// Every ore block the game registered. GregTech builds these at runtime from its material
// registry, so no file in this repo can be read to find out whether `gtceu:goethite_ore` is
// the id or a plausible-looking near miss.
const dumpOreBlocks = () => {
  const ids = [];
  BuiltInRegistryKeys.BLOCK.keySet().forEach((key) => {
    const id = key.toString();
    if (id.endsWith('_ore')) ids.push(id);
  });
  return ids.sort();
};

// Which biomes a dimension's generator actually emits.
//
// A biome that parses and is listed in a biome source still generates nowhere if nothing in
// the noise space is closest to its parameter point, and no codec catches that: the world
// simply never contains it. So the sample asks the generator itself, over a grid of quart
// positions around the origin, through the same climate sampler worldgen uses. It reads no
// chunks -- nothing here generates or loads terrain, which is what makes it affordable.
// 4096 blocks either side of the origin. A narrower window reported Gleba's green marshland
// as never generated: vegetation is sampled at xz_scale 0.25, which stretches the humidity
// lobes wide enough that 1024 blocks can sit entirely inside one of them. The radius has to
// clear a lobe, or the check invents failures.
const SAMPLE_RADIUS_QUARTS = 1024;
const SAMPLE_STEP_QUARTS = 8;       // every 32 blocks
const SAMPLE_HEIGHTS_QUARTS = [4, 16, 24];   // y = 16, 64, 96

// Rhino may expose a Java accessor as a property or as a method; read it either way.
const beanValue = (object, name) => {
  const value = object[name];
  return typeof value === 'function' ? object[name]() : value;
};

// A level's dimension id, whether `dimension` hands back a ResourceKey or a ResourceLocation.
const dimensionId = (level) => {
  const dimension = beanValue(level, 'dimension');
  const location = beanValue(dimension, 'location');
  return String(location === undefined || location === null ? dimension : location);
};

const sampleBiomes = (server) => {
  const biomes = {};
  server.getAllLevels().forEach((level) => {
    // Asked of the level rather than of the generator: ChunkGenerator.climateSampler() is
    // not reachable through Rhino's remapper, and the level's own lookup needs no sampler.
    // It reads a loaded chunk when there is one and falls through to the generator's biome
    // source when there is not, so it still loads no terrain.
    const names = level.registryAccess().registryOrThrow(RegistryKeys.BIOME);
    const found = {};
    for (let x = -SAMPLE_RADIUS_QUARTS; x <= SAMPLE_RADIUS_QUARTS; x += SAMPLE_STEP_QUARTS) {
      for (let z = -SAMPLE_RADIUS_QUARTS; z <= SAMPLE_RADIUS_QUARTS; z += SAMPLE_STEP_QUARTS) {
        SAMPLE_HEIGHTS_QUARTS.forEach((y) => {
          const key = names.getKey(level.getNoiseBiome(x, y, z).value());
          if (key !== null) found[key.toString()] = true;
        });
      }
    }
    // Rhino exposes some accessors as bean properties and others as callables, and which
    // one a given method gets is not ours to predict -- so both shapes are accepted, and
    // whatever the chain yields is stringified at the end.
    biomes[dimensionId(level)] = Object.keys(found).sort();
  });
  return biomes;
};

// The column a dimension actually loaded with.
//
// ADR-0019 gives Terra a 0..192 column, and that number is written twice -- in
// `dimension_type/overworld.json` and in the `noise` block of the dimension's noise settings.
// The two disagreeing is silent: the world loads, and terrain is generated against a column
// the dimension does not have. No registry entry mentions either number, so the only place to
// read it is the loaded level.
const dimensionBounds = (server) => {
  const bounds = {};
  server.getAllLevels().forEach((level) => {
    // `var`, not `const`: Rhino hoists a `const` declared inside a `try` out of the block, so
    // the second level through here would raise "redeclaration of var" -- the same trap
    // encodePlacement above already fell into.
    try {
      var type = beanValue(level, 'dimensionType');
      bounds[dimensionId(level)] = {
        min_y: beanValue(type, 'minY'),
        height: beanValue(type, 'height'),
      };
    } catch (error) {
      // Said out loud rather than left as an absent entry, which a fixture asserting bounds
      // would otherwise report as "no bounds dumped" without saying why.
      console.warn(`dimension bounds could not be read: ${error}`);
    }
  });
  return bounds;
};

const dumpRequested = () => {
  // JsonIO throws when the file is absent, which is the common case: no request, no dump.
  try {
    return KubeJsonIO.read(gamePath(REQUEST_FILE)) !== null;
  } catch (error) {
    // A guard that swallows silently once hid a broken path resolve for the whole life of
    // this check, so the reason the dump is skipped is always said out loud.
    console.warn(`worldgen registry dump not requested: ${error}`);
    return false;
  }
};

ServerEvents.loaded((event) => {
  if (!dumpRequested()) return;

  const serialized = JSON.stringify(dumpRegistries(event.server));
  console.info(LOG_MARKER + serialized);

  try {
    KubeJsonIO.write(gamePath(DUMP_FILE), KubeJsonIO.parseRaw(serialized));
    console.info(`worldgen registry dump written to ${DUMP_FILE}`);
  } catch (error) {
    console.warn(`worldgen registry dump could not be written to disk: ${error}`);
  }
});
