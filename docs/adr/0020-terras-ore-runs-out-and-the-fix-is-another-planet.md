---
status: accepted
---

# Terra's ore runs out, and the fix is another planet

ADR-0019 flattened Terra, deleted its caves and made its ore prospected rather than dug. It ranked
the sources of the Factorio feeling and put **finiteness first** — then delivered the other three
and said so plainly: "Finiteness is first and this ADR does not deliver it." This is that decision.

It is not a flavour mechanic. ADR-0019 makes depletion the **progression transition**: once the
starting patches are gone, manual extraction stops being the verb, and the player prospects and
places a machine. The shape of depletion decides how the hand-crafted opening becomes the automated
midgame — and, as it turns out, how Terra ends.

## Depletion is not a mechanism this pack builds

The framing this decision started with was that GregTech's veins are "bounded in area but not
depletable: mine one forever, never move." That is not what the code does. A GregTech vein is
ordinary blocks. Mining removes them, nothing regenerates them, and a GregTech Miner **replaces
mined ore with cobblestone** — the config is `replaceMinedBlocksWith`, defaulted to `minecraft:cobblestone`. What makes a vein
*feel* infinite is scale: cluster sizes of 32–40 on a 3-chunk grid, under a player hand-mining a
face rather than draining a patch.

So **physical block removal is the depletion mechanism**, and this ADR builds no second one. This is
not a shortcut; it is the strongest available answer. A patch that empties is legible in a way no
counter is — the player sees the hole. A yield counter layered on top of blocks that *also*
disappear gives the same patch two readings that can disagree, and the counter always wins the
argument while the player believes their eyes.

Everything downstream follows from this. There is **one rule for all of Terra's ore**: the starting
patches and the prospected outfield differ by size and access, not by mechanic. A small surface
patch worked by hand empties in an hour; a full buried vein under a miner takes many. That
difference is real without being authored twice.

## Terra is the bad way to get metal

Factorio's Nauvis ore never stops being minable. It stops being *worth* mining, because Vulcanus
gives effectively infinite metal at a better ratio for less infrastructure. The player does not
leave Nauvis because Nauvis is empty. They leave because Nauvis is slow.

Terra copies this, and it is why the pack needs no on-planet rescue mechanic:

- **Terra's ore→ingot ratio is the pack's deliberate floor.** Every later body improves on it. The
  scarcity the player feels early is a *processing* story, not a thin-patch story — which is what
  keeps it from ever becoming a wall (see below). ADR-0017 already puts extraction and assembly on
  GregTech, so the baseline chain is a GregTech chain and every body measures itself against it.
- **The constraint moves as the factory matures.** Once big miners, foundries and electromagnetic
  plants are unlocked and mass-produced, processing stops being the problem. What binds then is raw
  ore **throughput** — how fast Terra's ground gives ore up.
- **The pressure eases from outside, not inside.** As other planets' factories take over what Terra
  does worst — circuits, plastics — Terra's own inadequacy stops mattering, because Terra stops
  having to do those things.

**Terra's maximum sustainable ore throughput is bounded, and deliberately below what a mature
factory wants.** That is a design principle this ADR states and later tuning must respect. It is the
claim that makes departure land at the right moment: the player leaves not because Terra is empty
but because Terra cannot feed the factory fast enough.

**There is no on-planet fix, and that is the point.** No void miner, no deep drill, no late
Terra-only rescue. The off-ramp is **departure**. What body keeps that promise is that body's own
puzzle and is out of scope here; this ADR only records that the promise is kept off-world.

## Pressure, never a wall

The rocket is built **on** Terra. Every gram of metal for the 20–25 hour arc comes out of Terra's
ground, because the bodies that fix Terra are reachable only after launch.

So: **Terra's ore must comfortably outlast the launch.** Scarcity is a felt pressure, not a real
wall. A player eighteen hours in with no ore and no rocket has no move but a new world, and that
failure is unrecoverable in a way no amount of atmosphere pays for. The outfield is finite and
generous; no reasonable player is ore-blocked before launch. The *feeling* of scarcity comes from
what an ingot costs, not from running out.

The starting patches are the exception, and they are supposed to run out — that is the transition.
Which gives the **hard invariant**:

> The starting patches must not die before the player can prospect and place a miner.

Ordering, not duration. The target duration — the starting patches carry the player to the first
automated mining setup and no further, roughly the first 2–3 hours — rides along as a figure to tune
against, explicitly tuning rather than a discrete choice, exactly as the 20–25 hour arc figure is.

## The starting patches

ADR-0019 fixes the starting area as a spawn-anchored structure with a fixed resource set and a
randomized layout and patch sizes. Two things are settled here.

**The materials: iron, copper, zinc, tin and coal.** The set is chosen to bootstrap the three things
the opening needs — brass for Create, bronze for steam, and an LV miner — and it maps onto veins
Terra already registers: `gtceu:iron`, `gtceu:copper`, `planetaryfactory:sphalerite`,
`gtceu:cassiterite`, `gtceu:coal`. Nothing new is registered for it.

This membership is a **starting configuration, not an invariant** — it needs playtest. What ADR-0019
fixes is that the set does not vary *by seed*; whether these five are the right five is a question
for a player, not for a document. Terra's other veins — galena, magnetite, nickel, mica, olivine —
are currently outside it, which makes everything past bronze gated on prospecting. If playtest says
that gate lands wrong, the set moves.

**The patches are ore veins pinned to spawn, not ore placed by the structure.** The structure
anchors placement; the patches themselves are ordinary GregTech veins. This matters for a mechanical
reason: the map readout below is written against `GeneratedVeinMetadata`, which hand-placed ore in a
structure would not have. Authoring the patches as blobs would make the tutorial area the one place
in the world where the map readout silently does not work. Pinning veins also keeps the starting
patches and the outfield the *same object* at different sizes, which is the one-rule claim holding
all the way down.

## Reading remaining ore

A patch whose depletion is invisible is a trap, not a decision. Factorio reads remaining ore **on
the map**, and Terra does the same. Almost all of this already ships:

- **FTB Chunks is in the pack** (`ftb-chunks-neoforge-2101.1.21.jar`), and GregTech injects its ore
  layer into FTB Chunks' large map screen through `LargeMapScreenMixin`. The map surface is live
  today.
- **The Prospector fills a server-side saved cache** (`ServerCache` → `ServerCacheSavedData`),
  mirrored to a per-world client cache on disk (`ClientCacheManager`). Readings are **snapshots
  taken at scan time** — nothing re-reads the world afterwards. This is a real limitation and it is
  accepted: the map tells you what *was* there and, with the flag below, whether it is finished.
- **`GeneratedVeinMetadata` already carries a `depleted` boolean**, codec'd, saved, sent to the
  client, and honored by every map renderer GregTech ships. `OreVeinIcon` reads it, and
  `FTBChunksOptions.hideDepleted` lets a player hide worked-out sites entirely.
- **Bedrock deposits already have a live readout** on the miner: `gtceu.machine.bedrock_ore_miner.depletion`
  renders a depletion percentage, backed by `BedrockOreVeinSavedData`.

The one gap: **stock GregTech never sets `depleted` itself.** The only writers are worldgen and a
debug command; what players get is a manual "Mark as Depleted" button on the map icon.

So `planetaryfactory_core` **flips the flag automatically when a GregTech Miner exhausts its working
area**. The miner that drained the vein is the machine that knows, it has the information for free,
and it is already the outfield verb ADR-0019 chose. The flag is set by an *actual failure to find
ore* — never by a parallel counter — which is the same honesty the whole design rests on: the map
cannot lie about a patch, because the map is reporting the world rather than a bookkeeping entry.

Manual marking alone was rejected as clerical, and an unmarked dead vein is exactly the trap this
section exists to prevent.

## Bedrock deposits keep their tail

The three `terra_*` deposits are unchanged: `terra_ferrous_deposit` starts at a 24–48 yield, loses 1
per extraction at a 20% chance, and floors at `depleted_yield: 4` forever. `infiniteBedrockOresFluids`
stays `false`.

This is the one place the pack says two different things about two different resources, and it is
deliberate: **the ore in the ground runs out; the deposit under the bedrock is a trickle you tap
forever.** The tail's job is to stop any single outpost from being a *mistake* — an old site stays
weakly productive, so building there was never wrong.

The reasoning is written down because the number will be tempting later. Against a mature factory's
appetite, **4 is noise** — it cannot threaten the throughput ceiling, which is precisely why it is
safe. Raise it and it stops being a consolation and starts being a reason to stay.

## Considered Options

- **A per-patch yield counter, or ore blocks that thin out as they are mined.** Rejected under the
  two-readings argument: a counter competes with the hole in the ground, and the player believes the
  hole.
- **Different curves for the starting patches and the outfield.** Rejected. Under physical removal
  there is no second curve to assign — the difference is size and access, and authoring two rules
  would be a second mechanism returning through the back door.
- **A late Terra-only rescue: void miner, deep drill, asteroid harvesting on Terra.** Rejected. It
  is the on-planet fix Factorio deliberately does not have, and it defuses the pressure that makes
  the first launch mean anything.
- **Tuning scarcity through thin patches rather than lossy processing.** Rejected: it cannot satisfy
  the no-wall constraint. Thin patches leave a player empty-handed; a bad ratio leaves them slow,
  and slow is recoverable.
- **Hard-exhausting the bedrock deposits too, for consistency.** Rejected — the tail is what makes
  an old outpost a decision rather than a regret, and 4 is too small to undermine departure.
- **Adopting a map mod for the readout.** Moot, and nearly a mistake: FTB Chunks is already in the
  pack and GregTech already renders into it. Checked before deciding.
- **Manual "Mark as Depleted" only.** Rejected as clerical, though it remains available and costs
  nothing.
- **A three-state map — untouched / being worked / dead.** Deferred. It is where this wants to go,
  but it needs a field GregTech does not have, and a three-state map is a bigger teaching load than
  this transition needs.

## Consequences

- **`planetaryfactory_core` gains a miner-side depletion hook** — the automatic `depleted` flip. Per
  ADR-0015 this is mechanism and belongs in the mod, not in KubeJS.
- **The starting patches need spawn-pinned vein placement**, which is a structure that anchors vein
  generation rather than one that places ore. This is new mechanism and it is the build's problem,
  not this ADR's.
- **The prospected map can go stale.** Snapshots plus a `depleted` flag do not model partial
  extraction, so a half-worked vein reads as untouched. Accepted for now; the three-state map is the
  fix if playtest asks for it.
- **The starting material set is provisional and playtest owns it.** Recorded here so a later change
  reads as tuning rather than as contradicting a decision.
- **The throughput ceiling binds later tuning.** Vein density, miner rate and grid spacing on Terra
  are not free parameters — they answer to the ceiling, and raising them to solve a mid-game pinch
  would quietly delete the reason to launch.
- **Terra's ore→ingot ratio is now a stated floor**, and every later body's processing is measured
  against it. This constrains recipe work on bodies this map does not reach.
- **None of this is registry-checkable.** Depletion, the ceiling and the no-wall guarantee are all
  claims about play, verified by a human playing, in the same category ADR-0019 put flatness and
  cave-freeness. Recorded as a decision, not skipped.
- **Save invalidation is not a cost.** The pack is pre-release.

## Amended by #57: a derived yield readout is not a counter

**No counter, but a derived readout is not a counter.** The map states each charted patch's
**material, `depleted` flag and remaining yield**, because Factorio always states yield and that
overrides the instinct to show only a binary.

The reasoning above — *a counter competes with the hole in the ground, and the player believes the
hole* — is not overturned. It is the argument for **deriving** the number rather than storing one.
Yield is counted on demand from the ore blocks actually present in the vein's bounds, when a chunk
is charted; it is a *view of* the hole, not a rival to it, which is exactly how Factorio's own yield
works — summed from the entities that are there. What the Considered Options section rejects is a
**persisted** per-patch counter, which can disagree with the world; a number recomputed from the
world cannot.

Deriving is also the only option that adds no persisted state: `GeneratedVeinMetadata` carries
`originChunk`, `center`, `definition` and `depleted` and nothing else. There is no remaining-count
field, and nothing in GregTech computes one.

`depleted` is unchanged — the cheap binary the Miner flips when it fails to find ore. It is not
replaced by the yield number, and the two cannot drift: **yield 0 and `depleted` agree by
construction**, because both are answers about the same blocks. Snapshot staleness is unchanged too
(a half-worked vein still reads as it was last charted), and is accepted above.

**Bedrock deposits need no change.** They already carry yield —
`ProspectorMode.OreInfo(material, weight, left, yield)` — and the miner already renders a depletion
percentage. Only ore veins needed this decision.

## Amended by ADR-0021: the starting materials

**The starting patches are iron, copper and coal.** ADR-0021 cuts Terra's ore to Nauvis's set, and
zinc and tin are cut with it, so the five above are drawn from a pool that no longer contains them.
Uranium exists on Terra but is deliberately excluded from the starting area, as Factorio excludes it.

This is not the playtest tuning the section anticipated: the pool changed, not the judgement about
which of it belongs at spawn. The bootstrap targets named above — brass for Create, bronze for steam
— are casualties of that cut and are re-specified elsewhere; they are not an argument for keeping
the materials. Everything else here stands, including that the patches are spawn-pinned veins and
that membership does not vary by seed.
