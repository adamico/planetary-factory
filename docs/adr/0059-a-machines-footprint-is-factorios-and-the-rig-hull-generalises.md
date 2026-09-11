---
status: accepted
supersedes: [107]
---

# A machine's footprint is Factorio's, and the rig hull generalises

Factorio's machines are told apart on the ground before they are told apart in a GUI. An Assembling
Machine is 3x3, a Chemical Plant is 3x3, an Oil Refinery is 5x5, and a player who has played
Factorio reads a factory's shape without reading a single tooltip. Every pack machine so far is one
block, which reads as a wall of identical cubes and throws away the cheapest fidelity available.

ADR-0043 already built the answer for a different reason. Terra's rigs are 2x2x2 and 3x3x3, placed
from one item, broken from any part, refused visibly where they do not fit. That is a multiblock
placed as a single block, it shipped, and it reads well.

## The rule

**A pack machine's horizontal footprint is its Factorio entity's tile size, 1:1. Its height is
authored per machine. It is placed, broken and previewed by the rig idiom, generalised.**

## The three parts, separately

**Horizontal is extracted, not chosen.** Factorio's prototypes carry the tile size and the corpus
already holds it. Assembling Machine 3x3, Chemical Plant 3x3, Oil Refinery 5x5, Lab 3x3, Boiler 3x2,
Stone Furnace 2x2.

**Height is authored, and says so.** Factorio's entities have no height — the sprites are 2D art —
so any height the pack ships is a judgement. It lives as a row per machine in
`data/pack/machine-heights.json` with its reason, read at registration the way `RigCorpus` is read.
**Not a formula.** `ceil(n/2)` would give plausible numbers and no reader could later tell whether a
3 was measured or computed, which is the kind of invented precision the corpus rule exists to
prevent (ADR-0054). A committed row with a sentence is honest about being a choice.

**The hull is shared geometry, not a base class.** What generalises out of `core/mining/rig/` is
`RigGeometry`, `RigPartBlock`/`RigPartBlockEntity`, `RigBreaker` and `RigBlockItem`'s placement
preview: a footprint, its parts, its bed-and-door breakage returning exactly one item, and a visible
refusal where it does not fit. **It is not a machine base class.** A rig part is dumb scenery
forwarding to an anchor; MI's `MachineBlockEntity` wants to *be* the block. Any attempt to make one
extend the other is a misreading of this ADR.

## Why the pack's machines are Java subclasses

ADR-0056 sold Modern Industrialization partly on `RegisterMachinesEventJS.craftingSingleBlock`
needing no Java subclass. **This ADR is why the pack subclasses anyway**, and the reason is this rule
rather than anything about recipes: a footprint needs a custom block, block item, placement predicate
and breakage behaviour, none of which that KubeJS event exposes.

(An earlier draft reached the same conclusion from `CrafterComponent.Behavior.banRecipe` needing an
override point for research locks. ADR-0058 deletes that reason entirely. The answer stands on the
footprint alone.)

## The Oil Refinery is not an MI multiblock

#107 registered it as a GregTech multiblock at `(0, 0, 2, 3)`. MI's multiblocks are shapes, casings
and hatches — a different registration path and the longest pole in the migration — and the
refinery's envelope has **no item slots at all**, so the multiblock was buying visual mass rather
than capability. Under this rule it gets the mass anyway: a 5x5 hulled single placement. Whether it
earns a real multiblock hull later is its own argument, and ADR-0025's version of it was GregTech's
idiom, which is leaving.

## Scope, and what is deferred

The rule applies to every pack machine. It is **applied in the migration to the machines being
registered anyway** — the three assembler tiers, the Chemical Plant and the Oil Refinery — because a
footprint is part of a registration: blockstate, model, loot table, placement predicate and block
item. Registering them as single blocks and widening later means authoring those five things twice.

**The shipped blocks are follow-ups**: the three furnace tiers, the Boiler, the Offshore Pump and the
Research Lab. Re-specing them touches three asset checks that have nothing to do with this
migration, and there is no reason to put them on its critical path.

## Consequences

- The flip's merge gate grows a clause: five machines place and break correctly at their real sizes.
- The model and blockstate count per machine multiplies. That churn is the reason
  `docs/testing/what-to-check.md` suspends the cross-file hop-walking for pack machines until the
  machine set settles — recorded there, with the flip's world load as the trigger.
- `core/mining/rig/` gains a seam it did not have, and the rigs are the regression risk. The
  extraction lands on main, green, before the migration branch starts.
