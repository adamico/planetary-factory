#!/usr/bin/env python3
"""Assert the corpus admits a Crafting Plan for every hand-craftable recipe (#161, ADR-0038).

The Personal Assembler resolves a plan before it crafts anything: requesting a recipe whose
ingredients you lack queues the sub-crafts, recursively, until every branch bottoms out in
something the player can mine, smelt or machine-make. That resolver is a pure function over the
corpus and an inventory, so the half that depends only on the corpus is checkable here -- with no
Java and no game launch -- and it is the half that can be broken by a data regeneration nobody
looked at.

What is asserted:

  - the hand set is Assembling Machine 1's, derived: first category `crafting`, which is 113
    recipes and which already excludes Factorio's eleven fluid-free withholds (#88). The count is
    pinned because the predicate is prose everywhere else, and a regeneration that moves a recipe
    into or out of `crafting` silently changes what the Assembler can make
  - no item has two hand recipes, so the resolver never chooses between routes. ADR-0038 relies on
    this: a resolver that had to choose would need a cost model, and there is none
  - every one of the 113 resolves -- the walk terminates, with no cycle and no intermediate that is
    neither hand-craftable nor a known leaf
  - the leaves are exactly the 21 named below

WHAT IT CANNOT PROVE is that the *runtime* graph agrees with the corpus. The mod resolves against
the recipes actually loaded, which the converter emits, and the converter skips a Factorio name
whose `item-map.json` row is `undecided`. A recipe the converter has not emitted yet is simply not
offered by EMI, so it cannot be planned at all -- that is a coverage gap, not a broken plan, and
`tests/factorio/test_recipe_convert.py` is where emission is asserted. What this check owns is that
the *design* terminates, which is the property a regeneration can silently destroy.

Usage: tests/factorio/test_hand_resolver.py
"""
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent.parent
CORPUS = ROOT / "data/factorio/recipe.json"

# Assembling Machine 1's set, and the Personal Assembler's by ADR-0038: it has no recipe type of
# its own, so "hand-craftable" is a predicate over this field and nothing else.
HAND_CATEGORY = "crafting"

# Pinned because the predicate is otherwise only prose. `tests/factorio/test_subgroup_owner.py`
# derives the same number from the other direction -- the shelves the set spreads across -- so a
# regeneration that changes it fails in two places with two different explanations.
EXPECTED_HAND_RECIPES = 113

# Where every plan bottoms out: ingredients of a hand recipe that no hand recipe makes. Each is
# mined, smelted, or made by a machine, and each is therefore `Missing` rather than `To Craft` when
# the player has none -- the distinction the plan dialog exists to draw.
#
# Listed so it can be READ, and derived below rather than matched against, so this stays
# documentation and cannot quietly become the source of truth.
EXPECTED_LEAVES = {
    "battery", "carbon", "coal", "concrete", "copper-plate", "electric-engine-unit",
    "engine-unit", "ice", "iron-plate", "plastic-bar", "processing-unit",
    "productivity-module", "refined-concrete", "speed-module", "steel-plate", "stone",
    "stone-brick", "sulfur", "uranium-235", "uranium-238", "wood",
}

failures = []


def check(condition, message):
    if not condition:
        failures.append(message)


def hand_recipes(corpus):
    """The hand set, keyed by name."""
    return {r["name"]: r for r in corpus if r.get("category") == HAND_CATEGORY}


def makers(recipes):
    """Which hand recipe produces each item, as item -> [recipe name]."""
    by_item = {}
    for name, recipe in recipes.items():
        for result in recipe.get("results") or []:
            by_item.setdefault(result["name"], []).append(name)
    return by_item


def resolve(item, recipes, by_item, path, leaves):
    """Walk one branch, recording leaves. Returns the cycle as a list, or None.

    Depth-first with the ancestors carried rather than a visited set: a cycle is a repeat on the
    *current* path, and an item legitimately appears in many branches of one tree.
    """
    if item not in by_item:
        leaves.add(item)
        return None
    if item in path:
        return path[path.index(item):] + [item]
    for ingredient in recipes[by_item[item][0]].get("ingredients") or []:
        cycle = resolve(ingredient["name"], recipes, by_item, path + [item], leaves)
        if cycle:
            return cycle
    return None


def main():
    corpus = json.loads(CORPUS.read_text())
    recipes = hand_recipes(corpus)

    check(len(recipes) == EXPECTED_HAND_RECIPES,
          "the hand set is %d recipes, expected %d -- category `%s` is the whole definition of what "
          "the Personal Assembler can make (ADR-0038), so this number moving means the surface did"
          % (len(recipes), EXPECTED_HAND_RECIPES, HAND_CATEGORY))

    by_item = makers(recipes)
    for item, names in sorted(by_item.items()):
        check(len(names) == 1,
              "`%s` has %d hand recipes (%s) -- the resolver picks a route without a cost model, so "
              "two routes for one item has no defined answer (ADR-0038)"
              % (item, len(names), ", ".join(sorted(names))))

    leaves = set()
    for name in sorted(recipes):
        for result in recipes[name].get("results") or []:
            cycle = resolve(result["name"], recipes, by_item, [], leaves)
            check(cycle is None,
                  "resolving `%s` cycles: %s -- a plan that cannot terminate is one the Assembler "
                  "would hang resolving" % (name, " -> ".join(cycle or [])))

    for leaf in sorted(leaves - EXPECTED_LEAVES):
        check(False,
              "`%s` is a new leaf: a hand recipe wants it and no hand recipe makes it, so every "
              "plan through it now reports Missing. Add it to EXPECTED_LEAVES once something mines, "
              "smelts or machine-makes it" % leaf)
    for leaf in sorted(EXPECTED_LEAVES - leaves):
        check(False,
              "`%s` is no longer a leaf -- either a hand recipe now makes it, or nothing wants it. "
              "Drop it from EXPECTED_LEAVES" % leaf)

    for failure in failures:
        print("FAIL: " + failure)
    if failures:
        print("\n%d failure(s)" % len(failures))
        return 1
    print("ok: %d hand recipe(s), %d item(s) hand-made, %d leaf/leaves, no cycles"
          % (len(recipes), len(by_item), len(leaves)))
    return 0


if __name__ == "__main__":
    sys.exit(main())
