---
status: provisional
supersedes: [27, 37]
---

# Terra's drill ladder is two pack-authored rigs, and a drill ejects onto the tile it faces

ADR-0040 authored Terra's burner drill and left GregTech the electric one. It also recorded
"drills output onto a belt directly" as `excluded`. Both are overturned here, from the same
starting point: **a Factorio player who places their first burner drill over the starting iron
expects it to feed the furnace next to it, and expects to see which blocks it is working.**

Neither of those is a belt. ADR-0040's excluded row argued the whole question about belts, and
Factorio has no belt rule — a mining drill outputs onto **the tile its arrow points at**, and a
belt, a furnace, a chest or another drill is merely what happens to be standing there. Excluding
"output onto a belt" deleted the drill-and-furnace pair as collateral, which is the first machine
pair a Factorio player builds.

## The ladder is two rigs and GregTech owns neither

ADR-0040's rung 1 was `gtceu:lv_miner`. That block cannot hold the pack's ore model. `MinerLogic`
calls `ServerLevel.setBlock(pos, cobblestone)` and takes drops from the block's **loot table** —
it deletes an ore block whole, whatever amount it held, and never routes through `OreMining`, so
the position's `OreDelta` entry is left behind for the next block placed there to inherit. It also
queues anything in `c:ores` in a 17×17 column to bedrock at 160 ticks a block, against Factorio's
5×5 at 0.5 items/s.

Making it fit means overriding its replacement block, its drop source, its per-operation
semantics, its footprint, its speed and its output — maintaining GregTech's miner as a fork until
it stops being GregTech's miner — and still hand-writing the renderer and the auto-output supplier,
because `MachineBuilder` exposes no `autoOutput*` setter and GregTech renders no working area at
all.

**So `planetaryfactory_core` authors both rigs, and GregTech owns no drill on Terra.** ADR-0040's
own principle — "the pack authors first-party when fidelity demands it" — applied one rung further
up than it was willing to go.

| Rung | Rig | Factorio source | Footprint | Mining area |
| --- | --- | --- | --- | --- |
| 0 | Burner Mining Drill | `burner-mining-drill` | 2×2 | the 2×2 beneath it |
| 1 | Electric Mining Drill | `electric-mining-drill` | 3×3 | the 5×5 beneath it |

`item-map.json`'s `electric-mining-drill` re-points from `gtceu:lv_miner` to the first-party block.
GregTech's miners are removed from the game rather than left unobtainable: under ADR-0034's sweep
they are already uncraftable, and the only thing leaving them registered buys is a creative-mode
block that silently corrupts ore deltas.

## A drill ejects onto the tile it faces

**A rig has a horizontal facing, fixed at placement, and its output goes to that one tile.** Not to
any adjacent inventory, and not downward: the arrow is the mechanic, it makes placement a decision
the player gets right or wrong, and it is what they will recognise.

The order of attempts on that tile:

1. **An item handler** — `Capabilities.ItemHandler.BLOCK`. This covers the pack's furnace, a chest,
   a vanilla hopper, and every Create block that answers the capability.
2. **Otherwise, one item on the ground.** The rig drops a single `ItemEntity` on the faced tile and
   drops no second one while that item is still there. This is Factorio's own behaviour and its
   one-item-per-tile rule, and it is also what keeps the logistics path open without a Create
   dependency: anything that picks items up off the ground is fed by it.
3. **Otherwise it stalls**, holding output in a small internal buffer and burning no fuel. A
   mis-faced drill stops rather than voiding ore — under an amount model, overflow that vanishes
   destroys a finite resource — and rather than looking like it works.

**Create's `DirectBeltInputBehaviour` is deliberately not called, and the mod takes no Create
dependency.** A bare horizontal belt does answer `Capabilities.ItemHandler.BLOCK`, so rule 1 will
feed one, and a Create funnel, which answers no item handler, is reachable only by rule 2. Both are
accepted for now because the prior question is open: whether this pack should ship Create's belts at
all, or Factorio's own. That is #178, and this ADR is not the place to answer it.

*Corrected by ADR-0044, on two counts. **The paragraph above read that rule 1 feeds a belt "including
against its flow, which `canInsertFromSide` exists to reject". That is wrong.** `canInsertFromSide` is
on the `DirectBeltInputBehaviour` path — the path the rig does not take — so it never bore on this.
On the capability path, `BeltBlockEntity.registerCapabilities` hands back a side-ignoring provider and
`ItemHandlerBeltSegment.insertItem` gates only on `canInsertAt(offset)`, which is hard-wired to
`Direction.UP`; the item then lands on the queried segment and **travels in the belt's normal
direction**. Nothing rides backwards, and inserting onto a belt's last tile and having the item leave
the end is what Factorio does too. What is genuinely worse than stated is that a **stopped** belt accepts,
since that path never consults `getSpeed()`. **The real defect is the second clause, and it is
confirmed**: no funnel class appears among the jar's capability registrations, and
`content/logistics/funnel/` references `Capabilities` nowhere — so ADR-0040's named answer to a drill
that does not push is unreachable, along with chute, depot, brass tunnel, saw, millstone, basin and
item drain. That, not the insertion direction, is why the `DirectBeltInputBehaviour` call is worth
making. **#178 is now answered** — ADR-0044 keeps Create's belts — so the call is no longer waiting on
anything.*

## Both rigs draw their overlay on the ore

A player looking at a placed rig, or holding one, sees **the top face of every ore block in its
mining area tinted**, plus the footprint the rig occupies or would occupy. The preview refuses
visibly where the rig would not fit.

This is affordable because of how Terra's ore is shaped. The starting fields are **one block thick
and flush with the topsoil** — `GroundProcessor` replaces the topmost terrain block — so the ore's
top face is already the surface the player is standing on. And `OreBlock` carries exactly one
blockstate property, `STAGE`; the amount lives in a server-side chunk attachment and
`OreMining.remaining` returns 0 off the server. **So the overlay renders from blockstate alone —
"is an `OreBlock`" is client-visible — with no packet and no block entity sync.** If the tint is
ever to vary by richness, `stage`'s eight buckets are the resolution available for free.

The overlay is not permanent. A tint on every worked patch for the rest of the game is noise, and
Factorio does not do it either.

## The mining area is the layer directly beneath, on both rigs

Factorio is two-dimensional and its drills work the tiles they stand on. The starting fields are
one block thick, so the rule is unambiguous there. For the buried outfield veins (y 20–48, 3D
blobs) it means **the player digs down and places the rig on the vein**.

The alternative — scanning the column downward, GregTech-style — is rejected on the overlay's
account: under it the rig's area contains ore the player cannot see and the renderer cannot tint,
so the overlay would show an empty area over a rich vein. It also makes veins trivial, letting a
surface-placed rig eat forty blocks of depth with no exploration, which is the opposite of what
ADR-0019's prospecting was built to reward.

Whether those buried veins should exist at all is a separate open question — #179 asks whether flat
ore discs like the starting patches are the more Factorio-faithful shape. **This rule
holds either way**, which is why the drills do not wait on it.

## Placement, breaking, and rotation

- **The footprint auto-places.** One click places all four (or nine) blocks: an anchor holding the
  block entity and parts that forward to it. The square extends away from the player, anchored by
  horizontal look direction, and **placement is refused with nothing consumed** where the square
  does not fit. A silently-consumed item on a failed 2×2 is a bad first machine.
- **Breaking any part breaks the rig** and returns exactly one drill item. The bed-and-door idiom,
  and it makes the Engineer's Pick's dismantle verb work with no special case.
- **Rotation after placement is out of scope here.** A mis-faced rig is broken and re-placed, which
  costs one Pick swing and returns the item. The rig's facing is therefore whatever the player was
  facing when they placed it. Rotation arrives as a Factorio-shaped keybind — `R` on whatever is
  under the cursor — in #180, covering every block in the pack rather than these two, and
  retiring the Engineer's Pick's `wrench_rotate` when it does. Note that a 2×2 and a 3×3 are
  rotation-invariant footprints, so rotating a rig will move its arrow and its models and never
  re-pick which ore it is working.

Plain right-click opens the rig. It has a screen because it has a fuel slot.

## Every number is extracted, including the ones that do not exist yet

ADR-0041 states the rule — "every number is extracted, and none is chosen" — and `machine.json`
does not currently satisfy it here: it holds twelve crafting machines and no mining drill.
`factorio-machine-extract.py` is widened to emit the mining-drill prototypes, so `mining_speed`,
`energy_usage`, any `drain`, and `tile_width`/`tile_height` become committed data.

That is not bookkeeping. **The 2×2 and 3×3 footprints this ADR turns on become extracted facts
rather than two integers somebody typed**, and Vulcanus's Big Mining Drill later arrives as a data
row instead of a code change.

## Fuel is Factorio's, because there is no vanilla fuel to fall back on

The burner rig burns solid fuel, and **the burn duration is the fuel's joules over the rig's
watts** — no ADR-0029 conversion is involved, since nothing here is EU. A new extractor emits
`fuel_value` and `fuel_category` per Factorio item; an item is fuel here when `item-map.json` maps
a Factorio fuel onto it, read in reverse.

The obvious objection — that charcoal, sticks and planks stop being fuel — has no force in this
pack: ADR-0034's sweep means none of them is obtainable anyway. And there is no established
practice to contradict, because **there is no furnace yet**. ADR-0040 asserted the furnace ladder
was already built and it is not, so this rig is the pack's first fuel-burning block and the model
it sets is the one the furnace will inherit.

**Fuel burns only while an operation is in progress.** A rig that is stalled, unpowered or standing
on nothing consumes nothing. If the extracted prototype declares a `drain`, the stall case is
revisited deliberately rather than inherited — a drill quietly burning coal while blocked is a leak
the player cannot see.

The electric rig takes no fuel: it is a **supply-area pole customer** under ADR-0036. Standing
inside a pole's area powers it — no wire, no connection, Factorio's own rule. ADR-0040 already
called it "the pole's second customer"; that sentence survives even though its reasoning about
GregTech wrapping FE does not.

## Consequences

- **ADR-0017 is amended again.** GregTech owns no extraction on Terra — not the burner rig
  (ADR-0040), not the electric one (here). Its fluid rig is untouched by this ADR.
- **ADR-0040's excluded sub-rule is reversed**, and its rung-1 row is superseded. Its burner-drill
  reasoning otherwise stands.
- **The ledger's Mining drills row** moves off `planned`, takes `adapted` with a notice, and its two
  `unargued` sub-rules are closed.
- **#27's "automated mining — proposed as GT's, at the first tier"** and **#37's premise that
  GregTech is in the pack partly "for its miners"** are both false as stated; both are back-linked.
- **Three tickets are filed** by this decision: #178 (Factorio belts versus Create belts), #179
  (buried veins versus flat discs) and #180 (the universal `R` rotate verb).
- **This ADR is `provisional`** under ADR-0042. Nobody has placed either rig.

## Considered alternatives

- **Keep `gtceu:lv_miner` and mixin `MinerLogic`.** Rejected above: the override list is the class.
- **Keep it as-is and accept rung 1 breaks the amount model.** Rejected — it is a second extraction
  mechanism contradicting ADR-0041 on the same blocks, and ADR-0020's objection applies verbatim:
  "the counter always wins the argument while the player believes their eyes".
- **Push to any adjacent inventory rather than one faced tile.** The friendlier Minecraft idiom, and
  the one that turns a Factorio mechanic into a hopper.
- **A single-block rig with the areas rounded to 1×1 and 5×5.** Cheapest, and it costs the burner
  drill its entire justification: ADR-0040 put it in the starting pocket because "a burner drill
  covers four tiles and beats hands even at 0.25 items/s".
- **Take the Create dependency and call `DirectBeltInputBehaviour`.** Correct in isolation — it is
  one line in `mod/build.gradle`'s `compileOnly fileTree` — and deferred only because the belt
  question above it is open.
- **A no-GUI rig**, fuelled by right-clicking with coal in hand and read through Jade. Genuinely
  tempting for a machine with one input slot whose output leaves by itself. Rejected as a saving
  rather than a choice; it would make this the only machine in the pack with no screen.
