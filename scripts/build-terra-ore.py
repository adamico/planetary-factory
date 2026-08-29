#!/usr/bin/env python3
"""Cut Terra's ore to Factorio's set and re-band the survivors (ADR-0019/0021, issue #59).

Terra had four ore systems and three of them were invisible to prospecting, to depletion and
to the miner ladder. This closes all four:

- **Vanilla and Create** die by construction: Terra's palette biomes are authored by
  scripts/build-terra-worldgen.py with an empty `underground_ores` step, and they are
  deliberately *not* members of `#minecraft:is_overworld`, which is the tag every biome
  modifier in the jar set targets.
- **Mekanism** ships its own config toggles; they are flipped here.
- **GregTech** keeps four veins. The cut ones lose their `kubejs/data` override and the mod's
  originals are filtered out by packs/remove-terra-cut-veins.

The survivors move into a shallow band above bedrock, because ADR-0019 retires the deepslate
layer on Terra and a flattened world has no cliff face or cave wall left to expose ore.
"""

import json
import os
import re

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
VEINS = os.path.join(ROOT, "kubejs", "data", "gtceu", "gtceu", "ore_vein")
PF = os.path.join(ROOT, "kubejs", "data", "planetaryfactory")
FILTER_PACK = os.path.join(ROOT, "packs", "remove-terra-cut-veins")

BIOME_TAG = "#planetaryfactory:terra"

# The surviving set, and the band each lands in. The column is 0..192 with bedrock at 0..5 and
# the surface near 63, so everything here sits under the player's feet and above the floor.
# Coal is shallowest because it is the first thing the arc asks for; uranium is deepest and
# rarest because nothing before the reactor wants it.
KEEP = {
    "coal":    {"min_y": 30, "max_y": 55, "weight": 130},
    "iron":    {"min_y": 20, "max_y": 48, "weight": 160},
    "copper":  {"min_y": 20, "max_y": 48, "weight": 140},
}

# GregTech ships no overworld uranium vein -- its uranium is `pitchblende`, an End vein that
# packs/remove-nether-end-worldgen already blocks. Rather than unblock an End vein and drag its
# dimension filter around, Terra gets its own, built from pitchblende's generator.
URANIUM_SOURCE = "pitchblende"
URANIUM = {"min_y": 6, "max_y": 24, "weight": 40}

MEK_WORLD = os.path.join(ROOT, "config", "Mekanism", "world.toml")


def load(name):
    with open(os.path.join(VEINS, name + ".json")) as fh:
        return json.load(fh)


def save(name, obj):
    with open(os.path.join(VEINS, name + ".json"), "w") as fh:
        json.dump(obj, fh, indent=4, sort_keys=True)
        fh.write("\n")
    print("wrote kubejs/data/gtceu/gtceu/ore_vein/%s.json" % name)


def reband(vein, band):
    """Move a vein into its band. The generator carries its own min_y/max_y and they must
    agree with height_range, or the vein places outside the band it advertises."""
    vein["biomes"] = BIOME_TAG
    vein["dimension_filter"] = ["minecraft:overworld"]
    vein["layer"] = "stone"  # the deepslate layer is retired on Terra (ADR-0019)
    vein["weight"] = band["weight"]
    vein["height_range"] = {
        "height": {
            "type": "minecraft:uniform",
            "min_inclusive": {"absolute": band["min_y"]},
            "max_inclusive": {"absolute": band["max_y"]},
        }
    }
    gen = vein["generator"]
    if "min_y" in gen or "max_y" in gen:
        gen["min_y"] = band["min_y"]
        gen["max_y"] = band["max_y"]
    return vein


def main():
    existing = {f[:-5] for f in os.listdir(VEINS) if f.endswith(".json")}
    cut = sorted(existing - set(KEEP))

    for name, band in KEEP.items():
        save(name, reband(load(name), band))

    # Terra's uranium, from the End vein's generator.
    import zipfile
    jar = next(
        os.path.join(ROOT, "mods", f)
        for f in os.listdir(os.path.join(ROOT, "mods"))
        if re.match(r"gtceu-.*\.jar$", f)
    )
    with zipfile.ZipFile(jar) as z:
        src = json.loads(z.read("data/gtceu/gtceu/ore_vein/%s.json" % URANIUM_SOURCE))
    save("uranium", reband(src, URANIUM))

    for name in cut:
        os.remove(os.path.join(VEINS, name + ".json"))
        print("removed override kubejs/data/gtceu/gtceu/ore_vein/%s.json" % name)

    os.makedirs(FILTER_PACK, exist_ok=True)
    with open(os.path.join(FILTER_PACK, "pack.mcmeta"), "w") as fh:
        json.dump({
            "pack": {
                "pack_format": 48,
                "description": "PlanetaryFactory: cuts Terra's ore to iron, copper, coal and uranium",
            },
            "filter": {
                "block": [
                    {"namespace": "gtceu", "path": r"^gtceu/ore_vein/%s\.json" % n} for n in cut
                ]
            },
            "_comment": [
                "ADR-0021 restricts Terra to Nauvis's resources. The kubejs/data overrides for",
                "these veins are deleted rather than emptied, so this pack blocks the mod's own",
                "files -- an emptied override would still register a vein with zero weight.",
                "gtceu:uranium is a pack vein, not a mod one, so nothing here touches it.",
            ],
        }, fh, indent=2)
        fh.write("\n")
    print("wrote packs/remove-terra-cut-veins/pack.mcmeta (%d veins blocked)" % len(cut))

    # The tag GregTech's surviving veins are scoped to. It exists because Terra's palette
    # biomes are not in #minecraft:is_overworld -- which is what keeps Create's and vanilla's
    # biome-modifier ore off the planet.
    tag = os.path.join(PF, "tags", "worldgen", "biome", "terra.json")
    os.makedirs(os.path.dirname(tag), exist_ok=True)
    with open(tag, "w") as fh:
        json.dump({
            "replace": False,
            "values": [
                "planetaryfactory:terra_grassland",
                "planetaryfactory:terra_woodland",
                "planetaryfactory:terra_dry_steppe",
                "planetaryfactory:terra_desert",
                "planetaryfactory:terra_red_desert",
                "planetaryfactory:terra_shore",
                "planetaryfactory:terra_sea",
            ],
        }, fh, indent=2)
        fh.write("\n")
    print("wrote kubejs/data/planetaryfactory/tags/worldgen/biome/terra.json")

    # Mekanism's six ore toggles.
    with open(MEK_WORLD) as fh:
        toml = fh.read()
    toml, n = re.subn(r"shouldGenerate = true", "shouldGenerate = false", toml)
    with open(MEK_WORLD, "w") as fh:
        fh.write(toml)
    print("config/Mekanism/world.toml: %d shouldGenerate toggles set false" % n)


if __name__ == "__main__":
    main()
