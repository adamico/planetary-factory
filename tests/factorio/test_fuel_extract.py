#!/usr/bin/env python3
"""Assert the extracted fuel corpus still says what ADR-0047 reads off it.

`scripts/factorio-fuel-extract.py` reads a 28MB Factorio dump that is not in the repo, so
nothing here re-runs it. What is checkable without the dump -- and without launching the
game -- is whether the *committed* output still supports the decision:

  - **the joules are re-derived, not trusted.** Every row's `fuel_value` is recomputed from
    its own `fuel_value_raw` string, the way `test_resource_extract.py` re-derives the ore
    totals from Factorio's own formula. A hand-edited number fails here.
  - **the burn arithmetic ADR-0047 is built on.** A tick of work spends
    `energy_usage / 20`, so coal's 4 MJ is 888 whole ticks of Stone-tier work -- and the
    Steel tier, drawing the same 90 kW at twice the speed, gets exactly twice the crafts
    from it. That equality is the ladder's efficiency story; #155 asserted it in a javadoc
    and computed it from nothing.
  - **the category filter has something to filter.** `uranium-fuel-cell` must stay
    `nuclear` and `nuclear-fuel` must stay `chemical` -- the pair is the whole reason
    ADR-0047 carries the category rather than letting a `fuel_value` be sufficient. The
    day the names stop meaning that, the filter is wrong rather than merely unexercised.
  - **the burners agree with the fuels.** Both burner furnaces accept `chemical` at
    `effectivity: 1`, read from `machine.json` rather than typed. A machine's burner block
    and a fuel's category are two halves of one lookup, and they are extracted separately.
  - **no fluids, no duplicates.** A fluid fuel is a thruster's, not a furnace slot's.

Usage: tests/factorio/test_fuel_extract.py
"""
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent.parent

UNITS = {"k": 1e3, "M": 1e6, "G": 1e9, "T": 1e12}

TICKS_PER_SECOND = 20

# The pair the category filter exists for: same chapter, same word in the name, opposite
# answers. `nuclear-fuel` is an ordinary chemical fuel a furnace burns; the cell is not.
CATEGORY_PAIR = {"nuclear-fuel": "chemical", "uranium-fuel-cell": "nuclear"}

# ADR-0047's worked example, and the two tiers it is worked on.
BURNER_FURNACES = ("stone-furnace", "steel-furnace")


def joules(raw):
    """Re-parse a Factorio energy string, independently of the extractor."""
    text = str(raw).strip().rstrip("Jj")
    scale = UNITS.get(text[-1:])
    return float(text) if scale is None else float(text[:-1]) * scale


def main():
    fuel_data = json.loads((ROOT / "data/factorio/fuel.json").read_text())
    machine_data = json.loads((ROOT / "data/factorio/machine.json").read_text())
    fuels = fuel_data["fuels"]
    categories = fuel_data["categories"]
    by_name = {f["name"]: f for f in fuels}
    machines = {m["name"]: m for m in machine_data["machines"]}
    failures = []

    if not fuels:
        failures.append("fuel.json lists no fuels")

    seen = set()
    for fuel in fuels:
        key = (fuel["type"], fuel["name"])
        if key in seen:
            failures.append(f"{fuel['name']} appears twice as a {fuel['type']}")
        seen.add(key)

        if fuel["type"] == "fluid":
            failures.append(
                f"{fuel['name']} is a fluid -- a thruster's fuel is not a furnace slot's"
            )
        want = joules(fuel["fuel_value_raw"])
        if fuel["fuel_value"] is None or abs(fuel["fuel_value"] - want) > 1e-6:
            failures.append(
                f"{fuel['name']} carries {fuel['fuel_value']} J, but its own "
                f"{fuel['fuel_value_raw']!r} re-derives to {want}"
            )
        if not fuel["fuel_value"] or fuel["fuel_value"] <= 0:
            failures.append(f"{fuel['name']} has no fuel value")
        if not fuel["fuel_category"]:
            failures.append(f"{fuel['name']} has no fuel category")
        elif fuel["fuel_category"] not in categories:
            failures.append(
                f"{fuel['name']} is {fuel['fuel_category']!r}, which is not a declared category"
            )

    for name, category in sorted(CATEGORY_PAIR.items()):
        fuel = by_name.get(name)
        if not fuel:
            failures.append(f"{name} is not in the corpus -- ADR-0047's category pair is broken")
        elif fuel["fuel_category"] != category:
            failures.append(
                f"{name} is {fuel['fuel_category']!r}, not {category!r} -- ADR-0047's "
                "category filter is asserting the wrong thing"
            )

    for name in BURNER_FURNACES:
        machine = machines.get(name)
        if not machine:
            failures.append(f"{name} is not in machine.json")
            continue
        burner = machine.get("burner")
        if not burner:
            failures.append(f"{name} has no burner block -- ADR-0047's arithmetic has no terms")
            continue
        if burner["fuel_categories"] != ["chemical"]:
            failures.append(
                f"{name} accepts {burner['fuel_categories']}, not ['chemical'] -- the "
                "furnace's own filter has moved"
            )
        if burner["effectivity"] != 1:
            failures.append(
                f"{name} has effectivity {burner['effectivity']}, so a fuel item no longer "
                "banks its whole fuel_value"
            )

    for name in ("uranium-fuel-cell",):
        fuel = by_name.get(name)
        for furnace in BURNER_FURNACES:
            accepted = (machines.get(furnace) or {}).get("burner", {}) or {}
            if fuel and fuel["fuel_category"] in (accepted.get("fuel_categories") or []):
                failures.append(f"{name} would burn in {furnace}")

    stone, steel = (machines.get(n) or {} for n in BURNER_FURNACES)
    coal = by_name.get("coal")
    if coal and stone.get("energy_usage") and steel.get("energy_usage"):
        if stone["energy_usage"] != steel["energy_usage"]:
            failures.append(
                f"the burner tiers draw {stone['energy_usage']} and {steel['energy_usage']} W "
                "-- the Steel tier's twice-the-items-per-coal no longer follows from the draw"
            )
        per_tick = stone["energy_usage"] / TICKS_PER_SECOND
        ticks = int(coal["fuel_value"] // per_tick)
        if per_tick != 4500 or ticks != 888:
            failures.append(
                f"coal buys {ticks} ticks at {per_tick} J/t, not 888 at 4500 -- ADR-0047's "
                "worked example no longer holds"
            )
        stone_crafts = ticks / (320 / stone.get("crafting_speed", 1))
        steel_crafts = ticks / (320 / steel.get("crafting_speed", 1))
        if abs(steel_crafts - 2 * stone_crafts) > 1e-9:
            failures.append(
                f"one coal yields {stone_crafts} steel-plate crafts on Stone and "
                f"{steel_crafts} on Steel -- not the doubling #155's javadoc claims"
            )

    for name in ("chemical", "nuclear"):
        if not categories.get(name):
            failures.append(f"no burner declares the {name!r} category")

    for index, failure in enumerate(failures, 1):
        print(f"FAIL {index}: {failure}")
    if failures:
        return 1
    chemical = [f for f in fuels if f["fuel_category"] == "chemical"]
    print(
        f"ok   {len(fuels)} fuels ({len(chemical)} chemical), {len(categories)} categories, "
        "coal is 888 ticks at 4500 J/t and the Steel tier doubles it"
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
