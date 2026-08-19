# DRAFT — not filed

Target: `GregTechCEu/GregTech-Modern`, issue. Needs approval before filing: it posts to a third
party's tracker under the maintainer's GitHub account.

Duplicate check: not yet redone for this framing. The earlier search was for the sound-event
symptom, which was the wrong search. Search for `TO_REGISTER`, `actuallyRegister` and
"addon registrations missing" before filing.

---

**Title:** GTRegistries drops queued registrations for every registry but the first (7.0.2, 1.21.1)

### Summary

When any addon is present, GTCEu loses most of its own registrations: sound events, recipe types,
recipe serializers and recipe categories never reach their registries. Nothing throws and nothing
is logged. The first visible failure is world creation aborting on `gtceu:sus`, far from the cause.

### Mechanism

`GTRegistries.register` queues into `TO_REGISTER` while `isFrozen`. `isFrozen` starts `true`, so
anything registered before the first `RegisterEvent` is queued — and an addon fills that queue
simply by class-loading GT-backed classes during mod construction, which is what addons do.

`actuallyRegister` is registered at `EventPriority.LOW` and drains on the first `RegisterEvent`:

```java
for (Registry reg : TO_REGISTER.rowKeySet()) {
    event.register(reg.key(), helper -> TO_REGISTER.row(reg).forEach(helper::register));
}
TO_REGISTER.clear();
```

`RegisterEvent.register(key, ...)` is a no-op unless `key` equals the event's own registry key, so
on the first event only that registry's rows land. `clear()` then discards every other row. The
entries for registries whose `RegisterEvent` has not fired yet are gone, with no error.

`CommonInit.onRegister` makes it worse: it runs at `NORMAL` on that same first event, ahead of the
`LOW` drain, so `GTSoundEntries.init()` iterates a still-empty `gtceu:sound` registry and creates
no `SoundEvent`s.

### Evidence

Instrumented from an addon, logging `isFrozen`, the queue size, and whether `gtceu:sus` is
registered, at `HIGHEST`/`LOWEST` on every `RegisterEvent`. With the addon loaded:

```
HIGHEST registry=gtceu:element        isFrozen=false queued=224 susRegistered=false
LOWEST  registry=gtceu:element        isFrozen=false queued=0   susRegistered=false
HIGHEST registry=minecraft:sound_event isFrozen=false queued=0  susRegistered=false
```

224 entries queued before the first event; all drained on `gtceu:element`, where only element rows
can land; the queue is empty by the time `minecraft:sound_event` fires. Logging the rows at drain
time shows what is lost:

```
event=gtceu:element rows=[gtceu:recipe_category=61 gtceu:chance_logic=5
                          minecraft:recipe_type=61 minecraft:recipe_serializer=57
                          gtceu:sound=40]
```

With every addon registration disabled, the queue is empty at the first event and `gtceu:sus`
registers normally — the addon is not doing anything unusual, it is only filling the queue.

### Consequences downstream

`data/gtceu/jukebox_song/sus.json` references `gtceu:sus`, and datapack registries resolve
strictly at world load, so world creation aborts with `Failed to get element
ResourceKey[minecraft:sound_event / gtceu:sus]`. A datapack override does not help: 1.21.1's
`RegistryDataLoader` parses every pack's copy, so the mod's own file still fails. Later, recipe
generation fails on `Unregistered holder ... Direct{GTRecipeSerializer}` because the serializer was
discarded too.

### Suggested fix

Flush the queue before GT consumes its own registries. `onUnfreeze` runs at `HIGHEST` on the same
bus, ahead of `CommonInit.onRegister`, so registering the queued entries directly there restores
the state GT would have had unfrozen. Draining per-event in `actuallyRegister` instead is not
enough on its own: `CommonInit.onRegister` still runs before the drain for every registry after the
first.

Whatever the fix, `TO_REGISTER.clear()` should not discard entries that were never registered — a
warning when the queue is non-empty at freeze time would have made this immediately visible instead
of surfacing as an unrelated crash.

### Environment

GTCEu 7.0.2, Minecraft 1.21.1, NeoForge 21.1.248, addon: Gregicality Rocketry (1.21.1 branch).
Workaround in use: a mixin on `onUnfreeze` in our own build.
