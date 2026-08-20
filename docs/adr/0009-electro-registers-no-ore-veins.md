---
status: accepted
---

# Electro registers no ore veins, and the test fixture asserts that against the whole registry

`docs/gdd.md` §2 and `docs/scratch/planets.md` both give Fulgora no natural ores. Its economy is
recycling: scrap in, a spread of unrelated outputs back. A body with veins would have a second,
duller way to get materials, and the recycling chain would become optional decoration.

So **Electro registers no GregTech ore veins at all** — not a reduced set, not a low-weight set.
Prospecting it returns nothing.

The problem with an absence is that it is indistinguishable from an omission. A reader three
tickets from now, looking at `kubejs/data/planetaryfactory/gtceu/ore_vein/` and finding no Electro
files, cannot tell whether the body was designed barren or whether someone forgot. Three things
say it deliberately:

- this ADR;
- a `_comment` in `tests/worldgen/expected.json` pointing here;
- the fixture entry itself, whose `ore_veins` is an empty object rather than an absent key.

**The empty object is an assertion, not a blank.** `scripts/worldgen-check.py` reads a present-but-
empty `ore_veins` as "no vein in the loaded registry may reach this dimension" and walks the entire
registry to prove it. The alternative — listing every vein in the pack under `forbidden_ore_veins` —
is a list that is silently wrong the moment another body ticket adds a vein. GregTech's ore vein
codec makes `dimension_filter` a required field, so a vein reaches a body only by naming it; the
walk is exhaustive rather than a heuristic.

**Electro still has a worldgen layer.** `electro_rock` matches `gcyr:martian_rock` and is scoped to
`planetaryfactory:fulgora`, and it places nothing, because nothing references it. It exists so the
body has a tab in GregTech's prospecting tooling: a player who prospects Electro and is told there
are no veins has learned the design, where a player told nothing at all has found what looks like a
bug.

## What barren does not mean

Three things on Electro yield materials, and none of them is an ore vein:

- **A bedrock scrap deposit**, worked by a Bedrock Ore Miner. Permanent, diminishing, machine-fed —
  the scaled half of Electro's economy.
- **A heavy oil bedrock fluid deposit**, drilled by a Fluid Drilling Rig.
- **Fulgorite**, a sparse surface-exposed *placed feature* in the barren plateau biome, hand-mined
  for holmium. A feature, not a vein: it is placed by the biome's feature list, has no worldgen
  layer, no material weighting and no prospecting entry, and so does not contradict the assertion
  above.

Surface ruins and scrap piles are likewise structures and features. "No ore veins" is a statement
about GregTech's vein registry, not about whether the ground contains anything.

## Considered Options

- **A few low-weight veins as a safety net.** Rejected: it makes the recycling chain optional, which
  is the one thing Electro exists to prevent. If arriving on Electro feels starved, the lever is the
  density of hand-mineable scrap piles, which is a number in a placed feature and costs nothing to
  turn up.
- **Bedrock at y=0 to shorten the climb to the deposit.** Rejected on a factual error: GregTech's
  Bedrock Ore Miner does not have to reach bedrock by hand, so the gate is the MV multiblock, not
  the depth. Raising bedrock would change nothing about progression.
- **`forbidden_ore_veins` listing every vein in the pack.** Rejected as above — stale on the next
  body ticket, and silently so.
- **No fixture entry for Electro at all.** The cheapest option and the worst: a body with no
  expectations is a body the check cannot fail, which is exactly backwards for the one body whose
  defining property is what it does *not* have.

## Consequences

**Every material a player needs on Electro must arrive by rocket or come out of scrap.** That is the
intent, and it makes the `Puzzle: Electro` ticket load-bearing rather than decorative: until scrap
recycling exists, Electro is a body you can stand on and not much else.

**Fulgorite is the one hand-mined material source, and it is holmium-only.** GregTech registers
`gtceu:holmium` with no item forms at all — no dust, no ingot, no ore — so this pack adds `dust()`
by material modification. Holmium at scale, the 1% from scrap recycling, belongs to the puzzle
ticket.

**Adding a vein to Electro later fails the build, loudly.** That is the point. If a future ticket
genuinely wants one, it changes this ADR first and the fixture second.

**The terrain has no water at all.** Its oceans are `gtceu:heavy_oil` and aquifers are disabled, so
there is no water table to fall back on either. What a player does about that — ice from scrap, per
the source document — is the puzzle ticket's problem.
