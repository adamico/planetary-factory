# What our GCyR fork can be made to do about rockets

**Answer in one line: the rocket is validated on the *entity*, not on the scanner; the scanner
copies whatever blocks stand on the pad into a `RocketEntity` and asks no questions. A "silo has
N parts, launch permitted" rule therefore has two natural homes, both small. Rocket fuels are
ordinary GT recipes and a new one needs no source edit — KubeJS reaches `gcyr:rocket_fuel`
already. And a built rocket is never automatically taken apart, so it can be refuelled and
relaunched without rebuilding.**

Read against our fork at `~/Documents/curseforge/minecraft/Instances/gcyr-src`, branch `1.21.1`,
HEAD `8ab24f4` — the tree ADR-0001 builds `mods/gcyr-1.21.1-0.2.4+gt7.0.2-src.jar` from, carrying
the ADR-0003 registry mixin and the restored KubeJS integration. Paths below are relative to
`src/main/java/argent_matter/gcyr/` unless stated. GTCEu facts are read from
`~/.gradle/caches/modules-2/files-2.1/com.gregtechceu.gtceu/gtceu-1.21.1/7.0.2/…/gtceu-1.21.1-7.0.2.jar`
by `javap` — no sources jar exists, so those are bytecode readings, flagged where it matters.

## 1. How the pieces fit

### The scanner validates nothing about the rocket

`RocketScannerMachine` (`common/machine/multiblock/RocketScannerMachine.java`) is a GT multiblock
whose pattern is only the launch pad, a stainless-steel frame column and the controller
(`common/data/GCYRMachines.java:70-111`). Its structure check knows nothing about motors or tanks;
the shape info shows a motor/tank/seat only as a *hint* (`GCYRMachines.java:103-106`).

The whole "build" is `setRocketBuilt(true)` (`RocketScannerMachine.java:115-194`), reached from a
text button in the multiblock display (`:90`, `:96-97`). It:

- computes an AABB over the platform from `lDist/rDist/bDist/hDist`, the platform dimensions
  measured by `PlatformMultiblockMachine.updateStructureDimensions()`
  (`common/machine/multiblock/PlatformMultiblockMachine.java:45-80`, max radius 8, max height 15);
- aborts if a `RocketEntity` is already inside the bounds (`:149-152`) or if the volume is all air
  (`:154`, `:176`);
- otherwise copies **every** non-air block (with its block-entity NBT) into a fresh `RocketEntity`
  and sets the world blocks to air (`:159-190`).

There is **no** check that the assembly contains a motor, a tank, a seat, or anything else. A
single dirt block on the pad becomes a "rocket".

### The entity is where parts are counted

`RocketEntity.addBlock(PosWithState)` (`common/entity/RocketEntity.java:841-896`) is the only
place part data is derived, and it runs both when the scanner builds and when the entity is
loaded from disk (`:951-956`):

- `weight += destroyTime / 2.5` for every block with a positive destroy time (`:858-861`);
- `partCounts` counts anything whose block implements `IRocketPart` (`:864-866`);
- a `RocketMotorBlock` adds `getMotorType().getMotorCount()` to `THRUSTER_COUNT`, records a
  thruster position for particles, and updates `motorTier` as a running **average** of motor tiers
  (`:868-878`);
- a `FuelTankBlock` adds `getFuelStorage()` to `FUEL_CAPACITY` and averages `fuelTankTier`
  (`:879-888`);
- `GCYRBlocks.SEAT` registers a seat position (`:889-890`) — and seats are the *only* thing gating
  passengers: `getMaxPassengers() == getSeatPositions().size()` (`:271-273`), and a passenger with
  no seat is dismounted (`:279-282`);
- `partsTier = (motorTier + fuelTankTier) / 2` (`:893`).

`IRocketPart` is a one-method interface — `int getTier()` (`api/block/IRocketPart.java:3-6`).
`IRocketMotorType` adds `getMaxCarryWeight()` and `getMotorCount()`
(`api/block/IRocketMotorType.java:12-22`); `IFuelTankProperties` adds `getFuelStorage()`
(`api/block/IFuelTankProperties.java:14-19`). The stock motor types are
`BASIC(tier 1, weight 25, count 1)`, `ADVANCED(2, 50, 2)`, `ELITE(3, 75, 3)`
(`common/block/RocketMotorBlock.java:23-39`).

### `maxCarryWeight` gates nothing

`getMaxCarryWeight()` is declared in `IRocketMotorType.java:17`, implemented in
`SimpleRocketMotorType` and the `RocketMotorType` enum, exposed to KubeJS by
`RocketMotorBlockBuilder` — and **never read anywhere in the mod**. A grep for
`getMaxCarryWeight|MaxCarryWeight` over all of `src/main/java` returns only the interface
declaration. It is dead metadata.

Weight is not unused, though — it feeds thrust:

```
getRocketSpeed() = thrusterCount * 4.0 - (weight + 1)   // RocketEntity.java:931-933
```

which is the rocket's climb rate (`flightMovement()`, `:458-465`) and the number shown in the UI
(`getDisplayThrust()`, `:916-929`; red below 0.01). A too-heavy rocket does not refuse to launch —
it launches and never gains altitude, so `goToDestination()`'s `if (getY() < 600) return;`
(`:570`) never fires and it burns fuel forever. That is the *de facto* weight gate, and it is a
soft one.

### Where launch is actually permitted

Two entry points, both landing on `RocketEntity.startRocket()`:

1. the red **launch** button in the rocket's own UI, which calls `startRocket()` directly on the
   server (`RocketEntity.java:252`);
2. `PacketLaunchRocket` (`common/networking/c2s/PacketLaunchRocket.java:25-29`), a payload-free
   C2S packet registered at `common/data/GCYRNetworking.java:17`, which calls `startRocket()` if
   the sending player's vehicle is a `RocketEntity`.

`startRocket()` (`:401-446`) is the single gate, server-side only (`:403`), and it enforces
exactly five things: a player passenger exists (`:406-407`); the config slot holds an ID chip or
keycard (`:412`, `:443-444`); not already started (`:414`); `partsTier >= destination.rocketTier()`
(`:422-425`); and enough fuel for the destination (`:427-431`). Nothing about part *counts*,
nothing about the silo, nothing about weight.

Note that the entity UI's launch button is reachable while the rocket sits on the pad and,
notably, `PacketLaunchRocket` carries no data at all — the server trusts `player.getVehicle()`.

## 2. Inserting a "silo has N parts, launch permitted" condition

Three candidate seams, in increasing invasiveness:

| Seam | Edit | Size | What it can see |
| --- | --- | --- | --- |
| `RocketEntity.startRocket()` (`:401`) | insert a check after the tier check at `:425` | ~10 lines in one method | `partCounts`, `thrusterCount`, `fuelCapacity`, `seatPositions`, `weight`, destination |
| `RocketScannerMachine.setRocketBuilt()` (`:115`) | refuse to build unless the scanned volume holds N parts | ~15 lines, plus a UI message | the world blocks before they become an entity |
| Both | build-time refusal + launch-time refusal | ~25 lines | — |

The `startRocket()` seam is the right one for "launch permitted": it is already the place every
launch refusal lives, it already has the player to message
(`sendVehicleNotGoodEnoughMessage`, `:1049-1053` — a translatable key we can add siblings to in
`kubejs/assets/gcyr/lang/en_us.json`, which the pack already overrides), and it needs no new
state. `partCounts` is a live `Object2IntMap<IRocketPart>` on the entity (`:125`), so
"N parts of kind X" is a map lookup.

The scanner seam is the right one for "the silo is not a valid rocket at all", because refusing
there avoids ever creating a junk entity — but it must re-derive part counts from block states,
duplicating the logic `addBlock` already has.

**How invasive against a fork we already patch:** low. Both are additive edits inside existing
methods in files we already own; neither touches GTCEu, neither is a mixin, and neither changes
serialized formats. This is materially cheaper than our existing patches — ADR-0003's
`GTRegistriesMixin` is keyed to GT internals; this is not.

Two caveats worth pricing in:

- `partCounts` is **not** serialized. `writeAdditionalSaveData` (`:985-1003`) saves blocks,
  fuel capacity, thruster count, weight, timer, destination — not the counts or the tiers. They
  survive only because `readAdditionalSaveData` replays `addBlock` per block (`:951-956`). Any new
  condition should be derived in `addBlock` the same way, not stored, or it will be empty after a
  reload.
- `motorTier`/`fuelTankTier` are averages over *distinct part types*, not per-block counts
  (`:874-878`), because the divisor sums `partCounts` values filtered by
  `p.getKey() instanceof RocketMotorBlock` — but `partCounts` is keyed by the **block**, and the
  filter tests the key, so the arithmetic works only incidentally. Mixing motor tiers gives an
  integer-truncated average. If our design wants "the weakest motor decides", that is a change to
  `:874` and `:884`, not a new mechanism. *(Reading of the arithmetic; not observed in game.)*

## 3. Rocket fuel: `RocketFuelRecipes` and `gcyr:rocket_fuel`

`ROCKET_FUEL_RECIPES` is an ordinary `GTRecipeType`, registered as `gcyr:rocket_fuel` with one
fluid input slot and no outputs (`common/data/GCYRRecipeTypes.java:40-42`). The stock five are
code-defined in `data/recipe/RocketFuelRecipes.java:13-36`, emitted through GT's addon recipe hook
(`GCYRGTAddon.java:75-78` → `common/data/GCYRRecipes.java:16`).

They are never *run* as recipes. `RocketEntity` reads them as a lookup table, twice:

- the fuel tank's fill filter accepts a fluid if some `gcyr:rocket_fuel` recipe has it as input
  **and** `RecipeHelper.getRealEUt(recipe).voltage() <= motorTier` (`RocketEntity.java:144-153`);
- on contents change, the matching recipe is selected and its `duration` becomes
  `RECIPE_DURATION` (`:170-180`), which sets consumption:
  `drain = (thrusterCount + destinationTier) / (duration/20 + 1) * 2` (`:542-551`).

So a fuel recipe is three numbers: **which fluid** (input), **how good it is** (`duration` — higher
duration means *less* drain per tick), and **what motor tier it demands** (`EUt`).

### Can a new low-tier fuel come from KubeJS or a datapack?

**Yes, no source edit needed.** GTCEu's KubeJS plugin registers its recipe schema for *every*
`GTRecipeType` in `BuiltInRegistries.RECIPE_TYPE`, keyed by that type's ResourceLocation —
`GTKubeJSPlugin.registerRecipeSchemas` iterates the registry's key set, `instanceof
GTRecipeType`-filters, and registers `GTRecipeSchema.SCHEMA` under the id (verified by `javap -c`
on `com/gregtechceu/gtceu/integration/kjs/GTKubeJSPlugin.class`, bytecode offsets 0-66). `gcyr:rocket_fuel`
is in that registry via `GCYRRecipeTypes.register` (`GCYRRecipeTypes.java:44-55`, which registers
into `RECIPE_TYPES` and attaches a `GTRecipeSerializer`), so
`ServerEvents.recipes(e => e.recipes.gcyr.rocket_fuel(...).inputFluids(...).duration(n).EUt(t))`
is available with no fork change. A hand-written `data/<ns>/recipe/*.json` with
`"type": "gcyr:rocket_fuel"` should work for the same reason — the serializer is registered under
that id — though we have not written one to confirm the exact JSON shape. *(KubeJS path:
verified from bytecode, not yet exercised in-game. JSON path: unverified.)*

This matters because it is the opposite of the ADR-0003 material situation: materials needed a
fork-side loader; fuels do not.

### What the `EUt`-as-tier-indicator convention constrains

The upstream comment is explicit: *"use EUt as a bogus tier indicator. more than a rocket's motor
tier fuels aren't allowed to be used in the rocket"* (`RocketFuelRecipes.java:16`). The comparison
is `getRealEUt(recipe).voltage() > motorTier` (`RocketEntity.java:145`) where `motorTier` is 1..3
from `RocketMotorType`. So the `EUt` field is a **raw small integer compared against a motor
tier**, not a GT voltage: gasoline and diesel are `EUt(0)` (any motor), rocket fuel and hydrogen
`EUt(1)`, hydrogen plasma `EUt(3)`. Constraints that follow:

- a new *low-tier* fuel is `EUt(0)` or `EUt(1)` and needs a `duration` chosen relative to the
  existing scale (gasoline 25, diesel 18, rocket fuel 75, hydrogen 10, plasma 18);
- we cannot use `EUt` to mean energy anywhere in this recipe type — the field is spent;
- because `motorTier` is an **average** and starts at 0, a rocket with no motor accepts only
  `EUt(0)` fuels, and the filter is evaluated against whatever `motorTier` is at fill time.

**A latent bug to know about before designing around it:** `FUEL_CACHE` is a `static`
`Object2BooleanMap<Fluid>` (`RocketEntity.java:104`) populated with `computeIfAbsent`
(`:144`) — but the predicate it memoises depends on *this rocket's* `motorTier`. The first rocket
in the session to test a fluid fixes the answer for every rocket until restart. So a tier-1 rocket
that first touches hydrogen plasma will bar it for everyone; a tier-3 rocket that touches it first
will *allow* it in tier-1 rockets. If our progression leans on fuel tiering, this needs fixing
in the fork (drop the cache, or key it by `(fluid, tier)`) — a two-line change.

## 4. What the KubeJS plugin already exposes

`GCYRKubeJSPlugin` (`integration/kjs/GCYRKubeJSPlugin.java`) — restored in our fork by
`fcd2dfe` and re-keyed for KubeJS 2101.7.1 by `0829b4b` (see the in-file comment at `:13-14` and
ADR-0001's note about the deleted class) — registers exactly two block builder types:

| Builder type | Class | Settable |
| --- | --- | --- |
| `rocket_motor` | `integration/kjs/builders/RocketMotorBlockBuilder.java` | `tier`, `maxCarryWeight`, `motorCount`, `typeId` |
| `fuel_tank` | `integration/kjs/builders/FuelTankBlockBuilder.java` | (tank properties — same shape) |

`RocketMotorBlockBuilder.createObject()` (`:28-33`) builds a `SimpleRocketMotorType` from those
four fields, constructs a `RocketMotorBlock`, and registers it into
`GCYRBlocks.ALL_ROCKET_MOTORS` — so a script-declared motor is a first-class `IRocketPart` and is
counted by `addBlock` exactly like the built-in ones.

**What that means for us:** new rocket motors and fuel tanks at any tier, count, and storage are
scriptable today, from `kubejs/startup_scripts/blocks.js` where our other blocks already live.
Combined with §3, that covers *parts* and *fuels* with no fork change. What is **not** exposed and
would need a fork edit: the launch condition itself, the scanner's behaviour, `partsTier`
arithmetic, and anything about weight. Note also that `maxCarryWeight` is settable from KubeJS and
still does nothing (§1) — setting it is documentation, not mechanism.

## 5. Can the build be skipped, or reused for later launches?

**Reused: yes, and by default it already is.** Nothing automatically converts a landed rocket back
into blocks. `unBuild()` (`:698-728`) — which places the blocks back into the world and discards
the entity — has exactly three callers:

- `causeFallDamage`, only when `doCrashLandingExplosion` is on **and** fall distance > 48 (`:486-489`);
- the yellow **unbuild** button in the rocket's UI (`:253`);
- `doesDrop`, called from `checkOnBlocks` (`:509-539`) — and **`checkOnBlocks` is never called from
  anywhere**. A grep over `src/main/java` finds only its declaration. The landing-disassembly path
  upstream apparently intended is dead code in this tree.

After travel, `goToDestination` moves the entity to the destination dimension and resets
`ROCKET_STARTED`, `START_TIMER` and destination (`:686-695`), leaving the built rocket standing.
Refuel it, put a new ID chip in, press launch again. Fuel is persisted on the entity
(`:991-992`), as are the blocks. So "one build, many launches" needs *no* change at all — the
question is whether we want to *prevent* it.

**Skipped entirely: not without new code, but the surface is small.** The scanner is the only
producer of `RocketEntity` (`GCYREntities.ROCKET.create` at `RocketScannerMachine.java:156` is the
sole call site). Two shapes are available if we want a rocket without a player-built silo:

- have something else populate a `RocketEntity` via the public `addBlock(BlockPos, BlockState,
  CompoundTag)` (`:828-830`) and `addFreshEntity` — the same six lines the scanner uses. This is
  reachable from a KubeJS script in principle, since both are public, though nothing in the pack
  does it today and it is **unverified** whether KubeJS's Rhino bindings reach
  `GCYREntities.ROCKET` cleanly;
- skip the entity altogether and treat launch as a machine operation. Nothing in `startRocket()`
  is reusable for that — it is all entity state — so this is a new mechanism, not a reuse.

And flight itself cannot be short-circuited from outside: `goToDestination()` is private and
gated on `getY() >= 600` (`:569-570`), reached only from `tick()` after the 200-tick countdown
(`:196-205`, `:449-455`). Teleporting a rocket "as if launched" means either calling into the
entity from fork code, or reimplementing the destination logic.

## Summary of what is free vs. what costs a fork edit

| Want | Cost |
| --- | --- |
| New rocket motor / fuel tank blocks, any tier | KubeJS `startup_scripts` — free (§4) |
| New rocket fuel, any tier | KubeJS `ServerEvents.recipes`, `gcyr:rocket_fuel` — free (§3) |
| Reuse a built rocket for many launches | already the behaviour — free (§5) |
| "Silo has N parts" launch gate | ~10 lines in `startRocket()` — cheap fork edit (§2) |
| Build-time refusal of an invalid silo | ~15 lines in `setRocketBuilt()` — cheap fork edit (§2) |
| Weight actually limiting launch | fork edit; `maxCarryWeight` is dead metadata (§1) |
| Correct per-rocket fuel tiering | fork edit; `FUEL_CACHE` is static and cross-contaminates (§3) |
| Launch without building a rocket | new mechanism, not a reuse (§5) |
