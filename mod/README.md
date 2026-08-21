# `planetaryfactory_core`

The pack's first-party NeoForge mod, built as a Gradle subproject of this repo (ADR-0014).

## What is in here, and what is not

Its remit is **mechanism only** (ADR-0015). Today that is two `SaplingBlock`s and their
`TreeGrower`, because no scripting API in this pack exposes either. Nothing a designer would tune
is compiled in: tree shape, drop counts, growth chance, display names, models and textures are all
pack data, and **the jar ships no assets and no data at all** — only four classes.

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

There are none yet, and that is on purpose: four classes of registration have nothing to assert
that the game does not assert at startup. The subproject is configured with a `gameTestServer` run
so that NeoForge GameTest is available the moment this mod grows logic worth testing — which is a
capability the pack's sibling-clone mods (GCyR, `respoiled`) do not have.
