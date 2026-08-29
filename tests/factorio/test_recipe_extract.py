#!/usr/bin/env python3
"""Assert the extracted recipe corpus is internally consistent and still in scope.

`scripts/factorio-recipe-extract.py` reads a 28MB Factorio dump that is not in the repo, so
nothing here re-runs it. What is checkable without the dump is whether the *committed*
output still says what the decisions say it says:

  - every recipe's primary category is routed by `data/pack/category-map.json`, because an
    unrouted category is a recipe the converter would silently have nowhere to put
  - every recipe names a technology that exists in `technology.json`, or none at all
  - the corpus is still Nauvis pre-launch: no recipe arrives via a technology costing a
    science pack outside ADR-0018's four rungs
  - every recipe carries one of Factorio's item groups, because the item map is argued per
    group rather than per recipe, and a null group is a recipe no group decision covers
  - no recipe is empty: `recipe-unknown` is core's hidden placeholder icon and reached the
    corpus once, being enabled from the start like everything else with no technology
  - `setMaxIOSize` is unchanged, because ADR-0026 hard-codes those numbers into the recipe
    type and a regeneration that widens a recipe must not do it silently

Usage: tests/factorio/test_recipe_extract.py
"""
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent.parent

# ADR-0026 reads these off the data, and #73 registers the recipe types against them. A
# change here is a change to a registered machine's GUI, so it fails rather than drifts.
EXPECTED_IO = {
    "assembling": (5, 1, 1, 1),
    "chemical_plant": (2, 1, 2, 1),
    "oil_refinery": (0, 0, 2, 3),
    "rocket_silo": (3, 1, 0, 0),
    "smelting": (1, 1, 0, 0),
}

# Factorio's own taxonomy, resolved through each recipe's main product. `enemies`,
# `environment`, `fluids`, `signals`, `tiles` and `other` exist too, but no Nauvis
# pre-launch *recipe* lands in them; one that did would be a classification bug, not a new
# group to argue about.
GROUPS = {"intermediate-products", "logistics", "production", "combat", "space", "effects"}

RUNG_PACKS = {
    "automation-science-pack",
    "logistic-science-pack",
    "chemical-science-pack",
    "production-science-pack",
}


def io_sizes(recipes, routes):
    sizes = {}
    for recipe in recipes:
        machine = routes.get(recipe["category"])
        if machine is None or machine.startswith("!"):
            continue
        counts = list(sizes.get(machine, (0, 0, 0, 0)))
        for index, entries in enumerate((recipe["ingredients"], recipe["results"])):
            counts[index] = max(counts[index],
                                sum(1 for e in entries if e["type"] != "fluid"))
            counts[2 + index] = max(counts[2 + index],
                                    sum(1 for e in entries if e["type"] == "fluid"))
        sizes[machine] = tuple(counts)
    return sizes


def main():
    recipes = json.loads((ROOT / "data/factorio/recipe.json").read_text())
    techs = json.loads((ROOT / "data/factorio/technology.json").read_text())
    routes = json.loads((ROOT / "data/pack/category-map.json").read_text())["routes"]
    by_name = {t["name"]: t for t in techs}
    failures = []

    if not recipes:
        failures.append("recipe.json is empty")

    unrouted = sorted({r["category"] for r in recipes} - set(routes))
    if unrouted:
        failures.append("categories no route covers: " + ", ".join(unrouted))

    for recipe in recipes:
        tech = recipe["unlocked_by"]
        if tech is None:
            continue
        if tech not in by_name:
            failures.append(f"{recipe['name']} names technology {tech}, which is not extracted")
            continue
        unit = by_name[tech].get("unit") or {}
        packs = {i[0] for i in (unit.get("ingredients") or [])}
        if not packs <= RUNG_PACKS:
            failures.append(
                f"{recipe['name']} arrives via {tech}, which costs "
                + ", ".join(sorted(packs - RUNG_PACKS))
            )

    for recipe in recipes:
        if recipe.get("group") not in GROUPS:
            failures.append(
                f"{recipe['name']} has group {recipe.get('group')!r}, "
                "which is not one of Factorio's item groups"
            )
        if not recipe["results"]:
            failures.append(f"{recipe['name']} produces nothing -- a hidden placeholder?")

    for recipe in recipes:
        for entry in recipe["ingredients"] + recipe["results"]:
            if not entry.get("name") or entry.get("type") not in ("item", "fluid"):
                failures.append(f"{recipe['name']} has a malformed entry: {entry}")

    got = io_sizes(recipes, routes)
    for machine, want in EXPECTED_IO.items():
        if got.get(machine) != want:
            failures.append(
                f"setMaxIOSize for {machine} is {got.get(machine)}, expected {want} "
                "-- see ADR-0026 before changing this"
            )
    for machine in sorted(set(got) - set(EXPECTED_IO)):
        failures.append(f"{machine} is routed but has no expected IO size: {got[machine]}")

    for name, failure in enumerate(failures, 1):
        print(f"FAIL {name}: {failure}")
    if failures:
        return 1
    print(f"ok   {len(recipes)} recipes, every category routed, IO sizes unchanged")
    return 0


if __name__ == "__main__":
    sys.exit(main())
