#!/usr/bin/env python3
"""Assert the extracted resource corpus still says what ADR-0041 reads off it.

`scripts/factorio-resource-extract.py` reads a 28MB Factorio dump that is not in the repo,
so nothing here re-runs it -- the same arrangement the tech, recipe and machine checks
work under. What is checkable offline is whether the *committed* output still carries the
numbers the decision was made on:

  - **The starting totals re-derive.** Each resource's `starting_amount` is recomputed here
    from the committed formula and its own `base_density`, so a hand-edit to the total is a
    failure rather than a new fact. ADR-0041's per-block amount is this number divided by
    the blocks in a field, so a drifted total is a drifted patch.
  - **The alphabet is present and placed.** Iron, copper, coal and stone must each carry a
    starting patch; uranium must not, because Factorio gives it none and that is why Terra's
    starting area never had one. Terra's fifth field would otherwise be argued from an
    absence nobody checked.
  - **One distance law, shared.** Every resource carries the same `max((1000 + distance) /
    2600, 1)` term and the same flat-within radius. ADR-0041 quotes it as the arithmetic
    saying Factorio does not reward leaving early; if a resource ever carried its own, that
    sentence would be about the average of several laws rather than about the law.
  - **`PickTier` is no longer transcribed.** ADR-0039 labelled its two speeds as read off
    the wiki because the corpus held no resource dump; it holds one now, so the two tiers
    are asserted against the character's own `mining_speed` and `steel-axe`'s modifier,
    parsed out of `PickTier.java`. The pack's own `MINING_TIME` is *half* Factorio's, which
    is ADR-0039's amendment and stated here as a ratio rather than as a second number to
    keep in step. `steel-axe`'s modifier is a *fraction* -- `base * (1 + modifier)`, so +100%
    and not +1 -- which is what makes `PickTier.STEEL`'s 1.0 the researched speed.
  - **The stage ratios are material-independent, to rounding.** Iron, copper, coal and
    stone share one `stage_counts` list outright. Uranium's is that list scaled by about
    2/3 and then *rounded to two or three figures* -- its last rung is 50 where an exact
    scaling gives 53.3 -- so the fraction sets agree to a stated tolerance and not exactly.
    The tolerance is asserted here rather than smoothed away in the extractor, because the
    stages ADR-0041 renders are computed from a resource's own ratios.

Usage: tests/factorio/test_resource_extract.py
"""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent.parent

# The resources ADR-0041 places on Terra, and whether Factorio deals each a starting patch.
# Uranium is the false one and is the point of the row: it has none, so Terra has none.
STARTING_PATCH = {
    "iron-ore": True,
    "copper-ore": True,
    "coal": True,
    "stone": True,
    "uranium-ore": False,
}

# ADR-0041's table, which is quoted in prose and so must not drift silently.
STARTING_TOTALS = {
    "iron-ore": 400_000,
    "copper-ore": 320_000,
    "coal": 320_000,
    "stone": 160_000,
}

# How far apart two resources' stage ratios may sit and still be one fraction set.
# Uranium's rounding is the whole reason there is a tolerance; see the module docstring.
RATIO_TOLERANCE = 0.001

PICK_TIER = "mod/src/main/java/com/planetaryfactory/core/mining/PickTier.java"

# The four resources ADR-0039's flat mining time speaks for. Uranium is excluded on
# Factorio's own terms rather than on the pack's: its `mining_time` is 2 and it wants
# sulfuric acid, so it was never one of the four that number was flat across.
FLAT_MINING_TIME = ("iron-ore", "copper-ore", "coal", "stone")

# ADR-0039 halves Factorio's mining time; the amendment is this ratio, not a second number.
PACK_MINING_TIME_RATIO = 0.5


def pick_tiers(source):
    """The two tiers' mining speeds and the pack's mining time, read out of the Java."""
    speeds = {
        name: float(speed)
        for name, speed in re.findall(r'(\w+)\("[^"]+",\s*([0-9.]+)f\)', source)
    }
    time = re.search(r"MINING_TIME\s*=\s*([0-9.]+)f", source)
    return speeds, float(time.group(1)) if time else None


def main():
    data = json.loads((ROOT / "data/factorio/resource.json").read_text())
    resources = {r["name"]: r for r in data["resources"]}
    formula = data["starting_amount_formula"]
    controls = data["controls"]
    failures = []

    for name, wants_patch in sorted(STARTING_PATCH.items()):
        resource = resources.get(name)
        if resource is None:
            failures.append(f"{name} is not in the corpus -- Terra's alphabet has a hole")
            continue
        if resource["has_starting_area_placement"] != wants_patch:
            failures.append(
                f"{name} has_starting_area_placement is {resource['has_starting_area_placement']}, "
                f"expected {wants_patch}"
            )
        if wants_patch and not resource["base_density"]:
            failures.append(f"{name} carries no base_density -- its total has no source")

    for name, resource in sorted(resources.items()):
        total = resource["starting_amount"]
        if not resource["has_starting_area_placement"]:
            if total is not None:
                failures.append(f"{name} has no starting patch but states a total of {total}")
            continue
        want = eval(  # noqa: S307 -- the formula is the dump's and the names are bound here
            formula,
            {"__builtins__": {}},
            {
                "base_density": resource["base_density"],
                "frequency_multiplier": controls["frequency_multiplier"],
                "size_multiplier": controls["size_multiplier"],
            },
        )
        if total is None or abs(total - want) > 1e-6:
            failures.append(
                f"{name} states a starting total of {total}, but {formula} on "
                f"base_density {resource['base_density']} gives {want}"
            )

    for name, want in sorted(STARTING_TOTALS.items()):
        resource = resources.get(name)
        if resource and resource["starting_amount"] != want:
            failures.append(
                f"{name} starting total is {resource['starting_amount']}, and ADR-0041 quotes {want}"
            )

    laws = {
        (r["distance_law"] or {}).get("term") for r in resources.values()
    }
    if len(laws) != 1 or None in laws:
        failures.append(f"resources carry {len(laws)} distance laws, not one: {sorted(map(str, laws))}")
    else:
        flat = {(r["distance_law"] or {})["flat_within"] for r in resources.values()}
        if flat != {1600}:
            failures.append(f"the distance law is flat within {sorted(flat)} tiles, and ADR-0041 quotes 1600")

    staged = {n: r for n, r in resources.items() if len(r["stage_counts"]) > 1}
    if len(staged) < len(STARTING_PATCH):
        failures.append(
            f"only {len(staged)} resources carry sprite stages; every ore in the alphabet needs them"
        )
    reference = staged.get("iron-ore", {}).get("stage_ratios")
    if not reference:
        failures.append("iron-ore carries no stage ratios -- there is nothing to render a stage from")
    else:
        if abs(reference[0] - 1.0) > 1e-9:
            failures.append(f"iron-ore's first stage ratio is {reference[0]}, not a full block")
        for name, resource in sorted(staged.items()):
            ratios = resource["stage_ratios"]
            if len(ratios) != len(reference):
                failures.append(
                    f"{name} has {len(ratios)} stages against iron-ore's {len(reference)}"
                )
                continue
            drift = max(abs(a - b) for a, b in zip(ratios, reference))
            if drift > RATIO_TOLERANCE:
                failures.append(
                    f"{name}'s stage ratios differ from iron-ore's by {drift:.4f}, over the "
                    f"{RATIO_TOLERANCE} rounding tolerance -- the fraction set is no longer shared"
                )

    hand = data.get("hand_mining") or {}
    source = (ROOT / PICK_TIER).read_text()
    speeds, mining_time = pick_tiers(source)
    bare = hand.get("character_mining_speed")
    researched = hand.get("character_mining_speed_researched")
    if speeds.get("IRON") != bare:
        failures.append(
            f"PickTier.IRON mines at {speeds.get('IRON')}, and Factorio's character at {bare}"
        )
    if speeds.get("STEEL") != researched:
        failures.append(
            f"PickTier.STEEL mines at {speeds.get('STEEL')}, and Factorio's character after "
            f"steel-axe at {researched}"
        )
    factorio_times = {
        resources[name]["mining_time"] for name in FLAT_MINING_TIME if name in resources
    }
    if len(factorio_times) != 1:
        failures.append(
            f"the four flat resources carry {sorted(factorio_times)} mining times, not one "
            "-- ADR-0039's flat time no longer speaks for them"
        )
    elif mining_time is None:
        failures.append("PickTier states no MINING_TIME")
    else:
        want = factorio_times.pop() * PACK_MINING_TIME_RATIO
        if abs(mining_time - want) > 1e-6:
            failures.append(
                f"PickTier.MINING_TIME is {mining_time}, and half Factorio's is {want} "
                "-- ADR-0039 halves it and does not choose it"
            )

    for key in ("starting_resource_placement_radius", "regular_patch_fade_in_distance",
                "double_density_distance"):
        if not data["constants"].get(key):
            failures.append(f"the outfield law is missing {key}")
    if "regular_density_at" not in data["outfield_law"]:
        failures.append("no regular_density_at -- the outfield law did not come across")

    for index, failure in enumerate(failures, 1):
        print(f"FAIL {index}: {failure}")
    if failures:
        return 1
    print(
        f"ok   {len(resources)} resources, {len(staged)} with stages, one distance law "
        f"flat within 1600 tiles; totals re-derive from {formula}; PickTier {bare}/{researched} "
        f"matches the character"
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
