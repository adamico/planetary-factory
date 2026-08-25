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
