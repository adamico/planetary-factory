#!/usr/bin/env python3
"""Copy the tree corpus's felling rate into the resource the mod reads (#205, ADR-0051).

ADR-0051 charges `amount * seconds_per_log` for one felling gesture, where the amount is the tree's
own log count and the rate is Factorio's: `tree-01.mining_time 0.55` over `wood 4`, or 0.1375
seconds. The rate is extracted by `scripts/factorio-tree-extract.py`; this script is the short hop
from `data/factorio/tree.json` to
`mod/src/main/resources/planetaryfactory_core/felling/trees.json`, which `TreeCorpus` reads at
class-init the same way `PumpCorpus` reads `fluid/pumps.json` and `RigCorpus` reads
`mining/drills.json`.

**It copies; it does not derive.** The quotient is the extractor's, computed against every living
Nauvis prototype and refused if they disagree. What arrives here is one number and the names it came
from, so a reader can see which prototypes back it without opening a 28MB dump.

The names are carried for exactly that reason and for one more: they are what
`tests/factorio/test_tree_extract.py` re-derives the rate from, so a hand-edited resource is caught
against the corpus rather than against a literal typed twice.

Usage:

    scripts/build-tree-assets.py            # writes the resource
    scripts/build-tree-assets.py --check    # asserts it is already up to date; no writes
"""
import argparse
import json
import os
import sys

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
TREE_CORPUS = os.path.join(ROOT, "data", "factorio", "tree.json")
TREE_RESOURCE = os.path.join(
    ROOT, "mod", "src", "main", "resources", "planetaryfactory_core", "felling", "trees.json"
)


def resource_from_corpus():
    with open(TREE_CORPUS, encoding="utf-8") as handle:
        corpus = json.load(handle)
    rate = corpus.get("seconds_per_log")
    sources = corpus.get("rate_from") or []
    if not rate or not sources:
        sys.exit(
            f"{os.path.relpath(TREE_CORPUS, ROOT)} has no rate -- "
            "run scripts/factorio-tree-extract.py"
        )
    return {"seconds_per_log": rate, "rate_from": sources}


def check():
    expected = resource_from_corpus()
    if not os.path.isfile(TREE_RESOURCE):
        sys.exit(f"build-tree-assets.py --check failed:\nmissing: {TREE_RESOURCE}")
    with open(TREE_RESOURCE, encoding="utf-8") as handle:
        actual = json.load(handle)
    if actual != expected:
        sys.exit(
            "build-tree-assets.py --check failed:\n"
            f"stale: {os.path.relpath(TREE_RESOURCE, ROOT)}\n"
            f"  corpus says {expected['seconds_per_log']}, resource says "
            f"{actual.get('seconds_per_log')}"
        )
    print(f"OK -- {expected['seconds_per_log']}s per log, from {len(expected['rate_from'])} prototypes")


def build():
    expected = resource_from_corpus()
    os.makedirs(os.path.dirname(TREE_RESOURCE), exist_ok=True)
    with open(TREE_RESOURCE, "w", encoding="utf-8") as handle:
        json.dump(expected, handle, indent=2)
        handle.write("\n")
    print(
        f"wrote {os.path.relpath(TREE_RESOURCE, ROOT)} -- "
        f"{expected['seconds_per_log']}s per log"
    )


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    check() if args.check else build()


if __name__ == "__main__":
    main()
