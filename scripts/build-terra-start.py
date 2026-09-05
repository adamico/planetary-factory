#!/usr/bin/env python3
"""Build Terra's spawn-anchored starting area (ADR-0019, issue #84).

ADR-0019 puts surface ore fields *only* in the starting area, and makes that area "a
spawn-anchored structure with a fixed resource set and a randomized layout and patch sizes".
#59 built the flat world and re-banded the four veins into a band 20..55, which left a new
world with no reachable ore at all: this is the opening that fixes that.

Three decisions this script encodes, each argued in `docs/adr/0019-*.md` and in issue #84:

**The patches are pack-authored ore blocks, not GregTech's and not a GregTech vein.** ADR-0041
makes an ore block carry an amount, and GregTech models its material ore blocks at runtime --
so the blocks here are `planetaryfactory:<resource>_ore`, which carry the amount and the eight
sprite stages. They still drop GregTech's raw ore and still carry `c:ores`, which is the tag
GregTech's own Miner scans, so the miner ladder sees these patches exactly as before.

**The patch total is Factorio's and the per-block amount is a quotient.** This script writes no
amount into the templates. It writes the ore blocks; the mod counts what was actually placed at
stamp time and divides Factorio's own starting total by it (ADR-0041), because the size variant
is drawn at world generation and only the placed field knows its own block count.

**The patches are ordinary blocks, not a GregTech vein.** ADR-0020 already settled that
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

# The single ore block each patch is made of.
#
# One block per patch, and deliberately *not* the buried vein's mix. The vein files
# `kubejs/data/gtceu/gtceu/ore_vein/{iron,copper,coal}.json` deal four ore blocks each, and two
# of those veins are mixed across metals: the iron vein carries malachite, which smelts to copper,
# and the copper vein carries iron ore and pyrite, which smelt to iron. Underground that is a
# feature -- a vein is a place you learn the local rock -- but the starting patch is the tutorial,
# and a patch has to answer the question "what is this a patch of" with one word. A player who
# mines the iron field and gets copper has been taught something false about how the world is
# organised.
#
# So each field is one block: the plain ore of its own metal. The vein files name *materials*
# (`gtceu:copper`) because GregTech resolves a material to the block for the layer it is generating
# in; a structure template needs the block id itself, and Terra's surface stone layer makes that
# `gtceu:<material>_ore`.
PATCHES = {
    "iron": {
        "block": "planetaryfactory:iron_ore",
        "radii": [10, 12, 14],
        "facing": "east",
    },
    "copper": {
        "block": "planetaryfactory:copper_ore",
        "radii": [9, 10, 12],
        "facing": "north",
    },
    "coal": {
        "block": "planetaryfactory:coal_ore",
        "radii": [9, 11, 13],
        "facing": "west",
    },
    # The fourth field (ADR-0041). ADR-0021 ruled stone ambient terrain and "never a patch", and
    # that is reversed: it discharged stone's function onto a cobble generator, and Terra's noise
    # settings carry `aquifers_enabled: false` and place no lava, so a cobble generator is
    # unbuildable here. The smallest field of the four, which is Factorio's own ordering --
    # stone's starting patch is 160,000 against iron's 400,000.
    "stone": {
        "block": "planetaryfactory:stone_ore",
        "radii": [8, 9, 11],
        "facing": "south",
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

    The template is one block tall, and `planetaryfactory:ground` drops each of its columns onto
    the terrain, so y=0 is the topsoil block itself: the ore replaces it and the field lies flush
    with the surface. That is the Factorio reading ADR-0019 asks for -- a patch you see the outline
    of and plan a miner over -- and it is also what makes the field legible after half of it has
    been dug. Under a wood the ore goes beneath the trees, which still stand on it.
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

    # Always west, whatever direction the field is meant to run. The ore lies along +x in
    # template space, so the connector -- which points back at the hub from the far end --
    # is -x. Which way the field actually runs is then decided by rotation, and vanilla picks
    # the rotation for us: `JigsawBlock.canAttach` only accepts the one that leaves this front
    # opposite the hub's. Writing OPPOSITE[facing] here instead names the right direction in
    # world space and the wrong one in template space, which rotates the field off its axis --
    # copper's would land on top of iron's.
    connector, connector_nbt = jigsaw_block(
        "planetaryfactory:terra_start_patch",
        "planetaryfactory:terra_start_hub",
        "minecraft:empty",
        "west",
    )
    blocks.append({
        "pos": [nbt.Int(0), nbt.Int(0), nbt.Int(centre_z)],
        "state": nbt.Int(state_of(connector)),
        "nbt": connector_nbt,
    })

    ore = state_of({"Name": spec["block"]})
    for dx, dz in cells:
        x, z = centre_x + dx, centre_z + dz
        if x == 0 and z == centre_z:
            continue  # never bury the connector
        blocks.append({
            "pos": [nbt.Int(x), nbt.Int(0), nbt.Int(z)],
            "state": nbt.Int(ore),
        })

    write_template(
        os.path.join(STRUCTURES, "terra_start_%s_%s.nbt" % (resource, size_name)),
        (width, 1, depth), palette, blocks)


# How far a connector may wander along its face. The hub grows to accommodate it.
SCATTER = 7


def patch_span(resource):
    """Half the width of this resource's widest field, in blocks.

    The widest, not the drawn one: which size variant a connector deals is decided at world
    generation, so the hub has to be laid out for the largest it could deal.
    """
    return max(PATCHES[resource]["radii"]) + 3


def along_face(resource, dx, dz):
    """A connector's scatter runs along the hub face it sits on: z on an east or west face,
    x on a north or south one. The other component is pinned to the edge."""
    return dz if PATCHES[resource]["facing"] in ("east", "west") else dx


def build_hub(rng, index, offsets):
    """One hub variant: one connector per resource, at scattered positions.

    The hub places no block of its own. Its whole job is to hold the three connectors far
    enough apart, and at different enough offsets, that the four fields do not land on a
    fixed figure every world.

    Every connector sits on the hub's own outer face, pointing out of it, and that is not a
    style choice. Vanilla marks the parent's *entire* bounding box occupied the moment a
    connector points at a block inside it (`JigsawPlacement.Placer.tryPlacingChildren`), and
    then rejects every child that overlaps the occupied shape -- which is every child, since
    each one starts at that same interior block. An interior connector therefore attaches
    nothing at all, silently, with no warning in the log. So the scatter runs *along* each
    face rather than across the hub's interior.
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

    # The hub is sized by the widest field, not by the connector offsets, and this is the whole
    # reason it is large. A patch template's bounding box is a rectangle `2*span+1` wide running
    # the *entire* way from its connector to the far end of the field. Two fields on perpendicular
    # faces therefore both cover the diagonal corner beside the hub, overlap there, and vanilla
    # drops whichever it happens to try second -- silently, since a rejected child is not an error.
    # Making the hub at least as wide as the widest field plus its scatter keeps every field's
    # sideways extent inside the hub's own footprint, so no two can reach each other's corner.
    width = 2 * (max(patch_span(resource) for resource in offsets) + SCATTER) + 1
    occupied = set()
    for resource, (dx, dz) in offsets.items():
        facing = PATCHES[resource]["facing"]
        block, block_nbt = jigsaw_block(
            "planetaryfactory:terra_start_hub",
            "planetaryfactory:terra_start_patch",
            "planetaryfactory:terra_start_" + resource,
            facing,
        )
        # Clamped by this resource's own span, which is what makes the guarantee hold rather
        # than merely usually hold: the size variant is drawn at world generation, so the hub
        # has to fit the largest one this connector could ever deal.
        span = patch_span(resource)
        along = min(max(width // 2 + along_face(resource, dx, dz), span), width - 1 - span)
        x, z = {
            "east": (width - 1, along),
            "west": (0, along),
            "north": (along, 0),
            "south": (along, width - 1),
        }[facing]
        assert (x, z) not in occupied, "hub %d puts two connectors in one cell" % index
        occupied.add((x, z))
        blocks.append({
            "pos": [nbt.Int(x), nbt.Int(0), nbt.Int(z)],
            "state": nbt.Int(state_of(block)),
            "nbt": block_nbt,
        })

    write_template(os.path.join(STRUCTURES, "terra_start_hub_%d.nbt" % index),
                   (width, 1, width), palette, blocks)


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

    write_json(os.path.join(WORLDGEN, "processor_list", "terra_start_ground.json"), {
        "_comment": "Generated by scripts/build-terra-start.py. `planetaryfactory:ground` is "
                    "registered by planetaryfactory_core. It drops each column of a patch onto the "
                    "terrain, walking down through whatever grew there. Vanilla's "
                    "`minecraft:gravity` -- the one the `terrain_matching` projection applies -- "
                    "reads WORLD_SURFACE instead, which is 'the highest block that is not air', so "
                    "a field crossing a wood landed on the canopy as ore in place of leaves. No "
                    "vanilla heightmap avoids that; OCEAN_FLOOR and MOTION_BLOCKING stop at leaves "
                    "and logs too.",
        "processors": [{"processor_type": "planetaryfactory:ground"}],
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
                        # `rigid`, not `terrain_matching`, and the field still follows the ground:
                        # the processor below is what drops each column, and the projection is what
                        # would otherwise add vanilla's own gravity processor on top of it. See the
                        # processor list's comment for why vanilla's will not do.
                        "projection": "rigid",
                        "processors": "planetaryfactory:terra_start_ground",
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
        # The hub, then its three patches: one level of children, so 1. Vanilla gates children
        # on `maxDepth > 0` and draws from the real pool while `depth != maxDepth`, so at depth
        # 0 the patches are dealt and at depth 1 only the empty fallback is -- which is what we
        # want, since a patch carries no connector onward.
        "size": 1,
        # Both are dead weight now and are kept only so the structure still reads as a whole:
        # nothing places this structure through worldgen. `planetaryfactory_core` stamps the
        # pool onto world spawn instead (see TerraStartingArea), because no StructurePlacement
        # can see world spawn -- it is handed a ChunkGeneratorStructureState and nothing else.
        "start_height": {"absolute": 0},
        "project_start_to_heightmap": "WORLD_SURFACE_WG",
        # Must clear the furthest patch centre plus its own radius.
        "max_distance_from_center": 112,
        "use_expansion_hack": False,
    })


def main():
    rng = random.Random(SEED)
    os.makedirs(STRUCTURES, exist_ok=True)

    for resource, spec in PATCHES.items():
        for size, radius, distance in zip(SIZES, spec["radii"], DISTANCES):
            build_patch(rng, resource, size, radius, distance)

    # Three hubs, each spreading the connectors differently, so the fields do not sit on the
    # same figure in every world. Four connectors since ADR-0041 -- one per face, which is what
    # a fourth field costs: the hub has four of them and the fifth resource, uranium, has no
    # starting patch in Factorio and so needs none here.
    hub_offsets = [
        {"iron": (0, 0), "copper": (-6, 4), "coal": (2, -7), "stone": (5, 3)},
        {"iron": (3, -5), "copper": (0, 0), "coal": (-8, 2), "stone": (-3, 6)},
        {"iron": (-4, 6), "copper": (7, 1), "coal": (0, 0), "stone": (2, -4)},
    ]
    for index, offsets in enumerate(hub_offsets):
        build_hub(rng, index, offsets)

    build_datapack(len(hub_offsets))


if __name__ == "__main__":
    main()
