---
status: accepted
---

# Freshness is item identity, because stacks cannot be merged on our terms

Factorio's spoilage is a continuous percentage carried by every item, and it stays usable because
combining items averages their freshness — a stack of 10 at 50% plus one at 100% becomes 11 at 54.5%.
That averaging is what stops a warehouse from filling with unmergeable partial stacks.

Minecraft gives us no way to do it. `ItemStack.isSameItemSameComponents` is `static`, compares the two
`PatchedDataComponentMap`s directly, and has no `Item` dispatch and no NeoForge hook;
`IItemStackExtension` exposes 41 methods and none affects stacking; `ItemHandlerHelper.canItemStacksStack`
was removed in the component rewrite; and no merge or insertion event exists. Worse, averaging has to
run where the counts change, and `grow()`/`setCount()` do not know the donor stack — so every merge
site needs patching individually. Vanilla has around 19. **GTCEu reimplements the arithmetic in 24
more classes**, and AE2 and Create have their own storage layers again.

So a per-stack freshness value fragments stacks in proportion to how many distinct values exist —
1200 of them for a two-hour bioflux — and the one mechanism that would prevent it is unavailable.

**A spoilable material is therefore four registered items, not one item with a freshness value.**
Fresh, Ripe, Stale and Spoiling are siblings; Decay replaces one with the next; the last is replaced
by Spoilage, or for iron and copper bacteria by ore. Recipes reference a tag spanning all four.

Fragmentation becomes exactly four, and it becomes *legible* — four variants with names, textures and
tooltips read as information, where 1200 invisible timer values read as inventory noise.

Three things fall out for free. Nothing is stored on the stack, so there is no component to fragment,
no merge to intercept, and no Mixin fight with `recipeessentials` over `DataComponentMap.equals`.
Recipes see freshness natively in **every** mod, because tags and item IDs work identically in Create,
Mekanism, GregTech and Integrated Dynamics — no integration code, and no risk of enforcing the
mechanic unevenly across the pack's optional processing paths. And with no stored timer, advancement
must be probabilistic, which four sequential stages turn from an exponential distribution into an
Erlang-4 one — halving the spread, so the stage count doing the legibility work is the same stage
count doing the variance reduction.

## Considered Options

- **A per-stack freshness value with averaging on merge.** The faithful reproduction, and the reason
  this ADR exists. Ruled out above: the merge sites are scattered across vanilla and every mod's own
  storage layer, and a partial implementation is worse than none — freshness would average correctly
  in a chest and silently reset when a GT pipe touched it, a rule the player cannot learn because it
  holds only sometimes. It also breaks the `hashItemAndComponents` contract that `ItemStackLinkedSet`
  and `RecipeCache` rely on.
- **A quantised stamp with no averaging.** Bucketing a timestamp into eight epochs caps fragmentation
  at eight rather than 1200. Better, but the variants remain meaningless to the player — two stacks of
  jelly that differ by an invisible tick count and refuse to combine.
- **A single probabilistic stage, no data at all.** Zero fragmentation and trivially cheap, but the
  lifetime is exponential: memoryless, ~63% of items dead before nominal, and an unbounded tail. Too
  random for a mechanic whose entire tension is time pressure. Four stages keep the zero-data property
  and fix the distribution.
- **Lazy resolution on access instead of a sweep.** The original research recommendation. There is no
  machine-boundary hook that generalises past GT multiblocks — `recipeModifier` and `beforeWorking`
  exist only on `MultiblockMachineBuilderWrapper` — so an item would enter a Create or Mekanism recipe
  unresolved.
- **Freshness inheritance through crafting, and duration modulation on stale input.** Both are
  Factorio-faithful and both would need separate implementations in four mods' recipe systems. Uneven
  enforcement does not merely under-deliver, it inverts the design: it silently biases which
  processing path a player picks.

## Consequences

Every spoilable material costs four item registrations, four textures, four lang entries and a tag.
That is a real authoring burden, and it puts a ceiling on how many spoilable materials the pack can
carry — a ceiling Factorio does not have.

An item's remaining life is bounded but not exactly knowable. Erlang-4 means jelly's nominal 240
seconds lands roughly between 120 and 400. Ours is not a deterministic mechanic and cannot be
presented as one; a player who wants a precise clock will not find one. More stages would tighten the
spread at the cost of more variants, and that dial is available later.

Freshness does not propagate through crafting. A product's freshness is whatever its recipe declares,
which is a spec obligation on every recipe producing a spoilable, and a visible departure from
Factorio that players familiar with Gleba will notice.

Reversing this is expensive once recipes reference the four item IDs and the spanning tag — every
recipe, tag and piece of worldgen touching organics would have to be rewritten. The decision is
effectively load-bearing from the first Sapros recipe onward.

The mechanic needs a Java fork of Mrbysco/Spoiled (MIT), not KubeJS scripting. This narrows the
research ticket's framing that "the mechanic will be ours": it still is, but it is a fork of an
existing MIT mod rather than a script, because KubeJS cannot register component types and Spoiled's
sweep already solves the block-entity walking, chunk handling and container edge cases we would
otherwise reimplement badly.

## Amended after the grilling session for issue #17

Two things this ADR asserted have changed. Neither disturbs the decision itself — freshness is still
item identity — but both change what it costs and why the fork exists.

**The authoring ceiling is lower than stated.** "Four textures per material" is not the shape it
takes. A spoilable is drawn as a two-layer item model: `layer0` is the material's one texture, and
`layer1` is a stage badge drawn from a single shared set of four — green, yellow, orange, red, in the
manner of Food Spoilage. Fresh carries the green badge rather than being bare, so the badge is a
positive statement that this item is subject to Decay at all. The set is shared by every spoilable in
the pack, so the badges are authored once and a new material costs **one texture**, four generated
model JSONs and four lang entries.

An abstract status badge also sidesteps a problem a material-specific overlay would have had: a wilt
or mould motif reads correctly on jelly and absurdly on a bacterial culture or an egg. A coloured pill
carries no material semantics and is legible on all of them.

**The reasoning for needing a Java mod has expired, though the conclusion holds.** This ADR concluded
with "the mechanic needs a Java fork of Mrbysco/Spoiled, not KubeJS scripting, because KubeJS cannot
register component types." The design has no component types — that was the whole point of it — so
the premise no longer applies, and KubeJS *can* register the four items.

It now does. **KubeJS registers the items, their models and their lang entries; the fork registers no
content at all.** The fork survives on entirely different grounds: the sweep, the chunk catch-up, the
Purge route and the storage-blacklist mixins are Java-only, and none of them are content. See
ADR 0011, ADR 0012 and ADR 0013.

The consequence is a better division than the one this ADR anticipated. The engine can be complete
and correct while the pack contains no spoilable materials whatsoever, and adding one is a script
edit rather than a mod rebuild.
