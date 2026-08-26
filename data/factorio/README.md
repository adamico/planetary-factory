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
python3 tests/factorio/test_tech_extract.py
```

The dump lands in `~/Library/Application Support/factorio/script-output/data-raw-dump.json`. The
extractor finds it and the Steam install by default; both are overridable with `--dump` and
`--factorio-data`.

Rerunning rewrites `kubejs/server_scripts/factorio_tech_data.js` too — the check fails if that file
is stale.

## Files

- **`technology.json`** — the tree. One object per technology: `name` (Factorio's kebab-case, the
  key `fromFactorio()` uses), `suggested_id`, `localised_name`, `source`, `prerequisites`,
  `cost_kind` (`packs` or `trigger`), `unit`, `research_trigger`, `effects`, `icon`.
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
