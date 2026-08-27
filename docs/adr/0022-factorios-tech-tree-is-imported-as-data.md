---
status: accepted
---

# Factorio's tech tree is imported as data, not transcribed

ADR-0018 made Factorio's science packs the spine and Researchd the lab. It did not say where the
*shape* of the tree comes from — which research is downstream of which, in what order, at what cost.
Left unstated, that shape gets invented one research at a time, and a Factorio-literate player —
the audience the pack exists for — reads the result as arbitrary, because it is.

Factorio has spent a decade playtesting the order in which a factory learns things. **That ordering
is imported as data, from the game's own prototypes, and is never retyped.** What is not imported is
Factorio's content: its recipes, its items, its planets. Those are ours.

## What is imported, and what is not

`scripts/factorio-tech-extract.py` reads `factorio --dump-data` output and writes
`data/factorio/technology.json`. That file carries topology, names, costs and effects. Every
Minecraft-facing decision — which item is the icon, which recipe is unlocked, which of our bodies
gates it — is hand-authored in `kubejs/server_scripts/researchd.js` against it.

The seam is `fromFactorio(name, { ... })`. The data supplies `literalName`, `parents` and
`consumePack`; the call supplies `icon`, `unlocks`, and where needed `method`, `gatedBy` and
`costScale`. **A research is roughly four lines, all four of them decisions only we can make.**
Re-extracting after a Factorio update refreshes the topology without touching the script.

**No KubeJS is generated from the tree.** The extractor emits `factorio_tech_data.js` — the same
JSON as a top-level `var`, because the pack's KubeJS version is pinned and `JsonIO`'s path semantics
move between versions — but that file is data, never hand-edited, and there is nothing to keep in
sync. ADR-0023 makes that caution concrete: KubeJS relocated its own bindings package between
`2101.7.1` and `2101.7.2`, which is exactly the class of change a runtime file read would not
survive.

**Cross-file sharing is the shared `topLevelScope`, never `global`.** KubeJS evaluates every script
of a type against one scope, so a top-level `var` or `function` is visible to every file loading
after it. KubeJS's `global` binding looks like the obvious namespace and is not one: it is an
**unmodifiable Java map**, so writes fail and reads return `undefined`. A DSL built on it registers
no researches at all and reports nothing — the failure is total and silent, which is why the check
forbids `global.` outright.

**Load order is the `// priority:` header, not the filename.** KubeJS sorts scripts by that header
descending and is otherwise arbitrary — so `factorio_tech_data.js` is 20, `factorio_tech_dsl.js` is
10 and `researchd.js` is 0. Naming files to sort alphabetically does nothing; the first launch
loaded `researchd.js` second of six and threw `ReferenceError: "fromFactorio" is not defined`. That
costs one script out of six and leaves the other five, the recipes and the worldgen check all
passing, so the pack looks healthy.

**Everything in the DSL is `var`.** KubeJS's Rhino throws `TypeError: redeclaration of var <name>`
when a `const` or `let` inside a nested block is re-entered, and the flush re-enters one per
technology. It throws inside the event handler at runtime, so the scripts still load, `0 errors` is
still reported for the file, and the only symptom is Researchd reporting a parent that "does not
exist" — the flush having died partway through registering.

Both are asserted by the check, because neither is visible to any amount of reading. The researches themselves are written by hand. A generator would emit 162 nodes of
`TODO` and we would edit all of them anyway, now with a generator to maintain.

## Three families are dropped, because Researchd cannot express them

Of Factorio's 268 base + Space Age technologies, **106 are dropped**: those with
`max_level = "infinite"`, those costed by a `count_formula` rather than a number, and those flagged
`upgrade = true` — the levelled bonus chains for robot speed, braking force, stack size, inserter
capacity. Researchd has no concept that fits any of them, and a hand translation would be invented
rather than imported, which is the thing this ADR exists to prevent.

`upgrade` is the load-bearing flag; the other two mostly co-occur with it. **Dropping is not
orphaning**: a dropped node is transparent, and its children are re-pointed to its nearest surviving
ancestors. That walk is the only real logic in the extractor, and it is what
`tests/factorio/test_tech_extract.py` checks — its failure mode is a tree that loads fine and
silently omits half the pack.

A technology whose name ends in a digit but carries none of the three flags is **reported, not
filtered**, so a family this misses is visible rather than silent. `automation-2`, `logistics-3` and
`military-4` are genuine distinct technologies and are kept.

## Space Age's research triggers are `checkItemPresence`, and the pack has no `consumeItem`

Thirty-one of the surviving technologies have no science-pack cost at all. Space Age instead fires
them on an action: crafting an item, mining an entity, capturing a spawner. That is the least
obvious and most useful thing the extraction found, so `cost_kind` is an explicit discriminator in
the data rather than something inferred from a missing field.

**A Factorio research trigger does not consume what it fires on.** Craft fifty iron plates and the
technology unlocks; you keep the plates. Researchd's `checkItemPresence` is the same shape — it
counts matching items and latches, monotonically, without taking them — so it is the faithful
mapping, and the DSL exposes it as `has: ['item id', count]`. **`consumeItem` is used nowhere in
the pack**, and the check enforces that.

Reading Researchd 1.2.5 to establish this corrected a belief worth writing down. **`consumeItem`
scans the player's own inventory** and shrinks the largest stacks — exactly as `checkItemPresence`
does, minus the shrink. **Only `consumePack` reads the Research Lab.** So ADR-0018's automation rule
rests on `consumePack` alone: a `consumeItem` gate was never pipeable, and swapping it for a
presence check gives up no automation, only the destruction.

The four researches written before this ADR turned out to already be faithful ports: `steam-power`
is Factorio's `craft-item iron-plate ×50`, `automation-science-pack` is its `craft-item lab`, and
`logistic-science-pack` carried Factorio's `75, 5` verbatim. The DSL reproduces the hand-written
file from the data. That is the evidence the seam is in the right place.

## Planet gating is ours

Factorio gates on Vulcanus, Fulgora, Gleba and Aquilo. **We gate on Terra, Nauvis and Sapros**
(ADR-0020, ADR-0021), and those are not the same four bodies wearing different names. So the
extracted data carries **nothing planet-shaped**: `prerequisites` are Factorio's, and our gating is
applied at authoring time through `gatedBy`, which appends to them.

Ordering *within* a tier is imported faithfully, because that is the playtested part. Which body a
tier is reached from is a decision this pack makes for itself. Researchd's `unlockDimensions` is the
lever, exposed as `unlocksDimensions` alongside `gatedBy`; nothing in the extracted data implies
either.

## `recycler` is Space Age, whatever the folder says

Factorio ships `recycling` in its own data directory for packaging reasons. It is not a
technicality: it is gated on discovering Fulgora, it unlocks the recycler, and `holmium-processing`
depends on it. **It is extracted.** `quality` and `elevated-rails` are not — quality is an orthogonal
system rather than a tier, and elevated rail has no analogue in a Minecraft pack.

`recycling` unlocks 314 recipes, an order of magnitude more than any other technology. They are not
314 decisions: Factorio generates one per existing recipe, returning a quarter of its ingredients.
**The rule is recorded, not its output** — a single `unlock-recipe-family` effect — because that is
both smaller and truer, and because in this pack "recycling exists" is one decision about GregTech
maceration.

## Considered Options

- **Write a Lua script against the runtime API** (the premise this was chartered on). Rejected:
  technologies are data-stage prototypes, and the runtime view is both harder to serialise
  field-by-field and reflects the *player's* modded state rather than the game's.
- **Transcribe the tree by hand from the wiki.** Rejected: 162 technologies × topology and costs is
  where transcription errors live, and a Factorio update makes the whole transcription stale with no
  signal.
- **Generate `researchd.js` from the data.** Rejected: every research needs a hand-chosen icon,
  recipe id and gate. Generation would produce a file that must be regenerated and hand-edited both,
  which is the worst of the two.
- **A single overlay map plus a loop**, instead of 162 `fromFactorio()` calls. Rejected narrowly: it
  is tighter by two lines per research, but a per-research exception has nowhere local to live, and
  nearly every research will eventually want a `costScale`, a `gatedBy` or a bespoke `method`.
- **`consumeItem` for the trigger technologies**, which is what the first four researches used.
  Rejected once the mod was read: Factorio's triggers do not consume, `consumeItem` reads the same
  player inventory `checkItemPresence` does, and destroying the item taxes the player for the
  capability they just demonstrated. It buys no automation, because the Lab feeds neither.
- **Keep the upgrade chains and collapse each to a single research.** Rejected: the collapse is an
  invention, and inventions are what this ADR removes. If a bonus ladder is wanted later it is
  designed as ours, not imported as a flattened Factorio artifact.

## Consequences

- **The extracted file is committed, with its provenance.** `data/factorio/README.md` records the
  game version and the exact command; a re-extraction from a different version is a visible diff.
- **A research absent from `researchd.js` is absent from the pack**, and the DSL logs every
  undeclared technology on reload. With 162 nodes and incremental authoring, "which have I not done
  yet" is answered by the game rather than tracked by hand.
- **The pack's spine is four rungs; the data has twelve packs.** ADR-0018 fixes the ladder at
  `automation`, `logistic`, `chemical`, `production`, with military dropped and utility and space
  reserved for after the first launch. `data/factorio/science_packs.json` records all twelve in
  Factorio's order as *reference*. **ADR-0018 remains the spine**; a fifth rung is a decision that
  ADR supersedes, not something the extracted file grants by existing.
- **The pack's pack ids follow Factorio's spelling.** `logistics_science_pack` is renamed to
  `logistic_science_pack`, matching both Factorio and ADR-0018's table.
- **A `fromFactorio()` typo is silent in-game** — the override sits in a map nothing looks up — so
  the check asserts every declared name exists.
- **Researchd's builder holds one method and one effect.** `consumePack()` delegates to `method()`,
  and both fields overwrite rather than accumulate — so calling either in a loop keeps only the last
  value, silently. 117 of the 130 pack-costed technologies need more than one pack, and 60 unlock
  more than one recipe, so the DSL uses `consumePacks()` and `unlockRecipes()`, joining multiple
  effect kinds with `and()`. The check asserts the call shape, because nothing at runtime does.
- **`consumePacks()` is only correct while every ingredient amount is 1**, which is true of
  Factorio's whole tree today. The check asserts that data fact next to the code that depends on it;
  if it ever breaks, the cost needs `and(...)` composition instead.
- **Re-extraction is expected, not exceptional.** A Factorio update that reshapes the tree is a
  script rerun and a diff to read, not a re-transcription.
- **A datapack and KubeJS cannot both declare a research.** Researchd's `ReloadableRegistryManager`
  puts datapack JSON entries and KubeJS entries into one `ImmutableMap.Builder` — JSON first, KubeJS
  second — and calls `build()`, not `buildKeepingLast()`. Guava throws on a duplicate key, so an id
  declared in both places fails the registry load outright rather than one source winning. Anything
  that emits researches as data must therefore replace `fromFactorio` for those ids in the same
  change, or take a disjoint namespace.
- **Researchd has a GUI research editor, and it writes datapacks.** `EditorDatapackWriter` emits
  `pack.mcmeta` and `data/<namespace>/researchd/research/*.json`, editing dependencies, effects,
  methods and icons. **It writes into the world save, not into pack content**:
  `CreateDatapackPayload` fixes the root to
  `MinecraftServer.getWorldPath(LevelResource.DATAPACK_DIR)`, and `saves` is gitignored — so
  anything edited in the GUI is outside version control until a copy step brings it back, and a
  shipped copy of the same ids cannot be loaded alongside the world-local one. It is not a layout tool: `DisplayImpl` holds only `name`
  and `desc`, and placement is derived from `parents`. So the editor edits both halves of the seam
  this ADR draws — the extracted `parents` and the hand-authored icon, unlocks and method.
  **Whether the pack should emit its tree in that form is open, and tracked in issue #82.** Note
  what the rejection above does and does not cover: it rejects generating `researchd.js`, the
  *editing surface*. The tree itself is already generated — that is what this ADR decides — so
  emitting it as data is not the rejected option wearing a new format. What #82 must answer is
  narrower: which surface the hand-authoring happens on.
- **The editor reads researches through a different path than the commands do.** It enumerates via
  `ResearchdApi.getResearchManager()`, whose map includes the KubeJS entries, so the pack's tree is
  visible and browsable in it — confirmed in play. Researchd's own unlock/remove commands resolve
  through the vanilla datapack registry instead and cannot see KubeJS researches at all (#77). Same
  mod, two enumeration paths, opposite visibility. **Browsing is safe; saving is not.** An edit
  saved onto an id `researchd.js` also declares emits JSON for that id and trips the duplicate-key
  failure above on the next reload.
