#!/usr/bin/env python3
"""Assert Terra's steam-chain corpus and its two fluids' pack-side assets still agree (#223, ADR-0048).

Three things are asserted, and each fails in a different way:

  - **The corpus rows.** `scripts/build-steam-assets.py` copies the `boiler` (from `machine.json`'s
    `boilers` array) and `steam-engine` (from its `generators` array) rows -- whole, every field --
    into a resource the mod reads at class-init. That copy is the one place a hand-edited row would
    run the Boiler at a rate somebody chose with nothing else failing, since neither #224 nor #225
    exists yet to notice. So the resource is asserted against the corpus field by field, never
    against a literal, the `test_pump_assets.py` pattern.
  - **The fluids' lang keys.** These are `planetaryfactory:` fluids, so nothing else in the pack
    names them, and a missing `fluid_type` key renders the raw key in a tank tooltip.
  - **That neither fluid has a bucket.** ADR-0037 answered portable fluid for this pack --
    `planetaryfactory:barrel`, any fluid at Factorio's own 50 mB -- and states that capacity as a
    rule a later container "does not get to be re-argued from Minecraft's bucket" against. A
    1 000 mB bucket of steam is the twentyfold dose that ADR rejects, and it hands the player a
    hand-carry route around the Boiler-pipe-Engine chain rung 0 exists to teach. Asserted as an
    absence rather than left undone, because a bucket is the obvious thing to add back.
  - **That neither fluid is a GT material.** ADR-0048's central point: `gtceu:steam` is not inert,
    and nothing here may reach for it. Checked by grepping the fluid registration source for the
    string, since a static check cannot ask GregTech's own registry what accepted it.

What this file cannot assert is that the fluid actually renders in a tank -- that is a
`IClientFluidTypeExtensions` wiring fact, a world/client load, not a static one; see the ticket's
own Checks section.

Usage: tests/pack/test_steam_assets.py
"""

import json
import pathlib
import re
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
STEAM_CHAIN = ROOT / "mod/src/main/resources/planetaryfactory_core/fluid/steam_chain.json"
MACHINE_CORPUS = ROOT / "data/factorio/machine.json"
GENERATOR = ROOT / "scripts/build-steam-assets.py"
ASSETS = ROOT / "kubejs/assets/planetaryfactory"

FLUID_JAVA_DIR = ROOT / "mod/src/main/java/com/planetaryfactory/core/fluid"
PF_FLUID_TYPES = FLUID_JAVA_DIR / "PFFluidTypes.java"
PF_FLUIDS = FLUID_JAVA_DIR / "PFFluids.java"

BOILER_NAME = "boiler"
STEAM_ENGINE_NAME = "steam-engine"

FLUIDS = ("steam", "superheated_steam")


def resolves(path):
    return path.is_file()


def check_corpus(failures):
    machine = json.loads(MACHINE_CORPUS.read_text())
    boilers = {row["name"]: row for row in machine.get("boilers", [])}
    generators = {row["name"]: row for row in machine.get("generators", [])}

    boiler_row = boilers.get(BOILER_NAME)
    if boiler_row is None:
        failures.append(
            f"{BOILER_NAME} is not in {MACHINE_CORPUS.relative_to(ROOT)}'s boilers -- re-run the "
            "machine extractor"
        )
    steam_engine_row = generators.get(STEAM_ENGINE_NAME)
    if steam_engine_row is None:
        failures.append(
            f"{STEAM_ENGINE_NAME} is not in {MACHINE_CORPUS.relative_to(ROOT)}'s generators -- "
            "re-run the machine extractor"
        )
    if not resolves(STEAM_CHAIN):
        failures.append(f"{STEAM_CHAIN.relative_to(ROOT)} is missing -- run the generator")
        return
    if boiler_row is None or steam_engine_row is None:
        return

    mod_rows = json.loads(STEAM_CHAIN.read_text())
    mod_boiler = mod_rows.get(BOILER_NAME)
    mod_steam_engine = mod_rows.get(STEAM_ENGINE_NAME)
    if mod_boiler != boiler_row:
        failures.append(
            f"{BOILER_NAME}'s row in steam_chain.json does not match the corpus field by field -- "
            "every number here is extracted, not hand-edited"
        )
    if mod_steam_engine != steam_engine_row:
        failures.append(
            f"{STEAM_ENGINE_NAME}'s row in steam_chain.json does not match the corpus field by "
            "field -- every number here is extracted, not hand-edited"
        )

    # ADR-0048: one boiler tier, and the Steam Engine's ceiling is the same 165 C the Boiler
    # targets. If the corpus ever restated either, the ADR's "one boiler tier" claim would have
    # changed meaning without anything else here noticing.
    if boiler_row.get("target_temperature") != steam_engine_row.get("maximum_temperature"):
        failures.append(
            "the Boiler's target_temperature and the Steam Engine's maximum_temperature no longer "
            "agree -- ADR-0048's 'one boiler tier, no heat layer' was written against them matching"
        )


def check_assets(lang, failures):
    for fluid_name in FLUIDS:
        fluid_type_key = f"fluid_type.planetaryfactory.{fluid_name}"
        if not lang.get(fluid_type_key):
            failures.append(f"{fluid_name} has no {fluid_type_key} lang entry -- it would show its "
                            "raw key in a tank tooltip")


def check_no_bucket(lang, failures):
    """ADR-0037: the barrel is this pack's portable fluid container, at Factorio's 50 mB.

    A bucket is the obvious thing for a later hand to add back, so the absence is asserted rather
    than merely left undone.
    """
    source = PF_FLUIDS.read_text(encoding="utf-8")
    code = re.sub(r"/\*.*?\*/", "", source, flags=re.DOTALL)
    code = re.sub(r"//.*", "", code)
    if "BucketItem" in code:
        failures.append(
            "PFFluids registers a BucketItem -- ADR-0037 makes planetaryfactory:barrel this pack's "
            "portable fluid container at Factorio's 50 mB, and says a later container does not get "
            "to be re-argued from Minecraft's bucket"
        )

    for fluid_name in FLUIDS:
        bucket_key = f"item.planetaryfactory.{fluid_name}_bucket"
        if lang.get(bucket_key):
            failures.append(f"{bucket_key} is a lang entry, but {fluid_name} has no bucket")

        model = ASSETS / f"models/item/{fluid_name}_bucket.json"
        if resolves(model):
            failures.append(f"{model} exists, but {fluid_name} has no bucket")


def check_not_gtceu_steam(failures):
    """ADR-0048's central point: gtceu:steam is not inert, so nothing here may reach for it.

    Only *code* is checked, not prose: this file's own docstrings and the classes' javadoc are
    allowed to name `gtceu:steam` when explaining what must not be used, so block comments and
    line comments are stripped before the search.
    """
    for path in (PF_FLUID_TYPES, PF_FLUIDS):
        if not resolves(path):
            failures.append(f"{path.relative_to(ROOT)} is missing")
            continue
        source = path.read_text(encoding="utf-8")
        code = re.sub(r"/\*.*?\*/", "", source, flags=re.DOTALL)
        code = re.sub(r"//.*", "", code)
        if "gtceu" in code.lower():
            failures.append(
                f"{path.relative_to(ROOT)} mentions gtceu outside a comment -- ADR-0048 is "
                "explicit that these are planetaryfactory: fluids, never GregTech's own steam"
            )


def main():
    failures = []
    lang = json.loads((ASSETS / "lang/en_us.json").read_text())

    generated = subprocess.run(
        [sys.executable, str(GENERATOR), "--check"], capture_output=True, text=True
    )
    if generated.returncode != 0:
        failures.append(
            f"{GENERATOR.relative_to(ROOT)} --check: "
            f"{(generated.stdout + generated.stderr).strip()}"
        )

    check_corpus(failures)
    check_assets(lang, failures)
    check_no_bucket(lang, failures)
    check_not_gtceu_steam(failures)

    if failures:
        print(f"FAIL {len(failures)}:")
        for failure in failures:
            print(f"  - {failure}")
        return 1
    print("ok   steam chain: boiler and steam-engine rows extracted, both fluids named, "
          "no bucket, neither is gtceu:steam")
    return 0


if __name__ == "__main__":
    sys.exit(main())
