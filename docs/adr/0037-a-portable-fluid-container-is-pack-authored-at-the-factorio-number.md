---
status: accepted
supersedes: [101]
---

# A portable fluid container is pack-authored, and its capacity is the Factorio number

Factorio's barrel has had no item behind it since ADR-0035. `data/pack/subgroup-owner.json` routed
the shelf to `a Mekanism portable tank`, the mod left, and the sentence was struck rather than
answered — leaving ten rows in `data/pack/item-map.json` (`barrel` plus the nine `*-barrel` fluids)
pointing at `#106` for a container nobody had named.

**The barrel is `planetaryfactory:barrel`, registered in `planetaryfactory_core` as a real
`IFluidHandlerItem`. It holds 50 mB, stacks to 10, accepts any fluid, and is filled and emptied by
Create's Spout and Item Drain with no recipes at all.**

## The capacity is not a choice

`scripts/factorio-recipe-convert.py` fixes **one Factorio fluid unit to one millibucket**, 1:1, and
every emitted recipe is built on it — `oil_refinery/advanced_oil_processing.json` is Factorio's row
unscaled, 100 mB crude and 50 mB water in, 25 / 45 / 55 out.

Factorio's barrel holds 50 units. Under the converter's rule that is **50 mB**, and no other number
is available: the reason a player barrels anything is that a barrel is a known fraction of the
recipe that consumes it. A bucket-parity 1 000 mB would be a twentyfold dose of every fluid in the
corpus riding in one item, and a barrel of crude would stop meaning anything against the refinery
run it feeds.

This is worth stating as a rule rather than as a fact about one item: **a portable fluid container's
capacity is the Factorio number under the 1:1 unit rule.** The next container the pack needs does
not get to be re-argued from Minecraft's bucket.

50 mB reads as tiny beside a bucket, and that is Factorio's design rather than an accident of the
conversion. A barrel is deliberately bad at storage — 50 units against a 25 000-unit storage tank,
1:500 — because its job is to put fluid on a belt and in a wagon, not to hold it. The pack's ratio
lands at **1:160** against `create:fluid_tank`'s 8 000 mB per block. Different number, same ordering,
and the ordering is the part that matters: no quantity of barrels is a cheaper tank.

## The stack is Factorio's ten, and Create supports it

Ten barrels per slot is 500 mB — still under one tank block even at a full vanilla stack of 64, so
stacking threatens nothing.

**Verified against the installed jar, not assumed.** `create-1.21.1-6.0.10.jar`,
`GenericItemFilling` and `GenericItemEmptying`: both `ItemStack.copy()` the held stack and
`setCount(1)` (`iconst_1`, offsets 116 and 133 respectively) before touching the fluid capability.
The Spout and the Item Drain process **one barrel out of the stack per operation**. A stackable
fluid-handler item is a supported case in Create, not something the pack is getting away with.

## The container is pack-authored because nothing installed has the shape

GregTech ships seven fluid-container items and all of them are wrong here:

- They are a **six-material ladder** — tin, steel, aluminium, stainless, titanium, tungstensteel —
  expressing a tiered progression the pack does not want, for a Factorio concept that has exactly
  one tier.
- Several back onto `ThermalFluidHandlerItemStack`, which **refuses fluids outside a temperature
  band**. Factorio's barrel carries fluoroketone hot *and* cold; a banded container would split the
  nine rows across two items and delete the one-container simplification `#93` paid for.
- Their stack size is not ours to set.

None of this costs anything to walk away from: ADR-0034's sweep already removes every recipe that is
not pack-authored on a named surface, so GT's cells are **uncraftable today**. There is no losing
block to remove and no exception to argue — the general rule is that **the container is pack-authored
when no installed mod's item has the right shape**, and ADR-0015 puts an `IFluidHandlerItem` in the
mod rather than in KubeJS because it is mechanism.

The recipe is one steel plate in, one barrel out, on `pack:assembling` — Factorio's recipe, and a
surface already in `RECIPE_SURVIVORS`. Which rung grants it is `#25`'s call, not this document's.

## Any fluid, and Factorio's restriction is not ported

Factorio bars steam and most Space Age fluids from barrels. **This pack's barrel accepts anything
with a fluid capability**, and the divergence is deliberate.

Factorio's list is a **content budget**. Its barrels are nine distinct items with eighteen recipes,
so every admitted fluid costs two of them. Ours is one NBT-carrying container with **zero** recipes
(`#93`: the Spout and the Item Drain key on `IFluidHandlerItem`, so authoring fill and empty recipes
would duplicate a mechanic that already works). The reason for the list does not survive the port.

What a filter would actually buy was checked before being dropped:

- **No gate is bypassed.** Filling requires the Spout, which is the fluid-handling rung already. An
  unfiltered barrel moves no fluid the player could not already move.
- **No capability is new.** A vanilla bucket carries lava at 1 000 mB. At 50 mB the barrel is
  *twenty times worse* than the container the player is holding — carrying molten steel by barrel is
  a punishment, not a shortcut.
- **Nothing enumerates it.** No recipes means no EMI page per fluid, so an unexpected fluid never
  surfaces anywhere the player looks.

Against that, a filter has to track the corpus forever, and its failure mode is **a barrel that
silently refuses a fluid the pack expects it to carry** — no error, no log line, reaching the player
as a machine that never fills. A recurring maintenance cost whose worst case is worse than the thing
it prevents.

## Consequences

- `data/pack/item-map.json`: the nine `*-barrel` rows keep `native_mechanic` and name the container
  instead of pointing at `#106`. The `barrel` row itself **stays `undecided` until the mod item
  exists** — flipping it makes the converter emit `assembling/barrel`, whose output item would not be
  registered, which reaches the player as a world-load error. The flip, the item and the converter
  re-run are one change, and `#106` carries it.
- `data/pack/subgroup-owner.json`: the `fill-barrel` shelf note's `THIS READ "a Mekanism portable
  tank"` strikethrough gets a live reference to this ADR.
- **ADR-0017's bulk storage (fluid) row gains the sentence it was missing** — the objection `#106`
  raised against `#101`. Create's Fluid Tank is 8 000 mB per block against Factorio's 25 000-unit
  storage tank: **three blocks are one Factorio tank**, and because Create's tank is a multiblock the
  gap is paid in build effort rather than lost capacity. The row is argued now rather than assumed.
- Two checks (`docs/testing/what-to-check.md`): capacity, stack and a lossless round-trip through the
  capability are a mod unit test needing no Minecraft; the Spout and Item Drain actually filling and
  emptying is a world load. A third — that `barrel` resolves to a registered item — needs nothing new:
  the converter's `--check` hard-fails a missing item-map row, and it is what caught the sequencing
  problem above.

## Considered Options

- **Map `barrel` to a GregTech fluid cell.** Rejected above: six tiers for a one-tier concept, a
  temperature band that splits the fluoroketone rows, and a stack size the pack does not control.
- **Bucket-parity 1 000 mB.** Rejected: it breaks the 1:1 unit rule the whole corpus is built on, and
  it is the one capacity that would let barrels compete with the tank.
- **Nine distinct filled-barrel items, as Factorio has.** Rejected by `#93` already, and its fidelity
  cost is recorded in `subgroup-owner.json`. One NBT container buys a native, free mechanic; nine
  items would buy eighteen recipes duplicating it.
- **A fluid whitelist.** Rejected above — recurring churn against a risk that does not exist, with a
  silent failure mode.
