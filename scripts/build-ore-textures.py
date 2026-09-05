#!/usr/bin/env python3
"""Build the ore blocks' eight stage sprites, one set per resource (ADR-0041).

**Placeholder art, generated rather than drawn**, on the same footing as
`scripts/gen-flora-textures.py`: 16x16 RGBA, pure stdlib, meant to be replaced. What is *not*
placeholder is the arithmetic, and it is the reason this is a script rather than forty checked-in
PNGs nobody can re-derive.

ADR-0041 renders a block's remaining amount as one of Factorio's eight sprite stages. Factorio's
own thresholds are amounts -- 15000 down to 80 -- which do not port to blocks holding about a
thousand; what ports is the *ratio set*, and `data/factorio/resource.json` carries it per resource
as `stage_ratios`. **The speckle count of each stage is that ratio times the full stage's count**,
so the picture is the number: a block showing a quarter of its speckles is holding about a quarter
of its ore. That is what makes a stage unable to compete with the amount, which is ADR-0020's
objection to worn textures and the reason ADR-0041 could amend it.

The colours are the placeholder half and are chosen here, one per resource. The speckle *positions*
are drawn from a fixed seed per resource and then *removed in a fixed order* as the stages fall, so
a block thinning out looks like the same block losing ore rather than eight unrelated sprites.

Run after re-extracting the corpus, in case Factorio changed its stage counts:

    scripts/build-ore-textures.py
    scripts/build-ore-textures.py --check    # what tests/ runs: regenerate and diff

Writes `kubejs/assets/planetaryfactory/textures/block/ore/<resource>_stage<N>.png`.
"""
import argparse
import json
import os
import random
import struct
import sys
import zlib

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
CORPUS = os.path.join(ROOT, "data", "factorio", "resource.json")
OUT = os.path.join(ROOT, "kubejs", "assets", "planetaryfactory", "textures", "block", "ore")

# Terra's alphabet, and the Factorio resource each block's amounts are read from. The block ids
# are the pack's; the keys are Factorio's, because that is what the corpus is keyed by (ADR-0028).
RESOURCES = {
    "iron": "iron-ore",
    "copper": "copper-ore",
    "coal": "coal",
    "uranium": "uranium-ore",
    "stone": "stone",
}

# The placeholder half: a speckle colour per resource, and the stone they sit in. Chosen, not
# derived -- these are stand-ins for art, and the flora textures were made the same way.
STONE = (122, 122, 122)
SPECKLE = {
    "iron": (196, 168, 140),
    "copper": (196, 118, 62),
    "coal": (38, 38, 42),
    "uranium": (94, 176, 88),
    "stone": (166, 160, 150),
}

# How many of the 256 pixels a full block speckles. Enough to read as an ore at a glance and
# leave room for eight distinguishable steps below it.
FULL_SPECKLES = 96

# One seed per resource, so a rerun does not churn forty binaries for no reason.
SEED = 20260905


def png(pixels):
    """A 16x16 RGBA PNG, as bytes."""
    raw = b"".join(b"\x00" + b"".join(bytes(p) for p in row) for row in pixels)

    def chunk(tag, body):
        data = tag + body
        return struct.pack(">I", len(body)) + data + struct.pack(">I", zlib.crc32(data) & 0xFFFFFFFF)

    return (
        b"\x89PNG\r\n\x1a\n"
        + chunk(b"IHDR", struct.pack(">IIBBBBB", 16, 16, 8, 6, 0, 0, 0))
        + chunk(b"IDAT", zlib.compress(raw, 9))
        + chunk(b"IEND", b"")
    )


def shade(colour, delta):
    return [max(0, min(255, value + delta)) for value in colour] + [255]


def sprites(resource, ratios):
    """One sprite per stage, speckled in proportion to that stage's ratio.

    The speckle order is fixed and the stages *truncate* it, so stage `n + 1` shows a subset of
    stage `n`'s pixels: the block loses ore in place rather than being redrawn.
    """
    rng = random.Random(f"{SEED}:{resource}")
    positions = [(x, y) for y in range(16) for x in range(16)]
    rng.shuffle(positions)
    speckles = positions[:FULL_SPECKLES]
    ground = [[shade(STONE, rng.randint(-9, 9)) for _ in range(16)] for _ in range(16)]

    out = []
    for ratio in ratios:
        pixels = [row[:] for row in ground]
        # `ceil`, so a stage that Factorio still renders as ore never comes out as bare stone.
        count = min(len(speckles), -(-int(round(FULL_SPECKLES * ratio * 1000)) // 1000))
        for x, y in speckles[:count]:
            pixels[y][x] = shade(SPECKLE[resource], rng.randint(-14, 14))
        out.append(png(pixels))
    return out


def build():
    corpus = json.load(open(CORPUS, encoding="utf-8"))
    by_name = {entry["name"]: entry for entry in corpus["resources"]}
    files = {}
    for resource, factorio in sorted(RESOURCES.items()):
        entry = by_name.get(factorio)
        if entry is None:
            sys.exit(f"{factorio} is not in the corpus -- re-run scripts/factorio-resource-extract.py")
        ratios = entry["stage_ratios"]
        if len(ratios) < 2:
            sys.exit(f"{factorio} carries {len(ratios)} stage ratios; there is nothing to render")
        for stage, image in enumerate(sprites(resource, ratios)):
            files[os.path.join(OUT, f"{resource}_stage{stage}.png")] = image
    return files


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--check", action="store_true",
                        help="regenerate and compare, without writing")
    args = parser.parse_args()

    files = build()
    if args.check:
        stale = [
            path for path, image in sorted(files.items())
            if not os.path.exists(path) or open(path, "rb").read() != image
        ]
        for path in stale:
            print(f"FAIL: {os.path.relpath(path, ROOT)} is missing or stale")
        if stale:
            return 1
        print(f"ok   {len(files)} ore stage sprites match the corpus's ratios")
        return 0

    os.makedirs(OUT, exist_ok=True)
    for path, image in sorted(files.items()):
        open(path, "wb").write(image)
    print(f"wrote {len(files)} sprites into {os.path.relpath(OUT, ROOT)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
