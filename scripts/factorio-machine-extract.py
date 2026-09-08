#!/usr/bin/env python3
"""Extract Factorio's crafting-machine, drill, power and fluid-container prototypes.

`#126` settled the *shape* of the recipe conversion rule -- four channels, nothing scaled,
`crafting_speed` living on the machine rather than in the recipe -- but every number behind
it was an agent's recollection. This is the extraction that replaces the recollection,
under ADR-0022's extract-never-transcribe rule.

Same dump as `scripts/factorio-tech-extract.py` and `scripts/factorio-recipe-extract.py`,
and the same provenance block in `data/factorio/README.md`, so a dump still on disk feeds
all three.

Three things this script decides, because they are properties of the data rather than of
the pack:

  - **Scope.** Nauvis pre-launch, expressed the way the recipe corpus already expresses it:
    a machine is in scope when *its own item recipe* is in `data/factorio/recipe.json`. The
    scope rule therefore lives in exactly one place -- widen `RUNG_PACKS` in the recipe
    extractor and this file widens with it. Vulcanus's foundry and Gleba's biochamber fall
    out on their own; no machine list is typed here.
  - **Drain.** Factorio's crafting machines set no `drain` at all -- ten prototypes in the
    whole dump do, none of them a crafting machine -- so the figure is the engine's default
    of `energy_usage / 30` on an electric energy source, and nothing at all on a burner one.
    `#126` excludes drain from the conversion deliberately; the number is extracted so the
    ledger row saying so can quote it rather than assert it. It is derived, and says so:
    `drain_source` is `default` or `explicit`.
  - **The burner's own terms.** ADR-0047 spends a fuel item's `fuel_value` against the
    machine's `energy_usage` and `effectivity`, and admits it only if the burner's
    `fuel_categories` accept it. Two of those three were already here; `burner` carries the
    other two, and is `null` on an electric machine. Both burner furnaces come out at
    `effectivity: 1` and `["chemical"]`, which is why the Steel tier gets twice the items
    from one coal at the same 90 kW -- extracted, so the ladder can stop asserting it.
  - **Who declares a category.** `data/pack/category-map.json` routes a recipe category to a
    pack machine, and its right-hand side was hand-written. `categories` here is the
    authority it is checked against: every recipe category, and every entity declaring it.
    The character is included -- `hand-crafting` is a real category with no machine.

The fluid anchors are the other half. `#126`'s 1 unit = 1 mB claim is a derivation from
Factorio's own volume-to-footprint figures (a storage tank, a pipe) against Minecraft's
bucket of 1 m^3 = 1000 mB, and the derivation needs the figures.

What this script does *not* do is decide anything. `#126` holds the rule.

Usage:

    scripts/factorio-machine-extract.py            # finds the dump, writes data/factorio/machine.json
    scripts/factorio-machine-extract.py --dump PATH
"""

import argparse
import json
import math
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent

DEFAULT_DUMP = (
    Path.home()
    / "Library/Application Support/factorio/script-output/data-raw-dump.json"
)

# The prototype types that craft. `lab` crafts research rather than items and carries
# `researching_speed` instead of `crafting_speed`; it is here because ADR-0018's rungs are
# research and the Research Lab needs the same numbers.
CRAFTING_TYPES = ("assembling-machine", "furnace", "rocket-silo", "lab")

# Fluid containers, for the 1 unit = 1 mB derivation. Not crafting machines; extracted for
# their volume and footprint only.
CONTAINER_TYPES = ("storage-tank", "pipe")

# The three prototype types #188 adds. None of them crafts: a mining drill spends
# `mining_speed` against a resource's own hardness, a boiler spends joules on raising water
# to a temperature, and a generator spends hot fluid on power. They carry no
# `crafting_speed` and no `crafting_categories`, which is why they are extracted beside the
# crafting machines rather than into them -- a null `crafting_speed` in `machines` would be
# indistinguishable from a number nobody extracted, and `test_machine_extract.py` fails on
# exactly that.
DRILL_TYPE = "mining-drill"
BOILER_TYPE = "boiler"
GENERATOR_TYPE = "generator"

# Factorio's default electric drain, from the engine rather than from any prototype: an
# electric energy source with no `drain` set draws 1/30 of its `energy_usage` while idle.
DEFAULT_DRAIN_FRACTION = 30

# Factorio's tick, for the generator's derived power figure.
TICKS_PER_SECOND = 60

UNITS = {"": 1, "k": 1e3, "M": 1e6, "G": 1e9, "T": 1e12}


def si(value):
    """Factorio's SI strings -- `375kW`, `2.5MJ`, `0.2kJ` -- as a bare number.

    The unit letter is dropped and only the prefix is read, so this is as correct on a
    heat capacity in J/degree as it is on a power in W. `watts` is the name the crafting
    machines already read it under.
    """
    if value is None:
        return None
    text = str(value).strip().rstrip("Ww").rstrip("Jj")
    scale = UNITS.get(text[-1:], None)
    if scale is None:
        return float(text)
    return float(text[:-1] if text[-1:] in UNITS and text[-1:] else text) * scale


watts = si


def footprint(prototype):
    """Tiles occupied, from the selection box -- what `tile_width`/`tile_height` default to."""
    for key in ("tile_width", "tile_height"):
        if key in prototype:
            return prototype.get("tile_width"), prototype.get("tile_height")
    box = prototype.get("selection_box") or prototype.get("collision_box")
    if not box:
        return None, None
    (left, top), (right, bottom) = box
    return math.ceil(right - left), math.ceil(bottom - top)


def fluid_box(box):
    """The parts of a fluid box a consumer reads: how much it holds and what it admits.

    The temperature bounds are the steam engine's whole contract -- it consumes steam at or
    above `minimum_temperature` and pays out up to `maximum_temperature` -- and they live on
    the box rather than on the entity.
    """
    return {
        "production_type": box.get("production_type"),
        "volume": box.get("volume"),
        "filter": box.get("filter"),
        "minimum_temperature": box.get("minimum_temperature"),
        "maximum_temperature": box.get("maximum_temperature"),
    }


def fluid_boxes(prototype, *keys):
    """Named single boxes and the `fluid_boxes` list, flattened in declaration order."""
    boxes = []
    for key in keys:
        box = prototype.get(key)
        if isinstance(box, dict):
            boxes.append(dict(fluid_box(box), name=key))
    for box in prototype.get("fluid_boxes") or []:
        if isinstance(box, dict):
            boxes.append(dict(fluid_box(box), name=None))
    return boxes


def burner(prototype):
    """The two terms ADR-0047's fuel arithmetic needs, or None on a machine that has none.

    `effectivity` scales the joules a fuel item banks; `fuel_categories` is what the machine
    will accept at all, and defaults to `chemical` the way the engine defaults it.
    """
    source = prototype.get("energy_source") or {}
    if source.get("type") != "burner":
        return None
    return {
        "fuel_categories": source.get("fuel_categories") or ["chemical"],
        "effectivity": source.get("effectivity", 1),
        "fuel_inventory_size": source.get("fuel_inventory_size", 1),
    }


def energy(prototype):
    """Working draw, idle drain, and where the drain figure came from."""
    source = prototype.get("energy_source") or {}
    usage = watts(prototype.get("energy_usage"))
    if source.get("type") != "electric":
        return usage, None, "none", source.get("type")
    if "drain" in source:
        return usage, watts(source["drain"]), "explicit", "electric"
    if usage is None:
        return usage, None, "none", "electric"
    return usage, usage / DEFAULT_DRAIN_FRACTION, "default", "electric"


def in_scope(recipes):
    """The names of every recipe in the Nauvis pre-launch corpus."""
    return {recipe["name"] for recipe in recipes}


def extract_machines(dump, scope):
    machines, skipped = [], []
    for kind in CRAFTING_TYPES:
        for name, prototype in sorted((dump.get(kind) or {}).items()):
            if name not in scope:
                skipped.append(name)
                continue
            usage, drain, drain_source, energy_type = energy(prototype)
            width, height = footprint(prototype)
            machines.append(
                {
                    "name": name,
                    "type": kind,
                    "crafting_speed": prototype.get(
                        "crafting_speed", prototype.get("researching_speed")
                    ),
                    "energy_usage": usage,
                    "energy_type": energy_type,
                    "drain": drain,
                    "drain_source": drain_source,
                    "burner": burner(prototype),
                    "module_slots": prototype.get("module_slots", 0),
                    "crafting_categories": prototype.get("crafting_categories")
                    or prototype.get("inputs"),
                    "fluid_boxes": [
                        {
                            "production_type": box.get("production_type"),
                            "volume": box.get("volume"),
                        }
                        for box in (prototype.get("fluid_boxes") or [])
                        if isinstance(box, dict)
                    ],
                    "tile_width": width,
                    "tile_height": height,
                }
            )
    return machines, skipped


def extract_containers(dump, scope):
    containers = []
    for kind in CONTAINER_TYPES:
        for name, prototype in sorted((dump.get(kind) or {}).items()):
            if name not in scope:
                continue
            width, height = footprint(prototype)
            containers.append(
                {
                    "name": name,
                    "type": kind,
                    "volume": (prototype.get("fluid_box") or {}).get("volume"),
                    "tile_width": width,
                    "tile_height": height,
                }
            )
    return containers


def extract_drills(dump, scope):
    """Mining drills. `mining_speed` is Factorio's own, against a resource's hardness."""
    drills = []
    kind = DRILL_TYPE
    for name, prototype in sorted((dump.get(kind) or {}).items()):
        if name not in scope:
            continue
        usage, drain, drain_source, energy_type = energy(prototype)
        width, height = footprint(prototype)
        drills.append(
            {
                "name": name,
                "type": kind,
                "mining_speed": prototype.get("mining_speed"),
                "resource_categories": prototype.get("resource_categories"),
                "energy_usage": usage,
                "energy_type": energy_type,
                "drain": drain,
                "drain_source": drain_source,
                "burner": burner(prototype),
                "module_slots": prototype.get("module_slots", 0),
                "fluid_boxes": fluid_boxes(prototype, "input_fluid_box", "output_fluid_box"),
                "tile_width": width,
                "tile_height": height,
            }
        )
    return drills


def extract_boilers(dump, scope):
    """Boilers. What the buffer is, is what the prototype says it is -- ADR-0048 reads it."""
    boilers = []
    kind = BOILER_TYPE
    for name, prototype in sorted((dump.get(kind) or {}).items()):
        if name not in scope:
            continue
        # A boiler spends `energy_consumption`, not `energy_usage`; `energy()` is here
        # for the drain and the source type only.
        _, drain, drain_source, energy_type = energy(prototype)
        width, height = footprint(prototype)
        boilers.append(
            {
                "name": name,
                "type": kind,
                "energy_consumption": watts(prototype.get("energy_consumption")),
                "target_temperature": prototype.get("target_temperature"),
                # #188 asks for it by name. A boiler declares none of its own; the
                # figure is its energy source's, and 1 when the source has none.
                "effectivity": (prototype.get("energy_source") or {}).get(
                    "effectivity", 1
                ),
                "mode": prototype.get("mode", "heat-water-inside"),
                "energy_type": energy_type,
                "drain": drain,
                "drain_source": drain_source,
                "burner": burner(prototype),
                "fluid_boxes": fluid_boxes(prototype, "fluid_box", "output_fluid_box"),
                "tile_width": width,
                "tile_height": height,
            }
        )
    return boilers


def extract_generators(dump, scope):
    """Generators, and the one number of theirs Factorio does not state.

    `max_power_output` is optional and the steam engine omits it, so the figure is the
    engine's own product: a tick's fluid, times the degrees above the fluid's default
    temperature, times its heat capacity, times `effectivity`, times 60 ticks. It is
    derived and says so, the same way `drain` does.
    """
    generators = []
    kind = GENERATOR_TYPE
    for name, prototype in sorted((dump.get(kind) or {}).items()):
        if name not in scope:
            continue
        width, height = footprint(prototype)
        boxes = fluid_boxes(prototype, "fluid_box")
        power = si(prototype.get("max_power_output"))
        source = "explicit"
        consumed = boxes[0] if boxes else {}
        fluid = (dump.get("fluid") or {}).get(consumed.get("filter"))
        if power is None:
            if fluid is None:
                sys.exit(
                    f"{name} states no max_power_output and its consumption box "
                    f"filters on {consumed.get('filter')!r}, which is no fluid -- "
                    "the figure cannot be derived and must not be guessed"
                )
            power = (
                prototype["fluid_usage_per_tick"]
                * (prototype["maximum_temperature"] - fluid["default_temperature"])
                * si(fluid["heat_capacity"])
                * prototype.get("effectivity", 1)
                * TICKS_PER_SECOND
            )
            source = "derived"
        generators.append(
            {
                "name": name,
                "type": kind,
                "energy_source": (prototype.get("energy_source") or {}).get("type"),
                "effectivity": prototype.get("effectivity", 1),
                "fluid_usage_per_tick": prototype.get("fluid_usage_per_tick"),
                "maximum_temperature": prototype.get("maximum_temperature"),
                "max_power_output": power,
                "max_power_output_source": source,
                "burns_fluid": prototype.get("burns_fluid", False),
                "fluid_boxes": boxes,
                "tile_width": width,
                "tile_height": height,
            }
        )
    return generators


def extract_categories(dump):
    """Every recipe category, and every entity that declares it.

    The authority `data/pack/category-map.json`'s left-hand side is checked against. This
    walks the whole dump rather than `CRAFTING_TYPES`, because a category with no crafting
    machine is a real thing -- `hand-crafting` belongs to the character -- and a route that
    names one must not look like a typo.
    """
    declared = {name: [] for name in sorted(dump.get("recipe-category") or {})}
    for kind, prototypes in sorted(dump.items()):
        if not isinstance(prototypes, dict):
            continue
        for name, prototype in sorted(prototypes.items()):
            if not isinstance(prototype, dict):
                continue
            for category in prototype.get("crafting_categories") or []:
                declared.setdefault(category, []).append(f"{kind}/{name}")
    return declared


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--dump", type=Path, default=DEFAULT_DUMP)
    parser.add_argument("--out", type=Path, default=REPO / "data" / "factorio" / "machine.json")
    parser.add_argument(
        "--recipes",
        type=Path,
        default=REPO / "data" / "factorio" / "recipe.json",
        help="the scope: a machine is kept when its own item recipe is in this corpus",
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
    scope = in_scope(json.loads(args.recipes.read_text(encoding="utf-8")))

    machines, skipped = extract_machines(dump, scope)
    containers = extract_containers(dump, scope)
    drills = extract_drills(dump, scope)
    boilers = extract_boilers(dump, scope)
    generators = extract_generators(dump, scope)
    categories = extract_categories(dump)

    out = {
        "machines": machines,
        "containers": containers,
        "drills": drills,
        "boilers": boilers,
        "generators": generators,
        "categories": categories,
    }
    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(json.dumps(out, indent=2) + "\n", encoding="utf-8")

    print(f"in scope   {len(machines)} machines, {len(containers)} fluid containers")
    print(f"out of scope {len(skipped)}: " + ", ".join(sorted(skipped)))
    print(f"wrote      {args.out.relative_to(REPO)}\n")
    print(f"{'machine':22} {'speed':>6} {'kW':>8} {'drain kW':>9} {'mods':>5}  categories")
    for machine in machines:
        usage = machine["energy_usage"]
        drain = machine["drain"]
        print(
            f"{machine['name']:22} {machine['crafting_speed'] or 0:6} "
            f"{(usage or 0) / 1000:8.1f} {'' if drain is None else f'{drain / 1000:9.2f}':>9} "
            f"{machine['module_slots']:5}  {', '.join(machine['crafting_categories'] or [])[:60]}"
        )
    print("\nfluid anchors:")
    for container in containers:
        print(
            f"  {container['name']:14} {container['volume']:>6} units over "
            f"{container['tile_width']}x{container['tile_height']} tiles"
        )
    print("\nmining drills:")
    for drill in drills:
        print(
            f"  {drill['name']:22} speed {drill['mining_speed']:<5} "
            f"{(drill['energy_usage'] or 0) / 1000:7.1f} kW  "
            f"{drill['tile_width']}x{drill['tile_height']}"
        )
    print("\nboilers:")
    for boiler in boilers:
        print(
            f"  {boiler['name']:22} {(boiler['energy_consumption'] or 0) / 1e6:5.2f} MW "
            f"-> {boiler['target_temperature']}C  "
            + ", ".join(
                f"{b['name'] or b['production_type']} {b['volume']}" for b in boiler["fluid_boxes"]
            )
        )
    print("\ngenerators:")
    for generator in generators:
        print(
            f"  {generator['name']:22} {(generator['max_power_output'] or 0) / 1000:7.1f} kW "
            f"({generator['max_power_output_source']}) on "
            + ", ".join(
                f"{b['filter']} >={b['minimum_temperature']}C" for b in generator["fluid_boxes"]
            )
        )
    print("\ncategories with no crafting entity: "
          + ", ".join(n for n, who in categories.items() if not who))


if __name__ == "__main__":
    main()
