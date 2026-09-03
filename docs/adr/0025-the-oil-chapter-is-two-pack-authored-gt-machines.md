---
status: accepted
supersedes: [39, 40]
---

# The oil chapter is two pack-authored GT machines, and it moves off rung 4

ADR-0017 gave **Refining** to Mekanism, "by pack-authored recipes on machines it owns", and
recipe-removed GregTech's Distillation Tower and Distillery on Terra. `#39` had reached the same
place a rung earlier: *"Processing: Mekanism, by pack-authored recipes on machines it already owns
(PRC, Rotary Condensentrator, Thermal Evaporation, the chemical line)."*

Both were written without checking whether Mekanism can express the recipe.

**It cannot.** Factorio's advanced oil processing is `crude + water → heavy oil + light oil +
petroleum gas` — two fluids in, three fluids out — and no Mekanism machine has that shape. The
closest are the Electrolytic Separator (`mekanism:separating`, one fluid in, two chemicals out) and
the Pressurized Reaction Chamber (item + fluid + chemical in, item + chemical out). Two-in-one-out
and one-in-two-out both exist. Two-in-three-out does not.

This ADR records what the chapter runs on instead, and the progression re-cut that followed from
asking where sulfur comes from.

## The rule

**Terra's oil chapter runs on two machines this pack registers itself, on a GregTech chassis,
through KubeJS. It owns the recipe shapes Factorio needs and nothing else.**

## Why not the obvious candidates

**GregTech's Distillation Tower is not one fluid short — it is structurally wrong.**
`GTRecipeTypes.setMaxIOSize(itemIn, itemOut, fluidIn, fluidOut)`, read out of the 7.0.2 bytecode:

| Recipe type | itemIn | itemOut | fluidIn | fluidOut |
| --- | --- | --- | --- | --- |
| `distillation_tower` | 0 | 1 | **1** | 12 |
| `cracker` | 1 | 0 | 2 | 2 |
| `chemical_reactor` | 2 | 2 | 3 | 2 |
| `large_chemical_reactor` | 3 | 3 | 5 | 4 |

The tower takes **one** fluid. Crude plus water can never enter it, at any tier, with any coil. The
objection that the tower means "heavy recipe reauthoring" was true but incidental; the disqualifying
fact is the input count.

**The Large Chemical Reactor would work, and is still declined.** At `(3, 3, 5, 4)` it fits the
recipe with room to spare and costs nothing to register. It loses on legibility and on ownership: it
is a generic do-everything block that reads as GregTech, and pointing the chapter at it would hand
GregTech the Chemistry row ADR-0017 assigns to Mekanism, silently, by recipe placement.

**A Create addon — Petrochem or Diesel Generators — was declined on the fluid vocabulary.** Both are
live on 1.21.1 NeoForge. Both ship their own crude, diesel, kerosene and LPG, and Almost Unified
does not unify fluids. Adopting either means duplicate incompatible oils in EMI, or re-basing
ADR-0009's heavy-oil oceans and the polymer chain onto a third party's fluid set. Both are also
Create addons, and Create is hard-pinned at 6.0.10 by Electro's `required` mixins (ADR-0017).

**A Mekanism addon for a custom multiblock was declined on cost**, and is moot: GTCEu already
exposes `MACHINE_REGISTRY` and `RECIPE_TYPE_REGISTRY` to KubeJS
(`integration/kjs/builders/machine/*`), and **the pack registers custom GT machines this way** —
Launch Terminals, Receiving Terminals and Drop Hatches are designed against it (`docs/gdd.md`).

**Corrected by ADR-0026:** this paragraph read "the pack *already* registers custom GT machines this
way … the pack's established idiom", which was not true of the repo — no `MACHINE_REGISTRY` or
machine-builder call existed anywhere in `kubejs/`, so those three were design rather than code. The
conclusion here is unaffected: the technique is available and it is still the right one. But the
Assembling Machines of ADR-0026 are the **first**, and the Refinery and Chemical Plant inherit
whatever pattern they establish rather than the other way round.

## The two machines

| Machine | Form | `setMaxIOSize` | Recipes |
| --- | --- | --- | --- |
| **Oil Refinery** | Multiblock, one tier | `(0, 0, 2, 3)` | basic oil processing, advanced oil processing |
| **Chemical Plant** | Single block, one tier | `(2, 1, 2, 1)` | both crackings, lubricant, plastic, sulfur, three solid fuels, sulfuric acid, battery, explosives |

Both sizes are Factorio's, and both are read off **the extracted corpus** rather than chosen.

**Amended by #107, which registered them.** This table first read `(2, 0, 2, 3)` and `(2, 1, 2, 2)`,
taken from the wiki, and both numbers were wider than anything the pack can craft:

- **The Refinery's two item inputs were justified by simple coal liquefaction**, and *there is no
  liquefaction recipe in the corpus at all*. `coal-liquefaction` survives only in
  `data/factorio/technology.json`, where its unit includes `space-science-pack` and its
  prerequisite is `metallurgic-science-pack` — it is Space Age in 2.x, whatever the 1.1 wiki says,
  so ADR-0022's four-rung Nauvis-pre-launch filter drops its recipes. The Refinery crafts **two**
  recipes here, and item slots on it would be slots nothing can ever fill. Ignus is where
  liquefaction becomes reachable (`#12`), and adding it widens this back to `(2, 0, 2, 3)` — a
  one-line change, and the smaller half of that ticket's decision, which is whether a post-launch
  recipe extends the corpus filter or bypasses it through the override file.
- **The Chemical Plant's second output fluid is the Factorio *entity*, not any recipe.** The
  entity has two output fluid boxes; the widest of its twelve recipes emits one
  (`sulfuric-acid`, both crackings, lubricant). The item half of the envelope is unchanged and is
  the corpus's too: at most two item inputs (sulfuric acid, battery, explosives) and at most one
  item output.

`tests/factorio/test_recipe_extract.py` pins all four numbers against the corpus, and
`tests/pack/test_machine_assets.py` asserts the registered `setMaxIOSize` still equals them, so
neither a regeneration nor an edit to the registration can widen a GUI in silence. Factorio's
assembling-machine entities carry no item-slot count — item slots come from the recipe — which is
why the corpus is the only possible source for the first two figures of each pair.

**The Refinery is a multiblock and the Chemical Plant is not**, because Factorio's refinery is a
landmark the player builds a few of and plans around, and its chemical plant is a small thing the
player spams. Building Gadgets is in the pack for structure copy-paste, which makes the refinery
bearable; making the *high-count* block a multiblock is where that tax would actually bite.

**Neither has a voltage ladder.** One tier, one recipe set, no per-tier rebalancing. ADR-0017's
four-facts doctrine is already "spent, not exempted"; a tier ladder here would spend it again for a
speed multiplier.

**Both draw FE natively.** `config/gtceu.yaml` has `nativeEUToFE: true` with `enableFEConverters:
false`, which is `#39`'s "FE natively into GT machines, converters off". Every recipe is authored at
LV `EUt`, per ADR-0018.

## The fluids are GregTech's, and two of them were already right

Almost Unified does not unify fluids, and ADR-0017 restricts it to raw materials anyway. So the
fluid ids are a one-way commitment, and the pack takes GregTech's:

| Factorio | Pack | Lang |
| --- | --- | --- |
| Crude oil | `gtceu:raw_oil` | "Crude Oil" |
| Heavy oil | `gtceu:heavy_oil` | *unchanged — already correct* |
| Light oil | `gtceu:light_oil` | *unchanged — already correct* |
| Petroleum gas | `gtceu:oil` | "Petroleum Gas" |
| Lubricant | `gtceu:lubricant` | *unchanged* |
| Plastic bar | `gtceu:polyethylene` | "Plastic Bar" |

`gtceu:light_oil` and `gtceu:heavy_oil` exist as materials distinct from `light_fuel` and
`heavy_fuel`, and their lang strings are already verbatim Factorio. **This matters beyond
tidiness: ADR-0009 binds Electro's oceans to `gtceu:heavy_oil`.** Had the chapter used
`heavy_fuel`, Electro would have floated in a different fluid with a confusingly similar name, and
nothing refined on Terra would have worked on what Electro is made of. ADR-0009 is confirmed by this
ADR, not amended.

Petroleum gas rides on `gtceu:oil`, which is registered `.liquid()` while `gtceu:refinery_gas` is
`.gas()`. This was checked rather than assumed: `FluidBuilder` defaults to `hasFluidBlock = false`
and `hasBucket = true` for every GT fluid regardless of state, and `determineDensity()`'s
LIQUID/GAS/PLASMA split (1000 / -100 / -100000) only affects a placed fluid block, of which there is
none. **Fluid state has no mechanical consequence in this pack** — it is texture, tint and tooltip.
The choice is cosmetic and was made on readability.

**Every other GregTech fraction is kept, unrecipe'd and hidden.** `light_fuel`, `heavy_fuel`,
`naphtha`, `refinery_gas`, benzene, toluene, phenol and the whole sulfuric and cracked families
cannot be unregistered — GT materials are data files read by the fork — so the rule is operational:
author no recipes, and **hide them from EMI**. Visible-but-unreachable is the worse failure: a
Factorio player who finds Naphtha with no recipe reads it as a pack bug.

## Ratios are Factorio's, ×10

One craft consumes a bucket of crude, so tanks and pipes are sized sanely, and the ratios — which
are what a Factorio player actually feels, because refinery banks are built by ratio — survive
exactly.

```
Oil Refinery
  basic oil processing        1000 crude                    -> 450 petroleum gas
  advanced oil processing     1000 crude + 500 water        -> 250 heavy + 450 light + 550 gas
  (coal liquefaction and simple coal liquefaction are Space Age and out of the corpus; #12)

Chemical Plant
  heavy oil cracking          400 heavy + 300 water         -> 300 light
  light oil cracking          300 light + 300 water         -> 200 petroleum gas
  lubricant                   100 heavy                     -> 100 lubricant
  plastic bar                 200 petroleum gas + 1 coal    -> 2 plastic
  sulfur                      300 petroleum gas + 300 water -> 2 sulfur
  solid fuel                  from light / heavy / petroleum gas
  plus sulfuric acid, battery, explosives
```

## Sulfur is petroleum-derived, and that re-cuts the rungs

This is the largest consequence and it did not come from the machines. Asked where sulfur comes
from, the pack now answers **petroleum gas, and nothing else** — Factorio's own answer. The three
non-petroleum routes documented in `docs/research/sulfur-and-oil-chains.md` are removed:

1. Mekanism coal/charcoal gasification (`reaction/coal_gasification/*`) — the renewable route.
2. Mekanism `injecting/gunpowder_to_sulfur.json` — the mob-drop bootstrap.

   *(1) and (2) are amended by ADR-0035: both routes left with the mod, so neither needs removing.
   The removal that still has to happen is (3), which is GregTech's and the pack's own.*
3. The `gtceu:sulfur` slot at weight 1 in `terra_polymetallic_deposit.json`, and its line in
   `tests/worldgen/expected.json`.

(3) also deletes a known bug: GregTech's Sulfur has no raw-ore form, so that deposit currently drops
an ore-form item **nothing in the pack can process** — GT's ore line is removed and Mekanism has no
`c:ores/sulfur` recipe. `gtceu:sulfur` stays in Terra's `forbidden_ore_veins`; Ignus's sulfur vein
and sulfuric acid geyser are untouched.

**Sulfur gates blue science, so this forces the whole chapter down the ladder.** Factorio's tech
costs, read off the research pages:

```
Oil processing (red+green)  -> Oil refinery, Chemical plant, BASIC oil processing, solid fuel
  Sulfur processing (red+green) -> sulfur, sulfuric acid
    Chemical science pack
      Advanced oil processing (red+green+BLUE)
```

**Basic oil processing precedes chemical science; advanced follows it.** Shipping only advanced —
the original intent — together with petroleum-only sulfur is a hard cycle: blue needs sulfur needs
petroleum gas needs advanced processing needs blue. Factorio resolves it by shipping basic first,
and so does the pack. Basic oil processing is one `(0, 0, 1, 1)` recipe and it is the bootstrap that
makes the ladder work.

`Plastics` is also red+green, so the polymer moves with it.

## The rung table, re-cut

| Rung | Pack | Now holds |
| --- | --- | --- |
| 2 | `logistic` | Create package logistics, **Oil Refinery, Chemical Plant, basic oil processing, solid fuel, sulfur, sulfuric acid, plastic** |
| 3 | `chemical` | **Advanced oil processing, heavy and light cracking, lubricant** |
| 4 | `production` | **Rocket fuel, rocket control units, rocket parts, the silo** |

Rungs 0 and 1 are unchanged by this ADR. ADR-0018's spine rule — each rung grants what the next
rung's production physically requires — holds end to end on the new table: rung 2's sulfur buys
rung 3's blue science, rung 3's advanced processing buys rung 4's rocket fuel, and rung 4's fuel
buys the silo.

**Rung 4's gate is re-argued, and `#39`'s ownership decisions all survive.** `#39` justified rung 4
on the polymer, because every GCyR fuel tank and rocket motor is an Assembler recipe taking
`plate KaptonK ×6`. The polymer is now rung 2, so that argument is spent. The replacement is
Factorio's own: **the Rocket silo is the technology that costs production science**, and it requires
rocket fuel, rocket control units and concrete. That is rung 4.

## The polymer is Factorio's plastic, not Mekanism's HDPE

`#40` chose Option 1b — reuse Mekanism's substrate → ethene → HDPE line, bridged by a `c:ethene` tag
on `gtceu:ethylene`, re-basing GCyR's six Kapton-K part recipes onto HDPE sheet. **Superseded.**
Plastic is now Factorio's one step, `200 petroleum gas + 1 coal → 2 plastic`, on
`gtceu:polyethylene`, and GCyR's six recipes re-base onto polyethylene plate instead.

The re-basing is the same KubeJS edit either way, so 1b's stated advantage was already thin. What
decides it: the HDPE line's substrate input is renewable from `#c:fuels/bio`, which is a
**renewable plastic loophole sitting underneath the oil gate**. Factorio's recipe removes it, and
drops the `c:ethene` tag bridge with it.

## Fuel is `#39` executed, not reopened

GCyR's `gcyr:rocket_fuel` recipe type is rebound in KubeJS onto light oil → solid fuel → rocket
fuel. `gtceu:rocket_fuel` — Dimethylhydrazine + Dinitrogen Tetroxide, not petrochemistry — is
demoted to a later-planet tier. Hydrogen is cut as a launch fuel by raising its `EUt` above motor
tier. All three were already decided in `#39`; none needs a fork source edit.

**Solid fuel is a KubeJS item with a burn time.** It has no mechanism, so ADR-0015 keeps it out of
`planetaryfactory_core`; it has no ore, dust or plate form, so registering it as a GT material would
buy nothing and add twenty unwanted derived items.

## Lubricant is made on both bodies, and must never touch the grid

`10 heavy oil → 10 lubricant`. Its sink is the **Foundry**, the Vulcanus building that gates Big
Mining Drills — so lubricant is load-bearing on Ignus, which `docs/planets.md` already equips with
calcite patches, sulfuric acid geysers and "heavy oil = coal liquefaction". That is simple coal
liquefaction, and it is why the Oil Refinery is a cross-body machine rather than a Terra one.

Terra makes lubricant too, as Nauvis does. Ignus's route may be research-gated on Ignus's own tech
if Terra's rungs never need it.

**Standing constraint, mirroring ADR-0017's Transformer Oil rule:** Transformer Oil must not become
an input to anything in the oil chapter, and **lubricant must not become an input to anything
grid-side** — or rung 3 silently gates the Converter.

## Considered Options

- **Author the split on Mekanism anyway, in two machines.** Rejected: the crude split is one recipe
  in Factorio and the beat is that it is one recipe. Splitting it across two blocks to fit a mod's
  IO limits is the mod dictating the design.
- **Take the Large Chemical Reactor.** Rejected above on legibility and silent ownership drift.
- **Adopt Create: Petrochem or Create: Diesel Generators.** Rejected above on fluid vocabulary and
  the Create 6.0.10 pin.
- **Keep sulfur on coal and ship advanced processing only.** Rejected: it keeps the oil chapter at
  rung 4 and preserves `#39` intact, but it makes sulfur non-Factorio at the exact point the pack is
  buying fidelity, and it keeps a renewable sulfur loop under a rung meant to gate chemistry.
- **Cut sulfur from the chemical science pack recipe.** Rejected: the science pack recipes are
  ADR-0018's spine, and diverging there is a larger break than any of the alternatives.
- **Collapse rungs 3 and 4.** Rejected — this is the three-rung option `#26` already declined.

## Consequences

- **ADR-0017's table takes three edits** and ADR-0018's rung table is rewritten. Both are amended in
  place, because ADR-0017 is described in its own text as "consulted every time anyone adds a
  recipe" and a stale table poisons every future recipe review.
- **ADR-0017's Chemistry row moves to the pack.** The Chemical Plant takes Factorio's whole
  chemical-plant list, sulfuric acid and batteries included. A half-taken list would leave the player
  with a Chemical Plant that mysteriously cannot make sulfuric acid and Mekanism with a chemistry
  topic full of holes. ~~Mekanism keeps ore processing and power-at-scale.~~ *(Three later decisions
  void the compensations this sentence offered Mekanism, and it is kept struck rather than deleted
  because it is the reason the Chemistry row felt affordable at the time. The fluid logistics and
  bulk fluid storage named here went to Create under #101's amendment to ADR-0017; **ADR-0032
  deleted the ore-processing row entirely** — ore smelts 1:1, so there is no capability to keep;
  and **#104 struck the power-at-scale clause**, base Mekanism registering no generator block at
  all. Read ADR-0017's table, not this sentence.)*
- **`#39` is partly superseded**: its gate is re-argued, its ownership findings stand, its fuel
  decisions become execution. **`#40` is superseded** on the polymer's identity. Both get comments;
  neither is reopened.
- **Crude now gates rung 2, not rung 4.** Terra's five GregTech bedrock fluid deposits and the
  `gtceu:oilsands` vein become a **mid-game** blocker rather than a late one — and `tests/worldgen/
  expected.json` still has no `bedrock_fluids` block for Terra at all, while `#59`/`#60` are
  rewriting Terra as a flat, cave-free world. This is the most likely way the chapter breaks.
- **Ore multiplication is out of scope and unresolved.** *Resolved by ADR-0032: all of it is cut, pack-wide.* Deleting the 5x dissolution tier follows
  from sulfuric acid moving to rung 2, and the fidelity argument that kills 5x kills 4x, 3x and
  Create's rung-0 2x with it — rewriting ADR-0017's Ore processing row and emptying ADR-0018's rung
  1. Split to `#69`. **This ADR does not depend on the answer.**
- **Two new blocks need models and a structure**, and neither is cut until it has been played. The
  pack's standing rule — adopt whole, cut as necessary, cutting waits for hands-on play — applies.
- **Recipe removal grows.** ADR-0017 already recipe-removes GT's Distillation Tower and Distillery on
  Terra; this ADR adds the Mekanism sulfur routes, the HDPE line's role as the polymer, and an EMI
  hide list for roughly fifteen GregTech fractions.
