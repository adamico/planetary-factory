#!/usr/bin/env python3
"""Assert every authored vein's surface indicator can actually resolve.

An ore vein's `indicators[].block` is an `Either<BlockState, Material>`, and both sides fail in
ways that only appear at world creation -- as `Failed to load registries`, on the screen a player
is sat in front of, with the vein named but not the reason:

  - a **bare string** is read as a GregTech *material*, so `minecraft:cobblestone` there is
    "Unknown registry key in [gtceu:material]" rather than the block anyone meant;
  - a material that exists but has **no surface rock** registered -- `gtceu:stone` is one -- parses
    and then throws "No surface rock registered for material stone" a layer later.

Neither is reachable from a static read of our own files, because the authority is the jar. So this
reads it: the set of materials GregTech itself indicates with is the set that demonstrably has a
rock, and anything we author has to be in it or be the blockstate form instead.

Usage: tests/worldgen/test_vein_indicators.py
"""
import json
import os
import re
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent.parent
VEINS = ROOT / "kubejs/data/gtceu/gtceu/ore_vein"


def jar():
    return next(
        ROOT / "mods" / f
        for f in os.listdir(ROOT / "mods")
        if re.match(r"gtceu-.*\.jar$", f)
    )


def materials_with_rocks(path):
    """Every material GregTech's own veins indicate with -- so, every one that has a rock."""
    prefix = "data/gtceu/gtceu/ore_vein/"
    found = set()
    with zipfile.ZipFile(path) as archive:
        for name in archive.namelist():
            if name.startswith(prefix) and name.endswith(".json"):
                for indicator in json.loads(archive.read(name)).get("indicators") or []:
                    block = indicator.get("block")
                    if isinstance(block, str):
                        found.add(block)
    return found


def main():
    known = materials_with_rocks(jar())
    failures = []
    checked = 0

    if not known:
        failures.append("no indicator materials found in the jar -- has the vein path moved?")

    for path in sorted(VEINS.glob("*.json")):
        for indicator in json.loads(path.read_text()).get("indicators") or []:
            checked += 1
            block = indicator.get("block")
            if isinstance(block, dict):
                if "Name" not in block:
                    failures.append(f"{path.name}: blockstate indicator has no Name")
            elif isinstance(block, str):
                if block not in known:
                    failures.append(
                        f"{path.name}: indicates with {block!r}, which no GregTech vein uses -- "
                        "if it has no surface rock the world will not create"
                    )
            else:
                failures.append(f"{path.name}: indicator block is {type(block).__name__}")

    for index, failure in enumerate(failures, 1):
        print(f"FAIL {index}: {failure}")
    if failures:
        return 1
    print(f"ok   {checked} vein indicator(s), every material one has a surface rock")
    return 0


if __name__ == "__main__":
    sys.exit(main())
