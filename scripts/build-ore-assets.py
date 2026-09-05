#!/usr/bin/env python3
"""Emit the ore blocks' blockstates, models, loot tables, tags and names (ADR-0041).

Five ore blocks times eight sprite stages is forty models and forty blockstate variants, all of
them mechanical. They are generated for the same reason the recipes are: the interesting content
is the *decisions* -- which item a block drops, which tags it carries -- and those are the tables
at the top of this file, argued once and applied five times.

Three decisions live here, each argued in ADR-0041:

  - **Each block drops GregTech's raw ore.** The block changes and the item does not, which is
    what keeps `data/pack/item-map.json`, every generated recipe, ADR-0032's 1:1 chain and
    ADR-0034's sweep untouched. The loot table is the non-player path only -- an explosion, a
    creative break -- because a player's break is a *draw* and the mod pays that out itself,
    one unit at a time. A loot table that rolled the block's whole amount would be a way to
    empty a patch with one hit.
  - **`c:ores` is not decoration.** GregTech's Miner scans that tag (`MinerLogic` reads
    `Tags.Blocks.ORES`), so it is the tag that decides whether rung 1's drill can see a
    pack-authored block at all. `#planetaryfactory:factorio_mining_time` is the other one: it is
    what gives ADR-0039's flat seconds-per-ore to these blocks, and it already names `#c:ores`.
  - **The stage is the model, not a texture predicate.** One model per stage, one variant per
    stage, so a stage that has no sprite fails at load rather than rendering as a missing texture
    on a block a player is standing on. No item model and no `BlockItem`: these blocks are dealt
    by worldgen and pay out an item that is not themselves, so an ore patch in a creative tab
    would be a second way to obtain one that ADR-0034's sweep never agreed to.

The sprites themselves come from `scripts/build-ore-textures.py`, and the stage *count* comes from
the corpus, so a Factorio release that changed the ladder changes both.

    scripts/build-ore-assets.py
    scripts/build-ore-assets.py --check    # what tests/ runs
"""
import argparse
import json
import os
import sys

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
CORPUS = os.path.join(ROOT, "mod/src/main/resources/planetaryfactory_core/ore/amounts.json")
ASSETS = os.path.join(ROOT, "kubejs", "assets", "planetaryfactory")
DATA = os.path.join(ROOT, "kubejs", "data")

NAMESPACE = "planetaryfactory"

# What each ore block pays out, and what it is called. The drops mirror `OreResource.java`, which
# is the mod's own copy of the same decision; `tests/factorio/test_ore_assets.py` asserts the two
# agree rather than trusting that they do.
ORES = {
    "iron": {"drop": "minecraft:raw_iron", "name": "Iron Ore Patch"},
    "copper": {"drop": "minecraft:raw_copper", "name": "Copper Ore Patch"},
    "coal": {"drop": "minecraft:coal", "name": "Coal Patch"},
    "uranium": {"drop": "gtceu:raw_uranium", "name": "Uranium Ore Patch"},
    "stone": {"drop": "minecraft:cobblestone", "name": "Stone Patch"},
}

# The tags every one of them carries, and why. `c:ores` is the one with teeth -- GregTech's Miner
# scans it -- and `mineable/pickaxe` plus `needs_stone_tool` are what make the Engineer's Pick the
# tool for them.
BLOCK_TAGS = {
    "c/tags/block/ores.json": "GregTech's Miner scans this tag; a block outside it is invisible to rung 1's drill.",
    "minecraft/tags/block/mineable/pickaxe.json": "The Engineer's Pick is a pickaxe (ADR-0039).",
    "minecraft/tags/block/needs_stone_tool.json": "Rung 0's Iron Pick is the tool tier the opening ships with.",
}


def blockstate(ore, stages):
    return {
        "variants": {
            f"stage={stage}": {"model": f"{NAMESPACE}:block/ore/{ore}_stage{stage}"}
            for stage in range(stages)
        }
    }


def model(ore, stage):
    return {
        "parent": "minecraft:block/cube_all",
        "textures": {"all": f"{NAMESPACE}:block/ore/{ore}_stage{stage}"},
    }


def loot_table(ore):
    """The non-player path, and it pays nothing -- see the third bullet in the module docstring.

    The table exists rather than being omitted because a block with no loot table at all logs a
    missing-table error every time one is destroyed. `ORES[ore]["drop"]` is still the drop of
    record; it reaches the player through `OreMining.drop`, which is the metered path.
    """
    return {"type": "minecraft:block", "pools": []}


def build():
    corpus = json.load(open(CORPUS, encoding="utf-8"))
    files = {}
    stages = max(len(entry["stage_ratios"]) for entry in corpus["resources"].values())
    if sorted(corpus["resources"]) != sorted(ORES):
        sys.exit(f"the corpus deals {sorted(corpus['resources'])} and this script names {sorted(ORES)}")

    for ore in sorted(ORES):
        block = f"{ore}_ore"
        files[os.path.join(ASSETS, "blockstates", f"{block}.json")] = blockstate(ore, stages)
        for stage in range(stages):
            files[os.path.join(ASSETS, "models", "block", "ore", f"{ore}_stage{stage}.json")] = \
                model(ore, stage)
        files[os.path.join(DATA, NAMESPACE, "loot_table", "blocks", f"{block}.json")] = loot_table(ore)

    blocks = [f"{NAMESPACE}:{ore}_ore" for ore in sorted(ORES)]
    for path, why in BLOCK_TAGS.items():
        files[os.path.join(DATA, path)] = {"__comment": why, "replace": False, "values": blocks}

    return files, stages


def merge_lang(files):
    """The block names, appended to the pack's lang file rather than written over it.

    Order is preserved and new keys land at the end. The file is grouped by subject rather than
    sorted, and re-sorting it would bury a two-line addition in a thirty-line diff.
    """
    path = os.path.join(ASSETS, "lang", "en_us.json")
    lang = json.load(open(path, encoding="utf-8"))
    for ore, spec in ORES.items():
        lang[f"block.{NAMESPACE}.{ore}_ore"] = spec["name"]
    lang["tooltip.planetaryfactory.ore.jade.amount"] = "Ore left: %s of %s"
    files[path] = lang
    return files


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()

    files, stages = build()
    files = merge_lang(files)
    rendered = {path: json.dumps(body, indent=2) + "\n" for path, body in files.items()}

    if args.check:
        stale = [
            path for path, body in sorted(rendered.items())
            if not os.path.exists(path) or open(path, encoding="utf-8").read() != body
        ]
        for path in stale:
            print(f"FAIL: {os.path.relpath(path, ROOT)} is missing or stale")
        if stale:
            return 1
        print(f"ok   {len(rendered)} ore assets, {len(ORES)} blocks x {stages} stages")
        return 0

    for path, body in sorted(rendered.items()):
        os.makedirs(os.path.dirname(path), exist_ok=True)
        open(path, "w", encoding="utf-8").write(body)
    print(f"wrote {len(rendered)} files: {len(ORES)} ore blocks x {stages} stages")
    return 0


if __name__ == "__main__":
    sys.exit(main())
