---
status: accepted
---

# GTCEu's registration loss is patched by a mixin in our GCyR fork

GTCEu 7.0.2 silently discards most of its own registrations whenever a GregTech addon is present.
Sound events, recipe types, recipe serializers and recipe categories never reach their registries;
nothing throws and nothing is logged. The pack launches to the title screen and then fails at world
creation, resolving `gtceu:sus` from `data/gtceu/jukebox_song/sus.json` — a datapack registry entry
that resolves strictly, and therefore the first thing to notice the absence.

`GTRegistries` keeps a `TO_REGISTER` table and queues into it while `isFrozen`. Anything that
touches GT's registry classes during mod construction fills that queue, which is precisely what an
addon does — merely class-loading GT-backed classes is enough. `actuallyRegister` then drains the
whole queue on the **first** `RegisterEvent` it sees, at `EventPriority.LOW`, and calls
`TO_REGISTER.clear()`. But `RegisterEvent.register(key, ...)` is a no-op unless `key` matches that
event's own registry, so only entries for the first registry land and every other row is thrown
away. `CommonInit.onRegister` compounds it: it runs at `NORMAL` on that same first event, ahead of
the `LOW` drain, so `GTSoundEntries.init()` iterates a still-empty `gtceu:sound` registry and
creates no `SoundEvent`s at all.

We patch it with `GTRegistriesMixin` in our GCyR fork, flushing the queue at the head of
`onUnfreeze` — `HIGHEST` on GT's bus, ahead of `CommonInit.onRegister` — so GT sees exactly the
state it would have had if it had never been frozen.

## Considered Options

- **Ship a KubeJS script registering the missing sound event.** This was tried first and is a dead
  end: it treats one symptom, and KubeJS cannot register recipe serializers or recipe types at all.
  It also only ever worked on KubeJS 2101.7.2 — on 2101.7.1-build.181, the build GTCEu 7.0.2 needs,
  `StartupEvents.registry('sound_event', ...)` is a silent no-op that logs no error.

  Note that "KubeJS cannot register recipe types" is true of KubeJS's own registry API only.
  GTCEu's KubeJS plugin ships `integration/kjs/builders/recipetype/` and
  `integration/kjs/builders/machine/`, so a custom GT recipe type and machine *are* scriptable
  — which is what the Personal Assembler and the cargo terminals now depend on.
- **Drop GCyR.** Removing it makes the pack work immediately, because nothing then fills the queue.
  Rejected: every planet in the design depends on GCyR, and the defect would return with any other
  GT addon.
- **Patch GTCEu itself.** Correct in principle and the right shape for an upstream fix, but it
  means maintaining a GregTech build — far more than maintaining a GCyR build, which ADR-0001
  already commits us to.
- **A standalone mixin mod in the pack.** Cleaner separation, but a new artifact to build and
  version for one mixin, when our fork already has mixin infrastructure.

## Consequences

The patch lives in a mod that has no business fixing GregTech, which is misleading if read in
isolation — hence this ADR and the mixin's own comment. It is keyed to 7.0.2's internals
(`TO_REGISTER`, `isFrozen`, `onUnfreeze`) and will need review on any GT update; if upstream fixes
the drain, the mixin becomes redundant and should be deleted rather than left to fight the fix.

Honouring the queue exposed three GCyR bugs that `clear()` had been hiding, all fixed in the same
commit (`adamico/gcyr@b258df8`): recipe types registered twice (`DeferredRegister` and
`GTRegistries`), `GCYRRecipeConditions.init()` wired only to a commented-out 1.20 handler, and
`GCYRMachines`/`GCYRDimensionMarkers`/`GCYRSoundEntries` never called from anywhere — which left
every GCyR machine unbound. The 1.21.1 port was less finished than "it compiles" suggested.

GT-dependent initialisation runs on our first `RegisterEvent`, not in the constructor and not in
`gtInitComplete()`. Machine definitions read GT's casing blocks, which do not exist during
construction, and GT calls `gtInitComplete()` from its own constructor despite the name — its
javadoc claim that it "runs after GTCEu has set up its content" is wrong.

**Outcome: verified.** The pack loads a world with 39324 recipes, no registry errors, GT worldgen
and surface ore indicators present. This also retires the `kubejs.plugins.txt` residue noted in
ADR-0001 (issue #1), stripped from the installed jar.
