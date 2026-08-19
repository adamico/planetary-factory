# Can new GCyR bodies be defined by datapack alone?

**Answer: mostly yes — the body itself is pure data; GT ore generation on it is not.**

Read against our fork `~/Documents/curseforge/minecraft/Instances/gcyr-src`, branch `1.21.1`,
HEAD `b258df8` (the tree ADR-0001 builds `mods/gcyr-1.21.1-0.2.4+gt7.0.2-src.jar` from).

## What is data-driven

| Thing | Loaded from | Loader | Source |
| --- | --- | --- | --- |
| Planet | `data/<ns>/gcyr/planets/*.json` (**datapack**) | `PlanetData extends SimpleJsonResourceReloadListener("gcyr/planets")` | `data/loader/PlanetData.java:48` |
| Solar system | `assets/<ns>/gcyr/planet_assets/solar_systems/*.json` (**resource pack**) | `PlanetResources` | `data/loader/PlanetResources.java` |
| Galaxy | `assets/<ns>/gcyr/planet_assets/galaxies/*.json` | `PlanetResources` | same |
| Planet ring | `assets/<ns>/gcyr/planet_assets/planet_rings/*.json` | `PlanetResources` | same |
| Sky renderer | `assets/<ns>/gcyr/planet_assets/sky_renderers/*.json` | `PlanetResources` → `ClientModSkies.register()` | same |
| Dimension, dimension type, noise settings, biome | vanilla datapack registries | vanilla | `data/gcyr/dimension/`, `dimension_type/`, `worldgen/` |

There is no hard-coded planet registry and no Java-side list of bodies in any of these paths. Both
loaders rebuild their collections from whatever files they find.

### The Planet record

`api/space/planet/Planet.java` is a record with `DIRECT_CODEC`. Fields, all required except
`parent_world`:

`translation`, `galaxy`, `solar_system`, `world`, `orbit_world`, `parent_world` (optional),
`rocket_tier`, `gravity`, `has_atmosphere`, `days_in_year`, `temperature`, `solar_power`,
`has_oxygen`, `button_color`.

Note: GCyR's own `mars.json` carries an `orbit_solar_power` key that the codec does not declare. It
is silently ignored — do not copy it.

`translation` is a plain lang key, so the Latin display names of ADR-0004 are supplied by the pack's
own lang file with no code involved.

Planet data is server-authoritative and synced to clients as NBT (`PlanetData.writePlanetData` /
`readPlanetData`), so a server-side datapack is sufficient; clients need no matching datapack.

### Fuel tier by distance needs no code

`RocketEntity.computeRequiredFuelAmountForDestination` (`common/entity/RocketEntity.java:386`)
derives the tier purely from the Planet record — `parent_world` match → moon cost, same
`solar_system` → solar-system cost, same `galaxy` → galaxy cost, otherwise anywhere cost — with the
four amounts coming from `GCYRConfig.rocket.*FuelAmount` (8/14/26/48 buckets). Placing a new body in
the graph is enough to price it.

`rocket_tier` gates entry independently: `RocketEntity` refuses launch when the rocket's part tier
is below the destination's (`RocketEntity.java:422`).

## What GCyR hard-codes, and why it does not bind us

Three Java-side lists enumerate GCyR's four stock bodies, and together they are what lets GregTech
generate ore on a body:

1. `common/worldgen/GCYRWorldGenLayers.java:15-18` — one `SimpleWorldGenLayer` per dimension,
   binding a stone block and a dimension `ResourceKey`. GT ore veins target a worldgen layer.
2. `common/data/GCYRDimensionMarkers.java:20-31` — a marker block and a GTCEu `DimensionMarker` per
   body, used to show which dimension a vein belongs to.
3. `GCYRGTAddon.java:44-50` — `TagPrefix.oreTagPrefix` per body, the per-body ore block variants,
   with matching lang in `data/lang/LangHandler.java:12-14`.

GCyR writes these in Java because it is a mod. **The pack does not have to** — GTCEu 7.0.2 exposes
all three registries to KubeJS, verified against `mods/gtceu-1.21.1-7.0.2.jar`:

| Registry | KubeJS entry point |
| --- | --- |
| Worldgen layer | `GTCEuStartupEvents.WORLD_GEN_LAYERS` → `WorldGenLayerKubeEvent.create(name, builder)`, builder taking `targets(...)` and `dimensions([...])` |
| Dimension marker | `GTRegistries.DIMENSION_MARKER_REGISTRY`, registered through `BuilderTypeRegistry.addDefault` in `GTKubeJSPlugin` with `DimensionMarkerBuilder` (`iconSupplier`, `tier`, `overrideName`) |
| Ore tag prefix | `integration/kjs/builders/prefix/OreTagPrefixBuilder` and `TagPrefixBuilder` |

So giving a new body GT ore generation is startup-script work in this repo, not a fork change.

## Removing the stock bodies

`PlanetData.apply` has no remove directive: a datapack can override `gcyr:luna` by shipping a file
at the same path, but cannot delete the entry. It does dedupe by dimension — a later planet whose
`world` matches an earlier one evicts it (`PlanetData.java:60`) — but the stock ID survives in the
menu either way. Since ADR-0004 fixes our IDs to Factorio names, overriding the stock files in place
is not an option. The dedupe in `apply` does evict an earlier planet sharing a `world`
(`PlanetData.java:60`), but it iterates a `HashMap` in unspecified order, so it is not something to
rely on. Deleting the stock planet and dimension JSONs therefore belongs in the fork, which ADR-0001
already commits us to maintaining.

## Consequence for the pack

Defining Ignus, Electro, Sapros, Gelida and Atlantis — their planet entries, dimensions, worldgen,
skies, solar system, rocket tiers, fuel costs and display names — is datapack and resource pack work
in this repo, and giving them GregTech ore generation is KubeJS startup-script work. The fork is
needed for exactly one thing: dropping the stock bodies, which the planet loader offers no way to
remove.
