#!/usr/bin/env python3
"""Emit the two mining rigs' corpus row and pack-side assets (#192, #193).

ADR-0043's ladder is two rigs, and #192 is the shared idiom both stand on: an anchor block plus
parts that forward to it, placed as one square that extends away from the player. This script
supplies the one thing that idiom needs and this ticket must not type by hand -- the footprint
size -- plus the ordinary blockstate/model/lang/loot-table plumbing every `planetaryfactory:`
block needs under ADR-0015's split (mechanism in the mod, assets in the pack).

**Every number is read, not chosen.** `data/factorio/machine.json`'s `drills` rows carry the
whole prototype (#188, widened by #193); this script keeps only the two solid-ore drills
(`resource_categories` containing `basic-solid`) and copies the fields the mod reads to
`mod/src/main/resources/planetaryfactory_core/mining/drills.json`, which the mod reads at
class-init the same way `OreCorpus` reads `ore/amounts.json`. The pumpjack is a fluid drill
(`basic-fluid`) and neither ticket's scope.

**It copies; it does not derive.** `vector_to_place_result` and `resource_searching_radius` are
passed through as Factorio states them -- centre-relative, in tiles -- rather than turned into a
block offset here. Turning them into one is arithmetic against a footprint and a facing, and it
belongs in `RigOutputTile` and `RigArea`, where the mod's Minecraft-free test source set can
assert it. A derivation buried in a generator is a derivation nothing checks.

Usage:

    scripts/build-rig-assets.py            # writes the resource and the pack assets
    scripts/build-rig-assets.py --check    # asserts both are already up to date; no writes
"""
import argparse
import json
import os
import sys

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
MACHINE_CORPUS = os.path.join(ROOT, "data", "factorio", "machine.json")
DRILL_RESOURCE = os.path.join(
    ROOT, "mod", "src", "main", "resources", "planetaryfactory_core", "mining", "drills.json"
)
ASSETS = os.path.join(ROOT, "kubejs", "assets", "planetaryfactory")
DATA = os.path.join(ROOT, "kubejs", "data", "planetaryfactory")

NAMESPACE = "planetaryfactory"

# The two rigs. `factorio_name` is the corpus key the row is read from; everything else here is
# display/texture choices, not numbers ADR-0022 governs.
RIGS = {
    "burner_mining_drill": {
        "factorio_name": "burner-mining-drill",
        "name": "Burner Mining Drill",
        # A burner: the blast furnace's face reads as something that is fed and lit.
        "front": "minecraft:block/blast_furnace_front",
        "side": "minecraft:block/blast_furnace_side",
        "top": "minecraft:block/blast_furnace_top",
    },
    "electric_mining_drill": {
        "factorio_name": "electric-mining-drill",
        "name": "Electric Mining Drill",
        # No fire on this one (ADR-0036's pole customer), so a plain machine face instead.
        "front": "minecraft:block/observer_front",
        "side": "minecraft:block/observer_side",
        "top": "minecraft:block/observer_top",
    },
}

FACINGS = {"north": 0, "east": 90, "south": 180, "west": 270}

# The rig screen's own strings (#193). Not per-rig, so they sit beside the block names rather than
# being derived from RIGS -- the same three the furnace screen carries, because the gauge answers
# the same question: what is banked, and what a tick of work costs.
SCREEN_LANG = {
    "tooltip.planetaryfactory.rig.fuel": "%s / %s J",
    "tooltip.planetaryfactory.rig.fuel.seconds": "%ss of mining at %s J/t",
    "tooltip.planetaryfactory.rig.fuel.out": "No fuel burning",
}

# Every field the mod reads off a row. A corpus regeneration that drops one is a hard failure
# here rather than a null reaching Java, where it would surface as a rig that mines at no rate.
REQUIRED_FIELDS = (
    "tile_width",
    "tile_height",
    "mining_speed",
    "energy_usage",
    "energy_type",
    "vector_to_place_result",
    "resource_searching_radius",
)


def drills_from_corpus():
    with open(MACHINE_CORPUS, encoding="utf-8") as handle:
        machine = json.load(handle)
    drills = {
        row["name"]: row
        for row in machine["drills"]
        if "basic-solid" in (row.get("resource_categories") or [])
    }
    rows = {}
    for block_name, rig in RIGS.items():
        row = drills.get(rig["factorio_name"])
        if row is None:
            sys.exit(f"{rig['factorio_name']} is not a basic-solid drill in {MACHINE_CORPUS}")
        for field in REQUIRED_FIELDS:
            if row.get(field) is None:
                sys.exit(
                    f"{rig['factorio_name']} has no {field} in {MACHINE_CORPUS} "
                    "-- re-run scripts/factorio-machine-extract.py"
                )
        burner = row.get("burner") or {}
        rows[rig["factorio_name"]] = {
            "tile_width": row["tile_width"],
            "tile_height": row["tile_height"],
            "mining_speed": row["mining_speed"],
            "energy_usage": row["energy_usage"],
            "energy_type": row["energy_type"],
            "fuel_categories": burner.get("fuel_categories"),
            "vector_to_place_result": row["vector_to_place_result"],
            "resource_searching_radius": row["resource_searching_radius"],
        }
    return rows


def blockstate(model_name):
    return {
        "variants": {
            f"facing={facing}": ({"model": model_name} if y == 0 else {"model": model_name, "y": y})
            for facing, y in FACINGS.items()
        }
    }


def oriented_model(rig):
    """A model with a distinct front face, so the rig's facing is visible on the block.

    **The facing is the mechanic** -- ADR-0043 gives a rig one output tile and makes placement a
    decision the player gets right or wrong -- and a `cube_all` placeholder made it invisible: every
    block of the footprint looked the same on every side, so a mis-faced rig was indistinguishable
    from a correct one until it failed to fill anything.

    `minecraft:block/orientable` puts `front` on the north face, and the blockstate already rotates
    the model by facing, so the front texture lands on the side the rig ejects towards. Applied to
    the parts as well as the anchor: the footprint reads as one machine with one face, and the
    interior faces are occluded anyway. Which block is the anchor stays invisible, which is correct
    -- every part opens it and breaking any part breaks the rig.

    Still placeholder art, in that it is vanilla's rather than the pack's; what is no longer
    placeholder is that it has an orientation at all.
    """
    return {
        "parent": "minecraft:block/orientable",
        "textures": {
            "front": rig["front"],
            "side": rig["side"],
            "top": rig["top"],
            "particle": rig["side"],
        },
    }


def item_model(model_name):
    return {"parent": model_name}


def self_drop_loot_table(block_id):
    return {
        "type": "minecraft:block",
        "pools": [
            {
                "rolls": 1,
                "entries": [{"type": "minecraft:item", "name": block_id}],
            }
        ],
    }


EMPTY_LOOT_TABLE = {"type": "minecraft:empty"}


def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as handle:
        json.dump(data, handle, indent=2)
        handle.write("\n")


def planned_files(drills):
    """Every file this script owns, as {path: data}. The single source `--check` and the writer
    both walk."""
    files = {DRILL_RESOURCE: {"drills": drills}}

    lang = dict(SCREEN_LANG)
    for block_name, rig in RIGS.items():
        model_name = f"{NAMESPACE}:block/{block_name}"
        files[os.path.join(ASSETS, "blockstates", f"{block_name}.json")] = blockstate(model_name)
        files[os.path.join(ASSETS, "models", "block", f"{block_name}.json")] = oriented_model(rig)
        files[os.path.join(ASSETS, "models", "item", f"{block_name}.json")] = item_model(model_name)
        files[os.path.join(DATA, "loot_table", "blocks", f"{block_name}.json")] = self_drop_loot_table(
            f"{NAMESPACE}:{block_name}"
        )
        lang[f"block.{NAMESPACE}.{block_name}"] = rig["name"]

        part_name = f"{block_name}_part"
        part_model_name = f"{NAMESPACE}:block/{part_name}"
        files[os.path.join(ASSETS, "blockstates", f"{part_name}.json")] = blockstate(part_model_name)
        files[os.path.join(ASSETS, "models", "block", f"{part_name}.json")] = oriented_model(rig)
        # No item model: a part is never held. It is placed by the rig's own item and nothing
        # else, so it has no `BlockItem` and no entry in a creative tab.
        files[os.path.join(DATA, "loot_table", "blocks", f"{part_name}.json")] = EMPTY_LOOT_TABLE
        lang[f"block.{NAMESPACE}.{part_name}"] = f"{rig['name']} (part)"

    return files, lang


def lang_path():
    return os.path.join(ASSETS, "lang", "en_us.json")


def check():
    files, lang = planned_files(drills_from_corpus())
    problems = []
    for path, expected in files.items():
        if not os.path.isfile(path):
            problems.append(f"missing: {path}")
            continue
        with open(path, encoding="utf-8") as handle:
            actual = json.load(handle)
        if actual != expected:
            problems.append(f"stale: {path}")

    existing_lang = {}
    if os.path.isfile(lang_path()):
        with open(lang_path(), encoding="utf-8") as handle:
            existing_lang = json.load(handle)
    for key, value in lang.items():
        if existing_lang.get(key) != value:
            problems.append(f"lang key out of date: {key}")

    if problems:
        sys.exit("build-rig-assets.py --check failed:\n" + "\n".join(problems))
    print(f"OK -- {len(files)} files, {len(lang)} lang keys")


def build():
    files, lang = planned_files(drills_from_corpus())
    for path, data in files.items():
        write(path, data)

    existing_lang = {}
    if os.path.isfile(lang_path()):
        with open(lang_path(), encoding="utf-8") as handle:
            existing_lang = json.load(handle)
    existing_lang.update(lang)
    write(lang_path(), existing_lang)
    print(f"wrote {len(files)} files, updated {len(lang)} lang keys")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    check() if args.check else build()


if __name__ == "__main__":
    main()
