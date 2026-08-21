#!/usr/bin/env python3
"""Check that Sapros's flora data hangs together, without launching Minecraft.

Every failure this covers is one the game reports late, quietly, or not at all: a tree
feature naming a block nobody registers places air; a loot table naming an item nobody
registers drops nothing; a blockstate file missing a variant is a purple-black canopy the
first time a leaf fruits. All of them are hours away from the edit that caused them, and
all of them are a string comparison here.

What it deliberately does not check is behaviour -- refruiting, growth, whether Create's saw
fells the tree. That needs the game, and it is the launch test's job.

Usage: tests/flora/test_flora_data.py
"""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent.parent
DATA = ROOT / "kubejs/data/planetaryfactory"
ASSETS = ROOT / "kubejs/assets/planetaryfactory"

# Ids the mod registers, per ADR-0015's ownership rule. Parsed from the Java rather than
# hardcoded, so moving one across the boundary fails here instead of at startup.
MOD_SOURCE = ROOT / "mod/src/main/java/com/planetaryfactory/core/PFBlocks.java"
KUBEJS_BLOCKS = ROOT / "kubejs/startup_scripts/blocks.js"
KUBEJS_ITEMS = ROOT / "kubejs/startup_scripts/items.js"

failures = []


def check(condition, message):
    if not condition:
        failures.append(message)
    print(("ok   " if condition else "FAIL ") + message)


def mod_block_ids():
    return {"planetaryfactory:" + m
            for m in re.findall(r'sapling\("([a-z_]+)"', MOD_SOURCE.read_text())}


def kubejs_ids(path):
    return set(re.findall(r"event\.create\('(planetaryfactory:[a-z_]+)'", path.read_text()))


def json_strings(node):
    """Every string anywhere in a JSON document, so a check cannot miss a nesting level."""
    if isinstance(node, str):
        yield node
    elif isinstance(node, dict):
        for value in node.values():
            yield from json_strings(value)
    elif isinstance(node, list):
        for value in node:
            yield from json_strings(value)


def main():
    blocks = mod_block_ids() | kubejs_ids(KUBEJS_BLOCKS)
    items = kubejs_ids(KUBEJS_ITEMS)
    # A block item exists for every block, so a loot table may name either.
    registered = blocks | items

    check(mod_block_ids() == {"planetaryfactory:yumako_sapling",
                              "planetaryfactory:jellystem_sapling"},
          "the mod registers exactly the two saplings")
    check(not (mod_block_ids() & kubejs_ids(KUBEJS_BLOCKS)),
          "no id is registered by both the mod and KubeJS")
    check(items == {"planetaryfactory:yumako_fresh", "planetaryfactory:jellynut_fresh"},
          "the two harvested materials are registered as Fresh, and only Fresh")
    check(all(not i.endswith(("_ripe", "_stale", "_spoiling")) for i in items),
          "no Decay stage beyond Fresh ships here")
    # Jelly is what a Biochamber makes from Jellynut, and it belongs to `Puzzle: Sapros`.
    # Registering it here under any name would settle a design decision this ticket does not own.
    check(not any(re.match(r"planetaryfactory:jelly(_|$)", i) for i in registered),
          "no id registered here is called Jelly")

    for tree in ("yumako", "jellystem"):
        feature = DATA / f"worldgen/configured_feature/{tree}_tree.json"
        check(feature.is_file(), f"{tree} has a configured feature")
        referenced = {s for s in json_strings(json.loads(feature.read_text()))
                      if s.startswith("planetaryfactory:")}
        unknown = referenced - blocks
        check(not unknown, f"{tree}'s feature names only registered blocks (stray: {unknown})")

        placed = DATA / f"worldgen/placed_feature/{tree}_tree.json"
        check(placed.is_file(), f"{tree} has a placed feature")
        check(json.loads(placed.read_text())["feature"] == f"planetaryfactory:{tree}_tree",
              f"{tree}'s placed feature points at its configured feature")

    # The grower reaches its tree by resource key, and a typo there fails silently: the
    # sapling simply never grows.
    grower = (ROOT / "mod/src/main/java/com/planetaryfactory/core/PFTrees.java").read_text()
    check('feature(name + "_tree")' in grower and 'grower("yumako")' in grower
          and 'grower("jellystem")' in grower,
          "each grower names its tree's configured feature")

    for table in sorted((DATA / "loot_table/blocks").glob("*.json")):
        referenced = {s for s in json_strings(json.loads(table.read_text()))
                      if s.startswith("planetaryfactory:")}
        unknown = referenced - registered
        check(not unknown, f"{table.name} drops only registered items (stray: {unknown})")

    # The two harvests must stay mechanically distinct -- fruit off a standing canopy,
    # Jellynut out of a felled trunk. Identical drops would mean one behaviour on two blocks.
    leaves = json.loads((DATA / "loot_table/blocks/yumako_leaves.json").read_text())
    fruit_pool = [p for p in leaves["pools"]
                  if any(c.get("condition") == "minecraft:block_state_property"
                         for c in p.get("conditions", []))]
    check(len(fruit_pool) == 1,
          "yumako leaves drop their fruit only when the fruiting property is set")
    stem = json.loads((DATA / "loot_table/blocks/jellystem_stem.json").read_text())
    stem_drops = set(json_strings(stem)) & registered
    check(stem_drops == {"planetaryfactory:jellynut_fresh"},
          "a jellystem stem yields Jellynut and not itself")

    states = json.loads((ASSETS / "blockstates/yumako_leaves.json").read_text())["variants"]
    check(set(states) == {"fruiting=false", "fruiting=true"},
          "both fruiting states have a model")
    for variant in states.values():
        model = variant["model"].split(":")[1]
        check((ASSETS / f"models/block/{model.split('/')[-1]}.json").is_file(),
              f"model {variant['model']} exists")

    for texture in ("block/yumako_log", "block/yumako_leaves", "block/yumako_leaves_fruiting",
                    "block/jellystem_stem", "block/jellystem_leaves",
                    "block/yumako_sapling", "block/jellystem_sapling",
                    "item/yumako", "item/jellynut"):
        check((ASSETS / f"textures/{texture}.png").is_file(), f"texture {texture}.png exists")

    lang = json.loads((ASSETS / "lang/en_us.json").read_text())
    for sapling in mod_block_ids():
        key = "block.planetaryfactory." + sapling.split(":")[1]
        check(key in lang, f"the mod's {sapling} has a lang entry (the pack names it, not the jar)")

    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
