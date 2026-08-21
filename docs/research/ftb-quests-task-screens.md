# What FTB Quests Task Screens can actually do

**Answer: the lab premise holds for items, fluids and FE — those three, and only those three, are
pipe-feedable. Everything else in the ticket is a qualified no or a workaround.**

Read against the jars actually in `mods/`, plus the matching upstream tags:

| Thing | Version in `mods/` | Source read |
| --- | --- | --- |
| FTB Quests | `ftb-quests-neoforge-2101.1.31.jar` | `FTBTeam/FTB-Quests` tag `v2101.1.31` (jar `Implementation-Version` matches exactly) |
| FTB XMod Compat | `ftb-xmod-compat-neoforge-21.1.11.jar` | jar bytecode (`javap`) — this is where the KubeJS bridge lives |
| FTB Library | `ftb-library-neoforge-2101.1.35.jar` | jar bytecode |
| FTB Teams | `ftb-teams-neoforge-2101.1.10.jar` | jar bytecode |
| Certain Questing Additions | `certain_questing_additions-neoforge-1.2.0.4` | jar contents |

Paths below are relative to the FTB Quests source tree unless noted.

---

## 1. Which task types accept automated input

Ten task types are registered: `item`, `fluid`, `forge_energy`, `xp`, `advancement`, `biome`,
`checkmark`, `custom`, `dimension`, `kill`, `location`, `observation`, `stage`, `stat`, `structure`
(`quest/task/TaskTypes.java`).

A task can only be **assigned to a Task Screen at all** if `Task.consumesResources()` returns true —
`TaskScreenBlockEntity.isSuitableTask()` (`block/entity/TaskScreenBlockEntity.java:247`) filters the
config dropdown on exactly that. Only four types override it to true:

| Task | `consumesResources()` | Source |
| --- | --- | --- |
| `item` | `consume_items` tristate, defaulting to the chapter's | `quest/task/ItemTask.java:206` |
| `fluid` | always | `quest/task/FluidTask.java:89` |
| `forge_energy` | always | `quest/task/EnergyTask.java:79` |
| `xp` | always | `quest/task/XPTask.java:93` |
| everything else | `false` (base `Task.consumesResources()` → `canInsertItem()` → `false`) | `quest/task/Task.java:207-213` |

So: **an item task with `consume_items` off cannot go on a screen at all.** That is a submit-by-
clicking task, not a delivery task.

### The capabilities

`neoforge/.../FTBQuestsNeoForge.java:95-107` registers `Capabilities.ItemHandler.BLOCK`,
`Capabilities.FluidHandler.BLOCK` and `Capabilities.EnergyStorage.BLOCK` on **both** the core screen
block entity and the aux (multiblock filler) block entity, with **no side restriction** — every face
of every block of the multiblock accepts insertion. The aux delegates straight to the core
(`neoforge/.../NeoForgeTaskScreenAuxBlockEntity.java`).

That is a plain NeoForge capability, so anything that pushes into an `IItemHandler` /
`IFluidHandler` / `IEnergyStorage` works: vanilla hoppers and droppers, Create's chutes, funnels and
belts-into-funnel, GTCEu machine auto-output and pipes, AE2 export buses, and so on. **No player
interaction is required and no player needs to be online** — insertion runs entirely off
`TeamData`, which is server-side quest-file state.

Details that bite (`neoforge/.../NeoForgeTaskScreenBlockEntity.java`):

- **Items.** The handler exposes **two slots**: slot 0 is a read-only view of accumulated progress,
  slot 1 is the *only* insertion slot. `insertItem` returns the stack unchanged for slot 0. The
  two-slot design is deliberate — the comment says it stops inserters concluding the inventory is
  full. `isItemValid` is likewise slot-1-only. Most automation probes all slots and will find it,
  but anything hard-coded to slot 0 will silently fail.
- **XP tasks cannot be automated.** `xp` passes the `consumesResources()` gate so it *can* be put on
  a screen, but there is no XP capability — it is submitted by a player clicking. Treat "screen-
  feedable" as items, fluids and FE only.
- **Extraction is possible unless you forbid it.** Both the item and fluid handlers implement
  `extractItem` / `drain` against *task progress*, so a hopper under the screen will happily pull
  your delivered items back out and reset progress. The screen's `input_only` boolean disables item
  extraction (`isInputOnly()` guard in `extractItem`) — **but the fluid `drain` has no `isInputOnly`
  guard at all** (read at `NeoForgeTaskScreenBlockEntity` `TaskFluidHandler.drain`). Energy
  `canExtract()` is hard `false`. Any fluid screen is drainable by anything adjacent.
- **Progress is uncapped-in, capped-at-target.** All three handlers clamp to
  `maxProgress - progress`, so overflow is rejected rather than voided.

### Energy and GT

The energy task is `forge_energy` — a NeoForge `IEnergyStorage`, i.e. **FE/RF**. GTCEu 1.21.1's
native transfer is EU on its own capability. Whether GT machines/cables will push FE into the screen
without an explicit converter is **not verified here**; assume an FE-emitting intermediary (a GT
energy converter, or a Create/other FE source) is needed until tested in game.

---

## 2. Per-team, not per-player

`TeamData` is the only unit of quest progress in this version — keyed by team UUID, with a
`perPlayerData` sub-map used only for reward claims and a couple of per-player flags
(`quest/TeamData.java:75-80`). There is **no per-player progression mode**; `BaseQuestFile`'s
settings expose `progression_mode`, `reward_team`, `consume_items` and friends, but nothing that
splits task progress per player (`quest/BaseQuestFile.java:1258-1284`).

In FTB Teams every player always has a personal `PlayerTeam`; a `PartyTeam` is opt-in. So in
practice progress is per-player *until* players form a party, at which point it becomes shared.

The screen stores a **raw team UUID**, captured from the placing player at
`TaskScreenBlock.setPlacedBy()` (`block/TaskScreenBlock.java:133`), and all insertion resolves
through `getCachedTeamData()` on that UUID. Consequences:

- Anyone can pipe into anyone's screen — the capability has no permission check. Only *editing* the
  screen (opening its config GUI) is gated, by `hasPermissionToEdit()`: you must be the owner or in
  the owner's team.
- **Party changes leave screens stale.** When a player joins a party, `ServerQuestFile
  .playerChangedTeam()` (`quest/ServerQuestFile.java:299-320`) merges the old `TeamData` into the
  new one but *does not delete it*, and nothing anywhere rewrites a placed screen's stored
  `teamId`. Screens placed before the party formed therefore keep crediting the now-orphaned
  personal team. Read from code, **not tested in game** — but there is no code path that would
  prevent it.

As a progression gate the book is per-team, which is the right granularity for a co-op pack and the
wrong one if you wanted each player to build their own lab.

---

## 3. KubeJS observability, and recipe unlocks

### Observability: yes, via `ftb-xmod-compat`, not via FTB Quests itself

FTB Quests 2101 ships **no** KubeJS integration (no `integration/kubejs` package, no
`kubejs.classfilter.txt` in the jar). The bridge is in `ftb-xmod-compat`, package
`dev.ftb.mods.ftbxmodcompat.neoforge.ftbquests.kubejs`. Event group `FTBQuestsEvents`, with these
handlers (constant strings read from `FTBQuestsKubeJSEvents` bytecode):

| Event | Scope | Targeted by |
| --- | --- | --- |
| `FTBQuestsEvents.completed(id, cb)` | server | quest-object string id |
| `FTBQuestsEvents.started(id, cb)` | server | quest-object string id |
| `FTBQuestsEvents.customTask(id, cb)` | server | custom-task string id |
| `FTBQuestsEvents.customReward(id, cb)` | server | reward string id |
| `FTBQuestsEvents.customFilterItem(cb)` | **client** | — |

`completed` / `started` fire for **any** quest object — task, quest, chapter — since
`Task.onCompleted()` invokes `ObjectCompletedEvent.TASK` and cascades to the quest
(`quest/task/Task.java:104-115`). The event object gives `getObject()`, `getData()` (a team-data
wrapper with `addProgress`, `setLocked`, `complete`, `reset`, `getTaskProgress`, `isCompleted`,
`getOnlineMembers`), `getPlayer()`, `getNotifiedPlayers()`, `getOnlineMembers()`. A global
`FTBQuests` binding (`FTBQuestsKubeJSWrapper`) exposes `getFile(level)`, `getData(player)` and
`getObject(level, id)` for polling.

**Gap: there is no progress event.** `ObjectProgressEvent` exists in FTB Quests
(`events/ObjectProgressEvent.java`) but is **not** wired into the KubeJS bridge. You can observe
*completion*, not incremental delivery. An Emission-style live readout has to poll
`FTBQuests.getData(player).getTaskProgress(id)` on a tick, not react to an event.

### Recipe unlocks: no

Nothing in FTB Quests, KubeJS 2101 or GTCEu provides per-player recipe locking — recipes are
datapack-global. The closest real mechanism is **stages**:

- `StageReward` (`quest/reward/StageReward.java`) grants/removes a stage on quest completion via
  FTB Library's `StageHelper`.
- No GameStages mod is installed, but `ftb-xmod-compat` registers a `KubeJSStageProvider`
  (`generic/gamestages/neoforge/StagesSetupImpl` → `StageHelper.setProviderImpl`) that maps
  `StageHelper` straight onto KubeJS `player.stages`. So **quest completion can set a KubeJS
  stage**, and the reverse `StageTask` can make a stage satisfy a task.
- FTB Library's fallback provider when nothing else registers is `EntityTagStageProvider`.

But a KubeJS stage still cannot hide or unlock a recipe. Gating has to be expressed as: gate the
*machine* or the *ingredient* behind the quest (screen-delivered item unlocks the next machine's
crafting component), or cancel crafting in a KubeJS event on stage. Designing the spine around
"quest completion unlocks recipe X" is not supported by anything installed.

---

## 4. Rate: no. Cumulative total only.

Every task's progress is a monotonic `long` counter compared against `getMaxProgress()`
(`Task.getRelativeProgressFromChildren`, `quest/task/Task.java:83-98`). There is no time window,
no decay, no per-tick sampling of any of the three automated task types.

Two near-misses worth knowing:

- **`EnergyTask.max_input`** (`quest/task/EnergyTask.java:17,86`) caps how much energy a *single*
  `receiveEnergy` call may accept. Pushers normally call once per tick, so it behaves as a de-facto
  FE/t ceiling — it throttles the *maximum* rate, but it never *requires* one. Delivering 1 FE/t to
  a 1,000,000 FE task still completes it, just slowly. There is no equivalent for items or fluids.
- **`CustomTask` + `checkTimer`** (`quest/task/CustomTask.java`) is the only real escape hatch: a
  KubeJS-supplied `Check` callback runs every `checkTimer` ticks and can set progress arbitrarily,
  so a rate requirement could be *implemented* in script (sample a machine, reset progress if the
  sample is low). Caveats: `CustomTask.consumesResources()` is false, so **a custom task cannot be
  put on a Task Screen**; and it runs from `autoSubmitOnPlayerTick` (`FTBQuestsEventHandler.java:
  158-171`), i.e. **only while a player is online and ticking** — it will not observe a factory
  running unattended.

So a rate requirement means: script it as a custom task, accept that it only ticks with a player
present, and read the rate from the machine rather than from a screen.

---

## 5. Repeatable / resettable quests

They exist, at the **quest** level (`quest/Quest.java`):

- `can_repeat` — a tristate defaulting to the chapter's `default_repeatable` (`Quest.java:222`,
  `Chapter.java:48`).
- `repeat_cooldown` — seconds, default 0 (`Quest.java:82,1142`).

The reset fires from `TeamData.claimReward()` → `Quest.resetProgressIfRepeatable()`
(`Quest.java:978`, `TeamData.java:312-318`): once **all** of the quest's rewards have been claimed
by that player, task progress is force-reset, `completionCount` is incremented, and the cooldown
timestamp is stamped. A quest with **no rewards** therefore never resets — repeatability requires at
least one reward.

**What it costs the book's structure** — this is the important part:

```java
// quest/Quest.java:253
public boolean isOptionalForProgression(TeamData teamData) {
    return isOptional() || canBeRepeated() || teamData.isExcludedByOtherQuestline(this);
}
```

A repeatable quest is **optional for progression**. It cannot act as a gate: dependent quests do not
wait on it, and it does not count toward chapter completion. So a repeatable delivery quest is a
sink or a bounty board, never a spine node. If the spine needs both "gate the next tier" and "keep
delivering", those have to be two quests.

There is no repeatable *task*, and no per-task reset outside KubeJS
(`FTBQuestsKubeJSTeamData.reset(object)` / `changeProgress`, which the bridge does expose).

---

## 6. Friction

- **Chunk loading.** The task screen block entity has **no ticker** — insertion is entirely
  push-driven by the machine on the other end. So the screen itself needs nothing; the *pusher*
  needs its chunk loaded, which it does anyway. FTB Chunks is installed, so chunk-loading a lab is
  cheap. Nothing about the screen requires a player online. Custom tasks and the detector are the
  exceptions — both need a real online player.
- **Size and placement.** Four screen blocks are registered: `screen_1`, `screen_3`, `screen_5`,
  `screen_7` (`registry/ModBlocks.java:22-31`), i.e. 1×1, 3×3, 5×5, 7×7 wall panels. The core is the
  **bottom middle** block; placement fails unless the whole `getMultiblockBounds()` box is
  replaceable (`TaskScreenBlock.validatePlaceable`). Breaking **any** aux block destroys the whole
  multiblock and drops the core (`onRemove`). There is an `indestructible` flag per screen. The
  screen is wall-facing (horizontal `FACING` from the placer), so a 7×7 needs a real 7-high wall.
- **NBT sensitivity — items.** `ItemTask.match_components` is a three-way enum
  (`integration/item_filtering/ItemMatchingSystem.java:69`): `NONE` ignores components entirely
  (item id only), `FUZZY` requires the task stack's components to be present-and-equal on the
  submitted stack (extra components allowed), `STRICT` is `ItemStack.isSameItemSameComponents`. The
  default is `NONE`, which is the forgiving and usually correct choice for a factory pack — a
  damaged or renamed item still counts.
- **NBT sensitivity — fluids.** No such setting. `TaskFluidHandler.isFluidValid` builds
  `FluidStack.isSameFluidSameComponents` against the task's fluid — **always strict**. Any mod that
  stamps a data component on its fluid will fail to satisfy a fluid task.
- **Tags.** `ItemTask` targets a single `ItemStack`, not a tag — *but* `ftb-filter-system` is
  installed and `ftb-xmod-compat` registers an `ItemFilterAdapter` for it
  (`ftbquests/filtering/FFSSetup$FFSAdapter`, which has a `makeTagFilterStack(TagKey<Item>)`). So a
  filter item placed as the task's target gives you tag/mod/OR matching. Note the item handler
  refuses *extraction* when the task stack is a filter (`ItemMatchingSystem.INSTANCE.isItemFilter`
  guard in `extractItem`) — which is a free way to make an item screen input-only.
- **Fluid amounts overflow at ~2.1 B mB.** `FluidTask.getMaxProgress()` is a `long`, but
  `TaskFluidHandler.getTankCapacity` casts it to `int`. A task above `Integer.MAX_VALUE` mB will
  report a nonsense capacity to pipes. Same cast in `getFluidInTank` and in
  `TaskEnergyHandler.getMaxEnergyStored`. Keep single-task targets under 2.1 B.
- **A serialization bug in the screen's saved data.** `TaskScreenSaveData.CODEC`
  (`TaskScreenBlockEntity.java` bottom) registers the field name `"skin"` **twice** — once for
  `skin` and once for `inputModeIcon`. The input-mode icon will not round-trip correctly through
  NBT. Cosmetic only, but do not rely on that icon.
- **Certain Questing Additions adds nothing mechanical.** All 97 classes are client-side: APNG
  animated icons, entity icons, chapter shaders, quest-panel animation, EMI screen return, plus GUI
  mixins (`ru.hollowhorizon.additions.questing.*`). **No task types, no capabilities, no server
  logic.** It does not extend what a Task Screen can do.

---

## What this means for the spine

Safe to build on:

- Machines delivering **items, fluids and FE** into a Task Screen, unattended, chunk-loaded, no
  player interaction. This is the lab premise and it holds.
- Tag/filter-based item targets via FTB Filter System.
- Quest **completion** observed in KubeJS, driving stages, commands and rewards.

Do not build on:

- Per-player progression (it is per-team), or screens surviving a party change.
- Rate requirements read off a screen.
- Recipe unlocks driven by quest completion.
- Repeatable quests as progression gates.
- Live delivery-progress events in KubeJS (poll instead).

**Unverified in game** (read from code only): GT EU → screen FE without a converter; the stale-team
screen behaviour after a party forms; whether GTCEu auto-output finds the two-slot item handler.
