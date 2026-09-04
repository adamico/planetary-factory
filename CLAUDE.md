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

### Machine registration check

`tests/pack/test_machine_assets.py` asserts the machines `kubejs/startup_scripts/machines.js`
registers still agree with everything that names them: the registered `setMaxIOSize` against the
corpus envelope, `data/pack/category-map.json`'s `recipe_type` against what the script actually
creates, each machine's lang key against the id its builder produces, and — for the multiblock,
whose `kubejs:` namespace GregTech's model provider does not serve — every hop from blockstate to
model to texture. The two builders land in different namespaces, so the lang assertion is not
cosmetic. Run it after editing that script, the category map or the machine lang files. Whether a
machine's GUI and pattern behave is a world load, not a static check.

### Assembler queue and resolver check

`mod/src/test/java/com/planetaryfactory/core/assembler/` asserts the Personal Assembler's queue:
Start takes the whole raw cost at once, a chain runs its steps in order and delivers only what no
remaining step needs, cancelling refunds the unspent reservation plus the intermediates already made,
a craft that will not fit pauses the head instead of dropping, and a paused head stops the plans
behind it. `AssemblerCodecsTest` is the data attachment's round trip, which ADR-0038 asks for by
name — a codec that drops a field does not crash, it returns a queue that silently emptied over a
logout. `PlanResolverTest` is the other half: chain-crafting, an intermediate already held being used
rather than remade, `Missing` against `Locked`, and `all` as the largest count the inventory covers.
`PlanToQueueTest` is the seam between them, and the one neither side can assert alone — a plan the
resolver calls complete must be one the queue can run to the end, because a step the buffer cannot
feed throws *after* the reservation was taken. All four are
`./gradlew :planetaryfactory_core:test` with no game launch: the queue and the resolver name items by
string and the codec is DataFixerUpper's rather than Minecraft's, which is what keeps them checkable.

`tests/factorio/test_hand_resolver.py` is the corpus half — that the *design* terminates. All 113
category-`crafting` recipes resolve to plans bottoming out in the 21 known leaves, no item has two
hand recipes (the resolver picks a route with no cost model), and there are no cycles. It reads
`data/factorio/recipe.json` and fails the day a regeneration adds a recipe nothing hand-makes.

Run them after editing anything under `core/assembler/` or after re-extracting the corpus. Whether
EMI's Fill Recipe reaches the panel and a plan delivers is a world load, not a static check.

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

### Recipe conversion

`scripts/factorio-recipe-convert.py` turns the extracted corpus into GregTech recipe JSON under
`kubejs/data/planetaryfactory/recipe/`, reading five committed data files: the corpus, the category
map, the subgroup owners, `data/pack/item-map.json` and `data/pack/recipe-overrides.json`. Nothing is
decided in the script — a decision is a diff to a design document. Generated output is never
hand-edited; re-run the converter. A Factorio name with no item-map row is a hard failure, while an
`undecided` row is a recorded skip. `tests/factorio/test_recipe_convert.py` is the static check and
runs the converter's `--check`; the recipe *shape* needs one world load. See
`docs/testing/recipe-conversion-check.md`.

### Stock-recipe sweep

`kubejs/server_scripts/recipes.js` removes every recipe the pack does not admit by name, and
`recipe_survivors.js` is the allowlist it negates (ADR-0034: a stock recipe ships only if a
decision names it and names the surface it is crafted on). A survivor is a *surface*, not a
recipe, and its filter's recipe type must be the one `data/pack/category-map.json` registers for
that machine — so a machine landing later without a survivor entry fails
`tests/factorio/test_recipe_sweep.py` rather than having its recipes swept in silence. Run that
check after editing either script, the category map or the emitted recipes; whether the sweep
removed the right things in a running game is a world load, not a static check.

### Grid recipe check

`tests/factorio/test_grid_recipes.py` covers Create: Power Grid's recipes, re-authored onto the
pack's Assembling Machine because ADR-0034's sweep removes the mod's own and 84 of its 112 sit on
surfaces no block here executes (#172). `scripts/powergrid-recipe-convert.py` generates them from
two committed inputs — `data/powergrid/recipe.json`, the extracted corpus, and
`data/pack/grid-substitutions.json`, where every ingredient judgement lives with its reason.
Nothing is decided in the script, and an ingredient in neither the `keep` nor the `substitute`
table is a hard failure: under a default-deny sweep a vanilla item is not obtainable just because
it is vanilla, and half of Power Grid's ingredients are zinc-bearing against an alphabet ADR-0021
closed. The check also asserts one hand recipe per item and no cycles over the **union** the
Personal Assembler loads, which `test_hand_resolver.py` cannot see — it reads only the Factorio
corpus. Note that this converter and `factorio-recipe-convert.py` share an output directory and
each leaves the other's subtree alone; run both checks after touching either. Whether the sweep
kept the recipes in a running game is a world load. See `docs/testing/grid-recipe-check.md`.

### Research unlock check

`tests/factorio/test_research_unlocks.py` asserts every recipe id a research grants is a recipe the
pack emits. Researchd gates by recipe id and a recipe's id follows its type, so re-surfacing a
recipe leaves the research locked to an id nothing emits — with no error, no failed recipe and no
log line, reaching the player as a research that unlocks nothing. Run it after editing
`researchd.js` or after re-running the converter. It is the second half of #97; the first half —
nothing emitted or admitted is a vanilla grid recipe — lives in the sweep check. See
`docs/testing/research-unlock-check.md`.

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

<!-- rtk-instructions v2 -->
# RTK (Rust Token Killer) - Token-Optimized Commands

## Golden Rule

**Always prefix commands with `rtk`**. If RTK has a dedicated filter, it uses it. If not, it passes through unchanged. This means RTK is always safe to use.

**Important**: Even in command chains with `&&`, use `rtk`:
```bash
# ❌ Wrong
git add . && git commit -m "msg" && git push

# ✅ Correct
rtk git add . && rtk git commit -m "msg" && rtk git push
```

## RTK Commands by Workflow

### Build & Compile (80-90% savings)
```bash
rtk cargo build         # Cargo build output
rtk cargo check         # Cargo check output
rtk cargo clippy        # Clippy warnings grouped by file (80%)
rtk tsc                 # TypeScript errors grouped by file/code (83%)
rtk lint                # ESLint/Biome violations grouped (84%)
rtk prettier --check    # Files needing format only (70%)
rtk next build          # Next.js build with route metrics (87%)
```

### Test (60-99% savings)
```bash
rtk cargo test          # Cargo test failures only (90%)
rtk go test             # Go test failures only (90%)
rtk jest                # Jest failures only (99.5%)
rtk vitest              # Vitest failures only (99.5%)
rtk playwright test     # Playwright failures only (94%)
rtk pytest              # Python test failures only (90%)
rtk rake test           # Ruby test failures only (90%)
rtk rspec               # RSpec test failures only (60%)
rtk test <cmd>          # Generic test wrapper - failures only
```

### Git (59-80% savings)
```bash
rtk git status          # Compact status
rtk git log             # Compact log (works with all git flags)
rtk git diff            # Compact diff (80%)
rtk git show            # Compact show (80%)
rtk git add             # Ultra-compact confirmations (59%)
rtk git commit          # Ultra-compact confirmations (59%)
rtk git push            # Ultra-compact confirmations
rtk git pull            # Ultra-compact confirmations
rtk git branch          # Compact branch list
rtk git fetch           # Compact fetch
rtk git stash           # Compact stash
rtk git worktree        # Compact worktree
```

Note: Git passthrough works for ALL subcommands, even those not explicitly listed.

### GitHub (26-87% savings)
```bash
rtk gh pr view <num>    # Compact PR view (87%)
rtk gh pr checks        # Compact PR checks (79%)
rtk gh run list         # Compact workflow runs (82%)
rtk gh issue list       # Compact issue list (80%)
rtk gh api              # Compact API responses (26%)
```

### JavaScript/TypeScript Tooling (70-90% savings)
```bash
rtk pnpm list           # Compact dependency tree (70%)
rtk pnpm outdated       # Compact outdated packages (80%)
rtk pnpm install        # Compact install output (90%)
rtk npm run <script>    # Compact npm script output
rtk npx <cmd>           # Compact npx command output
rtk prisma              # Prisma without ASCII art (88%)
rtk uv run <cmd>        # Compact uv project command output
```

### Files & Search (60-75% savings)
```bash
rtk ls <path>           # Tree format, compact (65%)
rtk read <file>         # Code reading with filtering (60%)
rtk grep <pattern>      # Search grouped by file (75%). Format flags (-c, -l, -L, -o, -Z) run raw.
rtk find <pattern>      # Find grouped by directory (70%)
```

### Analysis & Debug (70-90% savings)
```bash
rtk err <cmd>           # Filter errors only from any command
rtk log <file>          # Deduplicated logs with counts
rtk json <file>         # JSON structure without values
rtk deps                # Dependency overview
rtk env                 # Environment variables compact
rtk summary <cmd>       # Smart summary of command output
rtk diff                # Ultra-compact diffs
```

### Infrastructure (85% savings)
```bash
rtk docker ps           # Compact container list
rtk docker images       # Compact image list
rtk docker logs <c>     # Deduplicated logs
rtk kubectl get         # Compact resource list
rtk kubectl logs        # Deduplicated pod logs
```

### Network (65-70% savings)
```bash
rtk curl <url>          # Compact HTTP responses (70%)
rtk wget <url>          # Compact download output (65%)
```

### Meta Commands
```bash
rtk gain                # View token savings statistics
rtk gain --history      # View command history with savings
rtk discover            # Analyze Claude Code sessions for missed RTK usage
rtk proxy <cmd>         # Run command without filtering (for debugging)
rtk init                # Add RTK instructions to CLAUDE.md
rtk init --global       # Add RTK to ~/.claude/CLAUDE.md
```

## Token Savings Overview

| Category | Commands | Typical Savings |
|----------|----------|-----------------|
| Tests | vitest, playwright, cargo test | 90-99% |
| Build | next, tsc, lint, prettier | 70-87% |
| Git | status, log, diff, add, commit | 59-80% |
| GitHub | gh pr, gh run, gh issue | 26-87% |
| Package Managers | pnpm, npm, npx | 70-90% |
| Files | ls, read, grep, find | 60-75% |
| Infrastructure | docker, kubectl | 85% |
| Network | curl, wget | 65-70% |

Overall average: **60-90% token reduction** on common development operations.
<!-- /rtk-instructions -->