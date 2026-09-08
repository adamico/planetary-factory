#!/usr/bin/env python3
"""Join the extracted fuel corpus onto the item map, as the fuel table the mod loads (ADR-0047, #187).

Reads two committed inputs and writes one datapack JSON file per burnable item:

  data/factorio/fuel.json    the corpus -- `name`, `fuel_value`, `fuel_category` (#185)
  data/pack/item-map.json    Factorio name -> pack item, tag or fluid (ADR-0026)

NOTHING IS DECIDED HERE. A fuel burns because Factorio says it has a `fuel_value` and because
`item-map.json` says what it is on Terra; both halves are design documents reviewed as diffs.
This script is the join and the join only.

WHAT STOPS A FUEL GETTING A ROW, and each one is a RECORDED SKIP rather than a silent absence:

  1. no item-map row at all       -- nothing on Terra is that item yet. `nuclear-fuel` is the
                                     standing example (ADR-0047): an ordinary chemical fuel with
                                     no row, waiting on #135, and a skip is what says so.
  2. an `undecided` item-map row  -- blocked on the decision that row names
  3. a `blocked_by` item-map row  -- decided, but the item is not registered yet: a row naming
                                     an unregistered item is read at world load, not here
  4. the row maps onto a fluid    -- a thruster's fuel is not a furnace slot's

Note the departure from `factorio-recipe-convert.py`, where a missing item-map row is a HARD
FAILURE. There the corpus is the pack's recipe list and a name nobody has looked at is a hole in
it; here the corpus is every fuel in Factorio including Gleba's and Aquilo's, most of which no
pack item is, and a missing row is the ordinary case rather than an oversight.

THE CATEGORY IS CARRIED, NOT FILTERED HERE. Every row keeps its `fuel_category` and the mod
applies the filter, because the filter has to hold for an item that arrives later:
`uranium-fuel-cell` is `nuclear`, has a `fuel_value`, and would become furnace fuel the day #135
gives it an item-map row if the only gate were a row's existence.

Generated output is never hand-edited; re-run this script.

Usage: scripts/factorio-fuel-convert.py [--check] [--quiet]
  --check  write nothing; exit non-zero if the emitted files on disk differ from what would be
           written.
"""
import argparse
import json
import shutil
import sys
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT_DIR = ROOT / "kubejs/data/planetaryfactory/fuel"


def load(relative):
    return json.loads((ROOT / relative).read_text(encoding="utf-8"))


def row_for(fuel, mapping):
    """The emitted row, or a (reason, detail) skip. Never both, never neither."""
    kind = mapping["kind"]
    if kind == "fluid":
        return None, ("fluid item-map row", mapping["target"])
    body = {"factorio_name": fuel["name"]}
    body["tag" if kind == "tag" else "item"] = mapping["target"]
    # Factorio's `fuel_value` is a float in the dump and a whole number of joules in fact; the
    # mod reads a long, so the narrowing happens here where it can be seen rather than in a codec.
    body["fuel_value"] = int(fuel["fuel_value"])
    body["fuel_category"] = fuel["fuel_category"]
    return body, None


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true",
                        help="write nothing; fail if the emitted files differ")
    parser.add_argument("--quiet", action="store_true")
    args = parser.parse_args()

    fuels = load("data/factorio/fuel.json")["fuels"]
    items = load("data/pack/item-map.json")["items"]

    emitted, skipped = {}, []
    for fuel in fuels:
        name = fuel["name"]
        mapping = items.get(name)
        if mapping is None:
            skipped.append((name, "no item-map row", fuel["fuel_category"]))
            continue
        if mapping.get("status") == "undecided":
            ticket = mapping.get("ticket")
            skipped.append((name, "undecided item-map row",
                            f"#{ticket}" if ticket else "no ticket"))
            continue
        if "blocked_by" in mapping:
            skipped.append((name, "item not registered yet", f"#{mapping['blocked_by']}"))
            continue
        body, skip = row_for(fuel, mapping)
        if skip is not None:
            skipped.append((name, *skip))
            continue
        emitted[name.replace("-", "_")] = body

    if args.check:
        written = {p.stem: json.loads(p.read_text(encoding="utf-8"))
                   for p in OUT_DIR.rglob("*.json")} if OUT_DIR.exists() else {}
        if written != emitted:
            added = sorted(set(emitted) - set(written))
            removed = sorted(set(written) - set(emitted))
            changed = sorted(k for k in set(emitted) & set(written) if emitted[k] != written[k])
            print("FAIL the fuel table on disk is stale -- re-run "
                  "scripts/factorio-fuel-convert.py")
            for label, names in (("missing", added), ("unexpected", removed), ("changed", changed)):
                if names:
                    print(f"  {label}: " + ", ".join(names))
            return 1
    else:
        if OUT_DIR.exists():
            shutil.rmtree(OUT_DIR)
        OUT_DIR.mkdir(parents=True, exist_ok=True)
        for stem, body in emitted.items():
            (OUT_DIR / f"{stem}.json").write_text(json.dumps(body, indent=2) + "\n")

    if not args.quiet:
        reasons = Counter(reason for _, reason, _ in skipped)
        print(f"ok   {len(emitted)} fuels emitted, {len(skipped)} not")
        for reason, count in reasons.most_common():
            print(f"     {count:3d} {reason}")
        for name, reason, detail in sorted(skipped):
            print(f"     skip {name} -- {reason} ({detail})")
    return 0


if __name__ == "__main__":
    sys.exit(main())
