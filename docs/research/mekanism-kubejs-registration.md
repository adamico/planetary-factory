# Can KubeJS register Mekanism chemicals and recipes on 1.21.1, or is CraftTweaker required?

**Retrieved**: 2026-08-22. Resolves #40 (map #25, follows the resolution of #39).

**Answer in one line: CraftTweaker is not required for anything the petroleum→polymer chain needs.
Mekanism's machine recipes are plain codec-backed datapack JSON that base KubeJS can already author
today; new Mekanism Chemicals *are* a hard Java-registry problem that base KubeJS cannot solve, but
the first-party `kubejs_mekanism` addon (latvian.dev, live on 1.21.1 NeoForge) solves it with a
`StartupEvents.registry('mekanism:chemical', …)` builder — and the chain needs zero new chemicals
anyway, because Mekanism's ethylene→HDPE line is keyed on the `c:ethene` *fluid tag*, which a GTCEu
fluid can simply join.**

---

## Method and trust levels

Every claim is tagged:

- **[verified]** — read out of a jar (by `unzip` of the shipped datapack, or `javap` disassembly of
  the shipped classes) that is either in this pack's `mods/`, or downloaded from the publisher's own
  CDN for this question. Highest trust.
- **[documented]** — from a first-party API or publisher page (Modrinth `/v2`), not from the bytecode.

Jars read:

| Jar | Where | Version |
| --- | --- | --- |
| `Mekanism-1.21.1-10.7.19.85.jar` | pack `mods/` | Mekanism 10.7.19.85 |
| `gtceu-1.21.1-7.0.2.jar` | pack `mods/` | GTCEu 7.0.2 |
| `kubejs-neoforge-2101.7.1-build.181.jar` | pack `mods/` | KubeJS 2101.7.1-build.181 |
| `gcyr-1.21.1-0.2.4+gt7.0.2-src.jar` | pack `mods/` | GCyR 0.2.4 |
| `kubejs-mekanism-neoforge-2101.1.6-build.6.jar` | Modrinth CDN, `sY2Fy24K` | KubeJS Mekanism 2101.1.6-build.6 |
| `kubejs-mekanism-neoforge-2101.1.7-build.18.jar` | Modrinth CDN, `sY2Fy24K` | KubeJS Mekanism 2101.1.7-build.18 |

Source clone read: `~/Documents/curseforge/minecraft/Instances/gcyr-src` (the fork whose jar is in
`mods/`).

`javap` needs Homebrew's JDK — `/opt/homebrew/opt/openjdk@21/bin`. The macOS stub reports "Unable to
locate a Java Runtime".

---

## 1. Can KubeJS register a new Mekanism Chemical?

**Not with base KubeJS alone. Yes with the first-party `kubejs_mekanism` addon, which is live and
obtainable for 1.21.1 NeoForge.**

### The registry is a Java registry, so no amount of JSON reaches it

`mekanism.api.MekanismAPI` declares [verified]:

```
public static final ResourceKey<Registry<Chemical>> CHEMICAL_REGISTRY_NAME;
public static final DefaultedRegistry<Chemical>     CHEMICAL_REGISTRY;
```

A `DefaultedRegistry` built at class-init is a **static** registry, not a datapack registry. Nothing
under `data/*/…` can add an entry to it. Mekanism populates it through
`mekanism.common.registration.impl.ChemicalDeferredRegister`, a `MekanismDeferredRegister<Chemical>`,
whose contents land during NeoForge's `RegisterEvent` [verified]. The only usable seam is therefore
Java code running on the mod event bus.

The seam is at least *open*: `Chemical` has a `public Chemical(ChemicalBuilder)` constructor and
`ChemicalBuilder.builder(ResourceLocation)` is `public static` [verified], with
`gaseous()`, `tint(int)`, `ore(TagKey)` and `with(ChemicalAttribute)` all public.

### Base KubeJS has no Mekanism integration at all

`kubejs.plugins.txt` inside `kubejs-neoforge-2101.7.1-build.181.jar` lists exactly four plugins —
the builtin one, the client one, Architectury and GameStages [verified]. There is no Mekanism
plugin, so `StartupEvents.registry('mekanism:chemical', …)` has no registered `BuilderType` and
`e.create(…)` cannot produce a `Chemical`.

There is a generic escape hatch — `RegistryKubeEvent.createCustom(id, Supplier<Object>)`, which wraps
any JS-supplied object in a `CustomBuilderObject` and files it under the event's registry key
[verified] — and `RegistryEventHandler.registerAll(RegisterEvent)` is a generic NeoForge
`RegisterEvent` listener that services whatever registry keys scripts have asked for [verified]. So a
hand-rolled `createCustom` + `new Chemical(ChemicalBuilder.builder(…))` is *architecturally* the
right shape. **Do not build it.** The addon below is the same mechanism, written by the KubeJS author,
with the texture/tint/attribute plumbing already correct.

Note the contrast with this pack's own recorded failure for `gtceu:material`
(`kubejs/startup_scripts/materials.js`): GregTech closes its material window during `CommonInit`,
*before* KubeJS's startup scripts are dispatched. Mekanism does not — chemicals are a
`DeferredRegister` on `RegisterEvent`, which is exactly where KubeJS hooks. **The gtceu:material
lesson does not transfer to mekanism:chemical.**

### The addon: KubeJS Mekanism, latvian.dev

Modrinth project `sY2Fy24K`, "KubeJS Mekanism", authors `latvian.dev`, issue tracker
`kubejs.com/support` — i.e. first-party to KubeJS, not a third-party fork. 105k downloads
[documented, `https://api.modrinth.com/v2/project/kubejs-mekanism`].

Three 1.21.1 NeoForge builds exist [documented,
`https://api.modrinth.com/v2/project/kubejs-mekanism/version`]:

| Build | Published | Requires kubejs | Requires mekanism |
| --- | --- | --- | --- |
| `2101.1.6-build.4` | 2024-08-30 | — | — |
| `2101.1.6-build.6` | 2024-09-07 | `[2101.7.0-build.166,)` | `[1.21.1-10.7.7.64,)` |
| `2101.1.7-build.18` | 2025-11-12 | `[2101.7.2-build.303,)` | `[1.21.1-10.7.14.79,)` |

Dependency ranges read from each jar's `META-INF/neoforge.mods.toml` [verified].

**`build.6` drops into this pack unchanged**: the pack has KubeJS `2101.7.1-build.181` (≥
`2101.7.0-build.166`) and Mekanism `10.7.19.85` (≥ `10.7.7.64`).

**`build.18` does not**: it wants KubeJS `2101.7.2-build.303`, which is newer than the pack's
`2101.7.1-build.181`. Taking the latest addon build means bumping KubeJS itself.

The addon registers a real builder type against Mekanism's registry key — `MekanismKubeJSPlugin`
calls `BuilderTypeRegistry.of(MekanismAPI.CHEMICAL_REGISTRY_NAME, …)` and adds an `addDefault` plus
the named sub-types `liquid`, `pigment`, `infuse_type`, `clean_slurry`, `dirty_slurry` [verified].
`KubeChemicalBuilder extends BuilderBase<Chemical>` and exposes `tint`, `gaseous`, `ore`, `radiation`,
`fuel`, `heatedCoolant`, `cooledCoolant`, `with` [verified]. So:

```js
StartupEvents.registry('mekanism:chemical', event => {
  event.create('planetaryfactory:naphtha').gaseous().tint(0xC8C8C8)
})
```

is the supported KubeJS surface for a new chemical, addon installed. **CraftTweaker is not needed for
this.**

---

## 2. A Rotary Condensentrator pair for an existing fluid, with no new chemical?

**Yes, and better than that — the chain needs no new rotary recipe at all, because Mekanism's rotary
and PRC recipes for ethene are keyed on a fluid *tag*.**

Mekanism's own `data/mekanism/recipe/rotary/ethene.json` [verified]:

```json
{"type":"mekanism:rotary",
 "chemical_input":{"amount":1,"chemical":"mekanism:ethene"},
 "chemical_output":{"amount":1,"id":"mekanism:ethene"},
 "fluid_input":{"amount":1,"tag":"c:ethene"},
 "fluid_output":{"amount":1,"id":"mekanism:ethene"}}
```

The fluid side is `#c:ethene`, a tag Mekanism itself declares at
`data/c/tags/fluid/ethene.json` [verified]. **Any fluid added to `c:ethene` by a datapack becomes a
legal rotary input on day one**, with zero registration of anything. Every other Mekanism rotary
recipe (`brine`, `chlorine`, `hydrogen`, `oxygen`, `lithium`, `sodium`, `steam`,
`hydrogen_chloride`, `hydrofluoric_acid`) follows the same tag-in / id-out shape [verified].

Authoring a *new* pair for an unrelated existing fluid is also possible without a new chemical, as
long as the chemical half already exists: the JSON's four keys are independent, and
`mekanism.api.recipes.RotaryRecipe` exposes `hasChemicalToFluid()` and `hasFluidToChemical()` as
separate predicates, with distinct error paths "This recipe has no chemical to fluid conversion." /
"…no fluid to chemical conversion." [verified] — so a one-directional rotary recipe is a supported
shape, not a malformed one. The addon's `rotary` schema marks all four keys `default_optional` and
declares `"unique": ["chemical_output","fluid_output"]` [verified], which is the same fact from the
other side.

---

## 3. Can KubeJS author Mekanism machine recipes?

**Yes. Two ways, both without CraftTweaker.**

### The unified `Chemical` change did *not* break datapack authoring

Mekanism 1.21.1 collapsed gas/slurry/infuse/pigment into one `Chemical`. The consequence for
authoring is cosmetic: every recipe JSON now takes a `chemical` field naming an entry in the single
`mekanism:chemical` registry, instead of `gas`/`slurry`/`infuse_type` variants. Read straight out of
the shipped datapack [verified]:

```json
// chemical_infusing/sulfuric_acid.json … left_input/right_input/output all "chemical"
{"type":"mekanism:chemical_infusing","left_input":{"amount":1,"chemical":"mekanism:hydrogen"},
 "right_input":{"amount":1,"chemical":"mekanism:chlorine"},"output":{"amount":1,"id":"…"}}

// reaction/substrate/ethene_oxygen.json  (the PRC)
{"type":"mekanism:reaction","chemical_input":{"amount":10,"chemical":"mekanism:oxygen"},
 "duration":60,"energy_required":1000,"fluid_input":{"amount":50,"tag":"c:ethene"},
 "item_input":{"count":1,"item":"mekanism:substrate"},
 "item_output":{"count":1,"id":"mekanism:hdpe_pellet"}}

// evaporating/lithium.json  (Thermal Evaporation — fluid in, chemical out)
{"type":"mekanism:evaporating","input":{"amount":10,"tag":"c:brine"},
 "output":{"amount":1,"id":"mekanism:lithium"}}

// dissolution — item + chemical -> chemical
{"type":"mekanism:dissolution","chemical_input":{"amount":1,"chemical":"mekanism:sulfuric_acid"},
 "item_input":{"count":1,"tag":"c:gems/fluorite"},
 "output":{"amount":1000,"id":"mekanism:hydrofluoric_acid"},"per_tick_usage":true}

// crystallizing/salt.json
{"type":"mekanism:crystallizing","input":{"amount":15,"chemical":"mekanism:brine"},
 "output":{"count":1,"id":"mekanism:salt"}}
```

All 25 machine recipe types are ordinary `RecipeSerializer`s registered through
`mekanism.api.recipes.MekanismRecipeSerializers` as NeoForge `DeferredHolder`s into
`BuiltInRegistries.RECIPE_SERIALIZER` — `CHEMICAL_INFUSING`, `ROTARY`, `REACTION`, `EVAPORATING`,
`DISSOLUTION`, `WASHING`, `CRYSTALLIZING`, `SEPARATING`, `OXIDIZING`, `ACTIVATING`,
`NUCLEOSYNTHESIZING`, `SAWING`, … [verified].

### Path A — base KubeJS today, zero new mods

`RecipeSchemaStorage` iterates `BuiltInRegistries.RECIPE_SERIALIZER` and, for every serializer with
no registered schema, files an `UnknownRecipeSchemaType` backed by `UnknownRecipeSchema.SCHEMA`
(a schema with an empty key map and `UnknownKubeRecipe.RECIPE_FACTORY`) [verified]. That is KubeJS's
raw-JSON passthrough: `event.custom({type: 'mekanism:reaction', …})` writes the JSON verbatim and
Mekanism's own codec parses it. No components, no type checking, no `.id()` conveniences — but it
works for every one of the 25 types, with the pack exactly as it stands.

### Path B — the addon, typed schemas

`kubejs-mekanism` ships `data/mekanism/kubejs/recipe_schema/*.json`: **32 schemas** covering
`reaction`, `rotary`, `chemical_infusing`, `evaporating`, `dissolution`, `washing`, `crystallizing`,
`separating`, `nucleosynthesizing`, `oxidizing`, `activating`, `centrifuging`, `pigment_mixing`,
`painting` and the rest, over nine shared bases including `fluid_chemical_to_chemical` and
`chemical_chemical_to_chemical` [verified]. It also registers the recipe components
`mekanism:chemical`, `mekanism:chemical_stack`, `mekanism:chemical_stack_ingredient` [verified], so
recipes get real validation and named constructors — e.g. the `reaction` schema declares four
constructor overloads and defaults `duration` to 100 ticks and `energy_required` to 0 [verified].

Modrinth's project page documents only the simple item-in/item-out helpers
(`event.recipes.mekanismCrushing(…)`, `mekanismCombining(…)`) [documented] — **the page understates
the jar.** The schemas in the jar are the authority, and they cover the whole chemical line.

### CraftTweaker, for completeness

Mekanism 10.7.19.85 ships a complete CrT integration —
`mekanism/common/integration/crafttweaker/` with `recipe/`, `chemical/`, `content/`, `bracket/`,
`ingredient/`, `jeitweaker/` sub-packages, including `CrTPressurizedReactionRecipe`,
`CrTFluidChemicalToChemicalRecipe`, `CrTChemicalDissolutionRecipe` and a chemical *registration*
path: `CrTContentUtils.queueChemicalForRegistration(ResourceLocation, Chemical)` +
`registerCrTContent(RegisterEvent)`, logging "Queueing Chemical '{}' for registration." under the
namespace `mekanismcontent`, driven by `CrTChemicalBuilder` [verified].

CraftTweaker itself is alive on 1.21.1 NeoForge: 36 NeoForge builds for 1.21.1, latest `21.0.38`
published **2026-03-17** [documented,
`https://api.modrinth.com/v2/project/crafttweaker/version?loaders=["neoforge"]&game_versions=["1.21.1"]`].

So **1a is technically available**. It is simply no longer necessary.

---

## 4. What already exists, and what the HDPE line actually eats

### GTCEu 7.0.2 fluids the chain can reuse

GTCEu registers a material's fluid under the bare material name when the requested storage key *is*
that material's primary key, and only prefixes `liquid_` / postfixes `_gas` otherwise —
`FluidStorageKeys.prefixedRegisteredName` returns the concatenation only when
`property.getPrimaryKey() != key` [verified]. Every fluid below is declared with `.liquid()` or
`.gas()` as its sole state in `com.gregtechceu.gtceu.data.material.UnknownCompositionMaterials` /
`OrganicChemistryMaterials` [verified], so each registers under its plain id:

| Fluid | Id | State |
| --- | --- | --- |
| Oil | `gtceu:oil` | liquid |
| Raw Oil | `gtceu:raw_oil` | liquid |
| Naphtha | `gtceu:naphtha` | liquid |
| Light Fuel | `gtceu:light_fuel` | liquid |
| Heavy Fuel | `gtceu:heavy_fuel` | liquid |
| Refinery Gas | `gtceu:refinery_gas` | gas |
| Ethylene | `gtceu:ethylene` | gas |

Plus the whole cracked/sulfuric family — `sulfuric_naphtha`, `lightly_/severely_steam_cracked_*`,
`lightly_/severely_hydro_cracked_*`, `steam_cracked_ethylene`, `hydro_cracked_ethylene` — 653
`material.gtceu.*` lang keys in total, including `light_oil`, `heavy_oil`, `diesel`, `benzene`,
`toluene`, `phenol`, `polyethylene`, `dimethylhydrazine` [verified, `assets/gtceu/lang/en_us.json`].

**There is no "kerosene" in GTCEu.** Light Fuel is the fraction that plays that role. Any spec text
saying "kerosene" needs to say Light Fuel or invent a display name.

**GTCEu tags none of these into `c:`** — its `data/c/tags/fluid/` contains only `potion.json`
[verified]. So joining `c:ethene` is a tag file this pack writes; nothing upstream does it.

### Kapton-K and its precursors: already registered, in GCyR

`gcyr-src/src/main/java/argent_matter/gcyr/common/data/GCYRMaterials.java:155` registers `KaptonK`
with `.polymer(1)`, `STD_METAL | GENERATE_FOIL`, components PyromelliticDianhydride + Oxydianiline
[verified]. `PolymerRecipes.kaptonKProcess` authors the full chain as GT Chemical Reactor /
Distillation recipes at `VA[HV]` [verified]:

```
Chlorobenzene + NitricAcid                     -> Nitrochlorobenzene + Water
CarbonMonoxide + Dimethylamine                 -> Dimethylformamide
Durene + Oxygen                                -> PyromelliticDianhydride + Water
AminoPhenol + Nitrochlorobenzene
  + Dimethylformamide + dust PotassiumCarbonate-> OxydianilineSludge + Water
distill OxydianilineSludge                     -> Dimethylformamide + Oxydianiline
PyromelliticDianhydride + Oxydianiline         -> KaptonK
```

**Nothing in that chain needs registering.** Both the GCyR materials
(`Nitrochlorobenzene`, `Dimethylformamide`, `PyromelliticDianhydride`, `Oxydianiline`,
`OxydianilineSludge`, `KaptonK`) and the GTCEu precursors (`Chlorobenzene`, `NitricAcid`,
`CarbonMonoxide`, `Dimethylamine`, `Durene`, `AminoPhenol`, `PotassiumCarbonate`) exist in the
installed jars. Option 1a's "register the fractions" was a premise about *Mekanism-side* chemicals,
not about Kapton-K, and it dissolves either way.

The gate #39 found still stands: `MiscRecipes.java` lines 138–178 — six Assembler recipes, every fuel
tank and rocket motor, each `.inputItems(plate, KaptonK, 6)` [verified].

### Mekanism's ethylene → HDPE line at 1.21.1: intact, and tag-fed

Three PRC recipes under `data/mekanism/recipe/reaction/substrate/` [verified]:

| Recipe | Item in | Fluid in | Chemical in | Out |
| --- | --- | --- | --- | --- |
| `water_hydrogen.json` | 2× `#c:fuels/bio` | 10 mB `#minecraft:water` | 100 `mekanism:hydrogen` | `mekanism:substrate` + 100 `mekanism:ethene` |
| `water_ethene.json` | 1× `mekanism:substrate` | 200 mB water | 100 `mekanism:ethene` | 8× substrate + 10 oxygen |
| **`ethene_oxygen.json`** | **1× `mekanism:substrate`** | **50 mB `#c:ethene`** | **10 `mekanism:oxygen`** | **1× `mekanism:hdpe_pellet`** |

Downstream, unchanged: `enriching/hdpe_sheet.json` = 3 pellets → 1 `mekanism:hdpe_sheet`;
`hdpe_rod.json` = 4 pellets → 1 rod; `hdpe_stick.json` = 2 rods → 1 stick [verified].

**The load-bearing detail is the middle column of the last row.** The polymer step's petroleum input
is a *fluid*, matched by **tag**, and its chemical input is oxygen — which the pack already has.
Substrate is the only item, and it is renewable from bio fuel today.

So `gtceu:ethylene` needs one four-line datapack tag file to feed Mekanism's stock HDPE line:

```json
// kubejs/data/c/tags/fluid/ethene.json
{ "replace": false, "values": ["gtceu:ethylene"] }
```

and — separately, if the pack wants the *chemical* `mekanism:ethene` reachable from GT ethylene for
the Chemical Infuser / dissolution side — the stock `rotary/ethene.json` already converts it, via the
same tag. **Zero registration. Zero new mods.**

---

## Verdict

**Take 1b. Do not add CraftTweaker.**

The question #40 was posed to answer — "is CraftTweaker required?" — has turned out to have a cleaner
answer than either option assumed: **CraftTweaker is not required for 1a either.** Kapton-K and all
seven steps of its precursor chain are already registered by GCyR and GTCEu, and Mekanism's machine
recipes are datapack JSON that base KubeJS can already write. What 1a would have bought — CrT's
`mekanismcontent` chemical registration — is available to KubeJS through a first-party addon, and is
not needed by the chain we want.

So the decision reduces to the *design* trade #39 already framed, decided on registration cost:

### Option 1b — reuse Mekanism's ethylene → HDPE line. **Chosen.**

**Registration surface: none.**

- No new mod. No new Mekanism Chemical. No new fluid. No source edit.
- One datapack tag file putting `gtceu:ethylene` into `c:ethene`, and Mekanism's stock
  `reaction/substrate/ethene_oxygen.json` and `rotary/ethene.json` accept it as-is [verified].
- Pack-authored Mekanism recipes to get from `gtceu:oil` to ethylene are ordinary
  `mekanism:reaction` / `mekanism:evaporating` JSON — writable **today** through base KubeJS's
  `UnknownRecipeSchema` passthrough, and writable *nicely* by dropping in
  `kubejs-mekanism-neoforge-2101.1.6-build.6.jar`, whose dependency ranges this pack already
  satisfies.
- Known cost, unchanged from #39: the polymer becomes **HDPE**, so GCyR's six part recipes get edited
  off `plate KaptonK` — a KubeJS recipe edit, no fork change — and the gate becomes ours rather than
  GCyR's.

### Option 1a — CraftTweaker, keep Kapton-K. **Rejected.**

**Registration surface it would need: also none — which is exactly why it loses.**

- CraftTweaker 21.0.38 is live on 1.21.1 NeoForge (2026-03-17) and Mekanism ships full CrT
  integration including chemical registration [verified/documented], so the option is real.
- But its whole justification was "Mekanism content registration wants CT". It does not, for this
  chain. Adding a scripting mod, a second scripting language and a second recipe-authoring idiom to
  buy a capability the pack already has is dependency risk for nothing — the same reasoning that
  declined IE+IP in #39, applied to a much smaller mod.
- Keeping Kapton-K is still *possible* without CT (its chain is entirely GT-side and already
  authored), but it would put refining back on GT's Chemical Reactor and Distillation Tower, which
  #39 explicitly declined to do.

### If the pack later does want a bespoke chemical

Install `kubejs_mekanism`. On **build.6** if KubeJS stays at `2101.7.1-build.181`; on **build.18**
only alongside a KubeJS bump to `2101.7.2-build.303` or newer. Then
`StartupEvents.registry('mekanism:chemical', e => e.create('planetaryfactory:x').gaseous().tint(…))`.
The addon also upgrades every Mekanism recipe from raw JSON to a typed schema, which is worth having
regardless of whether a new chemical is ever registered.

---

## Consequences for the map

- **#40 resolves without changing the mod list**, and #39's "one open fact" closes on 1b.
- The spec should say **HDPE**, not Kapton-K, as Terra's polymer, and should name **Light Fuel**
  rather than "kerosene" for the GT fraction.
- A build-time task falls out that is *not* research: GCyR's six `plate KaptonK` Assembler recipes
  (`MiscRecipes.java` 138–178) need KubeJS edits onto `mekanism:hdpe_sheet`.
- `kubejs_mekanism` build.6 is a cheap, reversible quality-of-life addition (typed schemas, no new
  content). Worth a separate decision; nothing on the spine waits on it.
