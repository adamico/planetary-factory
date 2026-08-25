---
status: accepted
---

# GregTech worldgen belongs to planets, so the Nether and the End keep their dimensions and lose their veins

The pack's progression is planetary: build a rocket, pay fuel priced by distance, arrive somewhere
hostile, establish a supply line. Two vanilla dimensions sit outside that entirely, reachable
through a portal at no fuel cost and no rocket tier, and GregTech populates both.

`gtceu-1.21.1-7.0.2.jar` ships 41 ore veins. Twelve are filtered to `minecraft:the_nether` —
sulfur, tetrahedrite, molybdenum, monazite, beryllium, certus quartz, banded iron, saltpeter, topaz,
quartz, redstone and manganese. Six are filtered to `minecraft:the_end` — naquadah, pitchblende,
scheelite, sheldonite, bauxite and magnetite. The single bedrock lava deposit is Nether-only.

Left alone, that is a complete parallel endgame behind two portals. It collides with the planetary
design at the exact points the design is built on: scheelite is GregTech's tungsten ore and the End
has it, while `docs/scratch/planets.md` makes tungsten the reason Ignus exists; the Nether holds
the infinite lava that Ignus's thermal puzzle is built around; and the End's naquadah and platinum
group are the endgame materials that reaching Atlantis is supposed to be about.

**GregTech worldgen belongs to planets.** The Nether and the End keep their dimensions, their
portals, their mobs and their vanilla resources. They lose their GregTech veins and bedrock
deposits, and those materials are redistributed to bodies you fly to.

Redistribution is decided per body rather than in one pass: each `Body:` ticket claims the materials
that suit it and says why. Ignus taking scheelite and the lava deposit is the clearest case and is
already implied by the source document.

## Considered Options

- **Leave both dimensions as they are.** Costs nothing and undercuts the whole planetary
  progression: a player with an End portal has no reason to build a tier-2 rocket, and Ignus,
  Gelida and the fuel-cost model lose their point.
- **Close the portals.** Thematically cleanest, since Factorio has no Nether, and by far the most
  expensive: blaze powder, netherite, ender pearls and every recipe in the stack that assumes them
  would need pack-authored replacements. A large blast radius for a purity win.
- **Keep Nether veins, strip only the End.** Halfway house. It preserves the sharpest conflicts —
  Nether lava against Ignus, Nether sulfur against Ignus's acid chain — while still paying for the
  decision.
- **Redistribute everything in one ticket.** Rejected in favour of per-body claims, because deciding
  eighteen materials' homes at once means deciding them without the context of the body receiving
  them.

## Consequences

Every GregTech material currently exclusive to the Nether or the End must find a planetary home, or
it becomes unobtainable. The per-body redistribution makes that a real risk: the last body to ship
must be checked against the full list, and anything unclaimed is a gap. That check belongs in the
final body ticket.

The Nether becomes a mob-and-vanilla-materials dimension rather than a resource tier. Its worldgen
still exists; nothing GregTech generates does.

Atlantis gains a candidate purpose. `docs/gdd.md` §8 leaves its mechanics deliberately open, and
"the only source of naquadah" is a puzzle-shaped hole an orbit-only endgame destination could fill.
This ADR does not decide that — it notes that the option now exists.

Removal uses the same mechanism as slice 1's stock-body removal: the datapack registry entries are
blocked by a `pack.mcmeta` filter in a zipped datapack under `kubejs/data/`, per
`docs/research/gcyr-planet-definition.md`. No fork change.

Players arriving from other GregTech packs will find the Nether and End empty of veins, which is
contrary to universal expectation in this mod's ecosystem. That is a deliberate signal that the
progression is elsewhere, and it needs saying in the quest book rather than being left as a
surprise.

## Amended by ADR-0019 for Terra

ADR-0019 makes Terra flat, cave-free and shallow, and this decision's consequences on that body move
with it. The decision itself is unchanged: worldgen still belongs to planets, and the Nether and End
are still empty.

What changes on Terra: the world column becomes `0..192`, so **negative Y ceases to exist** and the
`deepslate` layer is retired there — the eight veins that lived on it either move into the shallow
band above bedrock or are cut. Every surviving vein's `height_range` is reassigned, since several
currently start below Y 0. The other bodies are untouched; the `deepslate` retirement is Terra-only.

The `terra_*` bedrock deposits keep working — they key off the bedrock layer, which is now Y 0.

The practical consequence for anyone reading this ADR to place a vein: **Terra's Y-ranges are no
longer GregTech's**, and the shallow band is deliberate — ore has to be reachable without caving,
because Terra has no caves.
