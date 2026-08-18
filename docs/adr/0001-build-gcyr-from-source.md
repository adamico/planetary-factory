---
status: accepted
---

# GCyR is built from source, and GTCEu 7.x is the anchor

The released `gcyr-1.21.1-0.2.4.jar` (Sep 2024) is the newest published 1.21.1 build and it is
permanently incompatible with GTCEu 7.x: its `@GTAddon` annotation carries no `value`, so
`AddonFinder.getInstances` NPEs, GregTech fails construction, and the whole pack dies during mod
loading. We anchor on GTCEu 7.x and build GCyR ourselves from the upstream `1.21.1` branch, where
the port to the 7.x API — including `@GTAddon(GCYR.MOD_ID)` — is already done but has never been
released.

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
retire this decision. Two risks remain unverified until a build is attempted — the branch pins
`gtceu 7.1.0-SNAPSHOT` against our installed 7.0.2, and it pins ldlib `1.0.35.a` while the pack
ships ldlib2 `2.2.35`. Building requires a JDK 21 toolchain, which this machine does not currently
have. Redistribution of the pack requires checking GCyR's licence, since the jar is not a
published artifact.
