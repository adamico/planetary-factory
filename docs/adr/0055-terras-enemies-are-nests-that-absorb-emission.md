---
status: provisional
supersedes: [109, 110]
---

# Terra's enemies are nests that absorb emission, and the waves they send travel as data

Factorio's enemy loop is one mechanism, not two. Nests (`unit-spawner`) sit on the map, the
pollution cloud spreads chunk by chunk, a nest in a polluted chunk **absorbs** that pollution, and
absorbed pollution is what becomes an attack group. There is no separate raid system; the raid
*is* the nest's output. A global evolution factor — a function of time, pollution produced and
spawners killed — decides which unit tier the group is made of, and expansion sends groups to
found new nests on its own timer, independent of the cloud.

**The pack reproduces that loop.** Nests, absorption, evolution, expansion, and attacks that come
out of a nest you could have gone and cleared.

This supersedes ADR-0005 in two places.

## What ADR-0005 got, and what it got wrong

ADR-0005 was right about the thing it was written for: GTCEu 7.0.2 has no pollution system, so
Emission is ours. That stands unchanged.

Two of its decisions do not.

**Emission is scored per entity, not from EU/t draw.** ADR-0005 chose power draw and rejected
per-entity rates because they would cost "tagging every recipe in a GregTech pack, with a
permanent maintenance burden as recipes change". Factorio states emission per prototype as
`energy_source.emissions_per_minute`, and that field is in the `--dump-data` dump the seven
existing extractors already read. Extraction is not tagging; it regenerates with the corpus and
maintains itself. The option was rejected against a manual-labour cost that the corpus pipeline —
built *after* ADR-0005 — removed. Per ADR-0054 the corpus is the default answer and the only
argument for the proxy has gone, so the proxy goes.

The consequence ADR-0005 could not see: EU/t misranks entities badly. A boiler pollutes far more
per joule than an assembler, and an idle machine emits its idle rate rather than zero. Under the
proxy a coal-fired base and an electric one of the same draw are equally dirty, which is the
opposite of the thing the mechanic is supposed to teach.

**The consequence of emission is nests, not Illager raids.** ADR-0005 states "Only Terra converts
emission into Illager raids", and names the Overseer, Command Center, Cryo-Pod and Dormant Siege
as the systems that carry it. Those exist because `minecraft:raid` is village-anchored: vanilla's
raid needs something to path *at*, so the design supplied a stationary `NoAI` villager for it to
target. Write our own delivery and the anchor has nothing to do. **The Overseer, the Command
Center, the Cryo-Pod and the kill-switch are cut.**

ADR-0005 listed "drop the hazard loop" as a considered option and rejected it partly because it
would remove those four systems along with the Sapros organics chain feeding them. This ADR
removes the four **without** dropping the hazard loop — so Sapros keeps its chain and loses the
customer ADR-0005 named for it. That is a real consequence and it is not settled here; it is
filed against Sapros rather than answered by a combat decision.

## The three things Minecraft cannot do, and the one idea that answers them

Factorio simulates what Minecraft unloads. Three specific breaks, and the same resolution each
time — **the mechanism is data, and entities are only its rendering**:

- **A nest in an unloaded chunk would never absorb, expand or attack.** So a nest is a saved-data
  record — position, accumulated emission, tier — ticking arithmetically. Mobs instantiate when a
  player is there to see them. This is the Dormant Siege's own idea, and it is cheaper applied to
  a nest (a point with a number) than to a raid (a group of mobs).
- **Attack groups cannot walk kilometres.** A raider abandons its raid beyond **112 blocks**, and
  mob pathfinding gives up long before Factorio's distances. So a wave is a record too — origin
  nest, target chunk, tier, size, ETA — advancing at the units' own extracted `movement_speed`
  and instantiating as entities when it reaches the player it is aimed at. All three of Factorio's
  phases survive with Factorio's own parameters: the group gathers at its nest, takes real travel
  time proportional to real distance, and is interceptable once it is close enough for anyone to
  see. The only divergence is that the travel phase is not *drawn* the whole way.
- **There is no home for a global evolution scalar.** `SavedData`, shared with Emission, which
  needs exactly the same thing.

Because instantiation is relative to the **player** the wave is aimed at, simulation distance is
not a constraint on correctness — anything close enough to matter is ticking by construction. It
affects only how much warning a player gets, which is a UX question and not this ADR's.

## The rules

- **Emission per entity**, from `emissions_per_minute`, extracted. Per-chunk accumulation, decay
  and diffusion are unchanged from ADR-0005.
- **Nests are saved data** with worldgen placement whose density rises with distance from origin,
  as Factorio's does.
- **Absorption**: a nest in an emitting chunk absorbs from it, and absorbed emission is what buys
  attack groups.
- **Evolution is one global scalar** driven by all three of Factorio's inputs — time, emission
  produced, and nests destroyed. The third is counter-intuitive and it is the reason clearing the
  map is not a permanent win.
- **Expansion runs on its own timer**, not on the cloud, capped by the same distance-density rule
  that placed the original nests — an uncapped registry grows forever.
- **Waves travel as data and instantiate near their target.**
- **Enemies damage only blocks tagged `planetaryfactory:destructible`.** Factorio's biters eat
  walls and turrets; Minecraft mobs grief nothing. Unrestricted destruction is not fidelity here,
  it is an unbounded loss with no repair mechanic underneath it, so the blast radius is a tag.
- **Every number above is extracted, not chosen.** Absorption rates, the evolution coefficients,
  expansion cooldowns, group sizes and unit speeds are corpus values; a divergence needs its own
  ADR under ADR-0054.

## Consequences

- ADR-0005's Overseer/Command Center/Cryo-Pod line is cut, and `docs/gdd.md` §6 describing it is
  stale prose with no standing (ADR-0054).
- Sapros loses the customer ADR-0005 gave it. Filed separately; not answered here.
- `docs/factorio-mechanics.md`'s **Enemies and evolution** row keeps `planned` but its shape
  changes wholesale, and its `Nests, expansion and clearing territory` sub-rule stops being
  `excluded`/`by-consequence`. **Evolution factor** stops being `blocked`.
- Emission's row gains a notice: the ADR-0005 EU/t model was superseded before it shipped.
- An eighth extractor and an eighth corpus file are required before any of this can be built,
  because every number here is currently a claim about a file that does not exist yet.
- The arithmetic — absorption, evolution, travel time, group composition — is Minecraft-free and
  unit-testable the way `FellingCostTest` and `BoilerSpecTest` are. Whether a wave arrives and a
  nest spawns is a world load, and its GameTests are filed rather than assumed.

## Considered alternatives

- **Keep raids, keep the Overseer** (ADR-0005 as written). Attacks with no source: nothing to
  clear, nothing to intercept, and the only lever is producing less. It is a different game from
  the one the pack is reproducing, and it was never argued — it followed from borrowing vanilla's
  raid system.
- **Both nests and raids.** Two systems competing for one emission number, and a player who
  cannot tell which of them their pollution just fed. Factorio has one mechanism; this would be
  Factorio plus an invention.
- **Nests with no expansion** — a finite, clearable map-wide budget. Simpler, and it makes
  "clear everything once" a permanent win, which removes the reason to keep defending.
- **Mobs teleport in at the target**, nests as pure bookkeeping. Cheapest, and it destroys the
  reading that a wave came from somewhere you could have cleared — which is the whole offensive
  half of the loop.
- **Nests sited close enough for waves to walk.** Full local fidelity, at the cost of a map that
  is wall-to-wall nests and a distance-from-origin density rule that no longer means anything.
