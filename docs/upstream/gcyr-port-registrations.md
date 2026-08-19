# DRAFT — not filed

Target: `argentmatter/gcyr` (upstream of our fork), `1.21.1` branch. Two items: a PR for the fixes
and, optionally, an issue describing the state of the port. Needs approval before filing.

Duplicate check: not done. The 1.21.1 branch is unreleased and its last commits are "It builds?
Yes. It crashes at launch? Yes." and "Remove kjs, fix gcyr" (2025-09-04); check open PRs and the
eighteen forks first — someone may have carried this further.

Our commit with all three fixes: `adamico/gcyr@b258df8`.

---

**Title:** 1.21.1: register the content the port left unwired, and drop a duplicate registration

### Context

The 1.21.1 branch compiles but several registrations were never reconnected after the port, so the
mod loads with most of its content missing. This was masked by a GTCEu bug — GT discards its
pending-registration queue, taking the duplicate registration with it — so the symptoms only appear
once GT registers correctly. Filed separately against GregTech-Modern.

### 1. Recipe types are registered twice

`GCYRRecipeTypes.register` registers each type through both a `DeferredRegister` and
`GTRegistries`:

```java
RECIPE_TYPES.register(name, () -> recipeType);
GTRegistries.register(BuiltInRegistries.RECIPE_TYPE, recipeType.registryName, recipeType);
```

Both target `BuiltInRegistries.RECIPE_TYPE`. With GT's queue honoured this throws
`Adding duplicate key 'ResourceKey[minecraft:recipe_type / gcyr:oxygen_spreader]'`. The
`GTRegistries` call is redundant; removing it is enough.

### 2. Recipe conditions never register

`GCYRRecipeConditions.init()` is referenced only from a commented-out 1.20-era handler in
`GCYR.java`, so `gcyr:dyson_sphere` and `gcyr:orbit` are never registered. Any recipe using them
fails to serialise with `Unregistered holder in ResourceKey[minecraft:root / gtceu:recipe_condition]`.

### 3. Machines, dimension markers and sounds never register

`GCYRMachines.init()`, `GCYRDimensionMarkers.init()` and `GCYRSoundEntries.init()` are called from
nowhere in the branch — the same commented-out block was their only reference. Every machine is
therefore unbound; loading a world fails with
`Trying to access unbound value: ResourceKey[minecraft:item / gcyr:iv_oxygen_spreader]`.

### Where the calls belong

Sounds initialise safely in the constructor. The GT-dependent ones do not:

- **Not in the constructor.** Machine definitions read GTCEu's casing blocks, which do not exist
  yet — `NullPointerException: null key in entry: null=gtceu:block/casings/solid/...`.
- **Not in `gtInitComplete()`.** Despite the javadoc ("This runs after GTCEu has set up its
  content"), `CommonInit.init` calls it from GTCEu's *constructor*, before GT registers anything;
  putting machine init there makes GTCEu itself fail construction. The javadoc is worth correcting
  upstream in GregTech-Modern.
- **On the first `RegisterEvent`,** with a once-guard. GTCEu is constructed before GCyR, so its
  content exists by then, and this is still ahead of the vanilla block registry, so the machines
  register normally.

### Verification

With these three fixes and the GTCEu workaround, the pack reaches a world: 39324 recipes, no
registry errors, GCyR machines bound, GT worldgen present. Tested on Minecraft 1.21.1, NeoForge
21.1.248, GTCEu 7.0.2, KubeJS 2101.7.1-build.181.

### Also worth fixing

`kubejs.plugins.txt` still names `argent_matter.gcyr.integration.kjs.GCYRKubeJSPlugin`, deleted in
"Remove kjs, fix gcyr", so KubeJS logs a `ClassNotFoundException` on every launch. Deleting the
file silences it; restoring the bindings is a larger piece of work.
