# DRAFT — not filed

Target: `Mrbysco/Spoiled` (upstream of our `respoiled` fork), branch `multi/1.21`. A PR adding a
dimension allowlist config. Needs approval before filing: it posts to a third party's tracker under
the maintainer's GitHub account.

Duplicate check: not done. Search the tracker for "dimension", "Overworld", "other dimensions" and
"nether" before filing — a mod that only works in one dimension is the kind of thing someone will
already have raised, possibly as a bug report rather than a feature request.

Verify before filing: the line reference below (`SpoilHandler.java:42`) comes from our own reading
and has not been re-checked against the current head of `multi/1.21`. Confirm the line, the method
and that no config already gates it.

Not included in this PR: everything else our fork does. The timer removal, the probability model, the
chunk catch-up and the storage mixins are pack-specific inversions of the mod's design and are not
upstream material. This PR is the one piece of our divergence that is a plain defect fix and would
benefit every user of the mod.

---

**Title:** Allow spoiling outside the Overworld (configurable dimension allowlist)

### Summary

`SpoilHandler` returns early for any dimension that is not the Overworld:

```java
if (level.dimension() != Level.OVERWORLD) return;
```

There is no config for it, so on any world where the player spends time outside the Overworld the mod
silently does nothing. Food carried into the Nether, the End, or any modded dimension is preserved
indefinitely — which reads as a bug to a player who has no reason to expect the mod to be
Overworld-only, and is a straightforward exploit in any pack with a dimension mod.

Dimension mods are common enough that this is not a niche case: Twilight Forest, the Aether, and the
various tech-pack planet mods all put players in non-Overworld dimensions for long stretches.

### Proposed change

Replace the hardcoded check with a config-driven allowlist:

```java
// SpoiledConfig
public static final ModConfigSpec.ConfigValue<List<? extends String>> spoilDimensions;
// default: List.of("minecraft:overworld")

// SpoilHandler
if (!SpoiledConfig.spoilsIn(level.dimension())) return;
```

with a second option — `spoilInAllDimensions`, default `false` — so a pack author can opt in
wholesale without enumerating dimension IDs they may not know at config time.

**Defaults preserve current behaviour exactly.** An existing user who never touches the config sees no
change; only someone who edits it gets spoiling elsewhere.

### Why config rather than just removing the check

The check presumably exists for a reason — most likely that spoiling in the End or a creative-style
dimension was judged surprising. Making it configurable respects that judgement while unblocking
everyone it currently surprises in the other direction. If the check is instead vestigial, the
allowlist default can simply be widened later without another API change.

### Notes

Happy to adjust the config shape — `List<String>` allowlist, denylist, or a boolean plus overrides —
to whatever fits the mod's existing config conventions.
