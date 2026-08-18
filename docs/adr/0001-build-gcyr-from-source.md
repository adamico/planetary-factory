---
status: accepted
---

# GCyR is built from source, and GTCEu 7.x is the anchor

The released `gcyr-1.21.1-0.2.4.jar` (Sep 2024) is the newest published 1.21.1 build and it is
permanently incompatible with GTCEu 7.x: its `@GTAddon` annotation carries no `value`, so
`AddonFinder.getInstances` NPEs, GregTech fails construction, and the whole pack dies during mod
loading. We anchor on GTCEu 7.x and build GCyR ourselves from the upstream `1.21.1` branch, where
the port to the 7.x API — including `@GTAddon(GCYR.MOD_ID)` — has been carried far enough to
compile but has never been released or confirmed working.

## Considered Options

- **Patch the released jar.** Rejected on evidence: of 133 `com.gregtechceu` classes gcyr 0.2.4
  references, 20 are absent from 7.0.2 and 13 are gone outright — the material registration system
  (`MaterialEvent`, `MaterialRegistryEvent`, `IMaterialRegistryManager`, `UnificationEntry`), the
  registry system (`GTRegistry` and its inner classes) and ore worldgen (`GTOreDefinition`,
  `GTOres`). Fixing the annotation only moves the failure from an NPE to `NoClassDefFoundError`.
- **Downgrade GTCEu to 1.4.4.** Genuinely viable and cheaper today — gcyr is the only mod in the
  pack that declares a `gtceu` dependency at all, so nothing else objects. Rejected because it
  pins the pack two majors behind before any of the design is implemented, and every future GT
  addon targets 7.x.
- **Drop GCyR for Stellaris.** Stellaris is the standing fallback if the build proves impossible;
  it is not the preference. See ADR-0002.

## Consequences

We become the de facto maintainer of our own GCyR build: updates are a rebase and rebuild, not a
CurseForge click. An issue should be opened upstream requesting a 1.21.1 release, which would
retire this decision.

The branch's own history set expectations: its last two commits are "It builds? Yes. It crashes at
launch? Yes." followed by "Remove kjs, fix gcyr" (both 2025-09-04), and none of the eighteen public
forks has advanced it past that point.

**Outcome: this worked.** The build succeeds and the pack launches. Two `build.gradle` changes were
needed and no source changes at all; they live in our fork at `adamico/gcyr`, branch `1.21.1`,
commit `3434a0a`. GregTech now constructs, and the resulting jar references 124 gtceu classes with
none missing from 7.0.2 — against 133 referenced and 20 missing for the published release. The jar
is installed as `mods/gcyr-1.21.1-0.2.4+gt7.0.2-src.jar`; the broken release is parked in
`mods/.replaced/`, outside the mods scan.

One residue of "Remove kjs, fix gcyr": the jar still ships a `kubejs.plugins.txt` naming
`GCYRKubeJSPlugin`, a class the commit deleted, so KubeJS logs a `ClassNotFoundException` and
continues without any gcyr bindings. Non-fatal, but it matters for a pack scripted end to end in
KubeJS. Tracked separately.

Two risks flagged at the time of writing have since been cleared. `gtceu 7.1.0-SNAPSHOT` does
resolve from `maven.gtceu.com`, but we pin the build to `7.0.2` instead so it matches the jar
actually installed in the pack. The apparent ldlib mismatch was not one: gtceu 7.0.2's own POM
declares `com.lowdragmc.ldlib:ldlib-neoforge-1.21.1:1.0.35.a`, exactly what the branch pins — the
`ldlib2-…-2.2.35-all.jar` in `mods/` is the same library under its runtime versioning.

GCyR is LGPL-3.0, so redistributing our own build inside the pack is permitted provided it stays
LGPL and the source remains available. Our fork is at `adamico/gcyr`, which satisfies that.
