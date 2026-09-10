---
status: accepted
supersedes: [172]
---

# GregTech wins the item layer, and unification replaces rather than broadens

ADR-0017 settled which mod owns each *capability* — a table of blocks, one owner per rung. It said
almost nothing about which mod owns each *item*, beyond one paragraph: "Almost Unified stays,
restricted to raw materials. Ores, ingots, dusts, plates and gems unify. **Recipe types do not.**"
That paragraph names no winner. This ADR supplies the missing half and corrects two errors in it.

The first error is arithmetic: `config/almostunified/unification/materials.json` unifies **twelve**
tags, not five — `dusts, gears, gems, ingots, nuggets, ores, plates, raw_materials, rods,
storage_blocks, storage_blocks/raw_*, wires`. The prose was never reconciled with the config.

The second error is the one that shipped a bug.

## What Almost Unified actually does

**It replaces an ingredient. It does not broaden one. And it only reaches inside recipes.**

Every word there matters, and none of it is visible from any file in this repo:

- **Replaces.** `"item": "gtceu:iron_plate"` in an emitted recipe becomes `"item":
  "create:iron_sheet"` at load. The GT plate is not *also* accepted — it is accepted **nowhere**.
  The losing item becomes an orphan that no recipe in the pack will take.
- **Only inside recipes.** The jar ships a `GregTechModernRecipeUnifier` alongside its Create one,
  so the pack's `assembling/` and `chemical_plant/` GT recipes are rewritten like any other. Nothing
  outside a recipe is. A literal item id in KubeJS, in `planetaryfactory_core`, or in a research
  trigger is left exactly as written, pointing at whichever item unification just orphaned.

That combination is silent in both directions and it cost two separate evenings. The chain keeps
working — the furnace yields the sheet and the Assembling Machine takes the sheet — so nothing in
any log, any recipe check or EMI suggests a problem, while every non-recipe site naming the GT plate
has quietly become a reference to an item the pack cannot use.

**The observed failure.** `StartingKit.java` grants `gtceu:iron_plate` x8 and `gtceu:copper_plate`
x8 directly, which no unification touches. Confirmed in a running game: the kit arrives and its
sixteen items are inert — the opening hold, which is freeplay's debris chest read straight
across, could be spent on nothing. Rung 0 was broken from the first minute and looked perfect.
`#206`'s two research gates hit the same wall from the other side and were worked around by
re-pointing them at `create:*_sheet`, a workaround this ADR removes.

**Steel was the tell.** `gtceu:steel_plate` behaved correctly throughout, for the single reason that
Create ships no steel sheet, so there was nothing to lose to. The pack therefore held two classes of
plate with opposite liveness and nothing marking which was which.

## The rule

**One item per material, GregTech's, and the priority list is a decision this repo makes.**

`mod_priorities` becomes `minecraft, kubejs, gtceu, create`.

- **`gtceu` is added**, which is the whole fix. It was absent, so Create won every tag it appeared
  in by default rather than by choice.
- **`mekanism` is dropped.** The mod left the pack under ADR-0035; the entry has been dead since.
- **`minecraft` stays first, deliberately rather than by inheritance.** Vanilla keeps `c:ingots/*`,
  `c:ores/*`, `c:raw_materials/*` and `c:nuggets/*`. This costs the pack nothing — ADR-0032 smelts
  ore 1:1 to plate with no ingot step, so the ingot tags gate nothing — and vanilla winning
  `c:ores/*` is what keeps Terra's ore blocks vanilla-shaped.
- **`kubejs` is inert and stays where it is.** `items.js` registers fourteen items and applies no
  tags, so no KubeJS item carries a `c:` material tag. The position is recorded as harmless rather
  than as meaningful.
- **GregTech beats Create** on every tag where both appear. GT's `TagPrefix` emits eleven of the
  twelve — every one except `wires`, which is Power Grid's alone and which adding `gtceu` therefore
  cannot touch.

**And the general rule the failure teaches, which outlives this ordering:**

> Any site naming a literal item id **outside recipe JSON** must name the priority winner for that
> item's `c:` tag. Unification will not fix it, and nothing will report it.

Two such sites exist today — `researchd.js`'s `has:` and `icon:` ids, and `StartingKit.java` — and
both were wrong. The check named below is what keeps that at two-and-correct rather than growing.

## Why GregTech and not Create

Create winning was the live alternative, and it is the status quo, so it needed beating rather than
merely differing from.

- **Steel decides it.** Create ships no steel sheet and never will. A Create win guarantees the pack
  ships two different nouns for one concept permanently — copper and iron are sheets, steel is a
  plate — and that split is visible to the player in a way the current one is not.
- **It makes the repo's own text true.** `recipe/copper_plate.json` says `gtceu:copper_plate` and
  the player holds `gtceu:copper_plate`. Every emitted recipe, every `data/pack/item-map.json`
  target and the whole corpus converter already name `gtceu:`. Under a Create win those files stay
  as written and are rewritten at load — which is exactly the invisibility this ADR exists to end,
  preserved as policy.
- **It repairs the kit by doing nothing to the kit.** The sixteen items become live untouched.
- **GT plates are material items** with a registry behind them; Create's sheets are three hand-made
  items. `gtceu:steel_plate` exists at all only because GT generates it for the Steel material.

## Consequences

- **`create:*_sheet` become unobtainable**, which is correct and already true in substance:
  ADR-0034's sweep removes Create's own recipes, and `create-substitutions.json` re-authors the
  kinetic line onto the pack's Assembling Machine.
- **`#206`'s two `has:` ids revert** to `gtceu:copper_plate` and `gtceu:iron_plate`, and the long
  comments arguing for the sheets go with them.
- **`c:plates/zinc` needs no exemption.** Power Grid registers a zinc plate and ships the tag, so
  adding `gtceu` may hand the tag to GT — but `grid-substitutions.json` substitutes
  `#c:plates/zinc → #c:plates/copper` under ADR-0021, so **no emitted recipe names a zinc item** and
  unification of a tag nothing references rewrites nothing. Recorded as a reason and not as an
  `ignored_tags` entry, so that a future recipe naming zinc fails loudly instead of inheriting a
  silent exemption.
- **Two committed rationales were arguing from the false premise** and are corrected with this ADR:
  `create-substitutions.json` ("AlmostUnified makes it interchangeable with `create:iron_sheet`
  anyway", and the block at its head reading "the pack's smelted iron plate IS Create's iron sheet
  in the running game") and the same reading in `grid-substitutions.json`. Both files' *targets* were
  always right and are now more right; only their reasoning was wrong. This is why `#172` is
  superseded: its conclusion stands, but a reader of it learns something untrue about how
  unification behaves.
- **`recipe_viewer_hiding` and `emi_strict_hiding` stay `true`.** With GT winning, EMI shows the
  item the player receives, which is what those settings are for.
- **The Almost Unified debug dumps go on** — they were all six `false`, which is why none of this was
  visible outside a running game. No check may depend on them: a dump is the output of a game launch
  and every check in this repo is deliberately launch-free.

## The check

Static, launch-free, and it asserts the general rule rather than this instance: **every literal item
id outside recipe JSON names a mod that `mod_priorities` ranks at or above every other mod holding
an item in that id's `c:` tag.** It re-derives the winner from the installed jars and GT's
`TagPrefix`, so a GregTech or Create update that changes a tag's membership fails the check.

- `tests/factorio/test_research_unlocks.py` — `researchd.js`'s `has:` and `icon:` ids. That file
  already covers `unlocks:` ids and never covered these.
- `tests/pack/test_starting_kit.py` — the hold's ids.

The weaker assertion that was originally proposed — that each id resolves to a registered item —
is not sufficient and is not what ships. It catches only the `minecraft:air` silent default
(`checkItemPresence` resolves through a `DefaultedRegistry`), and it would have passed on all
sixteen dead kit items.

Whether the kit is spendable and the gates fire is a world load. Testing a research trigger needs a
full **restart**, not `/reload` — Researchd's registry re-fires the KubeJS event and can read the
previous script evaluation — and a **dequeue/requeue**, because the queued `ResearchProgress$Task`
captures its ingredient when queued. Without both, a correct id is indistinguishable from a wrong
one; that is how the first reading of this bug came to be retracted.
