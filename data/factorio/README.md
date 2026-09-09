# Factorio technology reference

Reference input, not a build artifact. The pack's research tree takes its *shape* from Factorio's
(ADR-0022); these files are that shape, extracted from the game's own prototypes so it is never
retyped.

## Provenance

| | |
| --- | --- |
| Game | Factorio **2.1.16** (Space Age), Steam, mac-arm64 |
| Mods loaded | `base`, `elevated-rails`, `quality`, `recycler`, `space-age` |
| Technologies kept | 162 of 268 (106 pruned; see ADR-0022) |
| Science packs | 12 |

`space-age` hard-depends on `quality` and `elevated-rails`, so a base-only load is impossible and
the filter is applied after the dump. `recycler` is kept — it is Space Age content in every sense
but the folder name.

## Regenerating

Factorio dumps every loaded prototype with `--dump-data`. Point it at a mod directory containing
only the DLC mod list, or the dump picks up whatever is installed:

```sh
mkdir -p /tmp/fmods
cat > /tmp/fmods/mod-list.json <<'JSON'
{"mods":[{"name":"base","enabled":true},{"name":"elevated-rails","enabled":true},
         {"name":"quality","enabled":true},{"name":"space-age","enabled":true}]}
JSON

"$HOME/Library/Application Support/Steam/steamapps/common/Factorio/factorio.app/Contents/MacOS/factorio" \
  --dump-data --mod-directory /tmp/fmods

scripts/factorio-tech-extract.py
scripts/factorio-recipe-extract.py
scripts/factorio-machine-extract.py
scripts/factorio-fluid-extract.py
scripts/factorio-resource-extract.py
scripts/factorio-fuel-extract.py
python3 tests/factorio/test_tech_extract.py
python3 tests/factorio/test_recipe_extract.py
python3 tests/factorio/test_machine_extract.py
python3 tests/factorio/test_resource_extract.py
python3 tests/factorio/test_fuel_extract.py

scripts/factorio-fuel-convert.py
python3 tests/factorio/test_fuel_convert.py
```

The last pair is downstream of the extraction rather than part of it: `fuel.json` is joined
onto `data/pack/item-map.json` into the table the mod loads (ADR-0047), so a re-extraction
that moves a fuel has to be followed by a re-conversion or the game keeps the old table.

All six extractors read the same dump, so a single `--dump-data` run feeds them. Order
matters: the recipe extractor reads `technology.json`, the machine extractor reads
`recipe.json` for its scope, and the fluid extractor reads `machine.json` for its scope
(the fluid names the boiler's own fluid boxes filter on -- see below). The resource
extractor reads only the dump, and the fuel extractor reads `recipe.json` for a flag rather
than for a scope -- see below.

The dump lands in `~/Library/Application Support/factorio/script-output/data-raw-dump.json`. The
extractor finds it and the Steam install by default; both are overridable with `--dump` and
`--factorio-data`.

Rerunning rewrites `kubejs/server_scripts/factorio_tech_data.js` too — the check fails if that file
is stale.

## Files

- **`technology.json`** — the tree. One object per technology: `name` (Factorio's kebab-case, the
  key `fromFactorio()` uses), `suggested_id`, `localised_name`, `source`, `prerequisites`,
  `cost_kind` (`packs` or `trigger`), `unit`, `research_trigger`, `effects`, `icon`.
- **`recipe.json`** — the recipe corpus the pack's recipes are generated from (ADR-0026). One
  object per recipe: `name`, `category` (the primary one), `categories` (all of them),
  `unlocked_by` (the technology, or `null` for enabled-from-the-start), `energy_required`,
  `ingredients`, `results`, `allow_productivity`, `group`, `subgroup`, `order`.

  `group` and `subgroup` are Factorio's own taxonomy — `intermediate-products`, `logistics`,
  `production`, `combat`, `space` — and they are what the item map is argued in, one group at a
  time rather than one recipe at a time. They are *resolved*, not copied: only 219 of the game's
  662 recipes set a subgroup themselves, and the rest inherit it from their main product's item
  prototype, which may live under any of a dozen prototype types (`item`, `ammo`, `armor`,
  `capsule`, `module`, `fluid`, …).

  **Scope is Nauvis pre-launch**: every recipe unlocked by a technology whose pack cost is a
  subset of ADR-0018's four rungs, closed downward through prerequisites, plus the recipes
  enabled from the start. 164 of Factorio's 662. A later body widens it by widening
  `RUNG_PACKS` in the script.

  **Routing is on `category`, the first entry.** Factorio 2.x gives a recipe a *list* of
  categories, and the extras are the DLC machines that may also craft it — `transport-belt`
  is `["crafting", "metallurgy"]` because Vulcanus's foundry exists, not because a belt is a
  metallurgy recipe. `data/pack/category-map.json` holds the routing, and running the
  extractor reports `setMaxIOSize` per machine off this data, which is where ADR-0026's
  numbers come from.

- **`machine.json`** — the machines the conversion rule reads, the drill and power
  prototypes ADR-0043 and ADR-0048 are authored against, and the fluid anchors the unit
  derivation comes from. Six sections:

  `machines`, one object per crafting machine: `name`, `type` (`assembling-machine`,
  `furnace`, `rocket-silo`, `lab`), `crafting_speed`, `energy_usage` (W), `energy_type`,
  `drain` (W), `drain_source`, `module_slots`, `crafting_categories`, `fluid_boxes` and the
  tile footprint. A lab has no `crafting_speed`; its `researching_speed` is extracted into
  that field, and its `inputs` into `crafting_categories`, because the pack reads both the
  same way.

  **Scope is the recipe corpus's scope, not a second one**: a machine is kept when its own
  item recipe is in `recipe.json`. Twelve of the game's nineteen survive; the foundry, the
  biochamber, the recycler, the biolab, the cryogenic and electromagnetic plants and the
  captive biter spawner are the seven that do not. Widening `RUNG_PACKS` in the recipe
  extractor widens this file with it.

  **`burner` is the block ADR-0047 reads**: `fuel_categories` and `effectivity` on a burner
  machine, `null` on an electric one. Both burner furnaces come out at `["chemical"]` and
  `effectivity: 1`, which — with their identical 90 kW — is why the Steel tier gets twice the
  items from one coal. `#155` asserted that ratio in a javadoc and computed it from nothing;
  ADR-0047 computes it from these.

  **`drain` is derived.** Not one crafting machine in the game sets `drain` — the ten
  prototypes that do are inserters, pumps, turrets and lightning rods — so the figure is
  the engine's default of `energy_usage / 30` on an electric source, and nothing at all on
  a burner one. `drain_source` records which. #126 excludes drain from the conversion
  deliberately; this is the number that exclusion is quoted against.

  `drills`, `boilers` and `generators` — #188's widening. None of the three crafts, so none
  carries a `crafting_speed` or a `crafting_category`, and they are listed apart from
  `machines` rather than inside it: a null crafting speed in `machines` is indistinguishable
  from a number nobody extracted. They share the scope rule and the `energy_type`/`drain`/
  `burner` treatment with the crafting machines.

  A drill adds `mining_speed`, `resource_categories`, `module_slots` and its footprint — the
  burner drill is 2×2 at 0.25 speed and 150 kW off `["chemical"]`, the electric one 3×3 at
  0.5 speed and 90 kW. A boiler adds `energy_consumption` (W), `target_temperature`, `mode`
  `effectivity`
  and both fluid boxes with their volumes: 1.8 MW to 165 °C, 200 units in and 200 out, which
  is what ADR-0048's buffer is — the prototype decides it, not us. A boiler declares no
  `effectivity` of its own, so the figure is its energy source's, and 1 where the source
  states none. A generator adds
  `energy_source`, `effectivity`, `fluid_usage_per_tick`, `maximum_temperature` and its
  consumption box's temperature bounds, which are the whole contract: steam at 100 °C or
  above, up to 165.

  **`max_power_output` is derived.** The steam engine omits it, so the figure is the engine's
  own product — a tick's fluid × the degrees above the fluid's default temperature × its heat
  capacity × `effectivity` × 60 — and `max_power_output_source` records `derived` against
  `explicit`; there is no third value, because a generator whose consumption box names no
  fluid fails the extraction rather than shipping a null. It comes out at 900 kW, which is the number the wiki states and the pack's own
  Steam Engine is authored against even though Factorio's entity pays out electricity and the
  pack's pays out rotation.

  `containers`, the fluid anchors for the 1 unit = 1 mB derivation: a storage tank holds
  25 000 units over 3×3 tiles, a pipe 100 over 1×1.

  `pumps` — ADR-0050/#210's fourth prototype, alongside `drills`, `boilers` and
  `generators` for the same reason: an offshore pump crafts nothing, so it is listed apart
  from `machines` rather than inside it. Carries `pumping_speed` (per *Factorio tick*, not
  per second — see the fluid extractor's note below), `energy_source` (`void`; the pump
  draws no power, and the `energy_usage` sitting beside it in the corpus is a display
  figure with no consumer, per ADR-0050), and its fluid box. Scope is the same
  "its own item recipe is in `recipe.json`" rule as the rest of this file.

  `categories`, every recipe category in the game and every entity declaring it — the
  authority `data/pack/category-map.json`'s left-hand side is checked against.
  `hand-crafting` belongs to the character rather than to a machine, and `parameters` to
  nothing at all.

- **`science_packs.json`** — the twelve packs in Factorio's own order. Reference only: ADR-0018
  fixes the pack's spine at four rungs.

`cost_kind: "trigger"` technologies carry a `research_trigger` instead of a `unit`. They map to
Researchd's `checkItemPresence` — Factorio's triggers do not consume what they fire on, and neither
does the pack (ADR-0022).

`icon` is a Factorio texture path and is useless as a Minecraft texture. It survives only as a hint
when picking the `gtceu:`/`create:` item that stands in for the technology.

`effects` are kept raw, including the ones with no Minecraft analogue — a node worth dropping is a
decision to make while looking at the tree, not one a script makes silently. The one exception is
`recycling`, whose 313 generated reverse-craft recipes collapse to a single `unlock-recipe-family`
effect recording the rule that produced them.

- **`fuel.json`** — what a fuel item is worth, and to which burners (ADR-0047). Two sections.

  `fuels`, one object per fuel-bearing item prototype: `name`, `type` (`item` or `capsule`),
  `fuel_value` in joules, `fuel_value_raw` (Factorio's own `4MJ`/`1.21GJ` string, kept so the
  check can re-derive the number rather than trust it), `fuel_category` — defaulted to
  `chemical` the way the engine defaults it, since a null would read as "nobody extracted
  this" — and `in_corpus`.

  **`burnt_result` is deliberately not extracted.** Factorio's spent-fuel mechanic is real,
  has no ledger row, and belongs to #135; ADR-0047's dropping of the pack's vanilla
  `getCraftingRemainingItem` branch is a different mechanic with the same silhouette.

  **Scope is every fuel-bearing item, unfiltered** — deliberately *not* `machine.json`'s
  "its own item recipe is in the corpus" rule, which would drop `coal` and `wood`, since
  Factorio's two most important fuels are mined and harvested rather than crafted. Pack
  reachability is `data/pack/item-map.json`'s question, and the join that reads this file
  records its own skips; filtering here would make an unreachable fuel indistinguishable
  from an unmapped one. `in_corpus` records that distinction without acting on it.

  **Fluids are excluded.** `thruster-fuel` and `thruster-oxidizer` carry a `fuel_value` and
  no category: a thruster is not a burner energy source, and a fluid is not what a furnace
  slot holds.

  `categories`, every fuel category and every entity whose burner accepts it — the authority
  ADR-0047's category filter is checked against, and the reason the filter exists at all: a
  furnace takes `chemical` and nothing else, so `uranium-fuel-cell` is not furnace fuel
  merely by having a `fuel_value`.

  The denominator lives in `machine.json`, not here: ADR-0047 spends a fuel item's joules at
  the machine's own `energy_usage`, scaled by its burner `effectivity`.

- **`fluid.json`** — the two thermal constants ADR-0050/#210's pump:boiler ratio needs, and
  nothing else. One `fluids` array. **Scope is deliberately narrow**: only the fluids
  `machine.json`'s boiler already declares in its own fluid boxes (`water` in, `steam`
  out), read off that file rather than typed here, so a future boiler change widens the
  scope with it instead of this file drifting from it. It is not a general fluid corpus —
  widening to the dump's other 31 fluids would carry rows nothing reads.

  Per fluid: `name`, `heat_capacity` in joules, `heat_capacity_raw` (Factorio's own
  `0.2kJ`/`2kJ` string, kept the way `fuel.json` keeps `fuel_value_raw` so the check can
  re-derive rather than trust the parse), and `default_temperature`.

  **`water.heat_capacity` is a red herring.** It is `2kJ`, six times steam's `0.2kJ`, and it
  is *not* the term the boiler's arithmetic reads — see ADR-0050's "the ratio, which is the
  number that means something" and the trap comments in
  `tests/factorio/test_resource_extract.py`.

- **`resource.json`** — how much ore the ground holds (ADR-0041). Read from the dump's own
  `resource_autoplace_all_patches` rather than from map generation, because Factorio's
  amounts are closed-form in the prototypes.

  `starting_amount_formula` is the function's own expression, carried across as a string
  and evaluated against `controls` — frequency and size at a default map's 1 — so a
  starting total is derived at extraction and re-derived by the check, never typed. One
  object per resource: `base_density`, `has_starting_area_placement`, `starting_amount`,
  `stage_counts` and `stage_ratios`, and `distance_law` with the `max((1000 + distance) /
  2600, 1)` term parsed into its `flat_within` radius of 1600 tiles.

  **`stage_ratios` is each resource's own.** Uranium's `stage_counts` is the shared list
  scaled by about 2/3 and then rounded — its last rung is 50 where the scaling gives 53.3 —
  so the fraction sets agree to a tolerance the check states rather than exactly.

  `constants` and `outfield_law` carry `regular_density_at` and its three radii whole, for
  a later body siting outfield veins. **Scope is the six resources that function places**;
  `skipped` names the other planets' six, which have no starting patch to read.
