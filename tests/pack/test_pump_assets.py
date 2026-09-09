#!/usr/bin/env python3
"""Assert the Offshore Pump's generated halves still agree with what registers it (#213, ADR-0050).

`docs/testing/what-to-check.md`'s "cross-file references resolve" claim, for the one block that is
the origin of every drop of water in the factory. Four things are asserted, and they fail in
different ways:

  - **The corpus number.** `scripts/build-pump-assets.py` copies the `pumps` row out of
    `data/factorio/machine.json` (#210) into a resource the mod reads at class-init. That copy is
    the one place ADR-0022's "extracted, never transcribed" could be quietly broken -- a
    hand-edited resource would run the pump at a rate somebody chose and nothing would fail, since
    the mod's own tests assert the arithmetic over whatever number they are handed. So the resource
    is asserted against the corpus field by field, never against a literal.
  - **The rate the ratio rests on.** `tests/factorio/test_resource_extract.py` holds "one pump
    feeds twenty boilers" against the corpus. What it cannot see is whether the *mod* reads that
    same figure: a resource carrying a different `pumping_speed` leaves the corpus check green and
    the game wrong. Asserted here because this is the only file that sees both.
  - **The refusal message.** ADR-0050 requires placement to be refused *with a message* -- a pump
    that places and then silently produces nothing reaches the player as a dead factory three
    machines later. A missing lang key does not fail: it renders the raw key. So the key the item
    actually asks for is read out of the item's source rather than typed here.
  - **The pack-side files.** It is a `planetaryfactory:` block, so GregTech's model provider does
    not serve it and every hop is ours: blockstate to model to texture, a lang key, a loot table.
    Each way of breaking those fails quietly -- a black-and-magenta cube, a raw translation key as
    the block's name, or a block that breaks into nothing.

Usage: tests/pack/test_pump_assets.py
"""

import json
import pathlib
import re
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
PUMPS = ROOT / "mod/src/main/resources/planetaryfactory_core/fluid/pumps.json"
MACHINE_CORPUS = ROOT / "data/factorio/machine.json"
ITEM_MAP = ROOT / "data/pack/item-map.json"
GENERATOR = ROOT / "scripts/build-pump-assets.py"
PUMP_ITEM = ROOT / "mod/src/main/java/com/planetaryfactory/core/fluid/OffshorePumpItem.java"
ASSETS = ROOT / "kubejs/assets/planetaryfactory"
DATA = ROOT / "kubejs/data/planetaryfactory"

FACTORIO_NAME = "offshore-pump"
BLOCK_NAME = "offshore_pump"
BLOCK_ID = f"planetaryfactory:{BLOCK_NAME}"

# Every field the generator copies. Kept here as well as in the generator so that dropping one
# there fails rather than quietly narrowing what the mod reads.
COPIED_FIELDS = ("pumping_speed", "energy_source")

# ADR-0050's figure, and the only literal in this file. It is not a second source of truth for the
# rate -- the field-by-field comparison above already holds that -- it is the assertion that the
# *ratio* the corpus check defends is the one the mod actually runs at. If Factorio ever restates
# `pumping_speed`, this line is where the pack notices that "one pump feeds twenty boilers" has
# changed meaning, rather than shipping a new ratio in silence.
EXPECTED_PUMPING_SPEED = 20

# `Component.translatable(NO_WATER_KEY)` resolves to whatever the constant holds -- so the constant
# is what gets read, not the call.
NO_WATER_KEY_RE = re.compile(r'NO_WATER_KEY\s*=\s*"([a-z_.]+)"')


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


def check_corpus(failures):
    corpus = {row["name"]: row for row in json.loads(MACHINE_CORPUS.read_text()).get("pumps", [])}
    row = corpus.get(FACTORIO_NAME)
    if row is None:
        failures.append(
            f"{FACTORIO_NAME} is not in {MACHINE_CORPUS.relative_to(ROOT)}'s pumps -- re-run the "
            "machine extractor"
        )
        return
    if not PUMPS.is_file():
        failures.append(f"{PUMPS.relative_to(ROOT)} is missing -- run the generator")
        return
    mod_row = json.loads(PUMPS.read_text()).get(FACTORIO_NAME)
    if mod_row is None:
        failures.append(f"pumps.json carries no {FACTORIO_NAME} row -- re-run the generator")
        return
    for field in COPIED_FIELDS:
        if mod_row.get(field) != row.get(field):
            failures.append(
                f"{FACTORIO_NAME}'s {field} is {mod_row.get(field)!r} in pumps.json but "
                f"{row.get(field)!r} in the corpus -- every number here is extracted"
            )
    if row.get("pumping_speed") != EXPECTED_PUMPING_SPEED:
        failures.append(
            f"the corpus states pumping_speed {row.get('pumping_speed')!r}, not "
            f"{EXPECTED_PUMPING_SPEED} -- ADR-0050's 'one pump feeds twenty boilers' was written "
            "against the latter, so this is a design change rather than a regeneration"
        )
    # ADR-0050: the pump takes no power, and that is Factorio's own `void` energy source rather
    # than something nobody wired up. A row that stopped saying so would make the mod ask for
    # energy the pack never gives it.
    if row.get("energy_source") != "void":
        failures.append(
            f"{FACTORIO_NAME}'s energy_source is {row.get('energy_source')!r}, not 'void' -- the "
            "pump is powerless by ADR-0050 and the mod reads this field to say so"
        )


def check_item_map(failures):
    """The pump's row must name the block the mod registers -- and only now that it exists.

    ADR-0050 is explicit that the row flips from `undecided` when the block lands and not before:
    an item-map target for a block that does not exist emits a recipe naming nothing.
    """
    row = json.loads(ITEM_MAP.read_text())["items"].get(FACTORIO_NAME)
    if row is None:
        failures.append(f"{FACTORIO_NAME} has no item-map row at all -- an unmapped corpus name is "
                        "a hard failure, not a skip (#72)")
        return
    if row.get("status") == "undecided":
        failures.append(
            f"{FACTORIO_NAME} is still `undecided` in the item map, but the block is registered -- "
            "the pump would have no recipe and be unobtainable"
        )
        return
    if row.get("target") != BLOCK_ID:
        failures.append(
            f"{FACTORIO_NAME} maps onto {row.get('target')!r}, not {BLOCK_ID} -- the row names "
            "something other than the block it is"
        )


def check_refusal_message(lang, failures):
    """The string the item asks for when it refuses a placement."""
    source = PUMP_ITEM.read_text(encoding="utf-8")
    keys = NO_WATER_KEY_RE.findall(source)
    if not keys:
        failures.append(
            f"no refusal key parsed out of {PUMP_ITEM.relative_to(ROOT)} -- has the constant moved, "
            "or has the refusal stopped saying anything?"
        )
    for key in keys:
        if not lang.get(key):
            failures.append(
                f"{key} has no lang entry -- a refused placement would print its own raw key, "
                "which is worse than the silent failure ADR-0050 wrote the message against"
            )


def check_assets(lang, failures):
    blockstate = ASSETS / f"blockstates/{BLOCK_NAME}.json"
    if not resolves(blockstate):
        failures.append(f"{BLOCK_NAME} has no blockstate")
    else:
        variants = json.loads(blockstate.read_text()).get("variants") or {}
        if not variants:
            failures.append(f"{BLOCK_NAME}'s blockstate declares no variants")
        for variant, definition in variants.items():
            entry = definition if isinstance(definition, dict) else definition[0]
            model = entry["model"]
            model_path = ASSETS / f"models/{model.split(':', 1)[-1]}.json"
            if not resolves(model_path):
                failures.append(f"{BLOCK_NAME}[{variant}] names model {model}, which is missing")
                continue
            declared = json.loads(model_path.read_text())
            for slot, reference in (declared.get("textures") or {}).items():
                texture = texture_path(reference)
                if texture is not None and not resolves(texture):
                    failures.append(f"{model}'s {slot} texture {reference} is missing")

    if not resolves(ASSETS / f"models/item/{BLOCK_NAME}.json"):
        failures.append(f"{BLOCK_NAME} has no item model -- it would be invisible in the hand")

    loot = DATA / f"loot_table/blocks/{BLOCK_NAME}.json"
    if not resolves(loot):
        failures.append(f"{BLOCK_NAME} has no loot table -- breaking it would drop nothing")
    else:
        names = json.dumps(json.loads(loot.read_text()))
        if BLOCK_ID not in names:
            failures.append(f"{BLOCK_NAME}'s loot table does not drop itself")

    if not lang.get(f"block.planetaryfactory.{BLOCK_NAME}"):
        failures.append(f"{BLOCK_NAME} has no lang entry -- it would show its raw key")


def main():
    failures = []
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

    check_corpus(failures)
    check_item_map(failures)
    check_refusal_message(lang, failures)
    check_assets(lang, failures)

    if failures:
        print(f"FAIL {len(failures)}:")
        for failure in failures:
            print(f"  - {failure}")
        return 1
    print("ok   offshore pump: corpus row extracted, item map names the block, refusal message "
          "and every asset hop resolve")
    return 0


if __name__ == "__main__":
    sys.exit(main())
