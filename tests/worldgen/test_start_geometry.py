#!/usr/bin/env python3
"""Terra's starting area: no two ore fields may claim the same ground.

Vanilla drops a jigsaw child whose bounding box overlaps one already placed, and it does so
without logging anything -- a rejected child is an ordinary outcome, not an error. So a hub
whose fields overlap does not fail loudly; it quietly deals two patches instead of three, and
only on some draws. That is what shipped once already: the fields' boxes missed each other by
a single block on the seed the harness happened to use, and collided on the next world.

A patch template's box is a rectangle `2*span+1` wide running the whole way from its connector
to the far end of the field, so two fields on perpendicular hub faces both cover the corner
beside the hub unless the hub is wide enough to hold them apart. This asserts that for every
hub variant against every combination of size variants -- the size is drawn at world
generation, so only the worst case is a guarantee.

Reads the generated .nbt templates, not the generator's own tables, so it fails if
`scripts/build-terra-start.py` is edited and not re-run.
"""

import gzip
import itertools
import os
import struct
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
STRUCTURES = os.path.join(ROOT, "kubejs", "data", "planetaryfactory", "structure")

STEP = {"east": (1, 0), "west": (-1, 0), "north": (0, -1), "south": (0, 1)}
RESOURCES = ["iron", "copper", "coal"]
SIZES = ["small", "medium", "large"]


def read_nbt(path):
    """Just enough NBT to read a structure template."""
    with gzip.open(path, "rb") as handle:
        data = handle.read()
    pos = [0]

    def take(n):
        chunk = data[pos[0]:pos[0] + n]
        pos[0] += n
        return chunk

    def name():
        return take(struct.unpack(">H", take(2))[0]).decode("utf8")

    def value(tag):
        if tag == 1:
            return struct.unpack(">b", take(1))[0]
        if tag == 2:
            return struct.unpack(">h", take(2))[0]
        if tag == 3:
            return struct.unpack(">i", take(4))[0]
        if tag == 4:
            return struct.unpack(">q", take(8))[0]
        if tag == 5:
            return struct.unpack(">f", take(4))[0]
        if tag == 6:
            return struct.unpack(">d", take(8))[0]
        if tag == 7:
            return take(struct.unpack(">i", take(4))[0])
        if tag == 8:
            return name()
        if tag == 9:
            element = take(1)[0]
            return [value(element) for _ in range(struct.unpack(">i", take(4))[0])]
        if tag == 10:
            out = {}
            while True:
                inner = take(1)[0]
                if inner == 0:
                    return out
                # Name first, deliberately: `out[name()] = value(inner)` would read the
                # payload before the key, because Python evaluates the right side first.
                key = name()
                out[key] = value(inner)
        if tag == 11:
            return [struct.unpack(">i", take(4))[0]
                    for _ in range(struct.unpack(">i", take(4))[0])]
        raise AssertionError("unhandled tag %d" % tag)

    assert take(1)[0] == 10
    name()
    return value(10)


def jigsaws(template):
    """Every jigsaw block in a template, as (pos, facing, pool)."""
    palette = template["palette"]
    out = []
    for block in template["blocks"]:
        entry = palette[block["state"]]
        if entry["Name"] != "minecraft:jigsaw":
            continue
        facing = entry["Properties"]["orientation"].split("_")[0]
        out.append((tuple(block["pos"]), facing, block["nbt"]["pool"]))
    return out


def patch_box(connector, facing, template):
    """Where a patch template lands, in hub-local coordinates.

    Its own connector sits one block in front of the hub's, template +x runs away from the hub,
    and the field is centred on the connector across that axis.
    """
    width, _, depth = template["size"]
    span = (depth - 1) // 2
    dx, dz = STEP[facing]
    px, pz = -dz, dx                     # the axis across the field
    cx, _, cz = connector
    corners = []
    for along in (1, width):
        for across in (-span, span):
            corners.append((cx + dx * along + px * across,
                            cz + dz * along + pz * across))
    xs = [c[0] for c in corners]
    zs = [c[1] for c in corners]
    return min(xs), min(zs), max(xs), max(zs)


def overlaps(a, b):
    return not (a[2] < b[0] or b[2] < a[0] or a[3] < b[1] or b[3] < a[1])


def main():
    failures = []
    patches = {
        (resource, size): read_nbt(
            os.path.join(STRUCTURES, "terra_start_%s_%s.nbt" % (resource, size)))
        for resource in RESOURCES for size in SIZES
    }

    hubs = sorted(f for f in os.listdir(STRUCTURES) if f.startswith("terra_start_hub_"))
    assert hubs, "no hub templates -- run scripts/build-terra-start.py"

    for hub_file in hubs:
        hub = read_nbt(os.path.join(STRUCTURES, hub_file))
        width, _, depth = hub["size"]
        hub_box = (0, 0, width - 1, depth - 1)
        connectors = jigsaws(hub)
        assert len(connectors) == len(RESOURCES), \
            "%s has %d connectors, expected %d" % (hub_file, len(connectors), len(RESOURCES))

        # Every connector must sit on the face it points out of. One that points at a block
        # inside the hub makes vanilla treat the whole hub as occupied, and then no child can
        # ever attach -- the bug this pack shipped before the fields ever appeared.
        for (cx, _, cz), facing, _ in connectors:
            dx, dz = STEP[facing]
            ahead = (cx + dx, cz + dz)
            if hub_box[0] <= ahead[0] <= hub_box[2] and hub_box[1] <= ahead[1] <= hub_box[3]:
                failures.append("%s: connector at %d,%d faces %s into its own box"
                                % (hub_file, cx, cz, facing))

        for draw in itertools.product(SIZES, repeat=len(connectors)):
            boxes = []
            for (connector, facing, pool), size in zip(connectors, draw):
                resource = pool.rsplit("_", 1)[-1]
                boxes.append((resource, size,
                              patch_box(connector, facing, patches[(resource, size)])))
            for (ra, sa, ba), (rb, sb, bb) in itertools.combinations(boxes, 2):
                if overlaps(ba, bb):
                    failures.append(
                        "%s: %s %s overlaps %s %s (%s vs %s) -- one of them will not place"
                        % (hub_file, ra, sa, rb, sb, ba, bb))
            for resource, size, box in boxes:
                if overlaps(box, hub_box):
                    failures.append("%s: %s %s overlaps the hub itself (%s)"
                                    % (hub_file, resource, size, box))

    for line in sorted(set(failures)):
        print("FAIL %s" % line)
    if failures:
        print("\n%d starting-area geometry failure(s)" % len(set(failures)))
        return 1
    print("ok   %d hub variant(s) x %d size draw(s): no field overlaps another or the hub"
          % (len(hubs), len(SIZES) ** len(RESOURCES)))
    return 0


if __name__ == "__main__":
    sys.exit(main())
