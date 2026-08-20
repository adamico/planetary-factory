#!/usr/bin/env python3
"""Regenerate Terra's reweighted copies of GregTech's overworld ore veins.

docs/scratch/planets.md gives the Overworld iron, copper, coal and zinc. Cutting
GregTech's set to those four would starve its own recipe tree, which wants tin, lead
and nickel in the first hours, so the four are made *prominent* instead: the materials
the early recipes demand go up, the decorative and late-tier veins go down, and nothing
is removed. Weights are relative within a layer, so stepping the clutter back is half
of raising the signal.

The reweighting is a datapack override rather than a script: GTCEu 7.0.2's
`GTCEuServerEvents.oreVeins` resolves the ore vein registry from a registry access that
does not have it during world load, and throws `Missing registry: gtceu:ore_vein`
before any handler of ours runs. So the pack ships its own copy of each vein, at the
same id, from a pack that sorts above GregTech's.

Each generated file is GregTech 7.0.2's own definition with one field changed. Rerun
this after a GregTech update so the copies do not silently pin an old definition:

    scripts/build-terra-vein-weights.py
"""
import json
import zipfile
from pathlib import Path

INSTANCE = Path(__file__).resolve().parent.parent
GTCEU_JAR = INSTANCE / "mods/gtceu-1.21.1-7.0.2.jar"
VEIN_PATH = "data/gtceu/gtceu/ore_vein/%s.json"
OUT_DIR = INSTANCE / "kubejs/data/gtceu/gtceu/ore_vein"

# gtceu vein id -> Terra's weight, with GregTech's stock weight beside it.
TERRA_VEIN_WEIGHTS = {
    # The early game, made unmissable.
    "iron": 160,          # 120
    "copper": 140,        #  80
    "coal": 130,          #  80
    "cassiterite": 90,    #  80  tin
    "copper_tin": 70,     #  50
    "galena": 60,         #  40  lead, silver
    "magnetite": 60,      #  80  iron, gold — still common, no longer rivalling iron
    "nickel": 55,         #  40
    # Decorative, chemical and late-tier veins, stepped back so the above stand out.
    "mineral_sand": 40,   #  80
    "garnet_tin": 40,     #  80
    "salts": 40,          #  50
    "redstone": 40,       #  60
    "sapphire": 30,       #  60
    "apatite": 30,        #  40
    "oilsands": 30,       #  40
    "lapis": 30,          #  40
    "diamond": 30,        #  40
    "lubricant": 20,      #  40
    "garnet": 20,         #  40
    "manganese": 15,      #  20
    "mica": 10,           #  20
    "olivine": 10,        #  20
}


def main():
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    for stale in OUT_DIR.glob("*.json"):
        stale.unlink()

    with zipfile.ZipFile(GTCEU_JAR) as jar:
        for vein_id, weight in TERRA_VEIN_WEIGHTS.items():
            vein = json.loads(jar.read(VEIN_PATH % vein_id))
            if "minecraft:overworld" not in vein["dimension_filter"]:
                raise SystemExit(f"{vein_id} is not an overworld vein — check the table")
            vein["weight"] = weight
            with open(OUT_DIR / f"{vein_id}.json", "w") as out:
                json.dump(vein, out, indent=4, sort_keys=True)
                out.write("\n")

    print(f"wrote {len(TERRA_VEIN_WEIGHTS)} reweighted veins to {OUT_DIR.relative_to(INSTANCE)}")


if __name__ == "__main__":
    main()
