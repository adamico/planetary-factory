---
status: accepted
---

# Every surface body generates on a stone block GCyR still registers, and none of them says so to the player

Slice 1 deleted GCyR's stock bodies — Luna, Mars, Mercury, Venus — by blocking their planet and
dimension entries with a `pack.mcmeta` filter. Blocks are code-registered, so the filter could not
touch them: `gcyr:moon_stone`, `gcyr:martian_rock`, `gcyr:mercury_rock` and `gcyr:venus_rock` are
all still in the registry, each with a GregTech `TagPrefix` already registered against it, ore
variants for every material, and textures.

**Each of the four surface bodies claims one of those stones as its ground.** Ignus takes
`gcyr:venus_rock`. What that buys is the entire ore-variant tier: a GregTech ore vein places ore by
*material*, and the block that lands in the ground is the variant registered for the stone its
worldgen layer matches. A body whose terrain is plain `minecraft:stone` generates the Overworld's
ore blocks; a body on a stone with no registered `TagPrefix` generates nothing at all. Reusing
GCyR's four means no ore textures, no ore blocks and no `TagPrefix` work in this pack.

**What is reused is the block, not the name.** GCyR's lang gives `tagprefix.venus` as
"Venus %s Ore" and `block.gcyr.venus_rock` as "Venus Rock". Those keys are overridden in
`kubejs/assets/gcyr/lang/en_us.json`. No player sees the name of a body this pack deleted.

Ignus's terrain, biomes and dimension type are copies in this pack's namespace rather than
references to GCyR's, so the shape of the world is ours to tune per body. The density functions and
noise the copied noise settings reference are still GCyR's, as are the blocks.

Ignus's worldgen layer is a new one, `ignus_rock`, registered from KubeJS startup and scoped to
`planetaryfactory:vulcanus`. GCyR's own `venus` layer still exists and is scoped to `gcyr:venus`;
reusing it would tie four bodies to a dimension key this pack does not offer as a planet.

## Considered Options

- **Register the pack's own stone blocks and ore variants.** Full independence, at the cost of four
  stones × every ore material in textures and registration, to arrive at what is already installed.
- **Generate plain vanilla stone on every body.** Free, and it defeats the point: every body would
  yield the Overworld's ore blocks, and nothing about a mined ore would say where it came from.
- **Reuse GCyR's `venus`, `mars`, `moon` and `mercury` worldgen layers as well as its blocks.**
  Fewer moving parts, but each of those layers is scoped to a GCyR dimension key, so the veins would
  have to live on GCyR dimensions too.
- **Unblock GCyR's bodies and build on them directly.** Rejected in slice 1 and not reopened here:
  the pack's six bodies are the design, and Venus is not one of them.

## Consequences

**If GCyR ever stops registering these blocks, four bodies stop generating ore.** That is the whole
risk of this decision, and it is not theoretical — the blocks are orphans upstream, kept alive only
by the mod still registering bodies this pack removed. The pack builds GCyR from its own fork
(ADR-0001), so the fix is available: pin the blocks in the fork. Until that happens, a GCyR update
that prunes them is a worldgen outage, not a compile error, and the worldgen registry check is what
would catch it.

Every body ticket after Ignus copies this: claim a stone, copy the noise settings and biomes into
this pack's namespace, register a layer scoped to the body's dimension, override the lang keys that
name GCyR's body.
