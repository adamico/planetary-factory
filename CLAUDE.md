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

### Furnace ladder check

`tests/pack/test_furnace_assets.py` asserts the three furnace tiers `FurnaceTier.java` registers
have their pack-side files: a blockstate covering both `facing` and `lit`, a model per state, an
item model, a lang key and a loot table. Two of its assertions are the ladder's own rather than
generic plumbing — the Electric tier's textures are asserted to exist *inside the GTCEu jar*, and
its model is asserted **not** to declare `"loader": "gtceu:machine"`, which GregTech's model
provider does not serve for a `planetaryfactory:` block. `tests/pack/test_smelting_type.py` holds
the recipe type itself: that the pack's recipe class is **not** assignable to vanilla's
`SmeltingRecipe` -- GT's `proxyRecipes` converts that class specifically and would drop the count,
turning `5 iron_plate -> 1 steel_plate` into a 1:1 with no error and no log line -- that the count
survives both codecs, and that nothing in the mod reads recipes off vanilla's smelting type. It is
a source-text check because the assertion needs to name a Minecraft class the unit-test classpath
deliberately does not have. The arithmetic and the rules are
Minecraft-free unit tests under `mod/src/test/java/com/planetaryfactory/core/smelting/`: the
per-tier duration, the 13 EU/t draw and its buffer, the unsided routing by item, and the stall —
a blocked output starts no smelt, burns no fuel and voids nothing (ADR-0041). Whether the three
blocks smelt in a running game is a world load, and its GameTests land with #156.

### Felling check

A tree is one entity holding an amount, and one gesture takes it whole (ADR-0051). Three checks,
none of which launches the game. `mod/src/test/java/com/planetaryfactory/core/felling/` is the rule:
`TreeShapeTest` is the fill over a block-position graph — it terminates on a ring of logs, respects
each of its three bounds, refuses a mid-trunk block, refuses a log cabin (no naturally-grown leaf),
never descends below the base, and does not cross into a touching canopy, which is vanilla's leaf
`distance` doing the work. `FellingCostTest` is the arithmetic: `amount × 0.1375s`, halved by
research, and a four-log tree costing Factorio's own 0.55s exactly — the rate is asked of
`TreeCorpus` rather than typed, because `0.5/4 = 0.125` is the *dead* trees' and the plants' rate and
#205 was written against it. `tests/factorio/test_tree_extract.py` re-derives the rate from the
corpus and names the three prototypes the discriminant must exclude, each of which yields a
different plausible-looking wrong number. `tests/factorio/test_pack_recipes.py` carries the two
hand-written recipe subtrees and the `fellable` tag: the sapling recipes' species list is read out
of Terra's biome files, since `oak_logs → oak_sapling` is only right while Terra grows oak, and a
`TagKey` whose JSON is missing resolves to an empty tag rather than an error — every tree silently
stops felling. Re-run `scripts/factorio-tree-extract.py` and then `scripts/build-tree-assets.py`
after a dump refresh; the second is the copy the mod reads. Whether a tree falls in a running game
is a world load.

### Starting kit check

The pocket and the hold `docs/spec/terra-progression.md` specifies are granted once per *player*
by `core/start/`, not once per join — a grant that re-fires on login is an unlimited iron supply
and would invalidate every pace reading after the first relog (#203). Two checks, neither of which
launches the game. `tests/pack/test_starting_kit.py` asserts every granted id resolves — ours
against the tier enums that produce the registry paths, the prospector and the quest book against
the installed jars, the hold against `data/pack/item-map.json` — and that the pocket is the spec's
pocket and the hold exactly the spec's three items: an id that names nothing is a silent empty slot,
and the moment the hold holds a green circuit rung 0 has stopped being taught.
`mod/src/test/java/com/planetaryfactory/core/start/` is the once-per-player rule and the flag's
codec round trip, which are Minecraft-free because the kit names items by string. Run both after
editing `core/start/` or the spec's Opening. Whether the kit is in the inventory at spawn is a
world load.

### Fuel table check

What a burner furnace burns is generated datapack JSON, not Forge's burn table (ADR-0047).
`scripts/factorio-fuel-convert.py` joins `data/factorio/fuel.json` onto `data/pack/item-map.json`
into `kubejs/data/planetaryfactory/fuel/`, and nothing is decided in the script: a fuel with no
item-map row, an `undecided` one or a fluid is a *recorded skip*, printed with its reason.
`tests/factorio/test_fuel_convert.py` asserts every decided fuel has a row and nothing else does,
that `uranium-fuel-cell` fails on category as well as on its row, that coal's row still buys 888
whole ticks at the Stone Furnace's own 4,500 J/t, that `wood` arrives as the tag `minecraft:logs`,
and that the mod's listener reads the folder the converter writes. The arithmetic and the
default-deny rule are `FuelBufferTest` and `FuelTableTest` under
`./gradlew :planetaryfactory_core:test`. Run all three after re-extracting the corpus, editing the
item map or touching `core/smelting/`. Whether a furnace burns a log in a running game is a world
load. See `docs/testing/fuel-table-check.md`.

### Assembler queue and resolver check

`mod/src/test/java/com/planetaryfactory/core/assembler/` asserts the Personal Assembler's queue:
Start takes the whole raw cost at once, a chain runs its steps in order and delivers only what no
remaining step needs, cancelling refunds the unspent reservation plus the intermediates already made,
a craft that will not fit pauses the head instead of dropping, and a paused head stops the plans
behind it. `AssemblerCodecsTest` is the data attachment's round trip, which ADR-0038 asks for by
name — a codec that drops a field does not crash, it returns a queue that silently emptied over a
logout. `PlanResolverTest` is the other half: chain-crafting, an intermediate already held being used
rather than remade, `Missing` against `Locked`, and `all` as the largest count the inventory covers.
`ItemKeyTest` is the identity itself (ADR-0052): an item is its registry id plus its data component
patch, encoded as one string, so an empty patch encodes to the bare id and every existing key is
unchanged, two differently-ordered patches encode identically, and matching is exact string equality
— a deliberate divergence from `neoforge:components`' subset match, without which the resolver would
have to compare `ItemStack`s and stop being a unit test. It is what lets Researchd's four science
packs, which are one item told apart by a component, be four items to the queue.
`PlanToQueueTest` is the seam between them, and the one neither side can assert alone — a plan the
resolver calls complete must be one the queue can run to the end, because a step the buffer cannot
feed throws *after* the reservation was taken. All five are
`./gradlew :planetaryfactory_core:test` with no game launch: the queue and the resolver name items by
string and the codec is DataFixerUpper's rather than Minecraft's, which is what keeps them checkable.

`tests/factorio/test_science_packs.py` is the emitted half of #222 — that both science pack recipes
are in the hand set and that every component-bearing output there is one the key format can name.
Whether the `RecipeGraph` admits them is a running server, and the absence of a refusal line naming
them is the signal.

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

### Vein indicator check

`tests/worldgen/test_vein_indicators.py` asserts every authored vein's surface indicator can
resolve. The field is an `Either<BlockState, Material>` and both sides fail only at world creation,
as `Failed to load registries` on the screen the player is sat in front of: a bare string is read
as a *material*, so `minecraft:cobblestone` there is an unknown registry key; and a material that
exists but has no surface rock — `gtceu:stone` is one — parses and then throws "No surface rock
registered" a layer later. Neither is visible from our own files, so the check reads the GTCEu jar:
the materials GregTech's own veins indicate with are the ones that demonstrably have a rock. Run it
after editing `scripts/build-terra-ore.py`. Both failure modes shipped once each before it existed.

### Ore amount checks

An ore block carries an amount and a break draws one unit (ADR-0041). That is four checks, none of
which launches the game: `tests/factorio/test_resource_extract.py` re-derives every starting total
from Factorio's own committed formula rather than trusting the number;
`mod/src/test/java/com/planetaryfactory/core/ore/` asserts a block pays out exactly what it holds
and that an exhausted position retires its delta, since a delta left behind is inherited by the next
block placed there; `tests/pack/test_ore_assets.py` walks all forty blockstate/model/texture hops
and asserts every ore block is in `c:ores`, which is the tag GregTech's miner scans;
and `MiningSpeedTest` asserts a field costs its *amount* times the tier's seconds rather than its
block count. Run them after editing anything under `core/ore/`, the two ore generators or the
extractor. See `docs/testing/ore-amount-check.md`.

### ADR back-links

An ADR that contradicts a closed ticket's stated answer declares it as `supersedes: [55, 62]` in
frontmatter, and each named ticket gets a comment containing the literal `ADR-00NN`. Tickets are the
route and the ADRs are the state; without the back-link a closed ticket keeps asserting an answer an
ADR has overridden. Run `scripts/adr-backlink-check.sh` after committing an ADR that declares the
key — it needs an authenticated `gh`, so it is not part of any offline check. See
`docs/agents/domain.md`.

### Offshore Pump check

`tests/pack/test_pump_assets.py` asserts the one block water enters the factory through (#213,
ADR-0050). `scripts/build-pump-assets.py` copies `data/factorio/machine.json`'s `pumps` row into a
resource the mod reads at class-init, the way `build-rig-assets.py` feeds `RigCorpus`, and the check
asserts that copy **field by field against the corpus** rather than against literals — a
hand-edited resource would run the pump at a rate somebody chose with nothing else failing. It also
holds the seam neither the corpus check nor the asset hops can see: that `pumping_speed` is still
Factorio's 20, so ADR-0050's "one pump feeds twenty boilers" has not quietly changed meaning; that
the item-map row names the block now that it exists; and that the **refusal message** has a lang key,
read out of `OffshorePumpItem` rather than typed, because a missing one renders the raw key on the
very gesture the message exists to explain.

The rule itself is Minecraft-free and lives under `mod/src/test/java/com/planetaryfactory/core/fluid/`:
`OffshorePumpSitingTest` is the predicate — one adjacent source, flowing refused, no minimum size —
and `OffshorePumpSpecTest` the two tick rates, which are the easiest thing here to get wrong, since
`pumping_speed` is stated per *Factorio* tick and its value happens to be Minecraft's tick rate.
`PumpCorpusTest` closes the loop by parsing the generated resource. Whether a pump placed against
the hub pool actually feeds a pipe is a world load.

### Boiler check

Terra's Boiler is the burner model's third customer (#224, ADR-0048): fuel and water in,
low-temperature steam out. Two checks, neither of which launches the game.
`mod/src/test/java/com/planetaryfactory/core/fluid/` holds the arithmetic and the stall —
`BoilerSpecTest` is the rate, and every figure in it is reachable by a wrong route that looks
right: the rise is paid for at **steam's** 0.2 kJ and water's is ten times larger (6 mB/s instead
of 60), and `energy_consumption` is per *second* against a buffer drained per tick. `BoilerCycleTest`
is the stall #224 names as mattering as much as the rate — a full steam tank makes no steam, burns
no fuel and, because water and room are asked *before* the fuel buffer is, lights no item either;
a boiler quietly eating coal into a full tank is a leak with no symptom.
`tests/pack/test_boiler_assets.py` is the pack side: the blockstate/model/texture/lang/loot hops,
which GregTech's model provider does not serve for a `planetaryfactory:` block, that `boiler`'s
item-map row is `authored` and names the block the mod registers rather than the LP Solid Boiler it
replaces, and a **second, independent derivation** of the 60 mB/s straight from the corpus. Run both
after editing `core/fluid/`, `scripts/build-steam-assets.py` or the corpus. Whether a placed Boiler
boils water is a world load, and — unlike the furnace ladder's, which are filed on #156 — no
GameTest is filed for it yet; that is a gap rather than a decision.

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
corpus. Note that this converter, `factorio-recipe-convert.py` and `create-recipe-convert.py` share
an output directory and each leaves the others' subtrees alone; run all three checks after touching
any. Whether the sweep kept the recipes in a running game is a world load. See
`docs/testing/grid-recipe-check.md`.

### Create kinetic recipe check

`tests/factorio/test_create_recipes.py` covers Create's own kinetic line — shaft, cogwheels,
gearboxes, water wheels, chute, funnel and tunnel — re-authored onto the pack's Assembling Machine
because ADR-0034's sweep removes Create's own and all 653 of its grid recipes sit on the vanilla
grid or the Mechanical Crafter, neither of which this pack executes. Two scripts, both committed:
`create-recipe-extract.py` dumps the corpus from the pinned jar into `data/create/recipe.json`, and
`create-recipe-convert.py` emits `recipe/assembling/create/` from it plus
`data/pack/create-substitutions.json`.

The one structural difference from the grid line: this converter is **closure-driven**. The
substitutions file names WANTED ROOTS and the converter walks the transitive closure, so adding a
kinetic component is one string there rather than a hand-written recipe file per ingredient it
drags in — which is the failure it was written against, ten hand-authored files that had drifted
from each other on the same substitution. A corpus row nothing converts is therefore normal here
and nowhere else in the repo.

The check asserts the closure is *closed* (every wanted root arrived, every substitution fired),
that counts survive the conversion, one hand recipe per item — Create's `*_from_conversion` pairs
are orientation swaps that craft directly here, so they are skipped rather than duplicated into EMI
— and that every `{"tag": ...}` names a tag that exists — `create:cogwheel` and `create:belt_connector` are items, not tags, and a tag that
does not exist matches nothing with no error in any log. Note that `create:shaft` and
`create:cogwheel` were substituted away by `grid-substitutions.json` and are now `keep`: they are
functional BLOCKS, and a recipe calling for a shaft means the shaft, not a rod that costs the same.
Whether the sweep kept these recipes in a running game is a world load. Whether a water wheel
then turns is Create's own business and not this pack's to check.
See `docs/testing/create-recipe-check.md`.

### Recipe duplication check

`tests/factorio/test_recipe_duplication.py` asserts no item is made by two emitted recipes unless
`MULTI_ROUTE` names it and says what the second route earns. Every other recipe check owns one
subtree and one input table, which is the right shape for "did this converter do its job" and blind
to the question none of them can ask: whether two converters, or one converter twice, made the same
item. Two routes to one block fails no schema, appears in no log and loads perfectly — it reaches
the player as two EMI entries for the same thing, and if both are `factorio_category: crafting` the
Personal Assembler's resolver has no cost model to choose between them. It shipped once, when
Create's two gearbox conversions and the large cogwheel's second route were emitted alongside the
direct recipes they duplicate and every subtree-local check passed. Four items legitimately have a
second route — Factorio's three solid-fuel oils and the three Power Grid conversion pairs, each of
which is the only route to its counterpart — and each is a row with its reason.

It also holds the **file-path invariant**, which is the other way one recipe becomes two entries and
the one nothing else can see: a GT recipe's first path component must equal its recipe type's path.
GregTech re-registers every loaded GTRecipe — `RecipeManagerLateMixin` strips everything before the
first `/` of the id and `GTRecipeBuilder.save` puts the type's path back on (#87, stated in full in
`factorio-recipe-convert.py`'s `emitted_path`) — so `recipe/grid/copper_coil.json` lands in the
manager as BOTH `planetaryfactory:grid/copper_coil` and `planetaryfactory:assembling/copper_coil`.
The file is valid, the sweep keeps it, and `ServerEvents.recipes` runs BEFORE the re-registration,
so even a probe inside the recipe event sees one recipe; only EMI shows the two. That is why
`grid/`, `create/` and the hand-written `pack/` all sit INSIDE `assembling/` — the Factorio
converter had the rule from #87 and the other three subtrees did not, so it shipped 91 duplicate
entries. `planetaryfactory:smelting` is the pack's own class, not a GTRecipe, so its four recipes
are not cloned and stay flat; that exemption is `FLAT_TYPES`, recorded rather than assumed.

Run it after any converter change. It does not assert the routes are balanced; costing is a
decision.

### Hand-written recipe check

`kubejs/data/planetaryfactory/recipe/assembling/pack/` is the one subtree no converter generates: ADR-0039's
two Engineer's Pick recipes, which the corpus can never author because Factorio has no mining-tool
prototype. `tests/factorio/test_pack_recipes.py` is what holds them, since every other recipe here
is checked against the corpus and these are checked against nothing otherwise — that both
converters and the converter's own check still list `pack` as foreign (a run that forgets deletes
them, and the sweep leaves no stock pickaxe to fall back on), that both land on a surface
`recipe_survivors.js` admits and carry `factorio_category: crafting` so the Personal Assembler
plans them at rung 0, that the steel recipe consumes the iron pick, and that each registered tier
has its model, texture, lang key, the two wrench tags that carry the dismantle verb and the block
tag the jar asks for by name. The Iron Pick's sprite is vanilla's own and the Steel Pick's is
GTCEu's Damascus Steel pickaxe, flattened by `scripts/build-pick-textures.py` because GT's tool art
is three greyscale layers that only become a material under a colour handler our item never
reaches; the check runs that script's `--check`, so a GTCEu update that redrew the art fails rather
than shipping the old sprite. The tier list is read out of `PickTier.java`. The pick's arithmetic —
that Factorio's seconds survive Minecraft's break-time formula — is `MiningSpeedTest` under
`./gradlew :planetaryfactory_core:test`. Whether the Pick mines every block class, dismantles a GT
machine and satisfies Create's wrench is a world load. See
`docs/testing/hand-written-recipe-check.md`.

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