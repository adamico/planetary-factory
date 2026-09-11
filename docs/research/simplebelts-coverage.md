# What SimpleBelts could carry: belts and inserters without Create

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
`docs/factorio-mechanics.md` that Create carries today, and at what cost?

## The hypothesis being surveyed

This survey sits **on top of** `oritech-coverage.md`'s hypothesis and replaces that hypothesis's
logistics row. There is one scenario, called S2 here:

| role | mod |
| --- | --- |
| belts | SimpleBelts (a fork) |
| inserters, splitters | `planetaryfactory_core` |
| trains | Railcraft Reborn |
| machines, fluids | Oritech |
| everything else | `planetaryfactory_core` |

**Leaves:** Create entirely. Create: Power Grid, GCyR, Modern Industrialization and GregTech were already gone.
**Minecraft version:** 26.1.2. That version is only reachable because Create leaves: Create has no
26.1.2 build, so keeping it for trains would pin the pack to 1.21.1.

## Method

**Levels.** Each row gets the cheapest level that reaches every number in it. SimpleBelts has **no
Native level** (fact 2), so there are three:

1. **KubeJS**: recipes and the sweep, with no Java.
2. **Fork**: an edit to the pack's fork of SimpleBelts. The fork is about 2,400 lines of Java. The
   pack already maintains three forks (GCyR, Researchd, Respoiled). A change that belongs upstream is
   also shaped as a pull request to Rearth, but adoption never depends on one being accepted.
3. **Core**: a block in `planetaryfactory_core` that owes SimpleBelts nothing but the item capability
   both of them speak.

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

### 1. A SimpleBelts belt joins two inventories, and its ends are loaders

The mod registers three blocks and two belt items (`ItemContent.java:16-19`):

- the **chute**
- the **conveyor support**
- **`belt`**, tier 1
- **`improved_belt`**, tier 2

Right-clicking a belt item on one chute and then on another makes a belt between them. Supports
along the way shape the spline.

The state lives on the source chute: a `target`, a list of `midPoints`, and a `Deque<BeltItem>`
of items in transit (`blocks/ChuteBlockEntity.java:53-58`). A chute is *either* a source *or* a
target. `BeltItem#useOnBlock` refuses a chute that `isUsed()`.

This has consequences:

- **Nothing gets on or off a belt between its ends.** There is no side-loading, no merging and no
  splitting.
- **A belt cannot feed a belt directly.** Between two belts there has to be a block that exposes
  the item capability.
- **A source chute pulls by itself.** Every `getExtractionInterval()` ticks it takes the first stack
  from the inventory behind it that passes the filter, up to 64 items (`:215-238`). A target chute
  pushes the whole stack into the inventory behind it, all or nothing (`:195-205`).

So each end of a belt is a **loader**. Factorio has loaders: `loader`, `fast-loader`,
`express-loader` and `turbo-loader` are in the dump, at the belt speeds. **All four are
`hidden: true`**, so a Factorio player never gets one. This is the one mechanic SimpleBelts adds that
Factorio withholds. See the open question at the end.

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
`@ModifyConstant`, but the tap needs new state and a new API on `ChuteBlockEntity`. That is past what
a mixin sensibly does, and a mixin into a mod labelled `exp` breaks on every version bump.

**As shipped, a belt's throughput is set by the chute, not by the belt.** A tier-1 chute grabs every
26 ticks, 0.77 times a second. One grab can be anywhere from 1 to 64 items, so a tier-1 belt carries
somewhere between **0.77 and 49 items/s**, depending on what the source happened to hold. Tier 2 is
double. The ledger's throughput sub-rule makes the same complaint about Create, which bounded
entries-per-second and left items-per-entry free. Here both are free, and both are one-line edits.

### 3. The chute speaks NeoForge's transfer API, and Oritech 2.0 fences its machines

`neoforge/.../NeoforgeItemApiImpl.java` wraps `level.getCapability(Capabilities.Item.BLOCK, …)`, which
returns a `ResourceHandler<ItemResource>` from NeoForge's transfer API with `Transaction`s. So a chute
works against **any** block that answers that capability. It needs no compatibility code per mod.

Oritech 2.0 answers it on every machine tagged `@AssignSidedInventory`
(`init/BlockEntitiesContent.java:308-312`), through `MachineBlockEntity#getItemLookup`. The default
lookup is `InOutInventoryStorage#getExternalAccess()`, which **accepts insertion only into input
slots and allows extraction only from output slots** (`api/transfer/item/InOutInventoryStorage.java:27-58`).
In the machine's `SIDED` input mode, `UP` is input-only and `DOWN` is output-only
(`MachineBlockEntity.java:566-600`).

So a source chute behind an Oritech machine takes products and never inputs, and a target chute feeds
inputs and never outputs. **Native, with no fork.** The chute's slot walk (`:224-237`) reads every slot
but extracts through the fenced handler, so an input slot it passes over returns 0 and it moves on.

### 4. The tap is a change to the data structure, not a hook

Items on the belt all advance by the same `progressDelta` each tick (`:168-212`). Spacing is enforced
only **in the queue at the target end**, where waiting items stack up `ITEM_QUEUE_SPACING` apart.
In transit, items are only as far apart as the extraction interval happened to leave them.

A tap therefore needs three things:

1. an ordered list in place of the `Deque`, so an item can go in at any progress and not only at the head
2. a gap check on insertion, so items stay `1/8` block apart at Factorio's density (fact 6)
3. a mapping from a world position to progress along the spline, so an inserter standing beside the
   belt knows which progress it is reaching into

`BeltData` already holds the spline's segment points and lengths (`record BeltData`, `:422`). `collision/BeltCollisionRegistry.java` already samples the spline every `SAMPLE_LENGTH = 0.2`
blocks for entity contact, and that sampling is the same walk a position lookup needs.

**The upstream PR shape** is two public methods on `ChuteBlockEntity`: *insert at progress `p` if the
gap allows* and *extract the nearest item within `ε` of `p`*, plus a public position→progress lookup.
It adds no gameplay of its own, which is what makes it plausible for Rearth to accept. It does
not depend on the inserter.

### 5. Every tick of movement resends the whole belt

`moveItemsOnBelt` sets `networkDirty` whenever any item moves (`:186-187`), and `tick` then calls
`sendBlockUpdated` (`:118-121`). `getUpdateTag` is `saveCustomOnly` (`:300-303`), which writes the
**whole** `moving` list (`:264-270`).

At today's density that costs little. At Factorio's density (fact 6), a 64-block yellow belt holds
512 entries, and that is **512 serialised stacks per belt per tick**. The fork has to send only
deltas, or send insertions and removals and let the client simulate movement from the known speed.
The renderer already interpolates through `lastRenderedPositions`, so it tolerates the second.
How many belts a base can run is `world-load (human)` either way.

### 6. Factorio's belt numbers, and what they become

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

`transport-belt`'s recipe is 1 `iron-plate` + 1 `iron-gear-wheel` → **2**. So Factorio's own price is
half a craft per tile.

### 7. With no Create, there is no rotation

The pack's only rotational unit (SU) came from Create. Under S2 nothing in the pack consumes or
produces rotation, so the logistics and energy boundary in `oritech-coverage.md` collapses:

- **Option (a), the FE→SU bridge:** void. There is nothing to drive.
- **Option (b), two currencies:** void.
- **Option (c), fluids to Oritech:** forced, since Create's pipes leave.

**The pack has one energy currency, FE.** The joules-per-FE constant (that survey's fact 1) becomes a
prerequisite of the inserter too (fact 8 below).

### 8. The core already speaks the old capability API

NeoForge 26.1 replaced `IItemHandler`, `IFluidHandler` and `IEnergyStorage` with the transfer API
that SimpleBelts 2.0 and Oritech 2.0 already use (fact 3). Ten classes in `mod/src/main/java` touch
the old API:

- `PFBlockEntities`
- `PFItems`
- `energy/PoleEnergyStorage`
- `fluid/BoilerBlockEntity`, `fluid/BoilerItemHandler`, `fluid/OffshorePumpBlockEntity`
- `mining/rig/RigBlockEntity`, `mining/rig/RigItemHandler`
- `smelting/FurnaceItemHandler`, `smelting/FurnaceSlots`

The core has **no Create compile dependency**. `RigBlockEntity:289-293` and `RigSlots:51` mention
`DirectBeltInputBehaviour` only in comments that say why it isn't called. So Create's departure
costs the core no code. The version change does.

---

## The matrix: Transport belts

Ledger verdicts are today's, with Create. **Level** is the cheapest level that closes every gap in the row.

| sub-rule | ledger today | SimpleBelts 2.0 as shipped | level | gap, and what closes it |
| --- | --- | --- | --- | --- |
| **Three belt tiers** | `adapted`: one belt, speed bought with RPM | two tiers, `belt` and `improved_belt` (`beltTier` 1 and 2) | **Fork** + **KubeJS** | `MAX_BELT_TIER` 2 → 4, and two more `BeltItem` registrations. Each tier's multiplier is replaced by Factorio's tiles/s (fact 6), because the tiers are 1× / 2× / 3× / 4× of 1.875, which is a clean multiplier anyway. Recipes are emitted at the corpus's costs onto the Assembler surface, and SimpleBelts' own (`dried_kelp` + `stick` → 8, `recipe/belt_item*.json`) go to ADR-0034's sweep. `logistics-2`, `logistics-3` and `turbo-transport-belt` **buy something again**, and drop off #25's prune list. The row would become Factorio's own. |
| **Throughput as a ratio budget** | `planned`: target deferred to play, dials named on Create | throughput is set by the chute, 0.77 to 49 items/s at tier 1 (fact 2) | **Fork** | Speed at tiles/s and spacing at `1/8` block, one item per entry, which clamps the literal 64 at `:228`. The belt then carries exactly 15 / 30 / 45 / 60 items/s. The number is **known and computable**, which is what the sub-rule asked for. It also carries the sync rewrite (fact 5), because the density is what makes the sync expensive. |
| **Underground belts** | `excluded`: the weaving problem is 2D | none | **unchanged** | The argument from the medium holds with more force: a spline goes over whatever it has to cross, by construction. With obstruction on (below), it still can't go *through* a belt. |
| **Splitters, with filtering and priority** | `adapted`: Brass Tunnel's `SelectionMode` | none; a chest with two source chutes splits whichever grabs first | **Core** | A core **Splitter** block that exposes the item capability. One belt ends into it through a target chute, and up to two belts leave it from source chutes. It routes by Factorio's rules: 1:1 alternation, input and output priority, and one filter. It needs **no fork**, because chutes already treat any capability as an inventory (fact 3). Mergers are the same block run in reverse. Speed per tier follows `splitter` / `fast-splitter` / … in fact 6. The recipe is corpus (`splitter`: 5 `electronic-circuit`, 5 `iron-plate`, 4 `transport-belt`). This closes the ledger's **constructed-balancer** gap, which Create's one-block outcome never did, because splitters can now be chained into a balancer. Whether priority behaves as Factorio's does when a chute is the one grabbing is `world-load (human)`. |
| **Two lanes per belt** | `excluded` | one lane | **unchanged** | The belt carries the whole belt's throughput (fact 6). |
| **Belt as buffer** | `excluded`: 64 items per 64 blocks against Factorio's 512 | a queue at the target end, `0.8`-block spacing | **Fork**, restored | At 8 items per block, the density throughput needs anyway, a 64-block belt holds **512**, which is Factorio's number. The ledger's stated reason for excluding the idiom disappears, and the fork that fixes throughput restores it with no further work. |
| **Output onto a moving belt with no intermediate block** (Mining drills, `shipped`) | the rig pushes into a bare Create belt's handler | a chute has no handler, so the rig's push (`RigBlockEntity#push`) finds nothing | **Native** | Reached the other way round, the way the ledger already describes for Create's funnel: a source chute set against the rig pulls through `RigItemHandler`. No code is needed. The chute has to sit against a position that answers the capability, and whether every hull position does under ADR-0059 is `world-load (human)`. |
| *added:* **The tap** | free with Create | none (fact 1) | **Fork**, PR-shaped | Fact 4: an ordered list, a gap check, and position→progress lookup. It is the precondition for the inserter's belt pickup and drop. The largest single edit in this survey. |
| *added:* **Cost per length** | Create charges one belt item per segment | one belt item for any length (`BeltItem#createBelt`, `stack.shrink(1)` at `:160`) | **Fork** | `BeltData.totalLength()` is known at placement. Consume `ceil(length)` belt items, which is Factorio's one per tile, and refuse the belt if the player holds fewer. The length stays uncapped, and the cost is what limits it. |
| *added:* **Obstruction** | Create belts are blocks | nothing checked: `BeltItem`'s only refusals are a chute already in use and a duplicate support (`items/BeltItem.java:69,105`); the endpoints only need to be replaceable (`:124,174,183`) | **Fork** | At placement, sample the spline every `SAMPLE_LENGTH` (the walk `BeltCollisionRegistry` already does) and refuse on a solid block or on another belt's registered segment. The curve through open air stays, and it is the one thing SimpleBelts does better than a Factorio belt. |

**What survives natively:** the belt backs up visibly at the target end (`outputQueue`, drawn with
spacing). That is the compression diagnostic, a belt that shows where the slow machine is. Filtering
at a source chute is one item or an FTB Filter System filter (`:240-250`), and FTB Filter System has a
26.1.2 build in pf2612.

## The matrix: Inserters

SimpleBelts has no inserter. The row goes to **Core**: Factorio's own entity, at the dump's numbers.
It reaches a belt through the tap.

| prototype | `rotation_speed` (rev/tick) | pickup / insert (tiles) | energy per movement / rotation | drain | notes |
| --- | --- | --- | --- | --- | --- |
| `burner-inserter` | 0.013 | 1 / 1.2 | 50 kJ / 50 kJ | burner, `chemical` | fuel through the pack's fuel table (ADR-0047). `FuelBuffer` exists. |
| `inserter` | 0.014 | 1 / 1.2 | 5 kJ / 5 kJ | 0.4 kW | |
| `long-handed-inserter` | 0.02 | 2 / 2.2 | 5 kJ / 5 kJ | 0.4 kW | |
| `fast-inserter` | 0.04 | 1 / 1.2 | 7 kJ / 7 kJ | 0.5 kW | |
| `bulk-inserter` | 0.04 | 1 / 1.2 | 20 kJ / 20 kJ | 1 kW | `bulk: true` |
| `stack-inserter` | 0.04 | 1 / 1.2 | 40 kJ / 40 kJ | 1 kW | Space Age. `stack_size_bonus` 4, `wait_for_full_hand`, `grab_less_to_match_belt_stack` |

All six have `filter_count` 5. **The items/s is not in the table on purpose.** `rotation_speed` gives
one full revolution every `1/rotation_speed` ticks, which is an upper bound on swings. The actual
rate also depends on `extension_speed`, pickup timing and hand size, and it has to be simulated, not
read off. Deriving it is the inserter's own unit test. Nobody should transcribe a wiki figure.

| sub-rule | ledger today | level | reading |
| --- | --- | --- | --- |
| **The inserter as an entity** | `adapted`: Create funnels and arms, #102 open on the Arm | **Core** | A block entity that swings between a pickup and an insert position, per the table. Against an inventory it uses the item capability. Against a belt it uses the tap. The electric tiers draw FE, which is conditional on the joules-per-FE constant. **#102's question dissolves**: the Arm leaves with Create, and the row becomes Factorio's entity rather than a stand-in for one. |
| **Swing-arm reach across a belt** | lost: "no swing-arm reach across a belt" | **Core** | Pickup at 1 tile and insert at 1.2 (2 and 2.2 for long-handed) is one block either side, or two. Reaching *into* a belt needs the tap. |
| **Long-handed tier** | lost | **Core** | A row in the table. The recipe is in the corpus, unlocked by `automation`. |
| **Stack-size bonus research** | lost | **Core** + research | In the full dump, `inserter-capacity-bonus-1`…`-7` grant `bulk-inserter-capacity-bonus` and, at `-2` and `-7`, `inserter-stack-size-bonus`. `belt-stack-size-bonus` is granted by **three** technologies: `stack-inserter`, `transport-belt-capacity-1` and `-2`. ADR-0044 says "exactly one". That was true of the pruned corpus, not of the dump. Belt stacking would mean relaxing the fork's one-item-per-entry clamp to the researched bonus. |
| **Visuals** | — | **free** or **Core asset** | Oritech 2.0 has no swing-arm model. `assets/oritech/geckolib/models/` holds machines, armour and tools. The nearest is `enderic_laser.geo.json`, an arm on a base that swivels like a turret. It could be rebound through the route `oritech-coverage.md` fact 9 describes (a `GeoBlockEntity` naming Oritech's model path; not re-verified at 2.0). Whether a turret reads as an inserter is `world-load (human)`. The alternative is an authored GeckoLib model. GeckoLib 5.5.2 is in pf2612. |
| **Collaboration** | — | outward, not priced | Rearth's issue tracker (11 issues) and the README's planned features mention neither inserters nor taps. Two things could be offered upstream: the tap API (fact 4), which is small and generic, and, beyond that, the inserter itself. Contacting Rearth is the user's call. The survey prices the fork so that adoption does not wait on it. |

---

## Create's other rows under S2

These are not SimpleBelts rows, and they are **not sized** here. They are listed so that nothing
Create carries today leaves without a recorded owner.

| row (ADR-0017's table, or the ledger) | Create carries today | under S2 |
| --- | --- | --- |
| **Trains** (ledger `planned`; outfield patches are "reached by rail", ADR-0045) | Create trains | **not SimpleBelts → Railcraft Reborn**. Its repo has a `26.1.x` branch. It is not yet in pf2612. |
| **Fluid logistics** | pipes, pumps | **not SimpleBelts → Oritech** pipes (`oritech-coverage.md` option (c), now forced). The Offshore Pump and Boiler move to the transfer API (fact 8). |
| **Bulk storage (fluid)** | Fluid Tank, three blocks to one Factorio tank (ADR-0037) | **not SimpleBelts → Oritech** or core. Unsized. |
| **Bulk storage (item)** | Item Vault | **unowned**. Factorio's chests are `containers` in `machine.json`, so a core block, or vanilla chests at the corpus's slot counts. Unsized. |
| **Package logistics** (ADR-0018 rung 2's "movement at scale") | Create 6 packages | **unowned.** Rung 2 keeps the oil chapter and loses its logistics clause. What `logistic` science buys at rung 2 is #25's call. |
| **Barrelling** | the Spout fills any fluid-holding item, so the 18 barrel recipes are not emitted (ADR-0017) | **unowned.** The 18 barrel recipes need a surface, most likely an Oritech machine that takes fluid. |
| **The Create kinetic recipe line** (`create-recipe-convert.py`, `data/pack/create-substitutions.json`, `test_create_recipes.py`) | shafts, cogwheels, gearboxes, water wheels | **deleted**, along with its converter, its check and its subtree under `recipe/assembling/create/`. |
| **Splitters, inserters** | Brass Tunnel, Mechanical Arm | **core** (the matrices above) |

---

## What 26.1.2 still costs

What pf2612 already runs is **proven to load together**. The rest is what adoption would still have to prove or build.

| item | in pf2612 | status |
| --- | --- | --- |
| Oritech | 2.0.0-exp6 | loads. **A pre-release**; the Oritech survey's citations are at 1.2.12. |
| SimpleBelts | 2.0.0-exp1 | loads. **A pre-release.** The fork would be taken from here. |
| Building Gadgets 2 | 1.4.6 | loads. It keeps the *Construction robots and blueprints* row. |
| FTB Filter System | 26.1.2.2 | loads. The chute's filter integration works with it. |
| KubeJS / Rhino | 8.0.6 | loads. ADR-0023's pin was GTCEu's, and it goes with GregTech. |
| JEI, Jade, Block Runner, Architectury, GeckoLib | yes | load |
| **EMI** | `emi-unofficial-port-unstable` 1.1.24 | **loads, unofficially.** The Personal Assembler depends on EMI's Fill Recipe reaching its panel, so an unstable EMI is a risk to the hand-crafting surface, not just to recipe viewing. |
| Railcraft Reborn | no | upstream `26.1.x` branch exists. Untried. |
| FTB Quests | no | upstream `main` is 26.1.2. Untried. |
| AE2, Sophisticated Backpacks | no | Modrinth lists 26.1.2 builds. Untried. |
| Almost Unified | no | not found on Modrinth or GitHub for 26.1.2. **Unverified**, not absent. |
| **`planetaryfactory_core`** | no | ours. 16,800 lines. Ten classes on the old capability API (fact 8), plus Minecraft 26.1's renames across everything else. |
| **Researchd** (fork) | no | ours to port. Upstream `Porting-Dead-Mods/Researchd` `main` is 1.21.1. |
| **Respoiled** (fork) | no | ours to port. `main` is 1.21.1. |

**The three forks are the real cost.** Everything else is "add it to pf2612 and launch".

---

## Tally

- **Native**: the chute against Oritech machines (fact 3), and the rig's output onto a belt (by pulling).
- **KubeJS**: belt, splitter and inserter recipes at the corpus's costs, and sweeping SimpleBelts' own.
- **Fork**: four belt tiers at Factorio's speeds, throughput at 15 / 30 / 45 / 60 items/s, the
  delta sync, belt as buffer (restored), the tap, cost per length, and obstruction.
- **Core**: the inserter family (six prototypes), the splitter and merger, and the inserter's model if
  `enderic_laser` doesn't read.
- **Unchanged**: undergrounds and lanes stay `excluded`, argued from the medium.
- **Moved elsewhere**: trains to Railcraft Reborn; fluids to Oritech. Item bulk storage, rung 2's
  packages and barrelling are left without an owner.

**The finding in one line:** SimpleBelts is a thinner belt than Create's, a link between two
inventories with loaders at both ends. But everything it lacks is *reachable*, and reachable at
Factorio's own numbers, which Create's RPM-driven belt never was. Tiers, a known items/s, belt as
buffer, reach, long-handed inserters and the stack bonus all move from `adapted` or `excluded` to
reachable. The price is a fork whose largest edit is the tap, a core inserter and splitter, and
a version change whose real cost is porting the pack's own three forks. Create's departure also takes
rotation out of the pack, so energy becomes one currency.

## Open, and not decided in the grilling

- **Do belt ends stay loaders?** Factorio hides its loaders (fact 1). With a core inserter in place,
  a faithful build would make chutes **passive**: a target chute that only accepts and a source chute
  that only offers, with inserters doing all the loading. The alternative keeps chutes as loaders and
  records a notice: "belt ends load and unload themselves, which Factorio does not let you build".
  Passive chutes are a small fork edit (turn off `loadItemsOnBelt` and the push at the end). Loader
  chutes make every inserter optional wherever a belt meets an inventory, and that changes how much
  the inserter row matters in play.

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
