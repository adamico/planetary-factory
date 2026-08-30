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
  - the Personal Assembler is a RULE and not a field: no row names it, and the rule as
    INTENDED ("anything without a fluid, except what needs a real machine") still picks out
    the same recipes as the rule as ENFORCED (first category `crafting`)
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

# Terminal values for the PROCESS axis, as distinct from the owner axis above. `native_mechanic`
# means the capability is in scope and fully supported, by a mod mechanic that needs no recipe:
# Create's Spout and Item Drain key on `IFluidHandlerItem`, so barrelling works and the pack emits
# nothing BECAUSE it works (#93). Separate from `not_emitted`, which means cut from the corpus --
# filing a working mechanic under that token would read to a later author as a removed feature.
PROCESS_TERMINALS = {
    "native_mechanic",
}

# The Personal Assembler crafts what Assembling Machine 1 crafts: first category `crafting`.
# Stored nowhere, derived here. The corpus's one `advanced-crafting` recipe is the rule's whole
# boundary -- Factorio withholds `engine-unit` from the hand although it needs no fluid -- so if
# a regeneration ever moves it, the rule stops meaning what its prose says and this fails.
HAND_CATEGORY = "crafting"

# The exceptions, stated so they can be READ -- they are asserted below by deriving them, never
# by matching this list, so this stays documentation and cannot rot into the source of truth.
# All 11 are fluid-free recipes Factorio still withholds from the hand, and each already has a
# home elsewhere in the file: the furnace is #91, the centrifuge #89, asteroids are not emitted,
# the silo is `pack:rocket_silo`. `engine-unit` is the one that earns the care: nothing about its
# ingredients says "machine", so "needs no fluid" alone would have swallowed it.
WITHHELD = {
    "iron-plate", "copper-plate", "steel-plate", "stone-brick",   # smelting
    "uranium-processing", "nuclear-fuel-reprocessing",            # centrifuging
    "metallic-asteroid-crushing", "carbonic-asteroid-crushing",   # crushing
    "oxide-asteroid-crushing",
    "rocket-part",                                               # rocket-building
    "engine-unit",                                               # advanced-crafting
}

# (recipes, shelves touched, shelves wholly hand-craftable, shelves split down the middle).
# The five split shelves are the point: the rule is a predicate over categories, so it crosses
# shelves instead of following them, and a shelf could not store it even if a row wanted to.
EXPECTED_SPREAD = (
    113,
    29,
    24,
    ["belt", "intermediate-product", "space-interactors", "terrain", "uranium-processing"],
)


def takes_fluid(recipe):
    """Factorio marks fluids on the ingredient itself; a recipe is fluid-free if none is."""
    both = list(recipe.get("ingredients") or []) + list(recipe.get("results") or [])
    return any(item.get("type") == "fluid" for item in both if isinstance(item, dict))


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
            if process is not None and process not in PROCESS_TERMINALS:
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
            for one in values:
                owner, process = parse(one)
                if owner is None:
                    if process not in PROCESSES | TERMINALS | PROCESS_TERMINALS:
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

    # The rule cuts across shelves, not along them, so a shelf cannot express it and no row
    # may try: a row naming the surface would read as "only these", which is the opposite of
    # what the rule says.
    named = sorted(
        f"{shelf}/{name}"
        for shelf, entry in shelves.items()
        for name, value in entry.get("per_recipe", {}).items()
        for one in (value if isinstance(value, list) else [value])
        if one == "personal_assembler"
    )
    if named:
        failures.append(
            f"{named} name the Personal Assembler as a value -- it is a rule over categories, "
            "not a field; a row that names it reads as an exclusive claim"
        )

    # The rule has two formulations: INTENDED, "anything that needs no fluid except what needs a
    # real machine", and ENFORCED, "first category is `crafting`". The second is what ships,
    # because it needs no list maintained; the first is what it MEANS. If they ever pick out
    # different recipes, the shipped rule has stopped saying what the prose says it says.
    hand = {r["name"] for r in recipes if r["category"] == HAND_CATEGORY}
    fluid_free = {r["name"] for r in recipes if not takes_fluid(r)}
    if fluid_free - hand != WITHHELD:
        failures.append(
            f"fluid-free recipes withheld from the hand are {sorted(fluid_free - hand)}, "
            f"expected {sorted(WITHHELD)} -- the two formulations of the rule have come apart, "
            "so `_comment`'s statement of intent no longer describes what is enforced"
        )

    # `_comment` cites the spread as its argument for why the rule is not a shelf field. Prose
    # that cites a number goes stale silently, so the number is pinned here.
    touched = {r["subgroup"] for r in recipes if r["name"] in hand}
    partial = {
        r["subgroup"] for r in recipes if r["name"] not in hand and r["subgroup"] in touched
    }
    spread = (len(hand), len(touched), len(touched - partial), sorted(partial))
    if spread != EXPECTED_SPREAD:
        failures.append(
            f"hand-craftable spread is {spread}, expected {EXPECTED_SPREAD} -- `_comment` "
            "argues from these numbers that a shelf is the wrong granularity for the rule"
        )

    for number, failure in enumerate(failures, 1):
        print(f"FAIL {number}: {failure}")
    if failures:
        return 1
    print(
        f"ok   {len(shelves)} subgroups, {counted} recipes, {len(hand)} hand-craftable by rule, "
        "every owner sourced, every cross-mod pair explained"
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
