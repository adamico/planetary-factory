# Where the Rocket Silo's 50-cycle count can live

**Answer in one line: the count lives as an `@Persisted int` on a first-party
`MetaMachine` subclass in `planetaryfactory_core`, registered as a GTCEu multiblock from a KubeJS
startup script that hands `.machine(...)` the Java constructor. GTCEu already has a
`consecutiveRecipes` counter and it is the wrong one — it is zeroed the moment the machine idles
or the structure de-forms. Nothing needs a mixin: the mod already hard-depends on `gtceu`. GCyR's
`startRocket` reaches the silo from `this.level()` plus `this.blockPosition()` through
`MultiblockWorldSavedData.getControllersInChunk`, which GT maintains keyed by every block of every
formed structure — and a simulated cargo launch reaches the same block entity through
`MetaMachine.getMachine(level, pos)` with no entity involved at all.**

Read against the pack's shipped `mods/gtceu-1.21.1-7.0.2.jar` and
`mods/kubejs-neoforge-2101.7.1-build.181.jar`, and our GCyR fork at
`~/Documents/curseforge/minecraft/Instances/gcyr-src`, branch `1.21.1`, HEAD `8ab24f4`.

**No sources jar for GTCEu exists** — `~/.gradle/caches/modules-2/files-2.1/com.gregtechceu.gtceu/`
holds only `gtceu-1.21.1-7.0.2.jar`. Every GTCEu claim below is read from **bytecode** with
`javap -c -p` (Homebrew `openjdk@21`). Signatures, field annotations and `putfield`/`getfield`
sites are exact; the `LineNumberTable` gives real source line numbers, quoted where they exist.
GCyR claims are read from the fork's Java source and carry file:line. Claims marked *inferred* are
reasoning on top of those readings, not readings.

## 1. How a custom multiblock is defined in GTCEu 7.0.2

Both routes exist and produce the same object. The Java builder is the real API; KubeJS is a thin
wrapper over it.

### The Java builder

`GTRegistrate.multiblock(String, Function<IMachineBlockEntity, ? extends MultiblockControllerMachine>)`
returns a `MultiblockMachineBuilder`, terminated by `register()` returning a
`MultiblockMachineDefinition`
(`com/gregtechceu/gtceu/api/registry/registrate/GTRegistrate.class`,
`com/gregtechceu/gtceu/api/registry/registrate/MultiblockMachineBuilder.class`).
`GTRegistrate.create(String)` / `createIgnoringListenerErrors(String)` are both public and static,
and `registerEventListeners(IEventBus)` attaches a registrate to a mod's bus.

This is exactly how GCyR defines its own multiblocks — `GCYRMachines.java:70-111` builds
`ROCKET_SCANNER` as `REGISTRATE.multiblock("rocket_scanner", RocketScannerMachine::new)` with
`.pattern(...)`, `.shapeInfos(...)`, `.tier(...)`, `.rotationState(...)`, `.register()`.

### The KubeJS route

`GTCEuStartupEvents` (`com/gregtechceu/gtceu/integration/kjs/GTCEuStartupEvents.class`) carries
**only four** handlers — `MATERIAL_ICON_INFO`, `WORLD_GEN_LAYERS`, `MATERIAL_MODIFICATION`,
`CRAFTING_COMPONENTS`. **There is no machine event there.** Machines come through KubeJS's own
registry system instead: `GTRegistries` registers a registry named `machine`
(`com/gregtechceu/gtceu/api/registry/GTRegistries.class`, string constant `machine` alongside
`material`, `cover`, `recipe_type`, …), i.e. `gtceu:machine`, and
`GTKubeJSPlugin.registerBuilderTypes` adds these builder types to it:

| type | builder class |
| --- | --- |
| *(default)* and `custom` | `KJSWrappingMachineBuilder` |
| `steam` | `KJSSteamMachineBuilder` |
| `generator` | `KJSWrappingMachineBuilder` |
| **`multiblock`** | **`MultiblockMachineBuilderWrapper`** |
| `tiered_multiblock` | `KJSWrappingMultiblockBuilder` |
| `primitive` | `MultiblockMachineBuilderWrapper` (pre-seeded with `PrimitiveFancyUIWorkableMachine::new`) |

(read from `GTKubeJSPlugin.class`, `lambda$registerBuilderTypes$8`, offsets 0–116.)

So the script form is `StartupEvents.registry('gtceu:machine', e => e.create('rocket_silo',
'multiblock')…)`.

`MultiblockMachineBuilderWrapper` mirrors the Java builder almost method for method — `pattern`,
`shapeInfo(s)`, `recipeType(s)`, `tier`, `rotationState`, `recipeModifier(s)`, `beforeWorking`,
`onWorking`, `afterWorking`, `additionalDisplay`, `abilities`, the model helpers — and crucially:

```
public MultiblockMachineBuilderWrapper machine(Function<IMachineBlockEntity, MetaMachine>)
public MultiblockMachineBuilderWrapper definition(Function<ResourceLocation, MultiblockMachineDefinition>)
```

`createKJSMulti(ResourceLocation)` with no creation function calls
`GTRegistrate.createIgnoringListenerErrors(id.getNamespace())` — **the definition lands in the
namespace of the id you pass**, so `planetaryfactory:rocket_silo` registers under
`planetaryfactory`, consistent with ADR-0015's one-namespace rule.

### The machine classes

- `MetaMachine` (`api/machine/MetaMachine.class`) — base; implements `IEnhancedManaged`, holds
  `FieldManagedStorage syncStorage`, `List<MachineTrait> traits`, and the static
  `MetaMachine.getMachine(BlockGetter, BlockPos)` that goes `getBlockEntity(pos)` →
  `IMachineBlockEntity.getMetaMachine()`.
- `MultiblockControllerMachine extends MetaMachine implements IMultiController` — owns
  `MultiblockState multiblockState`, `List<IMultiPart> parts`, `BlockPos[] partPositions`,
  `boolean isFormed`, `onStructureFormed()` / `onStructureInvalid()`.
- `WorkableMultiblockMachine extends MultiblockControllerMachine` (abstract) — adds
  `public final RecipeLogic recipeLogic`, `createRecipeLogic(Object...)`, and the working hooks
  `beforeWorking(GTRecipe)`, `onWorking()`, `afterWorking()`, `onWaiting()`.

`afterWorking()` on `WorkableMultiblockMachine` is the natural override point for a counter — it is
the first thing `RecipeLogic.onRecipeFinish()` calls (see §3).

## 2. Persistent custom state on a GTCEu machine — yes, two ways

**Verified.** GTCEu's persistence is LDLib's annotation-driven `ManagedFieldHolder` machinery, and
GTCEu's own fields use it.

`MetaMachineBlockEntity` (`api/blockentity/MetaMachineBlockEntity.class`) implements
`IManaged` and, through `IMachineBlockEntity` (`api/machine/IMachineBlockEntity.class`), extends
LDLib's `IAutoPersistBlockEntity`, `IAsyncAutoSyncBlockEntity` and `IRPCBlockEntity`. That is what
writes annotated fields to the block entity's NBT — no `saveAdditional` is written by hand
anywhere in GTCEu.

The annotations are `com.lowdragmc.lowdraglib.syncdata.annotation.Persisted`,
`…annotation.DescSynced` and `…annotation.UpdateListener`. `javap -v` on `RecipeLogic.class`
shows them verbatim on the fields:

```
protected int consecutiveRecipes;
  RuntimeVisibleAnnotations:
    com.lowdragmc.lowdraglib.syncdata.annotation.Persisted
    com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced
```

`progress`, `duration`, `totalContinuousRunningTime` carry `@Persisted` alone; `status` and
`isActive` carry all three plus `@UpdateListener(methodName = …)`.

The contract a subclass must honour: declare
`public static final ManagedFieldHolder MANAGED_FIELD_HOLDER` and override
`getFieldHolder()` to return it. Every level of the hierarchy does this —
`MetaMachine`, `MultiblockControllerMachine`, `WorkableMultiblockMachine` and `RecipeLogic` each
declare their own `MANAGED_FIELD_HOLDER` and their own `getFieldHolder()`.

The escape hatch, if annotations are unwanted, is the plain NBT pair also present on
`MetaMachine`:

```
public void saveCustomPersistedData(CompoundTag, boolean)
public void loadCustomPersistedData(CompoundTag)
```

`MetaMachine.saveCustomPersistedData` forwards to every attached `MachineTrait`, so an override is
expected to call `super`. The `boolean` distinguishes the drop/item path from the world-save path
(*inferred* from the signature and from the neighbouring `collectImplicitComponents` /
`removeItemComponentsFromTag`; the flag's exact meaning was not traced).

**A build note.** `mod/build.gradle:36-38` puts `mods/gtceu-*.jar` and `mods/researchd-*.jar` on
`compileOnly`. LDLib is **not** on the compile classpath that way: GTCEu jar-in-jars it —
`META-INF/jarjar/ldlib-neoforge-1.21.1-1.0.35.a.jar`, declared `embedded = true` in GTCEu's
`neoforge.mods.toml`. The `mods/ldlib2-neoforge-1.21.1-2.2.35-all.jar` the pack also ships is a
**different package** (`com.lowdragmc.lowdraglib2`) and is not what GTCEu links against
(`MetaMachine.class` references `com/lowdragmc/lowdraglib/` throughout). Writing `@Persisted` in
`mod/` therefore requires adding the extracted jarjar LDLib to `compileOnly`, or using the plain
`saveCustomPersistedData` pair, which needs no LDLib type at all. *(Verified: the package split and
the jarjar entry. Inferred: that Gradle will not see inside the jarjar on its own.)*

## 3. Does anything shipped already count completed recipes? Yes — and it is the wrong counter

`RecipeLogic` has `protected int consecutiveRecipes` with a public
`getConsecutiveRecipes()`, `@Persisted` and `@DescSynced`. It is incremented in
`onRecipeFinish()`:

```
onRecipeFinish()            // RecipeLogic.class, source lines 430-464
  430:  machine.afterWorking()
  431:  if (lastRecipe != null) {
  432:      consecutiveRecipes++            // offsets 16-23
  433:      handleRecipeIO(lastRecipe, IO.OUT)
  …
  447:      recipeCheck = checkRecipe(lastRecipe)
  448:      if (!recipeDirty && !suspendAfterFinish && recipeCheck.isSuccess())
  449:          setupRecipe(lastRecipe)     // keeps going, count survives
  451:      else { … setStatus(IDLE) …
  458:          consecutiveRecipes = 0      // offset 177 — count wiped
  459:          progress = 0
  460:          duration = 0
  461:          isActive = false }
```

**That is the disqualifier.** `consecutiveRecipes` means "recipes completed without the machine
ever stopping". The instant any of the three inputs runs dry — one tick of an empty HDPE bus — the
`checkRecipe` at line 447 fails and the counter goes to zero at line 458. A hopper hiccup at cycle
49 costs the player all 49.

It is zeroed in two more places: `resetRecipeLogic()` (source line 149) and `setupRecipe(…)`. And
`WorkableMultiblockMachine.onStructureInvalid()` calls `recipeLogic.resetRecipeLogic()` — so
**breaking one casing block also wipes it.** The pack's counter must not be this field.

`afterWorking()` (line 430, before the increment) is the hook a first-party machine overrides to
bump its own field. `MultiblockMachineBuilderWrapper.afterWorking(Consumer<IRecipeLogicMachine>)`
exposes the same hook to a script, but a script consumer has nowhere durable to write.

### Could parallel logic express "50x" instead?

Mechanically yes, and it changes the design. `ParallelLogic`
(`api/recipe/modifier/ParallelLogic.class`) exposes
`getParallelAmount(MetaMachine, GTRecipe, int)`, `getMaxByInput(…)`,
`limitByOutputMerging(…)` and `getParallelAmountFast(…)`; a `RecipeModifier`
(`api/recipe/modifier/RecipeModifier.class`) returns a `ModifierFunction` that scales a recipe, and
the builder takes `recipeModifier(RecipeModifier)`.

But `getMaxByInput` clamps parallels to **what is present in the input buses right now**. A 50×
recipe would demand all 250 HDPE + 250 circuits + 250 000 mB **simultaneously**, consume them in
one `duration`, and show one progress bar. There is no partial state and nothing accumulates: the
silo is either loaded for a whole launch or it is idle. `MultiblockDisplayText.Builder` even has
`addParallelsLine(int)` for showing it.

That is a legitimate alternative — "load the silo, run once" — but it is not the Factorio reading
of #41 ("50 cycles"), it removes the visible march toward a launch, and it needs a 250 000 mB tank
on the input side. *(Inferred design consequence; the `getMaxByInput` clamp itself is verified from
the signature and from `getParallelAmount` delegating to it.)*

## 4. Which surface owns the counter — and no mixin is needed

ADR-0015's rule is "the code owns the mechanism, the data owns the content", with
`planetaryfactory_core` reserved for *"any block whose behaviour needs a vanilla class no scripting
API exposes"*. A durable, synced, per-controller integer that survives de-forming is exactly that:
`MultiblockMachineBuilderWrapper` has no `@Persisted`-equivalent and KubeJS has no way to declare a
field on a machine. **The counter is the mod's.**

**The mod already depends on GTCEu.** `mod/build.gradle:36-38` compiles against
`mods/gtceu-*.jar`, and `mod/src/main/resources/META-INF/neoforge.mods.toml` declares
`modId = "gtceu"`, `type = "required"`, `versionRange = "[7.0.2,)"`, `ordering = "AFTER"`. The mod
already contains one GTCEu mixin,
`mod/src/main/java/com/planetaryfactory/core/mixin/gtceu/RecipeLogicMixin.java`, registered through
`mod/src/main/resources/planetaryfactory_core.mixins.json`.

**A mixin is not needed for the silo.** `MultiblockControllerMachine` and
`WorkableMultiblockMachine` are public and non-final; subclassing is the documented extension
point and is what GCyR does (`RocketScannerMachine extends …`). So:

```java
public class RocketSiloMachine extends WorkableElectricMultiblockMachine {
    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER =
        new ManagedFieldHolder(RocketSiloMachine.class, WorkableElectricMultiblockMachine.MANAGED_FIELD_HOLDER);
    @Persisted @DescSynced private int completedCycles;
    @Override public ManagedFieldHolder getFieldHolder() { return MANAGED_FIELD_HOLDER; }
    @Override public void afterWorking() { super.afterWorking(); completedCycles++; }
}
```

*(The `ManagedFieldHolder(Class, parent)` shape is inferred from every GTCEu class declaring its
own holder while inheriting one; the annotations, the `getFieldHolder()` override and
`afterWorking()` are verified.)*

Two ways to register it, both verified reachable:

1. **KubeJS calls into the mod.** `Java.loadClass` exists in this KubeJS build —
   `dev/latvian/mods/kubejs/bindings/JavaWrapper.class` declares
   `loadClass(KubeJSContext, String)` and `tryLoadClass(…)`. A startup script can do
   `const RocketSilo = Java.loadClass('com.planetaryfactory.core.machine.RocketSiloMachine')` and
   pass `.machine(be => new RocketSilo(be))` to the `multiblock` builder. **The pattern, the tier,
   the recipe type and the shape info stay in a script — data, per ADR-0015 — and only the
   mechanism is compiled.** This is the recommended shape.
2. **The mod registers it itself** with `GTRegistrate.create("planetaryfactory")` +
   `registerEventListeners(modEventBus)` + `.multiblock("rocket_silo", RocketSiloMachine::new)`.
   Works, but compiles the pattern, which ADR-0015 argues against.

## 5. How GCyR's `startRocket` reads and spends the count

`startRocket()` is `RocketEntity.java:401-445`. At that point it holds:

- `this.level()` — used at `:437`, `:440`;
- `this.blockPosition()` — the rocket's origin corner, the same value `unBuild()` uses at `:706`;
- the passenger `Player` (`:407`);
- the config item, destination `Planet`, `fuelTank`, `partsTier`.

`computeRequiredFuelAmountForDestination(Planet)` is `:386-399` and touches nothing but
`PlanetData`, `getFuelCapacity()` and four `GCYRConfig.INSTANCE.rocket.*` integers. It is a pure
function of the destination — **there is no seam in it**, which is why #41 put the launch price in
the silo rather than in the fuel bracket.

**The entity has no reference to the pad or to any machine.** Its full field list
(`RocketEntity.java:120-135`) is `fuelTank`, `configSlot`, `satelliteSlot`,
`destinationIsSpaceStation`, `partCounts`, `returnToStart`, `satelliteToLaunch`, the tier ints,
`speed`, `selectedFuelRecipe`, `thrusterPositions` — no origin `BlockPos`. And
`RocketScannerMachine.setRocketBuilt` (`:115-194`) never stores one: it copies block states into
`rocket.addBlock(...)` and calls `addFreshEntity`, and that is all. GCyR's `LAUNCH_PAD` is a plain
`BlockEntry<Block>` (`GCYRBlocks.java:431`) matched in the pattern by `blocks(LAUNCH_PAD.get())`
(`GCYRMachines.java:84`) — it is **not** an `IMultiPart`, so there is no `getControllers()` on it.

Three lookup paths, best first.

### (a) GT's own structure index — no fork state at all

`MultiblockWorldSavedData` (`api/multiblock/MultiblockWorldSavedData.class`) is a real
`SavedData` with:

```
public static MultiblockWorldSavedData getOrCreate(ServerLevel)
public final Map<BlockPos, MultiblockState> mapping;                      // keyed by controllerPos
public final Map<ChunkPos, Set<MultiblockState>> chunkPosMapping;
public Set<MultiblockState> getControllersInChunk(ChunkPos)
```

`addMapping(MultiblockState)` puts the state under `state.controllerPos`, then **iterates
`state.getCache()`** — the `LongOpenHashSet cache` of every block position the structure occupies —
and adds the state to `chunkPosMapping` for each of those positions' chunks. So a chunk containing
only a launch pad still resolves to the silo. `MultiblockState` exposes `getController()`,
`controllerPos`, `world` and `cache` publicly.

The lookup from `startRocket` is therefore:

```java
MultiblockWorldSavedData.getOrCreate((ServerLevel) level())
    .getControllersInChunk(new ChunkPos(blockPosition()))
    .stream()
    .filter(s -> s.cache.contains(padPos.asLong()))     // or: an AABB test on blockPosition()
    .map(MultiblockState::getController)
    .filter(RocketSiloMachine.class::isInstance)
```

**Verified**: the class, the two maps, `getControllersInChunk`, and that `addMapping` indexes by
the whole cache rather than by the controller alone. *Inferred*: that the rocket's chunk is one of
the silo's chunks — true if the pad is part of the silo pattern, which is what #41's "the Silo
takes over the scanner's role" implies.

### (b) The silo stamps itself onto the rocket it builds

Since the Silo replaces the Rocket Scanner as the thing that builds the rocket (#41), the
build method — the silo's analogue of `setRocketBuilt` — can set a new field on the entity before
`addFreshEntity`. That is a fork edit to `RocketEntity` (one `BlockPos` field, saved in
`addAdditionalSaveData` near `:991` and read near `:959`), and it makes `startRocket` a direct
`MetaMachine.getMachine(level(), siloPos)`. Costs a fork edit; buys exactness and survives the
rocket being flown away and back.

### (c) Bounded scan from `blockPosition()`

`level().getBlockEntity(pos)` over a small box, testing
`MetaMachine.getMachine(level, pos) instanceof RocketSiloMachine`. Cheapest to write, fuzziest
semantics with two silos side by side. Not recommended.

**Spending it.** Whatever the path, the edit inside `startRocket` sits alongside the existing
`partsTier` (`:422-425`) and fuel (`:427-431`) gates — same shape, same early `return`, and
`sendVehicleNotGoodEnoughMessage` is already the precedent for the refusal message. On success,
call a method on the silo that decrements by 50 and marks the machine dirty
(`MetaMachine.markDirty()`), placed **after** `data.set(ROCKET_STARTED, true)` at `:440` so a
refusal earlier in the method cannot spend the cycles. *(Inferred placement; the gate structure and
line numbers are verified.)*

## 6. What the player sees

**A custom line in the multiblock UI is a first-class feature, verified two ways.**

- `IDisplayUIMachine` (`api/machine/feature/multiblock/IDisplayUIMachine.class`) declares
  `default void addDisplayText(List<Component>)`, `handleDisplayClick(String, ClickData)` and
  `createUI(Player)`. A first-party machine overrides `addDisplayText` and appends its own line.
- `MultiblockDisplayText.Builder` (`api/machine/multiblock/MultiblockDisplayText$Builder.class`)
  is the formatter GT's own machines use, with `addProgressLine(double, double, double)`,
  `addProgressLineOnlyPercent(double)`, `addParallelsLine(int)`, and the general
  **`addCustom(Consumer<List<Component>>)`**.
- From a script, `MultiblockMachineBuilderWrapper.additionalDisplay(BiConsumer<IMultiController,
  List<Component>>)` appends lines without any Java.

`handleDisplayClick` is also how GCyR's scanner puts its build button in the panel
(`RocketScannerMachine.java:90`, `:96-97`), so a "Launch" button in the silo's own display is
precedent, not novelty.

The counter needs `@DescSynced` as well as `@Persisted` for the client to see it — that is exactly
the pair `RecipeLogic.consecutiveRecipes` and `status` carry.

**Breaking the silo mid-count.** Two distinct cases, and the difference is the point of not using
`consecutiveRecipes`:

- **Breaking a casing (structure de-forms).** `MultiblockControllerMachine.onStructureInvalid()`
  runs; `WorkableMultiblockMachine.onStructureInvalid()` additionally calls
  `recipeLogic.resetRecipeLogic()`, which zeroes `consecutiveRecipes`, `progress`, `duration` and
  `isActive` (`RecipeLogic.java:146-158`). **A custom `@Persisted` field on the machine is not
  touched by any of that** — the controller's block entity is still there, so the count survives
  de-form and re-form. Verified.
- **Breaking the controller itself.** `MetaMachineBlock.onRemove` calls
  `IMachineLife.onMachineRemoved()` on the machine, drops covers, then
  `level.removeBlockEntity(pos)`. The block entity and its NBT are gone; the count goes with it.
  `IMachineLife.onMachineRemoved()` (`api/machine/feature/IMachineLife.class`) is the hook if the
  pack wants to refund or warn instead of silently losing progress. Verified.

The builder also has `recoveryItems(Supplier<ItemLike[]>)` / `recoveryStacks(…)` if partial
progress should drop as something.

## 7. The simulated cargo launch reads the same field

**Yes, and by a shorter path than the rocket's.** A cargo launch with no `RocketEntity` still has a
server `Level` and a silo `BlockPos` — whatever triggers it (a display button through
`handleDisplayClick`, a quest task, a redstone edge) is already standing on the silo. The lookup is
one call:

```java
MetaMachine.getMachine(level, siloPos) instanceof RocketSiloMachine silo
```

`MetaMachine.getMachine(BlockGetter, BlockPos)` is `public static` on `MetaMachine` and does
nothing but `getBlockEntity(pos)` → `IMachineBlockEntity.getMetaMachine()`. No entity, no GCyR
code, no fork edit — which is what makes #41's "both launch kinds pay the same 50 cycles" cheap:
**one field, two readers**, and the expensive reader is the rocket's, not the cargo one's.

## Summary

| Question | Answer | Evidence |
| --- | --- | --- |
| Custom multiblock definition | `GTRegistrate.multiblock(name, ctor)` → `MultiblockMachineBuilder.register()`, or KubeJS `StartupEvents.registry('gtceu:machine')` type `multiblock` → `MultiblockMachineBuilderWrapper` | verified (bytecode) |
| Persistent custom state | yes — LDLib `@Persisted` / `@DescSynced` + own `ManagedFieldHolder`, or `saveCustomPersistedData`/`loadCustomPersistedData` on `MetaMachine` | verified |
| Existing recipe counter | `RecipeLogic.consecutiveRecipes`, but zeroed on idle (`onRecipeFinish` line 458), on `resetRecipeLogic` and on structure de-form | verified — **unusable** |
| Parallels instead of a counter | possible; `getMaxByInput` clamps to inputs present, so it becomes "load 250/250/250 000 mB and run once" | verified mechanism, inferred consequence |
| Owner under ADR-0015 | `planetaryfactory_core`, as a `WorkableMultiblockMachine` subclass; pattern and tuning stay in KubeJS | ADR-0015 + verified subclass reachability |
| Mixin needed? | **no** — mod already `compileOnly`s the GTCEu jar and hard-depends on `gtceu [7.0.2,)`; LDLib must be added to the compile classpath from GTCEu's jarjar | verified |
| `startRocket` lookup | `MultiblockWorldSavedData.getOrCreate(level).getControllersInChunk(new ChunkPos(blockPosition()))`, filtered by `MultiblockState.cache`; or stamp the silo pos on the entity at build time | verified index, inferred fit |
| Player-visible progress | `IDisplayUIMachine.addDisplayText` + `MultiblockDisplayText.Builder.addCustom`, or script `additionalDisplay(...)` | verified |
| Break mid-count | de-form keeps a custom field (only `RecipeLogic` is reset); breaking the controller destroys it, with `IMachineLife.onMachineRemoved()` as the refund hook | verified |
| Simulated cargo launch | same field via `MetaMachine.getMachine(level, siloPos)` — no entity, no fork edit | verified |
