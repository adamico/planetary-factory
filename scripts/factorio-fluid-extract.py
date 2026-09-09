#!/usr/bin/env python3
"""Extract Factorio's fluid prototypes -- the two thermal constants ADR-0050's ratio needs.

`#210` closes a gap the machine extractor left: `machine.json`'s boiler carries
`target_temperature` and `energy_consumption`, but the two terms that turn those into a
water draw -- `steam.heat_capacity` and `water.default_temperature` -- live on the *fluid*
prototypes, which nothing extracts. Without them the boiler's 60 mB/s and the pump's 20:1
ratio are typed rather than read, which is exactly what ADR-0022 forbids.

Same dump as the other extractors, same provenance block in `data/factorio/README.md`.

**Scope is narrow, on purpose.** This is not a general fluid corpus: it extracts only the
fluids `machine.json`'s boiler declares in its own fluid boxes (`water` in, `steam` out),
read off that file rather than hardcoded here, so the scope tracks the boiler rather than
drifting from it. Widening to all 33 fluids in the dump would carry 31 rows nothing reads
and would make it look like fluid crafting is in scope, which #210 does not touch.

Two fields per fluid, nothing else: `heat_capacity` and `default_temperature`. Both are
read raw off the prototype -- `heat_capacity` is Factorio's SI string (`0.2kJ`), kept
alongside its parsed joule value the same way `fuel.json` keeps `fuel_value_raw`, so the
check can show its work rather than trust a parse.

What this script does not do is decide anything, and it does not perform the boiler's own
arithmetic -- that derivation, and its two traps (the pump's per-tick rate, and water's
heat capacity being the wrong constant), belong to `tests/factorio/test_resource_extract.py`,
which re-derives the ratio rather than trusting a number this script could have written down.

Usage:

    scripts/factorio-fluid-extract.py            # finds the dump, writes data/factorio/fluid.json
    scripts/factorio-fluid-extract.py --dump PATH
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

UNITS = {"": 1, "k": 1e3, "M": 1e6, "G": 1e9, "T": 1e12}


def joules(value):
    """Factorio's SI strings -- `0.2kJ`, `2kJ` -- as a bare number of joules."""
    if value is None:
        return None
    text = str(value).strip().rstrip("Jj")
    scale = UNITS.get(text[-1:], None)
    if scale is None:
        return float(text)
    return float(text[:-1] if text[-1:] in UNITS and text[-1:] else text) * scale


def scope_from_machine(machine_path):
    """The fluid names the committed boiler already declares -- read, not typed.

    Reading `machine.json` rather than naming `water`/`steam` here keeps this script's
    scope a function of the boiler's own fluid boxes: if a future boiler variant filtered
    on a different fluid, widening it there widens this extraction with it, the same
    relationship the machine extractor has with the recipe corpus.
    """
    data = json.loads(machine_path.read_text(encoding="utf-8"))
    names = set()
    for boiler in data.get("boilers") or []:
        for box in boiler.get("fluid_boxes") or []:
            if box.get("filter"):
                names.add(box["filter"])
    return names


def extract_fluids(dump, scope):
    fluids = []
    for name, prototype in sorted((dump.get("fluid") or {}).items()):
        if name not in scope:
            continue
        fluids.append(
            {
                "name": name,
                "heat_capacity": joules(prototype.get("heat_capacity")),
                "heat_capacity_raw": prototype.get("heat_capacity"),
                "default_temperature": prototype.get("default_temperature"),
            }
        )
    return fluids


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--dump", type=Path, default=DEFAULT_DUMP)
    parser.add_argument("--out", type=Path, default=REPO / "data" / "factorio" / "fluid.json")
    parser.add_argument(
        "--machine",
        type=Path,
        default=REPO / "data" / "factorio" / "machine.json",
        help="the scope: fluids named by the boiler's own fluid box filters",
    )
    args = parser.parse_args()

    if not args.dump.is_file():
        sys.exit(
            f"no dump at {args.dump}\n"
            "run:  factorio --dump-data --mod-directory <dir with base+SA only>"
        )
    if not args.machine.is_file():
        sys.exit(f"no machine corpus at {args.machine} -- run factorio-machine-extract.py first")

    dump = json.loads(args.dump.read_text(encoding="utf-8"))
    scope = scope_from_machine(args.machine)
    if not scope:
        sys.exit("the boiler in machine.json declares no fluid filters -- nothing to scope on")

    fluids = extract_fluids(dump, scope)
    missing = scope - {f["name"] for f in fluids}
    if missing:
        sys.exit(f"the dump has no fluid prototype for {sorted(missing)}")

    out = {"fluids": fluids}
    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(json.dumps(out, indent=2) + "\n", encoding="utf-8")

    print(f"{len(fluids)} fluids, scoped from the boiler's own fluid boxes: {sorted(scope)}")
    print(f"wrote      {args.out.relative_to(REPO)}\n")
    for fluid in fluids:
        print(
            f"  {fluid['name']:10} {fluid['heat_capacity_raw']:>8} = {fluid['heat_capacity']:6.1f} J  "
            f"default {fluid['default_temperature']}C"
        )


if __name__ == "__main__":
    main()
