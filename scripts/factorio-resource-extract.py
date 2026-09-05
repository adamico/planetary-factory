#!/usr/bin/env python3
"""Extract Factorio's resource amounts -- how much ore a patch holds.

ADR-0041 makes an ore block carry an amount, and ADR-0022's extract-never-transcribe rule
says none of those numbers may be chosen. They do not have to be: Factorio's resource
amounts are *closed-form in the prototype dump*, not buried in map generation.

Three things are read, and nothing is decided:

  - **The starting patch total.** `resource_autoplace_all_patches` defines it outright as
    `20000 * base_density * (frequency_multiplier + 1) * size_multiplier`. The formula is
    read out of the function's own local expressions rather than typed here, and each
    resource's `base_density` out of the arguments its `default-<name>-patches` noise
    expression passes. At default controls -- frequency and size both 1, which is what a
    default map deals -- that is 400,000 for iron and 320,000 for copper and coal.
  - **The distance law.** Every resource's `richness_expression` carries the same term,
    `max((1000 + distance) / 2600, 1)`: flat inside spawn's neighbourhood and rising
    linearly beyond. The break-even distance is solved from the term, not typed, and it is
    the arithmetic saying Factorio does not reward leaving early. The outfield law --
    `regular_density_at`, with its three radii -- is closed-form in the same function and
    comes across whole, so a later body siting outfield veins reads it here.
  - **The hand-mining numbers.** Each resource's `minable.mining_time`, the character's own
    `mining_speed`, and what `steel-axe` adds to it. ADR-0039 labelled these as *transcribed
    from the wiki, not extracted*, because `data/factorio/` held no resource dump and so
    they could not be checked against the repo the way the technology tree can. It holds
    one now, and `tests/factorio/test_resource_extract.py` asserts `PickTier` against them.
  - **The stage thresholds.** `stage_counts` is what Factorio renders its eight sprite
    stages against. They are extracted as *ratios of each resource's own first rung*,
    because that is the only form usable against blocks holding a thousand units rather
    than fifteen thousand. Iron, copper, coal and stone share one list; uranium's is that
    list scaled by about 2/3 and then rounded to two or three figures, so the fraction sets
    agree to rounding and not exactly. The rounding is preserved rather than smoothed --
    `stage_ratios` is each resource's own -- and `tests/factorio/test_resource_extract.py`
    is where the agreement is asserted with the tolerance stated.

A Factorio tile and a Minecraft block are both one metre, so every distance here is a
block count already. Nothing in this file converts anything.

Scope is the resources placed by `resource_autoplace_all_patches` -- Nauvis's six. A
resource placed by some other expression (Vulcanus's calcite, Fulgora's scrap) has no
starting amount to read and is skipped by name in the output, the way the other extractors
report what they left out.

Two files are written from one read. `data/factorio/resource.json` is the corpus, and
`mod/src/main/resources/planetaryfactory_core/ore/amounts.json` is the slice the mod loads at
class-init: the five resources ADR-0041 puts on Terra, their patch totals, their stage ratios and
the distance law. It is a *classpath* resource rather than a datapack file because the stage count
sizes a blockstate property, which is fixed before any world exists -- and it is generated here
rather than typed in Java for the same reason nothing else in the pack is typed twice.

Usage:

    scripts/factorio-resource-extract.py            # finds the dump, writes both files
    scripts/factorio-resource-extract.py --dump PATH
"""

import argparse
import json
import re
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent

DEFAULT_DUMP = (
    Path.home()
    / "Library/Application Support/factorio/script-output/data-raw-dump.json"
)

# The autoplace function whose arguments carry the amounts. A resource placed by anything
# else is out of scope; see the module docstring.
PATCH_FUNCTION = "resource_autoplace_all_patches"

# The map-generation controls, at the settings a default map deals. Factorio's own
# defaults for `frequency`, `size` and `richness` are 1; the dump carries the controls'
# existence but not their default values, so this is the one number here that is stated
# rather than read -- and it is a map setting, not a property of any resource.
DEFAULT_CONTROL = 1.0

# The local expressions worth carrying whole: the outfield law's three radii, in tiles,
# which are blocks one-for-one.
CARRIED_CONSTANTS = (
    "starting_resource_placement_radius",
    "regular_patch_fade_in_distance",
    "double_density_distance",
    "starting_patches_split",
)

# Terra's alphabet (ADR-0041), and the Factorio resource each pack ore block reads its amounts
# from. The keys are the pack's block names and the values are Factorio's, which is ADR-0028's
# declared exception: the corpus is keyed by Factorio's own names.
TERRA_ALPHABET = {
    "iron": "iron-ore",
    "copper": "copper-ore",
    "coal": "coal",
    "uranium": "uranium-ore",
    "stone": "stone",
}

ARGUMENT = re.compile(r"(\w+)\s*=\s*([^,{}]+?)\s*(?=,\s*\w+\s*=|\}$)")
DISTANCE_TERM = re.compile(r"max\(\s*\(\s*(\d+)\s*\+\s*distance\s*\)\s*/\s*(\d+)\s*,\s*1\s*\)")


def patch_arguments(expression):
    """The arguments a `default-<name>-patches` expression passes to the patch function.

    Returns `None` when the expression calls something else, which is how a resource
    placed by another planet's rules falls out of scope.
    """
    if PATCH_FUNCTION not in expression:
        return None
    body = expression[expression.index("{"):]
    return {name: value for name, value in ARGUMENT.findall(body)}


def number(value):
    """An argument that is a literal number, or `None` when it is a control variable."""
    try:
        return float(value)
    except ValueError:
        return None


def starting_amount(formula, base_density):
    """The starting patch total, evaluated from the function's own formula.

    The formula is read out of the dump and evaluated here against the default controls,
    so a Factorio release that changes the constant changes this output rather than
    disagreeing with it silently.
    """
    return eval(  # noqa: S307 -- the formula is the dump's, and the names are bound below
        formula,
        {"__builtins__": {}},
        {
            "base_density": base_density,
            "frequency_multiplier": DEFAULT_CONTROL,
            "size_multiplier": DEFAULT_CONTROL,
        },
    )


def distance_law(richness_expression):
    """The `max((1000 + distance) / 2600, 1)` term, and where it stops being flat.

    The break-even distance is solved from the term the prototype carries -- the point at
    which the ratio reaches 1 -- rather than typed, because it is the number ADR-0041
    quotes for why leaving the starting area early buys nothing.
    """
    found = DISTANCE_TERM.search(richness_expression or "")
    if not found:
        return None
    offset, divisor = int(found.group(1)), int(found.group(2))
    return {
        "term": found.group(0),
        "offset": offset,
        "divisor": divisor,
        "flat_within": divisor - offset,
    }


def hand_mining(dump):
    """The character's mining speed, and the speed `steel-axe` leaves them mining at.

    ADR-0039's two tiers are these two numbers: the bare character, and the character after
    the research. Both are read rather than transcribed, which is the weakness that ADR
    labelled and named an extractor as the fix for.

    **`character-mining-speed` is a fraction, not an addend.** Factorio applies the modifier
    as `base * (1 + modifier)`, so `steel-axe`'s `1` is +100% and takes the character from
    0.5 to 1.0 rather than to 1.5. ADR-0039's prose says the research "adds 1 to it" and its
    `PickTier.STEEL` ships 1.0 -- the number is right and the sentence describes the wrong
    operation, which is exactly the kind of drift a transcription hides and an extraction
    does not.
    """
    character = (dump.get("character") or {}).get("character") or {}
    effects = ((dump.get("technology") or {}).get("steel-axe") or {}).get("effects") or []
    bonus = sum(
        effect.get("modifier", 0)
        for effect in effects
        if effect.get("type") == "character-mining-speed"
    )
    return {
        "character_mining_speed": character.get("mining_speed"),
        "steel_axe_modifier": bonus,
        "character_mining_speed_researched": (character.get("mining_speed") or 0) * (1 + bonus),
    }


def extract(dump):
    function = (dump.get("noise-function") or {}).get(PATCH_FUNCTION)
    if not function:
        sys.exit(f"dump carries no noise function {PATCH_FUNCTION!r}")
    locals_ = function.get("local_expressions") or {}
    functions = function.get("local_functions") or {}

    formula = locals_.get("starting_amount")
    if not formula:
        sys.exit(f"{PATCH_FUNCTION} carries no `starting_amount` expression")

    expressions = dump.get("noise-expression") or {}
    resources, skipped = [], []
    laws = {}

    for name, prototype in sorted((dump.get("resource") or {}).items()):
        autoplace = prototype.get("autoplace") or {}
        patches = expressions.get(f"default-{name}-patches") or {}
        arguments = patch_arguments(patches.get("expression", ""))
        if arguments is None:
            skipped.append(name)
            continue

        density = number(arguments.get("base_density"))
        starts = number(arguments.get("has_starting_area_placement")) == 1
        stages = prototype.get("stage_counts") or []
        law = distance_law(autoplace.get("richness_expression"))
        if law:
            laws[law["term"]] = laws.get(law["term"], 0) + 1

        resources.append(
            {
                "name": name,
                "category": prototype.get("category", "basic-solid"),
                "infinite": bool(prototype.get("infinite")),
                "minimum": prototype.get("minimum"),
                "base_density": density,
                "base_spots_per_km2": number(arguments.get("base_spots_per_km2")),
                "has_starting_area_placement": starts,
                # A resource with no starting patch has no starting total to state, and
                # `null` says so rather than a zero that reads as an empty patch.
                "starting_amount": starting_amount(formula, density) if starts else None,
                "mining_time": (prototype.get("minable") or {}).get("mining_time"),
                "required_fluid": (prototype.get("minable") or {}).get("required_fluid"),
                "stage_counts": stages,
                "stage_ratios": [count / stages[0] for count in stages] if stages and stages[0] else [],
                "distance_law": law,
            }
        )

    return {
        "starting_amount_formula": formula,
        "controls": {
            "frequency_multiplier": DEFAULT_CONTROL,
            "size_multiplier": DEFAULT_CONTROL,
            "richness": DEFAULT_CONTROL,
        },
        "constants": {
            key: number(locals_[key]) for key in CARRIED_CONSTANTS if key in locals_
        },
        "outfield_law": {
            name: {
                "parameters": body.get("parameters"),
                "expression": body.get("expression"),
            }
            for name, body in sorted(functions.items())
        },
        "hand_mining": hand_mining(dump),
        "resources": resources,
        "skipped": skipped,
    }, laws


def mod_slice(out):
    """The part of the corpus the mod loads, keyed by the pack's own block names.

    Deliberately thin: a total, a ratio set and the distance law. Everything else in the corpus
    is read by scripts, and a number that reaches Java is a number that has to survive a
    recompile to be corrected.
    """
    by_name = {entry["name"]: entry for entry in out["resources"]}
    law = next(
        (entry["distance_law"] for entry in out["resources"] if entry["distance_law"]), None
    )
    resources = {}
    for block, factorio in sorted(TERRA_ALPHABET.items()):
        entry = by_name.get(factorio)
        if entry is None:
            sys.exit(f"{factorio} is not in the dump -- Terra's alphabet has a hole")
        resources[block] = {
            "factorio_name": factorio,
            "starting_amount": entry["starting_amount"],
            "stage_ratios": entry["stage_ratios"],
        }
    return {
        "__generated_by": "scripts/factorio-resource-extract.py",
        "distance_law": law,
        "resources": resources,
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--dump", type=Path, default=DEFAULT_DUMP)
    parser.add_argument("--out", type=Path, default=REPO / "data" / "factorio" / "resource.json")
    parser.add_argument(
        "--mod-out",
        type=Path,
        default=REPO / "mod/src/main/resources/planetaryfactory_core/ore/amounts.json",
        help="the slice the mod loads at class-init",
    )
    args = parser.parse_args()

    if not args.dump.is_file():
        sys.exit(
            f"no dump at {args.dump}\n"
            "run:  factorio --dump-data --mod-directory <dir with base+SA only>"
        )

    dump = json.loads(args.dump.read_text(encoding="utf-8"))
    out, laws = extract(dump)

    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(json.dumps(out, indent=2) + "\n", encoding="utf-8")

    slice_ = mod_slice(out)
    args.mod_out.parent.mkdir(parents=True, exist_ok=True)
    args.mod_out.write_text(json.dumps(slice_, indent=2) + "\n", encoding="utf-8")

    print(f"starting amount = {out['starting_amount_formula']}")
    print(
        f"hand mining     character {out['hand_mining']['character_mining_speed']}, "
        f"steel-axe +{out['hand_mining']['steel_axe_modifier']:.0%} "
        f"-> {out['hand_mining']['character_mining_speed_researched']}"
    )
    print(f"wrote      {args.out.relative_to(REPO)}")
    print(f"wrote      {args.mod_out.relative_to(REPO)} ({len(slice_['resources'])} pack ores)")
    print(f"out of scope {len(out['skipped'])}: " + ", ".join(out["skipped"]))
    print()
    print(f"{'resource':14} {'density':>7} {'starting total':>15} {'stages':>7}  distance law")
    for resource in out["resources"]:
        total = resource["starting_amount"]
        print(
            f"{resource['name']:14} {resource['base_density'] or 0:7} "
            f"{'none' if total is None else f'{total:,.0f}':>15} "
            f"{len(resource['stage_counts']):7}  "
            f"{(resource['distance_law'] or {}).get('term', '-')}"
        )
    print()
    for term, count in sorted(laws.items()):
        law = next(r["distance_law"] for r in out["resources"] if (r["distance_law"] or {}).get("term") == term)
        print(f"{count} resources carry {term} -- flat within {law['flat_within']} tiles")


if __name__ == "__main__":
    main()
