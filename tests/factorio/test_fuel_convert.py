#!/usr/bin/env python3
"""Assert the generated fuel table is the join it claims to be, and that the mod reads it.

`scripts/factorio-fuel-convert.py` joins `data/factorio/fuel.json` onto `data/pack/item-map.json`
and writes `kubejs/data/planetaryfactory/fuel/*.json`, which is what a burner furnace burns
(ADR-0047, #187). What is checkable without launching the game:

  - **the emitted table is not stale.** The converter's own `--check`, the rule that generated
    output is never hand-edited.
  - **every fuel with a decided item-map row has a row, and nothing else does.** Under ADR-0034's
    default-deny sweep an item is not fuel because it is vanilla and burnable; it is fuel because
    Factorio gave it a `fuel_value` and `item-map.json` says what it is on Terra. A fuel with
    neither an emitted row nor a recorded reason is the silence the join exists to prevent.
  - **`uranium-fuel-cell` does not resolve, on category.** Two independent gates, because #135
    will remove the first one: it has no decided item-map row today, and its category is `nuclear`
    which `FuelTable` refuses. No emitted row is anything but `chemical`.
  - **the burn duration is re-derived, not stored.** Coal's row divided by `stone-furnace`'s own
    `energy_usage / 20` is 888 whole ticks, and the Steel tier -- same watts, half the craft --
    gets exactly twice the crafts. `test_fuel_extract.py` makes the same division on the corpus;
    this one makes it on what the *game* will read, which is the artifact a player meets.
  - **`wood` reaches the table as a tag.** `minecraft:logs`, not one species and not an item row,
    or every log but one stops burning.
  - **the mod reads this folder, in this namespace.** The listener's folder and the emitted path
    are one string in two files; a rename in either is a table that silently loads nothing.
  - **the vanilla burn table is gone, and so is the flame.** `getBurnTime` reached for a
    default-allow alphabet, and the flame widget was drawn from a burn-tick fraction that no
    longer exists.

Usage: tests/factorio/test_fuel_convert.py
"""
import json
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent.parent
OUT_DIR = ROOT / "kubejs/data/planetaryfactory/fuel"
CONVERTER = ROOT / "scripts/factorio-fuel-convert.py"
SMELTING = ROOT / "mod/src/main/java/com/planetaryfactory/core/smelting"

TICKS_PER_SECOND = 20

# The one category a Factorio furnace burns. `test_fuel_extract.py` asserts the two burner
# machines' own `fuel_categories` say so; this asserts the emitted table agrees.
CHEMICAL = "chemical"

# ADR-0047's worked example: coal on the Stone tier, and the steel-plate craft it is counted in.
REFERENCE_RECIPE = "steel-plate"
BURNER_FURNACES = ("stone-furnace", "steel-furnace")


def load(relative):
    return json.loads((ROOT / relative).read_text(encoding="utf-8"))


def reference_craft():
    """steel-plate's emitted length in ticks, from the corpus's own `energy_required` seconds."""
    for recipe in load("data/factorio/recipe.json"):
        if recipe["name"] == REFERENCE_RECIPE:
            return recipe["energy_required"] * TICKS_PER_SECOND
    return None


def expected_names(fuels, items):
    """The fuels a row is owed to: a decided, non-fluid item-map row and nothing else."""
    owed = set()
    for fuel in fuels:
        row = items.get(fuel["name"])
        if not row or row.get("status") == "undecided" or "blocked_by" in row:
            continue
        if row["kind"] == "fluid":
            continue
        owed.add(fuel["name"])
    return owed


def main():
    failures = []

    fuel_data = load("data/factorio/fuel.json")
    fuels = {f["name"]: f for f in fuel_data["fuels"]}
    items = load("data/pack/item-map.json")["items"]
    machines = {m["name"]: m for m in load("data/factorio/machine.json")["machines"]}

    if not OUT_DIR.is_dir():
        print(f"FAIL 1: {OUT_DIR.relative_to(ROOT)} does not exist -- run {CONVERTER.name}")
        return 1
    emitted = {p.stem: json.loads(p.read_text(encoding="utf-8")) for p in OUT_DIR.glob("*.json")}

    run = subprocess.run([sys.executable, str(CONVERTER), "--check", "--quiet"],
                         capture_output=True, text=True)
    if run.returncode != 0:
        failures.append(f"{CONVERTER.name} --check failed: {run.stdout.strip() or run.stderr.strip()}")

    by_factorio_name = {}
    for stem, row in sorted(emitted.items()):
        name = row.get("factorio_name")
        if name not in fuels:
            failures.append(f"{stem}.json names {name!r}, which is not a fuel in the corpus")
            continue
        by_factorio_name[name] = row
        if stem != name.replace("-", "_"):
            failures.append(f"{stem}.json holds {name!r}, so the file name is not its id")
        if row.get("fuel_category") != CHEMICAL:
            failures.append(
                f"{name} is emitted as {row.get('fuel_category')!r} -- only {CHEMICAL} burns"
            )
        if row["fuel_value"] != int(fuels[name]["fuel_value"]):
            failures.append(
                f"{name} carries {row['fuel_value']} J, but the corpus says "
                f"{int(fuels[name]['fuel_value'])}"
            )
        target = items[name]["target"]
        key = "tag" if items[name]["kind"] == "tag" else "item"
        if row.get(key) != target:
            failures.append(f"{name} is emitted as {row!r}, not {key} {target!r}")

    owed = expected_names(fuel_data["fuels"], items)
    for name in sorted(owed - set(by_factorio_name)):
        failures.append(f"{name} has a decided item-map row and no fuel table row")
    for name in sorted(set(by_factorio_name) - owed):
        failures.append(f"{name} burns, and no decided item-map row says what it is")

    # #135's item, checked at both gates -- the second is the one that survives it landing.
    cell = fuels.get("uranium-fuel-cell")
    if not cell:
        failures.append("uranium-fuel-cell is not in the corpus -- the category gate is untested")
    elif cell["fuel_category"] == CHEMICAL:
        failures.append("uranium-fuel-cell is chemical in the corpus, so nothing stops it burning")
    if "uranium-fuel-cell" in by_factorio_name:
        failures.append("uranium-fuel-cell has a fuel table row -- it is #135's nuclear item")

    # The arithmetic, on what the game reads. Both terms come out of committed data.
    coal = by_factorio_name.get("coal")
    stone, steel = (machines.get(n) or {} for n in BURNER_FURNACES)
    reference = reference_craft()
    if not coal:
        failures.append("coal has no fuel table row -- ADR-0047's worked example has no numerator")
    elif not stone.get("energy_usage") or reference is None:
        failures.append("stone-furnace or steel-plate is missing -- the arithmetic has no terms")
    else:
        per_tick = stone["energy_usage"] / TICKS_PER_SECOND
        ticks = int(coal["fuel_value"] // per_tick)
        if per_tick != 4500 or ticks != 888:
            failures.append(
                f"coal's row buys {ticks} ticks at {per_tick} J/t, not 888 at 4500"
            )
        crafts = {}
        for label, machine in (("stone", stone), ("steel", steel)):
            per_craft = (machine["energy_usage"] / TICKS_PER_SECOND) * (
                reference / machine.get("crafting_speed", 1))
            crafts[label] = coal["fuel_value"] / per_craft
        if abs(crafts["steel"] - 2 * crafts["stone"]) > 1e-9:
            failures.append(
                f"coal's row yields {crafts['stone']:.3f} {REFERENCE_RECIPE} crafts on Stone and "
                f"{crafts['steel']:.3f} on Steel -- not the doubling ADR-0047 makes arithmetic"
            )

    wood = by_factorio_name.get("wood")
    if not wood:
        failures.append("wood has no fuel table row")
    elif wood.get("tag") != "minecraft:logs":
        failures.append(
            f"wood is emitted as {wood!r} -- ADR-0047 resolves it through minecraft:logs, so an "
            "item row here stops every log but one burning"
        )

    # The folder and the namespace are one string in two files. A rename in either is a table
    # that loads nothing, in a game that reports no error because an absent fuel is not fuel.
    listener = (SMELTING / "PFFuel.java").read_text(encoding="utf-8")
    if f'super(GSON, "{OUT_DIR.name}")' not in listener:
        failures.append(
            f"PFFuel does not read the {OUT_DIR.name!r} folder the converter writes"
        )
    if "NAMESPACE.equals(file.getKey().getNamespace())" not in listener:
        failures.append("PFFuel no longer restricts the table to the pack's own namespace")

    # The two removals ADR-0047 names.
    for path in sorted(SMELTING.rglob("*.java")):
        text = path.read_text(encoding="utf-8")
        if "getBurnTime" in text:
            failures.append(
                f"{path.name} still asks Forge's vanilla burn table, which is a default-allow "
                "alphabet under ADR-0034's default-deny sweep"
            )
        if "lit_progress" in text:
            failures.append(
                f"{path.name} still draws the flame, which was a fraction of a burn duration "
                "that no longer exists"
            )

    # The buffer persists or it does not; there is no third outcome and no error either way.
    # A field saved and not loaded is a furnace that comes back cold with the coal gone, which is
    # the silent loss ADR-0038 asks the assembler's codecs to catch. The block entity is a
    # Minecraft type and the unit test source set has no Minecraft, so this is a source-text check
    # -- the same reason `tests/pack/test_smelting_type.py` is one.
    entity = (SMELTING / "FurnaceBlockEntity.java").read_text(encoding="utf-8")
    for key in ("FuelJoules", "FuelLitJoules"):
        if entity.count(f'"{key}"') != 2:
            failures.append(
                f"FurnaceBlockEntity does not both save and load {key!r} -- a part-spent buffer "
                "does not survive a reload, and nothing reports it"
            )

    lang = json.loads((ROOT / "kubejs/assets/planetaryfactory/lang/en_us.json")
                      .read_text(encoding="utf-8"))
    for key in ("tooltip.planetaryfactory.furnace.fuel",
                "tooltip.planetaryfactory.furnace.fuel.seconds",
                "tooltip.planetaryfactory.furnace.fuel.out",
                # The item tooltip is where a default-deny table stops being invisible: vanilla's
                # intuitions about what burns are wrong in both directions here.
                "tooltip.planetaryfactory.fuel.joules",
                "tooltip.planetaryfactory.fuel.burn"):
        if key not in lang:
            failures.append(f"{key} is not in the lang file, so the burner's hover reads as its key")

    for index, failure in enumerate(failures, 1):
        print(f"FAIL {index}: {failure}")
    if failures:
        return 1
    print(f"ok   {len(emitted)} fuels in the table, {len(fuels) - len(emitted)} corpus fuels "
          "recorded as skips, coal is 888 ticks and the Steel tier doubles it")
    return 0


if __name__ == "__main__":
    sys.exit(main())
