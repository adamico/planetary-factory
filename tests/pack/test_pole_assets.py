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

The tier list is read out of `PoleTier.java` rather than typed here, so adding a fourth tier fails
this check instead of silently shipping without assets. The reverse -- a tier *removed* from the
enum -- is checked too: the files it left behind stop being reachable from the tier list, so they
have to be asserted absent by name.
"""

import json
import pathlib
import re
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[2]
POLE_TIER = ROOT / "mod/src/main/java/com/planetaryfactory/core/energy/PoleTier.java"
TRANSLATING_SOURCES = (
    ROOT / "mod/src/main/java/com/planetaryfactory/core/energy/SupplyAreaPoleItem.java",
    ROOT / "mod/src/main/java/com/planetaryfactory/core/compat/PoleJadePlugin.java",
)
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


def translatable_calls(source):
    """Every `Component.translatable("key", arg, ...)` in a Java source, as (key, argument count).

    Parsed rather than regexed because the pole's own tooltip nests one `Component.translatable`
    inside another's arguments, and a regex that stops at the first `)` would read the inner call's
    arguments as the outer call's.
    """
    calls = []
    marker = "Component.translatable("
    start = source.find(marker)
    while start != -1:
        i = start + len(marker)
        depth, args, current = 1, [], []
        while depth > 0:
            char = source[i]
            if char in "([":
                depth += 1
            elif char in ")]":
                depth -= 1
                if depth == 0:
                    break
            if char == "," and depth == 1:
                args.append("".join(current).strip())
                current = []
            else:
                current.append(char)
            i += 1
        args.append("".join(current).strip())
        key = args[0].strip()
        if key.startswith('"') and key.endswith('"'):
            calls.append((key[1:-1], len(args) - 1))
        start = source.find(marker, i)
    return calls


class PoleAssets(unittest.TestCase):
    def setUp(self):
        self.tiers = registered_tiers()

    def test_the_three_shipped_tiers_are_registered(self):
        self.assertEqual(
            {
                "small_electric_pole": 5,
                "medium_electric_pole": 7,
                "substation_electric_pole": 18,
            },
            self.tiers,
            "the supply areas are Factorio's own; Factorio's fourth tier, the big pole, is "
            "deliberately absent (see PoleTier)",
        )

    def test_the_dropped_big_pole_leaves_no_files_behind(self):
        # A tier is a name spread over seven files in three trees, and dropping one is seven
        # deletions with nothing checking that they all happened. A blockstate, model, texture or
        # lang key for a block nothing registers is inert -- it ships, and nothing reports it. The
        # recipe JSON is worse: it is generated, so a stale one comes back as a converter diff on
        # whoever next runs it rather than as a failure here. Named rather than generalised over
        # every unregistered pole, because this asserts one decision, not a rule about files.
        stale = sorted(
            path.relative_to(ROOT).as_posix()
            for tree in (ASSETS, DATA)
            for path in tree.rglob("*big_electric_pole.*")
        )
        self.assertEqual([], stale, "the big pole is dropped, so nothing may still name it")

        lang = json.loads((ASSETS / "lang" / "en_us.json").read_text(encoding="utf-8"))
        self.assertNotIn("block.planetaryfactory.big_electric_pole", lang)

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


    def test_every_translation_key_the_pole_uses_exists(self):
        # The pole is the only block in the pack that explains itself -- no cable to trace, no GUI,
        # no recipe that hints at reach -- so a missing key here is not a cosmetic blemish. It ships
        # the raw key where the explanation should be, and nothing logs it.
        lang = json.loads((ASSETS / "lang" / "en_us.json").read_text(encoding="utf-8"))
        for source in TRANSLATING_SOURCES:
            for key, _ in translatable_calls(source.read_text(encoding="utf-8")):
                if not key.startswith("tooltip.planetaryfactory."):
                    continue
                with self.subTest(source=source.name, key=key):
                    self.assertIn(key, lang, f"{source.name} translates {key}, which has no entry")
                    self.assertTrue(lang[key].strip(), f"{key} is blank")

    def test_every_translation_key_takes_the_arguments_it_is_given(self):
        # A key whose `%s` count disagrees with the Java crashes the client formatting it, at the
        # moment a player hovers the item. Nothing at build time compares the two sides.
        lang = json.loads((ASSETS / "lang" / "en_us.json").read_text(encoding="utf-8"))
        for source in TRANSLATING_SOURCES:
            for key, count in translatable_calls(source.read_text(encoding="utf-8")):
                if key not in lang:
                    continue  # the test above owns that failure
                with self.subTest(source=source.name, key=key):
                    self.assertEqual(
                        lang[key].count("%s"), count,
                        f"{key} takes {lang[key].count('%s')} arguments, "
                        f"{source.name} passes {count}",
                    )


if __name__ == "__main__":
    unittest.main()
