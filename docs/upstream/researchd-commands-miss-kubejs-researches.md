# DRAFT — not filed

Target: `Porting-Dead-Mods/Researchd`, the `researchd` mod as shipped in `researchd-1.2.5.jar`
(`mod_version=1.2.5`, MC 1.21.1 / NeoForge 21.1.230). A bug report, not a PR — the fix touches the
mod's own command layer and the maintainers may prefer a different shape than the one sketched
below. Needs approval before filing: it posts to a third party's tracker under the maintainer's
GitHub account.

Duplicate check: not done. Search the tracker for "command", "unlock", "KubeJS" and "registry"
before filing. KubeJS support is a first-class feature of this mod, so someone driving researches
from scripts may well have hit this already.

Verify before filing: the code below is read from `Porting-Dead-Mods/Researchd@90ace36`, which is
what our local clone sits at. Re-check against the current head — `ResearchCommands`,
`ReloadableRegistryManager.apply` and `ResearchdManagers` — and confirm the behaviour on a clean
instance with a single KubeJS-declared research rather than on our pack, so the report does not
depend on anything of ours.

Not included: our pack-side workaround (a KubeJS `/pf research grant|revoke|list` command). It is a
workaround for this bug, not upstream material.

---

**Title:** `/researchd research unlock|remove` cannot see KubeJS-declared researches

### What happens

With a research declared through `ResearchdEvents.registerResearches` in KubeJS:

- `/researchd research unlock <team> all` returns silently, having unlocked nothing.
- `/researchd research unlock <team> <that research's id>` rejects the id as unknown.

Both forms of `remove` fail the same way. A datapack-declared research in the same world works
fine, so the commands themselves are not broken — they just cannot see the KubeJS half of the
registry.

### Why

`ResearchCommands` enumerates and resolves through the **vanilla datapack registry**:

```java
Stream<ResourceKey<Research>> researches = context.getSource()
        .registryAccess()
        .lookupOrThrow(ResearchdRegistries.RESEARCH_KEY)
        .listElementIds();
```

and the id form uses `ResourceOrTagArgument.resourceOrTag(context, ResearchdRegistries.RESEARCH_KEY)`
against that same registry.

But KubeJS researches never land there. `ReloadableRegistryManager.apply` merges them into the
manager's own `byName` map, after the datapack entries have been decoded:

```java
if (this.registry.equals(ResearchdRegistries.RESEARCH_KEY)) {
    Map<ResourceLocation, Research> kubeJSResearches = KubeJSCompat.getKubeJSResearches();
    for (Map.Entry<ResourceLocation, Research> entry : kubeJSResearches.entrySet()) {
        builder.put(ResourceKey.create(this.registry, entry.getKey()), (T) entry.getValue());
    }
    Researchd.LOGGER.info("Loaded {} KubeJS researches", kubeJSResearches.size());
}
this.byName = builder.build();
```

So `ReloadableRegistryManager#getByName` has both halves and the vanilla registry has only one, and
the commands read the one that is missing the KubeJS entries. Everything else in the mod — the
research screen, the lab, `ResearchManager` — goes through the manager, which is why this shows up
only on the commands.

The log shows both halves in a world where the commands find nothing:

```
Loaded 4 KubeJS researches
Loaded 4 entries for registry researchd:research
```

Four researches exist; the commands' stream is empty.

### Aggravating: it fails silently

`unlockAllResearches` iterates an empty stream and returns `0` without sending any feedback, so
`unlock ... all` looks like it ran. The id form at least errors, via the argument type. A
`source.sendFailure` on "resolved nothing" would have made this a five-minute diagnosis instead of
a source-reading session.

### Suggested fix

Have the commands read the same map as the rest of the mod:

- **Enumeration** (`unlock all` / `remove all`): replace the `registryAccess().lookupOrThrow(...)`
  stream with `ResearchdManagers.getResearchesManager(level).getLookup().keySet()`.
- **Resolution** (the id form): `ResourceOrTagArgument` is bound to a real registry and cannot see
  the manager's map, so it would need to become a custom argument type — a `ResourceLocation`
  argument validated against `getResearchesManager(level).getByName()`, suggesting its keys. That
  also gets tab-completion right, which today lists only the datapack researches.

A narrower alternative, if the mod would rather not touch the command layer: register KubeJS
researches into the datapack registry itself instead of merging them in afterwards, so that
everything reading `ResearchdRegistries.RESEARCH_KEY` sees one complete registry. That is the
larger change but it removes the whole class of bug, of which the commands are likely not the only
instance.

### Impact

For a pack that declares its research tree in KubeJS — the documented way to do it — there is no
command route to grant or revoke a research at all. That makes any in-world test of a research-gated
mechanic a full legitimate playthrough of the tree with no way to reset it afterwards.
