#!/usr/bin/env python3
"""Assert that the worldgen registries a fresh world loads match tests/worldgen/expected.json.

GregTech's ore veins, bedrock ore deposits and worldgen layers are datapack
registries: their JSON parses through codecs that throw on malformed input, so a
broken file already fails loudly. What no codec catches is the semantic half — a
vein whose layer does not cover the dimension it filters to, a vein leaking onto the
wrong body, a deposit that never loaded at all. That needs a running game.

This launches the pack into a freshly created world, waits for
kubejs/server_scripts/registry_dump.js to write what loaded, kills the game and
compares the dump to the fixture. Adding a body means adding a fixture entry; this
file should not need to change. See docs/testing/worldgen-registry-check.md.

Usage: scripts/worldgen-check.py [--timeout SECONDS] [--keep-world] [--dump-only]
"""
import argparse
import contextlib
import importlib.util
import json
import os
import shutil
import signal
import subprocess
import sys
import time
from pathlib import Path

INSTANCE = Path(__file__).resolve().parent.parent
WORLD_NAME = "WorldgenCheck"
WORLD_TEMPLATE = INSTANCE / "tests/worldgen/world-template"
WORLD = INSTANCE / "saves" / WORLD_NAME
EXPECTED = INSTANCE / "tests/worldgen/expected.json"
REQUEST = INSTANCE / "local/registry-dump.request.json"
DUMP = INSTANCE / "local/registry-dump.json"
LOG_MARKER = "WORLDGEN_DUMP "


def fresh_world():
    """Rebuild the test world from the template, so every run loads its datapacks anew.

    A world persists the dimension list it was created with, so reusing one answers
    the wrong question. The template is a bare level.dat: everything else — regions,
    entities, the loaded registries this check reads — is regenerated on load.
    """
    if WORLD.exists():
        shutil.rmtree(WORLD)
    shutil.copytree(WORLD_TEMPLATE, WORLD)


def run_game(timeout, no_display=False):
    REQUEST.parent.mkdir(parents=True, exist_ok=True)
    REQUEST.write_text('{"requested_by": "scripts/worldgen-check.py"}\n')
    DUMP.unlink(missing_ok=True)
    # The log is a dump handle too, so the previous run's copy has to go first or it
    # would be read as this run's answer.
    (INSTANCE / "logs/latest.log").unlink(missing_ok=True)

    game = subprocess.Popen(
        [sys.executable, str(INSTANCE / "scripts/launch.py"),
         *(["--headless"] if no_display else []),
         "--quickPlaySingleplayer", WORLD_NAME],
        stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
        start_new_session=True,
    )
    try:
        deadline = time.time() + timeout
        while time.time() < deadline:
            if DUMP.exists() or dump_from_log() is not None:
                return True
            if game.poll() is not None:
                return load_dump() is not None
            time.sleep(2)
        return False
    finally:
        REQUEST.unlink(missing_ok=True)
        if game.poll() is None:
            os.killpg(os.getpgid(game.pid), signal.SIGTERM)
            try:
                game.wait(timeout=30)
            except subprocess.TimeoutExpired:
                os.killpg(os.getpgid(game.pid), signal.SIGKILL)


def dump_from_log():
    """Recover the dump from the game log.

    KubeJS's class filter blocks java.nio, so the dump script writes through KubeJS's
    own helper — which a future KubeJS could equally well put out of reach. It always
    logs the same JSON, so the log is the handle that cannot be taken away.
    """
    log = INSTANCE / "logs/latest.log"
    if not log.exists():
        return None
    found = None
    for line in log.read_text(errors="replace").splitlines():
        marker = line.find(LOG_MARKER)
        if marker != -1:
            found = line[marker + len(LOG_MARKER):]
    return json.loads(found) if found else None


def load_dump():
    if DUMP.exists():
        return json.loads(DUMP.read_text())
    return dump_from_log()


def compare(dump, expected):
    """Yield one failure line per expectation the dump does not meet."""
    veins = dump.get("ore_veins", {})
    deposits = dump.get("bedrock_ores", {})
    fluids = dump.get("bedrock_fluids", {})
    layers = dump.get("worldgen_layers", {})
    biomes = dump.get("biomes", {})
    structures = dump.get("structures", {})
    structure_sets = dump.get("structure_sets", {})
    blocks = set(dump.get("blocks", []))
    bounds = dump.get("dimension_bounds", {})

    def barren(spec, key, registry, noun, dimension, body):
        """Assert an emptiness against the whole registry, not against a list of names.

        A present-but-empty object in the fixture claims the body carries none of this
        kind of thing at all. Walking the registry is the only form of that claim which
        cannot go stale as later tickets add entries, and it catches an entry that
        reaches the body without naming it -- an empty `dimension_filter` included,
        which GregTech reads as "nowhere" and which must therefore appear as nowhere
        here too.
        """
        if spec.get(key) != {} or key not in spec:
            return
        for entry_id, got in sorted(registry.items()):
            if dimension in got["dimensions"]:
                yield (f"{body}: {noun} {entry_id} reaches {dimension}, which is "
                       f"expected to carry no {noun}s at all")

    for body, spec in expected["bodies"].items():
        dimension = spec["dimension"]

        # The dimension's column. Nothing else in the dump implies it: a vein's height range
        # is clamped to whatever the dimension turns out to be, so a column that silently
        # reverted to vanilla's -64..320 still loads every vein the fixture names.
        want_bounds = spec.get("dimension_bounds")
        if want_bounds is not None:
            got_bounds = bounds.get(dimension)
            if got_bounds is None:
                yield f"{body}: no dimension bounds dumped for {dimension}"
            else:
                for field, value in sorted(want_bounds.items()):
                    if got_bounds.get(field) != value:
                        yield (f"{body}: dimension {dimension} has {field} "
                               f"{got_bounds.get(field)!r}, expected {value!r}")

        # The body's own worldgen layer, asserted directly rather than through a vein. A
        # body with no veins has nothing else that would mention its layer, and a layer
        # that does not cover the dimension is invisible until someone prospects there.
        want_layer = spec.get("worldgen_layer")
        if want_layer is not None:
            got_layer = layers.get(want_layer)
            if got_layer is None:
                yield f"{body}: worldgen layer {want_layer} did not load"
            elif dimension not in got_layer.get("dimensions", []):
                yield (f"{body}: worldgen layer {want_layer} does not cover {dimension} "
                       f"(covers {got_layer.get('dimensions', [])})")

        # A biome that parses but occupies unreachable noise space generates nowhere, and
        # nothing but the generator itself can say which of the two it is.
        emitted = biomes.get(dimension)
        for biome_id in spec.get("biomes", []):
            if emitted is None:
                yield f"{body}: no biome sample for {dimension}, so {biome_id} is unproven"
            elif biome_id not in emitted:
                yield (f"{body}: biome {biome_id} is never emitted by {dimension}'s "
                       f"generator")

        # A present-but-empty object is not "nothing to check": it is the assertion
        # that the body carries none of that kind of thing at all. See `barren`.
        yield from barren(spec, "ore_veins", veins, "ore vein", dimension, body)
        yield from barren(spec, "bedrock_ores", deposits, "bedrock ore deposit",
                          dimension, body)
        yield from barren(spec, "bedrock_fluids", fluids, "bedrock fluid deposit",
                          dimension, body)

        for vein_id, want in spec.get("ore_veins", {}).items():
            got = veins.get(vein_id)
            if got is None:
                yield f"{body}: ore vein {vein_id} did not load"
                continue
            if dimension not in got["dimensions"]:
                yield (f"{body}: ore vein {vein_id} does not filter to {dimension} "
                       f"(filters to {got['dimensions']})")
            if "layer" in want and got["layer"] != want["layer"]:
                yield (f"{body}: ore vein {vein_id} names layer {got['layer']}, "
                       f"expected {want['layer']}")
            if "weight" in want and got["weight"] != want["weight"]:
                yield (f"{body}: ore vein {vein_id} has weight {got['weight']}, "
                       f"expected {want['weight']}")
            # A vein on a layer that does not reach this dimension generates nothing,
            # however well-formed both halves are on their own.
            layer = layers.get(got["layer"], {})
            if dimension not in layer.get("dimensions", []):
                yield (f"{body}: layer {got['layer']} does not cover {dimension}, "
                       f"so {vein_id} cannot generate there")

        # A structure loads, lists biomes and is placed by a set, and each of the three can
        # be wrong on its own without the other two noticing. The block list is the fourth
        # failure: a template naming an id that does not exist places air and says nothing.
        for structure_id, want in spec.get("structures", {}).items():
            got = structures.get(structure_id)
            if got is None:
                yield f"{body}: structure {structure_id} did not load"
                continue
            emitted_here = biomes.get(dimension) or []
            for biome_id in want.get("biomes", []):
                if biome_id not in got["biomes"]:
                    yield (f"{body}: structure {structure_id} does not list biome "
                           f"{biome_id}")
                elif emitted_here and biome_id not in emitted_here:
                    yield (f"{body}: structure {structure_id} lists biome {biome_id}, "
                           f"which {dimension}'s generator never emits")
            for block_id in want.get("blocks", []):
                if block_id not in blocks:
                    yield (f"{body}: structure {structure_id} is built from {block_id}, "
                           f"which is not a registered block -- it will place air")
            want_set = want.get("set")
            if want_set is not None:
                got_set = structure_sets.get(want_set)
                if got_set is None:
                    yield f"{body}: structure set {want_set} did not load"
                elif structure_id not in got_set["structures"]:
                    yield (f"{body}: structure set {want_set} does not contain "
                           f"{structure_id}")
                else:
                    # The placement is dumped as the JSON its own codec produces, so a
                    # fixture names only the fields it cares about and stays indifferent to
                    # which placement subclass carries them.
                    got_placement = got_set.get("placement") or {}
                    for field, value in want.get("placement", {}).items():
                        if got_placement.get(field) != value:
                            yield (f"{body}: structure set {want_set} has placement "
                                   f"{field} {got_placement.get(field)!r}, expected "
                                   f"{value!r}")

        for vein_id in spec.get("forbidden_ore_veins", []):
            got = veins.get(vein_id)
            if got is not None and dimension in got["dimensions"]:
                yield f"{body}: ore vein {vein_id} leaks onto {dimension}"

        for deposit_id, want in spec.get("bedrock_ores", {}).items():
            got = deposits.get(deposit_id)
            if got is None:
                yield f"{body}: bedrock ore deposit {deposit_id} did not load"
                continue
            if dimension not in got["dimensions"]:
                yield (f"{body}: bedrock ore deposit {deposit_id} does not filter to "
                       f"{dimension} (filters to {got['dimensions']})")
            materials = [entry["material"] for entry in got["materials"]]
            if "materials" in want and sorted(materials) != sorted(want["materials"]):
                yield (f"{body}: bedrock ore deposit {deposit_id} yields {materials}, "
                       f"expected {want['materials']}")
            # A depleted deposit that yields nothing is an exhausted vein with extra
            # steps, which is the one thing a bedrock deposit must never be.
            floor = want.get("depleted_yield_at_least")
            if floor is not None and got["depleted_yield"] < floor:
                yield (f"{body}: bedrock ore deposit {deposit_id} depletes to "
                       f"{got['depleted_yield']}, expected at least {floor}")

        for deposit_id in spec.get("forbidden_bedrock_ores", []):
            got = deposits.get(deposit_id)
            if got is not None and dimension in got["dimensions"]:
                yield f"{body}: bedrock ore deposit {deposit_id} leaks onto {dimension}"

        for deposit_id, want in spec.get("bedrock_fluids", {}).items():
            got = fluids.get(deposit_id)
            if got is None:
                yield f"{body}: bedrock fluid deposit {deposit_id} did not load"
                continue
            if dimension not in got["dimensions"]:
                yield (f"{body}: bedrock fluid deposit {deposit_id} does not filter to "
                       f"{dimension} (filters to {got['dimensions']})")
            if "fluid" in want and got["fluid"] != want["fluid"]:
                yield (f"{body}: bedrock fluid deposit {deposit_id} holds "
                       f"{got['fluid']}, expected {want['fluid']}")
            floor = want.get("depleted_yield_at_least")
            if floor is not None and got["depleted_yield"] < floor:
                yield (f"{body}: bedrock fluid deposit {deposit_id} depletes to "
                       f"{got['depleted_yield']}, expected at least {floor}")

        for deposit_id in spec.get("forbidden_bedrock_fluids", []):
            got = fluids.get(deposit_id)
            if got is not None and dimension in got["dimensions"]:
                yield f"{body}: bedrock fluid deposit {deposit_id} leaks onto {dimension}"


def _headless_guard():
    """The headless() context manager out of launch.py, which is not importable by name."""
    spec = importlib.util.spec_from_file_location("pf_launch", INSTANCE / "scripts/launch.py")
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod.headless()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--timeout", type=int, default=600,
                        help="seconds to wait for the dump (default: 600)")
    parser.add_argument("--keep-world", action="store_true",
                        help="leave saves/%s in place afterwards" % WORLD_NAME)
    parser.add_argument("--dump-only", action="store_true",
                        help="compare the existing dump without launching the game")
    parser.add_argument("--headless", action="store_true",
                        help="run with no display (remote session, CI) — see launch.py")
    args = parser.parse_args()

    if not args.dump_only:
        fresh_world()
        # run_game kills the whole process group, which takes launch.py down with the
        # game and skips the restore in its own headless() finally -- leaving
        # config/fml.toml patched. Hold the guard out here instead, where nothing is
        # killing us. launch.py keeps its own for standalone use; nesting is harmless
        # because the inner one restores to whatever the outer one set.
        with _headless_guard() if args.headless else contextlib.nullcontext():
            ok = run_game(args.timeout, args.headless)
        if not ok:
            print(f"no registry dump after {args.timeout}s — see logs/latest.log", file=sys.stderr)
            return 2
        if not args.keep_world and WORLD.exists():
            shutil.rmtree(WORLD)

    dump = load_dump()
    if dump is None:
        print(f"no dump at {DUMP}, and none in logs/latest.log", file=sys.stderr)
        return 2

    failures = list(compare(dump, json.loads(EXPECTED.read_text())))
    if failures:
        print(f"{len(failures)} worldgen expectation(s) failed:")
        for failure in failures:
            print(f"  {failure}")
        return 1

    print("worldgen registries match tests/worldgen/expected.json")
    return 0


if __name__ == "__main__":
    sys.exit(main())
