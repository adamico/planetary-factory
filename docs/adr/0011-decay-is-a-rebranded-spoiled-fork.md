---
status: accepted
---

# Decay is a rebranded Spoiled fork that owns no content

`Mrbysco/Spoiled` (MIT, branch `multi/1.21`, NeoForge 1.21.1) already solves the tedious half of
Decay: walking block entities, handling containers, and surviving the edge cases a sweep hits in a
loaded world. It does not solve our half. `SpoilHandler.java:42` is
`if (level.dimension() != Level.OVERWORLD) return;` — hardcoded, no config — so on Sapros the mod
does nothing at all. That single line forces a fork; ADR 0010 forces the rest.

We fork it as **`respoiled`**, built the way ADR 0001 builds GCyR: a sibling clone outside this
repo, a personal GitHub fork, a manual build, and the jar dropped into a gitignored `mods/`. Only
the ADRs describing it are tracked here.

**The fork owns no content.** It knows the spanning tag and the `respoiled:spoil_recipe` type, both
datapack-supplied, and nothing about which materials spoil. KubeJS registers the four items, their
two-layer models and their lang entries. Adding a spoilable is a script edit, not a mod rebuild.

## Considered Options

- **Food Spoilage (CurseForge).** All Rights Reserved, no public source, food-oriented, and built on
  the continuous per-item freshness percentage ADR 0010 rules out. Its container-preservation
  multipliers already exist in Spoiled as `containerModifier` / `itemContainerModifier`, and we are
  deleting those anyway. Do not revisit.
- **Keep the mod ID `spoiled`.** Cheaper — upstream item IDs, recipe type and config paths stay
  put, and rebasing never has to reconcile a namespace. Rejected: the pack's glossary term is
  Decay, the mod is no longer recognisably Spoiled once the timer, the Overworld gate and the
  container modifiers are gone, and `respoiled:` in a recipe JSON says what it is.
- **`respoiled` registers the items itself, from a config listing materials.** One artifact,
  one place to look. Rejected: ADR 0010's stated ceiling is the four-textures-per-material
  authoring burden, and putting registration in Java makes every new spoilable a mod rebuild. It
  also grows the fork's diff, which is what makes upstream rebasing expensive.
- **An in-repo Gradle subproject instead of a sibling fork.** Rejected for consistency with
  ADR 0001; the diff here is surgical changes to someone else's mod, not a new mod.
- **KubeJS scripting instead of a fork.** ADR 0010 ruled this out on the grounds that KubeJS cannot
  register component types. That reasoning has since expired — the design has no component types.
  The fork survives on different grounds: the sweep, the chunk catch-up, the Purge route and the
  storage mixins are all Java-only. See the ADR 0010 amendment.

## The diff

Subtractive except where noted.

1. **Remove the Overworld gate.** Decay runs in every dimension, including in flight.
2. **Delete `SpoilTimer`** and its `DataComponentType`. `SpoilHelper.updateSpoilingStack` becomes a
   probability roll: one global 30-tick sweep, with each item's per-pass advance probability derived
   from its Factorio shelf life. Rolls are **binomial per item** over the stack, so a 64-stack
   splits; when there is no free slot to receive the split, the whole stack advances together as a
   fallback. It never stalls, and the coarseness self-limits the moment a slot frees.
3. **Cache `getSpoilRecipe` by item ID** at reload. The per-stack
   `getRecipesFor(SPOIL_RECIPE_TYPE, new SingleRecipeInput(stack), level)` is the cost driver. No
   sweep frequency is worth tuning until this lands; profile with `spark` afterwards.
4. **Per-chunk last-swept tracking** in a `SavedData`, with catch-up on chunk load. See ADR 0012.
5. **Delete `containerModifier` / `itemContainerModifier`.** No container alters the Decay rate.
   Containers are still walked, one level deep, so a shulker is not a fridge — nested-shulker is a
   Creative-mode edge case, not a supply chain.
6. **Hook `ItemEntity` tick.** The sweep walks block entities, so a dropped stack would otherwise be
   an unlimited fridge. Vanilla's 5-minute despawn is longer than iron bacteria's entire 60-second
   life, which makes the floor a strictly better container than a chest.
7. **Two storage mixins**, added. See ADR 0013.
8. **Entity results**, added. A spoil recipe may yield an entity instead of an item. It always
   fires, always clears the slot, and never blocks — eggs punish with mobs, everything else punishes
   with Clog. Inventory spoilage spawns at the player.

## Interaction with the Biochamber

A spoiled input Purges to a dedicated Spoilage output bus, which is a **structure requirement**: the
multiblock does not form without it. That turns a mysterious permanent Clog into a build-time error,
where GT already puts this class of mistake, and guarantees every formed Biochamber has somewhere to
Purge to. When the Purge bus is full the input blocks — that is the Clog, and it is Factorio's rule
verbatim. Once inputs are consumed and a cycle starts, the craft is immune.

## Consequences

We maintain a second fork. Updates are a rebase and a rebuild.

**No spoilable may have a fluid form.** A fluid has no item identity and therefore no Freshness, so
`Jelly → organic fluid → tank → back` launders freshness completely. There is no engine defence and
we are not building one: it is a constraint on the Biochamber's recipe surface, enforced by whoever
authors recipes. A future author reaching for a fluid intermediate on a spoilable is the reader this
paragraph exists for.

Item registration living in KubeJS means the engine can be correct while the pack has no spoilables
at all, which is exactly the state this ticket ships in.

The upstream Overworld-gate defect affects anyone using Spoiled with a dimension mod. A config-gated
fix is drafted at `docs/upstream/spoiled-dimension-gate.md`, not filed.
