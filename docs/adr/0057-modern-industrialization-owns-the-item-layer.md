---
status: accepted
---

# Modern Industrialization owns the item layer, and unification arbitrates one supplier

ADR-0053 gave the item layer to GregTech. ADR-0056 takes GregTech out of the pack and says nothing
about the item layer at all, which leaves twelve unified tags with no winner named and 153 emitted
recipes pointing at `gtceu:` items that will not exist. This ADR supplies the replacement.

## The rule

**Modern Industrialization supplies the item layer for the ADR-0021 alphabet, and the pack registers
only the gaps.** AlmostUnified stays, and its job is unchanged: it arbitrates between MI, Create and
vanilla, exactly as it arbitrated between GregTech, Create and vanilla.

## Why a mod owns this at all, again

ADR-0053's argument survives its subject leaving. A plate is not a mechanism — it is an item with a
texture, a tag and a recipe — and the pack authoring forty of them buys nothing that an installed
mod does not already ship. What ADR-0053 got right, and what this ADR keeps, is that the *owner must
be named*: an item layer with two suppliers and no declared winner is what #220 is, and #220 is not a
bug in AlmostUnified.

`MaterialRegistry`, `MaterialBuilder` and `MIParts` are MI's equivalent of GregTech's material
registry, reachable from KubeJS through `AddMaterialsEventJS`. The alphabet ADR-0021 closed is
mostly MI's stock set already, so the pack registers gaps rather than a whole alphabet.

## Which parts are taken, and which are refused

**Taken:** the ingot, plate, dust, gear, rod, nugget and storage-block parts — the twelve tags
`config/almostunified/unification/materials.json` already names.

**Refused: cables and batteries.** `MIParts` generates both from the same material rows, and
ADR-0056 already refused MI's voltage ladder, cables, pipes and logistics. Taking a material must not
silently take every part template hanging off it; the two are separable and this ADR separates them.
A cable part that reaches the game is a distribution mechanism the pack did not choose, competing
with ADR-0036's Supply Area Pole.

## What this resolves rather than fixes

**#220 dissolves.** The pack ships two plate items per material because GregTech and Create both
supply them and unification replaces rather than broadens (ADR-0053's second error, and the one that
shipped). With GregTech gone the pair is MI-and-Create rather than GregTech-and-Create — one pair,
not two — and the arbiter has one overlap to resolve instead of a three-way. That is a smaller
problem, not an absent one: **AlmostUnified stays**, because Create still ships sheets and vanilla
still ships ingots.

**ADR-0053's non-recipe trap does not go away.** Its central finding — that unification reaches
inside recipes and nowhere else, so a literal item id in KubeJS, in `planetaryfactory_core` or in a
research trigger points at whatever unification just orphaned — is a fact about AlmostUnified, not
about GregTech. Every site ADR-0053 identified has to be re-pointed at MI ids in the same commit
that regenerates the recipes, `StartingKit.java` included.

## Consequences

- `data/pack/item-map.json`'s 16 `gtceu:` rows change namespace, and the change is a *re-judgement*
  rather than a rename: MI's part naming is not GregTech's, and a row that resolves to nothing is a
  silent empty slot in the starting kit (#203's lesson).
- `config/almostunified/` is regenerated against MI tags in the flip commit, not after it. A stale
  config is not an error — it is an ingredient that stops being replaced, with nothing in any log.
- The regeneration of all four converters depends on this ADR, which is why it is written before the
  branch starts rather than alongside its ticket.

This ADR supersedes ADR-0053. ADR-0053's account of what AlmostUnified does remains correct and is
the reason this one is short.
