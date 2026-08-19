---
status: accepted
---

# Emission is ours, because GTCEu Modern has no pollution

The design's hazard loop is Factorio's: the factory produces something noxious, and the noxious
thing attracts an attack. The original draft assigned that to "GT:M's native chunk-based pollution
system". No such system exists — `gtceu-1.21.1-7.0.2.jar` contains not one class or resource
matching `pollut`. Chunk pollution belongs to GregTech 5 Unofficial and to GT:CE, neither of which
is this mod.

We build it ourselves, as **Emission**: a per-chunk score accumulated from the EU/t draw of running
GT machines, decaying over time and diffusing to neighbouring chunks.

Power draw is the measure because every GT machine already exposes it, so no recipe, machine or
material needs tagging, and any machine added later — ours, GCyR's, a future addon's — participates
automatically. Decay and diffusion are what make the score a spatial problem: without them, outpost
placement is irrelevant and emission is a counter that only goes up.

## Considered Options

- **Add a pollution mod.** There is no 1.21.1 chunk-pollution mod to add. This was checked before
  the rest of the decision was made.
- **Drop the hazard loop.** Coherent, and it removes the Overseer, Command Center, Cryo-Pod and
  Dormant Siege systems along with the Sapros organics chain that feeds them — which is most of the
  reason Sapros exists.
- **Score dirty recipe outputs instead of power draw.** More expressive and better-targeted, at the
  cost of tagging every recipe in a GregTech pack, with a permanent maintenance burden as recipes
  change. Power draw is a coarser proxy that costs nothing to maintain.
- **Per-outpost totals with no spatial component.** Cheaper to compute and it makes the score a
  progress bar rather than a reason to think about where things go.

## Consequences

Emission is a KubeJS subsystem the pack owns and tunes, with no upstream to inherit balance from.
Accumulation, decay and diffusion rates are all ours to discover.

The scoring runs against loaded chunks; unloaded outposts accumulate arithmetically as part of the
Dormant Siege model, so the two systems are coupled and must agree on rates.

Only Terra converts emission into Illager raids. Other planets accumulate it and convert it into a
planet-appropriate consequence — those consequences are named in the GDD but not yet specified,
which is the largest open question the model leaves behind.

The term is **Emission**, not pollution, precisely so that nobody reading the scripts assumes a
GregTech feature is behind it.
