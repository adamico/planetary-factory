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
