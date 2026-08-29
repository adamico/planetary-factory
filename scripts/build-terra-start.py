#!/usr/bin/env python3
"""Build Terra's spawn-anchored starting area (ADR-0019, issue #84).

ADR-0019 puts surface ore fields *only* in the starting area, and makes that area "a
spawn-anchored structure with a fixed resource set and a randomized layout and patch sizes".
#59 built the flat world and re-banded the four veins into a band 20..55, which left a new
world with no reachable ore at all: this is the opening that fixes that.

Three decisions this script encodes, each argued in `docs/adr/0019-*.md` and in issue #84:

**The patches are ordinary ore blocks, not a GregTech vein.** ADR-0020 already settled that
depletion *is* physical block removal -- there is no depleted flag on a GT vein to set, and
a GT Miner scans for ore blocks rather than consulting the vein registry, so the miner ladder
sees these patches exactly as it sees a prospected one. A vein also cannot be spawn-anchored:
GregTech places veins on its own grid (size 6, random offset 24, per ADR-0019's amendment),
and nothing in that placement can be told "one, here".

**Anchoring is `minecraft:concentric_rings` at distance 0, count 1.** That is the only vanilla
placement type that puts a bounded number of a structure near the world origin instead of on a
spread grid, and vanilla's own spawn search starts from the origin on a world whose land biomes
are everywhere -- so the player and the structure arrive in the same place.

**Randomization is jigsaw, not noise.** The hub carries one connector per resource, each
pointing at that resource's own single-purpose pool. One pool per resource is what makes the
*set* fixed -- a shared pool would happily deal three copper patches and no coal -- while the
size variants inside each pool, the three hub variants and vanilla's rotation of both give the
layout its variety.

Patch sizing is anchored on ADR-0020's own figure: a small surface patch worked by hand empties
in about an hour. A mid-size draw here is ~1150 ore blocks across the three patches, which at a
couple of seconds a block is about that. It is a tuning number, not a discrete choice.

Run from anywhere; writes into `kubejs/data/planetaryfactory/`.
"""

import json
import math
import os
import random
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import nbt  # noqa: E402

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
PF = os.path.join(ROOT, "kubejs", "data", "planetaryfactory")
STRUCTURES = os.path.join(PF, "structure")
WORLDGEN = os.path.join(PF, "worldgen")

DATA_VERSION = 3955  # 1.21.1, the version electro_ruin_1.nbt already carries.

# The patch outlines are generated, so they need a seed to be reproducible: rerunning this
# script must not churn nine binary files for no reason. The variety a player sees comes from
# the jigsaw at world generation, not from rerunning this.
SEED = 20260829

# The ore blocks each patch is made of, and their weights.
#
# These mirror `kubejs/data/gtceu/gtceu/ore_vein/{iron,copper,coal}.json` -- the starting patch
# of a resource has to look like the buried vein of the same resource, or the player learns the
# wrong rock. The vein files name *materials* (`gtceu:goethite`) because GregTech resolves a
# material to the block for the layer it is generating in; a structure template needs the block
# id itself, and Terra's surface stone layer makes that `gtceu:<material>_ore`.
PATCHES = {
    "iron": {
        "blocks": [("gtceu:goethite_ore", 5), ("gtceu:yellow_limonite_ore", 2),
                   ("gtceu:hematite_ore", 2), ("gtceu:malachite_ore", 1)],
        "radii": [10, 12, 14],
        "facing": "east",
    },
    "copper": {
        "blocks": [("gtceu:chalcopyrite_ore", 5), ("gtceu:iron_ore", 2),
                   ("gtceu:pyrite_ore", 2), ("gtceu:copper_ore", 2)],
        "radii": [9, 10, 12],
        "facing": "north",
    },
    "coal": {
        "blocks": [("gtceu:coal_ore", 1)],
        "radii": [9, 11, 13],
        "facing": "west",
    },
}

SIZES = ["small", "medium", "large"]

# How far a patch's centre sits from the hub, per size variant. Far enough that the three
# patches read as separate fields rather than one blob, near enough that the whole opening is
# walkable before the player has a road. Bounded by the structure's max_distance_from_center.
DISTANCES = [34, 48, 62]

# Terra's land biomes. The sea and the shore are excluded: a starting area under water is the
# one layout the fixed-set promise cannot survive.
LAND_BIOMES = [
    "planetaryfactory:terra_desert",
    "planetaryfactory:terra_dry_steppe",
    "planetaryfactory:terra_grassland",
    "planetaryfactory:terra_red_desert",
    "planetaryfactory:terra_woodland",
]

# Jigsaw orientations are `<front>_<top>`; every connector here is horizontal, so the top is up.
OPPOSITE = {"east": "west", "west": "east", "north": "south", "south": "north"}


def jigsaw_block(name, target, pool, facing):
    """A jigsaw block, palette entry and block entity together.

    `final_state` is air: the connector is scaffolding, and the patch it attaches has to be
    the only thing the player finds.
    """
    return (
        {"Name": "minecraft:jigsaw", "Properties": {"orientation": facing + "_up"}},
        {
            "id": "minecraft:jigsaw",
            "name": name,
            "target": target,
            "pool": pool,
            "final_state": "minecraft:air",
            "joint": "aligned",
            "placement_priority": 0,
            "selection_priority": 0,
        },
    )


def disc(rng, radius):
    """A patch outline: a disc whose edge wanders, so it reads as a field rather than a token.

    The wander is a low-frequency sum of two sines with random phase -- enough to break the
    circle at a distance, not so much that the patch stops reading as one shape.
    """
    phase_a, phase_b = rng.uniform(0, math.tau), rng.uniform(0, math.tau)
    cells = []
    span = radius + 3
    for dx in range(-span, span + 1):
        for dz in range(-span, span + 1):
            distance = math.hypot(dx, dz)
            if distance < 0.5:
                cells.append((dx, dz))
                continue
            angle = math.atan2(dz, dx)
            edge = radius * (1.0 + 0.16 * math.sin(2 * angle + phase_a)
                             + 0.09 * math.sin(3 * angle + phase_b))
            if distance <= edge:
                cells.append((dx, dz))
    return cells


def weighted_pick(rng, blocks):
    total = sum(weight for _, weight in blocks)
    roll = rng.uniform(0, total)
    for block, weight in blocks:
        roll -= weight
        if roll <= 0:
            return block
    return blocks[-1][0]


def write_template(path, size, palette, blocks):
    nbt.write(path, {
        "DataVersion": nbt.Int(DATA_VERSION),
        "size": [nbt.Int(n) for n in size],
        "palette": palette,
        "blocks": blocks,
        "entities": [],
    })
    print("wrote %s (%d blocks)" % (os.path.relpath(path, ROOT), len(blocks)))


def build_patch(rng, resource, size_name, radius, distance):
    """One patch template: a connector at the west edge, the ore field `distance` blocks east.

    The template is one block tall. `terrain_matching` projection drops each column onto the
    heightmap, and the piece itself is projected to `WORLD_SURFACE_WG`, so y=0 is the air just
    above the ground: the field lies *on* the surface. That is the Factorio reading ADR-0019
    asks for -- a patch you see the outline of and plan a miner over -- and it is also what
    makes the field legible after half of it has been dug.
    """
    spec = PATCHES[resource]
    cells = disc(rng, radius)
    span = radius + 3
    # The connector sits at x=0; the field's centre at x=distance. The template is padded to
    # hold both, with the field's own span either side in z.
    centre_x, centre_z = distance, span
    width, depth = distance + span + 1, 2 * span + 1

    palette_index = {}
    palette = []
    blocks = []

    def state_of(entry):
        key = json.dumps(entry, sort_keys=True)
        if key not in palette_index:
            palette_index[key] = len(palette)
            palette.append(entry)
        return palette_index[key]

    connector, connector_nbt = jigsaw_block(
        "planetaryfactory:terra_start_patch",
        "planetaryfactory:terra_start_hub",
        "minecraft:empty",
        OPPOSITE[spec["facing"]],
    )
    blocks.append({
        "pos": [nbt.Int(0), nbt.Int(0), nbt.Int(centre_z)],
        "state": nbt.Int(state_of(connector)),
        "nbt": connector_nbt,
    })

    for dx, dz in cells:
        x, z = centre_x + dx, centre_z + dz
        if x == 0 and z == centre_z:
            continue  # never bury the connector
        blocks.append({
            "pos": [nbt.Int(x), nbt.Int(0), nbt.Int(z)],
            "state": nbt.Int(state_of({"Name": weighted_pick(rng, spec["blocks"])})),
        })

    write_template(
        os.path.join(STRUCTURES, "terra_start_%s_%s.nbt" % (resource, size_name)),
        (width, 1, depth), palette, blocks)


def build_hub(rng, index, offsets):
    """One hub variant: three connectors, one per resource, at scattered positions.

    The hub places no block of its own. Its whole job is to hold the three connectors far
    enough apart, and at different enough offsets, that the three fields do not land on a
    fixed triangle every world.
    """
    palette = []
    palette_index = {}
    blocks = []

    def state_of(entry):
        key = json.dumps(entry, sort_keys=True)
        if key not in palette_index:
            palette_index[key] = len(palette)
            palette.append(entry)
        return palette_index[key]

    span = max(max(abs(x), abs(z)) for x, z in offsets.values())
    for resource, (dx, dz) in offsets.items():
        block, block_nbt = jigsaw_block(
            "planetaryfactory:terra_start_hub",
            "planetaryfactory:terra_start_patch",
            "planetaryfactory:terra_start_" + resource,
            PATCHES[resource]["facing"],
        )
        blocks.append({
            "pos": [nbt.Int(span + dx), nbt.Int(0), nbt.Int(span + dz)],
            "state": nbt.Int(state_of(block)),
            "nbt": block_nbt,
        })

    write_template(os.path.join(STRUCTURES, "terra_start_hub_%d.nbt" % index),
                   (2 * span + 1, 1, 2 * span + 1), palette, blocks)


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as handle:
        json.dump(obj, handle, indent=2, sort_keys=True)
        handle.write("\n")
    print("wrote %s" % os.path.relpath(path, ROOT))


def build_datapack(hub_count):
    write_json(os.path.join(PF, "tags", "worldgen", "biome", "terra_land.json"), {
        "_comment": "Generated by scripts/build-terra-start.py. Terra's land biomes, which is a "
                    "different question from `#planetaryfactory:terra` -- that one is the vein "
                    "tag and includes the sea and the shore.",
        "replace": False,
        "values": LAND_BIOMES,
    })

    write_json(os.path.join(WORLDGEN, "template_pool", "terra_start.json"), {
        "_comment": "Generated by scripts/build-terra-start.py. Hub variants: they place no "
                    "block, only the three connectors that decide where the fields land.",
        "fallback": "minecraft:empty",
        "elements": [
            {
                "weight": 1,
                "element": {
                    "element_type": "minecraft:single_pool_element",
                    "location": "planetaryfactory:terra_start_hub_%d" % index,
                    "projection": "rigid",
                    "processors": "minecraft:empty",
                },
            }
            for index in range(hub_count)
        ],
    })

    for resource in PATCHES:
        write_json(os.path.join(WORLDGEN, "template_pool", "terra_start_%s.json" % resource), {
            "_comment": "Generated by scripts/build-terra-start.py. One pool per resource is "
                        "what makes ADR-0019's resource set fixed: a shared pool would deal "
                        "three of one ore and none of another. The size variants inside it are "
                        "what makes the patch sizes randomized.",
            "fallback": "minecraft:empty",
            "elements": [
                {
                    "weight": 1,
                    "element": {
                        "element_type": "minecraft:single_pool_element",
                        "location": "planetaryfactory:terra_start_%s_%s" % (resource, size),
                        # The field lies on the ground, so every column follows the heightmap.
                        "projection": "terrain_matching",
                        "processors": "minecraft:empty",
                    },
                }
                for size in SIZES
            ],
        })

    write_json(os.path.join(WORLDGEN, "structure", "terra_starting_area.json"), {
        "type": "minecraft:jigsaw",
        "biomes": LAND_BIOMES,
        "step": "surface_structures",
        "spawn_overrides": {},
        # The fields sit on the surface and must not be bearded into a plinth.
        "terrain_adaptation": "none",
        "start_pool": "planetaryfactory:terra_start",
        # One level of expansion: the hub, then its three patches. Nothing expands further.
        "size": 1,
        "start_height": {"absolute": 0},
        "project_start_to_heightmap": "WORLD_SURFACE_WG",
        # Must clear the furthest patch centre plus its own radius.
        "max_distance_from_center": 112,
        "use_expansion_hack": False,
    })

    write_json(os.path.join(WORLDGEN, "structure_set", "terra_starting_area.json"), {
        "_comment": "concentric_rings at distance 0, count 1: exactly one starting area, at the "
                    "origin, where vanilla's own spawn search begins. random_spread cannot "
                    "express 'one, here', and ADR-0019 wants the opening anchored to spawn.",
        "structures": [
            {"structure": "planetaryfactory:terra_starting_area", "weight": 1},
        ],
        "placement": {
            "type": "minecraft:concentric_rings",
            "distance": 0,
            "spread": 1,
            "count": 1,
            # Not `#planetaryfactory:terra`: that tag is the *vein* tag and holds the sea and
            # the shore, and concentric_rings picks its ring position by preferred biome. A
            # starting area chosen into open water is the one layout the fixed-set promise
            # cannot survive, so the placement gets a land-only tag of its own.
            "preferred_biomes": "#planetaryfactory:terra_land",
            "salt": 508113774,
        },
    })


def main():
    rng = random.Random(SEED)
    os.makedirs(STRUCTURES, exist_ok=True)

    for resource, spec in PATCHES.items():
        for size, radius, distance in zip(SIZES, spec["radii"], DISTANCES):
            build_patch(rng, resource, size, radius, distance)

    # Three hubs, each spreading the connectors differently, so the fields do not sit on the
    # same triangle in every world.
    hub_offsets = [
        {"iron": (0, 0), "copper": (-6, 4), "coal": (2, -7)},
        {"iron": (3, -5), "copper": (0, 0), "coal": (-8, 2)},
        {"iron": (-4, 6), "copper": (7, 1), "coal": (0, 0)},
    ]
    for index, offsets in enumerate(hub_offsets):
        build_hub(rng, index, offsets)

    build_datapack(len(hub_offsets))


if __name__ == "__main__":
    main()
