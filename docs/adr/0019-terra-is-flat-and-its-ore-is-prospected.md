---
status: accepted
---

# Terra is flat and cave-free, and its ore is prospected rather than dug

Terra is the Nauvis analogue, and it has been the vanilla Overworld untouched: no noise settings,
no dimension type, no density functions, no terrain mod. Vanilla terrain answers a different design
question than this pack asks. Its relief is there to be explored, its caves are there to be
descended, and its ore is hidden on purpose. A logistics pack wants the opposite of all three.

This ADR changes Terra's world. **Which mod owns each capability is ADR-0017 and the progression
spine is ADR-0018**; this one is about the ground they stand on. It amends ADR-0007, which gives
GregTech worldgen to planets: that decision stands, and its consequences on Terra change.

## What the Factorio feeling actually is

Ranked, because the ranking decides everything downstream:

1. **Patches are finite** and force the factory outward.
2. **Patches are legible** — you see a patch's outline and plan a miner layout before placing a
   block.
3. **Terrain is flat**, so belts and rails route without terraforming.
4. **Patches are belt-reachable** from where you already are.

Flatness is third, and it is worth being precise about why it is on the list at all: Nauvis is flat
because Factorio is 2D, not because Wube decided flatness felt good. Copying the flatness and
missing the legibility would be cargo-culting the substrate. **Finiteness is first and this ADR does
not deliver it** — that is its own decision, and it is the more important one.

The frictions this removes, also ranked: vertical traversal, then the terraforming tax, then search
cost, then combat. Combat is last and stays.

## The decision

**Shape.** Low-amplitude terrain — roughly ±6–10 blocks — with **rare dramatic landmarks**: cliff
walls and canyons, infrequent enough to read as landmarks and frequent enough to give a base a
shape. Not a superflat plane. A featureless plane is the opposite of legible: nothing reads at
distance and there is nothing to navigate by. With carvers off, canyons are terrain, produced by a
narrow steep segment in the offset spline, not by the `minecraft:canyon` carver.

**Column.** `min_y: 0`, `height: 192`, `logical_height: 192`, sea level 63. Negative Y ceases to
exist. Both the dimension type and the noise settings' own `noise` block carry these numbers — a
disagreement writes outside the chunk's section array. Two vanilla router terms hardcode the old
range and are retuned with it: `final_density`'s gradients at −64→−40 and 240→256, and `depth`'s
at −64→320.

**Water.** `aquifers_enabled: false`. The fluid picker degenerates to a flat global water table at
sea level: oceans and lakes survive and get *more* reliable, and what is lost is perched water and
flooded cave systems, of which there will be none. This matters more than it sounds, because
**vanilla 1.21 overworld biomes ship no water-lake feature at all** — `plains.json`'s LAKES step is
lava only. All surface water comes from terrain dipping below y=63. **That Terra has water is a
requirement of this ADR, not an implementation detail**: a flattening spline that never dips below
63 produces a world with no water, no steam and no chemistry, and it will not announce itself.

**Biomes.** A reduced palette of 5–8, Nauvis-like, no cold biome. Terra is home and reads as one
coherent world; contrast belongs on Sapros and Ignus. Sixty flattened vanilla biomes read as vanilla
with a bug, and the palette size is also what makes the carver work tractable.

**Caves: none.** Three sources, all closed. The noise caves fold into `noise_router.final_density`,
which is replaced wholesale rather than edited — the `overworld/caves/*` density functions are left
in place and simply referenced by nothing. Per-biome carvers are emptied with `"carvers": {}` in
each palette biome; note that in 1.21.1 `carvers` is an object keyed by carving step, not the flat
list the wiki documents for 1.21.2+. And as a global backstop, all three overworld carvers share
`#minecraft:overworld_carver_replaceables`, which nothing else in vanilla references: emptied with
`{"replace": true, "values": []}`, every carver — including in modded biomes nobody enumerated — can
replace no block.

**Ore.** The `deepslate` layer is retired **on Terra only**, and the surviving veins compress into a
shallow band above bedrock. The vein list itself is not decided here: restricting Terra's ore to
Factorio's set plus vanilla progression is its own ticket, and assigning bands before that lands
would be assigning bands to veins about to be deleted.

**Legibility.** Surface ore fields exist **only in the starting area**. Beyond it, veins are buried
and marked by GregTech's indicators — which are already enabled and are, on their own, useless,
because an indicator that never reaches the map is not information. A prospecting affordance is a
hard prerequisite of this ADR, not an enhancement.

**The starting area** is a spawn-anchored structure with a **fixed resource set** and a
**randomized layout and patch sizes**. Fixed set because a 20–25 hour arc cannot have seeds without
coal; randomized layout because a start worth surveying is better than a start memorised.

**Extraction.** Once the starting patches deplete, manual extraction stops being the verb. Beyond
spawn the player prospects, then places a machine. GregTech's miners are used exactly as they
already behave — finite, and replacing mined ore with cobblestone, so nothing is left as a crater.
**This is what justifies having no caves**: you do not dig because digging is not the verb, and a
world with no caves is only cruel if digging is how ore is found.

## Mechanism

A real datapack in `datapacks/`, not `kubejs/data/`.

ADR-0015's rule — worldgen is datapack JSON because KubeJS `2101.7.1-build.181` has no worldgen
scripting package — holds unchanged. What is new is the *direction*: everything the pack ships from
`kubejs/data/` today is either a new namespace, which is additive, or a `minecraft:` **tag**, which
merges. This is the pack's first **wholesale replacement** of a vanilla `minecraft:` worldgen entry,
and that depends on pack sort order rather than on merge semantics. `datapacks/` is unambiguous by
design, and keeping the pack's first vanilla override visibly separate from pack-namespace content
is worth more than the consistency it costs.

The world-type selector is a live hole: "Large Biomes" and "Amplified" reference their own noise
settings, so a player choosing either gets vanilla terrain and a broken pack. Forcing the default
world type is assumed here and decided in its own ticket.

The file set:

```
datapacks/<pack>/data/minecraft/dimension_type/overworld.json
datapacks/<pack>/data/minecraft/dimension/overworld.json                # new; vanilla ships none
datapacks/<pack>/data/minecraft/worldgen/noise_settings/overworld.json
datapacks/<pack>/data/minecraft/worldgen/density_function/overworld/{offset,factor,jaggedness}.json
datapacks/<pack>/data/minecraft/tags/block/overworld_carver_replaceables.json
datapacks/<pack>/data/minecraft/worldgen/biome/<each of the palette>.json
```

`multi_noise_biome_source_parameter_list/overworld.json` is **not** in that list and cannot be: it
is a 37-byte `{"preset": "minecraft:overworld"}` stub whose codec accepts only hardcoded preset
names. The palette lives in an explicit `biomes` list in the dimension file — the same shape
`planetaryfactory:gleba` already uses.

Flatness is expressed in three density functions: `offset` as a shallow spline carrying both the
relief and the rare cliff segment, `factor` as a large constant, `jaggedness` as `0.0`. The mapping
from spline values to block relief is empirical; this is the one part of the ADR that is tuning
rather than a discrete choice.

## Considered Options

- **Superflat.** Rejected under the legibility argument: a plane with no landmarks gives a player
  nothing to read at distance and no reason for a base to have a shape.
- **Keep vanilla terrain and only surface the ore.** Genuinely tempting, and it delivers the
  second-ranked source of the feeling on its own. Rejected because the vertical traversal friction —
  the top-ranked friction — is terrain-shaped, not ore-shaped.
- **Flatten and leave veins where they are.** Rejected, and it is the trap this ADR started as:
  flattening *removes* the caves and cliff faces that currently expose ore, so flatness without a
  band change makes ore strictly harder to find than vanilla.
- **Replace Terra with a `planetaryfactory:terra` dimension.** Rejected: a rename that invalidates
  the GCyR planet entry, every vein's `dimension_filter` and the three `terra_*` bedrock deposits,
  for no mechanical gain.
- **Vanilla `−64..384` column.** Rejected in favour of `0..192`: with no caves and a shallow band,
  the negative column is empty rock and the sky above 192 is headroom a factory never uses.
- **A landfill item.** Rejected. Factorio needs landfill because it has no block placement; a
  Minecraft player fills a lake reflexively, and an item for it would be ceremony over a verb the
  game already has.
- **Flat as a pack-wide principle.** Rejected. Terra is the Nauvis analogue; the other bodies are
  the contrast. Precedent, not policy.

## Consequences

- **Vanilla structures and the Illager raid loop are collateral, accepted.** `CONTEXT.md` makes
  Terra the only body with raids, and nothing in the pack protects village placement — there is no
  structure set or spacing override, and `guardvillagers` and `torchmaster` tune village population,
  not placement. The bases that matter are player-made outposts.
- **Mob pressure moves to the surface**, all of it, on a world with maximal open ground. Left
  vanilla deliberately. `config/incontrol/` is installed and inert, so the lever exists if
  playtesting says night is unbearable; it is not pulled speculatively.
- **The `terra` fixture in `tests/worldgen/expected.json` is invalidated** — 11 vein rows with
  weights and layers, 3 bedrock deposits, 3 forbidden veins. It is rewritten, not preserved.
- **A restored carver or a drifted amplitude ships undetected.** Flatness and cave-freeness are not
  registry facts and `scripts/worldgen-check.py` cannot see them. The check is a human on delivery,
  recorded as a decision rather than skipped.
- **Two new early beats land in the beat sheet** — the prospector and the Personal Assembler, both
  before the first automation beat. Handing the prospector out late reproduces exactly the blind
  digging this ADR exists to prevent.
- **Ore depletion is now load-bearing and unwritten.** It is the top-ranked source of the Factorio
  feeling, and with manual extraction ending after the starting patches it is also the transition
  from the hand-crafted opening to the automated midgame. Its own ADR, and the higher priority.
- **Save invalidation is not a cost.** The pack is pre-release.

## Amended by ADR-0021: the noise settings suppress vanilla ore

Terra's `noise_settings` override must also **suppress vanilla ore features**, not only replace
terrain and close the carvers. Because Terra had no noise settings before this ADR, it generated the
full vanilla ore set; ADR-0021 cuts Terra's ore to iron, copper, coal and uranium across all four of
its ore systems, and vanilla's is one of them. Vanilla ore left in place would be uncharted by
prospecting and undepletable by a miner — a straight bypass of ADR-0020.

## Amended in build: the datapack lands in `kubejs/data/`, not `datapacks/`

The Mechanism section above puts the file set in an instance-root `datapacks/` folder. **Nothing in
this jar set reads that folder.** It is not a vanilla or NeoForge load path — world datapacks live in
`saves/<world>/datapacks/` — and the pack ships no OpenLoader-style global-datapack mod. The folder
exists in the instance and is empty.

The files therefore land in `kubejs/data/`, and the ADR's stated worry — that a wholesale replacement
of a vanilla `minecraft:` entry depends on pack sort order rather than merge semantics — turns out to
be already answered there: `kubejs/data/gtceu/gtceu/ore_vein/*.json` has been wholesale-replacing
GregTech's own vein files since before this ADR. KubeJS's data pack sorts above both vanilla and the
mods, so the override is the proven seam rather than the speculative one.

Two further corrections found while building:

- **The three shaping density functions must not override `minecraft:overworld/{offset,factor,
  jaggedness}`.** Sapros, Ignus and Electro all point their noise router's `depth` at
  `minecraft:overworld/depth`, which is built from `overworld/offset` — overriding it would flatten
  three other bodies along with Terra, which is exactly the "flat as a pack-wide principle" option
  this ADR rejected. They live under `planetaryfactory:terra/` instead, and Terra's `final_density`
  inlines them.
- **Vanilla ore needs no suppression clause in the noise settings.** Terra's palette is seven
  pack-namespace biomes authored from scratch, so vanilla ore is absent by omission — the
  `underground_ores` step is simply empty. The same fact closes Create's ore and every other
  biome-modifier feature: the palette biomes are deliberately **not** members of
  `#minecraft:is_overworld`, which is the tag those modifiers target. *This bullet also named
  Mekanism, which generated from its own config rather than a biome modifier and was switched off in
  `config/Mekanism/world.toml`; ADR-0035 removed the mod, so there is nothing left to switch off.*

## Amended in build: vein spacing is a config number, and the default makes ore continuous

GregTech places veins on a grid, and `config/gtceu.yaml`'s default `oreVeinGridSize: 3` puts one
every 48 blocks. Terra's four veins run 38–52 blocks across, so at the default they **touch**: the
first in-world look reported ore in every chunk, which is a continuous layer rather than patches.

Grid size is raised to **6** (96 blocks, wider than the largest vein) with `oreVeinRandomOffset` at
**24**, a quarter of the cell as at the default, so patches stay off a visible lattice. This is
empirical like the terrain constants, and it is tuned against play rather than derived.

Note that **GregTech rewrites `config/gtceu.yaml` on every game load**, discarding any comment not
its own. The reasoning cannot live next to the value; it lives here.

## Amended in build: the starting area is a jigsaw structure, and its patches are plain blocks

This ADR asks for "a spawn-anchored structure with a fixed resource set and a randomized layout
and patch sizes" and leaves the mechanism open. Issue #84 closed it, and three of the four
answers are worth recording because each rules out an option that looks obvious first.

**The patches are ordinary ore blocks, not GregTech veins.** The tempting reading is that a
starting patch should be a vein so that ADR-0020's depletion and the miner ladder can see it.
Neither needs it. ADR-0020 already settled that depletion *is* physical block removal — there is
no depleted flag on a vein to set — and a GregTech Miner scans for ore blocks rather than
consulting the vein registry. Against that, a vein cannot be spawn-anchored at all: GregTech
places veins on its own grid, and nothing in that placement can be told "one, here".

**Anchoring is not worldgen at all: `planetaryfactory_core` stamps the pool onto world
spawn.** This corrects a first attempt at `minecraft:concentric_rings` with `distance: 0`,
`count: 1`, which anchors to the world *origin* and not to spawn — a difference that only looks
cosmetic. That placement pins the ring to chunk (0,0) and then searches a hardcoded 112 blocks
for a `preferred_biomes` match. Terra has a sea, so on a seed whose origin is open water the
search fails, the ring falls back to (0,0), the structure's land-biome predicate correctly
refuses to start there, and the player gets no opening whatsoever — silently, with nothing in
the log. That is not a tuning failure; it is roughly a coin flip on seed.

No custom placement type fixes it either, which is the load-bearing fact. A `StructurePlacement`
is asked `isPlacementChunk(ChunkGeneratorStructureState, x, z)` and that state carries a biome
source, a random state and the seeds — no `ServerLevel`, no `ServerLevelData`, no world spawn.
Worldgen is deliberately isolated from level state so it stays deterministic and thread-safe, so
*no* placement type can see spawn. Nor can `random_spread` be bent into "exactly one, on land":
`spacing` caps at 4096 chunks, a large `spacing`/`separation` pair collapses the position back
onto chunk (0,0), and its biome filtering happens at start time, so a moderate spacing yields
many starting areas rather than one.

So the placement is imperative and lives in the mod (ADR-0015 puts mechanism there): on
`ServerStartedEvent` — after vanilla has chosen a spawn, which is on land by construction, and
after it has prepared the spawn chunks there are blocks to write into — the mod runs vanilla's
own `JigsawPlacement` against the same start pool. Everything above the placement is unchanged:
same hub variants, same per-resource pools, same size and rotation randomisation. A `SavedData`
flag makes it once per world. This is also what the packs that do this well do; the two dedicated
mods in the space both stamp imperatively at world creation, and neither uses a structure set.

`generateJigsaw` itself is inlined rather than called, for one reason: **the pieces have to have
loaded chunks under them before anything is written.** A `terrain_matching` piece is placed
through a `GravityProcessor`, which asks the *level* for the surface height — and `Level.getHeight`
does not generate. On a chunk that is not loaded it returns `getMinBuildHeight()`, so the field is
not dropped, it is written at y=-64 inside the bedrock. The prepared spawn area is a few chunks
across and the fields reach a hundred blocks out, so most of the opening landed in bedrock and the
player found one partial patch, varying by seed, with a clean log and a successful return value.
The stamp therefore walks the pieces' own bounding boxes and calls `level.getChunk` over them
first. It also logs one line per piece: a jigsaw child rejected for overlap is dropped silently,
so the piece count is the only evidence that the hub dealt fewer than three fields.

The cost is honest and small: the structure is no longer part of worldgen, so `/locate` cannot
find it and the pack logs the coordinates instead. There is no structure set, and the worldgen
check's fixture row for it therefore asserts what the stamp depends on — that the structure
loaded, that its biomes are ones Terra emits, that its ore ids are real blocks — rather than a
placement that no longer exists.

The land-only biome tag survives the change and still matters. `#planetaryfactory:terra_land` is
not the vein tag `#planetaryfactory:terra`, which holds the sea and the shore; the structure's
own biome list is what keeps a field off the seabed.

**Randomization is jigsaw, and the fixed set is one pool per resource.** A single shared pool
would deal three copper patches and no coal — exactly the failure the fixed-set requirement
exists to prevent. So the hub carries one connector per resource, each pointing at that
resource's own pool, and the size variants inside each pool are what randomizes the patch sizes.

**A patch is one ore block, and not the vein's mix.** The buried veins deal four ore blocks each
and two of them cross metals: the iron vein carries malachite, which smelts to copper, and the
copper vein carries iron ore and pyrite, which smelt to iron. Underground that is a feature — a
vein is where you learn the local rock. In the opening it is a lie, because the patch is the
tutorial and has to answer "what is this a patch of" with one word; a player who mines the iron
field and gets copper has been taught something false about how the world is organised. So each
field is a single block: `gtceu:iron_ore`, `gtceu:copper_ore`, `gtceu:coal_ore`.

**The fields lie on the terrain, and the mod owns the projection that puts them there.** They are
one block thick and replace the topsoil block, which is both the Factorio reading and what keeps a
half-dug patch legible. That is *not* `terrain_matching`: vanilla's projection applies a
`minecraft:gravity` processor, which on a `ServerLevel` reads the `WORLD_SURFACE` heightmap —
defined as "the highest block that is not air". A tree is not air, so a field crossing a wood
landed on the canopy, ore in place of leaves twenty blocks up, split between treetop and ground
wherever the wood ended. No vanilla heightmap avoids it; `OCEAN_FLOOR` and `MOTION_BLOCKING` stop
at leaves and logs too. So `planetaryfactory_core` registers `planetaryfactory:ground`, a structure
processor that walks the column down past whatever grew there and lands on the first real terrain
block, and the elements are `rigid` — the projection is what would add the gravity processor back,
and it runs last. Under a wood the field therefore lies *beneath* the trees, which go on standing
on it.

**The hub is sized by the widest field, not by how far apart the connectors look.** A patch
template's bounding box is a rectangle as wide as the field running the whole way from its
connector to the far end, so two fields on perpendicular faces both cover the corner beside the
hub and overlap there. Vanilla drops an overlapping jigsaw child without logging anything — a
rejected child is an ordinary outcome, not an error — so this ships as "two patches instead of
three", on some seeds only. Making the hub at least as wide as the widest field plus its scatter,
and clamping each connector by its own resource's widest variant, keeps every field's sideways
extent inside the hub's footprint, which is what makes three fields a guarantee rather than a
usual outcome. `tests/worldgen/test_start_geometry.py` asserts it across every hub variant and
every combination of size draws, because the size is drawn at world generation and only the worst
case is a guarantee.

**Stone is not one of the patches**, against the ADR's "iron, copper, coal and stone at
minimum". Terra's surface is soil over stone everywhere, so a stone patch is decoration rather
than a resource decision, and it would be the only patch in the opening that teaches nothing.
Iron, copper and coal are placed; oil is not, because #58 makes Terra's bedrock deposits the
planet's only petroleum source and those are everywhere already.

The templates are generated by `scripts/build-terra-start.py` rather than built in a creative
world and exported, so a tuning change is a re-run and a layout is reviewable as a diff. Patch
sizing is anchored on ADR-0020's own figure — a small surface patch worked by hand empties in
about an hour — which a mid-size draw of roughly 1,150 ore blocks meets. That is tuning against
play, not a discrete choice, and it is the number to move if the opening feels long or thin.
