#!/usr/bin/env python3
"""Extract Factorio's fuel prototypes -- what an item is worth, and to which burners.

ADR-0047 settled that a burner holds a buffer in *joules*: lighting an item banks its
`fuel_value`, and a working tick spends the machine's own `energy_usage / 20`. There is no
burn-time number anywhere in Factorio and there is none in the pack; burn time is a
quotient. This is the numerator's extraction. The denominator is already in
`data/factorio/machine.json`, which this run also teaches to carry the burner's
`fuel_categories` and `effectivity` -- the other two terms ADR-0047's formula names.

Same dump as the other four extractors and the same provenance block in
`data/factorio/README.md`, so a dump still on disk feeds all five.

Three things this script decides, because they are properties of the data:

  - **Scope: every fuel-bearing item prototype, unfiltered.** Deliberately *not*
    `machine.json`'s "its own item recipe is in the corpus" rule, which would drop `coal`
    and `wood` -- Factorio's two most important fuels are mined and harvested, not crafted.
    Reachability in the pack is `data/pack/item-map.json`'s question and the join that reads
    this file records its own skips; a scope filter here would make an unreachable fuel
    indistinguishable from one nobody has mapped yet. `in_corpus` records, without deciding,
    whether the name appears in the Nauvis pre-launch recipe corpus at all.
  - **Fluids are out.** `thruster-fuel` and `thruster-oxidizer` carry a `fuel_value` and no
    `fuel_category`: they are burned by a spaceship thruster, which is neither a burner
    energy source nor in scope. A fluid is not something a furnace slot holds.
  - **The default category is Factorio's, not a null.** An item prototype with a
    `fuel_value` and no `fuel_category` is `chemical` to the engine, so it is `chemical`
    here -- the same default `machine.json`'s burner block applies to `fuel_categories`. A
    null would read as "nobody extracted this" and would fail the check on a prototype
    Factorio considers perfectly ordinary fuel. No committed row hits the default today.

Factorio's own spent-fuel mechanic (`burnt_result` -- `uranium-fuel-cell` leaves a depleted
one) is deliberately **not** extracted. It is a real mechanic with no row in the ledger, and
it is #135's: ADR-0047 drops the pack's *vanilla* remainder branch (`getCraftingRemainingItem`,
the lava-bucket rule, which reaches no fuel here), and that is a different mechanic with the
same silhouette. Extracting a column for a ticket this one lists as out of scope is how a
corpus grows fields nobody reads.

What this script does not do is decide anything. ADR-0047 holds the rule.

Usage:

    scripts/factorio-fuel-extract.py            # finds the dump, writes data/factorio/fuel.json
    scripts/factorio-fuel-extract.py --dump PATH
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

UNITS = {"k": 1e3, "M": 1e6, "G": 1e9, "T": 1e12}

# Factorio's own default: an item with a `fuel_value` and no `fuel_category` is chemical.
DEFAULT_CATEGORY = "chemical"

# Fluids carry a `fuel_value` and are burned by a thruster rather than by a burner energy
# source. Not a furnace's business, and not an item.
EXCLUDED_TYPES = ("fluid",)


def joules(value):
    """Factorio energy strings -- `4MJ`, `1.21GJ`, `250kJ` -- as a number in J."""
    if value is None:
        return None
    text = str(value).strip().rstrip("Jj")
    scale = UNITS.get(text[-1:])
    if scale is None:
        return float(text)
    return float(text[:-1]) * scale


def extract_fuels(dump, corpus_names):
    """Every item-like prototype with a `fuel_value`, in name order within its type."""
    fuels = []
    for kind, prototypes in sorted(dump.items()):
        if kind in EXCLUDED_TYPES or not isinstance(prototypes, dict):
            continue
        for name, prototype in sorted(prototypes.items()):
            if not isinstance(prototype, dict) or "fuel_value" not in prototype:
                continue
            fuels.append(
                {
                    "name": name,
                    "type": kind,
                    "fuel_value": joules(prototype["fuel_value"]),
                    "fuel_value_raw": prototype["fuel_value"],
                    "fuel_category": prototype.get("fuel_category", DEFAULT_CATEGORY),
                    "in_corpus": name in corpus_names,
                }
            )
    return fuels


def extract_categories(dump):
    """Every fuel category, and every entity whose burner accepts it.

    The authority ADR-0047's category filter is checked against, and the reason the filter
    is carried at all: a furnace accepts `chemical` and nothing else, so an item is not fuel
    for it merely by having a `fuel_value`. Walks the whole dump rather than a type list --
    a locomotive and a boiler are burners too, and #37 and #135 will both read this.
    """
    declared = {name: [] for name in sorted(dump.get("fuel-category") or {})}
    for kind, prototypes in sorted(dump.items()):
        if not isinstance(prototypes, dict):
            continue
        for name, prototype in sorted(prototypes.items()):
            if not isinstance(prototype, dict):
                continue
            source = prototype.get("energy_source")
            if not isinstance(source, dict) or source.get("type") != "burner":
                continue
            for category in source.get("fuel_categories") or []:
                declared.setdefault(category, []).append(f"{kind}/{name}")
    return declared


def corpus(path):
    """Every name the Nauvis pre-launch corpus mentions, as a result or an ingredient."""
    names = set()
    for recipe in json.loads(path.read_text(encoding="utf-8")):
        for side in ("ingredients", "results"):
            for entry in recipe.get(side) or []:
                if isinstance(entry, dict) and entry.get("name"):
                    names.add(entry["name"])
    return names


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--dump", type=Path, default=DEFAULT_DUMP)
    parser.add_argument("--out", type=Path, default=REPO / "data" / "factorio" / "fuel.json")
    parser.add_argument(
        "--recipes",
        type=Path,
        default=REPO / "data" / "factorio" / "recipe.json",
        help="read for the `in_corpus` flag only -- it is not a scope filter",
    )
    args = parser.parse_args()

    if not args.dump.is_file():
        sys.exit(
            f"no dump at {args.dump}\n"
            "run:  factorio --dump-data --mod-directory <dir with base+SA only>"
        )
    if not args.recipes.is_file():
        sys.exit(f"no recipe corpus at {args.recipes} -- run factorio-recipe-extract.py first")

    dump = json.loads(args.dump.read_text(encoding="utf-8"))
    fuels = extract_fuels(dump, corpus(args.recipes))
    categories = extract_categories(dump)

    out = {"fuels": fuels, "categories": categories}
    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(json.dumps(out, indent=2) + "\n", encoding="utf-8")

    print(f"{len(fuels)} fuels, {len(categories)} categories")
    print(f"wrote      {args.out.relative_to(REPO)}\n")
    print(f"{'fuel':22} {'type':10} {'category':10} {'MJ':>10}  corpus")
    for fuel in fuels:
        print(
            f"{fuel['name']:22} {fuel['type']:10} {fuel['fuel_category']:10} "
            f"{fuel['fuel_value'] / 1e6:10.3f}  {'yes' if fuel['in_corpus'] else ' - ':^6}"
        )
    print("\ncategories and the burners that accept them:")
    for name, who in categories.items():
        print(f"  {name:10} {len(who):3}  {', '.join(who)[:70]}")


if __name__ == "__main__":
    main()
