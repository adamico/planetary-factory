---
status: accepted
supersedes: [42, 91]
---

# Mekanism leaves the pack, and FE stops being a currency

ADR-0017 adopted Mekanism as one of Terra's three tech mods and gave it seven rows of the ownership
table. Fifteen months of amendments have taken all seven back, one at a time and each for its own
good reason, and nobody stopped to ask what was left. The answer, checked rather than assumed:
**Mekanism owns no recipe shelf at all.** `data/pack/subgroup-owner.json` has zero rows with
`owner: mekanism` — the counts are `create` 7, `gregtech` 1, `pack` 4, `split` 5, `undecided` 6,
`not_emitted` 11 — and line 302 already says so in capitals, crediting `#104` for making it
deliberate rather than incidental.

**Mekanism is removed from the manifest.** Both entries go: `mekanism.pw.toml` and
`just-enough-mekanism-multiblocks.pw.toml`. Nothing else in the manifest names it.

## The attrition, so the next reader does not re-litigate it row by row

Every row is already decided elsewhere. This ADR adds no new argument for any of them; it observes
that together they leave nothing.

| ADR-0017 row | Where it went |
| --- | --- |
| Ore processing | Row deleted, ADR-0032. Purification, Injection, Washer and Crystallizer recipe-removed; ADR-0033 then cut the Dissolution Chamber, taking the chain to **zero blocks** |
| Power generation | **Zero blocks** — `#104`. The pack installs base Mekanism, which registers no generator; every generator lives in MekanismGenerators, the jar ADR-0033 refused |
| Fluid logistics, bulk storage (fluid) | Create's outright, `#101`. Mechanical Pipes and the Dynamic Tank recipe-removed |
| Item logistics, bulk storage (item) | Create's. Logistical Transporters, Bins and QIO cut |
| Refining, Chemistry | The pack's Oil Refinery and Chemical Plant, ADR-0025 — no Mekanism machine has the two-fluids-in, three-fluids-out shape |
| Hand-crafting surface | Formulaic Assemblicator cut, `#34` |
| Uranium fuel chain | The pack registers its own Centrifuge, ADR-0033 and `#135` |
| HDPE and ethene | Superseded, ADR-0025. Plastic is Factorio's one step on `gtceu:polyethylene`; the `c:ethene` tag bridge is dropped |

**Three block roles survived**, and this ADR disposes of them: the Energized Smelter, Universal
Cables, and the Energy Cube with the Induction Matrix.

## The Energized Smelter becomes the Electric Furnace, a fourth pack-registered machine

`#91` mapped Factorio's three furnace tiers onto the vanilla Furnace, a `planetaryfactory_core`
block, and Mekanism's Energized Smelter renamed. The middle tier is unaffected. **The top tier
becomes a pack-registered machine on a GT chassis** — the ADR-0025 / ADR-0026 idiom that `#107` and
`#135` already execute three times between them.

**`#91`'s stated cost for this does not materialise, and it was wrong on its own terms.** Its
*Note to #69* argued that "a single-block Mekanism footprint is cheaper than the alternative, a
pack-authored GT machine forcing a fifth recipe type." There is no fifth recipe type. `#91`'s own
answer fixed the recipe type as vanilla `minecraft:smelting` precisely *because* three different
mods' furnace-shaped blocks read it without translation — and the Steel Furnace it authored in the
same breath is already a pack block reading vanilla smelting. The Electric Furnace is the same
trick a tier higher. What is actually paid is one more block class, on a chassis the pack has
tooling for.

Two things fall out for free. `production/smelting-machine`'s `undecided` row in
`data/pack/subgroup-owner.json` gets an owner. And `#42`'s conflict — a rung-4 science ingredient
that is a block the player has owned for three rungs — stops being a coincidence that `#91` had to
argue away, since the slot's item is now the pack's own.

**The upgrade slots go.** `#91`'s table gives the electric tier "upgrade slots", which were
Mekanism's upgrade economy. Nothing replaces them. The ladder was always declared speed-and-fuel
only, gating nothing, and the top tier's whole contribution is the removal of fuel handling
(`#126`'s correction: Factorio's `crafting_speed` is stone 1, steel 2, electric 2). An upgrade
economy was never part of that claim.

## `#42`'s slot list loses two entries, and one of them was already moving

The `chemical` pack's first slot was **Mekanism Sulfur Dust**. Sulfur is the Chemical Plant's
(ADR-0025), so the slot takes the pack's sulfur and the role is unchanged — this is a source change,
not a design change.

The `production` pack was **Energized Smelter + a Mekanism upgrade + Create Train Track**. The
smelter slot survives as the pack's Electric Furnace. **The upgrade slot has no successor and needs
none here**, because `#136` moved `production` behind the launch: Space Age's `rocket-silo` costs
1000 × (automation + logistic + chemical) and names no production pack anywhere. The slot lands on
the orbital arc unresolved, alongside the module problem `#120` already carries for it.

`#42`'s standing rules are untouched — slots take roles rather than transliterations, counts and
roles are fixed, quantities are free.

## FE is demoted from a currency to an endpoint format

This is the part that reaches furthest, so it is stated plainly rather than left to be inferred from
the cables' removal.

With Mekanism gone, **nothing distributes FE and nothing stores it**. The pack's energy currency is
EU on the machine side and volts on the grid side. FE survives only as the format some third-party
blocks happen to speak: AE2, Charging Gadgets, Building Gadgets. **They are fed where they are
touched.** There is no FE network and no FE storage block.

**ADR-0017's Energy storage row stops splitting on the Converter boundary.** It read "Electro
(Accumulator) grid-side; Mekanism (Energy Cube, Induction Matrix) FE-side". It now reads grid-side,
full stop. The table gets *shorter* — three rows collapse to two — rather than gaining a
replacement owner, and that is the shape of this whole decision.

**In-area distribution is not left empty**, and it is not handed to GregTech either. It becomes a
pack-authored supply-area pole, which is ADR-0036's subject and is a prerequisite of this removal
rather than a consequence of it. The two were decided in one session and are written as two
documents because they supersede different tickets; neither ships without the other.

## Considered Options

- **Keep Mekanism for the three surviving roles.** Rejected. A whole mod in the manifest for one
  furnace, one cable and one battery is the "kept but outclassed" failure ADR-0017 exists to reject,
  applied to a mod instead of a block. It also keeps ~500 blocks and items in JEI that no decision
  names, which is precisely the tail ADR-0034 is trying to shrink.
- **Hand in-area distribution to GregTech and keep the rest.** Rejected on the capability surface,
  and the reasoning is ADR-0036's. It also re-admits the voltage-tier ladder `#37` removed entire.
- **Adopt MekanismGenerators to give the mod a reason to stay.** Rejected twice already — ADR-0033
  refused it for the reactor row, and it brings six generators onto a row Electro owns. Adopting a
  second jar to justify the first is the argument backwards.
- **Keep the Energized Smelter only, cutting the rest by recipe.** Rejected: it is the same mod in
  the manifest, and the recipe-type argument that made it cheap turned out to be wrong.

## Consequences

- **The removal is net negative work.** ADR-0032 calls Mekanism's recipe removal "wide" — every ore
  recipe on four machines plus the Enrichment Chamber's. Deleting the jar deletes that work, and it
  shrinks the stock-recipe sweep and the undecided tail that `#136`'s ticket set will carry.
- **Amendment surface**: ADR-0017 (three table rows plus the Converter paragraph), ADR-0032,
  ADR-0033, ADR-0021's `config/Mekanism/world.toml` toggles and its `mekanism:dissolution` acid-gate
  prose, ADR-0025's residual sulfur routes. `docs/factorio-mechanics.md` rows that name Mekanism.
- **Code and data**: `scripts/build-terra-ore.py`'s `MEK_WORLD` block becomes dead — Mekanism's six
  worldgen toggles have nothing to switch off. `tests/factorio/test_subgroup_owner.py`'s `MODS` set
  loses a value, which is the check that will catch a stale `owner` string. Both `.pw.toml` entries
  and `config/Mekanism/` go.
- **The pack is three mods plus the grid**, not four plus the grid. `docs/gdd.md` and `#94`'s
  framing both describe a four-mod pack.
- **Nothing is invalidated**, because nothing is built. The recipe layer is empty (ADR-0034 records
  the state on the ground), and the pack is pre-release, so no world carries a Mekanism block.
- **This is the fifth time an ADR-0017 row has been corrected in the same direction** — an owner
  named without checking what the mod can actually do. The four before it are recorded under `#93`
  and `#101`. The difference here is that the row is not being reassigned; the mod is leaving,
  because after four corrections there was nothing under it.
