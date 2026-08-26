# Sulfur's source chains, and what oil processing is in this pack

> **Its verdict is superseded by ADR-0025; its findings are not.** This document asked whether
> sulfur *can* be had without oil and answered yes, on three routes. ADR-0025 later decided that it
> *should not* be — sulfur is petroleum-derived, as in Factorio — and removed all three: coal and
> charcoal gasification, gunpowder, and the weight-1 `gtceu:sulfur` slot in Terra's polymetallic
> bedrock deposit. Everything below is still an accurate read of the jars as installed on
> 2026-08-22, and the routes it documents are the routes ADR-0025 deletes. Read §1 as the evidence
> for that deletion rather than as the pack's current sulfur supply. §4's fluid inventory carries one
> correction, also in ADR-0025: the chapter uses `gtceu:light_oil` and `gtceu:heavy_oil`, not
> `light_fuel` and `heavy_fuel`.

Research for #38, resolving the unverified claim #26 built rung 3 on: that *in-pack sulfur is an
ore/dust product and not a refinery by-product*. Everything below is read out of the jars and pack
files actually installed on 2026-08-22, not from a wiki or from memory.

## Verdict

**Yes — #26's four-tier split survives, and on stronger evidence than the claim it was made on.**

Sulfur is reachable on Terra with no oil at all, by at least three independent routes, one of which
(coal/charcoal gasification) is renewable and scales without a drill. Nothing in the pack makes
sulfur *only* an oil derivative.

But the claim survives for a different reason than #26 gave, and two of #26's supporting statements
are wrong. See **Where the evidence contradicts #26**.

## Sources of truth

| Thing | Version | Path |
| --- | --- | --- |
| Mekanism | 10.7.19.85 | `mods/Mekanism-1.21.1-10.7.19.85.jar` |
| GregTech CEu Modern | 7.0.2 | `mods/gtceu-1.21.1-7.0.2.jar` |
| GCyR (fork) | 0.2.4+gt7.0.2 | `mods/gcyr-1.21.1-0.2.4+gt7.0.2-src.jar`, source at `../gcyr-src` |
| Create | 6.0.10 | `mods/create-1.21.1-6.0.10.jar` |
| AlmostUnified | 1.4.2 | `mods/almostunified-neoforge-1.21.1-1.4.2.jar` |

Loader is NeoForge 1.21.1. GTCEu's recipes are generated in code, not shipped as JSON, so its
chains below are read out of the bytecode of
`com/gregtechceu/gtceu/data/recipe/serialized/chemistry/*.class` with `javap -c`; recipe *ids* are
the string constants in those classes.

There is **no Immersive Engineering, no Thermal, no PneumaticCraft and no Create: Dieselgenerators**
in the pack. Only two mods have any petrochemistry: GregTech (all of it) and Mekanism (none of it).

---

## 1. Sulfur's real source chains

`c:dusts/sulfur` is the tag every downstream recipe reads. In-pack it holds `mekanism:dust_sulfur`
(`data/c/tags/item/dusts/sulfur.json` in the Mekanism jar) and GregTech's generated `sulfur_dust`,
unified by AlmostUnified — `config/almostunified/unification/materials.json` lists
`c:dusts/{material}` under `tags` and `sulfur` is in `config/almostunified/placeholders.json:42`.

### Reachable without oil

**A. Coal / charcoal gasification — Mekanism Pressurized Reaction Chamber.** The decisive one.

- `data/mekanism/recipe/reaction/coal_gasification/coals.json`
  — `#minecraft:coals` ×1 + water 100 + oxygen 100 → **`mekanism:dust_sulfur` ×1** + hydrogen 100
- `.../coal_gasification/blocks_coals.json` — a coal **or charcoal** storage block → **9 sulfur dust**
- `.../coal_gasification/dusts_coals.json` — `c:dusts/coal` or `c:dusts/charcoal` → 1 sulfur dust

Because charcoal counts, this is **renewable from trees**: logs → charcoal → gasify → sulfur, with
hydrogen as a free co-product. No ore, no drill, no oil, no planet other than Terra. Terra's coal
vein is reweighted *up* to 130 (`scripts/build-terra-vein-weights.py`, stock 80), so the ore route
is abundant too.

**B. Gunpowder — Mekanism Chemical Injection Chamber.**
`data/mekanism/recipe/injecting/gunpowder_to_sulfur.json`: `c:gunpowders` + hydrogen chloride →
`mekanism:dust_sulfur`. HCl comes from salt
(`data/mekanism/recipe/chemical_conversion/salt_to_hydrogen_chloride.json`) — again no oil. Mob-drop
sourced, so it is a bootstrap rather than a line.

**C. Terra's polymetallic bedrock deposit — GregTech.**
`kubejs/data/planetaryfactory/gtceu/bedrock_ore/terra_polymetallic_deposit.json` lists
`gtceu:sulfur` at weight 1 alongside lead (4) and silver (2). Asserted by the worldgen check at
`tests/worldgen/expected.json` under `terra.bedrock_ores`. Infinite-ish and automatable, being a
bedrock deposit.

**D. GregTech chemistry from sulfur dust.**
`AcidRecipes.class` → `sulfuric_acid_from_sulfur`: Sulfur **dust** + Water → `gtceu:sulfuric_acid`.
Also `sulfur_dioxide_from_sulfur` (Sulfur + Oxygen), `sulfur_trioxide`, `sulfuric_acid_from_trioxide`.

**E. Mekanism chemistry from sulfur dust.**
- `data/mekanism/recipe/chemical_conversion/sulfur_to_sulfuric_acid.json`: `c:dusts/sulfur` → 2
  `mekanism:sulfuric_acid`. This is a *chemical conversion*, i.e. the dust can go straight into a
  machine's chemical input slot. One item, no intermediate machine.
- `data/mekanism/recipe/oxidizing/sulfur_dioxide.json`: dust → 100 sulfur dioxide (Chemical Oxidizer)
- `data/mekanism/recipe/chemical_infusing/sulfur_trioxide.json`, `.../sulfuric_acid.json`: the
  SO2 → SO3 → H2SO4 chain in the Chemical Infuser.

### Not on Terra

- `gtceu:sulfur` **ore veins are explicitly forbidden on Terra** —
  `tests/worldgen/expected.json`, `terra.forbidden_ore_veins` contains `gtceu:sulfur`. GregTech's
  own `data/gtceu/gtceu/ore_vein/sulfur.json` is Nether-only anyway.
- The sulfur *vein* and a sulfuric-acid geyser are **Ignus's**:
  `kubejs/data/planetaryfactory/gtceu/ore_vein/ignus_sulfur.json` and
  `kubejs/data/planetaryfactory/gtceu/bedrock_fluid/ignus_sulfuric_acid_geyser.json`, both filtered
  to `planetaryfactory:vulcanus`. Ignus's coal bedrock deposit carries sulfur too.

### Where the sulfur *slurry* line needs it

Mekanism's ore-processing ladder, which #37 hands Mekanism from rung 1:

| Tier | Machine | Chemical | Evidence |
| --- | --- | --- | --- |
| 3× | Purification Chamber | oxygen | `processing/iron/clump/from_raw_ore.json` |
| 4× | Chemical Injection Chamber | **hydrogen chloride** | `processing/iron/shard/from_raw_ore.json` |
| 5× | Chemical Dissolution Chamber | **sulfuric acid** | `processing/iron/slurry/dirty/from_raw_ore.json` |

So sulfuric acid is what buys the 5× tier, and per (A)+(E) that is *coal + water + oxygen, one
machine, no oil*. This is the mechanical fact rung 3 actually rests on.

## 2. Does anything make sulfur an oil derivative?

**Mekanism: no.** Grepping the whole jar for `oil`/`petrol` returns only Boiler classes. Mekanism has
no crude oil, no refinery, no petroleum gas, no fraction of any kind.

**GregTech: yes, as an additional route — but not as the only one, and not the cheap one.**
`PetrochemRecipes.class` defines `desulfurize_heavy_fuel`, `desulfurize_light_fuel`,
`desulfurize_naphtha`, `desulfurize_natural_gas`, `desulfurize_refinery_gas`. Each is
`Sulfuric<X> + Hydrogen → <X> + HydrogenSulfide` (verified in the bytecode: the operand order
`SulfuricHeavyFuel, Hydrogen → HydrogenSulfide, HeavyFuel`). Hydrogen sulfide then feeds
`sulfur_dioxide_from_sulfide` and `sulfuric_acid_from_sulfide` in `AcidRecipes.class`.

That is genuinely Factorio-shaped — desulfurizing oil fractions yields sulfur. But it is a
**by-product of a refinery you built for other reasons**, not the gate on having sulfur at all. The
one-machine coal route beats it by a wide margin at rung 3.

**Nothing else installed touches sulfur.** Create has no chemistry; GCyR adds fluids only via
GregTech materials.

## 3. "One chemical + one circuit + one mechanical" in Mekanism

Given #37 hands Mekanism ore processing from rung 1, intermediates and power-at-scale, the Factorio
set (sulfur, advanced circuit ×3, engine unit ×2) maps cleanly except for the last slot:

- **Chemical** — the obvious fill is **sulfur dust** itself, which keeps Factorio's literal
  ingredient. A fluid task on `mekanism:sulfuric_acid` is the more expressive alternative: it forces
  the whole Reaction Chamber → chemical-conversion loop and is exactly the capability rung 3 is
  testing. `mekanism:hdpe_sheet` is the third candidate and is *not* oil-derived here
  (`data/mekanism/recipe/enriching/hdpe_sheet.json`, from HDPE pellets via substrate/ethene).
- **Circuit** — **`mekanism:advanced_control_circuit`** is a direct hit.
  `data/mekanism/recipe/control_circuit/advanced.json`: infused alloy + `c:circuits/basic` + infused
  alloy. Basic circuit is `data/mekanism/recipe/control_circuit/basic.json`: osmium ingot +
  redstone in a Metallurgic Infuser. Infused alloy is copper + redstone, same machine
  (`metallurgic_infusing/alloy/infused.json`). One new machine, two tiers, no oil.
- **Mechanical** — **Mekanism has no engine unit and no motor.** This slot has no clean Mekanism
  filler. The honest options are an Enriched/Reinforced Alloy (chemical-flavoured, not mechanical),
  a Mekanism machine casing, or breaking #26's "items from the owning mod" rule and taking Create's
  Precision Mechanism. **Flagged for #27** — this is a real gap in the ownership table, not
  something to settle from recipe files.

## 4. What oil processing actually is

**GregTech owns the refinery, entirely.** Two machines matter:

- **Distillation Tower** (`DistillationTowerMachine.class`) — the multiblock, many outputs at once.
- **Distillery** (`lv_distillery` … `uxv_distillery` models) — the single-output singleblock.

Sources of crude, all four of them GregTech bedrock fluid deposits reachable by a **Fluid Drilling
Rig**, all filtered to `minecraft:overworld` and **none of them overridden or forbidden by this
pack** (`kubejs/data/gtceu/gtceu/bedrock_fluid/` contains only `lava_deposit.json`):

| Deposit | Fluid | Weight | Yield |
| --- | --- | --- | --- |
| `oil_deposit` | `gtceu:oil` | 20 (+5 ocean, +5 sandy) | 175–300 |
| `raw_oil_deposit` | `gtceu:raw_oil` | 20 | 200–300 |
| `light_oil_deposit` | `gtceu:light_oil` | 25 | 175–300 |
| `heavy_oil_deposit` | `gtceu:heavy_oil` | 15 (+5 ocean, +10 sandy) | 100–200 |
| `natural_gas_deposit` | `gtceu:natural_gas` | 15 | 100–175 |

Plus the **`gtceu:oilsands` ore vein**, kept on Terra at weight 30
(`scripts/build-terra-vein-weights.py:49`, stock 40; generated copy at
`kubejs/data/gtceu/gtceu/ore_vein/oilsands.json`).

Distilling any crude (`distill_oil`, `distill_raw_oil`, `distill_light_oil`, `distill_heavy_oil` in
`PetrochemRecipes.class`) gives the same four fractions, all sulfur-bearing:

**Sulfuric Heavy Fuel · Sulfuric Light Fuel · Sulfuric Naphtha · Sulfuric Gas**

Each is desulfurized with hydrogen into its clean form (Heavy Fuel, Light Fuel, Naphtha, Refinery
Gas) plus hydrogen sulfide. From there: cracking (`hydro_crack_*`, `steam_crack_*` at lightly /
moderately / severely), distillation into ethylene, propene, butadiene, benzene, toluene, phenol,
octane; and the fuel chain in `FuelRecipeChains.class` — Naphtha → Raw Gasoline → Gasoline →
High Octane Gasoline, and Diesel / Cetane-Boosted Diesel in `MixerRecipes.class`.

This is a genuinely large, genuinely new production mode. #26's "oil earns a chapter" holds on its
own merits.

## 5. Rocket fuel

`../gcyr-src/src/main/java/argent_matter/gcyr/data/recipe/RocketFuelRecipes.java` — the fork's full
fuel list, with `EUt` used as a bogus motor-tier indicator and `duration/10` as the consumption
divisor:

| Fuel | `EUt` (tier) | `duration` |
| --- | --- | --- |
| `gtceu:gasoline` | 0 | 25 |
| `gtceu:diesel` | 0 | 18 |
| `gtceu:rocket_fuel` | 1 | 75 |
| `gtceu:hydrogen` | 1 | 10 |
| hydrogen plasma | 3 | 18 |

Note the id: it is **`gtceu:rocket_fuel`**, a GregTech material, not `gcyr:rocket_fuel` as #25/#31
record it. GCyR only tags it (`src/generated/resources/data/gcyr/tags/fluid/vehicle_fuels.json`).

And `gtceu:rocket_fuel` is **not oil-derived**. `MixerRecipes.class`, recipe id
`rocket_fuel_from_oxygen`, combines **Dimethylhydrazine + Dinitrogen Tetroxide**. Dimethylhydrazine
comes from `ReactorRecipes.class` (`dimethylhydrazine_from_methanol` → Dimethylamine → …), and
dinitrogen tetroxide from the nitrogen/ammonia chain in `AcidRecipes.class`. It is amine and
nitrogen chemistry, not petrochemistry. Gasoline and Diesel *are* oil-derived — and they are the
*lower* tier fuels.

---

## Where the evidence contradicts #26

Three things, in descending order of how much they matter.

**1. "Rocket fuel is oil-derived there too" is false in this pack.** #26 used it to justify putting
the silo and rocket fuel behind an oil rung. `gtceu:rocket_fuel` is a nitrogen/amine chain (§5).
Worse, **`gtceu:hydrogen` fuels a rocket at the same motor tier (`EUt` 1)** as rocket fuel, and
hydrogen falls out of the coal gasification recipe that makes sulfur, and out of any electrolyzer.
A player can reach orbit on hydrogen without ever building a refinery. Rung 4 as "oil is physically
required for the rocket" **does not hold as written** — either the rung needs to be re-argued on
what oil actually gates (plastics, Gasoline/Diesel as the *efficient* fuels, LDS and the
polymer chain), or the fork needs to restrict the fuel list. #31 already established the fork edit
is cheap. This is the finding that most needs a decision.

**2. The reason #26 gave for the split is weaker than the reason that actually holds.** #26 said
sulfur is "an ore/dust product". On Terra it is essentially *not* an ore product — `gtceu:sulfur`
veins are forbidden there, and the only ore-shaped source is one weight-1 slot in a bedrock deposit.
It is a **coal product**, via Mekanism gasification. That is a better fact for the pack than the one
recorded: it puts rung 3's chemistry on Terra's most abundant reweighted vein and on a renewable
charcoal loop, and it needs no GregTech at all. The conclusion is unchanged; the rationale in the
ADR should be the coal one.

**3. Oil is not absent from Terra, and it is not gated.** #26 reads as though oil is a thing rung 4
introduces. In fact GregTech's five oil/gas bedrock deposits and the oilsands vein are all live on
Terra today, untouched by the pack's overrides, and `docs/planets.md` separately assigns petroleum
oceans to Electro and coal liquefaction to Ignus. Rung 4 will be gating a *machine* (the Fluid
Drilling Rig and the Distillation Tower), never the resource. Worth saying explicitly in the spec,
and worth noting that `terra` in `tests/worldgen/expected.json` has **no `bedrock_fluids` block at
all** — Terra's oil deposits are currently unasserted by the worldgen check.

## Adjacent findings worth carrying to the map

- **A supply gap: sulfur ore → sulfur dust has no owner.** `config/gtceu.yaml:243` sets
  `bedrockOreDropTagPrefix: raw`, and `BedrockOreMinerLogic` falls back
  `raw → crushed → gem → ore → dust`. GregTech's Sulfur has no raw-ore form, so the Terra
  polymetallic deposit will drop an ore-form item. #37 removes GregTech's ore-processing line, and
  **Mekanism has no recipe for `c:ores/sulfur`** — its ore processing is per-material and sulfur is
  not one of its materials. Route (C) therefore has a hole unless a KubeJS recipe fills it. This
  belongs on the map's existing "GT dust-supply audit" out-of-scope item. Routes (A) and (B) are
  unaffected, so nothing is blocked.
- **AlmostUnified's `mod_priorities` omits `gtceu`** (`config/almostunified/unification/materials.json`
  lists minecraft, kubejs, create, mekanism). Unification still pools both sulfur dusts into the tag,
  but every unified output resolves to the Mekanism item. Probably intended given #37; worth a
  conscious confirmation.
- **`gcyr:rocket_fuel` is a wrong id** and appears in #25 and #31. It is `gtceu:rocket_fuel`.
