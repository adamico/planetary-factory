#!/usr/bin/env python3
"""Emit the Offshore Pump's corpus row and pack-side assets (#213, ADR-0050).

ADR-0050 makes the pump the *origin* of every drop of water in the factory: it is where the rule
"water is extracted and transported, never created" becomes a block the player places rather than
a number in a config file. This script supplies the one thing the mod must not type by hand --
`pumping_speed` -- plus the ordinary blockstate/model/lang/loot-table plumbing every
`planetaryfactory:` block needs under ADR-0015's split (mechanism in the mod, assets in the pack).

**Every number is read, not chosen.** `data/factorio/machine.json`'s `pumps` row (added by #210)
carries the prototype; this script copies the fields the mod reads to
`mod/src/main/resources/planetaryfactory_core/fluid/pumps.json`, which `PumpCorpus` reads at
class-init the same way `RigCorpus` reads `mining/drills.json`.

**It copies; it does not derive.** `pumping_speed` is passed through as Factorio states it -- per
*Factorio* tick, which is not Minecraft's tick -- and turning it into millibuckets a second or a
tick is `OffshorePumpSpec`'s, where the mod's Minecraft-free test source set can assert it. A
derivation buried in a generator is a derivation nothing checks, and this particular one has two
traps in it (see that class).

Usage:

    scripts/build-pump-assets.py            # writes the resource and the pack assets
    scripts/build-pump-assets.py --check    # asserts both are already up to date; no writes
"""
import argparse
import json
import os
import sys

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
MACHINE_CORPUS = os.path.join(ROOT, "data", "factorio", "machine.json")
PUMP_RESOURCE = os.path.join(
    ROOT, "mod", "src", "main", "resources", "planetaryfactory_core", "fluid", "pumps.json"
)
ASSETS = os.path.join(ROOT, "kubejs", "assets", "planetaryfactory")
DATA = os.path.join(ROOT, "kubejs", "data", "planetaryfactory")
NAMESPACE = "planetaryfactory"

BLOCK_NAME = "offshore_pump"
FACTORIO_NAME = "offshore-pump"
DISPLAY_NAME = "Offshore Pump"

# Display choices, not numbers ADR-0022 governs. Vanilla art, as the rigs use: what is not
# placeholder about it is that the block reads as machinery sat on water.
TEXTURES = {
    "front": "minecraft:block/dispenser_front",
    "side": "minecraft:block/furnace_side",
    "top": "minecraft:block/cauldron_top",
}

FACINGS = {"north": 0, "east": 90, "south": 180, "west": 270}

# The refusal message (ADR-0050): a pump that places and then silently produces nothing reaches
# the player as a dead factory three machines later. It lives in the pack's lang file beside the
# block name rather than in the mod's, because it names the block and reads as part of it.
BLOCK_LANG = {
    f"block.{NAMESPACE}.{BLOCK_NAME}": DISPLAY_NAME,
    f"message.{NAMESPACE}.{BLOCK_NAME}.no_water":
        "An Offshore Pump must touch still water. Flowing water is not a source.",
}

# Every field the mod reads off the row. A corpus regeneration that drops one is a hard failure
# here rather than a null reaching Java, where it would surface as a pump that produces nothing.
REQUIRED_FIELDS = ("pumping_speed", "energy_source")


def pump_from_corpus():
    with open(MACHINE_CORPUS, encoding="utf-8") as handle:
        machine = json.load(handle)
    rows = {row["name"]: row for row in machine.get("pumps", [])}
    row = rows.get(FACTORIO_NAME)
    if row is None:
        sys.exit(
            f"{FACTORIO_NAME} is not in {MACHINE_CORPUS}'s pumps "
            "-- re-run scripts/factorio-machine-extract.py"
        )
    for field in REQUIRED_FIELDS:
        if row.get(field) is None:
            sys.exit(
                f"{FACTORIO_NAME} has no {field} in {MACHINE_CORPUS} "
                "-- re-run scripts/factorio-machine-extract.py"
            )
    return {
        FACTORIO_NAME: {
            "pumping_speed": row["pumping_speed"],
            # Carried so the mod's "this machine takes no power" is a read fact rather than an
            # omission. ADR-0050: the `energy_usage` sitting beside it in Factorio has no consumer.
            "energy_source": row["energy_source"],
        }
    }


def blockstate(model_name):
    return {
        "variants": {
            f"facing={facing}": ({"model": model_name} if y == 0 else {"model": model_name, "y": y})
            for facing, y in FACINGS.items()
        }
    }


def oriented_model():
    """A model with a distinct front face, so the pump's facing is visible on the block.

    The facing is not mechanism here -- the predicate looks at every neighbour, so a pump works
    whichever way it points. It is oriented anyway because a `cube_all` machine reads as
    scenery, and the player needs to see at a glance which side of it is against the water.
    """
    return {
        "parent": "minecraft:block/orientable",
        "textures": {
            "front": TEXTURES["front"],
            "side": TEXTURES["side"],
            "top": TEXTURES["top"],
            "particle": TEXTURES["side"],
        },
    }


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


def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as handle:
        json.dump(data, handle, indent=2)
        handle.write("\n")


def planned_files(rows):
    files = {PUMP_RESOURCE: rows}
    model_name = f"{NAMESPACE}:block/{BLOCK_NAME}"
    files[os.path.join(ASSETS, "blockstates", f"{BLOCK_NAME}.json")] = blockstate(model_name)
    files[os.path.join(ASSETS, "models", "block", f"{BLOCK_NAME}.json")] = oriented_model()
    files[os.path.join(ASSETS, "models", "item", f"{BLOCK_NAME}.json")] = {"parent": model_name}
    files[os.path.join(DATA, "loot_table", "blocks", f"{BLOCK_NAME}.json")] = self_drop_loot_table(
        f"{NAMESPACE}:{BLOCK_NAME}"
    )
    return files, dict(BLOCK_LANG)


def lang_path():
    return os.path.join(ASSETS, "lang", "en_us.json")


def check():
    files, lang = planned_files(pump_from_corpus())
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
        sys.exit("build-pump-assets.py --check failed:\n" + "\n".join(problems))
    print(f"OK -- {len(files)} files, {len(lang)} lang keys")


def build():
    files, lang = planned_files(pump_from_corpus())
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
