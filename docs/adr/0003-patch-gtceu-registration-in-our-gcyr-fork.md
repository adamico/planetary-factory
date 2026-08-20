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

---

# Custom materials reach GregTech as data files read by the fork

The same fork, for the same reason, now also carries a material loader. A modpack cannot register
a GregTech material at all. `Material.Builder.buildAndRegister` carries Rhino's `@HideFromJS` and
KubeJS hides `BuilderBase.createObject`, so a script cannot finish a builder by hand; the one
supported seam, `StartupEvents.registry('gtceu:material', ...)`, is dispatched after
`CommonInit.onRegisterEarly` has closed the material registry and generated every material's
items. Measured on a probe launch, KubeJS gets the event 340ms too late and the registration is
rejected with `IllegalStateException: Materials cannot be registered in the PostMaterialEvent (or
after)`.

Only code inside GregTech's material window can add a material, and only a mod can be there. So
the fork offers a file format instead: `data/<namespace>/gt_materials/<name>.json`, read during
the window, from any loaded mod's files and from the pack's `kubejs/data` directory. The fork
learns the format and nothing else — it does not know that `planetaryfactory:scrap` exists, the
same way `GTRegistriesMixin` above does not know which registrations it is rescuing. This is the
move the ore vein weights already made, shipping as datapack overrides because
`GTCEuServerEvents.oreVeins` proved unusable on 7.0.2.

## Considered Options

- **Register scrap directly in the fork.** Puts a modpack-specific resource in a mod that should
  not know about it, and makes every future pack material another fork release.
- **Substitute an existing GT material.** Reverses the tier-3 reasoning that introduced scrap:
  its whole role is to be one distinct input recycling into a spread of unrelated outputs, and no
  GregTech, Mekanism or Create material plays that part.
- **Defer `MaterialRegistry.close()` with a mixin.** Does not work. Item generation has already
  run by the time the close happens, so a material admitted late would exist with no dust item —
  closing before item generation is what the flag is for.
- **Fix the mod ordering.** Not the cause, and not worth re-investigating: `gtceu`'s own
  `neoforge.mods.toml` declares `kubejs` with `ordering = "AFTER"` and our fork declares no kubejs
  constraint, so the order is already `kubejs` -> `gtceu` -> `gcyr`.

## Consequences

Definitions are read off disk with `java.nio` rather than through `ResourceManager`. At
mod-loading time no resource pack has been assembled and no datapack exists, so the usual
`data/<namespace>/...` lookup has to be done by hand — which also means a definition is *not*
datapack-overridable and does not reload.

Discovery runs in the `GCYR` constructor while registration runs on the `RegisterEvent`, and the
split is load-bearing in a way worth spelling out. GregTech generates a material's items on a
`GTRegistrate` belonging to the material's *namespace*, created on demand while the item registry
event is being dispatched, falling back to GregTech's own mod bus for a namespace that is not a
loaded mod. A listener attached to a bus mid-dispatch never sees the event being dispatched, so a
registrate created that late registers nothing and the material ends up itemless — the same
failure as registering too late, reached from the other side. Creating the registrate during
construction, before any registry event fires, is what lets a namespace that owns no mod own a
material.

A malformed definition costs its own material and logs, rather than throwing. Throwing inside the
material window aborts mod loading, and this issue is on record as one where that surfaces as a
crash in an unrelated mod: the dead kubejs container broke resource loading, which left FTB Quests
with no theme file and crashed it on an empty shape map.

`GTMaterialSpec` (parsing) and `GTMaterialFinder` (walking the directories) touch no Minecraft or
GregTech class, which is what makes them testable; the fork gained a JUnit source set for them,
its first. Applying a spec to a `Material.Builder` is still only exercised by launching.

**Outcome: verified.** The pack launches clean. `Found 1 data-driven GregTech material(s) in
namespace(s) [planetaryfactory]` during construction, `Registered data-driven GregTech material
planetaryfactory:scrap` in the material window, no `Skipping material`, and KubeJS back to 4/4
startup scripts with 0 errors.

The item generation is the part worth recording, because it is what the design was uncertain
about: `scrap_dust`, `small_scrap_dust` and `tiny_scrap_dust` all exist. A namespace owning no mod
does get its material items, which confirms that creating the `GTRegistrate` during construction
is both necessary and sufficient.
