---
status: accepted
---

# KubeJS is pinned by GTCEu, not by KubeJS

The pack ships `kubejs-neoforge-2101.7.1-build.181`. Twice the version was raised and twice it was
rolled back, and both times the reason was recorded only as folklore: *bumping KubeJS breaks the
pack*. `mods/` is gitignored wholesale, so neither attempt left a stack trace, a log, or a note.
The constraint outlived the evidence for it, and by the time it was written down as an agent memory
it had become a rule with no reason attached.

This ADR records the reason. It is not KubeJS.

## The immediate cause

KubeJS `2101.7.2` relocated its bindings package:

| build | `ServerEvents` lives at |
| --- | --- |
| `2101.7.1-build.181` | `dev/latvian/mods/kubejs/bindings/event/ServerEvents` |
| `2101.7.2-build.374` | `dev/latvian/mods/kubejs/plugin/builtin/event/ServerEvents` |

`gtceu 7.0.2` holds a compiled reference to the old path:

```
java.lang.NoClassDefFoundError: dev/latvian/mods/kubejs/bindings/event/ServerEvents
  at gtceu@7.0.2/com.gregtechceu.gtceu.data.recipe.GTRecipes$KJSCallWrapper.recipeEventHasListeners(GTRecipes.java:134)
  at gtceu@7.0.2/com.gregtechceu.gtceu.data.recipe.GTRecipes.recipeAddition(GTRecipes.java:112)
  at net.minecraft.server.ReloadableServerResources.loadResources(ReloadableServerResources.java)
  at net.minecraft.client.gui.screens.worldselection.WorldOpenFlows.loadWorldDataBlocking(WorldOpenFlows.java:200)
```

## Why it was mistaken for a KubeJS problem

The throw site is `ReloadableServerResources.loadResources` — **world load**, not mod construction.
On `build.374` the pack starts perfectly: no construction failures, all six KubeJS-plugin mods
load, `6/6` startup scripts and `1/1` client scripts run with zero errors and zero warnings, and
GTCEu's own classes resolve through KubeJS in `worldgen_layers.js`. The game reaches the title
screen in under twenty seconds and looks entirely healthy.

The failure arrives only when a world is opened, and it presents as *"Caught error loading
resourcepacks, removing all selected resourcepacks"* — a message that names neither KubeJS nor
GTCEu. Anyone bumping the jar, watching the pack boot cleanly and then seeing a world fail to open
would reasonably conclude that KubeJS itself was at fault. That is how the version came to be
described as unbumpable.

## The decision

**The KubeJS version is a function of the GTCEu version. Treat it as GTCEu's constraint, and record
it against GTCEu.**

Concretely:

- Do not describe KubeJS as pinned. Describe `gtceu 7.0.2` as pinning it to `2101.7.1`.
- The ceiling moves when GTCEu moves. Upstream's `1.21` branch already imports
  `dev.latvian.mods.kubejs.plugin.builtin.event.ServerEvents`, but that fix ships in
  **`1.21-8.0.0 SNAPSHOT`**, not in any `7.x` release.
- Therefore raising KubeJS means a GTCEu **7.0.2 → 8.0.0** major bump, which pulls the `gcyr` fork
  (`gcyr-1.21.1-0.2.4+gt7.0.2`) along with it and re-opens the registration patches in
  [ADR-0003](0003-patch-gtceu-registration-in-our-gcyr-fork.md). That is a fork migration, not a
  jar swap, and it should be planned as one.
- A KubeJS addon that requires a newer KubeJS is still refused — but the reason to state is GTCEu's
  bytecode, not a property of KubeJS.

## What this costs

ProbeJS `8.x` requires KubeJS `[2101.7.2-build.365,)` and therefore cannot run until GTCEu moves.
The pack stays on ProbeJS `7.5.1`.

This is a smaller loss than it first appeared. ProbeJS `7.5.1` generates complete typings already —
375 `.d.ts` files. The pack's *absent* autocomplete was never caused by the version: it was a
literal `null` that ProbeJS writes into the `include` array of the `jsconfig.json` files it emits,
which makes tsserver reject the config outright, plus a missing `"types"` entry and no TypeScript
language server installed at all. Those are fixed independently, and editor support works on
`7.5.1`.

## How this is not repeated

The failure mode above was discovered three times and recorded once. Two things make the difference:

- The jar set is tracked, so a rolled-back jar leaves evidence. This was a flat snapshot when this
  ADR was written; ADR-0024 replaced it with a packwiz manifest, and a rollback is now `git revert`.
- `scripts/launch.py --headless` and `scripts/worldgen-check.py --headless` let the check run
  without a display, so "does this bump work" is a command anyone can run rather than a thing only
  reproducible at a particular desk.

**But note what nearly went wrong.** On `build.374` both existing checks passed:
`check-launch.sh` reported a clean launch, and `worldgen-check.py` reported *"worldgen registries
match"*. Minecraft caught the `NoClassDefFoundError`, dropped every resource pack and loaded the
world anyway; ore veins and worldgen layers are not touched by GTCEu's recipe hook, so the fixture
still matched. The bump would have been declared green.

What actually caught it was grepping `logs/latest.log` for exceptions. A check that asserts on
registries cannot see a subsystem that silently stopped running, so `check-launch.sh` now also
fails on a thrown `NoClassDefFoundError`/`ClassNotFoundException` and on
*"Caught error loading resourcepacks"* — the message Minecraft prints when it recovers by
discarding state. **A recovered error is still a failure.**

## Consequences

- The `kubejs-version-is-pinned` agent memory is rewritten to name GTCEu as the constraint.
- Issue #67 becomes a GTCEu 8 migration rather than a KubeJS jar swap.
- Any future "can we use newer KubeJS?" question is answered by checking GTCEu's version, and by
  running the headless worldgen check — which now reaches world load, where this class of breakage
  actually lives.
