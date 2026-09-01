## Agent skills

### Issue tracker

Issues live in this repo's GitHub Issues, managed with the `gh` CLI. See `docs/agents/issue-tracker.md`.

### Triage labels

The five canonical triage roles, used verbatim as label strings. See `docs/agents/triage-labels.md`.

### Rejected scope

`.out-of-scope/` holds one file per rejected enhancement, so a `wontfix` keeps its reasoning and a
repeat request is recognised rather than re-argued. `/triage` reads it while gathering context. Only
rejected enhancements go there — never bugs, never something already built, never a deferral. A
Factorio mechanic the pack does not reproduce belongs in `docs/factorio-mechanics.md` instead, which
distinguishes `excluded` from `blocked`; a decision with a considered alternative belongs in an ADR.
See `.out-of-scope/README.md`.

### Domain docs

Single-context — `CONTEXT.md` and `docs/adr/` at the repo root. See `docs/agents/domain.md`.

### Testing policy

Which check a feature warrants — and whether it warrants one at all — is decided by the claim the
feature makes, not ad hoc per ticket. Six claims, six answers, and a content ticket names its check
kind explicitly so that "no check" is a recorded decision. See `docs/testing/what-to-check.md`.

### Worldgen check

`scripts/worldgen-check.py` launches a fresh world and asserts the loaded ore vein, bedrock
ore and worldgen layer registries against `tests/worldgen/expected.json`. A new body adds a
fixture entry, not code. See `docs/testing/worldgen-registry-check.md`.

### Flora data check

`tests/flora/test_flora_data.py` asserts Sapros's tree and surface data are internally consistent
— features, loot tables, blockstates, textures and lang against what is actually registered, plus
which marshland carries which tree and that no stromatolite drops ore — with no game launch. Run it
after any edit to the trees, the stromatolites or the five biomes.

### Starting-area geometry check

`tests/worldgen/test_start_geometry.py` asserts Terra's starting area can actually deal all three
ore fields: every hub connector sits on the face it points out of, and no two fields overlap each
other or the hub, for every hub variant against every combination of size variants. Vanilla drops
an overlapping jigsaw child silently, so this failure ships as "two patches instead of three" on
some seeds and nothing in a log. Run it after any edit to `scripts/build-terra-start.py`; it reads
the generated `.nbt` files, so it also catches forgetting to re-run the generator.

### ADR back-links

An ADR that contradicts a closed ticket's stated answer declares it as `supersedes: [55, 62]` in
frontmatter, and each named ticket gets a comment containing the literal `ADR-00NN`. Tickets are the
route and the ADRs are the state; without the back-link a closed ticket keeps asserting an answer an
ADR has overridden. Run `scripts/adr-backlink-check.sh` after committing an ADR that declares the
key — it needs an authenticated `gh`, so it is not part of any offline check. See
`docs/agents/domain.md`.

### Factorio mechanic ledger

`docs/factorio-mechanics.md` is the tracked list of every Factorio mechanic — base game and Space
Age — and what the pack does about it: one of `planned`, `shipped`, `adapted`, `blocked`,
`excluded`, never `undecided`. Read it before deciding a mechanic is out of scope, and update the
rows a ticket touches; a mechanic dropped without a row is exactly the failure it exists to catch.
It is not derived from `data/pack/subgroup-owner.json` and does not derive it — `not_emitted` there
is never evidence for `excluded` here — and it places nothing on a progression ladder, which is
#25's call. Row keys are Factorio's names by declared exception (ADR-0028).

### Factorio tech tree

The pack's research tree takes its shape from Factorio's, extracted rather than transcribed
(ADR-0022). `data/factorio/technology.json` is the committed reference; `researchd.js` declares
each research with `fromFactorio(name, {icon, unlocks, ...})` and supplies only the
Minecraft-specific parts. Regeneration and provenance are in `data/factorio/README.md`.
`tests/factorio/test_tech_extract.py` asserts the pruned tree is still a valid tree and that every
declared name exists — run it after re-extracting or after editing `researchd.js`.

### Pack manifest

The jar set is a packwiz manifest tracked in git (ADR-0024) — `pack.toml`, `index.toml` and one
`mods/*.pw.toml` per externally-sourced mod. `mods/*` is gitignored with `!mods/*.pw.toml` re-included;
never rewrite that as a bare `mods`, or the manifest silently stops being tracked. The two forked jars
are unmanaged hashed entries and `planetaryfactory_core` is not indexed at all. `scripts/pack-check.sh`
asserts the installed jars still match. See `docs/pack/packwiz-workflow.md`.

### First-party mod

`planetaryfactory_core` is a Gradle subproject in `mod/`, built from the repo root with
`./gradlew :planetaryfactory_core:installToPack` — required after a fresh clone, since the jar
lands in the gitignored `mods/`. It owns mechanism only; ADR-0015 has the ownership table for
what goes in the mod, in KubeJS and in datapack JSON. See `mod/README.md`.
