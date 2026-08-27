# `planetaryfactory_core`

The pack's first-party NeoForge mod, built as a Gradle subproject of this repo (ADR-0014).

## What is in here, and what is not

Its remit is **mechanism only** (ADR-0015). Today that is two things no scripting API in this pack
exposes:

- **Flora** — two `SaplingBlock`s and their `TreeGrower`.
- **Research locks** — a mixin teaching GregTech machines to honour Researchd's `unlock_recipe`
  effects, plus the `research/` package behind it. KubeJS cannot mixin, and GregTech never asks the
  vanilla `RecipeManager`, so there is nowhere else this can live.
- **The lock annotation** — a recipe the viewing team has not researched is marked in both recipe
  viewers, from `compat/emi` and `compat/jei` over the shared `research/client` note (issue #75).
  JEI is a plugin its own annotation scan discovers. EMI is a mixin instead: its decorator API
  registers fine but only runs behind `EmiConfig.showRecipeDecorators`, which defaults off for
  players, so the documented seam is invisible to them. Either way these are Java interfaces and
  render calls, which is another thing KubeJS cannot do.

Nothing a designer would tune is compiled in: tree shape, drop counts, growth chance, display names,
models and textures are all pack data. **The jar's only asset is its own lang file**, holding the
handful of strings the mod itself emits — the GregTech lock refusal and the recipe-viewer annotation
— because a string a Java class passes to `Component.translatable` has no pack-side author to own
it. Which research locks which recipe is likewise data — `kubejs/server_scripts/researchd.js` — and
this mod only enforces whatever that declares.

Note the two names, which are deliberately different:

| | |
| --- | --- |
| Mod id | `planetaryfactory_core` |
| Registry namespace | `planetaryfactory` — shared with KubeJS |

So `planetaryfactory:yumako_sapling` is this mod's, and `planetaryfactory:yumako_leaves` is
`kubejs/startup_scripts/blocks.js`'s. Registering the same id twice is a startup crash whose
message will not mention either file, so read the ownership table in ADR-0015 before adding a block.

## Building

From the **repo root**, not from `mod/`:

```sh
./gradlew :planetaryfactory_core:installToPack
```

That builds the jar and copies it into `mods/`, which is the whole install step. `mods/` is
gitignored, so this has to be run once on any machine that intends to launch the pack — including
after a fresh clone, where the pack will otherwise start with two saplings missing and every
`planetaryfactory:*_sapling` reference failing to resolve.

`./gradlew :planetaryfactory_core:build` builds without installing. The first run downloads and
decompiles Minecraft and takes a few minutes; later runs are seconds.

Requires **JDK 21**. On this repo's macOS setup that is Homebrew's, and the usual macOS locations
are empty, so the toolchain has to be pointed at explicitly:

```sh
JAVA_HOME=/opt/homebrew/opt/openjdk@21 ./gradlew :planetaryfactory_core:installToPack
```

## Version pinning

`gradle.properties` pins Minecraft `1.21.1` and NeoForge `21.1.248` to match the pack. Both are
also written into the mod's dependency ranges, so a mismatched jar refuses to load rather than
crashing obscurely. When the pack's NeoForge build moves, move `neoforge_version` with it.

## Tests

```
./gradlew :planetaryfactory_core:test
```

JUnit 5, run on a plain JVM. This is the pack's "this pack logic computes something" row in
[what to check](../docs/testing/what-to-check.md), and it covers exactly that: `research/` holds the
recipe-to-research index, the lock-bypass dedupe and the viewer's lock lookup, and all three are
written free of any Minecraft type so the check needs no game. What touches Minecraft is glue that
holds no rules — `ResearchLocks`, `research/client` and the two viewers' compat code — and it is the
"looks or feels right" row instead: checked by a human on delivery. Registration — blocks, items, trees — still has nothing to assert that
the game does not assert louder at startup, and gets no test.

**The test source set is deliberately absent from `neoForge.mods` in `build.gradle`**, so Minecraft
is not on its classpath. That is what keeps the split honest: logic that drifts into needing a
`Level` stops compiling in the test source set rather than quietly becoming untestable. The glue
that does need a `Level` lives in `ResearchLocks`, holds no rules of its own, and is checked by a
human in-game.

The subproject is also configured with a `gameTestServer` run, so NeoForge GameTest is available the
moment this mod grows behaviour that needs a server — a capability the pack's sibling-clone mods
(GCyR, `respoiled`) do not have. Nothing trips that trigger today.
