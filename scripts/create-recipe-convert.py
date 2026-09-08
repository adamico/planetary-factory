#!/usr/bin/env python3
"""Re-author the Create kinetic recipes the pack wants onto its own Assembling Machine.

ADR-0034's sweep is default-deny: `kubejs/server_scripts/recipes.js` removes every recipe that is
not the pack's own on a named surface. Create ships 653 grid recipes -- 375 `crafting_shaped`, 274
`crafting_shapeless` and 4 `mechanical_crafting` -- on surfaces NO BLOCK IN THIS PACK EXECUTES:
the vanilla grid went with #90/#34 and #140, and ADR-0017 cuts the Mechanical Crafter by name.

THE DIFFERENCE FROM `powergrid-recipe-convert.py`. That script converts its whole corpus, because
Power Grid's corpus was extracted as exactly the set #172 converted. Here the corpus is the mod's
entire grid-craftable surface and the pack wants a dozen items out of it, most of the rest being
decorative palette variants. So this converter is CLOSURE-DRIVEN: `create-substitutions.json`
names WANTED ROOTS, and everything those recipes need transitively is pulled in and emitted with
them. Adding a kinetic component is one string in that file rather than a hand-written recipe per
ingredient it drags in -- which is the failure this script was written against, ten hand-authored
files that had drifted from each other on the same substitution.

Reads two committed inputs and, like the other two converters, decides nothing itself:

  data/create/recipe.json              the extracted corpus, patterns already flattened
  data/pack/create-substitutions.json  the wanted roots, and which ingredients are obtainable

THE CONVERSION RULE.

  1. A SHAPE IS ALREADY A LIST. `create-recipe-extract.py` flattened it, so a recipe asking for
     eight planks still asks for eight. Nothing is scaled here.

  2. THE SOURCE SURFACE PICKS THE CATEGORY, which keeps Create's own progression.
     `minecraft:crafting_*` was hand-craftable, so it becomes Factorio's `crafting` and the
     Personal Assembler will plan it (`RuntimeHandRecipes` keys on exactly this field).
     `create:mechanical_crafting` needed a machine, so it becomes `advanced-crafting` -- routed to
     the same Assembling Machine by `category-map.json` and excluded from the hand set by the same
     predicate. `machine_only` overrides this per recipe.

  3. AN INGREDIENT IS CRAFTED, KEPT OR SUBSTITUTED, NEVER GUESSED. If the closure emits it, it is
     obtainable by construction. Otherwise it must be classified. One that is none of the three is
     a HARD FAILURE, following `item-map.json`'s rule (#72).

  4. ONE HAND RECIPE PER ITEM. The Personal Assembler's resolver picks a route with no cost model,
     so two `crafting` recipes for one output is ambiguity it cannot resolve -- and
     `tests/factorio/test_hand_resolver.py` asserts the property globally. A second route must be
     declared `machine_only` or skipped; this converter will not choose for you.

  5. SUBSTITUTION MERGES. Two ingredients that substitute to the same item become one entry with
     their counts summed, which is what keeps the emitted recipes inside the Assembling Machine's
     five item-input envelope (`ASSEMBLING_IO`, machines.js).

Usage: scripts/create-recipe-convert.py [--check] [--quiet]
  --check  write nothing; exit non-zero if the emitted files on disk differ from what would be
           written. Generated output is never hand-edited (ADR-0026), and this is what says so.
"""
import argparse
import collections
import json
import shutil
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
CORPUS = ROOT / "data/create/recipe.json"
SUBSTITUTIONS = ROOT / "data/pack/create-substitutions.json"

# This subtree is this script's. `factorio-recipe-convert.py` owns every other directory under
# the recipe tree and wipes what it owns on each run -- it is taught to leave this one alone, see
# FOREIGN_SUBTREES there. The namespace stays `planetaryfactory` because that is what carries
# these recipes through ADR-0034's sweep; a second namespace would need a second survivor entry.
# NESTED UNDER `assembling/` for #87's reason, which `factorio-recipe-convert.py`'s `emitted_path`
# states in full: GregTech's `RecipeManagerLateMixin` strips everything before the first `/` of a
# loaded GTRecipe's id and `GTRecipeBuilder.save` puts the recipe type's path back on. A file at
# `recipe/create/shaft.json` loads as `planetaryfactory:create/shaft` and is re-registered as
# `planetaryfactory:assembling/shaft` -- both ids in the recipe manager, two EMI entries for one
# recipe. Nesting under the type's own directory closes the round trip.
OUT_DIR = ROOT / "kubejs/data/planetaryfactory/recipe/assembling/create"

CATEGORY_OF_SOURCE = {
    "minecraft:crafting_shaped": "crafting",
    "minecraft:crafting_shapeless": "crafting",
    "create:mechanical_crafting": "advanced-crafting",
}

RECIPE_TYPE = "gtceu:assembling"

# `ASSEMBLING_IO[0]` in `kubejs/startup_scripts/machines.js`. A recipe with more distinct item
# inputs than this cannot run in the machine that accepts it, and nothing in the game says so.
MAX_ITEM_INPUTS = 5


def ingredient_json(name):
    """`#minecraft:logs` is a tag, anything else is an item. The corpus uses the `#` convention."""
    return {"tag": name[1:]} if name.startswith("#") else {"item": name}


def index_by_result(corpus, skip):
    by_result = collections.defaultdict(list)
    for name in sorted(corpus):
        if name in skip:
            continue
        by_result[corpus[name]["result"]].append(name)
    return by_result


def walk_closure(corpus, subs, problems):
    """Every recipe name reachable from the wanted roots. Rule 3's crafted/kept/substituted split.

    Returns the reachable recipe names. An item that is neither classified nor craftable is
    reported here rather than emitted, because that is the exact failure -- a recipe the player
    cannot craft, loaded with no error.
    """
    keep, substitute, skip = subs["keep"], subs["substitute"], subs["skip"]
    by_result = index_by_result(corpus, skip)

    reachable = set()
    seen = set()
    queue = list(subs["wanted"])
    roots = set(queue)

    while queue:
        item = queue.pop()
        if item in seen:
            continue
        seen.add(item)

        names = by_result.get(item)
        if not names:
            if item in roots:
                problems.append(
                    "create-substitutions.json wants `%s`, which has no grid recipe in the corpus "
                    "-- Create makes it on a surface this pack does not execute, so it cannot be "
                    "crafted into the closure. Substitute it or drop it from `wanted`." % item)
            else:
                problems.append(
                    "`%s` is needed by the closure, is neither kept nor substituted, and has no "
                    "grid recipe. Classify it in create-substitutions.json: an unclassified "
                    "ingredient reaches the player as a recipe that cannot be crafted." % item)
            continue

        for name in names:
            reachable.add(name)
            for entry in corpus[name]["ingredients"]:
                source = entry["ingredient"]
                if source in keep or source in substitute:
                    continue
                queue.append(source)

    return reachable


def convert(corpus, subs, reachable, problems):
    """The reachable recipes as pack recipe JSON, keyed by the stem they are written to."""
    keep = subs["keep"]
    substitute = subs["substitute"]
    machine_only = subs["machine_only"]
    duration = subs["duration"]
    overrides = duration["overrides"]
    emitted = {}

    for name in sorted(reachable):
        recipe = corpus[name]
        category = CATEGORY_OF_SOURCE.get(recipe["source_type"])
        if name in machine_only:
            category = "advanced-crafting"
        if category is None:
            problems.append(
                "%s is a %s, which this converter does not route" % (name, recipe["source_type"]))
            continue

        # Rule 5: substitute first, then merge, so that counts survive the collapse.
        merged = {}
        order = []
        for entry in recipe["ingredients"]:
            source = entry["ingredient"]
            if source in substitute:
                target = substitute[source]["to"]
            else:
                target = source
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
            "duration": overrides.get(name, duration[category]),
            "data": {"factorio_category": category},
        }

    # Rule 4, checked on what was actually emitted rather than on the corpus: the resolver sees
    # only these.
    hand_routes = collections.defaultdict(list)
    for name, recipe in emitted.items():
        if recipe["data"]["factorio_category"] == "crafting":
            hand_routes[recipe["outputs"]["item"][0]["content"]["ingredient"]["item"]].append(name)
    for output, names in sorted(hand_routes.items()):
        if len(names) > 1:
            problems.append(
                "`%s` has %d hand recipes (%s). The Personal Assembler's resolver picks a route "
                "with no cost model, so it cannot choose between them -- declare all but one "
                "`machine_only` in create-substitutions.json, or skip it (#hand-resolver)"
                % (output, len(names), ", ".join(sorted(names))))

    return emitted


def unused_rows(corpus, subs, reachable, emitted, problems):
    """A table row nothing reads is a rule left behind by a re-extraction, not a spare part."""
    used = {entry["ingredient"]
            for name in reachable for entry in corpus[name]["ingredients"]}
    for table in ("keep", "substitute"):
        for row in sorted(subs[table]):
            if row not in used:
                problems.append(
                    "create-substitutions.json `%s` names `%s`, which no converted recipe takes "
                    "-- either a typo or a row a re-extraction left behind" % (table, row))
    for table in ("skip", "machine_only"):
        for name in sorted(subs[table]):
            if name not in corpus:
                problems.append("create-substitutions.json `%s` names `%s`, which is not in the "
                                "corpus" % (table, name))
    for name in sorted(subs["machine_only"]):
        if name in subs["skip"]:
            problems.append("create-substitutions.json both skips and forces `%s` onto the "
                            "machine" % name)
        elif name not in reachable:
            problems.append(
                "create-substitutions.json `machine_only` names `%s`, which the closure never "
                "reaches -- it emits nothing, so the row decides nothing" % name)
    for name in sorted(subs["duration"]["overrides"]):
        if name not in emitted:
            problems.append(
                "create-substitutions.json `duration.overrides` names `%s`, which is not emitted"
                % name)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true",
                        help="write nothing; fail if the files on disk differ")
    parser.add_argument("--quiet", action="store_true")
    args = parser.parse_args()

    corpus = json.loads(CORPUS.read_text())
    subs = json.loads(SUBSTITUTIONS.read_text())

    problems = []
    reachable = walk_closure(corpus, subs, problems)
    emitted = convert(corpus, subs, reachable, problems)
    unused_rows(corpus, subs, reachable, emitted, problems)

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
            print("FAIL: the emitted Create recipes on disk are not what this converter writes. "
                  "Generated output is never hand-edited -- re-run "
                  "`scripts/create-recipe-convert.py` (ADR-0026).", file=sys.stderr)
            for label, names in (("missing", missing), ("unexpected", extra),
                                 ("changed", changed)):
                if names:
                    print("  %s: %s" % (label, ", ".join(names)), file=sys.stderr)
            return 1
        if not args.quiet:
            print("ok: %d Create recipe(s) on disk match the converter" % len(emitted))
        return 0

    if OUT_DIR.exists():
        shutil.rmtree(OUT_DIR)
    OUT_DIR.mkdir(parents=True)
    for name, recipe in sorted(emitted.items()):
        (OUT_DIR / (name + ".json")).write_text(json.dumps(recipe, indent=2) + "\n")

    if not args.quiet:
        hand = sum(1 for r in emitted.values() if r["data"]["factorio_category"] == "crafting")
        print("wrote %d Create recipe(s) to %s from %d wanted root(s)"
              % (len(emitted), OUT_DIR.relative_to(ROOT), len(subs["wanted"])))
        print("  %d hand-craftable (`crafting`), %d machine-only (`advanced-crafting`)"
              % (hand, len(emitted) - hand))
    return 0


if __name__ == "__main__":
    sys.exit(main())
