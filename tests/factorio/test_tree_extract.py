#!/usr/bin/env python3
"""Assert the extracted tree corpus still says what ADR-0051 reads off it.

`scripts/factorio-tree-extract.py` reads a 28MB Factorio dump that is not in the repo, so
nothing here re-runs it. What is checkable without the dump -- and without launching the
game -- is whether the *committed* output still supports the decision:

  - **the rate is re-derived, not trusted.** `seconds_per_log` is recomputed from every row
    the file says it came from, the way `test_fuel_extract.py` re-derives the joules. A
    hand-edited 0.1375 fails here, which matters more than usual: the mod charges this
    number and nothing in a running game would look wrong if it were 0.125.
  - **the discriminant still discriminates.** The rate must come from the *living Nauvis*
    trees and no others. Three prototypes are named as the ones it must exclude, each for a
    different reason and each of which produced a different wrong rate while #205 was being
    written: `dry-tree` is on Nauvis's own control and yields the same `wood x4` at 0.5;
    `funneltrunk` is alive and wood-bearing but Gleba's; `dead-tree-desert` is Nauvis and
    dead. If the file stops excluding them the rate is wrong rather than merely unexercised.
  - **the divergence ADR-0051 declares is real.** Factorio's tree yields a flat `wood x4`.
    The ADR says so and then diverges from it deliberately, so the 4 has to still be there:
    the day the corpus stops saying 4, the ADR is arguing with nothing.
  - **`plant` is not a tree.** Yumako and jellystem must keep yielding fruit and *zero
    wood*, at a flat mining time, with a growth timer. That is the whole basis for keeping
    them out of the felling rule and handing them to #23; if a regeneration ever shows them
    yielding wood, the deferral needs re-arguing rather than assuming.

Usage: tests/factorio/test_tree_extract.py
"""
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent.parent

# What ADR-0051 charges per log, and what the mod is built on.
EXPECTED_RATE = 0.1375

# Factorio's own tree yield -- the number the pack diverges from on purpose.
FACTORIO_WOOD = 4

# Prototypes the rate must not be drawn from, and why each one is a trap.
MUST_EXCLUDE = {
    "dry-tree": "Nauvis's own control and the same wood x4, but dead -- gives 0.125",
    "funneltrunk": "alive and wood-bearing, but Gleba's -- gives 0.0833",
    "dead-tree-desert": "Nauvis and dead -- gives 0.25",
}

# The two plants #23 owns, and the fruit each must still yield.
PLANTS = {"yumako-tree": "yumako", "jellystem": "jellynut"}


def main():
    path = ROOT / "data" / "factorio" / "tree.json"
    if not path.is_file():
        sys.exit(f"missing {path} -- run scripts/factorio-tree-extract.py")
    data = json.loads(path.read_text(encoding="utf-8"))

    failures = []
    trees = {t["name"]: t for t in data["trees"]}
    plants = {p["name"]: p for p in data["plants"]}

    # The rate, re-derived from the rows the file credits it to.
    sources = data["rate_from"]
    if not sources:
        failures.append("rate_from is empty -- nothing supports seconds_per_log")
    quotients = set()
    for name in sources:
        tree = trees.get(name)
        if tree is None:
            failures.append(f"rate_from names {name}, which is not in trees")
            continue
        wood = tree["results"].get("wood")
        if not wood:
            failures.append(f"rate source {name} yields no wood")
            continue
        quotients.add(round(tree["mining_time"] / wood, 12))
        if wood != FACTORIO_WOOD:
            failures.append(
                f"{name} yields wood x{wood}; ADR-0051 diverges from Factorio's "
                f"flat x{FACTORIO_WOOD} and needs the 4 to still be there"
            )
    if len(quotients) > 1:
        failures.append(f"rate sources disagree: {sorted(quotients)}")
    elif quotients:
        derived = quotients.pop()
        if derived != data["seconds_per_log"]:
            failures.append(
                f"seconds_per_log is {data['seconds_per_log']}, but its own sources "
                f"derive {derived} -- the file was edited by hand"
            )
        if derived != EXPECTED_RATE:
            failures.append(
                f"the rate is {derived}, not the {EXPECTED_RATE} ADR-0051 and the mod "
                "are built on"
            )

    # The discriminant: each of these would have produced a different, plausible rate.
    for name, why in MUST_EXCLUDE.items():
        if name not in trees:
            failures.append(f"{name} is gone from the corpus; it is the check's control")
        elif name in sources:
            failures.append(f"{name} is being used for the rate -- {why}")

    # A plant is not a tree, which is why #23 owns them and #205 does not.
    for name, fruit in PLANTS.items():
        plant = plants.get(name)
        if plant is None:
            failures.append(f"{name} is not in plants -- ADR-0051's deferral names it")
            continue
        if "wood" in plant["results"]:
            failures.append(
                f"{name} now yields wood; it was a plant precisely because it does not, "
                "so the deferral to #23 needs re-arguing"
            )
        if not plant["results"].get(fruit):
            failures.append(f"{name} no longer yields {fruit}")
        if not plant["growth_ticks"]:
            failures.append(f"{name} has no growth_ticks -- a plant regrows, a tree does not")

    for failure in failures:
        print(f"FAIL  {failure}")
    if failures:
        sys.exit(1)
    print(
        f"ok  {len(data['trees'])} trees, {len(sources)} living Nauvis at "
        f"{data['seconds_per_log']}s per log, {len(plants)} plants and none of them wood"
    )


if __name__ == "__main__":
    main()
