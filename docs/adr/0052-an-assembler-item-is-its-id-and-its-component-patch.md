---
status: accepted
---

# An Assembler item is its id and its component patch

ADR-0038 built the Personal Assembler's queue, resolver and codecs around an item named by its
registry id as a bare string. That is the decision that keeps the queue's rules — reservations,
chain order, the pause, the refund — Minecraft-free unit tests, with `InventoryPlayerItems` as the
one place the string meets an `ItemStack`. It is a good decision and this ADR does not reverse it.

It is also incomplete. Researchd's science packs are all the same item, `researchd:research_pack`,
distinguished only by the `researchd:research_pack` data component. Under a bare-id identity,
`count("researchd:research_pack")` answers with every pack the player holds and a plan for
automation science is satisfiable out of a stack of chemical science. `RuntimeHandRecipes` refuses
any component-bearing stack rather than fold them, so both science pack recipes — emitted, swept in,
visible in EMI — never enter the graph and simply do not exist in the Assembler (#222).

## The rule

**An Assembler item is identified by its registry id together with its data component patch, encoded
as one string.** An item with an empty patch encodes to exactly its registry id, so every existing
key, every committed queue attachment and every unit test fixture is unchanged. A component-bearing
item encodes to its id followed by the patch in vanilla's own item-argument syntax:

```
researchd:research_pack[researchd:research_pack="planetary_factory:automation_science_pack"]
```

That format and not a hash, because a queue attachment on disk and a refusal line in a log are both
read by a person, and the one thing that made #222 diagnosable was a log line naming the item. It is
already the syntax `kubejs/server_scripts/researchd.js` writes in its own comment.

The identity stays a `String`. That is the whole point: `ItemAmount`, `Ingredient`, `ItemBag`,
`PlayerItems`, `AssemblerQueue`, `PlanResolver` and `AssemblerCodecs` keep their types, keep their
tests and keep not importing `net.minecraft`. The codec needs no change at all — which is not a
reason to skip asserting the round trip, since a science pack that comes back as a different science
pack over a logout is exactly the failure ADR-0038 asked the round trip to catch.

## Matching is exact, which is a deliberate divergence

Two keys are the same item when the strings are equal. A key with no patch therefore names the
pristine item and nothing else — it does **not** match a stack carrying components.

Minecraft's own `neoforge:components` ingredient is a *subset* match: a stack with extra unrelated
components still matches. This diverges from it knowingly. Subset matching is not expressible as
string equality, so adopting it would mean the resolver comparing `ItemStack`s, which is the
Minecraft-free property this ADR exists to defend. Nothing needs it: no recipe the pack emits
references a bare `researchd:research_pack`, and the four science packs are the only rows in
`data/pack/item-map.json` carrying a `components` patch at all.

## Encoding is canonical, and that means sorted

The same stack must always produce the same string. `DataComponentPatch` guarantees no iteration
order across loads, so `ItemKey` sorts patch entries by component-type id before writing. Two
differently-ordered patches encoding identically is asserted, not assumed: every science pack has
exactly one component today, which is why skipping the sort would work perfectly until the first
two-component item and then produce two keys for one item — #222 again, in the other direction.

## `ItemKey` splits, and only one half knows Minecraft

- The **format** half — building a key from an id and a patch, parsing one back into an id and raw
  patch text, and the ordering rule — is Minecraft-free and lives in `core/assembler/` beside the
  queue it serves. It is a unit test.
- The **resolve** half — turning a parsed key into an `ItemStack`, which needs a registry and a
  `HolderLookup.Provider` — happens only where a provider already exists.

Splitting it is what keeps the format itself checkable without a world load. It also means a key
can be well-formed and still resolve to nothing.

## The crossing point is one class, and today it is five

`ItemAmount` and `InventoryPlayerItems` both state in their Javadoc that `InventoryPlayerItems` is
the single crossing between an item id and an `ItemStack` — ADR-0038 never wrote it down, which is
part of why it drifted. It has not been true since the runtime plan source landed: `RuntimePlanSource`,
`RuntimeHandRecipes` (twice) and the client's `PlanItems` each do their own
`BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()`. Five crossings that agree on bare ids
would disagree the moment one of them learns about components, and the disagreement is silent — a
plan whose recipe names a component-bearing output, reserved against an inventory counted without
it. **Every crossing goes through `ItemKey`.** Restoring the single-crossing claim is part of this
change, not a follow-up, and ADR-0038 — the document anyone touching the queue reads first — carries
an amendment line pointing here, since it is where the identity is now decided.

## The give side is part of the identity

Reading is only half. `InventoryPlayerItems.toStack` builds `new ItemStack(type, count)`, which for
a science pack is a blank research pack with no component — a plan that resolved correctly, ran
correctly and delivered the wrong item. `toStack`, `give` and `fits` all take the patch from the
key, and `count`/`take` compare the full key rather than `stack.is(type)`, which is the fold itself.
`fits` is named here for a reason: two packs differing by component do not stack, so room counted by
item type alone over-counts and `give`'s all-or-nothing guarantee breaks.

## The client draws the item, not the id

`PlanItems` is the one place a key becomes a sprite and a name. It resolves the patch and builds the
real stack, so Researchd's own rendering names the pack. Falling back to the bare item would put
four identical icons in a plan's `To Craft` list — the same fold this ADR removes from the queue,
moved onto the screen where it is worse, because there it looks like a duplicate row.

## A key that resolves to nothing is refused at the graph, and says so

`RuntimeHandRecipes` validates every key while it builds the graph and refuses a recipe whose key
resolves to nothing, on the refusal log line it already prints. An unresolvable key therefore never
reaches the resolver, which stays free of any notion of resolvability, and the failure arrives as a
named line rather than as a queue that pauses forever with nothing in the log. That log line is the
only reason #222 was a ticket and not a mystery.

## What this does not do

It does not admit outputs matching several items, an output with a chance, or an ingredient with no
fixed count. Those refusals in `RuntimeHandRecipes` are about a result the plan cannot name, which
is a different problem and stays refused. The component branch, however, goes from **both** sides at
once: a component-bearing input is as representable as an output now, and leaving half the branch in
place would read as the change not having worked.

## Science packs stay ungated, and that is not an oversight

Admitting these recipes makes automation science hand-craftable from a copper plate and an iron gear
before any research exists. That is Factorio's own bootstrap — red science is craftable from the
first minute by design — and gating a science pack behind research it is the input to is a cycle.
Recorded here so it is not later "fixed".

## Rejected: one registered item per science pack

Ids stay unique, the refusal never fires, nothing in the Assembler changes. Rejected on two counts.
It is a Researchd integration decision — the Lab reads its own component to decide what a pack is
worth — so it trades a contained change here for an uncontained one across everything that names
`researchd:research_pack`, and `data/pack/item-map.json` says in four separate notes that a plain
item of the same name is a second, inert pack the Lab cannot read. And it does not generalise: the
next component-bearing ingredient re-opens the same hole with the Assembler having learned nothing.

## Consequences

- Both science pack recipes enter the graph and are craftable at the hand.
- A future component-bearing ingredient works without a further decision.
- The queue, resolver and codecs remain Minecraft-free unit tests.
- The converter, `data/pack/item-map.json` and the emitted recipe JSON are untouched: the key is a
  runtime representation, and writing it into the JSON would be a second source of truth for one
  fact, leaving the static check asserting the converter against itself.
- The encoding is **not** a save-format commitment. The pack is pre-release; the format may change
  freely and no datafixer is owed. Stated so nobody writes one.

## Checks

- The queue and resolver count, reserve and deliver two items sharing a registry id without folding
  them — **unit test**, `core/assembler/`.
- `ItemKey` round-trips, an empty patch encodes to the bare id, and two differently-ordered patches
  encode identically — **unit test**. The empty-patch case is what leaves every existing key alone.
- A queued plan holding a science pack survives a logout as the same pack — **unit test**,
  extending `AssemblerCodecsTest`.
- The emitted `factorio_category: crafting` set contains `automation_science_pack` and
  `chemical_science_pack` — **static data check**, `tests/factorio/`. Those two by name: whether
  logistic and production are hand-craftable is the corpus and #25's ladder talking, not this.
- The Assembler offers and delivers an automation science pack, and no refusal line names either
  recipe — **human on delivery**. The `RecipeGraph` is a running server.
