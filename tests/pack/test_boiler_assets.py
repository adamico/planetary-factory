#!/usr/bin/env python3
"""Assert Terra's Boiler has the pack-side files it needs, and the numbers it was built on (#224).

This is a `planetaryfactory:` block, so **GregTech's model provider does not serve it** -- every
hop from blockstate to model to texture is the pack's own, and a missing one reaches the player as
a purple-and-black cube with no error in any log. The `test_machine_assets.py` and
`test_pump_assets.py` pattern, applied to the one block ADR-0048 puts at the head of the steam
chain.

Four things are asserted, and each fails differently:

  - **The asset hops.** Blockstate covering every `facing`, a model per variant, an item model, a
    lang key and a loot table. A Boiler with no loot table is a machine that vanishes when broken,
    and it holds the coal it was burning.
  - **The item-map row.** `boiler` is `authored` and names this block. The converter hard-fails on
    an unmapped name, so what this adds is the other direction: that the row's target is the block
    the mod actually registers, and that the row is no longer `undecided` -- one of the three rows
    #166 tracks.
  - **The generator is current.** `scripts/build-steam-assets.py --check`, so a corpus refresh that
    moved a number fails here rather than shipping a stale copy.
  - **The rate is still Factorio's own.** 1.8 MW and 165 °C into 60 mB a second, re-derived here
    from the corpus rather than read off the Java -- this is the assertion that would catch a
    prototype change quietly re-rating the block. The heat capacity used is *steam's*; water's is
    ten times larger and yields a plausible-looking 6 mB/s. The mod's own arithmetic is
    `BoilerSpecTest`, and the two are deliberately independent derivations of one number.

Whether a placed Boiler actually boils water is a world load, not a static check.

Usage: tests/pack/test_boiler_assets.py
"""

import json
import pathlib
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
ASSETS = ROOT / "kubejs/assets/planetaryfactory"
DATA = ROOT / "kubejs/data/planetaryfactory"
MACHINE_CORPUS = ROOT / "data/factorio/machine.json"
FLUID_CORPUS = ROOT / "data/factorio/fluid.json"
ITEM_MAP = ROOT / "data/pack/item-map.json"
GENERATOR = ROOT / "scripts/build-steam-assets.py"
PF_BLOCKS = ROOT / "mod/src/main/java/com/planetaryfactory/core/PFBlocks.java"

BLOCK = "boiler"
BLOCK_ID = f"planetaryfactory:{BLOCK}"
FACINGS = ("north", "east", "south", "west")

# The screen's own keys, plus the block name. A missing tooltip key renders the raw key on the
# gauge that exists to say why the machine has stopped.
LANG_KEYS = (
    f"block.planetaryfactory.{BLOCK}",
    "tooltip.planetaryfactory.boiler.fuel",
    "tooltip.planetaryfactory.boiler.fuel.seconds",
    "tooltip.planetaryfactory.boiler.fuel.out",
    "tooltip.planetaryfactory.boiler.water",
    "tooltip.planetaryfactory.boiler.steam",
)

MINECRAFT_TICKS_PER_SECOND = 20


def texture_exists(texture):
    """A `namespace:block/name` reference, resolved against the pack and against vanilla.

    Vanilla's own textures are not on disk here, so a `minecraft:` reference is taken on trust --
    the Boiler borrows the blast furnace's art, the way the pump borrows the dispenser's.
    """
    namespace, _, path = texture.partition(":")
    if namespace == "minecraft":
        return True
    return (ROOT / f"kubejs/assets/{namespace}/textures/{path}.png").is_file()


def check_assets(failures):
    blockstate_path = ASSETS / f"blockstates/{BLOCK}.json"
    if not blockstate_path.is_file():
        failures.append(f"{blockstate_path.relative_to(ROOT)} is missing -- run the generator")
        return
    variants = json.loads(blockstate_path.read_text()).get("variants", {})
    for facing in FACINGS:
        key = f"facing={facing}"
        if key not in variants:
            failures.append(f"the blockstate has no {key} variant -- that facing renders as nothing")
            continue
        model = variants[key].get("model")
        model_path = ASSETS / f"models/{model.split(':', 1)[1]}.json"
        if not model_path.is_file():
            failures.append(f"{key} names {model}, which is not on disk")
            continue
        for role, texture in json.loads(model_path.read_text()).get("textures", {}).items():
            if not texture_exists(texture):
                failures.append(f"the block model's {role} texture {texture} is not on disk")

    item_model = ASSETS / f"models/item/{BLOCK}.json"
    if not item_model.is_file():
        failures.append(f"{item_model.relative_to(ROOT)} is missing -- the item renders as nothing")

    loot_table = DATA / f"loot_table/blocks/{BLOCK}.json"
    if not loot_table.is_file():
        failures.append(
            f"{loot_table.relative_to(ROOT)} is missing -- a broken Boiler would drop nothing"
        )
    else:
        dropped = json.dumps(json.loads(loot_table.read_text()))
        if BLOCK_ID not in dropped:
            failures.append(f"the loot table does not drop {BLOCK_ID}")

    lang = json.loads((ASSETS / "lang/en_us.json").read_text())
    for key in LANG_KEYS:
        if not lang.get(key):
            failures.append(f"{key} has no lang entry -- it would render as its raw key")


def check_item_map(failures):
    row = json.loads(ITEM_MAP.read_text())["items"].get(BLOCK)
    if row is None:
        failures.append("item-map.json has no boiler row at all")
        return
    if row.get("status") == "undecided":
        failures.append(
            "the boiler row is still `undecided` -- #224 lands the block, so the row #166 tracks "
            "is decided now"
        )
    if row.get("target") != BLOCK_ID:
        failures.append(
            f"the boiler row targets {row.get('target')!r} rather than {BLOCK_ID} -- ADR-0048 "
            "replaces the LP Solid Boiler with a pack-authored block"
        )
    if "gtceu" in json.dumps(row.get("target", "")):
        failures.append(
            "the boiler row points back at GregTech -- ADR-0048 is explicit that a GT boiler "
            "emitting gtceu:steam re-opens the power layer #37 removed"
        )


def check_registered(failures):
    """That the mod registers the id the row names. A row naming nothing is an empty slot."""
    source = PF_BLOCKS.read_text(encoding="utf-8")
    if f'BLOCKS.register("{BLOCK}"' not in source:
        failures.append(
            f"{PF_BLOCKS.relative_to(ROOT)} does not register {BLOCK_ID} -- the item-map row "
            "names a block that does not exist"
        )


def check_rate(failures):
    """Factorio's own numbers into 60 mB a second, derived here and in BoilerSpec independently."""
    machine = json.loads(MACHINE_CORPUS.read_text())
    boiler = next((row for row in machine.get("boilers", []) if row["name"] == BLOCK), None)
    fluids = {row["name"]: row for row in json.loads(FLUID_CORPUS.read_text()).get("fluids", [])}
    if boiler is None or "steam" not in fluids or "water" not in fluids:
        failures.append("the corpus no longer carries the boiler row or its two fluids")
        return

    # Steam's heat capacity, not water's: Factorio pays for the rise at the OUTPUT fluid's rate.
    per_unit = (boiler["target_temperature"] - fluids["water"]["default_temperature"]) * fluids[
        "steam"
    ]["heat_capacity"]
    per_second = boiler["energy_consumption"] / per_unit
    if per_second != 60:
        failures.append(
            f"the corpus now implies {per_second} mB/s rather than Factorio's 60 -- ADR-0050's "
            "'one pump feeds twenty boilers' was written against 60"
        )
    per_tick = boiler["energy_consumption"] / MINECRAFT_TICKS_PER_SECOND / per_unit
    if per_tick != int(per_tick):
        failures.append(
            f"a Minecraft tick no longer converts a whole number of millibuckets ({per_tick}) -- "
            "BoilerSpec's integer arithmetic would silently round the rate down"
        )


def main():
    failures = []
    generated = subprocess.run(
        [sys.executable, str(GENERATOR), "--check"], capture_output=True, text=True
    )
    if generated.returncode != 0:
        failures.append(
            f"{GENERATOR.relative_to(ROOT)} --check: "
            f"{(generated.stdout + generated.stderr).strip()}"
        )

    check_assets(failures)
    check_item_map(failures)
    check_registered(failures)
    check_rate(failures)

    if failures:
        print(f"FAIL {len(failures)}:")
        for failure in failures:
            print(f"  - {failure}")
        return 1
    print("ok   boiler: every asset hop resolves, the item-map row names the block, "
          "and the corpus still implies 60 mB/s")
    return 0


if __name__ == "__main__":
    sys.exit(main())
