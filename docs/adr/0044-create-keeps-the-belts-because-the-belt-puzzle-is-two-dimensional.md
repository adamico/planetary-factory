---
status: provisional
---

# Create keeps the belts, because the puzzle they carry is two-dimensional

`#178` asked the question ADR-0043 carved out and refused to answer: does this pack ship Create's
belts, or author Factorio's own — transport belt, underground belt, splitter, and the inserter
family, at Factorio's throughputs?

**Create keeps them.** Not because authoring belts is expensive, though it is, and not because
ADR-0017 already gave logistics to Create, which is an argument from precedent rather than from the
mechanic. The reason is that **most of what makes Factorio's belt a puzzle is a consequence of
Factorio being flat**, and this pack is not.

## The argument that decided it

Factorio's belt carries four separable puzzles. Two of them exist only in two dimensions:

- **Underground belts** are a *weaving* problem — getting two lanes past each other inside a fixed
  footprint, when you cannot go over or under. In three dimensions that problem dissolves: a belt
  that needs to cross another one goes up. Create already ships sloped belt runs, so the pack has
  the answer to the 2D problem *and* an answer Factorio cannot express.
- **Two lanes per belt** is the same story one level down. Lane balancing is a compression trick for
  a conveyor one tile wide on a plane. It is not a general logistics idea; it is what you do when
  the only free axis is the one running along the belt.

So the two mechanics Create is worst at are the two the medium already deleted. What replaces them
— routing in Y — is a mechanic **Create ships and Factorio has no reference for**, which means
authoring first-party belts would not be reproducing Factorio's belt. It would be designing a 3D
belt from scratch, with Factorio offering nothing to be faithful to, in exchange for losing the one
Create already has.

That is the whole decision. The cost argument and the ADR-0017 precedent both point the same way and
neither was needed.

## What the pack was actually promising

`CONTEXT.md` opened by claiming the pack reproduces the *"progression, logistics puzzles and
interplanetary scope"* of Space Age, while this ledger's `Transport belts` row conceded that *"none
of the routing patterns a Factorio player has memorised transfer."* Both statements were tracked and
current, and they contradicted each other. `#178` was, underneath, a question about which one was
wrong.

The sentence was. **"Logistics puzzle" here means the production-chain routing problem — what feeds
what, at what ratio, over what distance — and explicitly not the belt-lane micro-puzzle.** That
reading is honest rather than convenient: ratios live in the recipes and in machine durations
(ADR-0029) and survive on any conveyance, whereas lane balancing does not survive and never could
have. `CONTEXT.md` now says `production-chain routing` and carries the term in its Language section.

## The four claims the belt makes, and where each landed

`#178` was unanswerable for as long as "Factorio's belts" was treated as one thing. It is four:

| Claim | Verdict |
| --- | --- |
| **Throughput as a budget** — a belt carries N items/s and the build must fit under it | `planned`, deferred to play — see below |
| **The belt as a spatial constraint** — the factory's shape follows what you can route | Kept. `maxBeltLength` raised 20 → 64 |
| **Balancers** — even distribution and priority across outputs | Already served, and the ledger was wrong to say otherwise |
| **Compression as a diagnostic** — a backed-up belt is how you find the slow machine | Already served |

**Balancers were the ledger's factual error.** The `Splitters` sub-rule said the balancer *"is not
buildable, and there is no output priority"*. Brass Tunnel's `SelectionMode` has seven values —
`SPLIT`, `FORCED_SPLIT`, `ROUND_ROBIN`, `FORCED_ROUND_ROBIN`, `PREFER_NEAREST`, `RANDOMIZE`,
`SYNCHRONIZE` — where the `FORCED_*` pair refuses to distribute unless every target can take its
share, which *is* a balancer, and `PREFER_NEAREST` is positional priority. There is also one filter
slot per output. What Create genuinely lacks is **building a balancer out of splitter pairs** — the
constructed pattern rather than the outcome — and the outcome is what this pack is promising.

**Compression survives** because `canInsertAtFromSide` refuses when the target segment is occupied
and a merging belt simply stalls, giving the through-line implicit priority. A backed-up belt is
visible, which is the diagnostic.

## Throughput is deferred to play, and the dials are named here

Create's belt moves `getSpeed() / 480` blocks per tick — **`RPM / 24` blocks per second**, capped by
`maxRotationSpeed = 256` at 10.67 b/s. A belt entry is a `TransportedItemStack` holding up to 64
items, and entries are held one block apart by a hard `spacing = 1` in `BeltInventory.tick()`.

Both games have a stacked and an unstacked regime, so the naive comparison is wrong in both
directions: a rig handing over one item at a time produces one-item entries, and Factorio 2.0's bulk
inserters and Big Mining Drill stack onto belts too. **The real difference is that Factorio's belt
has a known items/s *and* a bounded, researched stack multiplier — two constants, which is why a
ratio is computable — while Create's entries/s is known and its items-per-entry is whatever the
upstream inserter happened to hand over, unbounded to 64 and surfaced nowhere.** One factor is
designable; the other is not.

Factorio's own belt stacking is no help as a model: `belt-stack-size-bonus` is granted by exactly one
technology in the corpus, `stack-inserter`, at **+1** — Space Age, behind `carbon-fiber`,
`production-science-pack`, `utility-science-pack` and `bulk-inserter`, costing 1000 packs of all
seven science types. It is deep post-launch, nowhere near Terra, and mapping a stack ladder onto the
belt researches would be inventing a house rule rather than reproducing a mechanic.

**So this is not decided before play.** With single-item entries the belt does `RPM/24` items/s, and
Factorio's tiers — 15 / 30 / 45 / 60 items/s — land at 360 / 720 / 1080 / 1440 RPM. Whether the pack
needs to reach those numbers is a question about whether the belt *feels* like a constraint, and
ADR-0042 says an answer written before play is provisional. The value of deciding it now is low; the
value of **locating the dials** is high, so they are recorded:

- **The play-test.** In a rig → belt → furnace chain, is the belt a constraint the player designs
  around, or does it disappear as a consideration? The observation is whether a single belt ever
  bottlenecks a production line before the machines do.
- **First dial — belt items/s.** `BeltBlockEntity.getBeltMovementSpeed()`, the `getSpeed() / 480f`
  divisor. A mixin here changes belt speed per RPM and leaves the rest of the kinetic network alone.
- **Second dial — the global ceiling.** `maxRotationSpeed`, if it is the kinetics cap that binds
  rather than the belt. Reached for only after the first, because it speeds up every Create machine
  as a side effect.
- **If entries must be bounded to one item**, there are three intervention points and no public
  hook: `BeltBlockEntity#tryInsertingFromSide`, which all seventeen external insertion routes funnel
  through; `ItemHandlerBeltSegment#insertItem` for the NeoForge capability path that bypasses it; and
  `BeltInventory#insert` as a splitter for in-place processing outputs, where clamping would void
  items because that method has no remainder channel. `BeltInventory` never merges entries, so a
  clamp at insertion is not defeated after the fact.

**Rescaling was considered and is foreclosed.** Restating Factorio's belt numbers at some pack-native
scale would break every ratio against machines that are already faithful, because ADR-0029 gives
machine durations Factorio's seconds unmodified.

## `maxBeltLength` goes to 64

The pack capped a belt run at 20 blocks; it is now **64 — four chunks**. Factorio's belts are
unbounded, and a 20-block cap made the spatial constraint a fact about Create's chaining idiom
rather than about factory layout.

This costs nothing, and for a fixed distance it *saves*. Non-controller belt segments early-return
from `tick()`; only the controller runs `BeltInventory.tick()`, which is a single pass over the item
list with no segment sweep, so per-tick cost is proportional to items carried, not to run length.
`maxBeltLength` is read in three places, all placement-time. Two chained 20-block belts pay a
handoff for **every item crossing the seam** — an O(items) scan of the receiving belt plus two full
inventory re-syncs — which one 64-block belt does not pay at all. The config's only floor is 5, it
has no ceiling, and the sole hard limit anywhere is a 1000-segment safety counter in `BeltBlock`'s
chain walk.

Because the config is checked only when a belt is connected, raising it leaves placed belts alone and
lowering it later breaks nothing already built. It is a tuning dial, not a commitment.

## What this ADR does not decide

**The inserter.** `#178` scoped "the inserter family" alongside the belts, and folding the two
together is a large part of why it stayed open. The conveyance is settled here; whether Create's
Mechanical Arm is Factorio's inserter is `#102`, which this ADR unblocks rather than answers. For the
record, the Arm reaches 5 blocks against an inserter's 1 (2 long-handed), moves up to a full stack per
cycle at roughly 2–2.5 transfers/s at maximum RPM, and does **not** implement
`DirectBeltInputBehaviour` — it uses the separate `ArmInteractionPointType` registry.

**Belt-as-buffer** is knowingly dropped. A 64-block belt at one item per block buffers 64 items where
a 64-tile yellow belt buffers 512. Using belts as storage is a real Factorio idiom and it is not among
the four claims above.

## Consequences

- **ADR-0017's `Item logistics` row is re-affirmed, now argued from the mechanic** rather than from
  the cut-list rule. The row does not change; its justification does.
- **ADR-0018 keeps both its Create clauses.** Rung 0's *"Create's belts"* and rung 2's *"Movement at
  scale — Create 6 package logistics"* stand. Rung 2 remains the one place the pack spends Factorio's
  `logistic` science on something categorically unlike what Factorio spends it on, and that is left
  as `#25`'s call, not re-opened here.
- **`logistics-2`, `logistics-3` and `turbo-transport-belt` still buy nothing** and remain candidates
  for `#25`'s prune. They unlock fast/express/turbo belts, undergrounds and splitters; all are
  excluded or bought with RPM. This ADR considered giving them a stack-size ladder to buy and
  rejected it as a house rule.
- **ADR-0043's stated reason for not calling `DirectBeltInputBehaviour` was wrong on the facts** and
  is corrected there. The insertion path is not the defect; the funnel is.
- **The mod takes a `compileOnly` Create dependency** when the `DirectBeltInputBehaviour` work lands.
  That is a build-config line alongside the five the mod already carries, and it commits nothing at
  runtime.
