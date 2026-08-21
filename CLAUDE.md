## Agent skills

### Issue tracker

Issues live in this repo's GitHub Issues, managed with the `gh` CLI. See `docs/agents/issue-tracker.md`.

### Triage labels

The five canonical triage roles, used verbatim as label strings. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context — `CONTEXT.md` and `docs/adr/` at the repo root. See `docs/agents/domain.md`.

### Worldgen check

`scripts/worldgen-check.py` launches a fresh world and asserts the loaded ore vein, bedrock
ore and worldgen layer registries against `tests/worldgen/expected.json`. A new body adds a
fixture entry, not code. See `docs/testing/worldgen-registry-check.md`.

### Flora data check

`tests/flora/test_flora_data.py` asserts Sapros's tree data is internally consistent — features,
loot tables, blockstates, textures and lang against what is actually registered — with no game
launch. Run it after any edit to the trees.

### First-party mod

`planetaryfactory_core` is a Gradle subproject in `mod/`, built from the repo root with
`./gradlew :planetaryfactory_core:installToPack` — required after a fresh clone, since the jar
lands in the gitignored `mods/`. It owns mechanism only; ADR-0015 has the ownership table for
what goes in the mod, in KubeJS and in datapack JSON. See `mod/README.md`.
