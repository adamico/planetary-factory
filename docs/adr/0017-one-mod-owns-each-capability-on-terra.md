---
status: accepted
---

# One mod owns each capability on Terra, and the losing block is recipe-removed

Terra runs three tech mods in series on one ladder — GregTech, Create, Mekanism — and each of them
ships a full-stack answer to mining, moving, processing and powering. Left alone, that is three
parallel routes to the same capability, and a Factorio-literate player finds the cheapest one and
never learns the line the quest book teaches. The spine (ADR pending, `#33`) is only a spine if
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
| Power generation | **Create** first (Steam Engine, rung 0); **Mekanism** at scale as FE | GT's power layer removed entire |
| Power transmission | **Mekanism** (Universal Cables) — *written in pencil, see below* | GT cables removed with the power layer |
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

## The transmission row is written in pencil

Mekanism's Universal Cables are recorded as the owner so the beat sheet can be written, **not**
because transmission is settled. The Factorio/Satisfactory grid-as-built-object idiom is still live,
and whatever wins it would **replace** the cable rather than add a row: the candidate is
Create: Electro Energetics (`#46`), adopted as a fourth mod, with the crux being whether its voltage
simulation is separable from its poles. Until that resolves, do not write "Mekanism cables" into any
spec as though the question were closed. The spine survives either way, which is why this row blocks
nothing.

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
