#!/usr/bin/env python3
"""Exercise scripts/worldgen-check.py's comparison against synthetic dumps.

The check itself needs a graphical session and eight minutes of world load. This
covers the half that decides pass or fail, so a fixture edit can be checked in
seconds and the expensive run is left to confirm what the game actually loaded.

Usage: tests/worldgen/test_compare.py
"""
import importlib.util
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent.parent
spec = importlib.util.spec_from_file_location("worldgen_check",
                                              ROOT / "scripts/worldgen-check.py")
worldgen_check = importlib.util.module_from_spec(spec)
spec.loader.exec_module(worldgen_check)
compare = worldgen_check.compare

IGNUS = "planetaryfactory:vulcanus"
ELECTRO = "planetaryfactory:fulgora"
SAPROS = "planetaryfactory:gleba"
GREEN = "planetaryfactory:gleba_green_marshland"
RED = "planetaryfactory:gleba_red_marshland"

EXPECTED = {
    "bodies": {
        "ignus": {
            "dimension": IGNUS,
            "ore_veins": {"planetaryfactory:ignus_coal": {"weight": 120,
                                                          "layer": "ignus_rock"}},
            "bedrock_ores": {"planetaryfactory:ignus_tungsten_deposit": {
                "materials": ["gtceu:tungsten"], "depleted_yield_at_least": 1}},
            "bedrock_fluids": {"gtceu:lava_deposit": {"fluid": "minecraft:lava",
                                                      "depleted_yield_at_least": 1}},
            "forbidden_ore_veins": ["gtceu:iron"],
            "forbidden_bedrock_fluids": ["gtceu:oil_deposit"],
        },
        # A body asserted barren: the empty object is the assertion, not an omission.
        "electro": {
            "dimension": ELECTRO,
            "ore_veins": {},
            "bedrock_ores": {"planetaryfactory:electro_scrap_deposit": {
                "materials": ["planetaryfactory:scrap"], "depleted_yield_at_least": 1}},
        },
        # A vanilla dimension stripped of GregTech worldgen (#16): same emptiness
        # assertion as a barren body, made against a dimension the pack does not own.
        "nether": {
            "dimension": "minecraft:the_nether",
            "ore_veins": {},
            "bedrock_ores": {},
            "bedrock_fluids": {},
        },
        # A body barren of all three, whose layer is asserted directly because no vein
        # names it, and whose biomes have to be emitted rather than merely registered.
        "sapros": {
            "dimension": SAPROS,
            "worldgen_layer": "sapros_rock",
            "biomes": [GREEN, RED],
            "ore_veins": {},
            "bedrock_ores": {},
            "bedrock_fluids": {},
        },
    }
}

GOOD = {
    "ore_veins": {
        "planetaryfactory:ignus_coal": {"weight": 120, "layer": "ignus_rock",
                                        "dimensions": [IGNUS]},
        "gtceu:iron": {"weight": 160, "layer": "stone",
                       "dimensions": ["minecraft:overworld"]},
    },
    "bedrock_ores": {
        "planetaryfactory:ignus_tungsten_deposit": {
            "materials": [{"material": "gtceu:tungsten", "weight": 5}],
            "depleted_yield": 6, "dimensions": [IGNUS]},
        "planetaryfactory:electro_scrap_deposit": {
            "materials": [{"material": "planetaryfactory:scrap", "weight": 1}],
            "depleted_yield": 12, "dimensions": [ELECTRO]},
    },
    "bedrock_fluids": {
        "gtceu:lava_deposit": {"fluid": "minecraft:lava", "depleted_yield": 30,
                               "weight": 65, "dimensions": [IGNUS]},
        "gtceu:oil_deposit": {"fluid": "gtceu:oil", "depleted_yield": 5, "weight": 105,
                              "dimensions": ["minecraft:overworld"]},
    },
    "worldgen_layers": {"ignus_rock": {"dimensions": [IGNUS]},
                        "sapros_rock": {"dimensions": [SAPROS]},
                        "stone": {"dimensions": ["minecraft:overworld"]}},
    "biomes": {SAPROS: [GREEN, RED, "planetaryfactory:gleba_marshes"],
               IGNUS: ["planetaryfactory:ignus_barren_plains"]},
}


def mutate(fn):
    dump = json.loads(json.dumps(GOOD))
    fn(dump)
    return dump


CASES = [
    ("a matching dump passes", GOOD, 0),
    ("a vein that did not load fails",
     mutate(lambda d: d["ore_veins"].pop("planetaryfactory:ignus_coal")), 1),
    ("a vein filtered to the wrong dimension fails",
     mutate(lambda d: d["ore_veins"]["planetaryfactory:ignus_coal"]
            .__setitem__("dimensions", ["minecraft:overworld"])), 1),
    # The failure no codec catches: both halves well-formed, nothing generated.
    ("a layer that does not cover the body fails",
     mutate(lambda d: d["worldgen_layers"]["ignus_rock"]
            .__setitem__("dimensions", ["gcyr:venus"])), 1),
    ("a forbidden vein leaking onto the body fails",
     mutate(lambda d: d["ore_veins"]["gtceu:iron"]["dimensions"].append(IGNUS)), 1),
    ("a bedrock ore that depletes to nothing fails",
     mutate(lambda d: d["bedrock_ores"]["planetaryfactory:ignus_tungsten_deposit"]
            .__setitem__("depleted_yield", 0)), 1),
    ("a bedrock fluid that did not load fails",
     mutate(lambda d: d["bedrock_fluids"].pop("gtceu:lava_deposit")), 1),
    ("a bedrock fluid holding the wrong fluid fails",
     mutate(lambda d: d["bedrock_fluids"]["gtceu:lava_deposit"]
            .__setitem__("fluid", "minecraft:water")), 1),
    ("a bedrock fluid still reaching the wrong dimension fails",
     mutate(lambda d: d["bedrock_fluids"]["gtceu:oil_deposit"]["dimensions"]
            .append(IGNUS)), 1),
    # A barren body's emptiness is the assertion, and it has to hold against veins
    # nobody thought to forbid — including ones added to the pack years from now.
    ("any vein reaching a body asserted barren fails",
     mutate(lambda d: d["ore_veins"]["gtceu:iron"]["dimensions"].append(ELECTRO)), 1),
    ("a vein this pack adds later, reaching a barren body, fails",
     mutate(lambda d: d["ore_veins"].__setitem__(
         "planetaryfactory:electro_scrap", {"weight": 80, "layer": "electro_rock",
                                            "dimensions": [ELECTRO]})), 1),
    # An empty dimension_filter is GregTech's "nowhere", and the barren walk has to read
    # it the same way rather than as "everywhere" or as an entry it may skip.
    ("a vein with an empty dimension filter leaves a barren body barren",
     mutate(lambda d: d["ore_veins"].__setitem__(
         "planetaryfactory:unfiltered", {"weight": 1, "layer": "sapros_rock",
                                         "dimensions": []})), 0),
    ("a bedrock ore deposit reaching a body asserted to have none fails",
     mutate(lambda d: d["bedrock_ores"]["planetaryfactory:electro_scrap_deposit"]
            ["dimensions"].append(SAPROS)), 1),
    ("a bedrock fluid deposit reaching a body asserted to have none fails",
     mutate(lambda d: d["bedrock_fluids"]["gtceu:oil_deposit"]["dimensions"]
            .append(SAPROS)), 1),
    # A layer nothing references is invisible until someone prospects the body.
    ("a layer that did not load fails",
     mutate(lambda d: d["worldgen_layers"].pop("sapros_rock")), 1),
    ("a layer scoped away from its body fails",
     mutate(lambda d: d["worldgen_layers"]["sapros_rock"]
            .__setitem__("dimensions", ["gcyr:mercury"])), 1),
    # The failure that made this fixture grow biomes: a biome that parses, is listed in the
    # biome source, and is closest to no point in the noise space, so it generates nowhere.
    ("a biome the generator never emits fails",
     mutate(lambda d: d["biomes"][SAPROS].remove(RED)), 1),
    ("a dimension with no biome sample at all fails",
     mutate(lambda d: d["biomes"].pop(SAPROS)), 2),
    # #16: the stock Nether veins and the Nether natural gas deposit are filtered out,
    # and nothing may put them back.
    ("a stock vein still reaching the Nether fails",
     mutate(lambda d: d["ore_veins"].__setitem__(
         "gtceu:sulfur", {"weight": 100, "layer": "netherrack",
                          "dimensions": ["minecraft:the_nether"]})), 1),
    ("a bedrock fluid deposit still reaching the Nether fails",
     mutate(lambda d: d["bedrock_fluids"].__setitem__(
         "gtceu:nether_natural_gas_deposit",
         {"fluid": "gtceu:natural_gas", "depleted_yield": 3, "weight": 15,
          "dimensions": ["minecraft:the_nether"]})), 1),
    ("a barren body with veins elsewhere in the registry passes",
     mutate(lambda d: d["ore_veins"].__setitem__(
         "planetaryfactory:ignus_sulfur", {"weight": 100, "layer": "ignus_rock",
                                           "dimensions": [IGNUS]})), 0),
]


def main():
    failed = 0
    for name, dump, want in CASES:
        got = list(compare(dump, EXPECTED))
        if len(got) != want:
            failed += 1
            print(f"FAIL {name}: expected {want} failure(s), got {len(got)}: {got}")
        else:
            print(f"ok   {name}")

    # The shipped fixture has to be readable by the same code that consumes it.
    json.loads((ROOT / "tests/worldgen/expected.json").read_text())
    print("ok   tests/worldgen/expected.json parses")

    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
