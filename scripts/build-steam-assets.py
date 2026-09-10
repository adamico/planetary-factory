#!/usr/bin/env python3
"""Emit Terra's steam-chain corpus resource and its two fluids' pack-side assets (#223, ADR-0048).

ADR-0048 is explicit that the Boiler and the Steam Engine (#224, #225) must be authored against
numbers that are *read*, not chosen: `data/factorio/machine.json`'s `boilers` array carries the
`boiler` prototype and its `generators` array carries `steam-engine` (both added by #188's widened
extractor). This script copies both rows -- whole, every field, nothing selected -- into
`mod/src/main/resources/planetaryfactory_core/fluid/steam_chain.json`, which
{@code SteamChainCorpus} reads at class-init the same way {@code PumpCorpus} reads `pumps.json` and
{@code RigCorpus} reads `mining/drills.json`.

**Nothing is decided here.** Neither #224 nor #225 exists yet, so this script does not guess which
fields the Boiler or the Steam Engine will need -- it copies the rows whole, and the two tickets
that consume them pick their own fields out of what is already on disk. A hand-edited resource
would run the Boiler at a rate somebody chose, with nothing else failing; that is the one thing
this script exists to prevent.

Alongside the corpus copy, this script also writes the two fluids' lang keys (`fluid_type` and
bucket item names) and the two bucket items' models. Registration itself -- the `Fluid`,
`FluidType`, `LiquidBlock` and `BucketItem` -- is mechanism (ADR-0015) and lives in the mod as
ordinary Java; only the display names and the bucket icons are pack-side data, and both buckets
point at vanilla's own filled-bucket textures the way the Offshore Pump points at vanilla's own
block textures -- reused by reference, not duplicated.

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
STEAM_CHAIN_RESOURCE = os.path.join(
    ROOT, "mod", "src", "main", "resources", "planetaryfactory_core", "fluid", "steam_chain.json"
)
ASSETS = os.path.join(ROOT, "kubejs", "assets", "planetaryfactory")
NAMESPACE = "planetaryfactory"

BOILER_NAME = "boiler"
STEAM_ENGINE_NAME = "steam-engine"

# The two fluids ADR-0048 registers. Both `planetaryfactory:`, never `gtceu:steam` -- see the ADR.
FLUIDS = {
    "steam": "Steam",
    "superheated_steam": "Superheated Steam",
}

# Vanilla's own filled-bucket textures, referenced rather than duplicated -- the same reuse the
# Offshore Pump makes of vanilla's block textures. Steam borrows the water bucket's icon (it is,
# after all, boiled water); Superheated Steam borrows the lava bucket's -- the hottest thing vanilla
# draws a bucket for.
BUCKET_TEXTURES = {
    "steam": "minecraft:item/water_bucket",
    "superheated_steam": "minecraft:item/lava_bucket",
}

FLUID_LANG = {f"fluid_type.{NAMESPACE}.{name}": display for name, display in FLUIDS.items()}
BUCKET_LANG = {
    f"item.{NAMESPACE}.{name}_bucket": f"{display} Bucket" for name, display in FLUIDS.items()
}


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
    }


def bucket_item_model(fluid_name):
    return {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": BUCKET_TEXTURES[fluid_name]},
    }


def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as handle:
        json.dump(data, handle, indent=2)
        handle.write("\n")


def planned_files(rows):
    files = {STEAM_CHAIN_RESOURCE: rows}
    for fluid_name in FLUIDS:
        files[os.path.join(ASSETS, "models", "item", f"{fluid_name}_bucket.json")] = (
            bucket_item_model(fluid_name)
        )
    lang = dict(FLUID_LANG)
    lang.update(BUCKET_LANG)
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
