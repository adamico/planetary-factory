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

const gamePath = (relative) => KubeJSPathConstants.GAMEDIR.resolve(relative);

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

  eachEntry(registryAccess, GTRegistryKeys.ORE_VEIN_REGISTRY, (id, vein) => {
    const layer = vein.layer();
    const layerName = layer.getSerializedName();
    oreVeins[id] = {
      weight: vein.weight(),
      layer: layerName,
      dimensions: dimensionIds(vein.dimensionFilter()),
    };
    // Layers are shared between veins; a layer that covers no dimension a vein filters
    // to is exactly the failure this dump exists to make visible.
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
    ore_veins: oreVeins,
    bedrock_ores: bedrockOres,
    bedrock_fluids: bedrockFluids,
    worldgen_layers: worldGenLayers,
  };
};

const dumpRequested = () => {
  // JsonIO throws when the file is absent, which is the common case: no request, no dump.
  try {
    return KubeJsonIO.read(gamePath(REQUEST_FILE)) !== null;
  } catch (error) {
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
