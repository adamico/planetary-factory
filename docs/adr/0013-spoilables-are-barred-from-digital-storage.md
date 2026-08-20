---
status: accepted
---

# Spoilables are barred from digital storage, by mixin, because nothing else works

An AE2 ME network does not store `ItemStack`s in an `IItemHandler`. It stores an `AEItemKey` and a
count in a `MEStorage`. Mekanism's QIO is the same shape. ADR 0011's sweep walks block entities and
item handlers, so it never sees a single item in either — which makes "put your Jelly in the ME
system" an infinite-freshness fridge, and both mods ship in this pack.

**Spoilables are therefore rejected on insertion into digital storage.** They cannot be stored at
all, rather than being stored and decaying.

## Why not decay them in place

Because the supported hooks to do either do not exist, and rejection is the one that is exploit-proof
by construction. Both mods were read at the bytecode level:

- AE2 19.2.17 has **no blacklist tag, no config option, and no reachable public API**.
  `IBasicCellItem.isBlackListed` is public API and *is* consulted by `BasicCellInventory.insert`, but
  AE2's own `BasicStorageCell` never overrides it, and `StorageCells.addCellHandler` appends to a
  list whose first match wins — so a third-party handler registered later can never shadow AE2's own
  cells. `AEKeyFilter` is misleadingly named: it is GUI and partition filtering and is **not on the
  insert path**. Cell Workbench partitioning is player-configurable and therefore
  player-removable.
- Mekanism 10.7.19.85 QIO is worse: **no hook at all**, public or internal. `QIOFrequency.massInsert`
  checks emptiness and capacity, and nothing else.
- **No published 1.21.1 NeoForge mod does this.** The adjacent ones (View Cells, Magnetic Cells)
  filter display or ground-pickup, not storage. Every food-spoilage mod stops at "a fridge prevents
  spoilage" and none touches ME networks. There is nothing to adopt.
- **KubeJS cannot.** Neither mod ships a KubeJS plugin. Applied KubeJS exists, but its `storageDelta`
  event is observational and **not cancellable**, and it requires KubeJS 2101.7.2+ — one build newer
  than the 2101.7.1-build.181 that ADR 0001 pins us to for GTCEu compatibility.

So: two `@Inject(at = HEAD, cancellable = true)` mixins, living in the `respoiled` fork, both
`required = false` so the pack still loads without either mod, both behind one shared tag predicate
so the two sides cannot drift.

- `appeng.me.storage.NetworkStorage#insert(AEKey, long, Actionable, IActionSource)`
- `mekanism.common.content.qio.QIOFrequency#massInsert(ItemStack, long, Action)` and `#addItem`

Both are the **network-level** chokepoint, not the drive or cell level. One method each, and it
covers import buses, interfaces, pattern providers and terminal inserts uniformly. The tag check
itself uses public API (`AEItemKey.isTagged`), so the mixin bodies need no internal types.

## Considered Options

- **Per-mod integration that decays network contents.** The faithful answer and the first instinct.
  Rejected: it needs a real integration per mod with no shared abstraction, each with its own
  sweep-cost problem over a storage layer that is not an inventory, and it leaves every *future*
  storage mod as a fresh hole. Rejection generalises; decay-in-place does not.
- **A standalone mixin mod.** Cleaner separation of concerns — `respoiled` has no obvious business
  patching AE2. Rejected for the same reason ADR 0003 rejected it: a third build artifact to version
  for four lines of injection. If the fork's dependency surface later blocks a rebase, splitting them
  out is a cheap refactor.
- **Forking Mekanism.** It is MIT, so it is legal and clean. Buys nothing: AE2 is LGPL-3.0 and needs
  a mixin regardless, so a fork adds an artifact without removing one.
- **Accept the bypass.** Rejected outright. Decay's entire tension is that organics cannot be
  hoarded; a mod that removes that is not a balance problem, it is an off switch.

## Consequences

**A storage bus pointed at a chest of Jelly still shows that Jelly in the terminal.** The network can
see it and refuses to store it. This is accepted and, we think, the right lesson — filtering the read
path too would hide inventory the player owns, which is a worse failure than a confusing one.

**An import bus set to pull a spoilable simply stalls.** Silently, forever: nothing jams, nothing
turns red. This is the least discoverable failure in the design, which is why the surfacing is not
optional — the Jade provider reports "Decay: not storable in networks" on any spoilable, and the item
tooltip carries the same line permanently. The tooltip is the load-bearing half, because it is on the
item and the player reads it before building the bus rather than after.

Both mixin targets are internal classes. They have been stable across the 1.21.1 line, but they are
internals and an AE2 or Mekanism update can move them. `required = false` means the failure mode is a
silently missing blacklist rather than a crash, which is the wrong way round for an anti-exploit
measure — the worldgen-check harness should assert the mixins applied.

Organics are pushed into physical buffers, which is not a side effect but the point: Clog only bites
when storage is finite.
