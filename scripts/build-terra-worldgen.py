#!/usr/bin/env python3
"""Emit Terra's flat, cave-free worldgen (ADR-0019, issue #59).

Terra is the vanilla Overworld, so every file here is a wholesale replacement of a
`minecraft:` entry rather than an addition -- the first the pack ships. It lands in
`kubejs/data/`, not the instance-root `datapacks/` the ADR names: that folder is read by
nothing in this jar set (no OpenLoader), whereas KubeJS's data folder already overrides
GregTech's own ore-vein files and is therefore the proven seam.

Re-run after editing the palette or the terrain constants; it overwrites its outputs.
"""

import json
import os

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
MC = os.path.join(ROOT, "kubejs", "data", "minecraft")
PF = os.path.join(ROOT, "kubejs", "data", "planetaryfactory")
SAPROS = os.path.join(PF, "worldgen", "noise_settings", "sapros.json")

# The column. Both the dimension type and the noise settings' own `noise` block carry these;
# a disagreement writes outside the chunk's section array.
MIN_Y, HEIGHT, SEA_LEVEL = 0, 192, 63

# The terrain. Surface sits where final_density crosses zero:
#   y = GRAD_LO + (GRAD_HI - GRAD_LO) / 2 * (1 + noise)
# so with RELIEF 0.55 the ground runs y 60..74 and dips under sea level often enough that
# Terra actually has water -- which ADR-0019 makes a requirement, not a detail, because
# vanilla 1.21 overworld biomes ship no water-lake feature at all.
GRAD_LO, GRAD_HI = 55, 79
RELIEF = 0.55
CLIFF_LIFT = 0.6  # the rare steep segment: landmarks, not roughness


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as fh:
        json.dump(obj, fh, indent=2)
        fh.write("\n")
    print("wrote", os.path.relpath(path, ROOT))


def noise(name, xz, y=0.0):
    return {"type": "minecraft:noise", "noise": name, "xz_scale": xz, "y_scale": y}


def mul(a, b):
    return {"type": "minecraft:mul", "argument1": a, "argument2": b}


def add(a, b):
    return {"type": "minecraft:add", "argument1": a, "argument2": b}


# --- density functions -------------------------------------------------------------

# `offset` carries the relief; the cliff term is a narrow range of the erosion noise that
# lifts the ground wholesale, so the transition reads as a wall rather than a slope.
CLIFF = {
    "type": "minecraft:range_choice",
    "input": noise("minecraft:erosion", 0.18),
    "min_inclusive": 0.62,
    "max_exclusive": 1.0,
    "when_in_range": CLIFF_LIFT,
    "when_out_of_range": 0.0,
}

OFFSET = add(mul(RELIEF, noise("minecraft:continentalness", 0.22)), CLIFF)
FACTOR = 6.0          # large constant: the spline is shallow, so nothing amplifies it
JAGGEDNESS = 0.0

GRADIENT = {
    "type": "minecraft:y_clamped_gradient",
    "from_y": GRAD_LO,
    "to_y": GRAD_HI,
    "from_value": 1.0,
    "to_value": -1.0,
}


def build_noise_settings():
    """Terra's noise settings, from Sapros's -- the pack's proven overworld-shaped router.

    Four things change: the column, aquifers off (the fluid picker degenerates to a flat
    global water table, which is what a cave-free world wants), stone as the default block,
    and `final_density` replaced wholesale. The vanilla `overworld/caves/*` functions are
    left in the registry and simply referenced by nothing -- ADR-0019 is explicit that the
    cave tree is not edited.
    """
    d = json.load(open(SAPROS))
    d["_comment"] = [
        "Terra's terrain (ADR-0019). Flat by construction: final_density is a y gradient plus",
        "a shallow relief noise, so the vanilla cave tree is referenced by nothing rather than",
        "edited. Aquifers are off; all surface water comes from terrain dipping below sea level,",
        "which is why RELIEF in scripts/build-terra-worldgen.py is load-bearing. Generated --",
        "edit the script, not this file.",
    ]
    d["aquifers_enabled"] = False
    d["ore_veins_enabled"] = False
    d["default_block"] = {"Name": "minecraft:stone"}
    d["default_fluid"] = {"Name": "minecraft:water", "Properties": {"level": "0"}}
    d["sea_level"] = SEA_LEVEL
    d["noise"] = {
        "min_y": MIN_Y,
        "height": HEIGHT,
        "size_horizontal": 1,
        "size_vertical": 2,
    }

    r = d["noise_router"]
    # depth is the one vanilla term that hardcodes the old column; retuned with it.
    r["depth"] = {
        "type": "minecraft:y_clamped_gradient",
        "from_y": MIN_Y,
        "to_y": MIN_Y + HEIGHT,
        "from_value": 1.5,
        "to_value": -1.5,
    }
    r["final_density"] = add(GRADIENT, OFFSET)
    r["initial_density_without_jaggedness"] = add(GRADIENT, mul(RELIEF, noise("minecraft:continentalness", 0.22)))
    # Ore veins are the GregTech vein system's job on Terra, not the router's.
    r["vein_toggle"] = 0.0
    r["vein_ridged"] = 0.0
    r["vein_gap"] = 0.0
    d["surface_rule"] = SURFACE_RULE
    d["spawn_target"] = [
        {
            "temperature": [-1.0, 1.0],
            "humidity": [-1.0, 1.0],
            "continentalness": [0.1, 1.0],
            "erosion": [-1.0, 0.5],
            "weirdness": [-1.0, 1.0],
            "depth": 0.0,
            "offset": 0.0,
        }
    ]
    return d


# --- the palette -------------------------------------------------------------------

# Six biomes plus a sea. Nauvis-like, no cold biome: Terra is home and reads as one
# coherent world, and contrast belongs on Sapros and Ignus (ADR-0019).
PALETTE = [
    # Six biomes plus a sea, laid out along EROSION rather than along temperature and humidity.
    # Two earlier attempts failed the emitted check: sat 0.1 apart on temp/hum the deserts
    # collapsed into one another, and pushed apart on continentalness the red desert asked for a
    # far-inland corner the router's `continents` spline does not reach. Erosion is the axis that
    # genuinely spans its range across Terra, so the palette reads as one gradient -- woodland
    # where the ground is uneroded, desert where it is worn to nothing -- and every entry has a
    # region it wins. Temperature and humidity are held at 0 so they cannot re-crowd it; the
    # biomes' own `temperature`/`downfall` fields, which drive rain and mob rules, are separate.
    # name              temp  hum   cont   eros   top block             rain  temp_val
    ("terra_woodland",   0.0,  0.0,  0.30, -0.70, "minecraft:grass_block", True, 0.7),
    ("terra_grassland",  0.0,  0.0,  0.30, -0.20, "minecraft:grass_block", True, 0.8),
    ("terra_dry_steppe", 0.0,  0.0,  0.30,  0.35, "minecraft:grass_block", False, 1.2),
    ("terra_desert",     0.0,  0.0,  0.30,  0.85, "minecraft:sand",        False, 2.0),
    ("terra_red_desert", 0.0,  0.0,  0.55,  0.10, "minecraft:red_sand",    False, 2.0),
    ("terra_shore",      0.0,  0.0, -0.15,  0.40, "minecraft:sand",        True, 0.8),
    ("terra_sea",        0.0,  0.0, -0.80,  0.20, "minecraft:gravel",      True, 0.7),
]

VEGETATION = {
    "terra_grassland": ["minecraft:trees_plains", "minecraft:flower_plains", "minecraft:patch_grass_plain"],
    "terra_woodland": ["minecraft:trees_birch_and_oak", "minecraft:flower_default", "minecraft:patch_grass_forest"],
    "terra_dry_steppe": ["minecraft:trees_savanna", "minecraft:patch_grass_savanna", "minecraft:patch_dead_bush"],
    "terra_desert": ["minecraft:patch_cactus_desert", "minecraft:patch_dead_bush_2", "minecraft:patch_sugar_cane_desert"],
    "terra_red_desert": ["minecraft:patch_dead_bush_badlands", "minecraft:patch_cactus_decorated"],
    "terra_shore": ["minecraft:patch_sugar_cane", "minecraft:patch_grass_plain"],
    "terra_sea": ["minecraft:seagrass_simple", "minecraft:kelp_warm"],
}

MONSTERS = [
    {"type": "minecraft:spider", "weight": 100, "minCount": 4, "maxCount": 4},
    {"type": "minecraft:zombie", "weight": 95, "minCount": 4, "maxCount": 4},
    {"type": "minecraft:skeleton", "weight": 100, "minCount": 4, "maxCount": 4},
    {"type": "minecraft:creeper", "weight": 100, "minCount": 4, "maxCount": 4},
    {"type": "minecraft:enderman", "weight": 10, "minCount": 1, "maxCount": 4},
    {"type": "minecraft:witch", "weight": 5, "minCount": 1, "maxCount": 1},
]
CREATURES = [
    {"type": "minecraft:sheep", "weight": 12, "minCount": 4, "maxCount": 4},
    {"type": "minecraft:pig", "weight": 10, "minCount": 4, "maxCount": 4},
    {"type": "minecraft:chicken", "weight": 10, "minCount": 4, "maxCount": 4},
    {"type": "minecraft:cow", "weight": 8, "minCount": 4, "maxCount": 4},
]

# GenerationStep.Decoration, in order. `underground_ores` is deliberately empty: Terra's ore
# is GregTech's four veins and nothing else (ADR-0021), and vanilla ore left here would be
# uncharted by prospecting and undepletable by a miner -- a straight bypass of ADR-0020.
STEPS = 11
VEGETAL, TOP_LAYER = 9, 10


def build_biome(name, temp, hum, cont, eros, top, rain, temp_val):
    features = [[] for _ in range(STEPS)]
    features[VEGETAL] = VEGETATION[name]
    return {
        "_comment": "Generated by scripts/build-terra-worldgen.py (ADR-0019). Carvers are empty by construction; underground_ores is empty because Terra's ore is GregTech's alone (ADR-0021).",
        "has_precipitation": rain,
        "temperature": temp_val,
        "downfall": 0.4 if rain else 0.0,
        "carvers": {},
        "features": features,
        "spawn_costs": {},
        "spawners": {
            "monster": [] if name == "terra_sea" else MONSTERS,
            "creature": [] if name in ("terra_sea", "terra_desert", "terra_red_desert") else CREATURES,
            "ambient": [],
            "axolotls": [],
            "underground_water_creature": [],
            "water_creature": [],
            "water_ambient": [],
            "misc": [],
        },
        "effects": {
            "fog_color": 12638463,
            "sky_color": 7907327,
            "water_color": 4159204,
            "water_fog_color": 329011,
            "mood_sound": {
                "sound": "minecraft:ambient.cave",
                "tick_delay": 6000,
                "block_search_extent": 8,
                "offset": 2.0,
            },
        },
    }


def surface_rule():
    """Top block per biome, over dirt, over stone. Bedrock at the floor.

    Deliberately thin: with no caves and no vanilla ore, the column below the surface is
    stone all the way to the GregTech vein band, and there is nothing else to express.
    """
    def biome_is(names):
        return {"type": "minecraft:biome", "biome_is": ["planetaryfactory:" + n for n in names]}

    floor = {
        "type": "minecraft:stone_depth",
        "add_surface_depth": False,
        "offset": 0,
        "secondary_depth_range": 0,
        "surface_type": "floor",
    }
    under = dict(floor, offset=3, secondary_depth_range=6)

    tops = []
    for name, _t, _h, _c, _e, top, _r, _tv in PALETTE:
        tops.append({
            "type": "minecraft:condition",
            "if_true": biome_is([name]),
            "then_run": {"type": "minecraft:block", "result_state": {"Name": top}},
        })

    grassy = [n for n, *_rest in PALETTE if _rest[4] == "minecraft:grass_block"]

    return {
        "type": "minecraft:sequence",
        "sequence": [
            {
                "type": "minecraft:condition",
                "if_true": {
                    "type": "minecraft:vertical_gradient",
                    "random_name": "minecraft:bedrock_floor",
                    "true_at_and_below": {"above_bottom": 0},
                    "false_at_and_above": {"above_bottom": 5},
                },
                "then_run": {"type": "minecraft:block", "result_state": {"Name": "minecraft:bedrock"}},
            },
            {
                "type": "minecraft:condition",
                "if_true": floor,
                "then_run": {
                    "type": "minecraft:sequence",
                    "sequence": [
                        # Underwater, grass would be a lie; gravel reads as a lake bed.
                        {
                            "type": "minecraft:condition",
                            "if_true": {
                                "type": "minecraft:not",
                                "invert": {
                                    "type": "minecraft:water",
                                    "offset": 0,
                                    "surface_depth_multiplier": 0,
                                    "add_stone_depth": False,
                                },
                            },
                            "then_run": {"type": "minecraft:block", "result_state": {"Name": "minecraft:gravel"}},
                        },
                    ] + tops,
                },
            },
            {
                "type": "minecraft:condition",
                "if_true": under,
                "then_run": {
                    "type": "minecraft:sequence",
                    "sequence": [
                        {
                            "type": "minecraft:condition",
                            "if_true": biome_is(["terra_desert", "terra_shore"]),
                            "then_run": {"type": "minecraft:block", "result_state": {"Name": "minecraft:sandstone"}},
                        },
                        {
                            "type": "minecraft:condition",
                            "if_true": biome_is(["terra_red_desert"]),
                            "then_run": {"type": "minecraft:block", "result_state": {"Name": "minecraft:red_sandstone"}},
                        },
                        {"type": "minecraft:block", "result_state": {"Name": "minecraft:dirt"}},
                    ],
                },
            },
        ],
    }


SURFACE_RULE = surface_rule()


def build_dimension():
    return {
        "_comment": "Terra. The palette is an explicit biomes list because multi_noise_biome_source_parameter_list is a preset-only stub whose codec accepts hardcoded names -- the same shape planetaryfactory:gleba already uses.",
        "type": "minecraft:overworld",
        "generator": {
            "type": "minecraft:noise",
            "settings": "minecraft:overworld",
            "biome_source": {
                "type": "minecraft:multi_noise",
                "biomes": [
                    {
                        "biome": "planetaryfactory:" + name,
                        "parameters": {
                            "temperature": temp,
                            "humidity": hum,
                            "continentalness": cont,
                            "erosion": eros,
                            "weirdness": 0,
                            "depth": 0,
                            "offset": 0,
                        },
                    }
                    for name, temp, hum, cont, eros, _top, _r, _tv in PALETTE
                ],
            },
        },
    }


def main():
    write(os.path.join(MC, "dimension_type", "overworld.json"), {
        "_comment": "ADR-0019: Terra's column is 0..192. These numbers must match the `noise` block in worldgen/noise_settings/overworld.json.",
        "ultrawarm": False,
        "natural": True,
        "piglin_safe": False,
        "respawn_anchor_works": False,
        "bed_works": True,
        "has_raids": True,
        "has_skylight": True,
        "has_ceiling": False,
        "effects": "minecraft:overworld",
        "coordinate_scale": 1.0,
        "ambient_light": 0.0,
        "infiniburn": "#minecraft:infiniburn_overworld",
        "min_y": MIN_Y,
        "height": HEIGHT,
        "logical_height": HEIGHT,
        "monster_spawn_block_light_limit": 0,
        "monster_spawn_light_level": {"type": "minecraft:uniform", "min_inclusive": 0, "max_inclusive": 7},
    })

    write(os.path.join(MC, "worldgen", "noise_settings", "overworld.json"), build_noise_settings())
    write(os.path.join(MC, "dimension", "overworld.json"), build_dimension())

    # Terra's three shaping functions live in the pack's own namespace, NOT as overrides of
    # `minecraft:overworld/{offset,factor,jaggedness}`. Those are shared: Sapros, Ignus and
    # Electro all point their router's `depth` at `minecraft:overworld/depth`, which is built
    # from `overworld/offset` -- so flattening it here would flatten three other bodies with
    # Terra. ADR-0019 is precedent, not policy.
    for fname, df in (("offset", OFFSET), ("factor", FACTOR), ("jaggedness", JAGGEDNESS)):
        write(os.path.join(PF, "worldgen", "density_function", "terra", fname + ".json"),
              df if isinstance(df, dict) else {"type": "minecraft:constant", "argument": df})

    # The global carver backstop. All three overworld carvers share this tag and nothing else
    # in vanilla references it, so emptying it leaves every carver -- including in modded
    # biomes nobody enumerated -- able to replace no block.
    write(os.path.join(MC, "tags", "block", "overworld_carver_replaceables.json"),
          {"replace": True, "values": []})

    # Terra offers one world type (#61). The presets still exist; they stop being offered.
    for tag in ("normal", "extended"):
        write(os.path.join(MC, "tags", "worldgen", "world_preset", tag + ".json"),
              {"replace": True, "values": ["minecraft:normal"]})

    write(os.path.join(MC, "world_preset", "normal.json"), {
        "_comment": "The create-world screen builds the overworld from the preset, not from dimension/overworld.json, so Terra's palette has to be named here too.",
        "dimensions": {
            "minecraft:overworld": build_dimension(),
            "minecraft:the_nether": {
                "type": "minecraft:the_nether",
                "generator": {
                    "type": "minecraft:noise",
                    "settings": "minecraft:nether",
                    "biome_source": {"type": "minecraft:multi_noise", "preset": "minecraft:nether"},
                },
            },
            "minecraft:the_end": {
                "type": "minecraft:the_end",
                "generator": {
                    "type": "minecraft:noise",
                    "settings": "minecraft:end",
                    "biome_source": {"type": "minecraft:the_end"},
                },
            },
        },
    })

    for entry in PALETTE:
        write(os.path.join(PF, "worldgen", "biome", entry[0] + ".json"), build_biome(*entry))


if __name__ == "__main__":
    main()
