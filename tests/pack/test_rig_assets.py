#!/usr/bin/env python3
"""Assert both mining rigs' generated halves still agree with what registers them (#192).

`docs/testing/what-to-check.md`'s "cross-file references resolve" claim, for the rig seam
(ADR-0043). Two different generated things are asserted here, and they fail in different ways:

  - **The footprint sizes.** `scripts/build-rig-assets.py` copies `tile_width`/`tile_height` out of
    `data/factorio/machine.json`'s `drills` rows (#188) into a resource the mod reads at class-init.
    That copy is the one place ADR-0041's "every number is extracted, and none is chosen" could be
    quietly broken: a hand-edited resource would place a 3x3 burner rig with nothing failing, since
    the mod's own `RigFootprintsTest` only asserts the numbers it reads and would read the edit.
    So the resource is asserted against the corpus itself, not against the expected integers.
  - **The vertical extent.** How many blocks a rig stands is the one figure the corpus cannot
    supply: Factorio is played on a plane and a prototype states `tile_width` and `tile_height`,
    both ground extent. ADR-0043 carries the declared exception; what is asserted here is only that
    it has not silently gone back to one, which is what read as a platform rather than a machine.
  - **The pack-side files.** Both rigs are `planetaryfactory:` blocks, so GregTech's model provider
    does not serve them and every hop is ours: blockstate to model to texture, a lang key, and a
    loot table. Each way of breaking those fails quietly -- an untextured black-and-magenta cube
    with a client-side warning, a raw translation key as the block's name, or a block that breaks
    into nothing and reads as a game bug rather than a packaging one.

The tier list is read out of `RigTier.java` rather than typed here, so a third rung added to the
enum fails this check instead of silently shipping without assets.

Usage: tests/pack/test_rig_assets.py
"""

import json
import pathlib
import re
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
RIG_TIER = ROOT / "mod/src/main/java/com/planetaryfactory/core/mining/rig/RigTier.java"
FOOTPRINT = ROOT / "mod/src/main/resources/planetaryfactory_core/mining/footprint.json"
MACHINE_CORPUS = ROOT / "data/factorio/machine.json"
GENERATOR = ROOT / "scripts/build-rig-assets.py"
ASSETS = ROOT / "kubejs/assets/planetaryfactory"
DATA = ROOT / "kubejs/data/planetaryfactory"

# `BURNER("burner-mining-drill", 2),` -- the enum constant, the corpus key it reads its ground size
# from, and the one number here the corpus cannot supply: how many blocks tall it stands.
TIER_RE = re.compile(r'^\s{4}([A-Z][A-Z_]*)\("([a-z-]+)",\s*(\d+)\)[,;]', re.MULTILINE)


def registered_tiers():
    tiers = TIER_RE.findall(RIG_TIER.read_text(encoding="utf-8"))
    if not tiers:
        raise AssertionError(f"no rig tiers parsed out of {RIG_TIER} -- has the enum moved?")
    return {name.lower(): (factorio, int(tall)) for name, factorio, tall in tiers}


def resolves(path):
    return path.is_file()


def texture_path(reference):
    namespace, _, name = reference.partition(":")
    if not name:
        namespace, name = "minecraft", reference
    if namespace == "minecraft":
        # Vanilla's own art, served from the jar; nothing pack-side to resolve.
        return None
    return ROOT / f"kubejs/assets/{namespace}/textures/{name}.png"


def main():
    failures = []
    tiers = registered_tiers()
    corpus = {row["name"]: row for row in json.loads(MACHINE_CORPUS.read_text())["drills"]}
    footprints = json.loads(FOOTPRINT.read_text())["drills"]
    lang = json.loads((ASSETS / "lang/en_us.json").read_text())

    # The generator is the authority on its own output; if it is stale, everything below is
    # asserting yesterday's files.
    generated = subprocess.run(
        [sys.executable, str(GENERATOR), "--check"], capture_output=True, text=True
    )
    if generated.returncode != 0:
        failures.append(
            f"{GENERATOR.relative_to(ROOT)} --check: "
            f"{(generated.stdout + generated.stderr).strip()}"
        )

    for tier, (factorio_name, blocks_tall) in sorted(tiers.items()):
        # The vertical extent is chosen, not extracted -- Factorio states two ground figures and
        # no third -- so it is asserted here rather than against the corpus. A rig one block tall
        # is the thing that read as a platform rather than as a machine, and nothing upstream of
        # this file can catch a silent return to it. ADR-0043 carries the declared exception.
        if blocks_tall < 2:
            failures.append(
                f"{tier} stands {blocks_tall} block tall -- a one-block rig reads as a platform; "
                "see ADR-0043's declared exception"
            )
        row = corpus.get(factorio_name)
        if row is None:
            failures.append(
                f"{tier} names {factorio_name!r}, which is not a drill in "
                f"{MACHINE_CORPUS.relative_to(ROOT)} -- re-run the machine extractor"
            )
            continue
        size = footprints.get(factorio_name)
        if size is None:
            failures.append(f"{factorio_name} has no footprint row -- re-run the generator")
        elif (size["tile_width"], size["tile_height"]) != (row["tile_width"], row["tile_height"]):
            failures.append(
                f"{factorio_name}'s footprint is "
                f"{size['tile_width']}x{size['tile_height']}, but the corpus says "
                f"{row['tile_width']}x{row['tile_height']} -- the size is extracted, not chosen"
            )

        block = f"{tier}_mining_drill"
        for name in (block, f"{block}_part"):
            blockstate = ASSETS / f"blockstates/{name}.json"
            if not resolves(blockstate):
                failures.append(f"{name} has no blockstate")
                continue
            variants = json.loads(blockstate.read_text()).get("variants") or {}
            if not variants:
                failures.append(f"{name}'s blockstate declares no variants")
            for variant, definition in variants.items():
                model = (definition if isinstance(definition, dict) else definition[0])["model"]
                model_path = ASSETS / f"models/{model.split(':', 1)[-1]}.json"
                if not resolves(model_path):
                    failures.append(f"{name}[{variant}] names model {model}, which is missing")
                    continue
                for slot, reference in (json.loads(model_path.read_text()).get("textures") or {}).items():
                    texture = texture_path(reference)
                    if texture is not None and not resolves(texture):
                        failures.append(f"{model}'s {slot} texture {reference} is missing")

            if f"block.planetaryfactory.{name}" not in lang:
                failures.append(f"{name} has no lang key -- it would ship its raw translation key")
            if not resolves(DATA / f"loot_table/blocks/{name}.json"):
                failures.append(f"{name} has no loot table -- it would break into nothing")

        if not resolves(ASSETS / f"models/item/{block}.json"):
            failures.append(f"{block} has no item model")

    stray = set(footprints) - {factorio for factorio, _ in tiers.values()}
    if stray:
        failures.append(
            f"footprint.json carries {sorted(stray)}, which no registered tier reads -- "
            "a tier removed from the enum leaves its row behind"
        )

    for index, failure in enumerate(failures, 1):
        print(f"FAIL {index}: {failure}")
    if failures:
        return 1
    print(f"ok   {len(tiers)} rigs, footprints match the corpus, every asset hop resolves")
    return 0


if __name__ == "__main__":
    sys.exit(main())
