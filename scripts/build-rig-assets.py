#!/usr/bin/env python3
"""Emit the two mining rigs' footprint data and pack-side assets (#192).

ADR-0043's ladder is two rigs, and #192 is the shared idiom both stand on: an anchor block plus
parts that forward to it, placed as one square that extends away from the player. This script
supplies the one thing that idiom needs and this ticket must not type by hand -- the footprint
size -- plus the ordinary blockstate/model/lang/loot-table plumbing every `planetaryfactory:`
block needs under ADR-0015's split (mechanism in the mod, assets in the pack).

**The footprint is read, not chosen.** `data/factorio/machine.json`'s `drills` rows carry
`tile_width`/`tile_height` for every mining-drill prototype (#188); this script keeps only the two
solid-ore drills (`resource_categories` containing `basic-solid`) and writes them to
`mod/src/main/resources/planetaryfactory_core/mining/footprint.json`, which the mod reads at
class-init the same way `OreCorpus` reads `ore/amounts.json`. The pumpjack is a fluid drill
(`basic-fluid`) and #192's scope is the two solid rigs only.

Nothing here decides operations-per-second, fuel or the overlay -- #192 is an inert footprint with
a facing, and those numbers belong to #193/#194.

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
FOOTPRINT_RESOURCE = os.path.join(
    ROOT, "mod", "src", "main", "resources", "planetaryfactory_core", "mining", "footprint.json"
)
ASSETS = os.path.join(ROOT, "kubejs", "assets", "planetaryfactory")
DATA = os.path.join(ROOT, "kubejs", "data", "planetaryfactory")

NAMESPACE = "planetaryfactory"

# The two rigs. `factorio_name` is the corpus key the footprint is read from; everything else here
# is display/texture choices, not numbers ADR-0022 governs.
RIGS = {
    "burner_mining_drill": {
        "factorio_name": "burner-mining-drill",
        "name": "Burner Mining Drill",
        "texture": "minecraft:block/coal_block",
    },
    "electric_mining_drill": {
        "factorio_name": "electric-mining-drill",
        "name": "Electric Mining Drill",
        "texture": "minecraft:block/iron_block",
    },
}

FACINGS = {"north": 0, "east": 90, "south": 180, "west": 270}


def footprints_from_corpus():
    with open(MACHINE_CORPUS, encoding="utf-8") as handle:
        machine = json.load(handle)
    drills = {
        row["name"]: row
        for row in machine["drills"]
        if "basic-solid" in (row.get("resource_categories") or [])
    }
    footprints = {}
    for block_name, rig in RIGS.items():
        row = drills.get(rig["factorio_name"])
        if row is None:
            sys.exit(f"{rig['factorio_name']} is not a basic-solid drill in {MACHINE_CORPUS}")
        footprints[rig["factorio_name"]] = {
            "tile_width": row["tile_width"],
            "tile_height": row["tile_height"],
        }
    return footprints


def blockstate(model_name):
    return {
        "variants": {
            f"facing={facing}": ({"model": model_name} if y == 0 else {"model": model_name, "y": y})
            for facing, y in FACINGS.items()
        }
    }


def cube_model(texture):
    return {"parent": "minecraft:block/cube_all", "textures": {"all": texture}}


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


def planned_files(footprints):
    """Every file this script owns, as {path: data}. The single source `--check` and the writer
    both walk."""
    files = {FOOTPRINT_RESOURCE: {"drills": footprints}}

    lang = {}
    for block_name, rig in RIGS.items():
        model_name = f"{NAMESPACE}:block/{block_name}"
        files[os.path.join(ASSETS, "blockstates", f"{block_name}.json")] = blockstate(model_name)
        files[os.path.join(ASSETS, "models", "block", f"{block_name}.json")] = cube_model(rig["texture"])
        files[os.path.join(ASSETS, "models", "item", f"{block_name}.json")] = item_model(model_name)
        files[os.path.join(DATA, "loot_table", "blocks", f"{block_name}.json")] = self_drop_loot_table(
            f"{NAMESPACE}:{block_name}"
        )
        lang[f"block.{NAMESPACE}.{block_name}"] = rig["name"]

        part_name = f"{block_name}_part"
        part_model_name = f"{NAMESPACE}:block/{part_name}"
        files[os.path.join(ASSETS, "blockstates", f"{part_name}.json")] = blockstate(part_model_name)
        files[os.path.join(ASSETS, "models", "block", f"{part_name}.json")] = cube_model(rig["texture"])
        # No item model: a part is never held. It is placed by the rig's own item and nothing
        # else, so it has no `BlockItem` and no entry in a creative tab.
        files[os.path.join(DATA, "loot_table", "blocks", f"{part_name}.json")] = EMPTY_LOOT_TABLE
        lang[f"block.{NAMESPACE}.{part_name}"] = f"{rig['name']} (part)"

    return files, lang


def lang_path():
    return os.path.join(ASSETS, "lang", "en_us.json")


def check():
    footprints = footprints_from_corpus()
    files, lang = planned_files(footprints)
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
    footprints = footprints_from_corpus()
    files, lang = planned_files(footprints)
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
