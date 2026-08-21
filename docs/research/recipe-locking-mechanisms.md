# What can gate a recipe behind progression on 1.21.1 / NeoForge

**Retrieved**: 2026-08-21. Resolves #35 (map #25).
**Premise inherited from `docs/research/ftb-quests-task-screens.md`**: FTB Quests does not do
per-player recipe locking. That still holds, and this document does not re-derive it — but it turns
out FTB Quests *does* supply the **trigger** half of a gate (a stage reward), and something else has
to supply the **enforcement** half.

**Answer in one line: the GameStages / Recipe Stages lineage is dead on 1.21.1 and KubeJS deleted its
own `.stage()` because it was only ever a shim over Recipe Stages; the one enforcing mechanism already
present in `mods/` is GregTech CEu's `RecipeCondition` system (GT machine recipes only, visible not
hidden), the best third-party option is AStages (vanilla stations only), and every recipe-viewer
mechanism — JEI's, EMI's, KubeJS's — hides without locking, which for this pack is cosmetic.**

---

## Method and trust levels

Every claim below is tagged:

- **[jar]** — read out of a jar actually in `mods/`, by `javap` disassembly. Highest trust.
- **[src]** — read from the mod's own Git repository or its published API.
- **[api]** — read from a first-party registry API (Modrinth `/v2/project`, GitHub REST).
- **[unverified]** — nobody's primary source confirms it; called out as such.

Jars read (all from this pack's `mods/`):

| Jar | Version |
| --- | --- |
| `gtceu-1.21.1-7.0.2.jar` | GTCEu 7.0.2 |
| `kubejs-neoforge-2101.7.1-build.181.jar` | KubeJS 2101.7.1 build 181 (built 2024-10-13) |
| `ftb-library-neoforge-2101.1.35.jar` | FTB Library 2101.1.35 |
| `ftb-xmod-compat-neoforge-21.1.11.jar` | FTB XMod Compat 21.1.11 |
| `ftb-quests-neoforge-2101.1.31.jar` | FTB Quests 2101.1.31 |
| `jei-1.21.1-neoforge-19.44.0.402.jar` | JEI 19.44.0.402 |
| `emi-1.1.24+1.21.1+neoforge.jar` | EMI 1.1.24 |

Also relevant: **both JEI (`jei-1.21.1-neoforge-19.44.0.402`) and EMI (`emi-1.1.24+1.21.1`) are
installed.** Anything that only integrates with one of them leaves the other showing the locked
recipe. **No GameStages, ItemStages, Recipe Stages or Chapters jar is present** — the pack has no
stage *enforcement* mod today.

---

## 1. The GameStages / Recipe Stages lineage: dead on 1.21.1

| Mod | Latest release | Loader | Status |
| --- | --- | --- | --- |
| **Game Stages** (Darkhax) | `GameStages-Forge-1.20.1-15.0.2`, 2024-10-07 | Forge | repo not archived, last push 2024-10-07, default branch `1.20.4`; **no 1.21.x file on CurseForge** |
| **Recipe Stages** (jaredlll08) | 2024-03-11 | Forge | Modrinth game versions stop at **1.20.1** |
| **Item Stages** (Darkhax) | 2024-03-26 | Forge | Modrinth game versions stop at **1.20.3** |

Sources: GitHub REST on `Darkhax-Minecraft/Game-Stages` (`archived: false`, `pushed_at:
2024-10-07T02:36:06Z`, `default_branch: 1.20.4`) **[api]**; Modrinth `/v2/project/recipe-stages`
(`loaders: ["forge"]`, newest `game_versions` entry `1.20.1`) and `/v2/project/item-stages`
(`loaders: ["forge"]`, newest `1.20.3`) **[api]**; the Game Stages CurseForge file list, whose three
newest entries are 1.20.1 / 1.19.2 / 1.18.2 **[src]**.

Note the shape of the lineage, because it matters for what a replacement has to do: **Game Stages
was only ever a stage *store*.** It held "player X has string Y". It locked nothing. *Recipe Stages*
and *Item Stages* were separate mods that consumed the store and did the locking. So "GameStages is
gone" is really two losses, and the store half turns out to be the easy half (see §3).

A web search surfaced a claim that a 1.21.1 NeoForge Game Stages was "imported by wark53" in May
2026. **[unverified]** — it does not appear on Darkhax's own CurseForge file list, it is not on
Modrinth, and it is not in Darkhax's repo. Treat any such reupload as an unmaintained third-party
fork and do not build on it.

### Successors on 1.21.1 / NeoForge

Four candidates exist. All four are **closed-licence** on Modrinth
(`LicenseRef-All-Rights-Reserved`) except Chapters (MIT), though three publish readable source.

| Mod | Latest | Loaders / MC | Downloads | First published | Source |
| --- | --- | --- | --- | --- | --- |
| **AStages** | 2026-08-16 | forge + neoforge, 1.20.1 + 1.21.1 | **17,453** | 2025-11-01 | `Alessandro-Casale/AStages` (8 stars, pushed 2026-08-19) |
| **History Stages** | 2026-08-19 | fabric/forge/neoforge, …1.21.1 | 3,318 | 2026-02-22 | `Flix100000/History-Stages` (7 stars, pushed 2026-08-21) |
| **ProgressiveStages** | 2026-08-11 | neoforge, 1.21–1.21.11 | 1,833 | 2026-02-04 | `EnVisione/ProgressiveStages` (repo **not publicly readable**) |
| **Chapters** | 2026-08-02 | neoforge, 1.21.1 + 26.1.2 | **277** | 2026-05-08 | `GabinFqt/chapters` (MIT, **0 stars**) |
| **Epoch Stages** | 2026-06-23 | neoforge, 1.21.1 | 57 | 2026-06-23 | none published |

All figures **[api]** from Modrinth `/v2/project/<slug>` and GitHub REST, retrieved 2026-08-21.

**AStages is the clear leader on every axis** — downloads, breadth, and, decisively, it is the only
one that locks the craft *and* hides in all three viewers off one signal. From its source tree
**[src]**:

- Server-side enforcement is by **mixin on the crafting stations themselves**:
  `ACraftingMenu`, `ASmithingMenu`, `AStonecutterMenu`, `AAbstractFurnaceBlockEntity`, `ACampfireBlock`.
  That is a real lock, not a hide — but note the list: **vanilla stations only.** No GT machine, no
  Create, no Mekanism.
- Restriction kinds go well past recipes: `AItemRestriction`, `AItemTagRestriction`,
  `AItemModRestriction`, `ARecipeRestriction`, `ARecipeModRestriction` (lock every recipe of a mod
  namespace), `ADimensionRestriction`, `AOreRestriction`, `AMobRestriction`, `ALootRestriction`,
  `AEnchantRestriction`, and — practically interesting for us — **`AScreenRestriction`**, which gates
  *opening a screen*. Gating the GT machine's GUI is a coarser but working substitute for gating its
  recipes.
- `astages-ftb-quests` (374 downloads, 2026-07-07) bridges stages to FTB Quests **[api]**.

**Chapters** is the honest but weak option: per-player, KubeJS-friendly, registers itself as FTB
Library's `StageProvider` so FTB Quests' Stage Reward grants a chapter with no glue **[src]** — but
its own limitations page says the quiet part: "Other stations (**smithing, mod machines**, etc.)
**are not guarded the same way** today. Someone might craft an item elsewhere even if JEI hides the
vanilla recipe entry." It is JEI-only (no EMI plugin in the tree), two months old, 277 downloads, 0
stars. Not a dependency to hang a pack's progression on.

**History Stages** gates behind "research-based eras" unlocked **server-wide** through a Research
Pedestal **[src]** — world-global by design, so it cannot express per-player state at all. It has
both JEI and EMI compat including `LockedRecipeDecorator` / `LockedEmiRecipeDecorator` (draw a lock
overlay rather than hide). Worth a look only if world-global is the desired granularity.

**ProgressiveStages** advertises "FTB Quests/KubeJS/JEI integration" **[api]** but its GitHub repo
returns nothing readable, so **[unverified]** — none of its behaviour could be checked against
source. Do not select it without a bench test.

## 2. GregTech CEu already has this concept — do not reinvent it

GTCEu 7.0.2 ships a first-class `RecipeCondition` registry. Every GT machine recipe can carry
conditions; `RecipeLogic` tests them before the recipe is allowed to run.

Registered condition types, from `com.gregtechceu.gtceu.data.recipe.GTRecipeConditions` **[jar]**:

`biome`, `dimension`, `pos_y`, `rain`, `thunder`, `rock_breaker`, `adjacent_block`, `steam_vent`,
`cleanroom`, `daytime`, `eu_to_start`, `environmental_hazard`, `research`, and three registered
**only when the corresponding mod is loaded**: `ftb_quest` (guarded by `Mods.isFTBQuestsLoaded()`),
`game_stage` (guarded by `Mods.isGameStagesLoaded()`, which checks mod id `"gamestages"`), and
`heracles_quest` (`Mods.isHeraclesLoaded()`).

The four progression-shaped ones:

### `game_stage` — needs the dead mod

`GameStageCondition.testCondition` **[jar]**, decompiled:

```
machine.getOwner().getMembers()  ->  for each UUID:
    net.darkhax.gamestages.data.GameStageSaveHandler.getPlayerData(uuid).hasStage(stageName)
```

So: it calls **Darkhax's GameStages directly**, and it passes if **any member of the machine's
owner** has the stage. It is registered only when mod id `gamestages` is loaded — which, per §1,
cannot be a maintained 1.21.1 jar. **This condition is unreachable for us.**

### `ftb_quest` — works today, keys off FTB Quests team data

`FTBQuestCondition` **[jar]** resolves `machine.getOwner()`; for an `FTBOwner` it takes
`getTeam()` and asks `FTBQuestsAPI.api().getQuestFile(true).getOrCreateTeamData(...)` whether the
quest object (looked up by numeric quest id, cached in a `Long2ObjectMap`) is complete. Reverse mode
exists (`isReverse` → "Requires %s **not** completed").

This is the one progression gate that is **installed and functional right now**. Granularity is
**per-team** (FTB Quests completion is team state), keyed off a **quest id**, and it applies to GT
machine recipes.

### `heracles_quest` — Heracles is not installed. Not an option.

### `research` — GT's native, mechanism-shaped gate

`ResearchCondition` carries a `ResearchData` **[jar]**. This is GT's Assembly Line research: a
recipe requires a Data Stick imprinted with a research id, produced by a Scanner or a Research
Station. Builder surface in KubeJS: `.scannerResearch(...)`, `.stationResearch(...)`,
`.researchWithoutRecipe(...)`, `.researchScan(bool)` **[jar]**.

Granularity is **world-global-by-way-of-an-item**: the gate is not "player knows X", it is "this
Data Stick in this machine holds X". Anyone who can carry the stick can run the recipe. That is
arguably the most *GregTech-native* answer to "gate a recipe behind progression", and it is the one
concept in this space we would definitely be reinventing if we wrote our own.

### The KubeJS surface for all of them

`GTRecipeSchema$GTKubeRecipe` **[jar]** exposes, on any GT recipe built in KubeJS:

```
.gameStage(String[, boolean reverse])
.ftbQuest(String[, boolean reverse])
.heraclesQuest(String[, boolean reverse])
.dimension(ResourceKey<Level>[, boolean])
.biome(ResourceKey<Biome>[, boolean])
.cleanroom(CleanroomType)
.environmentalHazard(MedicalCondition[, boolean])
.scannerResearch(...) / .stationResearch(...) / .researchWithoutRecipe(...)
.addCondition(RecipeCondition)   // arbitrary, including custom ones
```

`.addCondition` being public means a first-party condition registered from
`planetaryfactory_core` would slot into the same pipeline.

### Scope limit — read this twice

`RecipeCondition.testCondition(GTRecipe, RecipeLogic)` takes a **`RecipeLogic`** — a GT machine's
recipe-running trait. **GT recipe conditions gate GT machine recipes and nothing else.** They do not
touch the vanilla crafting grid, and they do not touch Create, Mekanism, AE2 or Integrated Crafting
recipes. Given the pack's "processing spans four tech mods" stance, a GT-conditions-only gate would
gate one quarter of the pack.

---

## 3. Can KubeJS alone do it?

Two separate questions. The answers are opposite.

### 3a. Stage *storage*: yes, natively, per-player, no extra mod

KubeJS 2101 ships `dev.latvian.mods.kubejs.stages` **[jar]**:

- `Stages` interface: `has/add/remove/set/addNoUpdate/removeNoUpdate/getAll/sync`, reachable as
  `player.stages` (`PlayerKJS.kjs$getStages()`).
- Default implementation is **`TagWrapperStages`** — stages are literally **vanilla entity tags on
  the player** (`Player.addTag/removeTag/getTags`). They persist in player NBT, they survive without
  any mod's save data, and `/tag` sees them.
- Changes fire `PlayerEvents.STAGE_ADDED` / `STAGE_REMOVED` and push
  `AddStagePayload` / `RemoveStagePayload` / `SyncStagesPayload` **to the client**, so client-side
  scripts can react.
- `StageCreationEvent` lets another mod swap the backing store.
- FTB Library defines the neutral SPI `StageProvider {has/add/remove/sync/getName}` with a
  `StageHelper` singleton; its own fallback is `EntityTagStageProvider` — again, entity tags **[jar]**.
- **FTB XMod Compat ships `KubeJSStageProvider`** which bridges FTB Library's SPI straight onto
  `PlayerKJS.kjs$getStages()`, selected by config `FTBXModConfig.STAGE_SELECTOR` **[jar]**.

Consequence, and this is the useful finding: **with exactly the jars already in `mods/`,
`/ftbquests`' Stage Reward already writes a per-player stage that KubeJS can read as
`player.stages.has('x')`.** No GameStages needed. FTB Quests' `StageTask` and `StageReward`
**[jar]** additionally support a **team** stage (`teamStage` boolean; `TeamStagesHelper
.addTeamStage/hasTeamStage`, stage name suffixed `_team`), so the trigger side offers **both
per-player and per-team** granularity out of the box.

The store is solved. It is the enforcement that is missing.

### 3b. Recipe *enforcement*: no — and KubeJS deliberately removed its own attempt

The installed jar still has a `stage` field on `ShapedKubeJSRecipe` / `ShapelessKubeJSRecipe`
(`kjs$getStage()`, `KubeJSCraftingRecipe.STAGE_KEY`, `RecipeFlags.STAGE`) **[jar]** — so
`.stage("x")` will not error in build 181. **But nothing in the jar reads it during crafting**:
neither special recipe overrides `matches`, and the shared `kjs$assemble` only consults
`kjs$getModifyResult`, never the stage.

The reason is in upstream history **[src]**. On branch `2101`, commit `fba54aa9`, 2026-03-15,
titled *"Remove stage from kjs recipes"*, the diff to `KubeRecipe.java` shows what `.stage()` used
to do:

```java
-	if (type.event.stageSerializer != null && json.has(KubeJSCraftingRecipe.STAGE_KEY)
-			&& !type.idString.equals("recipestages:stage")) {
-		var staged = new JsonObject();
-		staged.addProperty("stage", json.get(KubeJSCraftingRecipe.STAGE_KEY).getAsString());
-		staged.add("recipe", json);
-		json = staged;
-	}
```

It **wrapped the recipe in Recipe Stages' `recipestages:stage` serializer**. KubeJS never enforced
staging itself; it delegated to the mod from §1. The replacement body:

```java
	/**
	 * @deprecated It doesn't look like recipe staging is likely to return any time soon; ...
	 */
	@Deprecated(forRemoval = true)
	public KubeRecipe stage(String s) {
		throw new KubeRuntimeException("recipe.stage() is no longer supported by default due to vanilla changes!")
```

Current `2101` HEAD's `KubeJSCraftingRecipe.java` has no `STAGE_KEY` at all **[src]**. So: **in our
build `.stage()` is a silent no-op; on any newer 2101 build it throws.** Either way it is not a
mechanism.

### 3c. Runtime add/remove vs datapack-load only

`ServerEvents.recipes` is fired from `RecipeManagerMixin.customRecipesHead`, injected into
`RecipeManager.apply` **[jar]** — i.e. it runs **only during a datapack reload**, never in response
to a gameplay event. `kjs$replaceRecipes` exists on the mixin, but the whole `RecipesKubeEvent`
lifecycle hangs off that one injection point. There is no "a player earned a stage → add this
recipe" hook.

So the only way to change the recipe set mid-game is `/reload`, which costs:

- a full `ReloadableServerResources` rebuild — every datapack recipe, tag, loot table, advancement
  and function re-parsed;
- a re-run of **all** KubeJS server scripts, plus GTCEu's entire recipe generation (GT synthesises
  tens of thousands of recipes);
- a re-sync of the recipe set and recipe book to every client.

On a modpack this size that is a multi-second, visible stall — and it is **world-global**: everyone
gets the new recipe set, so this can never express per-player state. Reloading is also a well-known
source of trouble for in-flight GregTech machine recipes **[unverified]** — not confirmed against
source here, and worth a bench test before anyone leans on `/reload` as a mechanism.

Corollary: **KubeJS-only recipe locking can only be world-global and only at reload boundaries.** If
world-global is acceptable, the honest KubeJS design is not "lock a recipe" at all — it is
`ServerEvents.recipes` reading a persisted world flag and emitting a different recipe set, plus an
explicit `/reload` when the flag flips.

---

## 4. Granularity summary

| Mechanism | Granularity | Keys off | Available in this pack |
| --- | --- | --- | --- |
| GT `ftb_quest` condition | **per-team** | FTB Quests quest id, via team data | **yes, today** |
| GT `research` condition | world-global (via an item) | Data Stick contents | **yes, today** |
| GT `game_stage` condition | per-machine-owner ("any member has stage") | GameStages store | no — needs a dead mod |
| GT `heracles_quest` condition | per-player | Heracles quest id | no — Heracles absent |
| KubeJS `player.stages` | **per-player** | entity tag on the player | yes — but *stores* only, enforces nothing |
| FTB Quests Stage Reward | per-player **or** per-team | quest claim | yes — trigger only |
| KubeJS `ServerEvents.recipes` | **world-global**, reload-only | whatever the script reads | yes |
| AStages | per-player | its own stage store (+ FTB Quests bridge) | not installed; **vanilla stations only** |
| History Stages | world-global | Research Pedestal | not installed |
| Chapters | per-player | its own stage store | not installed; vanilla grid only; JEI only |
| Recipe Stages / Item Stages | per-player | GameStages store | dead on 1.21.1 |

Note the mismatch that runs down the middle of that table: **the mechanisms that enforce are
world-global or per-team; the mechanism that is per-player enforces nothing.** Single-player, which
this ticket assumes, collapses all three columns — every granularity is equivalent for one player in
one team. That makes the choice free *now* and expensive later only if the pack ever goes multiplayer.

---

## 5. Recipe viewers, and the hide-vs-lock axis

### 5a. What is actually installed

Checked, not assumed **[jar]**:

| Viewer | Installed? | Version |
| --- | --- | --- |
| **JEI** | **yes** | `jei-1.21.1-neoforge-19.44.0.402.jar` |
| **EMI** | **yes** | `emi-1.1.24+1.21.1+neoforge.jar` |
| REI | no | — |

**Both viewers are installed at once.** That is the single most consequential fact in this section:
any mechanism that integrates with one viewer leaves the other one cheerfully listing the recipe you
just hid. A JEI-only solution is half a solution here.

(Also present and easily mistaken for relevant: `Not Enough Recipe Book`, `recipeessentials`,
`JustEnoughMekanismMultiblocks`, `almostunified`. None of these gate anything on progression.)

### 5b. The axis that matters: hiding ≠ locking

**Hiding a recipe in the viewer is not locking the craft.** A viewer hide changes what the player is
*told*; the crafting grid still assembles the item if they know the pattern, and any automation
already pointed at the recipe keeps running. For a pack whose players read wikis and copy setups,
viewer-only gating is **cosmetic**.

Ranked on that axis, everything in this document:

| Mechanism | Locks the craft? | Hides in viewer? | Verdict |
| --- | --- | --- | --- |
| GT `ftb_quest` / `research` condition | **yes** (GT machines) | no — shows "Requires X" | **enforcing**, self-documenting |
| AStages | **yes** (vanilla stations) | JEI live, EMI at reload, REI | **enforcing**, viewer-synced |
| Chapters | partial (vanilla grid only) | JEI only | weak |
| History Stages | yes, world-global | JEI + EMI, decorator overlay | enforcing but not per-player |
| KubeJS `ServerEvents.recipes` | **yes** (removes the recipe outright) | n/a — recipe ceases to exist | enforcing, but world-global + reload-only |
| KubeJS `RecipeViewerEvents` / JEI+EMI remove events | **no** | yes, static per session | **cosmetic** |
| JEI `IRecipeManager.hideRecipes` (raw API) | **no** | yes, live | **cosmetic** |
| EMI `EmiRegistry.removeRecipes` (raw API) | **no** | yes, at plugin registration only | **cosmetic** |

### 5c. The raw viewer APIs, and how they differ

**JEI** exposes live, runtime, client-side hiding — `mezz.jei.api.recipe.IRecipeManager` **[jar]**:

```java
<T> void hideRecipes(RecipeType<T>, Collection<T>);
<T> void unhideRecipes(RecipeType<T>, Collection<T>);
void hideRecipeCategory(RecipeType<?>);
void unhideRecipeCategory(RecipeType<?>);
```

plus `IIngredientManager.addIngredientsAtRuntime / removeIngredientsAtRuntime` for the ingredient
list, and `IRecipeManagerPlugin` for computing a recipe list dynamically per lookup. `IRecipeManager`
is reachable from `IJeiRuntime`, i.e. **at any moment during play** — this is what makes per-player,
mid-session JEI gating possible at all.

**EMI has no equivalent.** `dev.emi.emi.api.EmiRegistry.removeRecipes(Predicate<EmiRecipe>)` **[jar]**
is handed to `EmiPlugin.register(...)` — it exists **only during plugin registration**, i.e. at load
and on an EMI reload. There is no runtime `hideRecipes` on EMI's public API. (`dev.emi.emi.runtime
.EmiHidden` exists but is internal, and backs the player's own ctrl-right-click hide list, not an API.)

This asymmetry is not academic. AStages, the best-maintained option, hits it head-on **[src]**:

- `JeiRecipeStagesPlugin` holds the `IJeiRuntime` and, from `onStagesChanged(operation, syncedStages)`,
  calls `RUNTIME.getRecipeManager().hideRecipes(...)` / `.unhideRecipes(...)` — **live, the moment a
  stage changes.**
- `EmiRecipeStagesPlugin.register(registry)` calls `registry.removeRecipes(this::hideRecipes)` once,
  and its `onStageChanged(...)` method body is **literally empty**.

So even the leading mod's EMI listing goes stale until EMI reloads, while its JEI listing updates
instantly. With both viewers installed here, that discrepancy is visible to the player.

Note also that AStages' JEI updater iterates **vanilla `RecipeType`s** (`updateRecipesForType(
RecipeType<T> vanillaType, mezz.jei.api.recipe.RecipeType<T> jeiType)`), so its hiding does not reach
GT machine categories either.

### 5d. Do GTCEu and KubeJS already ship viewer-visibility control?

**Yes to both — and it means we would not need to add a mod just for the viewer half.**

**KubeJS 2101** ships plugins for **all three** viewers **[jar]**:
`integration/jei/` (`JEIRemoveRecipesKubeEvent`), `integration/emi/` (`KubeJSEMIPlugin`,
`EMIRemoveEntriesKubeEvent`, `EMIAddEntriesKubeEvent`, `EMIAddInformationKubeEvent`), and
`integration/rei/`. `JEIRemoveRecipesKubeEvent` wraps `mezz.jei.api.recipe.IRecipeManager` directly.
Above them sits a viewer-agnostic layer, `RecipeViewerEvents` **[jar]**: `ADD_ENTRIES`,
`REMOVE_ENTRIES`, `REMOVE_ENTRIES_COMPLETELY`, `GROUP_ENTRIES`, `ADD_INFORMATION`,
`REGISTER_SUBTYPES`, `REMOVE_CATEGORIES`, `REMOVE_RECIPES` — one script, every installed viewer.

There is even a **server-authoritative** path: `recipe/viewer/server/RecipeViewerData.collect()`
carries `removedCategories`, `removedGlobalRecipes`, per-category data, item and fluid data over a
`StreamCodec` to the client, with `RemoteRecipeViewerDataUpdatedEvent` and `Server*KubeEvent`
variants **[jar]**. But `collect()` takes **no player argument** and the field is named
`removedGlobalRecipes` — this is **world-global viewer state**, computed once and broadcast, not a
per-player filter. It cannot express "hide this from Alice but not Bob".

**GTCEu** ships its own JEI/EMI-shared widget layer (`integration/xei/widgets/GTRecipeWidget`) which
renders every `RecipeCondition.getTooltips()` **[jar]**. It exposes *annotation*, not visibility
control: GT's answer is to show the recipe with a "Requires X" label. Its `en_us.json` **[jar]**
carries exactly the strings the player reads:

```
"recipe.condition.gamestage.unlocked_stage": "Unlocked at stage: %s",
"recipe.condition.gamestage.locked_stage":   "Locked at stage: %s",
"recipe.condition.quest.completed.tooltip":  "Requires %s completed",
"gtceu.recipe.research":                     "Requires Research",
```

So a GT-gated recipe is **visible and self-documenting, and fails by the machine idling** rather
than by an error. That is a deliberate design property — it tells the player what to go do — but it
is the opposite of hiding, and worth choosing on purpose rather than inheriting by accident.

### 5e. Can one signal drive both halves?

**Yes, and it should.** The evidence:

- The **stage store is already shared and neutral**: FTB Library's `StageProvider` SPI, backed by
  KubeJS `player.stages` (entity tags) through FTB XMod Compat's `KubeJSStageProvider`, written by
  FTB Quests' Stage Reward **[jar]** (§3a).
- Stage changes already **push to the client** — KubeJS sends `AddStagePayload` /
  `RemoveStagePayload` / `SyncStagesPayload`, and fires `PlayerEvents.STAGE_ADDED` / `STAGE_REMOVED`
  **[jar]**. A client-side listener on those is exactly the signal a JEI `hideRecipes` call needs.
- AStages demonstrates the pattern end-to-end: one stage set, `SyncRecipeS2C` to the client, server
  mixins enforce, viewer plugins hide **[src]**.

So there is no need for two progression models. The design that follows from the evidence is: **one
stage set → server-side enforcement (the lock) → the same stage-change event drives viewer hiding
(the cosmetic layer)**. What no existing mod does is drive that pipeline for *GregTech machine*
recipes, which is where this pack's interesting recipes live.

## 6. What happens to an already-crafted item when its recipe locks?

**Nothing — in every mechanism examined.** This is worth stating plainly because the ticket asks it
and the intuitive answer is wrong.

- **GT conditions** attach to the *recipe*, tested at *recipe selection time* in `RecipeLogic`. An
  item already in a chest is untouched, fully usable, and still stacks and still crafts as an
  ingredient in other recipes. Re-locking a stage does not retroactively invalidate output.
- **KubeJS `ServerEvents.recipes`** removing a recipe removes only the recipe; existing items are
  ordinary items.
- The only mechanism that touches *existing* items is **Item Stages' successor behaviour** — and it
  is a separate feature from recipe locking. Chapters, for example, will "prevent pickup of locked
  items" and "auto-drop locked items from the player inventory (every second while online, and when
  a stage is removed or definitions reload)" **[src]**. That is an item lock, not a recipe lock, and
  it is aggressive enough to be a footgun: a stage *removal* silently vomits inventory onto the floor.

So "lock a recipe" and "lock an item" are two different features with two different blast radii, and
the pack should decide which it actually wants. Recipe locking is safe with respect to existing
inventory. Item locking is not.

---

## 7. Bottom line for the pack

1. **Nothing installed today can lock a *crafting-grid* recipe behind progression.** The lineage that
   used to do it is Forge/1.20.1 and stopped there.
2. **The one working, installed, maintained gate is GTCEu's `ftb_quest` recipe condition**, which
   gates **GT machine recipes** by **FTB Quests quest completion**, per team, with a "Requires X
   completed" tooltip in both JEI and EMI. If the design can live with "GT machines only, visible not
   hidden", this is a zero-new-dependency answer and it is GregTech-idiomatic.
3. **GT's `research` (Data Stick) condition is the concept we would be reinventing** if we built a
   bespoke recipe gate. Check any proposal against it before writing code.
4. **The stage *store* is already free** — `player.stages` (entity tags), bridged to FTB Quests'
   Stage Reward via FTB Library's `StageProvider` and FTB XMod Compat's `KubeJSStageProvider`. Any
   first-party enforcement we write should read that store rather than inventing a fifth one.
5. **KubeJS cannot enforce a recipe lock**, cannot react to a gameplay event by changing recipes, and
   its `.stage()` was removed upstream with the note *"It doesn't look like recipe staging is likely
   to return any time soon."*
6. **Hiding is not locking, and no viewer mechanism should be mistaken for a gate.** JEI's
   `hideRecipes`, EMI's `removeRecipes`, and KubeJS's `RecipeViewerEvents` all change what the player
   is *told*, never what they can build. Treat the viewer layer as the annotation on a lock, never as
   the lock. And because **both JEI and EMI are installed**, any candidate that integrates with only
   one is disqualified on that ground alone — which rules Chapters out.
7. If a **crafting-grid** lock is genuinely required, the realistic options are: adopt **AStages**
   (17k downloads, actively pushed, real station mixins, JEI + EMI + REI plugins, FTB Quests bridge —
   but vanilla stations only, closed licence, and its EMI listing does not update live), or implement
   it in `planetaryfactory_core` against the FTB Library `StageProvider` SPI plus a `CraftingMenu` /
   result-slot hook and **both** a JEI and an EMI plugin. ADR-0015's ownership table says mechanism
   belongs in the mod, so option two is the one that fits the pack's own rules.
8. **One signal can drive both halves.** The stage store, the stage-change client packets, and the
   viewer APIs are all already present; there is no need for two progression models. What no existing
   mod supplies is enforcement for **GT machine** recipes — which is exactly the half GTCEu's own
   `RecipeCondition` system already covers. The two fit together.

## Open questions this document does not answer

- Whether `/reload` breaks in-flight GTCEu machine recipes. **[unverified]** — needs a bench test.
- Whether a custom `RecipeCondition` registered from `planetaryfactory_core` survives GTCEu's recipe
  serialization round-trip across a dedicated-server boundary (it has `toNetwork`/`fromNetwork`, so
  it should, but this was not tested).
- Whether EMI can be driven to hide recipes per-player **live** on 1.21.1. Verified: its public API
  offers hiding only at plugin registration, and AStages leaves its EMI stage-change handler empty
  **[src]**. Whether an EMI reload can be triggered programmatically on a stage change — History
  Stages ships an `EmiReloadBridge` that suggests yes — was **not** verified.
- Whether AStages' `AScreenRestriction` can gate a GTCEu machine GUI in practice. If it can, it is a
  cheap coarse substitute for GT recipe conditions. **[unverified]** — needs a bench test.
