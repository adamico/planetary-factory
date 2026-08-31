#!/usr/bin/env python3
"""Assert the extracted machine corpus is internally consistent and still routable.

`scripts/factorio-machine-extract.py` reads a 28MB Factorio dump that is not in the repo,
so nothing here re-runs it. What is checkable without the dump -- and without launching the
game -- is whether the *committed* output still says what the decisions say it says:

  - every route in `data/pack/category-map.json` names a category some extracted entity
    actually declares, because a route naming a category nothing crafts is a typo that
    would silently send recipes nowhere. `!`-prefixed routes are checked too: a deliberate
    non-route still has to name a real category.
  - every routed category a pack machine owns is declared by at least one *in-scope*
    machine, because that machine is where the pack machine's speed and power come from.
    `hand-crafting` is the one exception, and it is one by nature: Factorio hands it to the
    character rather than to any machine, so the pack's `personal_assembler` has no
    prototype to read a speed off and never will.
  - every machine carries the four numbers `#126`'s rule reads -- crafting speed, energy
    usage, module slots, categories -- because a null is a number nobody extracted
  - drain is derived, not copied: an electric machine's drain is exactly `energy_usage/30`
    unless the prototype set one, and a burner machine has none. `#126` excludes drain from
    the conversion; the ledger quotes this figure, so it must not drift silently.
  - the fluid anchors are present, because `#126`'s 1 unit = 1 mB is derived from them

Usage: tests/factorio/test_machine_extract.py
"""
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent.parent

DEFAULT_DRAIN_FRACTION = 30

# Declared by `character` and `god-controller`, never by a machine. A route to it is
# correct and still has no crafting-speed source; see the module docstring.
CHARACTER_CATEGORIES = {"hand-crafting"}


def main():
    data = json.loads((ROOT / "data/factorio/machine.json").read_text())
    routes = json.loads((ROOT / "data/pack/category-map.json").read_text())["routes"]
    machines = data["machines"]
    categories = data["categories"]
    by_name = {m["name"]: m for m in machines}
    failures = []

    if not machines:
        failures.append("machine.json lists no machines")

    for category, machine in sorted(routes.items()):
        if category not in categories:
            failures.append(
                f"route {category!r} -> {machine} names a category no entity declares"
            )
        elif not machine.startswith("!") and not categories[category]:
            failures.append(
                f"route {category!r} -> {machine} names a category with no crafting entity"
            )

    in_scope = {c for m in machines for c in (m["crafting_categories"] or [])}
    for category, machine in sorted(routes.items()):
        if machine.startswith("!") or category not in categories:
            continue
        if category in CHARACTER_CATEGORIES:
            continue
        if category not in in_scope:
            failures.append(
                f"{machine} crafts {category!r}, which no in-scope machine declares "
                "-- its speed and power have no source"
            )

    for machine in machines:
        for field in ("crafting_speed", "energy_usage", "crafting_categories"):
            if machine[field] in (None, [], 0):
                failures.append(f"{machine['name']} has no {field}")
        if machine["module_slots"] is None:
            failures.append(f"{machine['name']} has no module_slots")

    for machine in machines:
        drain, source = machine["drain"], machine["drain_source"]
        if machine["energy_type"] != "electric":
            if drain is not None or source != "none":
                failures.append(
                    f"{machine['name']} is a {machine['energy_type']} machine with a drain"
                )
        elif source == "default":
            want = machine["energy_usage"] / DEFAULT_DRAIN_FRACTION
            if drain is None or abs(drain - want) > 1e-6:
                failures.append(
                    f"{machine['name']} drains {drain}, but the engine default on "
                    f"{machine['energy_usage']}W is {want}"
                )
        elif source != "explicit":
            failures.append(f"{machine['name']} has drain_source {source!r}")

    anchors = {c["name"]: c for c in data["containers"]}
    for name in ("storage-tank", "pipe"):
        anchor = anchors.get(name)
        if not anchor or not anchor.get("volume") or not anchor.get("tile_width"):
            failures.append(f"no fluid anchor for {name} -- #126's 1 unit = 1 mB is underived")

    for index, failure in enumerate(failures, 1):
        print(f"FAIL {index}: {failure}")
    if failures:
        return 1
    print(
        f"ok   {len(machines)} machines, {len(data['containers'])} fluid anchors, "
        f"every route names a declared category"
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
