#!/usr/bin/env python3
"""Build the Engineer's Steel Pick texture: GTCEu's Damascus Steel pickaxe, flattened.

The Steel Pick is not a GregTech tool, so GregTech's item-colour handler never sees it -- and
GT's tool art is three greyscale layers that only become a material when that handler tints them.
Referencing them from our model would render an uncoloured grey pickaxe. So the tint is baked here
instead: handle, head and overlay composited into one RGBA sprite, with Damascus Steel's own colour
read from the value GTCEu registers (`SecondDegreeMaterials`: `damascus_steel .color(7237230)`).

The Iron Pick needs no such file. Its model points straight at `minecraft:item/iron_pickaxe`,
which needs no tint and no copy.

Run after updating GTCEu, in case its tool art changed:

    scripts/build-pick-textures.py

Reads the installed `mods/gtceu-*.jar` and writes
`kubejs/assets/planetaryfactory/textures/item/engineers_steel_pick.png`. No PIL: the PNG codec
below is a minimal 8-bit reader and an RGBA writer, which is less to install than Pillow and less
to explain than a checked-in binary with no provenance.
"""
import zlib, struct, sys, pathlib

def read_png(path):
    data = path.read() if hasattr(path, "read") else pathlib.Path(path).read_bytes()
    assert data[:8] == b"\x89PNG\r\n\x1a\n", path
    i, idat, plte, trns, ihdr = 8, b"", None, None, None
    while i < len(data):
        length = struct.unpack(">I", data[i:i+4])[0]
        tag = data[i+4:i+8]
        body = data[i+8:i+8+length]
        if tag == b"IHDR": ihdr = struct.unpack(">IIBBBBB", body)
        elif tag == b"IDAT": idat += body
        elif tag == b"PLTE": plte = body
        elif tag == b"tRNS": trns = body
        i += 12 + length
    w, h, depth, ctype, comp, filt, interlace = ihdr
    assert depth == 8 and interlace == 0, (path, depth, interlace)
    channels = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[ctype]
    raw = zlib.decompress(idat)
    stride = w * channels
    out, prev = [], bytearray(stride)
    pos = 0
    for _ in range(h):
        f = raw[pos]; pos += 1
        line = bytearray(raw[pos:pos+stride]); pos += stride
        for x in range(stride):
            a = line[x-channels] if x >= channels else 0
            b = prev[x]
            c = prev[x-channels] if x >= channels else 0
            if f == 1: line[x] = (line[x] + a) & 0xFF
            elif f == 2: line[x] = (line[x] + b) & 0xFF
            elif f == 3: line[x] = (line[x] + (a + b) // 2) & 0xFF
            elif f == 4:
                p = a + b - c
                pa, pb, pc = abs(p-a), abs(p-b), abs(p-c)
                pr = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
                line[x] = (line[x] + pr) & 0xFF
        prev = line
        row = []
        for x in range(w):
            px = line[x*channels:(x+1)*channels]
            if ctype == 6: row.append(tuple(px))
            elif ctype == 2: row.append((px[0], px[1], px[2], 255))
            elif ctype == 4: row.append((px[0], px[0], px[0], px[1]))
            elif ctype == 0: row.append((px[0], px[0], px[0], 255))
            else:
                idx = px[0]
                r, g, b = plte[idx*3:idx*3+3]
                alpha = trns[idx] if trns and idx < len(trns) else 255
                row.append((r, g, b, alpha))
        out.append(row)
    return w, h, out

def write_png(path, rows):
    h = len(rows); w = len(rows[0])
    raw = b"".join(b"\x00" + b"".join(bytes(px) for px in row) for row in rows)
    def chunk(tag, body):
        c = tag + body
        return struct.pack(">I", len(body)) + c + struct.pack(">I", zlib.crc32(c))
    blob = (b"\x89PNG\r\n\x1a\n"
        + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
            + chunk(b"IDAT", zlib.compress(raw, 9))
            + chunk(b"IEND", b""))
    if hasattr(path, "write"):
        path.write(blob)
    else:
        pathlib.Path(path).write_bytes(blob)

def tint(rows, colour):
    r, g, b = (colour >> 16) & 0xFF, (colour >> 8) & 0xFF, colour & 0xFF
    return [[(px[0]*r//255, px[1]*g//255, px[2]*b//255, px[3]) for px in row] for row in rows]

def over(base, top):
    out = []
    for by, ty in zip(base, top):
        row = []
        for bp, tp in zip(by, ty):
            a = tp[3] / 255.0
            if a == 0: row.append(bp)
            elif a == 1: row.append(tp)
            else:
                row.append(tuple(int(tp[i]*a + bp[i]*(1-a)) for i in range(3))
                           + (max(bp[3], tp[3]),))
        out.append(row)
    return out

ROOT = pathlib.Path(__file__).resolve().parent.parent
OUT = ROOT / "kubejs/assets/planetaryfactory/textures/item/engineers_steel_pick.png"
LAYERS = ("handle.png", "pickaxe.png", "pickaxe_overlay.png")
DAMASCUS = 0x6E6E6E          # GTMaterials: damascus_steel .color(7237230)
HANDLE_WOOD = 0x7A5530       # GT tints the handle by its rod material; ours is a wooden haft.

def gt_layers():
    """GT's three tool layers, read straight out of the jar the pack actually ships."""
    import io, zipfile
    jars = sorted((ROOT / "mods").glob("gtceu-*.jar"))
    if len(jars) != 1:
        raise SystemExit("expected exactly one mods/gtceu-*.jar, found %d -- the jar set is a "
                         "packwiz manifest (ADR-0024), so reconcile mods/ with it" % len(jars))
    with zipfile.ZipFile(jars[0]) as jar:
        for name in LAYERS:
            with jar.open("assets/gtceu/textures/item/tools/" + name) as handle:
                yield read_png(io.BytesIO(handle.read()))[2]


def main():
    handle, head, overlay = gt_layers()
    image = over(over(tint(handle, HANDLE_WOOD), tint(head, DAMASCUS)), overlay)
    if "--check" in sys.argv:
        current = OUT.read_bytes() if OUT.is_file() else b""
        import io
        buffer = io.BytesIO()
        write_png(buffer, image)
        if buffer.getvalue() != current:
            print("FAIL %s is stale -- re-run scripts/build-pick-textures.py"
                  % OUT.relative_to(ROOT))
            return 1
        print("ok   %s is current" % OUT.relative_to(ROOT))
        return 0
    write_png(OUT, image)
    print("ok   wrote %s" % OUT.relative_to(ROOT))
    return 0


if __name__ == "__main__":
    sys.exit(main())
