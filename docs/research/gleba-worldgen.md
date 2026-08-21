# Gleba worldgen, transcribed from the Factorio wiki

**Source**: <https://wiki.factorio.com/Gleba>
**Retrieved**: 2026-08-21

This document is the source of truth for Sapros's terrain, biomes and flora. It exists because
`docs/planets.md` is an unblessed scratch transcription that flattened all of the below to a single
line — "similar to overworld with high frequency of swamps" — and tickets reasoning from that line
reached conclusions this document overturns. Cite this file, not the scratch file, and not the wiki
directly: a wiki page changes under you, and an acceptance criterion needs something fixed to check
against.

Terminology below is the wiki's own. The mapping to the pack's vocabulary is in `CONTEXT.md`; the
naming rule is ADR-0004 as extended by ADR-0015.

## Biome families

The wiki names four families. The fourth is one family with two colours, and the colours are
load-bearing — they are what separates the two trees — so the pack registers **five** biomes.

| Wiki term                  | Pack identifier                | Display name       | What it is                                                    |
| -------------------------- | ------------------------------ | ------------------ | ------------------------------------------------------------- |
| Dark highlands             | `gleba_dark_highlands`         | Dark Highlands     | Elevated. Where stone is found.                                |
| Orange/turquoise midlands  | `gleba_midlands`               | Midlands           | Also elevated. Lacks shallow water.                            |
| Blue marshes               | `gleba_marshes`                | Marshes            | Typically found next to deep water lakes.                      |
| Green marshland            | `gleba_green_marshland`        | Green Marshland    | Marshland. **Yumako trees.** Stromatolites in abundance.        |
| Red marshland              | `gleba_red_marshland`          | Red Marshland      | Marshland. **Jellystem.** Stromatolites in abundance.           |

Direct quotes:

- "The dark highlands biome are where stone may be found."
- "The orange or turquoise midland biomes are also elevated and lack shallow water."
- "The blue biomes are marshes that are typically found next to deep water lakes."
- "The red and green biomes are marshlands where Jellystem and Yumako trees can be found."

Yumako grows in the green biomes; Jellystem grows in the red. Collapsing red and green into one
marshland puts both trees in one place, which is not Gleba — on Gleba a green grove and a red grove
are different destinations you route logistics between.

## Resources

"Copper and iron stromatolites" appear "in abundance" in the red and green marshland biomes, and are
"good sources of ore and ore bacteria."

Note what stromatolites yield: **ore bacteria**, not ore. The bacteria become metal by spoiling,
which is the Decay engine's job (ADR-0010, ADR-0011) and not worldgen's. A stromatolite that drops
ore directly deletes the mechanic Sapros exists to carry.

## Water and terrain

- Shallow water tiles, which "cannot directly be built upon without landfill".
- Deep water lakes.
- Cliffs typically mark transitions between elevation zones.

There is no oil, no crude and no petroleum deposit of any kind. Gleba replaces every petroleum
product biologically — biolubricant, bioplastic, bio rocket fuel — and that substitution belongs to
`Puzzle: Sapros`, not to the body. The body's job is to make the absence real.

## Soil taxonomy — out of scope for the body

Within plantable zones the wiki classifies tiles further:

- **Fertile soil tiles** — "those surrounding natural Jellystems and Yumako trees".
- **Wetland tiles** — "a larger number of wetland tiles" around the fertile soil.
- **Overgrowth tiles** — darker red or green tiles where overgrowth soils can be placed.

This is agricultural-tower territory and belongs to `Puzzle: Sapros`. It is recorded here so the
next reader knows it was seen and deliberately deferred, rather than missed.

## Flora and harvesting

Yumako and Jellystem are **real trees with structure**, not tall crops. On Gleba they are planted
from seeds and harvested by agricultural towers, and harvesting is destructive — the trees are not
an infinitely standing crop. The pack's equivalent is saplings plus a Create tree farm, which is why
a first-class `SaplingBlock` and `TreeGrower` are needed and why the pack acquired a mod for them
(ADR-0014).

The two harvests differ, and the asymmetry is the point:

- **Yumako** — the fruit is picked from fruiting leaves, and the leaves refruit.
- **Jellystem** — **Jellynut** comes from the stem blocks, taken from the trunk.

`Jelly` is what a Biochamber makes *from* Jellynut. It is not a worldgen item and no tree is named
after it.
