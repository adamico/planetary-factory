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
scripts/factorio-resource-extract.py
python3 tests/factorio/test_tech_extract.py
python3 tests/factorio/test_recipe_extract.py
python3 tests/factorio/test_machine_extract.py
python3 tests/factorio/test_resource_extract.py
```

All four extractors read the same dump, so a single `--dump-data` run feeds them. Order
matters: the recipe extractor reads `technology.json`, and the machine extractor reads
`recipe.json` for its scope. The resource extractor reads only the dump.

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

- **`machine.json`** — the machines the conversion rule reads, and the fluid anchors it
  derives from. Three sections:

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

  **`drain` is derived.** Not one crafting machine in the game sets `drain` — the ten
  prototypes that do are inserters, pumps, turrets and lightning rods — so the figure is
  the engine's default of `energy_usage / 30` on an electric source, and nothing at all on
  a burner one. `drain_source` records which. #126 excludes drain from the conversion
  deliberately; this is the number that exclusion is quoted against.

  `containers`, the fluid anchors for the 1 unit = 1 mB derivation: a storage tank holds
  25 000 units over 3×3 tiles, a pipe 100 over 1×1.

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
