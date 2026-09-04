#!/usr/bin/env python3
"""Re-author Create: Power Grid's unreachable recipes onto the pack's own surfaces (#172).

ADR-0034's sweep is default-deny: `kubejs/server_scripts/recipes.js` removes every recipe that is
not the pack's own on a named surface. Power Grid ships 112 recipes and 84 of them sit on surfaces
NO BLOCK IN THIS PACK EXECUTES -- 43 `minecraft:crafting_shaped`, 28 `minecraft:crafting_shapeless`
and 13 `create:mechanical_crafting`. The vanilla grid went with #90/#34 and #140, and ADR-0017 cuts
Create's Mechanical Crafter by name. So those 84 recipes are not merely swept: admitting them would
put 84 entries in EMI that are craftable nowhere, which is the hazard ADR-0034 exists to name.

This script re-authors them as pack recipes on the Assembling Machine, which is what ADR-0017
already decided for exactly this case -- Create's own casings "fall through to where every other
fluid-free `crafting` row already goes (#88): the Personal Assembler, and the Assembling Machines
above it". Power Grid is the same case, arriving later.

Reads two committed inputs and, like `factorio-recipe-convert.py`, decides nothing itself:

  data/powergrid/recipe.json         the extracted corpus -- 84 recipes, patterns already
                                     flattened to ingredient lists with counts (#172)
  data/pack/grid-substitutions.json  which ingredients are obtainable and what the rest become

THE CONVERSION RULE.

  1. A SHAPE BECOMES A LIST. A shaped pattern is flattened to an unordered ingredient list, each
     ingredient carrying the number of grid cells it filled. Nothing is scaled: a recipe asking
     for three iron plates still asks for three. That is the whole of "shaped becomes shapeless",
     and it is why the corpus stores counts rather than patterns.

  2. THE SOURCE SURFACE PICKS THE CATEGORY, which is what keeps Power Grid's own progression.
     `minecraft:crafting_*` was hand-craftable in a Create pack, so it becomes Factorio's
     `crafting` and the Personal Assembler will plan it (`RuntimeHandRecipes` keys on exactly
     this field). `create:mechanical_crafting` needed a machine, so it becomes
     `advanced-crafting` -- routed to the same Assembling Machine by `category-map.json`, and
     excluded from the hand set by the same predicate. The distinction the mod drew survives the
     move; it is not re-invented here.

  3. AN INGREDIENT IS KEPT OR SUBSTITUTED, NEVER GUESSED. One in neither table is a HARD FAILURE,
     following `item-map.json`'s rule (#72): a name nobody has looked at must never be quietly
     passed into a recipe the player cannot craft, which is the exact failure #172 was filed
     against.

  4. SUBSTITUTION MERGES. Two ingredients that substitute to the same item become one entry with
     their counts summed -- a recipe asking for andesite alloy AND an iron plate asks for iron
     plates twice over once zinc is gone. This is what keeps the emitted recipes inside the
     Assembling Machine's five item-input envelope (`ASSEMBLING_IO`, machines.js); without it
     five recipes would exceed it and be unrunnable in a machine that accepted them.

DURATION is not in the corpus, because a vanilla grid recipe has none, so one has to be chosen.
It is chosen in `grid-substitutions.json`'s `duration` table rather than here, for the reason
everything else is: nothing is decided in the script.

`machine_only` overrides rule 2 for a named recipe, and exists for Power Grid's `*_from_conversion`
rows -- both directions of a block/orientation swap. As hand recipes they are 2-cycles AND second
routes to items that already have one; on the machine they are neither, and both directions
survive. That table says which and why.

Usage: scripts/powergrid-recipe-convert.py [--check] [--quiet]
  --check  write nothing; exit non-zero if the emitted files on disk differ from what would be
           written. Generated output is never hand-edited (ADR-0026), and this is what says so.
"""
import argparse
import json
import shutil
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
CORPUS = ROOT / "data/powergrid/recipe.json"
SUBSTITUTIONS = ROOT / "data/pack/grid-substitutions.json"

# The pack's recipe tree is shared with `factorio-recipe-convert.py`, which owns every other
# subdirectory and wipes what it owns on each run. This subtree is this script's, and that script
# is taught to leave it alone -- see FOREIGN_SUBTREES there. Splitting by directory rather than
# by namespace is deliberate: the recipe id follows the path, and `mod: 'planetaryfactory'` in the
# survivor allowlist is what keeps these recipes through the sweep. A second namespace would need
# a second survivor entry and would put this line outside ADR-0034's rule.
OUT_DIR = ROOT / "kubejs/data/planetaryfactory/recipe/grid"

# Which Factorio category a source surface becomes. Both route to `assembling` in
# `category-map.json`; the difference is the Personal Assembler, which plans `crafting` only.
CATEGORY_OF_SOURCE = {
    "minecraft:crafting_shaped": "crafting",
    "minecraft:crafting_shapeless": "crafting",
    "create:mechanical_crafting": "advanced-crafting",
}

RECIPE_TYPE = "gtceu:assembling"

# `ASSEMBLING_IO[0]` in `kubejs/startup_scripts/machines.js`. A recipe with more distinct item
# inputs than this cannot run in the machine that accepts it, and nothing in the game says so --
# it simply never matches. Asserted here so a substitution table edit cannot quietly cross it.
MAX_ITEM_INPUTS = 5


def ingredient_json(name):
    """`#c:plates/iron` is a tag, anything else is an item. The corpus uses the `#` convention."""
    return {"tag": name[1:]} if name.startswith("#") else {"item": name}


def convert(corpus, subs, problems):
    """The 84 recipes as pack recipe JSON, keyed by the stem they are written to."""
    keep = subs["keep"]
    substitute = subs["substitute"]
    skip = subs["skip"]
    machine_only = subs["machine_only"]
    duration = subs["duration"]
    emitted = {}

    for name in sorted(corpus):
        if name in skip:
            continue
        recipe = corpus[name]
        category = CATEGORY_OF_SOURCE.get(recipe["source_type"])
        if name in machine_only:
            category = "advanced-crafting"
        if category is None:
            problems.append(
                "%s is a %s, which this converter does not route -- the corpus holds only the "
                "three surfaces #172 converts" % (name, recipe["source_type"]))
            continue

        # Rule 4: substitute first, then merge, so that counts survive the collapse.
        merged = {}
        order = []
        for entry in recipe["ingredients"]:
            source = entry["ingredient"]
            if source in keep:
                target = source
            elif source in substitute:
                target = substitute[source]["to"]
            else:
                problems.append(
                    "%s takes `%s`, which grid-substitutions.json neither keeps nor substitutes. "
                    "Classify it: an unclassified ingredient reaches the player as a recipe that "
                    "cannot be crafted (#172)" % (name, source))
                continue
            if target not in merged:
                order.append(target)
                merged[target] = 0
            merged[target] += entry["amount"]

        if len(merged) > MAX_ITEM_INPUTS:
            problems.append(
                "%s needs %d distinct item inputs and the Assembling Machine takes %d "
                "(ASSEMBLING_IO, machines.js). It would load and never match"
                % (name, len(merged), MAX_ITEM_INPUTS))

        emitted[name] = {
            "type": RECIPE_TYPE,
            "inputs": {"item": [
                {"content": {"ingredient": ingredient_json(target), "count": merged[target]}}
                for target in order
            ]},
            "outputs": {"item": [
                {"content": {"ingredient": {"item": recipe["result"]},
                             "count": recipe["result_count"]}}
            ]},
            "duration": duration[category],
            "data": {"factorio_category": category},
        }
    return emitted


def unused_rows(corpus, subs, problems):
    """A table row nothing reads is a rule left behind by a re-extraction, not a spare part."""
    used = {entry["ingredient"]
            for name, recipe in corpus.items() if name not in subs["skip"]
            for entry in recipe["ingredients"]}
    for table in ("keep", "substitute"):
        for row in sorted(subs[table]):
            if row not in used:
                problems.append(
                    "grid-substitutions.json `%s` names `%s`, which no converted recipe takes -- "
                    "either a typo or a row a re-extraction left behind" % (table, row))
    for name in sorted(subs["machine_only"]):
        if name not in corpus:
            problems.append(
                "grid-substitutions.json `machine_only` names `%s`, which is not in the corpus"
                % name)
        elif name in subs["skip"]:
            problems.append(
                "grid-substitutions.json both skips and forces `%s` onto the machine" % name)
    for name in sorted(subs["skip"]):
        if name not in corpus:
            problems.append(
                "grid-substitutions.json `skip` names `%s`, which is not in the corpus" % name)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true",
                        help="write nothing; fail if the files on disk differ")
    parser.add_argument("--quiet", action="store_true")
    args = parser.parse_args()

    corpus = json.loads(CORPUS.read_text())
    subs = json.loads(SUBSTITUTIONS.read_text())

    problems = []
    emitted = convert(corpus, subs, problems)
    unused_rows(corpus, subs, problems)

    if problems:
        for problem in problems:
            print("FAIL: " + problem, file=sys.stderr)
        print("\n%d problem(s)" % len(problems), file=sys.stderr)
        return 1

    if args.check:
        on_disk = {p.stem: json.loads(p.read_text())
                   for p in OUT_DIR.glob("*.json")} if OUT_DIR.exists() else {}
        if on_disk != emitted:
            missing = sorted(set(emitted) - set(on_disk))
            extra = sorted(set(on_disk) - set(emitted))
            changed = sorted(k for k in set(emitted) & set(on_disk) if on_disk[k] != emitted[k])
            print("FAIL: the emitted grid recipes on disk are not what this converter writes. "
                  "Generated output is never hand-edited -- re-run "
                  "`scripts/powergrid-recipe-convert.py` (ADR-0026).", file=sys.stderr)
            for label, names in (("missing", missing), ("unexpected", extra),
                                 ("changed", changed)):
                if names:
                    print("  %s: %s" % (label, ", ".join(names)), file=sys.stderr)
            return 1
        if not args.quiet:
            print("ok: %d grid recipe(s) on disk match the converter" % len(emitted))
        return 0

    if OUT_DIR.exists():
        shutil.rmtree(OUT_DIR)
    OUT_DIR.mkdir(parents=True)
    for name, recipe in sorted(emitted.items()):
        (OUT_DIR / (name + ".json")).write_text(json.dumps(recipe, indent=2) + "\n")

    if not args.quiet:
        hand = sum(1 for r in emitted.values() if r["data"]["factorio_category"] == "crafting")
        print("wrote %d grid recipe(s) to %s" % (len(emitted), OUT_DIR.relative_to(ROOT)))
        print("  %d hand-craftable (`crafting`), %d machine-only (`advanced-crafting`), "
              "%d skipped" % (hand, len(emitted) - hand, len(subs["skip"])))
    return 0


if __name__ == "__main__":
    sys.exit(main())
