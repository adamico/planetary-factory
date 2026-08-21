# Placeholder textures for Sapros's flora, generated rather than drawn. 16x16 RGBA PNGs,
# pure stdlib. They exist so the trees are visually distinguishable from each other and from
# every vanilla tree; they are meant to be replaced by real art.
import zlib, struct, random, os

BASE = "kubejs/assets/planetaryfactory/textures"

def png(path, pixels):
    raw = b"".join(b"\x00" + b"".join(bytes(p) for p in row) for row in pixels)
    def chunk(t, d):
        c = t + d
        return struct.pack(">I", len(d)) + c + struct.pack(">I", zlib.crc32(c) & 0xffffffff)
    data = (b"\x89PNG\r\n\x1a\n"
            + chunk(b"IHDR", struct.pack(">IIBBBBB", 16, 16, 8, 6, 0, 0, 0))
            + chunk(b"IDAT", zlib.compress(raw, 9))
            + chunk(b"IEND", b""))
    os.makedirs(os.path.dirname(path), exist_ok=True)
    open(path, "wb").write(data)

def shade(c, d):
    return [max(0, min(255, v + d)) for v in c[:3]] + [c[3]]

def noise(base, spread, seed, alpha_holes=0.0):
    rnd = random.Random(seed)
    return [[shade(base, rnd.randint(-spread, spread)) if rnd.random() >= alpha_holes
             else [0, 0, 0, 0] for _ in range(16)] for _ in range(16)]

def bark(base, streak, seed):
    rnd = random.Random(seed)
    rows = noise(base, 12, seed)
    for x in range(16):
        if rnd.random() < 0.35:
            d = rnd.randint(-30, -12)
            h = rnd.randint(4, 14)
            y0 = rnd.randint(0, 15 - h)
            for y in range(y0, y0 + h):
                rows[y][x] = shade(streak, d)
    return rows

def rings(outer, inner, seed):
    rows = noise(outer, 10, seed)
    for y in range(16):
        for x in range(16):
            if 4 <= x <= 11 and 4 <= y <= 11:
                rows[y][x] = shade(inner, (x * y) % 9 - 4)
    return rows

def blob(bg, fg, seed, cx=8, cy=9, r=5):
    rows = [[list(bg) for _ in range(16)] for _ in range(16)]
    rnd = random.Random(seed)
    for y in range(16):
        for x in range(16):
            if (x - cx) ** 2 + (y - cy) ** 2 <= r * r:
                rows[y][x] = shade(fg, rnd.randint(-14, 14))
    return rows

CLEAR = [0, 0, 0, 0]

# Yumako: warm orange canopy over pale bark. Nothing vanilla is this colour. The fruit is
# drawn into the leaves because it is always there -- the tree is harvested once, by felling.
png(f"{BASE}/block/yumako_log.png", bark([164, 122, 84, 255], [120, 86, 58, 255], 1))
fruiting = noise([94, 158, 66, 255], 16, 2)
for (fx, fy) in [(3, 4), (11, 6), (6, 11), (13, 12)]:
    for dy in range(3):
        for dx in range(3):
            if (dx, dy) in ((0, 0), (2, 0), (0, 2), (2, 2)):
                continue
            fruiting[(fy + dy) % 16][(fx + dx) % 16] = shade([236, 138, 40, 255], (dx + dy) * 6 - 6)
png(f"{BASE}/block/yumako_leaves.png", fruiting)

# Jellystem: a deep red trunk with a translucent-looking violet core, purple canopy.
png(f"{BASE}/block/jellystem_stem.png", rings([138, 54, 62, 255], [176, 92, 156, 255], 3))
png(f"{BASE}/block/jellystem_leaves.png", noise([132, 74, 148, 255], 14, 4))

# Saplings: a sprig on a transparent background, tinted per tree.
def sprig(stem, leaf, seed):
    rows = [[list(CLEAR) for _ in range(16)] for _ in range(16)]
    rnd = random.Random(seed)
    for y in range(8, 15):
        rows[y][7] = shade(stem, rnd.randint(-10, 10))
        rows[y][8] = shade(stem, rnd.randint(-10, 10))
    for y in range(3, 10):
        for x in range(3, 13):
            if abs(x - 8) + abs(y - 6) <= 4 and rnd.random() < 0.85:
                rows[y][x] = shade(leaf, rnd.randint(-16, 16))
    return rows

png(f"{BASE}/block/yumako_sapling.png", sprig([132, 98, 66, 255], [94, 158, 66, 255], 5))
png(f"{BASE}/block/jellystem_sapling.png", sprig([138, 54, 62, 255], [132, 74, 148, 255], 6))

# Items. Single-layer for now: the Freshness badge ADR-0010 describes as layer1 arrives
# with the Decay engine (#17), and nothing here may depend on it existing.
png(f"{BASE}/item/yumako.png", blob(CLEAR, [236, 138, 40, 255], 7))
png(f"{BASE}/item/jellynut.png", blob(CLEAR, [176, 92, 156, 255], 8, r=4))
print("ok")
