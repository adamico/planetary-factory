#!/usr/bin/env python3
"""Assert the starting kit #203 grants is the one the spec specifies, and that every id resolves.

`docs/testing/what-to-check.md`'s "cross-file references resolve" claim. The kit is a list of item
id STRINGS -- it has to be, because that is what makes the once-per-player rule a Minecraft-free
unit test -- and a string that names nothing reaches the player as an empty pocket slot. There is no
crash, no failed load and no log line until the grant runs in a world, by which point the pace run
has already opened wrong.

Three things are checked, and the second and third are the ones a compiler could never see:

1. **Every granted id resolves.** Ours against the enums that produce the registry paths, the two
   foreign items against the installed jars, and the three hold items against `item-map.json`, which
   is the pack's authority on how a Factorio item is spelled here.
2. **The pocket is the spec's pocket.** Not a superset: an extra tool in the pocket is a beat the
   opening no longer teaches, and it fails nothing else.
3. **Nothing in the hold is otherwise unobtainable.** The spec's own sentence -- the moment the hold
   contains a green circuit, rung 0 has stopped being taught. The hold is asserted to be exactly the
   three Factorio names the spec lists, at single-digit counts.
"""

import functools
import json
import pathlib
import re
import unittest
import zipfile

ROOT = pathlib.Path(__file__).resolve().parents[2]
MOD = ROOT / "mod/src/main/java/com/planetaryfactory/core"
KIT = MOD / "start/StartingKit.java"
ITEM_MAP = ROOT / "data/pack/item-map.json"
SPEC = ROOT / "docs/spec/terra-progression.md"
MODS = ROOT / "mods"

NAMESPACE = "planetaryfactory"


# What each pocket entry has to be recognisable as in the spec's "What you start with" bullet. The
# book is beat 1's own sentence rather than the bullet's, so it is matched against the beat table.
POCKET_IN_SPEC = {
    "gtceu:prospector.lv": "prospector",
    "planetaryfactory:stone_furnace": "Stone Furnace",
    "planetaryfactory:burner_mining_drill": "Burner Mining Drill",
    "planetaryfactory:engineers_iron_pick": "Engineer's Iron Pick",
    "ftbquests:book": None,
}

ENTRY = re.compile(r'new Entry\("([^"]+)",\s*(\d+)\)')


def spec_hold():
    """The hold, read out of the spec's own bullet rather than copied into this file.

    Copying it here would make the one list the spec explicitly warns about -- "the moment the hold
    contains a green circuit, rung 0 stops being taught" -- checked against a second copy of itself,
    which a fourth row could be added to in the same commit. The bullet names its items in prose, so
    the names are read from it and turned into Factorio's spelling: "iron plate" is `iron-plate`.
    """
    bullet = re.search(r"\*\*Hold\*\*:(.*?)\.", SPEC.read_text(encoding="utf-8"), re.DOTALL)
    assert bullet is not None, "the spec's Hold bullet has moved"
    return tuple(part.strip().replace(" ", "-")
                 for part in bullet.group(1).replace(" and ", ", ").split(","))


@functools.lru_cache(maxsize=None)
def entries(field):
    """The Entry rows of one `List<Entry>` field in StartingKit.java, in order."""
    source = KIT.read_text(encoding="utf-8")
    body = re.search(
        r"public static final List<Entry> %s = List\.of\((.*?)\);" % field, source, re.DOTALL)
    assert body is not None, "StartingKit.%s has moved or changed shape" % field
    return tuple((item, int(count)) for item, count in ENTRY.findall(body.group(1)))


def jar_lang(namespace):
    """The lang keys the installed jar for one namespace ships."""
    jars = sorted(MODS.glob("*.jar"))
    for jar in jars:
        with zipfile.ZipFile(jar) as archive:
            name = "assets/%s/lang/en_us.json" % namespace
            if name in archive.namelist():
                return set(json.loads(archive.read(name)))
    return set()


def enum_rows(path):
    """The constant names of the one enum in a tier file, e.g. STONE, ELECTRIC."""
    return set(re.findall(r"^\s{4}([A-Z]+)\(", path.read_text(encoding="utf-8"), re.MULTILINE))


class StartingKitIds(unittest.TestCase):
    def setUp(self):
        self.pocket = entries("POCKET")
        self.hold = entries("HOLD")
        self.item_map = json.loads(ITEM_MAP.read_text(encoding="utf-8"))["items"]

    def test_our_own_pocket_items_are_ones_the_mod_registers(self):
        """The three blocks and the tool, against the enums their registry paths come from.

        Each id is built by an enum method rather than typed -- `FurnaceTier.blockName()` is
        `stone_furnace` -- so the failure this catches is a rename on one side only, which compiles.
        """
        furnaces = {"%s_furnace" % name.lower()
                    for name in enum_rows(MOD / "smelting/FurnaceTier.java")}
        rigs = {"%s_mining_drill" % name.lower()
                for name in enum_rows(MOD / "mining/rig/RigTier.java")}
        # The Pick states its id outright rather than deriving it from the constant name.
        picks = set(re.findall(r'[A-Z]+\("([a-z_]+)"',
                               (MOD / "mining/PickTier.java").read_text(encoding="utf-8")))
        for rows, where in ((furnaces, "FurnaceTier"), (rigs, "RigTier"), (picks, "PickTier")):
            self.assertTrue(rows, "%s's rows have moved" % where)

        registered = furnaces | rigs | picks
        for item, _ in self.pocket:
            namespace, _, path = item.partition(":")
            if namespace != NAMESPACE:
                continue
            self.assertIn(path, registered,
                          "the starting kit grants %s, which no tier in the mod registers -- the "
                          "player starts with an empty slot and nothing is logged" % item)

    def test_the_foreign_pocket_items_exist_in_the_installed_jars(self):
        """The prospector and the book, against the jars rather than against our own belief."""
        for item, _ in self.pocket:
            namespace, _, path = item.partition(":")
            if namespace == NAMESPACE:
                continue
            keys = jar_lang(namespace)
            self.assertTrue(keys, "no installed jar ships assets/%s/lang/en_us.json" % namespace)
            self.assertIn("item.%s.%s" % (namespace, path), keys,
                          "the starting kit grants %s, which the installed %s jar does not name"
                          % (item, namespace))

    def test_the_hold_is_the_specs_three_items_and_nothing_else(self):
        """Nothing in the hold is otherwise unobtainable (spec, Opening).

        The hold is asserted against `item-map.json`, which is where a Factorio name becomes an item
        id. That is what makes this an assertion rather than a restatement: a fourth row here would
        have to be a Factorio item the pack maps, and the three that are allowed are named above.
        """
        held = [item for item, _ in self.hold]
        names = spec_hold()
        self.assertTrue(names, "the spec's Hold bullet names nothing")
        expected = []
        for name in names:
            row = self.item_map.get(name)
            self.assertIsNotNone(row, "item-map.json has no row for %r" % name)
            self.assertEqual("item", row["kind"],
                             "%r is not a plain item, so it cannot be a hold stack" % name)
            expected.append(row["target"])
        self.assertEqual(expected, held,
                         "the hold must be exactly the spec's iron plate, copper plate and coal -- "
                         "the moment it holds a green circuit, rung 0 has stopped being taught")

    def test_the_hold_is_single_digits(self):
        for item, count in self.hold:
            self.assertLess(count, 10,
                            "%s is granted %d, and the spec's hold is single digits -- matching "
                            "freeplay's eight-plate debris chest" % (item, count))

    def test_the_pocket_is_one_of_each(self):
        for item, count in self.pocket:
            self.assertEqual(1, count, "%s is a pocket tool, and the pocket holds one of each"
                             % item)


class StartingKitAgainstTheSpec(unittest.TestCase):
    def setUp(self):
        self.pocket = [item for item, _ in entries("POCKET")]
        self.spec = SPEC.read_text(encoding="utf-8")

    def test_the_pocket_is_the_specs_pocket(self):
        """Every pocket entry is named by the spec, and the spec names nothing the kit omits."""
        self.assertEqual(sorted(POCKET_IN_SPEC), sorted(self.pocket),
                         "the pocket has changed; the spec's Opening decides it, so change the "
                         "spec first and this table with it")
        bullet = re.search(r"\*\*Pocket\*\*:(.*?)\n\n", self.spec, re.DOTALL)
        self.assertIsNotNone(bullet, "the spec's Pocket bullet has moved")
        text = bullet.group(1)
        for item, phrase in POCKET_IN_SPEC.items():
            if phrase is None:
                continue
            self.assertIn(phrase, text,
                          "the kit grants %s and the spec's Pocket bullet does not name it" % item)

    def test_the_book_is_in_the_kit_because_beat_one_needs_it(self):
        """Beat 1 says the book is in the inventory, and beat 1 is before anything can grant it."""
        self.assertIn("The book is in your inventory", self.spec)
        self.assertIn("ftbquests:book", self.pocket)


if __name__ == "__main__":
    unittest.main()
