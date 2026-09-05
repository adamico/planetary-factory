#!/usr/bin/env python3
"""Cut Terra's ore to Factorio's set and re-band the survivors (ADR-0019/0021, issue #59).

Terra had four ore systems and three of them were invisible to prospecting, to depletion and
to the miner ladder. This closes the three that remain -- Mekanism's left with the mod
(ADR-0035), and its six worldgen toggles with it:

- **Vanilla and Create** die by construction: Terra's palette biomes are authored by
  scripts/build-terra-worldgen.py with an empty `underground_ores` step, and they are
  deliberately *not* members of `#minecraft:is_overworld`, which is the tag every biome
  modifier in the jar set targets.
- **GregTech** keeps four veins. The cut ones lose their `kubejs/data` override and the mod's
  originals are filtered out by packs/remove-terra-cut-veins.

The survivors move into a shallow band above bedrock, because ADR-0019 retires the deepslate
layer on Terra and a flattened world has no cliff face or cave wall left to expose ore.
"""

import json
import os
import re
import zipfile

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

# The block each vein deals, which since ADR-0041 is the pack's rather than GregTech's. A vein
# layer's `targets` is `Either<List<TargetBlockState>, Material>` in GregTech's own codec, so a
# block state is expressible and a material is not the only option -- which is what lets an
# outfield vein carry an amount exactly as a starting field does. The ore blocks still drop
# GregTech's raw ore, so the vein is a vein in every way the rest of the pack can observe.
PACK_ORE = {
    "coal": "planetaryfactory:coal_ore",
    "iron": "planetaryfactory:iron_ore",
    "copper": "planetaryfactory:copper_ore",
    "uranium": "planetaryfactory:uranium_ore",
    "stone": "planetaryfactory:stone_ore",
}

# Stone's outfield vein (ADR-0041). ADR-0021 ruled stone ambient terrain; that is reversed, and a
# metered patch that stops being metered once the starting field is gone is exactly the
# inconsistency the reversal exists to avoid. Shallow and common: it is the bulk feedstock, and
# nothing before it competes for the band.
STONE = {"min_y": 30, "max_y": 58, "weight": 150}

# What a vein replaces. Vanilla's own stone-ore predicate, which is what every GregTech stone-layer
# vein already resolves a material against.
STONE_REPLACEABLES = {
    "predicate_type": "minecraft:tag_match",
    "tag": "minecraft:stone_ore_replaceables",
}

# GregTech ships no overworld uranium vein -- its uranium is `pitchblende`, an End vein that
# packs/remove-nether-end-worldgen already blocks. Rather than unblock an End vein and drag its
# dimension filter around, Terra gets its own, built from pitchblende's generator.
URANIUM_SOURCE = "pitchblende"
URANIUM = {"min_y": 6, "max_y": 24, "weight": 40}


def load(name):
    with open(os.path.join(VEINS, name + ".json")) as fh:
        return json.load(fh)


def save(name, obj):
    with open(os.path.join(VEINS, name + ".json"), "w") as fh:
        json.dump(obj, fh, indent=4, sort_keys=True)
        fh.write("\n")
    print("wrote kubejs/data/gtceu/gtceu/ore_vein/%s.json" % name)


def retarget(vein, name):
    """Point every layer of a vein at the pack's ore block instead of a GregTech material.

    The surface indicator is left alone: it is the rock scattered on top as a prospecting hint,
    not the ore, and GregTech's own is what a player has learned to recognise.
    """
    block = PACK_ORE[name]
    generator = vein.get("generator") or {}
    for pattern in generator.get("layer_patterns") or []:
        for layer in pattern:
            layer["targets"] = [[{"target": STONE_REPLACEABLES, "state": {"Name": block}}]]
    return vein


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


def gtceu_jar():
    return next(
        os.path.join(ROOT, "mods", f)
        for f in os.listdir(os.path.join(ROOT, "mods"))
        if re.match(r"gtceu-.*\.jar$", f)
    )


def mod_veins(jar):
    """Every vein GregTech ships -- the authority on what there is to cut.

    Deliberately **not the contents of `VEINS`**. That directory is this script's own output and
    the script deletes the cut overrides out of it, so deriving the cut list from it works exactly
    once and then reads back an empty set -- silently emptying the filter pack and letting every
    vein ADR-0021 cut return to Terra. The jar cannot go stale that way: it is the thing being cut.

    The whole jar is in scope, not just the veins declaring `minecraft:overworld`. Blocking a vein
    file removes it from every dimension, which would matter if the pack had a Nether or an End to
    strip it from. It has neither, so a vein that only generates there generates nowhere, and the
    narrower reading would only be bookkeeping about dimensions nobody can visit.
    """
    prefix = "data/gtceu/gtceu/ore_vein/"
    with zipfile.ZipFile(jar) as z:
        return {
            name[len(prefix):-5]
            for name in z.namelist()
            if name.startswith(prefix) and name.endswith(".json")
        }


def main():
    jar = gtceu_jar()
    # `uranium` and `stone` are pack veins written below rather than mod overrides, so neither is
    # a cut: removing them would delete the file this script is in the middle of writing.
    cut = sorted(mod_veins(jar) - set(KEEP) - {"uranium", "stone"})

    for name, band in KEEP.items():
        save(name, retarget(reband(load(name), band), name))

    # Terra's uranium, from the End vein's generator.
    with zipfile.ZipFile(jar) as z:
        src = json.loads(z.read("data/gtceu/gtceu/ore_vein/%s.json" % URANIUM_SOURCE))
    save("uranium", retarget(reband(src, URANIUM), "uranium"))

    # Stone's own vein, built from coal's generator: the same shallow layer shape, dealing the
    # stone ore block. Nothing in GregTech ships a stone vein to start from, because stone is not
    # one of its materials -- which is the shape of ADR-0041's amendment to ADR-0021.
    #
    # It gets its own surface indicator rather than inheriting coal's: `gtceu:coal` scattered over
    # a stone vein would be a prospecting hint that names the wrong resource, and *no* indicator
    # would quietly exempt stone from ADR-0019's rule that ore is prospected rather than stumbled
    # on. Loose cobblestone is the honest hint, and it is legible precisely because ADR-0019 caps
    # Terra in dirt: rock lying on soil means rock underneath.
    stone = retarget(reband(load("coal"), STONE), "stone")
    stone["indicators"] = [{
        # A bare string here is read as a GregTech *material* -- which is what `gtceu:coal` is in
        # the vein we copied from -- and there is no cobblestone material, so the vein failed to
        # parse and took registry loading down with it at world creation. The object form is the
        # other side of the codec's `Either`: a block state, which is what we actually want.
        "block": {"Name": "minecraft:cobblestone"},
        "density": 0.2,
        "placement": "surface",
        "radius": 5,
        "type": "gtceu:surface",
    }]
    save("stone", stone)

    for name in cut:
        override = os.path.join(VEINS, name + ".json")
        if os.path.exists(override):
            os.remove(override)
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


if __name__ == "__main__":
    main()
