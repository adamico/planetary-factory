---
status: provisional
supersedes: [57, 179]
---

# Terra's ore is surface discs everywhere, and the buried veins go

`#179` asked whether GregTech's buried veins belong in a Factorio-faithful pack. They do not, and
the reason is older than the ticket: **the veins never won an argument in the first place.** ADR-0019
put them there as the residue of vanilla worldgen — it decided flatness, cave removal and a surface
starting area, and left "everywhere else" as it found it, then commissioned prospecting to make the
leftovers legible. What looked like a decision was a default with an affordance built on top of it.

This ADR deletes Terra's ore veins and gives the whole planet one ore shape: the starting area's
flat disc, one block thick, flush with the topsoil, dealt outward by Factorio's own placement
numbers. It amends ADR-0019 (legibility, extraction, the prospecting prerequisite), amends ADR-0041
(the outfield amount arithmetic and its datum), and closes `#57`.

## Why the veins go

ADR-0043 settled that a mining rig works the **layer directly beneath it**, because a rig that
scanned downward would have ore in its area the player cannot see and the renderer cannot tint. That
is Factorio's rule and it is right. Its consequence, against buried veins, is that reaching ore means
digging down and placing the machine at depth.

That is the gesture ADR-0019 removed the world's reason for. It cut caves on the explicit grounds
that *"you do not dig because digging is not the verb, and a world with no caves is only cruel if
digging is how ore is found."* Then it kept ore buried. The pack therefore asks for the one gesture
whose supporting terrain it deleted, and it asks for it with a machine ADR-0043 designed to sit on
the surface.

**The complaint is the gesture, not the visibility.** Legibility — ADR-0019's second-ranked Factorio
feeling — has a fix that is not a worldgen change, and ADR-0019 commissioned it. The gesture does
not. A map layer over a buried vein still leaves the player digging a shaft to stand a rig at y 30.

**Fidelity is the tiebreaker, not the argument.** "Nauvis has no buried ore" does not settle this;
ADR-0019 broke fidelity deliberately once and was entitled to. The pack's fidelity decisions are
about numbers and names — ADR-0021's resource set, ADR-0028's row keys, ADR-0041's distance law —
not about geometry. The veins lose on the gesture. Factorio agreeing is a bonus.

## The decision

**One ore shape on Terra.** A patch is a filled disc of one ore block, one block deep, landed on the
terrain surface. Terra registers **no ore veins at all**. The bedrock crude deposit is untouched: it
is a fluid, `adapted` under `#86`, and not an ore patch (ADR-0020 as amended).

**Outfield patches are worldgen; the starting area stays a stamp.** The starting area is stamped by
`planetaryfactory_core` at server start only because no `StructurePlacement` can see world spawn.
Outfield patches have no spawn anchor, so they are ordinary worldgen: one `structure_set` per
resource, which also makes them locatable — the thing the Radar will want.

**Spacing is extracted, not chosen.** `base_spots_per_km2` is in the corpus and is the number:
2.5 for coal, copper, iron and stone, 1.25 for uranium. One spot per `1e6 / spots` blocks² gives
~632 blocks (~40 chunks) and ~894 blocks (~56 chunks) mean spacing. The generator derives the
`spacing`/`separation` pair from that field rather than carrying 40 as a literal, so a regeneration
moves it. **This is train distance, and deliberately so** — an outfield patch is not a belt run.

**Two laws, and they do not overlap.** Factorio splits a patch's quantity between how much a tile
holds and how wide the patch is:

| | term | shape |
|---|---|---|
| density | `1 + clamp((d − 300)/1300, 0, 1)` | grows from 300 blocks, doubled and clamped at ~1600 |
| richness | `max((1000 + d)/2600, 1)` | flat to 1600 blocks, then linear forever |

Density finishes growing exactly where richness starts. Quantity raises the spot's **amplitude**
(per-tile amount) until `regular_blob_amplitude_at`'s cap, and past the cap it goes into **radius**.
That is why a far patch in Factorio is visibly bigger, and both halves ship here.

**The two laws split the quantity; they do not multiply it.** Per-block amount is the amplitude law,
capped. Footprint is the radius law. The patch total is what falls out. The existing fallback in the
amount arithmetic is the amplitude half and **rises forever, which is a bug this ADR names**: past
the crossover it pays Factorio's radius growth out as richness instead.

**Distance is measured from world origin, not from spawn.** Worldgen is isolated from level state by
construction — no `Structure` or `StructurePlacement` can see world spawn, custom ones included. The
radius law is worldgen, so its datum can only be `(0, 0)`; and one mechanic may not have two datums.
So ADR-0041's amount fallback is amended to measure from origin as well. The error is bounded by the
spawn-to-origin offset and lands inside the band where richness is flat anyway (1600 blocks), so
nothing observable changes near spawn and the divergence beyond is a few hundred blocks on a linear
ramp. The alternative — stamping outfield patches mod-side so they can see spawn — reintroduces the
unbounded saved data this ADR refuses below.

**A near-spawn exclusion, with the ramp dropped.** Factorio suppresses regular patches inside
`starting_resource_placement_radius` (150) and fades them in over `regular_patch_fade_in_distance`
(300), full at 450. A `structure_set` cannot express a ramp. A **flat exclusion at 150 blocks** ships
and the ramp is `adapted`: the ramp exists to stop a rich outfield patch landing on the tutorial, and
150 does that. Taking 450 would strip the entire early walkable band, which is the opposite of what
an opening with no belts and no trains needs.

**A procedural disc, not size-variant templates.** A continuous radius cannot come out of a jigsaw
pool dealing three fixed templates. Outfield patches are generated by a structure that fills a circle
at a computed radius, reusing the ground projection that walks a column past whatever grew there. The
starting area keeps its template pools untouched, so the geometry check written for it still guards
what it was written for.

**Outfield patches carry no record.** The amount fallback already covers any block outside a recorded
field. The per-field census exists solely to defeat vanilla's silent dropping of an overlapping
jigsaw child, which a spaced structure set does not suffer. Recording outfield patches would grow a
saved-data list without bound as the player explores, for a divisor the laws already supply.

**Uranium stops being a special case.** The fallback that borrowed *"the smallest amount any other
field recorded"* existed because Factorio states no `starting_amount` for uranium. Under the
amplitude law it is dead weight: uranium has its own `base_density` (0.9) and `base_spots_per_km2`
(1.25), and the quantity expression takes both. It is derived like everything else, and it is
outfield-only, which is what `has_starting_area_placement = 0` means.

**Two amount arithmetics, and that is correct.** The starting fields take `starting_amount` over the
census block count; the outfield takes the laws. ADR-0041's comment commits to *"one arithmetic for
the whole planet"* and that commitment is retired rather than honoured. They are one arithmetic in
Factorio too: `starting_amount` is the starting patch's special case, `regular_*` is everywhere else,
and `starting_patches_split` is the seam. The pack currently fakes the second with the first. This
stops faking it.

## What this costs

**Prospecting stops being a verb, and the Radar becomes Factorio's Radar.** ADR-0019 called a
prospecting affordance *"a hard prerequisite of this ADR, not an enhancement"*, and that prerequisite
is discharged rather than met: a visible patch needs finding, not detecting. Exploration replaces
prospecting. The Radar reveals map at range — Factorio's actual Radar — instead of the ore-detection
meaning the pack had to invent for it because ore was hidden. `#57` closes. The Ore Finder Satellite
loses its stated job and needs a new one or needs cutting; that is its own ticket, not this ADR's
call.

**GregTech's surface indicators become dead.** They marked buried veins and there are none. The
check that guards them goes with them, and both failure modes it was written for stop existing.

**A regeneration is required before this can be built.** `regular_blob_amplitude_maximum_distance`,
`random_spot_size_minimum`/`maximum` and `regular_rq_factor` are not in the committed corpus, and the
amplitude-then-radius crossover cannot be computed without them. Without a re-extraction the size law
would be invented, which is the one thing this repo does not do with a Factorio number. This blocks
the ADR's implementation, not the ADR.

**What is deliberately not ported.** Nothing scales patch *count* with distance — `base_spots_per_km2`
is constant in Factorio too. Patches are land-only, confined by the same land biome tag the starting
area uses, so the realised density lands below the extracted 2.5/km² by whatever fraction of Terra is
sea: faithful as a rule, slightly lean as an outcome.

## How this is checked

Three claims, three checks, none of which launches the game:

- **The laws are Factorio's.** The resource extraction check re-derives amplitude, radius and spacing
  from the committed formulas, as it already does for the starting totals.
- **The amount arithmetic is right.** Unit tests under the mod's ore package cover the amplitude cap
  and the radius crossover, including that the two laws split a quantity rather than multiplying it.
- **The registries are what we said.** The worldgen registry check asserts Terra's ore vein registry
  is **empty** and that the resource structure sets are present — a stronger assertion than the
  fixture makes today.

The vein indicator check is deleted with the veins. What stays unchecked and is a world load: whether
a procedural disc lands on real terrain across every biome, which is the same class of question the
starting area's geometry check cannot answer either.
