---
status: accepted
supersedes: [90]
---

# One mod owns each capability on Terra, and the losing block is recipe-removed

Terra runs two tech mods in series on one ladder — GregTech and Create — plus Create: Electro
Energetics for the grid, and each ships a full-stack answer to mining, moving, processing
and powering. *Amended by ADR-0035: this read "three tech mods … GregTech, Create, Mekanism".
Mekanism left the pack after every row below that named it had been taken back one at a time; the
rule this ADR states is what took them. Two tech mods is the count on this ladder; ADR-0035's "three
mods plus the grid" counts GCyR, which owns rockets rather than a rung.* Left alone, that is two
parallel routes to the same capability — three, while Mekanism was here — and a Factorio-literate player finds the cheapest one and
never learns the line the quest book teaches. The spine (ADR-0018) is only a spine if
exactly one mod owns each rung.

This ADR is consulted **every time anyone adds a recipe**. The spine ADR is read once. They are
separate documents for the same reason ADR-0015 split from ADR-0014.

## The rule

**One mod owns each capability. The losing mod's block is recipe-removed by default.**

"Kept but outclassed" is an exception that must be argued per block, and only where the block is a
component of something kept or is purely decorative. A curated pack that leaves the rejected idiom
craftable has not made the choice.

**The column has three values, not two — amended by ADR-0034.** *Cut* and *—* were the only two this
table admitted, and they cannot express the case `#91` actually produced: the Electric Furnace is
**granted at rung 3**. It is not cut, and it is not freely available either. *Amended by ADR-0035:
the block was Mekanism's Energized Smelter, kept and renamed; it becomes a pack-registered machine
on a GT chassis, which `#149` builds. The grant is what survives the mod.* The third value is
**gated**: the recipe ships, and a science rung decides when.

Gated is not a softer cut. It is the *opposite* claim — a cut block is one the pack decided against,
a gated block is one the pack wants and is spending a rung on. Reading a gated row as "kept but
outclassed" files a deliberate grant as an unargued exception; reading it as cut deletes a block the
ladder is built on.

The direction that does not reverse: **gated is not kept, either.** ADR-0034 is the authority there —
a recipe reachable only past a rung is still a recipe that must be named by a decision, with its
crafting surface named too. Late is not an argument for shipping something nobody chose.

## The table

| Capability | Owner | The losing blocks |
| --- | --- | --- |
| Extraction (ore) | **GregTech** | Create Mechanical Drill **cut**; Mekanism Digital Miner **cut** |
| Extraction (fluid) | **GregTech** — Fluid Drilling Rig, **rung 2** (was rung 4; moved with the oil chapter, ADR-0025) | — |
| Mining automation | **GregTech** — LP Steam Miner (rung 0) → Basic Ore Drilling Rig → Advanced, one rig per rung, each granted by a science tier | as above |
| ~~Ore processing~~ | **Row deleted by ADR-0032** — ore smelts 1:1 and no block in the pack multiplies it, so there is no capability between extraction and smelting to own. This row read "**Create** at rung 0 (Crushing Wheels, Millstone); **Mekanism** from rung 1 (enrichment → 5x)" | Mekanism's Purification, Injection, Washer and Crystallizer **recipe-removed**; Create's and Mekanism's ore recipes removed; GT's ore-processing line was already removed (`#37`) |
| Power generation (steam, solar) | **Electro** — *amended by #101, then by #104; this row read "**Create** generates first (Steam Engine, rung 0); **Mekanism** generates at scale as FE"*. Create's Steam Engine is the prime mover and emits SU, not electricity; the **Alternator** is what makes power. Solar is Electro's outright. **#104 struck the Mekanism clause as a mistake of fact**: the pack installs base Mekanism, which registers **no generator block at all** — every generator and the Industrial Turbine live in MekanismGenerators, the jar ADR-0033 refused *for this row*. Terra's chain is four steps, not three: **GT boiler → Create Steam Engine (SU) → Electro Alternator (W) → grid** | GT's power layer removed entire. **No Mekanism generator is recipe-removed, because none exists** — the default below removes a *losing block*, and Mekanism supplies none here |
| Power generation (superheated steam) | **The pack** (ADR-0033) — a registered Nuclear Reactor and Steam Turbine on a GT chassis. Named for the fluid, not for fission: Terra's reactor and Ignus's acid neutralisation are the same row | **MekanismGenerators not adopted** — it brings six other generators onto the row above; GT's own Steam Turbines stay removed with the rest of its power layer (`#37`) |
| Power transmission (between areas) | **Create: Electro Energetics** — poles, wire, catenary | GT cables removed with the power layer |
| Power distribution (inside an area) | **The pack** — a supply-area pole (ADR-0036) — *amended by ADR-0035; this row read "**Mekanism** (Universal Cables)"* | — |
| Energy storage | **Electro** (Accumulator), grid-side, full stop — *amended by ADR-0035; this row read "grid-side; **Mekanism** (Energy Cube, Induction Matrix) FE-side". Nothing stores FE, because nothing distributes it* | GT quantum batteries removed with the power layer |
| Item logistics | **Create** (belts, chutes, trains) | Mekanism Logistical Transporters recipe-removed *(removed with the mod instead, ADR-0035)*; GT item pipes and covers removed with the power layer |
| Fluid logistics | **Create** (fluid pipes, pumps) — *amended by #101; this row read "Mekanism (Mechanical Pipes)" and kept Create's pipes as a hand-placed exception* | Mekanism Mechanical Pipes **recipe-removed** *(removed with the mod instead, ADR-0035)* |
| Bulk storage (item) | **Create** (Item Vault) | Mekanism Bins and QIO **cut** *(removed with the mod instead, ADR-0035)*; GT quantum chests removed |
| Bulk storage (fluid) | **Create** (Fluid Tank) — *amended by #101; this row read "Mekanism (Dynamic Tank)". #106 objected that #101 named "Fluid Tanks" without a block being named by anyone, and ADR-0037 argues the row: Create's tank is 8 000 mB per block against Factorio's 25 000-unit storage tank, so **three blocks are one Factorio tank** — a multiblock, so the gap is paid in build effort rather than lost capacity* | Mekanism Dynamic Tank **recipe-removed** *(removed with the mod instead, ADR-0035)*; GT quantum tanks removed |
| Oil processing | **The pack** — a registered **Oil Refinery** on a GT chassis (ADR-0025) | GT Distillation Tower and Distillery **recipe-removed on Terra**; Create: Petrochem and Create: Diesel Generators declined |
| Chemistry | **The pack** — a registered **Chemical Plant** on a GT chassis (ADR-0025), carrying Factorio's whole chemical-plant list | ~~Mekanism keeps chemistry only where the oil chapter does not reach it~~ *(ADR-0035: there is no Mekanism chemistry left to reach)*; GT's chem line is not Terra's |
| Refining | **The pack** (ADR-0025) — *this row was "Mekanism, by pack-authored recipes on machines it owns" and was written without checking that Mekanism can express the recipe. It cannot: advanced oil processing is two fluids in and three out, and no Mekanism machine has that shape.* | as above |
| Assembly | **The pack** — three registered Assembling Machines on a GT chassis (ADR-0026) | GT's stock Assembler and its whole recipe corpus removed |
| Hand-crafting surface | **The pack** — the Personal Assembler, a panel on the inventory screen (`#90`, `#95`) | vanilla Crafting Table, Crafting on a Stick and CraftingTweaks **cut**; Sophisticated Backpacks' Crafting Upgrade and AE2's terminals recipe-removed; **Create's Mechanical Crafter and Mekanism's Formulaic Assemblicator cut** — *amended by `#34`, which supersedes `#90`'s decision to keep the two executors* |

GregTech is left with exactly extraction, plus the fluid rig that is extraction by another name.
Nothing was added to its share, and **the Assembly row has since left it** (ADR-0026): the stock
Assembler carried a circuit slot, a voltage ladder and several hundred recipes the pack does not
want, and KubeJS's `GTRecipeBuilder` could not author against it. The chassis is still GregTech's;
the capability is the pack's.

**Three rows now name "the pack" rather than a mod, and that is a fourth answer this table did not
originally admit.** ADR-0025 registers an Oil Refinery and a Chemical Plant through GTCEu's KubeJS
machine builders — a GregTech *chassis*, not a GregTech capability, in the same way Launch Terminals
and Drop Hatches already are. The rule the table enforces is unchanged: exactly one owner per
capability, and the losing block recipe-removed. What changed is that for one capability the owner
is a block this pack registers, because no installed mod can express the recipe shape Factorio needs.

**Plant Oil and Transformer Oil keep their one-way rule below, and ADR-0025 adds its mirror:**
lubricant must not become an input to anything grid-side.

## The extraction ladder is GregTech's end to end

LP Steam Miner at rung 0, Basic Ore Drilling Rig, Advanced Ore Drilling Rig — one rig per rung, each
granted by a science tier. That ladder is the reason two blocks die rather than merely lose:

- **Mekanism's Digital Miner is cut, not deferred.** It is a filtered-teleport miner — the AE2
  idiom, and AE2 is endgame. It would end the extraction ladder the moment it appeared.
- **Create's Mechanical Drill is cut.** Stock Create 6.0.10 is the only Create in the pack and no
  addon extends the drill, so its one function is breaking the block in front of it on a
  contraption. It is **not** part of ore processing — *this sentence continued "Crushing Wheels and
  the Millstone are, and they are kept", which **ADR-0032 makes wrong on both halves**: there is no
  ore-processing capability any more, and those two blocks are kept for their non-ore work only.* That leaves the drill with no non-extraction job: a gantry or train-mounted drill
  array over a GT ore vein *is* automated ore extraction, and restricting what a block breaker may
  break needs exactly the scripted restriction GDD §5 rules out. A gantry quarry costs build effort,
  not progression, so the player pays it at rung 1 and never touches a Drilling Rig. Tunnel-boring
  for trains is the real loss; it is scenery, not a capability the spine needs.

## The two crafting executors are cut, not kept

`#90` cut every *manual* crafting grid and deliberately kept the two *executors* — Create's
Mechanical Crafter and Mekanism's Formulaic Assemblicator — on the reasoning that they stop being
duplicate hand-crafting and become the only surface that can run their own mods' shaped recipes.
`#34` reverses that: **neither is one of this pack's crafting machines, so neither ships.**

The consequence `#90` identified is real and is simply paid rather than avoided. Create's casings
and Mekanism's internal circuits lose their last executor, and they fall through to where every
other fluid-free `crafting` row already goes (`#88`): the Personal Assembler, and the Assembling
Machines above it. They become ordinary corpus recipes with no special beat, which is why
`docs/spec/terra-progression.md` never mentions them.

The alternative — keeping two blocks whose only remaining job is to run recipes the pack would
otherwise author itself — buys a small amount of authoring work at the price of two extra crafting
idioms the player must learn, in a pack whose whole thesis is that there is one place you craft by
hand.

## Almost Unified stays, restricted to raw materials

Ores, ingots, dusts, plates and gems unify. **Recipe types do not.** Unifying materials is what stops
the player holding three kinds of copper dust; unifying recipe types is what would let a Mekanism
grid recipe satisfy a GT machine step and collapse the ladder — the cross-mod shortcut GDD §5 flags
for audit.

## Transmission is Electro's, and the Converter is the boundary

The transmission row was written in pencil until Create: Electro Energetics was adopted as the pack's
**fourth mod** (`#46`). *Amended by ADR-0035: it was adopted layering above Mekanism's cables, which
have since left with the mod. The boundary survives the mod that motivated it — the pack's own
supply-area pole (ADR-0036) distributes inside the area, and the Converter is still where the grid
ends.*

- **Poles transmit** between areas; **cables distribute** inside them.
- The **Converter is the mandatory boundary**. The grid never touches a machine directly. This is a
  teachable beat — the grid ends where the machines begin — and it makes the converter's 100 kW
  rating a real per-area budget rather than a number nobody meets.
- **Energy storage no longer splits on that boundary** — *amended by ADR-0035; this bullet read that
  the Accumulator stores grid-side and Mekanism's Energy Cube and Induction Matrix FE-side.* Storage
  is the Accumulator's, grid-side only. FE is an endpoint format some third-party blocks speak, not a
  currency: there is no FE network and no FE storage block.
- The grid is **granted at a science rung, not available at rung 0** — rung 0 already teaches steam,
  kinetics and Create ore processing, and SU → W → FE is a third unit hop on top of that. Which rung
  is the beat sheet's call.
- The mod runs at **shipped physics defaults**: voltage drop, per-material wire gauge, grounding,
  fuses, brownouts and component damage all on. The physics is not separable from the pole — only
  dial-able — and flattening resistance to the config floor would delete the wire-tier ladder that is
  the reason to adopt the mod at all.

The **Alternator** takes Create SU and emits watts. *This paragraph used to say it was "not counted
as generation in the table above" and that its value was cross-body, on a body where sun and steam
are not available — "Terra does not need that". #101 corrects both halves: the Alternator is the
generation row, and Terra needs it from the first watt.* It is a generation *system* in Factorio's
sense — prime mover plus alternator, as boiler plus steam engine is — and the prime mover being
swappable is what makes it cross-body as well: any SU source drives it, on any body.

**Plant Oil and Transformer Oil are kept and read as lubricant, not petroleum.** Seed oil is
renewable, so transformers are craftable at whatever rung grants the grid without borrowing rung 4's
oil chapter. The constraint is one-way: **Transformer Oil must not become an input to anything in the
oil chapter**, or the grid silently gates rung 2.

## This costs the four-facts doctrine, knowingly

The decision to remove GregTech's power layer entire cut GT literacy to four facts for a
Factorio-literate, GregTech-naive audience. Running Electro at defaults installs **more** electrical
modelling than what was deleted — transformers included, the same block class GT lost.

The doctrine is therefore **spent, not exempted**: it was about the pack's total literacy, and moving
the tax between mods does not dodge it. What justifies paying it is **audience perception rather than
literacy volume**. A pack that reads as "the full GregTech tech tree" carries a reputational cost that
a pack reading as "Create plus a friendly electricity addon" does not — same complexity, different
sticker, and the sticker is what the audience reads first. Removing GT's power layer and adopting
Electro's are the same decision made twice.

## Considered Options

- **Let the mods overlap and balance the recipes instead.** Rejected: balance does not remove a
  route, and the cheapest route is the one that gets learned. Cost tuning also has to be redone every
  time any of the three mods updates.
- **Restrict the losing block by script rather than removing its recipe.** Rejected on GDD §5 — the
  pack does not restrict what a player may do with a block they hold. Removing the recipe is a
  content decision; policing the block is a rule.
- **Keep the losing block craftable but outclassed.** Kept only as a per-block exception. *The
  example this bullet used to give — Create's fluid pipes, kept for hand-placed local runs — is void
  since #101 gave Create the row outright; the exception itself stands.*
- **Give GregTech the whole stack and use Create and Mekanism as decoration.** Rejected — it inverts
  the pack's design. GregTech is instrumental (GCyR needs it, its miners are good, it is a cheap
  chassis for custom machines), not the spine.

## Amended by #93: ownership binds the block, not the recipe

The table says who owns a capability. It has been read three times as also saying which machine a
recipe is authored on, and all three readings produced the same failure — an owner named off this
table without checking that the mod can express the recipe. The Refining row is one (recorded in
this ADR already), the barrel shelf's first assignment to Mekanism is another, and the Power
generation row naming a Create Steam Engine that emits no electricity is the third. #93 is the
inverse case — a mod that *can* express the recipe, chosen for that reason, where Factorio's own
`category` names a different machine — and it forces the fork to be settled.

**Owning a capability means supplying the blocks and the mechanics the player builds and uses. It
does not mean a Factorio recipe is authored on one of that mod's machines.** Where the two come
apart, the recipe follows Factorio's `category`, which is ADR-0021's fidelity axis.

**Create is the proof case, not the exception.** After #93 it owns Item logistics on Terra with
**zero** pack-authored recipes on a Create machine, and the row is satisfied entirely by what the
player builds:

- The four forming recipes — `iron-gear-wheel`, `iron-stick`, `copper-cable`, `barrel` — moved from
  `create:pressing` to the pack's Assembling Machine. All four are `crafting` in Factorio, so the
  hand-crafting rule makes them Personal-Assembler craftable; locking them to a Mechanical Press
  meant hand-making an iron gear and then learning a second mod's rotational power to make two per
  second, at the point Factorio's ramp is smoothest, on items that feed nearly everything downstream.
- The eighteen barrel recipes are **not emitted at all, and barrelling still works**. Create's Spout
  and Item Drain need no recipe: `GenericItemFilling.canItemBeFilled` and
  `GenericItemEmptying.canItemBeEmptied` key on the item's `IFluidHandlerItem` capability, so any
  fluid-holding item is fillable and drainable. The `filling`/`emptying` recipe files Create ships are
  for items that are *not* fluid handlers. Authoring nine of each would duplicate a free mechanic;
  routing them to a machine would put one in front of a mechanic that needs none.

That second bullet is why a capability can be wholly owned with an empty recipe corpus, and it is the
strongest available evidence for the rule: the mechanic *is* the ownership.

Recorded in `data/pack/subgroup-owner.json`, which gains a `native_mechanic` process value for
in-scope capabilities that need no recipe — deliberately distinct from `not_emitted`, which means cut.
The consequence for `cross_owner` is that a crossing is the **normal** case rather than a confession:
what it records is which way the recipe went and why, so a category-following route cannot be mistaken
for a missed ownership claim.

One fidelity cost is taken knowingly: Factorio has nine distinct filled-barrel items, and one
NBT-holding container collapses them to one.

## Consequences

- **Every new recipe is checked against this table.** A recipe that gives a mod a capability it does
  not own here is a bug, whatever it costs. **But the check is on the capability, not on the machine**
  — see *Amended by #93*: which machine a Factorio recipe is authored on follows Factorio's own
  `category`, and a recipe crafted on another mod's machine is not by itself an ownership breach.
- **Recipe removal is a large, ongoing edit** across three mods, and it is where most of the pack's
  KubeJS recipe work lives. Which specific blocks earn an exception is the pre-launch cut list
  (`#28`), not this table.
- **Cutting GT's ore-processing line may orphan Assembler recipes** that wanted a GT-specific dust.
  Finding and retuning them is build-time execution, not a decision this ADR reopens.
- **The extraction ladder is load-bearing for pacing**: three of the four science rungs grant a
  mining upgrade, so weakening any rig weakens the rung that grants it.
- **This table is Terra's.** A later body may hand a capability to a different mod; that is a fresh
  decision, recorded there.
- **Create is pinned, and harder than its declared range says.** Electro requires Create
  `[6.0.7, 6.1.0)` as a `type="required"` dependency — out of range is a load-time hard fail — and its
  mixin config is `"required": true` with `defaultRequire = 1`, mixing into Create's `Carriage`,
  `CarriageContraption`, `Train`, `SubLevelAssemblyHelper`, `FluidPropagator` and `SchematicPrinter`.
  A Create **6.0.x patch** that refactors those internals satisfies the range and still aborts
  startup. The pin binds Create too: the pack cannot take a Create 6.1 for Create's own sake without
  dropping the grid. **Pinned as of this ADR: Create 6.0.10, Electro Energetics 1.1.1** — `mods` is
  gitignored, so this prose is the record. Accepted on the pack's standing diligence: **do not update
  mods unless necessary.**
- **If the grid ever breaks, it degrades rather than collapses.** The Converter is bidirectional FE at
  both ends, so removing Electro falls back to Mekanism cables everywhere with **no machine
  redesigned**. The blast radius is the aesthetic, not the spine — which is why a pin this tight was
  acceptable in the first place.
- **The block-level cut list is not decided here.** Electro registers 108 blocks and 44 items, and no
  block is cut until every block has been tested in game. "Adopt whole, cut as necessary" holds; the
  cutting waits for hands-on play.

## Amended by #101: Create owns fluid logistics

The Fluid logistics row named Mekanism's Mechanical Pipes, and kept Create's fluid pipes alive
underneath it as a per-block exception for "short local runs — pumps and hand-placed, not a network".
That is the shape this ADR exists to reject: two craftable answers to one capability, with the split
drawn on how big the run is rather than on who owns it. A player laying Create pipes for a local run
has no signal telling them where the local run ends.

**Create owns fluid handling entire — pipes and pumps for moving, tanks for storing. Mekanism's
Mechanical Pipes and Dynamic Tank are recipe-removed by default**, like every other losing block on
this table.

This is the fourth time the table has been corrected in the same direction — an owner named without
checking the shape of what it actually has to do. The Refining row, the barrel shelf, the Power
generation row (all recorded under #93 above), and now this one.

**Bulk storage (fluid) moves with it.** Create's Fluid Tank owns it and the Dynamic Tank is
recipe-removed. *ADR-0037 supplies the capacity argument this paragraph did not make, and adds the
container that ADR-0035 left vacant: Factorio's barrel is `planetaryfactory:barrel`, 50 mB, which is
1:160 against a tank block where Factorio's own ratio is 1:500 — no quantity of barrels is a cheaper
tank.* Both fluid rows are Create's, so unlike the item rows — where Create owns logistics
and Create owns bulk storage too — there is no seam here to get wrong: **Mekanism has no fluid role
on Terra at all.** It keeps power distribution, energy storage, ore processing from rung 1 and the
chemistry the oil chapter does not reach; none of those is a fluid capability.

## Amended by #101: Electro owns steam and solar, and the Steam Engine is a prime mover

The Power generation row named Create as generating first, off the Steam Engine at rung 0. #93
already caught that a Create Steam Engine **emits no electricity** and listed it as one of three rows
written without checking what the block actually does — but it recorded the error without fixing the
row.

Fixed here. **Terra's first power is a three-step chain and Electro owns it:**

    Create Steam Engine (burns fuel, emits SU) → Electro Alternator (SU in, watts out) → the grid

**Solar is Electro's outright** — panel and Accumulator, on the same grid, with no Create step in
front of it.

The three-step chain is the point rather than an inconvenience. Factorio's first power is boiler plus
steam engine, two blocks and a pipe, and the player learns that electricity is *made from* something
before it is wired. Create's SU standing between the fuel and the watts teaches the same lesson in
one more hop, and it is the hop that makes the grid a distinct thing to be granted at a rung rather
than something that arrives with the first furnace.

The Alternator paragraph above said Terra did not need it. Terra needs nothing else.

**Mekanism's clause was left untouched by this amendment, and #104 has since struck it.** *This
paragraph read: "Mekanism's at-scale FE generation is untouched by this amendment. It was on the same
row and is a different claim... If Mekanism's generators are also to go, that is its own row and its
own losing blocks."* It got its own row, and the row turned out to have no blocks in it: **base
Mekanism registers no generator**, so there was never a losing block to remove and never an at-scale
tier to argue. The phrase **"generates at scale" is retired** — it named no rung, no block and no
threshold, and a tier is named here by its blocks and its rung or not at all.

The pack's generation tiers are Nauvis's three, and none of them is Mekanism's: the four-step boiler
chain, Electro's solar and accumulators, and ADR-0033's Nuclear Reactor feeding the **Steam
Turbine** — which is the pack's only FE-side generator, fenced from the rung-0 chain by the
superheated-steam fluid rather than by the Converter.
