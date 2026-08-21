---
status: accepted
---

# The pack has a first-party mod, built in-repo

Sapros's two trees need a first-class `SaplingBlock` backed by a `TreeGrower`. No scripting API in
this pack exposes one, so the pack acquires **`planetaryfactory_core`**: a first-party NeoForge mod,
built as a Gradle subproject inside this repo.

## Why Java, precisely

The reasoning matters more than the conclusion, because the obvious version of it is false and a
future reader will check.

**A datapack can build a structured tree.** `minecraft:tree` with trunk and foliage placers produces
exactly the trees worldgen places, and `/place feature` can invoke that same feature from a script —
KubeJS exposes `kjs$runCommand` on both `LevelKJS` and `MinecraftServerKJS`, and
`RandomTickCallbackJS` hands a script the level it would need. So "a datapack cannot make a real
tree" is **not** the reason, and an ADR that said so would be wrong.

The reason is that a *sapling* is more than a block that becomes a tree. `TreeGrower` detects 2×2
megatrees, validates the space is clear before growing, and runs through the game's own growth path
rather than a command string fired from a random tick. Getting that behaviour without it means
reimplementing it in JavaScript around a command invocation — which works, and which is a worse
version of something the game already does correctly.

There is a second reason, and it was the one stated first: **content coherence.** Sapros's flora is
one interlocking content set, and splitting its blocks across a scripting layer and a data layer by
which half the engine happens to expose is an authoring cost paid on every future edit. This is a
legitimate reason and it is recorded as what it is — a judgement about where content lives, not a
technical necessity.

Everything else on Sapros stays out of Java. See ADR-0015 for the boundary.

## What this reverses

`#9` user story 26 — *"As a pack developer, I want a body's whole definition to be datapack and
KubeJS work, so that tuning it does not mean rebuilding a mod"* — is demoted from a rule to a
default that yields. The thing it protects is **tuning**: vein weights, densities, biome frequency,
loot tables, lang. That protection survives intact under ADR-0015, which keeps every tunable value
in data. What story 26 does not get to decide is where a block class lives.

ADR-0011's principle is unchanged and is now pack-wide: **the code owns the mechanism, the data owns
the content.**

## Why in-repo, departing from ADR-0001

ADR-0001 (GCyR) and ADR-0011 (`respoiled`) both use a sibling clone outside this repo, a personal
GitHub fork, a manual build and a jar dropped into a gitignored `mods/`. That pattern is not
followed here, and ADR-0011's own words say why it should not be: it rejected an in-repo Gradle
subproject on the grounds that *"the diff here is surgical changes to someone else's mod, not a new
mod."*

This **is** a new mod. There is no upstream to rebase against, so the property the sibling pattern
exists to preserve — a small, rebasable diff against someone else's code — has nothing to preserve.
First-party source that only this pack consumes belongs in this pack's repo, where a change to a
tree's block class and a change to that tree's feature JSON land in the same commit and get reviewed
together. They will change together constantly.

A consequence worth naming: an in-repo subproject can run NeoForge **GameTest** in CI. A sibling
clone whose only output is a manually built jar cannot, easily. This was not part of the argument
for the decision but it is a real benefit of it, available if the mod ever grows logic worth testing
that way.

## Naming

Mod ID **`planetaryfactory_core`**. Registry namespace **`planetaryfactory`**.

These are deliberately different, and NeoForge allows it — `DeferredRegister.create(Registries.BLOCK,
"planetaryfactory")` is legal from a mod whose ID is something else.

- The **ID** avoids colliding with the modpack's own name once published. `_core` is the conventional
  companion-mod suffix and reads as subordinate to the pack rather than as a competing product. A
  scoped name like `_worldgen` was rejected: the remit is deliberately unscoped, and a mod ID cannot
  be renamed without breaking every world that used it.
- The **namespace** is shared with KubeJS's existing registrations — `planetaryfactory:scrap_pile`
  and `planetaryfactory:fulgorite` already exist under it. Giving the mod its own namespace would
  make every block ID advertise which tool registered it, leaking an implementation detail into
  every tag file, loot table and recipe in the pack.

## Considered Options

- **`/place feature` from a KubeJS random tick.** Verified reachable, and it keeps one tree
  definition. Rejected on the sapling semantics above, plus two unresolved questions it would have
  needed spiking — whether the KubeJS command source clears `/place feature`'s permission level 2,
  and whether the command applies the configured feature's placement filters and so silently no-ops
  on a farm plot.
- **Tall crops instead of trees.** `CropBlockBuilder` handles planting, bonemeal, growth and harvest
  with no Java at all. Rejected on fidelity: Gleba's Yumako and Jellystem are real trees, and the two
  harvest gestures the design turns on — fruit from a canopy, Jellynut from a trunk — need an actual
  canopy and an actual trunk.
- **Trees that never need planting**, harvested non-destructively forever. Rejected: on Gleba the
  agricultural tower plants and harvests destructively; trees are not an infinitely standing crop.
- **A Sapros-only mod.** Rejected: the thing it owns — custom blocks with behaviour only Java
  reaches — is obviously not Sapros-specific, and a second such mod later would split one namespace
  three ways.
- **A separate first-party repo with its own release cycle.** Rejected: nobody outside this pack
  consumes it, and the version-bump ceremony would be pure overhead.
