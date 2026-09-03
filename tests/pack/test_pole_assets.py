#!/usr/bin/env python3
"""Assert every supply-area pole the mod registers has the pack-side files it needs.

`docs/testing/what-to-check.md`'s "cross-file references resolve" claim, for the pole seam
(ADR-0036, #147).

The pole is split across the boundary ADR-0015 draws: `planetaryfactory_core` registers the block
because a radius scan is mechanism, and everything a designer would tune -- model, texture, name,
drop -- is data under `kubejs/`. That split is the reason this file exists. Nothing checks the two
halves against each other at build time, and each way of breaking them apart fails quietly:

  - a missing blockstate or model renders the untextured black-and-magenta cube, with only a
    client-side warning
  - a missing texture does the same one hop further down
  - a missing lang key ships the raw translation key as the block's name, and nothing logs it
  - a missing loot table makes the block break into nothing, which reads as a game bug rather
    than a packaging one

The tier list is read out of `PoleTier.java` rather than typed here, so adding a fifth tier fails
this check instead of silently shipping without assets.
"""

import json
import pathlib
import re
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[2]
POLE_TIER = ROOT / "mod/src/main/java/com/planetaryfactory/core/energy/PoleTier.java"
ASSETS = ROOT / "kubejs/assets/planetaryfactory"
DATA = ROOT / "kubejs/data/planetaryfactory"

# `SMALL(5),` -- the enum constant and its Factorio supply size.
TIER_RE = re.compile(r"^\s{4}([A-Z][A-Z_]*)\((\d+)\)[,;]", re.MULTILINE)


def registered_tiers():
    source = POLE_TIER.read_text(encoding="utf-8")
    tiers = TIER_RE.findall(source)
    if not tiers:
        raise AssertionError(f"no pole tiers parsed out of {POLE_TIER} -- has the enum moved?")
    return {name.lower() + "_electric_pole": int(size) for name, size in tiers}


class PoleAssets(unittest.TestCase):
    def setUp(self):
        self.tiers = registered_tiers()

    def test_all_four_factorio_tiers_are_registered(self):
        self.assertEqual(
            {
                "small_electric_pole": 5,
                "medium_electric_pole": 7,
                "big_electric_pole": 4,
                "substation_electric_pole": 18,
            },
            self.tiers,
            "the supply areas are Factorio's own; the big pole's 4x4 really is smaller than the "
            "medium pole's 7x7, because in Factorio it buys wire reach instead (see PoleTier)",
        )

    def test_every_pole_has_a_blockstate_naming_a_model_that_exists(self):
        for name in self.tiers:
            with self.subTest(pole=name):
                blockstate = ASSETS / "blockstates" / f"{name}.json"
                self.assertTrue(blockstate.is_file(), f"{blockstate} is missing")
                variants = json.loads(blockstate.read_text(encoding="utf-8"))["variants"]
                for variant in variants.values():
                    model = variant["model"]
                    self.assertTrue(model.startswith("planetaryfactory:"), model)
                    path = ASSETS / "models" / (model.split(":", 1)[1] + ".json")
                    self.assertTrue(path.is_file(), f"{blockstate} names {model}, which is missing")

    def test_every_model_names_textures_that_exist(self):
        for name in self.tiers:
            for kind in ("block", "item"):
                with self.subTest(pole=name, model=kind):
                    path = ASSETS / "models" / kind / f"{name}.json"
                    self.assertTrue(path.is_file(), f"{path} is missing")
                    model = json.loads(path.read_text(encoding="utf-8"))
                    for texture in model.get("textures", {}).values():
                        self.assertTrue(texture.startswith("planetaryfactory:"), texture)
                        png = ASSETS / "textures" / (texture.split(":", 1)[1] + ".png")
                        self.assertTrue(png.is_file(), f"{path} names {texture}, which is missing")

    def test_the_item_model_resolves_to_a_model_that_exists(self):
        for name in self.tiers:
            with self.subTest(pole=name):
                model = json.loads(
                    (ASSETS / "models" / "item" / f"{name}.json").read_text(encoding="utf-8"))
                parent = model["parent"]
                self.assertTrue(parent.startswith("planetaryfactory:"), parent)
                path = ASSETS / "models" / (parent.split(":", 1)[1] + ".json")
                self.assertTrue(path.is_file(), f"the item model names {parent}, which is missing")

    def test_every_pole_is_named(self):
        lang = json.loads((ASSETS / "lang" / "en_us.json").read_text(encoding="utf-8"))
        for name in self.tiers:
            with self.subTest(pole=name):
                key = f"block.planetaryfactory.{name}"
                self.assertIn(key, lang, f"{key} has no translation, so the block shows its key")
                self.assertTrue(lang[key].strip(), f"{key} is blank")

    def test_every_pole_drops_itself(self):
        for name in self.tiers:
            with self.subTest(pole=name):
                path = DATA / "loot_table" / "blocks" / f"{name}.json"
                self.assertTrue(path.is_file(), f"{path} is missing, so the pole breaks into nothing")
                table = json.loads(path.read_text(encoding="utf-8"))
                dropped = {
                    entry["name"]
                    for pool in table["pools"]
                    for entry in pool["entries"]
                    if entry.get("type") == "minecraft:item"
                }
                self.assertEqual({f"planetaryfactory:{name}"}, dropped)


if __name__ == "__main__":
    unittest.main()
