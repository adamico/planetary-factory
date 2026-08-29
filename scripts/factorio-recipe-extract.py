#!/usr/bin/env python3
"""Extract Factorio's recipe prototypes as the data the pack's recipes are generated from.

ADR-0026 stops authoring GregTech recipes through KubeJS's `GTRecipeBuilder` and makes them
raw JSON instead -- generated from Factorio's own prototypes rather than typed, under
ADR-0022's extract-never-transcribe rule. This is the extraction half. The conversion half
reads what this writes, plus the mapping files, and emits the recipe JSON itself.

Same dump as `scripts/factorio-tech-extract.py`, and the same provenance block in
`data/factorio/README.md`: `factorio --dump-data` writes every loaded prototype to
`script-output/data-raw-dump.json`, so no second Factorio run is needed if that file is
still on disk.

Three things this script decides, because they are properties of the data rather than of
the pack:

  - **Scope.** Factorio has 662 recipes and this map reaches Terra's first launch, so the
    corpus is Nauvis pre-launch: every recipe unlocked by a technology whose pack cost is a
    subset of ADR-0018's four rungs, plus the recipes enabled from the start. Later bodies
    widen it by widening `RUNG_PACKS`; nothing else changes.
  - **Routing.** A recipe carries a *list* of categories, and the extra entries are the DLC
    machines that may also craft it -- `transport-belt` is `["crafting", "metallurgy"]`
    because Vulcanus's foundry exists, not because a belt is a metallurgy recipe. The first
    entry is the primary one, and it is what `data/pack/category-map.json` routes on.
  - **Classification.** Every recipe carries Factorio's own `group` and `subgroup`, resolved
    through its main product where the recipe does not set one itself. The item map is
    argued per group -- combat, logistics, production, intermediate-products, space -- not
    per recipe, so the taxonomy is extracted rather than hand-assigned.
  - **`setMaxIOSize`.** ADR-0026 says the number is read off this output rather than
    guessed. The report prints it per machine, as (item in, item out, fluid in, fluid out).

What this script does *not* do is decide which pack item stands in for a Factorio one. That
is the item map, and it is the conversion half's input.

Usage:

    scripts/factorio-recipe-extract.py            # finds the dump, writes data/factorio/recipe.json
    scripts/factorio-recipe-extract.py --dump PATH
"""

import argparse
import collections
import json
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent

DEFAULT_DUMP = (
    Path.home()
    / "Library/Application Support/factorio/script-output/data-raw-dump.json"
)

# ADR-0018's four rungs. A technology whose unit ingredients fall inside this set is
# pre-launch on Terra; one that wants utility or space science belongs to a later map, and
# so do the recipes it unlocks. Trigger technologies cost no packs at all and are kept
# unconditionally -- Factorio fires them on crafting or mining something, which is a cost
# in the same sense.
RUNG_PACKS = frozenset(
    {
        "automation-science-pack",
        "logistic-science-pack",
        "chemical-science-pack",
        "production-science-pack",
    }
)

# Categories that exist only as UI scaffolding. `parameter-0` .. `parameter-9` are the
# placeholder items Factorio's parametrised blueprints substitute into; they are enabled
# from the start, which is the only reason they reach the corpus at all.
UI_CATEGORIES = frozenset({"parameters"})


def subgroup_index(dump):
    """`subgroup name -> group name`, and `prototype name -> subgroup name`.

    Factorio's own taxonomy is the classification the item map is argued in: an item group
    is one of `intermediate-products`, `logistics`, `production`, `combat`, `space`. It has
    to be *resolved* rather than copied, because a recipe carries its own `subgroup` on only
    219 of the game's 662 recipes -- the rest inherit it from their main product's item
    prototype. Items live under a dozen top-level prototype types (`item`, `ammo`, `gun`,
    `armor`, `capsule`, `module`, `fluid`, ...), so the index spans all of them.
    """
    groups = {name: proto["group"] for name, proto in (dump.get("item-subgroup") or {}).items()}
    if not groups:
        sys.exit("no `item-subgroup` prototypes in the dump -- did the key move?")

    subgroups = {}
    for prototype_type, protos in dump.items():
        if prototype_type == "recipe" or not isinstance(protos, dict):
            continue
        for name, proto in protos.items():
            if isinstance(proto, dict) and isinstance(proto.get("subgroup"), str):
                subgroups.setdefault(name, proto["subgroup"])
    return groups, subgroups


def classify(recipe, results, subgroups, groups):
    """A recipe's `(group, subgroup)` -- its own, else its main product's."""
    subgroup = recipe.get("subgroup")
    if not subgroup:
        # `main_product` is set to "" to declare that a multi-result recipe has none.
        main = recipe.get("main_product")
        if not isinstance(main, str) or not main:
            main = results[0]["name"] if results else None
        subgroup = subgroups.get(main)
    return groups.get(subgroup), subgroup


def scoped_technologies(techs):
    """The technologies whose cost sits inside ADR-0018's four rungs, closed downward.

    Closed rather than filtered: a technology whose prerequisite falls outside the set is
    not reachable pre-launch either, however cheap it is itself. Iterating to a fixed point
    is the honest way to say that -- the prerequisite graph is small enough that the cost
    does not matter, and a single pass would keep a technology whose parent it had already
    dropped.
    """
    by_name = {t["name"]: t for t in techs}
    keep = set()
    for tech in techs:
        unit = tech.get("unit") or {}
        packs = {ingredient[0] for ingredient in (unit.get("ingredients") or [])}
        if tech["cost_kind"] == "trigger" or packs <= RUNG_PACKS:
            keep.add(tech["name"])

    changed = True
    while changed:
        changed = False
        for name in sorted(keep):
            if any(parent not in keep for parent in by_name[name]["prerequisites"]):
                keep.discard(name)
                changed = True
    return keep, by_name


def scoped_recipes(recipes, techs, keep, by_name):
    """Recipe name -> the technology that unlocks it, or None for enabled-from-the-start.

    A recipe unlocked by several kept technologies takes the first in name order, which is
    stable across runs and is only a label: what unlocks a recipe in *this* pack is a
    Researchd effect, decided in `researchd.js`, not here.
    """
    unlocked = {}
    for name in sorted(keep):
        for effect in by_name[name]["effects"]:
            if effect.get("type") == "unlock-recipe" and effect["recipe"] in recipes:
                unlocked.setdefault(effect["recipe"], name)
    for name, recipe in recipes.items():
        # `enabled` defaults to true, and an enabled recipe needs no technology.
        if recipe.get("enabled", True):
            unlocked.setdefault(name, None)
    return unlocked


def contents(entries):
    """Ingredients or results, normalised to `{type, name, amount}`.

    Factorio writes a result's amount as `amount`, or as `amount_min`/`amount_max` with a
    `probability` for the random ones. Those three are kept verbatim rather than collapsed
    to an average: a converter that has to decide what a 0.8-probability output means in
    GregTech should be looking at the real numbers.
    """
    out = []
    for entry in entries or []:
        item = {
            "type": entry.get("type", "item"),
            "name": entry["name"],
        }
        for key in ("amount", "amount_min", "amount_max", "probability",
                    "ignored_by_productivity", "fluidbox_index", "temperature"):
            if key in entry:
                item[key] = entry[key]
        out.append(item)
    return out


def extract(dump, techs):
    recipes = dump.get("recipe") or {}
    if not recipes:
        sys.exit("no `recipe` prototypes in the dump -- did the key move?")

    keep, by_name = scoped_technologies(techs)
    unlocked = scoped_recipes(recipes, techs, keep, by_name)
    groups, subgroups = subgroup_index(dump)

    out = []
    skipped = collections.Counter()
    for name in sorted(unlocked):
        recipe = recipes[name]
        categories = recipe.get("categories") or ["crafting"]
        if categories[0] in UI_CATEGORIES:
            skipped[categories[0]] += 1
            continue
        # `recipe-unknown` is core's placeholder icon: hidden, no ingredients, no results.
        # It is enabled from the start, which is the only reason it reaches the corpus.
        if recipe.get("hidden"):
            skipped["hidden"] += 1
            continue
        results = contents(recipe.get("results"))
        group, subgroup = classify(recipe, results, subgroups, groups)
        if group is None:
            sys.exit(f"{name}: no item group -- its main product has no subgroup")
        out.append(
            {
                "name": name,
                "category": categories[0],
                "categories": categories,
                "unlocked_by": unlocked[name],
                "energy_required": recipe.get("energy_required", 0.5),
                "ingredients": contents(recipe.get("ingredients")),
                "results": results,
                "allow_productivity": bool(recipe.get("allow_productivity")),
                "group": group,
                "subgroup": subgroup,
                "order": recipe.get("order"),
            }
        )
    return out, keep, skipped


def io_sizes(recipes, routes):
    """Max (item in, item out, fluid in, fluid out) per routed machine.

    This is `setMaxIOSize` (ADR-0026), read off the data the way ADR-0025 read the chemical
    plant's envelope off the wiki. A machine whose envelope is too small silently cannot
    hold its own recipes; too large and the GUI grows slots nothing fills.
    """
    sizes = collections.defaultdict(lambda: [0, 0, 0, 0])
    for recipe in recipes:
        machine = routes.get(recipe["category"])
        if machine is None or machine.startswith("!"):
            continue
        counts = sizes[machine]
        for index, entries in enumerate((recipe["ingredients"], recipe["results"])):
            items = [e for e in entries if e["type"] != "fluid"]
            fluids = [e for e in entries if e["type"] == "fluid"]
            counts[index] = max(counts[index], len(items))
            counts[2 + index] = max(counts[2 + index], len(fluids))
    return {machine: tuple(counts) for machine, counts in sorted(sizes.items())}


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--dump", type=Path, default=DEFAULT_DUMP)
    parser.add_argument("--out", type=Path, default=REPO / "data" / "factorio" / "recipe.json")
    parser.add_argument(
        "--category-map",
        type=Path,
        default=REPO / "data" / "pack" / "category-map.json",
        help="routing table, read only to report IO sizes and unrouted categories",
    )
    args = parser.parse_args()

    if not args.dump.is_file():
        sys.exit(
            f"no dump at {args.dump}\n"
            "run:  factorio --dump-data --mod-directory <dir with base+SA only>"
        )

    dump = json.loads(args.dump.read_text(encoding="utf-8"))
    techs = json.loads((REPO / "data" / "factorio" / "technology.json").read_text())
    recipes, keep, skipped = extract(dump, techs)

    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(json.dumps(recipes, indent=2) + "\n", encoding="utf-8")

    by_category = collections.Counter(r["category"] for r in recipes)
    refs = {(e["type"], e["name"]) for r in recipes
            for e in r["ingredients"] + r["results"]}

    print(f"dump           {len(dump.get('recipe') or {})} recipes")
    print(f"in scope       {len(keep)} technologies, {len(recipes)} recipes")
    print(f"skipped        {dict(skipped)}")
    print(f"item map needs {len(refs)} distinct references "
          f"({sum(1 for t, _ in refs if t == 'fluid')} fluid)")
    print(f"wrote          {args.out.relative_to(REPO)}")
    by_group = collections.Counter(
        (r["group"], r["subgroup"]) for r in recipes)
    print("\nitem groups:")
    for group, count in collections.Counter(r["group"] for r in recipes).most_common():
        print(f"  {count:4}  {group}")
        for (g, subgroup), n in sorted(by_group.items()):
            if g == group:
                print(f"          {n:4}  {subgroup}")
    print("\nprimary categories:")
    for category, count in by_category.most_common():
        print(f"  {count:4}  {category}")

    if args.category_map.is_file():
        routes = json.loads(args.category_map.read_text())["routes"]
        unrouted = sorted(set(by_category) - set(routes))
        print("\nsetMaxIOSize per machine (item in, item out, fluid in, fluid out):")
        for machine, size in io_sizes(recipes, routes).items():
            print(f"  {machine:20} {size}")
        if unrouted:
            print("\ncategories the map does not route -- the converter would hard-fail:\n  "
                  + ", ".join(unrouted))
    else:
        print(f"\nno category map at {args.category_map} -- IO sizes not computed")


if __name__ == "__main__":
    main()
