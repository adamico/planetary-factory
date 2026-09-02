# The research unlock check

`tests/factorio/test_research_unlocks.py` asserts that every recipe id a research grants is a
recipe the pack actually emits. It is a static data check in `docs/testing/what-to-check.md`'s
terms — cross-file references resolve — and launches no game.

## The failure it catches

Researchd's `unlockRecipes` effect gates by recipe **id**, and a recipe's id is derived from its
type. Change a recipe's surface — the move #97 makes mandatory, since no pack recipe may be a
vanilla grid recipe once the crafting table is gone — and its id changes with it. The research
keeps its lock on the old id.

Nothing reports this. Researchd does not error on an id that resolves to nothing, the world load
counts no failed recipe, and no line appears in any log. It reaches the player as a completed
research that unlocks nothing, which is indistinguishable from a research that was meant to unlock
nothing.

## What it asserts

- every `unlocks` entry in `researchd.js` names a recipe under
  `kubejs/data/planetaryfactory/recipe/`, keyed as a datapack keys it: `<namespace>:<path>`
- a `fromFactorio` call, or an `unlocks` array, that the parser cannot read is a **failure**, not a
  skip. An unlock read by nothing is exactly the unchecked coupling the check exists to catch
- comments are stripped before parsing, so the file's own prose about `fromFactorio()` is not
  mistaken for a declaration

`researchd.js` is parsed rather than imported: it is a KubeJS script whose only reader is Rhino,
and its `fromFactorio` is defined in a sibling script that exists only inside a running game. This
is the same discipline `tests/factorio/test_recipe_sweep.py` applies to the survivor allowlist.

## Deliberate strictness

**A foreign-namespace unlock id fails.** A surviving stock recipe (ADR-0034's allowlist) belongs to
a third-party mod and cannot be resolved statically. Gating research on one is a decision that
should be recorded — an entry in the allowlist's reasoning, or an override — rather than a case
this check waves through because it cannot see it.

**It passes on an empty set.** `researchd.js` currently declares no `unlocks`; the research tree is
provisional and due to be harvested. The check landed ahead of #138, the first ticket to write
unlock ids in volume, so the guard exists before there is anything to guard.

## What it cannot prove

That Researchd accepted the lock in a running game — that the recipe manager holds the id at the
moment the research is registered. That is #138's world load: 0 failed recipes and 0 unresolved
research names.

## Its other half

`tests/factorio/test_recipe_sweep.py` carries #97's first half: nothing the pack emits, and nothing
the survivor allowlist admits, is a vanilla `crafting_shaped` or `crafting_shapeless` recipe. The
two are the same rule read from either end, and the sweep check is where the grid-type assertions
stay.
