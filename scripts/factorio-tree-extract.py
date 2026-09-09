#!/usr/bin/env python3
"""Extract Factorio's tree prototypes -- what a tree costs to fell, and what it yields.

ADR-0051 states that a tree is one entity holding an amount, and that the pack keeps
Factorio's **rate** while diverging on the **amount**: the yield is the log count of the
tree actually broken rather than Factorio's flat `wood x4`. The rate is the quotient this
script exists to produce -- `mining_time / wood`, seconds per log -- so the 0.1375 the mod
charges is derived from the game's own prototypes and never typed.

Same dump as the other five extractors and the same provenance block in
`data/factorio/README.md`.

Three things this script decides, because they are properties of the data:

  - **The rate comes from the living Nauvis trees, and the data names them.** Two fields do
    it, and neither is a name list: `autoplace.control == "trees"` is the Nauvis worldgen
    control, which Gleba's `funneltrunk` and `water-cane` are not on; and `colors` is the
    leaf-colour set a *living* tree has and a dry or dead one does not. That is 15
    prototypes, all at `mining_time 0.55` for `wood 4`.

    Both halves are load-bearing. Autoplace alone admits Gleba's wood-bearing plants at
    0.0833 and 0.1; the Nauvis control alone admits `dry-tree` at 0.5 for the same 4 wood.
    The 0.5 that looks like the obvious tree number belongs to the dead trees and to the
    `plant` prototypes, and reading it as the tree rate is exactly the mistake #205 made
    before this ran. Everything excluded is still extracted, so the file shows the
    difference rather than hiding it.
  - **`plant` prototypes are extracted, and are not trees.** Yumako and jellystem yield 50
    fruit and zero wood, are consumed by the harvest and regrow only from a seed. ADR-0051
    keeps them out of the felling rule and #23 owns them; they are carried here so that
    ticket reads numbers rather than re-deriving them, and so the difference is visible in
    the committed file rather than only in prose.
  - **The rate is a single number or the extraction fails.** If the living trees ever
    disagree on `mining_time / wood`, "the Factorio rate" has stopped being a thing and the
    mod cannot charge one. That is a hard failure here rather than a silent average.

What this script does not do is decide anything. ADR-0051 holds the rule.

Usage:

    scripts/factorio-tree-extract.py            # finds the dump, writes data/factorio/tree.json
    scripts/factorio-tree-extract.py --dump PATH
"""

import argparse
import json
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent

DEFAULT_DUMP = (
    Path.home()
    / "Library/Application Support/factorio/script-output/data-raw-dump.json"
)

# The item a tree has to yield to be the kind of tree ADR-0051 is about.
WOOD = "wood"

# Nauvis's worldgen control for trees. Gleba's plants ride `gleba_plants`.
NAUVIS_CONTROL = "trees"

# A living tree's leaves have colours; a dry or dead trunk has `pictures` and no `colors`.
LIVING = "colors"


def results(minable):
    """A prototype's mining results as `{name: amount}`.

    Factorio writes this two ways -- a `results` list, or the older single
    `result`/`count` pair -- and both are live in the same dump.
    """
    if not isinstance(minable, dict):
        return {}
    listed = minable.get("results")
    if isinstance(listed, list):
        return {
            entry["name"]: entry.get("amount", entry.get("count", 1))
            for entry in listed
            if isinstance(entry, dict) and entry.get("name")
        }
    if minable.get("result"):
        return {minable["result"]: minable.get("count", 1)}
    return {}


def row(name, prototype):
    """One prototype, flattened to the fields ADR-0051 and #23 read."""
    minable = prototype.get("minable") or {}
    return {
        "name": name,
        "mining_time": minable.get("mining_time"),
        "results": results(minable),
        # Which worldgen control places it -- `trees` is Nauvis, `gleba_plants` is not --
        # and whether its leaves have colours, which is what a dry trunk lacks.
        "autoplace_control": (prototype.get("autoplace") or {}).get("control"),
        "living": LIVING in prototype,
        # `plant` only: ticks from seed to harvestable, and the seed that plants it.
        "growth_ticks": prototype.get("growth_ticks"),
    }


def extract(dump):
    trees = [row(name, p) for name, p in sorted((dump.get("tree") or {}).items())]
    plants = [row(name, p) for name, p in sorted((dump.get("plant") or {}).items())]
    return trees, plants


def nauvis_living(trees):
    """The Nauvis trees an engineer fells: on Nauvis's control, alive, and wood-bearing."""
    return [
        t
        for t in trees
        if t["autoplace_control"] == NAUVIS_CONTROL
        and t["living"]
        and WOOD in t["results"]
    ]


def rate(trees):
    """Seconds per log -- `mining_time / wood` -- and it must be one number.

    ADR-0051 charges `amount * rate`, so a disagreement between prototypes is not something
    to average. It means the corpus no longer has a rate to keep.
    """
    quotients = {
        round(t["mining_time"] / t["results"][WOOD], 12): t["name"] for t in trees
    }
    if len(quotients) != 1:
        disagreement = ", ".join(f"{name}={q}" for q, name in sorted(quotients.items()))
        sys.exit(
            "the living trees disagree on seconds-per-wood, so there is no Factorio "
            f"rate to keep: {disagreement}"
        )
    return next(iter(quotients))


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--dump", type=Path, default=DEFAULT_DUMP)
    parser.add_argument("--out", type=Path, default=REPO / "data" / "factorio" / "tree.json")
    args = parser.parse_args()

    if not args.dump.is_file():
        sys.exit(
            f"no dump at {args.dump}\n"
            "run:  factorio --dump-data --mod-directory <dir with base+SA only>"
        )

    dump = json.loads(args.dump.read_text(encoding="utf-8"))
    trees, plants = extract(dump)
    nauvis = nauvis_living(trees)
    if not nauvis:
        sys.exit("no living wood-bearing Nauvis tree in the dump -- the `tree` type moved")

    seconds_per_log = rate(nauvis)
    out = {
        "seconds_per_log": seconds_per_log,
        "rate_from": sorted(t["name"] for t in nauvis),
        "trees": trees,
        "plants": plants,
    }
    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(json.dumps(out, indent=2) + "\n", encoding="utf-8")

    print(f"{len(trees)} trees ({len(nauvis)} living Nauvis), {len(plants)} plants")
    print(f"seconds per log: {seconds_per_log}")
    print(f"wrote      {args.out.relative_to(REPO)}\n")
    print(f"{'prototype':22} {'time':>6}  {'control':13} {'alive':5} results")
    for tree in trees:
        yields = ", ".join(f"{n} x{a}" for n, a in sorted(tree["results"].items()))
        print(
            f"{tree['name']:22} {tree['mining_time'] or 0:6.2f}  "
            f"{tree['autoplace_control'] or '-':13} "
            f"{'yes' if tree['living'] else ' - ':5} {yields}"
        )
    print()
    for plant in plants:
        yields = ", ".join(f"{n} x{a}" for n, a in sorted(plant["results"].items()))
        print(
            f"{plant['name']:22} {plant['mining_time'] or 0:6.2f}  "
            f"grows in {plant['growth_ticks']} ticks -- {yields}"
        )


if __name__ == "__main__":
    main()
