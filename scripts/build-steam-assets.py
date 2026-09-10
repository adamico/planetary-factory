#!/usr/bin/env python3
"""Emit Terra's steam-chain corpus resource and its two fluids' pack-side assets (#223, ADR-0048).

ADR-0048 is explicit that the Boiler and the Steam Engine (#224, #225) must be authored against
numbers that are *read*, not chosen: `data/factorio/machine.json`'s `boilers` array carries the
`boiler` prototype and its `generators` array carries `steam-engine` (both added by #188's widened
extractor). This script copies both rows -- whole, every field, nothing selected -- into
`mod/src/main/resources/planetaryfactory_core/fluid/steam_chain.json`, which
{@code SteamChainCorpus} reads at class-init the same way {@code PumpCorpus} reads `pumps.json` and
{@code RigCorpus} reads `mining/drills.json`.

**Nothing is decided here.** The script copies the rows whole and the tickets that consume them
pick their own fields out of what is already on disk. A hand-edited resource would run the Boiler
at a rate somebody chose, with nothing else failing; that is the one thing this script exists to
prevent.

#224 added two things to it: the two Factorio *fluid* rows the Boiler's rate is derived from -- the
rise is paid for at **steam's** heat capacity, and water's is ten times larger, so both are copied
rather than either being typed into Java -- and the Boiler block's own pack-side
blockstate/model/lang/loot-table plumbing, which under ADR-0015's split is the pack's rather than
the mod's. #225's Steam Engine will add its own.

Alongside the corpus copy, this script writes the two fluids' `fluid_type` lang keys. Registration
itself -- the `Fluid`, the `FluidType` and the `LiquidBlock` -- is mechanism (ADR-0015) and lives in
the mod as ordinary Java; only the display names are pack-side data.

**Neither fluid has a bucket**, so there is no bucket model and no bucket lang key to write.
ADR-0037 already answered portable fluid for this pack -- `planetaryfactory:barrel`, any fluid at
Factorio's own 50 mB -- and states that capacity as a rule a later container "does not get to be
re-argued from Minecraft's bucket" against. See `PFFluids`' javadoc.

Usage:

    scripts/build-steam-assets.py            # writes the resource and the pack assets
    scripts/build-steam-assets.py --check    # asserts both are already up to date; no writes
"""
import argparse
import json
import os
import sys

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
MACHINE_CORPUS = os.path.join(ROOT, "data", "factorio", "machine.json")
FLUID_CORPUS = os.path.join(ROOT, "data", "factorio", "fluid.json")
STEAM_CHAIN_RESOURCE = os.path.join(
    ROOT, "mod", "src", "main", "resources", "planetaryfactory_core", "fluid", "steam_chain.json"
)
ASSETS = os.path.join(ROOT, "kubejs", "assets", "planetaryfactory")
DATA = os.path.join(ROOT, "kubejs", "data", "planetaryfactory")
NAMESPACE = "planetaryfactory"

BOILER_NAME = "boiler"
STEAM_ENGINE_NAME = "steam-engine"

# The two fluids the Boiler's arithmetic is derived from (#224). `water` is what it consumes and
# `steam` what it makes, and the governing constant is *steam's* heat capacity rather than water's
# -- see `BoilerSpec`, which is where the trap is written down. Copied whole, the same rule the two
# machine rows follow: a heat capacity typed into Java is a rate nobody can check.
FLUID_NAMES = ("water", "steam")

BOILER_BLOCK = "boiler"
BOILER_DISPLAY = "Boiler"

# Display choices, not numbers ADR-0022 governs -- the same vanilla art the pump and the rigs use.
BOILER_TEXTURES = {
    "front": "minecraft:block/furnace_front_on",
    "side": "minecraft:block/blast_furnace_side",
    "top": "minecraft:block/blast_furnace_top",
}

FACINGS = {"north": 0, "east": 90, "south": 180, "west": 270}

# The Boiler's screen, which is the furnace ladder's screen with a second gauge on it. The keys are
# pack-side beside the block name for the reason the pump's refusal message is: they name the block
# and read as part of it.
BOILER_LANG = {
    f"block.{NAMESPACE}.{BOILER_BLOCK}": BOILER_DISPLAY,
    f"tooltip.{NAMESPACE}.boiler.fuel": "%s / %s J",
    f"tooltip.{NAMESPACE}.boiler.fuel.seconds": "%s s at %s J/t",
    f"tooltip.{NAMESPACE}.boiler.fuel.out": "Out of fuel",
    f"tooltip.{NAMESPACE}.boiler.water": "Water: %s / %s mB",
    f"tooltip.{NAMESPACE}.boiler.steam": "Steam: %s / %s mB",
}

# The two fluids ADR-0048 registers. Both `planetaryfactory:`, never `gtceu:steam` -- see the ADR.
FLUIDS = {
    "steam": "Steam",
    "superheated_steam": "Superheated Steam",
}

FLUID_LANG = {f"fluid_type.{NAMESPACE}.{name}": display for name, display in FLUIDS.items()}


def fluids_from_corpus():
    """The two fluid prototypes the Boiler's arithmetic reads, copied whole."""
    with open(FLUID_CORPUS, encoding="utf-8") as handle:
        rows = {row["name"]: row for row in json.load(handle).get("fluids", [])}
    fluids = {}
    for name in FLUID_NAMES:
        row = rows.get(name)
        if row is None:
            sys.exit(
                f"{name} is not in {FLUID_CORPUS} -- re-run scripts/factorio-fluid-extract.py"
            )
        fluids[name] = row
    return fluids


def steam_chain_from_corpus():
    with open(MACHINE_CORPUS, encoding="utf-8") as handle:
        machine = json.load(handle)
    boilers = {row["name"]: row for row in machine.get("boilers", [])}
    generators = {row["name"]: row for row in machine.get("generators", [])}

    boiler = boilers.get(BOILER_NAME)
    if boiler is None:
        sys.exit(
            f"{BOILER_NAME} is not in {MACHINE_CORPUS}'s boilers "
            "-- re-run scripts/factorio-machine-extract.py"
        )
    steam_engine = generators.get(STEAM_ENGINE_NAME)
    if steam_engine is None:
        sys.exit(
            f"{STEAM_ENGINE_NAME} is not in {MACHINE_CORPUS}'s generators "
            "-- re-run scripts/factorio-machine-extract.py"
        )

    # Whole rows, copied rather than filtered: see the module docstring.
    return {
        BOILER_NAME: boiler,
        STEAM_ENGINE_NAME: steam_engine,
        "fluids": fluids_from_corpus(),
    }


def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as handle:
        json.dump(data, handle, indent=2)
        handle.write("\n")


def blockstate(model_name):
    return {
        "variants": {
            f"facing={facing}": ({"model": model_name} if y == 0 else {"model": model_name, "y": y})
            for facing, y in FACINGS.items()
        }
    }


def oriented_model():
    """A model with a distinct front face, so the Boiler's fuel side is visible on the block."""
    return {
        "parent": "minecraft:block/orientable",
        "textures": {
            "front": BOILER_TEXTURES["front"],
            "side": BOILER_TEXTURES["side"],
            "top": BOILER_TEXTURES["top"],
            "particle": BOILER_TEXTURES["side"],
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


def planned_files(rows):
    files = {STEAM_CHAIN_RESOURCE: rows}
    model_name = f"{NAMESPACE}:block/{BOILER_BLOCK}"
    files[os.path.join(ASSETS, "blockstates", f"{BOILER_BLOCK}.json")] = blockstate(model_name)
    files[os.path.join(ASSETS, "models", "block", f"{BOILER_BLOCK}.json")] = oriented_model()
    files[os.path.join(ASSETS, "models", "item", f"{BOILER_BLOCK}.json")] = {"parent": model_name}
    files[os.path.join(DATA, "loot_table", "blocks", f"{BOILER_BLOCK}.json")] = self_drop_loot_table(
        f"{NAMESPACE}:{BOILER_BLOCK}"
    )
    lang = dict(FLUID_LANG)
    lang.update(BOILER_LANG)
    return files, lang


def lang_path():
    return os.path.join(ASSETS, "lang", "en_us.json")


def check():
    files, lang = planned_files(steam_chain_from_corpus())
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
        sys.exit("build-steam-assets.py --check failed:\n" + "\n".join(problems))
    print(f"OK -- {len(files)} files, {len(lang)} lang keys")


def build():
    files, lang = planned_files(steam_chain_from_corpus())
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
