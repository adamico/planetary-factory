---
status: accepted
---

# Sapros registers no ore veins, and its metal is unobtainable until the Decay engine ships

`docs/research/gleba-worldgen.md`, transcribed from the Factorio wiki, gives Gleba copper and
iron **stromatolites** "in abundance" in the red and green marshlands, and calls them "good
sources of ore and ore bacteria". What a stromatolite yields is **ore bacteria**, and a bacterium
becomes metal by *spoiling into it*. There is no vein, no drill and no furnace in that sentence.

So **Sapros registers no GregTech ore veins, no bedrock ore deposit and no bedrock fluid
deposit.** Everything metallic on the body comes off a hand-mined surface block as bacteria plus
stone, and turns into metal only by Decaying (ADR-0010, ADR-0011).

## This is not Electro's emptiness

Both bodies are barren of veins, and a reader who conflates them will implement the wrong fix
for the wrong symptom:

- **Electro has no metal at all.** Its economy is recycling: scrap in, a spread of unrelated
  outputs back (ADR-0009). Nothing in Electro's ground is metal waiting for a mechanic.
- **Sapros has metal, arriving through time rather than through the ground.** The stromatolites
  are dense and generous. The gap between mining one and holding iron is the Decay engine, and
  it is the puzzle, not a shortfall in worldgen.

## The consequence to state plainly: Sapros's metal is inaccessible until #17

On the day the body ships, the Decay engine does not exist. A player can mine a stromatolite,
receive `iron_bacteria_fresh`, and do nothing whatsoever with it. That is expected, and it is
recorded here, in `kubejs/startup_scripts/items.js`, in `kubejs/startup_scripts/blocks.js` and
in the `sapros` entry of `tests/worldgen/expected.json`, because the obvious "fix" —
a smelting recipe, or a loot table that drops ore directly — deletes the mechanic the whole
body exists to carry. The body is complete on its own terms: it generates its resources
correctly. What consumes them is #17's.

## Sapros still has a worldgen layer

`sapros_rock` matches `gcyr:mercury_rock` and is scoped to `planetaryfactory:gleba`, and it
places nothing, because nothing references it. Same reasoning as Electro's `electro_rock`: the
layer is what gives the body a tab in GregTech's prospecting tooling, and a player who prospects
Sapros and is told there are no veins has learned the design, where a player told nothing at all
has found what looks like a bug. Because no vein names the layer, the fixture asserts it
directly, through a `worldgen_layer` field.

## The stone claim

Per ADR-0008, Sapros claims **`gcyr:mercury_rock`** — Ignus took `gcyr:venus_rock`, Electro took
`gcyr:martian_rock`, and `gcyr:moon_stone` is left unclaimed for Gelida. GCyR's lang keys for the
Mercury block set are overridden in `kubejs/assets/gcyr/lang/en_us.json`, so no player sees the
name of a body this pack deleted. The claim buys the ore-variant tier it always buys; Sapros
simply never spends it.

## Considered Options

- **A few weak iron and copper veins, so a pickaxe works on Sapros.** Rejected: it makes Decay
  optional on the one body built around it, and a player who finds a vein will never touch the
  bacteria chain again.
- **Stromatolites drop ore directly, with the bacteria as a bonus.** Same rejection, arrived at
  by a shorter road. This is the specific mistake ADR-0010's sequencing invites, which is why
  the loot tables are asserted by `tests/flora/test_flora_data.py`.
- **Hold the body back until #17 ships.** Rejected: a body that generates its resources
  correctly is complete, and blocking it would idle the five biomes, the two trees and the
  terrain behind an engine on its own schedule. The cost is one gap, recorded here.
- **A bedrock ore deposit as a scaled fallback, as Electro has.** Rejected for the same reason
  as the veins, and it would also give Sapros an ore chain with no biological step in it.

## Consequences

Between this ticket and #17, Sapros is a body a player can farm on and cannot smelt on. Anyone
reading `kubejs/data/planetaryfactory/gtceu/` and finding no Sapros files should find this ADR
before they conclude something was forgotten. The fixture's three empty objects — `ore_veins`,
`bedrock_ores`, `bedrock_fluids` — are the executable half of that claim: each makes the check
walk the entire loaded registry and fail if anything at all reaches `planetaryfactory:gleba`.
