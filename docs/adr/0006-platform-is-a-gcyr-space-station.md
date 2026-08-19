---
status: accepted
---

# A Platform is a GCyR space station, and it never moves

GCyR ships `SpaceStationPackagerMachine`, `PacketCreateSpaceStation`, per-body orbit dimensions and
a world border sized by `spaceStationMaxSize`. The design's Platform is the same object: a
player-expanded orbital factory. We use GCyR's, rather than pasting our own structure into a void
dimension via KubeJS as an earlier draft proposed. The Orbital Starter Kit is GCyR's station
package item.

Platforms are static. They have no thrusters, no navigation computer, no interplanetary transit and
therefore no asteroid defence and no hull mass model. Players move between bodies in a GCyR
`RocketEntity`, paying GCyR's tiered fuel costs.

## Considered Options

- **Our own KubeJS structure-paste into a void dimension.** The original design. Rejected: it
  reimplements station creation, station dimensions and the world border, all of which GCyR already
  has, and it discards the `EntityEvents` intercept it needed along with them.
- **Mobile Platforms, as in Space Age.** More faithful — in Factorio the platform *is* the ship and
  rockets never leave the atmosphere. It requires a station to change which body it orbits, which
  GCyR has no concept of, plus a fuel-by-mass model, plus asteroid attrition, plus a defence
  mechanic for which no weapons mod is installed. Rejected on cost, not on merit.
- **Mobile for the endgame only, to reach Atlantis.** Rejected for the same cost against a single
  destination. Atlantis is instead gated on establishing a Platform in its orbit by unmanned
  logistics, which is a good final beat in its own right.

## Consequences

The GDD's "Interplanetary Player Transit", Navigation Computer, Thruster Arrays and Asteroid
Defense Systems are cut. Navigation Computer is removed from the glossary.

`spaceStationMaxSize` stays at GCyR's default 512. The config warns it must not change once a
station world has loaded, so the value is effectively permanent from the first real save; the
current `saves/New World` is disposable test state and does not constrain it. 512 is not a design
constraint we chose to lean on — Factorio platforms are unbounded, and mass is their real cost —
but with static Platforms there is no mass model for a larger station to interact with, so the
default stands.

Atlantis has no surface. Reaching an orbit-only destination requires a Platform there first, which
means the unmanned cargo and Vanguard-equivalent path must work before any player visit.

Cargo terminals are ours regardless: GCyR has `launch_pad` and `RocketEntity` but no cargo terminal
concept, so Launch Terminal, Receiving Terminal and Drop Hatch are custom GT machines built on
GTCEu's KubeJS machine builders.
