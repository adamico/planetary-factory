# What SimpleBelts could carry: belts and loaders without Create

Read against a clone of `Rearth/SimpleBelts` on its `26.1.2` branch at `92a97c3` (mod version
`2.0.0-exp1`), placed beside the pack as `../simplebelts-src`. The matching jar,
`belts-neoforge-2.0.0-exp1.jar`, is installed in the probe instance `../pf2612` (NeoForge 26.1.2.109).
The 1.21.1 line ended at tag `v0.2.2`. It is cited only where it differs. Paths below are relative to
`common/src/main/java/rearth/belts/` unless they say otherwise. SimpleBelts is **CC-BY-4.0**
(`LICENSE`), so a fork is allowed with attribution.

Oritech is re-read at `v2.0.0-exp6` (`230723a`) **only where a belt row touches it**. That means its
item capability and its machine inventories. The rest of `oritech-coverage.md` still cites `v1.2.12`.
Re-reading it at 2.0 is a separate job.

**This is a survey, not a decision.** No ADR is proposed and no ledger row is edited. It answers one
question: if Create left the pack and SimpleBelts took the belts, what would carry each row of
`docs/factorio-mechanics.md` that Create carries today, and at what cost? Rulings made after the first
pass (2026-09-11) are marked **Ruling** and are folded into the rows they settle.

## The hypothesis being surveyed

This survey sits **on top of** `oritech-coverage.md`'s hypothesis and replaces that hypothesis's
logistics row. There is one scenario, called S2 here:

| role | mod |
| --- | --- |
| belts, loaders, splitters | SimpleBelts (a fork) |
| trains | Railcraft Reborn |
| machines, fluids | Oritech |
| everything else | `planetaryfactory_core` |

**Leaves:** Create entirely. Create: Power Grid, GCyR, Modern Industrialization and GregTech were already gone.
**Minecraft version:** 26.1.2. That version is only reachable because Create leaves: Create has no
26.1.2 build, so keeping it for trains would pin the pack to 1.21.1.

## Language

SimpleBelts calls its belt-end block a **chute**. The pack would call it a **loader**, both in the
survey and in the fork's lang file. The name is honest about what the block does (fact 1) and avoids
two collisions: Create's Chute, and `CONTEXT.md`'s _Avoid: chute_ under **Drop Hatch**. The glossary
does not take the term until the block ships. The rest of this document says *loader* and keeps
`ChuteBlockEntity` for the class.

## Method

**Levels.** Each row gets the cheapest level that reaches every number in it. SimpleBelts has **no
Native level** (fact 2), so there are three:

1. **KubeJS**: recipes and the sweep, with no Java.
2. **Fork**: an edit to the pack's fork of SimpleBelts. The fork is about 2,400 lines of Java. The
   pack already maintains three forks (GCyR, Researchd, Respoiled). A change that belongs upstream is
   also shaped as a pull request to Rearth, but adoption never depends on one being accepted.
3. **Core**: a block or rule in `planetaryfactory_core`.

**Criteria.** The rows are the ledger's Transport belts and Inserters sub-rules. The grilling that
commissioned this survey added three criteria the ledger does not have, because Create made them free:

- **The tap.** A belt can be loaded and unloaded partway along its length, not only at its ends.
- **Cost per length.** A longer belt costs more belt.
- **Obstruction.** A belt cannot pass through solid blocks or other belts. It may still curve through open air.

**Numbers.** The committed corpus has belt, inserter and splitter *recipes* and *technologies*, but
`data/factorio/machine.json` has no rows for their *entities*. Every entity number below is read from
the raw dump (`~/Library/Application Support/factorio/script-output/data-raw-dump.json`) and named by
prototype. Extending the extractor so these numbers become committed corpus has been put off on purpose.

**Proof.** A claim about behaviour in play is marked **`world-load (human)`**.

---

## Cross-cutting facts

### 1. A SimpleBelts belt joins two loaders, and loaders are accepted

The mod registers three blocks and two belt items (`ItemContent.java:16-19`):

- the chute, which this survey calls the **loader**
- the **conveyor support**
- **`belt`**, tier 1
- **`improved_belt`**, tier 2

Right-clicking a belt item on one loader and then on another makes a belt between them. Supports
along the way shape the spline.

The state lives on the source loader: a `target`, a list of `midPoints`, and a `Deque<BeltItem>`
of items in transit (`blocks/ChuteBlockEntity.java:53-58`). A loader is *either* a source *or* a
target. `BeltItem#useOnBlock` refuses a loader that `isUsed()`.

This has consequences:

- **Nothing gets on or off a belt between its ends.** There is no side-loading, no merging and no
  splitting.
- **A belt cannot feed a belt directly.** Between two belts there has to be a block that exposes
  the item capability.
- **A source loader pulls by itself.** Every `getExtractionInterval()` ticks it takes the first stack
  from the inventory behind it that passes the filter, up to 64 items (`:215-238`). A target loader
  pushes the whole stack into the inventory behind it, all or nothing (`:195-205`).

- **Only a loader can be a belt end.** `BeltItem#useOnBlock` looks up
  `BlockEntitiesContent.CHUTE_BLOCK` at each end, `BeltData.create` refuses a target that isn't a
  loader, and a loader holds a single outgoing `target` and a single incoming `sourceBeltPos`.

Factorio has loaders too: `loader`, `fast-loader`, `express-loader` and `turbo-loader` are in the
dump, at the belt speeds. **All four are `hidden: true`**, so a Factorio player never gets one.

**Ruling (2026-09-11): loaders are accepted in place of inserters.** Factorio hides its loaders
because they are too strong, not because they fall outside its model, and Factorio mods that expose
them commonly balance them with an energy cost. The pack does the same (fact 5). The ledger would
record this as a notice, not a gap.

### 2. There is no config, and every dial is a constant

`ChuteBlockEntity.java:46-50`:

- `BASE_BELT_SPEED = 0.9f` blocks per second
- `EXTRACTION_INTERVAL = 26` ticks
- `ITEM_QUEUE_SPACING = 0.8f` blocks
- `MAX_BELT_TIER = 2`

All four are `private static final`. The 64-item grab is a literal at `:228`. A tier multiplies speed
and divides the interval and the spacing (`:326-345`). There is no config file, no KubeJS surface and
no public API except `api/item/ItemApi`, the platform wrapper around item storage.

That makes every dial a **fork edit**. A mixin could reach the inlined constants with
`@ModifyConstant`, but a mixin into a mod labelled `exp` breaks on every version bump, and several of
the edits below add state that a mixin shouldn't carry.

**As shipped, a belt's throughput is set by the loader, not by the belt.** A tier-1 loader grabs every
26 ticks, 0.77 times a second. One grab can be anywhere from 1 to 64 items, so a tier-1 belt carries
somewhere between **0.77 and 49 items/s**, depending on what the source happened to hold. Tier 2 is
double. The ledger's throughput sub-rule makes the same complaint about Create, which bounded
entries-per-second and left items-per-entry free. Here both are free, and both are one-line edits.

### 3. Loaders speak NeoForge's transfer API, and Oritech 2.0 fences its machines

`neoforge/.../NeoforgeItemApiImpl.java` wraps `level.getCapability(Capabilities.Item.BLOCK, …)`, which
returns a `ResourceHandler<ItemResource>` from NeoForge's transfer API with `Transaction`s. So a loader
works against **any** block that answers that capability. It needs no compatibility code per mod.

Oritech 2.0 answers it on every machine tagged `@AssignSidedInventory`
(`init/BlockEntitiesContent.java:308-312`), through `MachineBlockEntity#getItemLookup`. The default
lookup is `InOutInventoryStorage#getExternalAccess()`, which **accepts insertion only into input
slots and allows extraction only from output slots** (`api/transfer/item/InOutInventoryStorage.java:27-58`).
In the machine's `SIDED` input mode, `UP` is input-only and `DOWN` is output-only
(`MachineBlockEntity.java:566-600`).

So a source loader behind an Oritech machine takes products and never inputs, and a target loader
feeds inputs and never outputs. **Native, with no fork.** The loader's slot walk (`:224-237`) reads
every slot but extracts through the fenced handler, so an input slot it passes over returns 0 and it
moves on.

### 4. The tap is postponed, because the splitter covers the main bus

A tap is a change to the data structure. Every item on a belt advances by the same `progressDelta`
each tick (`:168-212`), and spacing is enforced only **in the queue at the target end**. Inserting
partway along a belt would need three things:

1. an ordered list in place of the `Deque`
2. a gap check on insertion
3. a mapping from a world position to progress along the spline

`BeltData` holds the spline's points and lengths (`record BeltData`, `:422`), and
`collision/BeltCollisionRegistry.java` already samples the spline every `SAMPLE_LENGTH = 0.2` blocks.
So the tap is reachable. The upstream PR shape would be two public methods (*insert at progress `p`
if the gap allows*, *extract the nearest item within `ε` of `p`*) plus a position→progress lookup.

**Ruling (2026-09-11): postponed.** The splitter (see the belts matrix) covers what the main bus needs:

- **Taking off the bus:** the bus runs into a splitter, one output belt goes to the machine and the
  other carries on down the bus.
- **Putting back onto the bus:** the same block merges a machine's output belt into the bus.

What the tap alone buys is a swing arm reaching into a belt partway along. The pack has no swing arm
(the Inserters matrix), so for now the tap has nothing to serve.

### 5. Loaders draw FE, as a balance cost

**Ruling (2026-09-11):**

- **Tier 1 is unpowered.** It stands in for Factorio's burner era, so belts work before the first pole.
- **Tiers 2 to 4 draw FE.** The charge is **per item moved, plus an idle drain**.
- **The starting figure is the matching inserter's energy per item:** `fast-inserter` for tier 2, and
  `bulk-inserter` for tiers 3 and 4 (the Inserters matrix). It is derived by simulating the swing from
  the dump, not typed, and it is expected to move after play. It is a starting point for a balance
  cost, not a claim that a loader should match an inserter.

In Factorio an item's path is machine → inserter → belt → inserter → machine, which is two inserter
moves. A source and a target loader are also two moves, so a factory's energy per item stays in
Factorio's range. For scale, a Factorio inserter spends somewhere over ten kJ per item, so a saturated
belt's two loaders draw a few hundred kW, against a Steam Engine's 900 kW. That is plausible and it is
also a lot, which is why the figure is a play-test dial.

**The splitter draws nothing.** Factorio's `splitter` prototype has no `energy_source`, and the pack's
splitter involves no loader (belts matrix). An item on the bus therefore pays only where it enters and
leaves the belt network, however many splitters it passes through.

**How the power arrives.** The fork adds an FE buffer to `ChuteBlockEntity` and exposes
`Capabilities.Energy.BLOCK` on it. The core's Supply Area Pole already powers *every* position in its
area that answers an energy capability (`SupplyAreaPoleBlockEntity#container`, `:207-222`). Today it
asks for GregTech's `GTCapability.CAPABILITY_ENERGY_CONTAINER`, which leaves with GregTech. Once it
asks for FE, it powers loaders with no wiring and no further core code.

### 6. Every tick of movement resends the whole belt

`moveItemsOnBelt` sets `networkDirty` whenever any item moves (`:186-187`), and `tick` then calls
`sendBlockUpdated` (`:118-121`). `getUpdateTag` is `saveCustomOnly` (`:300-303`), which writes the
**whole** `moving` list (`:264-270`).

At today's density that costs little. At Factorio's density (fact 7), a 64-block yellow belt holds
512 entries, and that is **512 serialised stacks per belt per tick**. The fork has to send only
deltas, or send insertions and removals and let the client simulate movement from the known speed.
The renderer already interpolates through `lastRenderedPositions`, so it tolerates the second.
How many belts a base can run is `world-load (human)` either way.

### 7. Factorio's belt numbers, and what they become

| prototype | `speed` (tiles/tick) | tiles/s (×60) | items/s (×480) | splitter | underground `max_distance` |
| --- | --- | --- | --- | --- | --- |
| `transport-belt` | 0.03125 | 1.875 | **15** | `splitter` 0.03125 | 5 |
| `fast-transport-belt` | 0.0625 | 3.75 | **30** | `fast-splitter` 0.0625 | 7 |
| `express-transport-belt` | 0.09375 | 5.625 | **45** | `express-splitter` 0.09375 | 9 |
| `turbo-transport-belt` | 0.125 | 7.5 | **60** | `turbo-splitter` 0.125 | 11 |

The 15 items/s is **two lanes of 7.5**. Lanes stay excluded, so **one SimpleBelts belt carries the
whole belt's 15 / 30 / 45 / 60**. Ratio arithmetic ("one yellow belt feeds N furnaces") is done
against the whole belt, and with no lanes there is nothing to halve.

The fork sets speed to Factorio's tiles/s and density to **8 items per block**, which is Factorio's 4
per lane across 2 lanes, with one item per entry. The belt's items/s then equals Factorio's exactly.
Rendering 8 items per block over a long run is `world-load (human)`.

**A loader has to put more than one item on per tick above 20 items/s.** A block entity ticks 20
times a second, so a loader that adds one entry per tick tops out at 20 items/s. That covers tier 1
but not tiers 2 to 4. The fork's loader adds `ceil(items/s ÷ 20)` entries per tick, spaced along the
belt head.

`transport-belt`'s recipe is 1 `iron-plate` + 1 `iron-gear-wheel` → **2**. So Factorio's own price is
half a craft per tile.

### 8. With no Create, there is no rotation

The pack's only rotational unit (SU) came from Create. Under S2 nothing in the pack consumes or
produces rotation, so the logistics and energy boundary in `oritech-coverage.md` collapses:

- **Option (a), the FE→SU bridge:** void. There is nothing to drive.
- **Option (b), two currencies:** void.
- **Option (c), fluids to Oritech:** forced, since Create's pipes leave.

**The pack has one energy currency, FE.** The joules-per-FE constant (that survey's fact 1) becomes a
prerequisite of the loader's charge too.

### 9. The core already speaks the old capability API

NeoForge 26.1 replaced `IItemHandler`, `IFluidHandler` and `IEnergyStorage` with the transfer API
that SimpleBelts 2.0 and Oritech 2.0 already use (fact 3). Ten classes in `mod/src/main/java` touch
the old API:

- `PFBlockEntities`
- `PFItems`
- `energy/PoleEnergyStorage`
- `fluid/BoilerBlockEntity`, `fluid/BoilerItemHandler`, `fluid/OffshorePumpBlockEntity`
- `mining/rig/RigBlockEntity`, `mining/rig/RigItemHandler`
- `smelting/FurnaceItemHandler`, `smelting/FurnaceSlots`

The Supply Area Pole is on GregTech's energy API as well (fact 5).

The core has **no Create compile dependency**. `RigBlockEntity:289-293` and `RigSlots:51` mention
`DirectBeltInputBehaviour` only in comments that say why it isn't called. So Create's departure
costs the core no code. The version change does.

---

## The matrix: Transport belts

Ledger verdicts are today's, with Create. **Level** is the cheapest level that closes every gap in the row.

| sub-rule | ledger today | SimpleBelts 2.0 as shipped | level | gap, and what closes it |
| --- | --- | --- | --- | --- |
| **Three belt tiers** | `adapted`: one belt, speed bought with RPM | two tiers, `belt` and `improved_belt` (`beltTier` 1 and 2) | **Fork** + **KubeJS** | `MAX_BELT_TIER` 2 → 4, and two more `BeltItem` registrations. Each tier's multiplier is replaced by Factorio's tiles/s (fact 7). The tiers are 1× / 2× / 3× / 4× of 1.875, so it is a clean multiplier anyway. Recipes are emitted at the corpus's costs onto the Assembler surface, and SimpleBelts' own (`dried_kelp` + `stick` → 8, `recipe/belt_item*.json`) go to ADR-0034's sweep. `logistics-2`, `logistics-3` and `turbo-transport-belt` **buy something again**, and drop off #25's prune list. The row would become Factorio's own. |
| **Throughput as a ratio budget** | `planned`: target deferred to play, dials named on Create | throughput is set by the loader, 0.77 to 49 items/s at tier 1 (fact 2) | **Fork** | Speed at tiles/s and spacing at `1/8` block, one item per entry, which clamps the literal 64 at `:228`. Loaders add several entries per tick above 20 items/s (fact 7). The belt then carries exactly 15 / 30 / 45 / 60 items/s. The number is **known and computable**, which is what the sub-rule asked for. It also carries the sync rewrite (fact 6), because the density is what makes the sync expensive. |
| **Underground belts** | `excluded`: the weaving problem is 2D | none | **unchanged** | The argument from the medium holds with more force: a spline goes over whatever it has to cross, by construction. With obstruction on (below), it still can't go *through* a belt. |
| **Splitters, with filtering and priority** | `adapted`: Brass Tunnel's `SelectionMode` | none; a chest with two source loaders splits whichever grabs first | **Fork** (**Ruling 2026-09-11**) | **One block, two blocks wide, with two belts in and two out**, which is Factorio's own `splitter` shape. Each half is a belt end for one incoming and one outgoing belt. Items go from the queue at the end of an incoming belt straight to the head of an outgoing belt, with no inventory and no loader in between. It splits 1:1, merges, and has input and output priority and one filter, so it is Factorio's splitter and merger in one entity. It runs at its tier's belt speed (`splitter` / `fast-splitter` / … in fact 7) and **draws no power**. It has to be in the fork, because only a loader can be a belt end (fact 1). The fork generalises `ChuteBlockEntity`'s single `target` and single `sourceBeltPos` into one of each per half. The recipe is corpus (`splitter`: 5 `electronic-circuit`, 5 `iron-plate`, 4 `transport-belt`). Chaining splitters builds a balancer, which closes the ledger's **constructed-balancer** gap that Create's one-block outcome never did. It is also a clean upstream PR, since it is generic belt topology. Priority when one output backs up is `world-load (human)`. |
| **Two lanes per belt** | `excluded` | one lane | **unchanged** | The belt carries the whole belt's throughput (fact 7). |
| **Belt as buffer** | `excluded`: 64 items per 64 blocks against Factorio's 512 | a queue at the target end, `0.8`-block spacing | **Fork**, restored | At 8 items per block, the density throughput needs anyway, a 64-block belt holds **512**, which is Factorio's number. The ledger's stated reason for excluding the idiom disappears, and the fork that fixes throughput restores it with no further work. |
| **Output onto a moving belt with no intermediate block** (Mining drills, `shipped`) | the rig pushes into a bare Create belt's handler | a loader has no handler, so the rig's push (`RigBlockEntity#push`) finds nothing | **Native** | Reached the other way round, the way the ledger already describes for Create's funnel: a source loader set against the rig pulls through `RigItemHandler`. No code is needed. The loader has to sit against a position that answers the capability, and whether every hull position does under ADR-0059 is `world-load (human)`. |
| *added:* **The tap** | free with Create | none (fact 1) | **postponed** (**Ruling 2026-09-11**) | The splitter covers the main bus in both directions (fact 4). It is reachable as a fork when something needs it, and nothing does while there is no swing arm. |
| *added:* **Cost per length** | Create charges one belt item per segment | one belt item for any length (`BeltItem#createBelt`, `stack.shrink(1)` at `:160`) | **Fork**, **mandatory** (**Ruling 2026-09-11**) | A precondition of adoption, not a tuning choice. `BeltData.totalLength()` is known at placement. Consume `ceil(length)` belt items, which is Factorio's one per tile, and refuse the belt if the player holds fewer. The length stays uncapped, and the cost is what limits it. |
| *added:* **Obstruction** | Create belts are blocks | nothing checked: `BeltItem`'s only refusals are a loader already in use and a duplicate support (`items/BeltItem.java:69,105`); the endpoints only need to be replaceable (`:124,174,183`) | **Fork** | At placement, sample the spline every `SAMPLE_LENGTH` (the walk `BeltCollisionRegistry` already does) and refuse on a solid block or on another belt's registered segment. The curve through open air stays, and it is the one thing SimpleBelts does better than a Factorio belt. |

**What survives natively:** the belt backs up visibly at the target end (`outputQueue`, drawn with
spacing). That is the compression diagnostic, a belt that shows where the slow machine is. Filtering
at a source loader is one item or an FTB Filter System filter (`:240-250`), and FTB Filter System has
a 26.1.2 build in pf2612.

## The matrix: Inserters

**Ruling (2026-09-11): no inserter.** The loader stands in for it. With the tap postponed, an inserter
would only move items between two neighbouring inventories, and a belt of one or two blocks does that,
cheaply at one belt item per block.

The inserter prototypes stay in this survey for one job: they anchor the loader's energy figure
(fact 5).

| prototype | `rotation_speed` (rev/tick) | pickup / insert (tiles) | energy per movement / rotation | drain | role here |
| --- | --- | --- | --- | --- | --- |
| `burner-inserter` | 0.013 | 1 / 1.2 | 50 kJ / 50 kJ | burner, `chemical` | none: tier-1 loaders are unpowered |
| `inserter` | 0.014 | 1 / 1.2 | 5 kJ / 5 kJ | 0.4 kW | none |
| `long-handed-inserter` | 0.02 | 2 / 2.2 | 5 kJ / 5 kJ | 0.4 kW | none |
| `fast-inserter` | 0.04 | 1 / 1.2 | 7 kJ / 7 kJ | 0.5 kW | anchors the tier-2 loader |
| `bulk-inserter` | 0.04 | 1 / 1.2 | 20 kJ / 20 kJ | 1 kW | anchors the tier-3 and tier-4 loaders |
| `stack-inserter` | 0.04 | 1 / 1.2 | 40 kJ / 40 kJ | 1 kW | Space Age. `stack_size_bonus` 4. See stack bonus below. |

All six have `filter_count` 5. **The energy per item is not in the table on purpose.** It depends on
`rotation_speed`, `extension_speed`, pickup timing and hand size, and it has to be simulated, not read
off. Deriving it is the loader charge's own unit test. Nobody should transcribe a wiki figure.

| sub-rule | ledger today | level | reading |
| --- | --- | --- | --- |
| **The inserter as an entity** | `adapted`: Create funnels and arms, #102 open on the Arm | **Fork** (the loader) | It would stay `adapted`, with the notice **"a belt's ends load and unload it; there is no swing arm"**. Loaders move items between an inventory and a belt at the belt's rate (fact 7). Tiers 2 to 4 draw FE per item moved (fact 5). **#102's answer becomes "the loader".** The Arm leaves with Create. |
| **Burner inserter** | — | **Fork** | Tier-1 loaders are unpowered, so they fill the burner inserter's slot at rung 0: automation before the first pole. |
| **Swing-arm reach across a belt** | lost | **excluded** | There is no swing arm to reach with. Adjacent inventories are joined by a short belt. |
| **Long-handed tier** | lost | **excluded** | Same reason. `long-handed-inserter`'s recipe goes unemitted, and `automation` unlocks only `assembling-machine-1`. |
| **Stack-size bonus research** | lost | **Fork** + research | It becomes a **researched grab size on the loader**. The one-item-per-entry clamp relaxes to `1 + bonus`, so a belt carries stacked entries, which is Factorio 2.0's belt stacking. In the full dump, `belt-stack-size-bonus` comes from **three** technologies: `stack-inserter`, `transport-belt-capacity-1` and `-2`. `inserter-capacity-bonus-1`…`-7` grant `bulk-inserter-capacity-bonus`, plus `inserter-stack-size-bonus` at `-2` and `-7`. ADR-0044 says "exactly one", which was true of the pruned corpus but not of the dump. Which technologies the loader reads is #25's call. |
| **Filters** | — | **Native** | One item or an FTB Filter System filter per source loader. Factorio's `filter_count` is 5. An FTB Filter System filter can combine several items, which covers it with no fork. How many items one filter can hold is `world-load (human)`. |

---

## Create's other rows under S2

These are not SimpleBelts rows, and they are **not sized** here. They are listed so that nothing
Create carries today leaves without a recorded owner.

| row (ADR-0017's table, or the ledger) | Create carries today | under S2 |
| --- | --- | --- |
| **Trains** (ledger `planned`; outfield patches are "reached by rail", ADR-0045) | Create trains | **not SimpleBelts → Railcraft Reborn**. Its repo has a `26.1.x` branch. It is not yet in pf2612. |
| **Fluid logistics** | pipes, pumps | **not SimpleBelts → Oritech** pipes (`oritech-coverage.md` option (c), now forced). The Offshore Pump and Boiler move to the transfer API (fact 9). |
| **Bulk storage (fluid)** | Fluid Tank, three blocks to one Factorio tank (ADR-0037) | **not SimpleBelts → Oritech** or core. Unsized. |
| **Bulk storage (item)** | Item Vault | **unowned**. Factorio's chests are `containers` in `machine.json`, so a core block, or vanilla chests at the corpus's slot counts. Unsized. |
| **Package logistics** (ADR-0018 rung 2's "movement at scale") | Create 6 packages | **unowned.** Rung 2 keeps the oil chapter and loses its logistics clause. What `logistic` science buys at rung 2 is #25's call. |
| **Barrelling** | the Spout fills any fluid-holding item, so the 18 barrel recipes are not emitted (ADR-0017) | **unowned.** The 18 barrel recipes need a surface, most likely an Oritech machine that takes fluid. |
| **The Create kinetic recipe line** (`create-recipe-convert.py`, `data/pack/create-substitutions.json`, `test_create_recipes.py`) | shafts, cogwheels, gearboxes, water wheels | **deleted**, along with its converter, its check and its subtree under `recipe/assembling/create/`. |
| **Splitters, inserters** | Brass Tunnel, Mechanical Arm | **the fork**: the splitter block and the loader (the matrices above) |

---

## What 26.1.2 still costs

What pf2612 already runs is **proven to load together**. The rest is what adoption would still have to prove or build.

| item | in pf2612 | status |
| --- | --- | --- |
| Oritech | 2.0.0-exp6 | loads. **A pre-release**; the Oritech survey's citations are at 1.2.12. |
| SimpleBelts | 2.0.0-exp1 | loads. **A pre-release.** The fork would be taken from here. |
| Building Gadgets 2 | 1.4.6 | loads. It keeps the *Construction robots and blueprints* row. |
| FTB Filter System | 26.1.2.2 | loads. The loader's filter integration works with it. |
| KubeJS / Rhino | 8.0.6 | loads. ADR-0023's pin was GTCEu's, and it goes with GregTech. |
| JEI, Jade, Block Runner, Architectury, GeckoLib | yes | load |
| **EMI** | `emi-unofficial-port-unstable` 1.1.24 | **loads, unofficially.** The Personal Assembler depends on EMI's Fill Recipe reaching its panel, so an unstable EMI is a risk to the hand-crafting surface, not just to recipe viewing. |
| Railcraft Reborn | no | upstream `26.1.x` branch exists. Untried. |
| FTB Quests | no | upstream `main` is 26.1.2. Untried. |
| AE2, Sophisticated Backpacks | no | Modrinth lists 26.1.2 builds. Untried. |
| Almost Unified | no | not found on Modrinth or GitHub for 26.1.2. **Unverified**, not absent. |
| **`planetaryfactory_core`** | no | ours. 16,800 lines. Ten classes on the old capability API, plus the pole on GregTech's (fact 9), plus Minecraft 26.1's renames across everything else. |
| **Researchd** (fork) | no | ours to port. Upstream `Porting-Dead-Mods/Researchd` `main` is 1.21.1. |
| **Respoiled** (fork) | no | ours to port. `main` is 1.21.1. |

**The three forks are the real cost.** Everything else is "add it to pf2612 and launch".

---

## Tally

- **Native**: loaders against Oritech machines (fact 3), the rig's output onto a belt (by pulling),
  and filters.
- **KubeJS**: belt and splitter recipes at the corpus's costs, and sweeping SimpleBelts' own.
- **Fork**: the belt, loader and splitter work.
  - four belt tiers at Factorio's speeds
  - throughput at 15 / 30 / 45 / 60 items/s, with loaders adding several entries per tick
  - the delta sync
  - belt as buffer (restored)
  - **cost per length (mandatory)**
  - obstruction
  - the two-wide splitter and merger
  - the loader's FE buffer and per-item charge on tiers 2 to 4
  - the loader's researched grab size
- **Core**: the pole asks for FE instead of GregTech's capability. That is already owed to GregTech's
  departure, and it is all the loader's power needs.
- **Postponed**: the tap.
- **Excluded**: undergrounds and lanes, argued from the medium. Swing-arm reach and the long-handed
  inserter, because there is no swing arm.
- **Moved elsewhere**: trains to Railcraft Reborn; fluids to Oritech. Item bulk storage, rung 2's
  packages and barrelling are left without an owner.

**The finding in one line:** SimpleBelts is a thinner belt than Create's, a link between two loaders.
But everything the pack needs from it is *reachable in one fork*, at Factorio's own numbers, which
Create's RPM-driven belt never was. Tiers, a known items/s, belt as buffer, a real splitter, the stack
bonus, and energy spent on moving items all move from `adapted` or `excluded` to reachable. The inserter
row stays `adapted`, now as a loader. After the rulings, **the core owes the belt layer nothing beyond
the pole's FE swap**. The pack's real cost is the version change, and within it, porting its own
three forks. Create's departure also takes rotation out of the pack, so energy becomes one currency.

## Out of scope, noted

- **The extractor.** Belt, splitter, loader and inserter prototypes have no rows in
  `data/factorio/machine.json`. Committing them is deferred; this survey reads the raw dump.
- **Oritech at 2.0.** Only fact 3 was re-read. The rest of `oritech-coverage.md`, including the
  subclassing route (fact 9) and the block-breakers (fact 7), cites 1.2.12.
- **Oritech: Space Age.** Oritech 2.0 builds a separate addon jar from `space-age/` (version `0.1.0`,
  about 2,100 lines): a Rocket Assembler, couplings, boosters, a Rocket Pad, and a 2D orbital flight
  simulation (`spaceage/simulation/SpaceSimulation.java`). It registers no dimension. The Oritech survey
  sized *Interplanetary travel* as "the heaviest core item". Whether this addon moves that row is a
  survey of its own.
- **Collaboration with Rearth.** The splitter and the tap are both generic belt features and both
  shaped as upstream PRs. Rearth's issue tracker (11 issues) and the README's planned features mention
  neither. Contacting Rearth is the user's call; the survey prices the fork so adoption doesn't wait on it.
