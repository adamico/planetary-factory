#!/usr/bin/env python3
"""Extract Create's grid-craftable recipes from the mod jar into a committed corpus (#172 follow-on).

WHY THIS EXISTS. `data/create/recipe.json` is read by `scripts/create-recipe-convert.py`, and a
converter that read the jar directly would work only on a machine that had already installed the
pack -- `mods/` is gitignored and the manifest is packwiz (ADR-0024). Same reason `data/factorio/`
and `data/powergrid/` are committed. Unlike Power Grid's corpus, this one was extracted by a
committed script rather than by hand, so a version bump is one command rather than a recollection.

WHAT IT KEEPS. The three surfaces no block in this pack executes:

    minecraft:crafting_shaped        the vanilla grid went with #90/#34, and #140 made the 2x2 inert
    minecraft:crafting_shapeless     as above
    create:mechanical_crafting       ADR-0017 cuts Create's Mechanical Crafter by name

Everything else Create ships -- milling, crushing, deploying, splashing, pressing, cutting,
haunting, filling, mixing, compacting, emptying, sequenced assembly, stonecutting and the vanilla
cooking types -- is NOT extracted. Whether the pack keeps any of those surfaces is a separate
question that no ticket has answered, and a corpus is not the place to answer it.

WHAT IT DOES NOT DECIDE. Everything. The corpus is the whole grid-craftable surface of the mod,
not the subset the pack wants; `data/pack/create-substitutions.json` names the wanted roots and
the converter walks the closure. That is the one place this differs from `data/powergrid/`, where
the corpus *is* the converted set -- Create ships 653 grid recipes and the pack wants a dozen, so
filtering at extraction would put the decision in the wrong file and force a re-extraction every
time the wanted set grew.

PATTERNS ARE FLATTENED HERE, not in the converter: a shaped recipe's grid becomes an unordered
ingredient list where `amount` is the number of cells that ingredient filled. That is the
conversion "a shape becomes a list", and doing it at extraction keeps the committed data in the
form the decision is about. `source_type` is kept because the converter reads it to choose the
Factorio category.

A KEY IS THE RECIPE'S FILE STEM, prefixed by its subdirectory where Create reuses a stem across
directories -- the mod does, and a silent collision would drop a recipe.

Usage: scripts/create-recipe-extract.py [--check] [--quiet]
  --check  write nothing; exit non-zero if the corpus on disk differs from the jar's
"""
import argparse
import collections
import json
import re
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "data/create/recipe.json"
MODS = ROOT / "mods"
JAR_GLOB = "create-*.jar"

KEPT_TYPES = (
    "minecraft:crafting_shaped",
    "minecraft:crafting_shapeless",
    "create:mechanical_crafting",
)


def find_jar():
    """The pinned Create jar. `createbetterfps-*.jar` also matches a naive glob -- exclude it."""
    jars = [p for p in sorted(MODS.glob(JAR_GLOB))
            if re.match(r"^create-\d", p.name)]
    if not jars:
        raise SystemExit(
            "FAIL: no %s in mods/. The jars are gitignored (ADR-0024) -- install the pack with "
            "packwiz first, or run `scripts/pack-check.sh`." % JAR_GLOB)
    if len(jars) > 1:
        raise SystemExit("FAIL: %d Create jars in mods/: %s"
                         % (len(jars), ", ".join(p.name for p in jars)))
    return jars[0]


def ingredient_names(node):
    """Every item/tag id in an ingredient node, which may be a string, an object or a list.

    A list is Create's "any of these" form. Both members are named: the converter classifies each
    one, so an alternative nobody has looked at is still a hard failure there rather than here.
    """
    if isinstance(node, str):
        return [node]
    if isinstance(node, list):
        return [name for entry in node for name in ingredient_names(entry)]
    if isinstance(node, dict):
        if "item" in node:
            return ingredient_names(node["item"])
        if "tag" in node:
            return ["#" + node["tag"]]
    return []


def flatten(recipe):
    """A recipe's ingredients as {name: cell count}, shaped or shapeless alike."""
    counts = collections.Counter()
    if recipe["type"] == "minecraft:crafting_shapeless":
        for entry in recipe["ingredients"]:
            for name in ingredient_names(entry):
                counts[name] += 1
        return counts
    key = recipe.get("key", {})
    for row in recipe.get("pattern", []):
        for char in row:
            if char == " ":
                continue
            for name in ingredient_names(key.get(char)):
                counts[name] += 1
    return counts


def extract(jar_path, problems):
    corpus = {}
    stems = collections.defaultdict(list)
    with zipfile.ZipFile(jar_path) as jar:
        paths = [n for n in jar.namelist()
                 if n.startswith("data/create/recipe/") and n.endswith(".json")]
        for path in paths:
            recipe = json.loads(jar.read(path))
            if recipe.get("type") not in KEPT_TYPES:
                continue
            stems[Path(path).stem].append(path)

        for path in sorted(paths):
            recipe = json.loads(jar.read(path))
            if recipe.get("type") not in KEPT_TYPES:
                continue
            stem = Path(path).stem
            # Disambiguate only where the mod actually reuses a stem, so the common case reads as
            # the recipe's own name and a collision cannot silently drop one.
            if len(stems[stem]) > 1:
                parent = Path(path).parent.name
                stem = "%s__%s" % (parent, stem)

            result = recipe.get("result", {})
            output = result.get("id") or result.get("item")
            if not output:
                problems.append("%s has no result id" % path)
                continue

            counts = flatten(recipe)
            if not counts:
                problems.append("%s flattened to no ingredients" % path)
                continue

            corpus[stem] = {
                "ingredients": [{"amount": counts[name], "ingredient": name}
                                for name in sorted(counts)],
                "result": output,
                "result_count": result.get("count", 1),
                "source_type": recipe["type"],
            }
    return corpus


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true",
                        help="write nothing; fail if the corpus on disk differs from the jar")
    parser.add_argument("--quiet", action="store_true")
    args = parser.parse_args()

    jar = find_jar()
    problems = []
    corpus = extract(jar, problems)
    if problems:
        for problem in problems:
            print("FAIL: " + problem, file=sys.stderr)
        return 1

    text = json.dumps(corpus, indent=2, sort_keys=True) + "\n"

    if args.check:
        if not OUT.exists() or OUT.read_text() != text:
            print("FAIL: data/create/recipe.json does not match %s. Re-run "
                  "`scripts/create-recipe-extract.py` (ADR-0026: generated data is never "
                  "hand-edited)." % jar.name, file=sys.stderr)
            return 1
        if not args.quiet:
            print("ok: %d recipe(s) on disk match %s" % (len(corpus), jar.name))
        return 0

    OUT.write_text(text)
    if not args.quiet:
        by_type = collections.Counter(r["source_type"] for r in corpus.values())
        print("wrote %d recipe(s) from %s to %s"
              % (len(corpus), jar.name, OUT.relative_to(ROOT)))
        for source_type, count in sorted(by_type.items()):
            print("  %-32s %d" % (source_type, count))
    return 0


if __name__ == "__main__":
    sys.exit(main())
