#!/usr/bin/env python3
"""Assert the Personal Assembler is offered both science pack recipes (#222, ADR-0052).

Researchd's science packs are all one item, `researchd:research_pack`, told apart by a data
component. The Assembler names an item by a string, and before ADR-0052 that string was the bare
registry id -- so `RuntimeHandRecipes` refused every component-bearing stack rather than fold the
four packs onto one another, and both emitted science pack recipes simply did not exist in the
Assembler: no plan, no `Missing`, no locked entry, and nothing wrong with the recipes themselves.

The key is now the id together with the component patch, so the refusal is gone and the recipes are
admitted. What is checkable here, with no game launch:

  - `automation_science_pack` and `chemical_science_pack` are emitted, carry `factorio_category:
    crafting`, and so are in the hand set. Those two by name: whether logistic and production are
    hand-craftable is the corpus and #25's ladder talking, not this
  - every component-bearing output in the hand set is REPRESENTABLE as a key -- one item, a patch
    that is a flat map of component id to value. An output the key format cannot name is refused at
    the graph, which is the same "recipe missing from the Assembler" this ticket was

WHAT IT CANNOT PROVE is that the `RecipeGraph` admits them: that is a running server, and its
absence of a refusal line is the human-on-delivery check. The identity itself is asserted in
`mod/src/test/java/com/planetaryfactory/core/assembler/ItemKeyTest.java`.

Usage: tests/factorio/test_science_packs.py
"""
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent.parent
EMITTED = ROOT / "kubejs/data/planetaryfactory/recipe"

HAND_CATEGORY = "crafting"

# The two #222 named. Both are craftable before any research exists, which is Factorio's own
# bootstrap -- red science is craftable from the first minute -- and gating a science pack behind
# research it is the input to would be a cycle (ADR-0052).
REQUIRED = ("automation_science_pack", "chemical_science_pack")

SCIENCE_PACK_ITEM = "researchd:research_pack"

failures = []


def check(ok, message):
    if not ok:
        failures.append(message)


def hand_recipes():
    """Every emitted recipe in the hand category, by file stem."""
    found = {}
    for path in sorted(EMITTED.rglob("*.json")):
        recipe = json.loads(path.read_text())
        if (recipe.get("data") or {}).get("factorio_category") != HAND_CATEGORY:
            continue
        found[path.stem] = (path, recipe)
    return found


def outputs(recipe):
    for entry in (recipe.get("outputs") or {}).get("item") or []:
        yield (entry.get("content") or {}).get("ingredient") or {}


def main():
    recipes = hand_recipes()
    check(recipes, "no emitted recipe carries factorio_category: %s -- re-run the converter"
                   % HAND_CATEGORY)

    for name in REQUIRED:
        check(name in recipes,
              "`%s` is not in the hand set: the Personal Assembler cannot plan it, and #222 is "
              "exactly the recipe being absent from the Assembler with nothing wrong with the "
              "recipe itself" % name)
        if name not in recipes:
            continue
        path, recipe = recipes[name]
        items = [out.get("items") or out.get("item") for out in outputs(recipe)]
        check(SCIENCE_PACK_ITEM in items,
              "%s no longer outputs %s (%s) -- if the pack moved off Researchd's componentised item "
              "this check is asserting the wrong thing" % (path.name, SCIENCE_PACK_ITEM, items))

    # Every component-bearing output in the whole hand set, not just the two: a key is an item id
    # plus a flat patch, and an output the format cannot name is refused at the graph and vanishes
    # from the Assembler exactly the way these two did.
    componentised = 0
    for name in sorted(recipes):
        path, recipe = recipes[name]
        for out in outputs(recipe):
            components = out.get("components")
            if components is None:
                continue
            componentised += 1
            check(isinstance(out.get("items"), str),
                  "%s names a component-bearing output with `items` = %r: a plan cannot promise an "
                  "item it cannot name, so this is refused at the graph"
                  % (path.name, out.get("items")))
            check(isinstance(components, dict) and components,
                  "%s carries an empty or non-map component patch (%r)" % (path.name, components))
            for component, value in (components or {}).items():
                check(":" in str(component),
                      "%s patches `%s`, which is not a component id -- the key writes it verbatim "
                      "and vanilla's parser will not read it back" % (path.name, component))
                check(value is not None,
                      "%s sets `%s` to null; a removal is a different thing and the converter emits "
                      "none" % (path.name, component))

    check(componentised >= len(REQUIRED),
          "found %d component-bearing output(s) in the hand set, expected at least the %d science "
          "packs" % (componentised, len(REQUIRED)))

    for failure in failures:
        print("FAIL: " + failure)
    if failures:
        print("\n%d failure(s)" % len(failures))
        return 1
    print("ok: %d hand recipe(s) emitted, %d component-bearing output(s), both science packs present"
          % (len(recipes), componentised))
    return 0


if __name__ == "__main__":
    sys.exit(main())
