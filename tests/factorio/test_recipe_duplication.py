#!/usr/bin/env python3
"""Assert no item is made by two recipes unless a decision says it is.

Every other recipe check in this directory owns ONE subtree and reads ONE input table: the Factorio
converter's, the grid converter's, the Create converter's, the hand-written pack subtree's. That is
the right shape for asking "did this converter do its job", and it is blind to the one question
none of them can ask -- whether two converters, or one converter twice, made the same item.

The failure is quiet in the way that matters. Two routes to one block is not an error, does not
fail a schema, appears in no log, and loads perfectly. It reaches the player as two entries in EMI
for the same thing, and if both carry `factorio_category: crafting` it also reaches the Personal
Assembler's resolver, which picks a route with no cost model and therefore cannot choose between
them (`test_hand_resolver.py` asserts that property over the Factorio corpus; this asserts it over
what is actually emitted). It shipped once: Create's two gearbox conversions and the large
cogwheel's second route were emitted alongside the direct recipes they duplicate, and every
subtree-local check passed.

WHAT IT ASSERTS

  - every item emitted by more than one recipe is named in `MULTI_ROUTE` with the reason it earns
    a second route. Anything else is a duplicate
  - a `MULTI_ROUTE` row that no longer has two routes is removed. A row nobody reads is a rule a
    converter change left behind, and it would silently re-admit a duplicate later
  - at most one route per item is hand-craftable, wherever the routes come from

WHAT IT IS NOT. It is not a check that the routes are BALANCED -- two routes at wildly different
costs is a progression escape, and costing is a decision no static check can make. It only asserts
that a second route was chosen by somebody.

Usage: tests/factorio/test_recipe_duplication.py
"""
import collections
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent.parent
EMITTED = ROOT / "kubejs/data/planetaryfactory/recipe"
# The pack's furnace type (#155): a count-bearing smelt, whose output is shaped differently.
PACK_SMELTING = "planetaryfactory:smelting"

# Recipe types GregTech re-registers, and therefore the ones whose FILE PATH is not a free choice.
# `RecipeManagerLateMixin` strips everything before the first `/` of a loaded GTRecipe's id and
# `GTRecipeBuilder.save` puts the recipe type's own path back on the front (#87). The round trip
# closes only for a file already under a directory named after its type: a recipe at
# `recipe/grid/copper_coil.json` loads as `planetaryfactory:grid/copper_coil` and is re-registered
# as `planetaryfactory:assembling/copper_coil`, leaving BOTH ids in the recipe manager with
# identical inputs and outputs. That is invisible to every other check here -- the file is valid,
# the sweep keeps it, and `ServerEvents.recipes` runs BEFORE the re-registration, so even a probe
# inside the recipe event sees one recipe. It reaches the player as two EMI entries, and it shipped
# for all 80 recipes of the grid subtree, the 9 of the Create subtree and the 2 hand-written picks.
GT_NAMESPACE = "gtceu"

# `planetaryfactory:smelting` is the pack's own recipe class (#155), not a GTRecipe, so it is not
# cloned and its files stay flat. Recorded rather than assumed: if it ever moves onto a GT type its
# four recipes start duplicating, and this line is where someone will look.
FLAT_TYPES = {"planetaryfactory:smelting"}

# Items an emitted recipe is allowed to make twice, and why. A row here is a DECISION: it says the
# second route earns its EMI entry. The default is one route per item, because under ADR-0034's
# default-deny sweep every recipe in the game is one the pack chose to author.
MULTI_ROUTE = {
    "planetaryfactory:solid_fuel": (
        "Factorio's own three routes -- heavy oil, light oil and petroleum gas each make solid "
        "fuel, and which one is worth running is the whole point of the oil line. ADR-0031 says "
        "the corpus authors what it contains, and it contains all three."),
    "powergrid:generator_commutator": (
        "Direct, plus `generator_commutator_from_conversion`. The conversion is kept because it "
        "is the ONLY route to `powergrid:generator_vertical_commutator`, its other output side "
        "of the pair -- dropping it would make that block uncraftable. See `machine_only` in "
        "data/pack/grid-substitutions.json (#172)."),
    "powergrid:generator_housing": (
        "Direct, plus `generator_housing_from_conversion`, kept for the same reason: the "
        "conversion is the only route to `powergrid:vertical_generator_housing`."),
    "powergrid:voltage_gauge": (
        "Direct, plus `voltage_gauge_from_conversion`, kept for the same reason: the conversion "
        "is the only route to `powergrid:current_gauge`."),
}

failures = []


def check(condition, message):
    if not condition:
        failures.append(message)


def ingredient_name(entry):
    if "item" in entry:
        return entry["item"]
    if "tag" in entry:
        return "#" + entry["tag"]
    return None


def outputs_of(recipe):
    """The item names a recipe produces. Fluids are out of scope -- nothing duplicates one."""
    if recipe.get("type") == PACK_SMELTING:
        return [recipe["result"]["id"]]
    names = [ingredient_name(entry["content"]["ingredient"])
             for entry in recipe.get("outputs", {}).get("item", [])]
    return [name for name in names if name is not None]


def main():
    check(EMITTED.is_dir(), "kubejs/data/planetaryfactory/recipe/ does not exist")
    if not EMITTED.is_dir():
        return report()

    routes = collections.defaultdict(list)
    for path in sorted(EMITTED.rglob("*.json")):
        recipe = json.loads(path.read_text())
        where = path.relative_to(EMITTED).as_posix()
        hand = recipe.get("data", {}).get("factorio_category") == "crafting"
        for item in outputs_of(recipe):
            routes[item].append((where, hand))

    total = sum(len(paths) for paths in routes.values())

    # The file-path invariant. This is the duplicate nothing else can see.
    for path in sorted(EMITTED.rglob("*.json")):
        recipe = json.loads(path.read_text())
        recipe_type = recipe.get("type", "")
        where = path.relative_to(EMITTED).as_posix()
        if recipe_type in FLAT_TYPES:
            continue
        if not recipe_type.startswith(GT_NAMESPACE + ":"):
            continue
        type_path = recipe_type.split(":", 1)[1]
        first = where.split("/")[0] if "/" in where else None
        check(first == type_path,
              "%s is a %s recipe, but its first path component is %s. GregTech re-registers every "
              "loaded GTRecipe under its OWN type path (#87), so this file lands in the recipe "
              "manager twice -- once as `planetaryfactory:%s` and once as "
              "`planetaryfactory:%s/%s`, two EMI entries for one recipe. Move it under `%s/`"
              % (where, recipe_type,
                 "`%s`" % first if first else "absent (the file is flat)",
                 where.rsplit(".json", 1)[0],
                 type_path, where.split("/")[-1].rsplit(".json", 1)[0],
                 type_path))

    for item, paths in sorted(routes.items()):
        if len(paths) > 1:
            check(item in MULTI_ROUTE,
                  "`%s` is made by %d recipes (%s) and MULTI_ROUTE does not name it. Two routes "
                  "to one item is two EMI entries for the same thing, and nothing in the game "
                  "reports it -- skip one where it is generated, or add a row here saying what "
                  "the second route earns"
                  % (item, len(paths), ", ".join(where for where, _ in paths)))

        hands = [where for where, hand in paths if hand]
        check(len(hands) <= 1,
              "`%s` has %d hand recipes (%s). The Personal Assembler's resolver picks a route "
              "with no cost model, so it cannot choose between them -- at most one route per "
              "item may carry `factorio_category: crafting`"
              % (item, len(hands), ", ".join(hands)))

    for item, why in sorted(MULTI_ROUTE.items()):
        found = len(routes.get(item, []))
        check(found > 1,
              "MULTI_ROUTE names `%s`, which %s. The row decides nothing now, and left in place "
              "it would silently re-admit a duplicate later. Its reason was: %s"
              % (item,
                 "no recipe emits" if found == 0 else "only one recipe emits",
                 why))

    return report(routes, total)


def report(routes=None, total=0):
    for failure in failures:
        print("FAIL: " + failure)
    if failures:
        print("\n%d failure(s)" % len(failures))
        return 1
    routes = routes or {}
    multi = sum(1 for paths in routes.values() if len(paths) > 1)
    print("ok: %d recipe output(s) across %d item(s); %d item(s) have a second route and each is "
          "a recorded decision" % (total, len(routes), multi))
    return 0


if __name__ == "__main__":
    sys.exit(main())
