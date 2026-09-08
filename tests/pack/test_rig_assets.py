#!/usr/bin/env python3
"""Assert both mining rigs' generated halves still agree with what registers them (#192, #193).

`docs/testing/what-to-check.md`'s "cross-file references resolve" claim, for the rig seam
(ADR-0043). Two different generated things are asserted here, and they fail in different ways:

  - **Every corpus number.** `scripts/build-rig-assets.py` copies a `drills` row out of
    `data/factorio/machine.json` (#188, widened by #193) into a resource the mod reads at
    class-init: the footprint, the mining speed, the wattage, the output vector and the searching
    radius. That copy is the one place ADR-0041's "every number is extracted, and none is chosen"
    could be quietly broken -- a hand-edited resource would place a 3x3 burner rig with nothing
    failing, since the mod's own `RigCorpusTest` only asserts the numbers it reads and would read
    the edit. So the resource is asserted against the corpus itself, field by field, and never
    against expected literals.
  - **The vertical extent.** How many blocks a rig stands is the one figure the corpus cannot
    supply: Factorio is played on a plane and a prototype states `tile_width` and `tile_height`,
    both ground extent. ADR-0043 carries the declared exception; what is asserted here is only that
    it has not silently gone back to one, which is what read as a platform rather than a machine.
  - **That the drill is obtainable and fuellable.** The item-map row has to name the block the mod
    actually registers, and something at rung 0 has to burn: a rig nothing can fuel is a rig that
    never turns, and neither half fails anywhere else. The fuel table is default-deny and category
    filtered, so "there are fuel files" is not the assertion -- "at least one names a `chemical`
    fuel against a real item" is.
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
DRILLS = ROOT / "mod/src/main/resources/planetaryfactory_core/mining/drills.json"
ITEM_MAP = ROOT / "data/pack/item-map.json"
FUEL = ROOT / "kubejs/data/planetaryfactory/fuel"
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


def check_item_map(tiers, item_map, failures):
    """A decided rig must name the block the mod registers.

    `tests/factorio/test_recipe_convert.py` already asserts that a `planetaryfactory:` target is
    registered somewhere; what it cannot see is whether *this* rig's row names *this* rig. A row
    pointing at the other rung would resolve, emit a recipe and hand the player the wrong machine.
    An `undecided` row is left alone -- the electric rig is #194's to decide.
    """
    for tier, (factorio_name, _) in sorted(tiers.items()):
        row = item_map.get(factorio_name)
        if row is None:
            failures.append(
                f"{factorio_name} has no item-map row at all -- an unmapped corpus name is a hard "
                "failure, not a skip (#72)"
            )
            continue
        if row.get("status") == "undecided":
            continue
        expected = f"planetaryfactory:{tier}_mining_drill"
        if row.get("target") != expected:
            failures.append(
                f"{factorio_name} maps onto {row.get('target')!r}, but {tier} registers "
                f"{expected} -- the row names a different machine than the one it is"
            )


def check_fuel_reaches_a_burner(rows, failures):
    """A rig that burns fuel must have something to burn at rung 0.

    ADR-0047's table is default-deny and category filtered, so this cannot be read off "there are
    files in the fuel folder": a table full of `nuclear` rows would leave a burner rig unfuellable
    with every one of them present and valid. What has to hold is that a rig's own
    `fuel_categories` intersect a row that names a real item -- and rung 0 has exactly one answer,
    coal, which is why nothing here would survive the fuel converter quietly dropping it.
    """
    if not FUEL.is_dir():
        failures.append(f"{FUEL.relative_to(ROOT)} does not exist -- run the fuel converter")
        return
    table = [json.loads(path.read_text()) for path in sorted(FUEL.glob("*.json"))]
    for factorio_name, row in sorted(rows.items()):
        categories = set(row.get("fuel_categories") or [])
        if not categories:
            continue
        burnable = [
            fuel for fuel in table
            if fuel.get("fuel_category") in categories
            and (fuel.get("item") or fuel.get("tag"))
            and (fuel.get("fuel_value") or 0) > 0
        ]
        if not burnable:
            failures.append(
                f"{factorio_name} burns {sorted(categories)} and the fuel table names nothing in "
                "those categories -- the rig would never turn"
            )


def check_screen_lang(lang, failures):
    """The rig screen's own strings. A missing one ships a raw translation key on the hover."""
    for key in ("tooltip.planetaryfactory.rig.fuel",
                "tooltip.planetaryfactory.rig.fuel.seconds",
                "tooltip.planetaryfactory.rig.fuel.out"):
        if not lang.get(key):
            failures.append(f"{key} has no lang entry -- the fuel hover would show its raw key")


def main():
    failures = []
    tiers = registered_tiers()
    corpus = {row["name"]: row for row in json.loads(MACHINE_CORPUS.read_text())["drills"]}
    rows = json.loads(DRILLS.read_text())["drills"]
    item_map = json.loads(ITEM_MAP.read_text())["items"]
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
        mod_row = rows.get(factorio_name)
        if mod_row is None:
            failures.append(f"{factorio_name} has no row in drills.json -- re-run the generator")
        else:
            # Field by field against the corpus, never against literals. A generator patched to
            # emit numbers somebody chose passes its own --check and fails here, which is the whole
            # point of asserting against the source rather than against an expectation.
            for field in ("tile_width", "tile_height", "mining_speed", "energy_usage",
                          "energy_type", "vector_to_place_result", "resource_searching_radius"):
                if mod_row.get(field) != row.get(field):
                    failures.append(
                        f"{factorio_name}'s {field} is {mod_row.get(field)!r} in drills.json but "
                        f"{row.get(field)!r} in the corpus -- every number here is extracted"
                    )
            corpus_categories = (row.get("burner") or {}).get("fuel_categories")
            if mod_row.get("fuel_categories") != corpus_categories:
                failures.append(
                    f"{factorio_name} admits {mod_row.get('fuel_categories')!r} but the corpus "
                    f"says {corpus_categories!r}"
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
            seen_rotations = set()
            for variant, definition in variants.items():
                entry = definition if isinstance(definition, dict) else definition[0]
                model = entry["model"]
                seen_rotations.add(entry.get("y", 0))
                model_path = ASSETS / f"models/{model.split(':', 1)[-1]}.json"
                if not resolves(model_path):
                    failures.append(f"{name}[{variant}] names model {model}, which is missing")
                    continue
                # THE FACING HAS TO BE VISIBLE ON THE BLOCK. ADR-0043 gives a rig one output tile
                # and makes placement a decision the player gets right or wrong; a `cube_all` model
                # renders every side the same, so a mis-faced rig looks exactly like a correct one
                # until it fails to fill anything. Nothing else in the pack can catch that -- the
                # blockstate rotates a model that does not care, and every other hop still resolves.
                declared = json.loads(model_path.read_text())
                if not (declared.get("textures") or {}).get("front"):
                    failures.append(
                        f"{name}'s model {model} declares no `front` texture -- its facing would "
                        "be invisible, and a mis-faced rig would look identical to a correct one"
                    )
                for slot, reference in (declared.get("textures") or {}).items():
                    texture = texture_path(reference)
                    if texture is not None and not resolves(texture):
                        failures.append(f"{model}'s {slot} texture {reference} is missing")

            if seen_rotations != {0, 90, 180, 270}:
                failures.append(
                    f"{name}'s blockstate turns the model through {sorted(seen_rotations)} rather "
                    "than all four quarters -- the front face would point the wrong way on "
                    "at least one facing"
                )
            if f"block.planetaryfactory.{name}" not in lang:
                failures.append(f"{name} has no lang key -- it would ship its raw translation key")
            if not resolves(DATA / f"loot_table/blocks/{name}.json"):
                failures.append(f"{name} has no loot table -- it would break into nothing")

        if not resolves(ASSETS / f"models/item/{block}.json"):
            failures.append(f"{block} has no item model")

    stray = set(rows) - {factorio for factorio, _ in tiers.values()}
    if stray:
        failures.append(
            f"drills.json carries {sorted(stray)}, which no registered tier reads -- "
            "a tier removed from the enum leaves its row behind"
        )

    check_item_map(tiers, item_map, failures)
    check_fuel_reaches_a_burner(rows, failures)
    check_screen_lang(lang, failures)

    for index, failure in enumerate(failures, 1):
        print(f"FAIL {index}: {failure}")
    if failures:
        return 1
    print(f"ok   {len(tiers)} rigs, every number matches the corpus, every asset hop resolves")
    return 0


if __name__ == "__main__":
    sys.exit(main())
