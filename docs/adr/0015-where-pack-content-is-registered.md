---
status: accepted
---

# Where pack content is registered: mod, KubeJS, or datapack

The pack now has three places to put content: `planetaryfactory_core` (ADR-0014), KubeJS startup
scripts, and datapack JSON. All three register into the **`planetaryfactory`** namespace, so a block
ID does not say which one produced it. That is deliberate — and it means the boundary has to be
written down, because nothing in the IDs enforces it.

This ADR is consulted every time anyone adds a block, a feature or a material. ADR-0014 is consulted
once. They are separate documents for that reason.

## The rule

**The code owns the mechanism. The data owns the content.** Inherited from ADR-0011, now pack-wide.

| Goes in                       | What                                                                                                  |
| ----------------------------- | ----------------------------------------------------------------------------------------------------- |
| **`planetaryfactory_core`**   | Flora and any block whose behaviour needs a vanilla class no scripting API exposes — `SaplingBlock`, `TreeGrower`, multi-block growth. |
| **KubeJS startup scripts**    | Every other custom block and item. GregTech worldgen layers, via `GTCEuStartupEvents.WORLD_GEN_LAYERS`. |
| **Datapack JSON**             | Biomes, noise settings, surface rules, configured and placed features, loot tables, tags, recipes, ore veins, bedrock deposits. |
| **Resource pack / lang**      | Every player-facing string, including overrides of GCyR's orphaned stone names.                        |

Two consequences follow, and they are the point of the rule:

1. **Nothing tunable is compiled.** Densities, weights, biome frequency, drop counts, tree shape,
   names — all data, all editable without a build. This is what `#9` story 26 was protecting, and it
   survives ADR-0014 intact.
2. **A tree is defined once.** Its shape is a `minecraft:tree` feature JSON. Worldgen places that
   feature; the mod's `TreeGrower` invokes the same one. There is no second definition in Java and
   none in JavaScript.

## Collision convention

Two registration sources writing into one namespace collide the day someone adds
`planetaryfactory:yumako_log` in KubeJS without knowing the mod already has it. The failure is a
startup crash whose message will not say that.

The convention that prevents it is the table above, read as ownership: **flora and multi-block
features are the mod's; every other block and item is KubeJS's.** A comment at the top of
`kubejs/startup_scripts/blocks.js` states it, because that is the file someone will have open when
they are about to break it.

## Worldgen is datapack because it has to be

Not a preference. KubeJS `2101.7.1-build.181` — the build this pack ships — has **no worldgen
package at all**. `dev/latvian/mods/kubejs/` contains 33 packages and none is worldgen, feature,
biome or structure; a filename search for `*eature*`, `*orldgen*` and `*tructure*` across the mod
returns nothing. It cannot register or modify a feature, a biome or a placement.

`kubejs/startup_scripts/worldgen_layers.js` is not a counterexample: that is GregTech's own
`GTCEuStartupEvents.WORLD_GEN_LAYERS` API, called from a script, not KubeJS worldgen.

## What KubeJS can do, verified

Recorded because it was assumed wrong once in each direction, and the assumptions cost time.

- **Block builder types are a closed list of fifteen**: `basic`, `detector`, `slab`, `stairs`,
  `fence`, `wall`, `fence_gate`, `pressure_plate`, `button`, `falling`, `crop`, `cardinal`, `carpet`,
  `door`, `trapdoor`. **No sapling, no leaves, no tree.**
- **`BlockBuilder` supports real blockstate work**: `property(Property<?>)`, `defaultState(...)`,
  `placementState(...)`, `randomTick(Consumer<RandomTickCallbackJS>)`. `RandomTickCallbackJS` yields a
  `BlockContainerJS` and a `RandomSource`; `BlockContainerJS` exposes `set(Block, Map)` and
  `setBlockState(BlockState, int)`. `BlockRightClickedKubeEvent.getBlock()` returns the same mutable
  container.
- Therefore **Yumako's fruiting leaves need no Java**: a real blockstate property, regrown on random
  tick, reset on right-click.
- **`CropBlockBuilder`** offers `age(n)`, `crop(item, count)`, `bonemeal(...)`, `growTick(...)`,
  `survive(...)`, `farmersCanPlant()`, and a `SeedItemBuilder`.
- **`kjs$runCommand` / `kjs$runCommandSilent`** exist on `LevelKJS` and `MinecraftServerKJS`. This is
  what made `/place feature` reachable, and ADR-0014 rejected that route on other grounds.
- **KubeJS cannot register a GregTech material** — see `#18`. Materials are data files read by the
  GCyR fork.
- **A block entity with a persistent, automatable, player-openable inventory is KubeJS's**, verified
  in a world by `#133`. `blockEntity(be => { be.inventory(name, [], 9, rows); be.rightClickOpensInventory(name) })`
  gives a container that hoppers and pipes see, that survives save and reload, and that opens as a
  chest screen. So a container with a screen is **not** mechanism in the sense that sends a block to
  the mod — the table's second row already covers it, and no new row is needed. Three constraints
  come with it, all read off `2101.7.1-build.181` and all silent when broken:
  - **Every face is the empty set `[]`, never `null`.** Rhino coerces the argument through
    `Set.of(...)`, which throws before KubeJS's `attach` can branch on `isEmpty()`.
  - **The width is 9 or the screen is wrong.** `CustomChestMenu` lays slots out nine per row
    regardless of the attachment's `width`, while `KubeJSGUI` sizes the window from its `height`.
  - **Six rows is the ceiling** — `CustomChestMenu.TYPES` is `GENERIC_9x1..9x6`, indexed by row count.
  - `rightClickOpensInventory` **writes `BlockBuilder.rightClick`**, so such a block cannot also
    carry a custom right-click callback.

## Create integration comes free if the tags are right

`TreeCutter` finds a tree by `isLog(BlockState)` and `isLeaf(BlockState)`, both tag-driven, and the
Deployer places block items through the normal use-on path. So a Create tree farm works on the
pack's trees with no integration code, provided the blocks carry `minecraft:logs` and
`minecraft:leaves`. Both the mod and `BlockBuilder.tagBlock()` can supply those.

## Considered Options

- **Give the mod its own namespace.** Rejected in ADR-0014: block IDs would advertise their
  registration source.
- **Move all custom blocks into the mod for consistency.** Rejected: it compiles content that has no
  reason to be compiled, and `blocks.js` already demonstrates the scripted path working.
- **Ship the mod's JSON as builtin data, datapack-overridable.** Rejected: two sources for the same
  file is how an afternoon disappears into editing the copy that is not being read.
