---
status: accepted
---

# One mod owns each capability on Terra, and the losing block is recipe-removed

Terra runs three tech mods in series on one ladder — GregTech, Create, Mekanism — plus Create: Electro
Energetics for the grid, and each of the three ships a full-stack answer to mining, moving, processing
and powering. Left alone, that is three
parallel routes to the same capability, and a Factorio-literate player finds the cheapest one and
never learns the line the quest book teaches. The spine (ADR-0018) is only a spine if
exactly one mod owns each rung.

This ADR is consulted **every time anyone adds a recipe**. The spine ADR is read once. They are
separate documents for the same reason ADR-0015 split from ADR-0014.

## The rule

**One mod owns each capability. The losing mod's block is recipe-removed by default.**

"Kept but outclassed" is an exception that must be argued per block, and only where the block is a
component of something kept or is purely decorative. A curated pack that leaves the rejected idiom
craftable has not made the choice.

## The table

| Capability | Owner | The losing blocks |
| --- | --- | --- |
| Extraction (ore) | **GregTech** | Create Mechanical Drill **cut**; Mekanism Digital Miner **cut** |
| Extraction (fluid) | **GregTech** — Fluid Drilling Rig, rung 4 | — |
| Mining automation | **GregTech** — LP Steam Miner (rung 0) → Basic Ore Drilling Rig → Advanced, one rig per rung, each granted by a science tier | as above |
| Ore processing | **Create** at rung 0 (Crushing Wheels, Millstone); **Mekanism** from rung 1 (enrichment → 5x) | GT's ore-processing line recipe-removed (ADR pending, `#37`) |
| Power generation | **Create** generates first (Steam Engine, rung 0); **Mekanism** generates at scale as FE; **Electro carries what either makes** | GT's power layer removed entire |
| Power transmission (between areas) | **Create: Electro Energetics** — poles, wire, catenary | GT cables removed with the power layer |
| Power distribution (inside an area) | **Mekanism** (Universal Cables) | — |
| Energy storage | **Electro** (Accumulator) grid-side; **Mekanism** (Energy Cube, Induction Matrix) FE-side | GT quantum batteries removed with the power layer |
| Item logistics | **Create** (belts, chutes, trains) | Mekanism Logistical Transporters recipe-removed; GT item pipes and covers removed with the power layer |
| Fluid logistics | **Mekanism** (Mechanical Pipes) | Create fluid pipes **kept** for short local runs — pumps and hand-placed, not a network |
| Bulk storage (item) | **Create** (Item Vault) | Mekanism Bins and QIO **cut**; GT quantum chests removed |
| Bulk storage (fluid) | **Mekanism** (Dynamic Tank) | GT quantum tanks removed |
| Chemistry | **Mekanism** | GT has no petrochemistry to lose; its chem line is not Terra's |
| Refining | **Mekanism**, by pack-authored recipes on machines it owns | GT Distillation Tower and Distillery **recipe-removed on Terra** |
| Assembly | **GregTech** (Assembling Machine I/II/III) | — |

GregTech is left with exactly extraction and assembly, plus the fluid rig that is extraction by
another name. Nothing was added to its share.

## The extraction ladder is GregTech's end to end

LP Steam Miner at rung 0, Basic Ore Drilling Rig, Advanced Ore Drilling Rig — one rig per rung, each
granted by a science tier. That ladder is the reason two blocks die rather than merely lose:

- **Mekanism's Digital Miner is cut, not deferred.** It is a filtered-teleport miner — the AE2
  idiom, and AE2 is endgame. It would end the extraction ladder the moment it appeared.
- **Create's Mechanical Drill is cut.** Stock Create 6.0.10 is the only Create in the pack and no
  addon extends the drill, so its one function is breaking the block in front of it on a
  contraption. It is **not** part of ore processing — Crushing Wheels and the Millstone are, and
  they are kept. That leaves the drill with no non-extraction job: a gantry or train-mounted drill
  array over a GT ore vein *is* automated ore extraction, and restricting what a block breaker may
  break needs exactly the scripted restriction GDD §5 rules out. A gantry quarry costs build effort,
  not progression, so the player pays it at rung 1 and never touches a Drilling Rig. Tunnel-boring
  for trains is the real loss; it is scenery, not a capability the spine needs.

## Almost Unified stays, restricted to raw materials

Ores, ingots, dusts, plates and gems unify. **Recipe types do not.** Unifying materials is what stops
the player holding three kinds of copper dust; unifying recipe types is what would let a Mekanism
grid recipe satisfy a GT machine step and collapse the ladder — the cross-mod shortcut GDD §5 flags
for audit.

## Transmission is Electro's, and the Converter is the boundary

The transmission row was written in pencil until Create: Electro Energetics was adopted as the pack's
**fourth mod** (`#46`). It does not displace Mekanism — it **layers above** it:

- **Poles transmit** between areas; **cables distribute** inside them.
- The **Converter is the mandatory boundary**. The grid never touches a machine directly. This is a
  teachable beat — the grid ends where the machines begin — and it makes the converter's 100 kW
  rating a real per-area budget rather than a number nobody meets.
- **Energy storage splits on the same boundary**, which is why the row above is new: the Accumulator
  stores on the grid side, Mekanism's Energy Cube and Induction Matrix on the FE side. One boundary
  rule to teach, not two.
- The grid is **granted at a science rung, not available at rung 0** — rung 0 already teaches steam,
  kinetics and Create ore processing, and SU → W → FE is a third unit hop on top of that. Which rung
  is the beat sheet's call.
- The mod runs at **shipped physics defaults**: voltage drop, per-material wire gauge, grounding,
  fuses, brownouts and component damage all on. The physics is not separable from the pole — only
  dial-able — and flattening resistance to the config floor would delete the wire-tier ladder that is
  the reason to adopt the mod at all.

The **Alternator** takes Create SU and emits watts. It is not counted as generation in the table
above, but it is a generation *system* in Factorio's sense — prime mover plus alternator, as boiler
plus steam engine is — and its distinctive value is **cross-body**: an SU-driven alternator is a power
answer on a body where sun and steam are not available. Terra does not need that; later bodies may.

**Plant Oil and Transformer Oil are kept and read as lubricant, not petroleum.** Seed oil is
renewable, so transformers are craftable at whatever rung grants the grid without borrowing rung 4's
oil chapter. The constraint is one-way: **Transformer Oil must not become an input to anything in the
oil chapter**, or the grid silently gates rung 4.

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
- **Keep the losing block craftable but outclassed.** Kept only as a per-block exception. Create's
  fluid pipes are the model: hand-placed local runs are not a network and do not compete with
  Mekanism's.
- **Give GregTech the whole stack and use Create and Mekanism as decoration.** Rejected — it inverts
  the pack's design. GregTech is instrumental (GCyR needs it, its miners are good, it is a cheap
  chassis for custom machines), not the spine.

## Consequences

- **Every new recipe is checked against this table.** A recipe that gives a mod a capability it does
  not own here is a bug, whatever it costs.
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
