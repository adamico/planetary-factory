#!/usr/bin/env python3
"""Assert `data/pack/subgroup-owner.json` still covers the corpus and says only legal things.

The file is #88's guard rail: which mod owns each of Factorio's subgroups (read off
ADR-0017, never invented) and -- where it is known -- which recipe type crafts it. It
routes nothing. Its job is to be the thing `process-map.json` is checked against once that
exists, so a recipe quietly handed to the wrong machine fails a test instead of shipping.

What is checkable now, with no converter and no game:

  - the file covers every subgroup in the corpus and nothing else, and each `count` is
    real, because a shelf that gains a recipe in a Factorio update must not be routed by a
    row written before it existed
  - every `split` entry names every recipe on its shelf, since a partial split is a silent
    fallback and a fallback is how `crafting` -> `assembling` became wholesale
  - every owner and process is in the vocabulary, and a `mod / process` pair really does
    cross mods (a same-mod pair is one token, so the cross-mod cases stay visible)
  - a cross-mod pair explains itself: owner != process without a `cross_owner` reason is
    exactly the unexamined routing this file exists to prevent
  - the dual-surface rows are still the two the bootstrap earns, because a recipe on two
    surfaces is a rung the player can skip
"""

import json
import sys
from collections import Counter, defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent.parent

# ADR-0017's owners, plus `pack` for the three rows that name a machine this pack registers
# on a GT chassis rather than a mod (ADR-0025, ADR-0026).
MODS = {"gregtech", "create", "mekanism", "electro", "pack"}

# Terminal values: not a machine, and deliberately so. `undecided` means ADR-0017 has no row
# for the capability -- a decision nobody has taken, not an oversight. `undecided:smelting`
# is narrower: Factorio smelts ore straight to plate in one hop, so the shape is settled and
# only the furnace's owner is open.
TERMINALS = {
    "undecided",
    "undecided:smelting",
    "deferred",
    "not_emitted",
    "personal_assembler",
}

# Every recipe type the file may name. A process outside this set is either a typo or a
# machine nobody registered; both should fail here rather than at datapack load.
PROCESSES = {
    "create:pressing",
    "create:filling",
    "create:emptying",
    "create:mixing",
    "pack:assembling",
    "pack:chemical_plant",
    "pack:oil_refinery",
    "pack:rocket_silo",
}

# Rung 1's bootstrap: the machine, and the pack that buys it. Both must exist on the
# Personal Assembler (or the first Assembling Machine is unbuildable) and on the Assembling
# Machine (or neither can ever be scaled). Nothing else earns two surfaces, so a third dual
# row is a finding -- it is a rung the player can skip.
EXPECTED_DUAL = {"assembling-machine-1", "automation-science-pack"}


def parse(value):
    """`owner / process`, or a single token that is one or the other."""
    parts = [part.strip() for part in value.split("/")]
    return (parts[0], parts[1]) if len(parts) == 2 else (None, parts[0])


def main():
    owners = json.loads((ROOT / "data/pack/subgroup-owner.json").read_text())
    recipes = json.loads((ROOT / "data/factorio/recipe.json").read_text())
    shelves = owners["subgroups"]

    corpus = defaultdict(set)
    for recipe in recipes:
        corpus[f"{recipe['group']}/{recipe['subgroup']}"].add(recipe["name"])

    failures = []

    for shelf in sorted(set(corpus) - set(shelves)):
        failures.append(f"{shelf} is in the corpus and has no owner row")
    for shelf in sorted(set(shelves) - set(corpus)):
        failures.append(f"{shelf} has an owner row and is not in the corpus")

    dual = set()
    counted = 0
    for shelf, entry in sorted(shelves.items()):
        if shelf not in corpus:
            continue
        if entry["count"] != len(corpus[shelf]):
            failures.append(
                f"{shelf} claims {entry['count']} recipes, the corpus has "
                f"{len(corpus[shelf])}"
            )

        if "per_recipe" not in entry:
            counted += entry["count"]
            if entry["owner"] not in MODS | TERMINALS:
                failures.append(f"{shelf} has owner {entry['owner']!r}")
            process = entry.get("process")
            if process is not None:
                if process not in PROCESSES:
                    failures.append(f"{shelf} has process {process!r}")
                elif process.split(":")[0] != entry["owner"] and "cross_owner" not in entry:
                    failures.append(
                        f"{shelf}: owner {entry['owner']} crafts on {process} with no "
                        "cross_owner reason -- say why, or the routing is unexamined"
                    )
            continue

        missing = corpus[shelf] - set(entry["per_recipe"])
        extra = set(entry["per_recipe"]) - corpus[shelf]
        if missing:
            failures.append(f"{shelf} splits but omits {sorted(missing)}")
        if extra:
            failures.append(f"{shelf} splits on recipes not in the corpus: {sorted(extra)}")

        for name, value in sorted(entry["per_recipe"].items()):
            counted += 1
            values = value if isinstance(value, list) else [value]
            if isinstance(value, list):
                dual.add(name)
            for one in values:
                owner, process = parse(one)
                if owner is None:
                    if process not in PROCESSES | TERMINALS:
                        failures.append(f"{shelf}/{name}: {process!r} is not a known value")
                    continue
                if owner not in MODS:
                    failures.append(f"{shelf}/{name}: owner {owner!r}")
                if process not in PROCESSES:
                    failures.append(f"{shelf}/{name}: process {process!r}")
                elif process.split(":")[0] == owner:
                    failures.append(
                        f"{shelf}/{name}: {one!r} pairs a mod with its own machine -- write "
                        "it as one token, so the cross-mod rows stay visible"
                    )

    if counted != len(recipes):
        failures.append(f"{counted} recipes assigned, the corpus has {len(recipes)}")

    if dual != EXPECTED_DUAL:
        failures.append(
            f"dual-surface rows are {sorted(dual)}, expected {sorted(EXPECTED_DUAL)} -- a "
            "recipe on two surfaces is a rung the player can skip; only the bootstrap "
            "earns it, so read a new one as a finding"
        )

    for number, failure in enumerate(failures, 1):
        print(f"FAIL {number}: {failure}")
    if failures:
        return 1
    print(
        f"ok   {len(shelves)} subgroups, {counted} recipes, every owner sourced, "
        "every cross-mod pair explained"
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
