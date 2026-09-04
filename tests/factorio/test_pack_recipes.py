#!/usr/bin/env python3
"""Assert ADR-0039's hand-written recipe subtree, and the Engineer's Pick's pack-side files.

`docs/testing/what-to-check.md`'s "cross-file references resolve" claim, for the one subtree of
`kubejs/data/planetaryfactory/recipe/` that no converter generates.

`recipe/pack/` is ADR-0031's single stated exception: the corpus authors every recipe it contains,
and Factorio has no mining-tool prototype, so the Engineer's Pick's two recipes cannot come from
`data/factorio/recipe.json` at all. That exemption is what makes this file necessary -- every other
recipe here is regenerated and checked against the corpus, and these two are checked against
nothing unless something checks them here.

What fails quietly without it:

  - a converter run wiping the subtree, because `FOREIGN_SUBTREES` stopped naming it. The pick
    recipes vanish, the sweep removes every stock alternative, and the pack is back to the state
    #165 describes: nothing can be mined at all.
  - a recipe landing on a surface `recipe_survivors.js` does not name, so ADR-0034's sweep removes
    it on load with no error.
  - dropping `factorio_category: crafting`, which is the entire definition of the Personal
    Assembler's hand set (`RuntimeHandRecipes`). The recipe survives, is craftable in a machine the
    player cannot build yet, and rung 0 is a dead end.
  - a file under `kubejs/` whose name carries an uppercase letter. KubeJS validates every name it
    scans and rejects one outright -- `Invalid file name: Uppercase 'R' in
    kubejs/data/planetaryfactory/recipe/pack/README.md` -- and that ERROR stops a world from
    loading. It is asserted here because this subtree is the one place a human writes files under
    `kubejs/` by hand rather than generating them, and a README next to the recipes is the obvious
    thing to reach for.
  - a pick with no model, texture or lang key -- the black-and-magenta cube and a raw translation
    key, neither of which is logged as an error.
  - the steel recipe not consuming the iron pick, which ADR-0039 states in one line and which no
    other file would notice.

The item ids are read out of `PickTier.java` rather than typed here, so a third tier fails this
check instead of shipping without assets or a recipe.

Usage: tests/factorio/test_pack_recipes.py
"""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
EMITTED = ROOT / "kubejs/data/planetaryfactory/recipe"
SUBTREE = "pack"
PACK = EMITTED / SUBTREE
PICK_TIER = ROOT / "mod/src/main/java/com/planetaryfactory/core/mining/PickTier.java"
PICK_ITEM = ROOT / "mod/src/main/java/com/planetaryfactory/core/mining/EngineersPick.java"
ASSETS = ROOT / "kubejs/assets/planetaryfactory"
DATA = ROOT / "kubejs/data"
SURVIVORS = ROOT / "kubejs/server_scripts/recipe_survivors.js"
# The two trees KubeJS scans and name-validates. `kubejs/README.txt` sits above both, which is why
# the roots are named rather than `kubejs/` itself.
SCANNED = (ROOT / "kubejs/data", ROOT / "kubejs/assets")
CONVERTER = ROOT / "scripts/factorio-recipe-convert.py"
CONVERT_CHECK = ROOT / "tests/factorio/test_recipe_convert.py"
NAMESPACE = "planetaryfactory"

# The category the Personal Assembler's predicate keeps, and nothing else is hand-craftable.
HAND_CATEGORY = "crafting"

# `IRON("engineers_iron_pick", 0.5f),`
TIER_RE = re.compile(r'^\s{4}([A-Z][A-Z_]*)\("([a-z_]+)",\s*([0-9.]+)f\)[,;]', re.MULTILINE)
FOREIGN_RE = re.compile(r"^FOREIGN_SUBTREES = \((.*)\)$", re.MULTILINE)

failures = []


def check(condition, message):
    if not condition:
        failures.append(message)
    return condition


def tiers():
    """The registered picks, as {item id: mining speed}, read off the enum."""
    parsed = TIER_RE.findall(PICK_TIER.read_text(encoding="utf-8"))
    if not parsed:
        raise AssertionError("no pick tiers parsed out of %s -- has the enum moved?" % PICK_TIER)
    return {item: float(speed) for _, item, speed in parsed}


def survivor_types():
    """`surface: type` from the allowlist, which is the set of surfaces the sweep spares.

    Read entry by entry rather than as two scans zipped together: an entry that omitted `type`, or
    any other `surface:` string in the file, would slide the pairing along by one and leave this
    file asserting a surface against another entry's recipe type -- which passes, and means nothing.
    """
    entries = re.findall(r"\{(.*?)\}", SURVIVORS.read_text(encoding="utf-8"), re.S)
    types = {}
    for entry in entries:
        surface = re.search(r"surface: '([^']+)'", entry)
        kind = re.search(r"type: '([^']+)'", entry)
        if surface and kind:
            types[surface.group(1)] = kind.group(1)
    if not types:
        raise AssertionError("no survivor entries parsed out of %s -- has the file moved?"
                             % SURVIVORS)
    return types


def items_of(recipe, side):
    out = []
    for entry in recipe.get(side, {}).get("item", []):
        ingredient = entry["content"]["ingredient"]
        out.append((ingredient.get("item") or "#" + ingredient["tag"],
                    entry["content"].get("count", 1)))
    return out


def main():
    picks = tiers()

    # The subtree exists and is exactly the two recipes. A third file here is a decision this ADR
    # did not make: its exception is narrow by design, and a general escape hatch was rejected.
    check(PACK.is_dir(), "%s does not exist -- ADR-0039's hand-written recipes are missing" % PACK)
    if not PACK.is_dir():
        return report()
    recipes = {p.stem: json.loads(p.read_text()) for p in sorted(PACK.glob("*.json"))}
    check(set(recipes) == set(picks),
          "recipe/%s/ holds %s; ADR-0039's exception covers exactly %s. A third recipe here needs "
          "its own decision, not this one's precedent"
          % (SUBTREE, sorted(recipes), sorted(picks)))

    # Both converters must leave the subtree alone, and so must the converter's own check.
    for path in (CONVERTER, CONVERT_CHECK):
        declared = FOREIGN_RE.search(path.read_text(encoding="utf-8"))
        if check(declared is not None, "%s declares no FOREIGN_SUBTREES" % path.name):
            check(('"%s"' % SUBTREE) in declared.group(1),
                  "%s does not list `%s` as foreign, so a converter run deletes ADR-0039's "
                  "hand-written recipes and nothing can be mined again (#165)"
                  % (path.name, SUBTREE))

    types = survivor_types()
    for name, recipe in sorted(recipes.items()):
        where = "%s/%s.json" % (SUBTREE, name)
        check(recipe["type"] in types.values(),
              "%s is type %r, which recipe_survivors.js does not admit -- ADR-0034's sweep removes "
              "it on load with no error" % (where, recipe["type"]))
        check(recipe.get("data", {}).get("factorio_category") == HAND_CATEGORY,
              "%s is not category %r, so the Personal Assembler will not plan it and rung 0 has no "
              "route to a pick" % (where, HAND_CATEGORY))
        outputs = items_of(recipe, "outputs")
        check(outputs == [("%s:%s" % (NAMESPACE, name), 1)],
              "%s outputs %s; a recipe under pack/ is named for the single item it makes"
              % (where, outputs))

    # ADR-0039: the steel recipe CONSUMES the iron pick, so the player holds one or the other.
    steel = recipes.get("engineers_steel_pick")
    if steel is not None:
        inputs = dict(items_of(steel, "inputs"))
        check(inputs.get("%s:engineers_iron_pick" % NAMESPACE) == 1,
              "the Steel Pick recipe does not consume the Iron Pick. ADR-0039 has the player "
              "holding one tier or the other, never both")

    # The pack-side files each registered pick needs. Every one of these fails silently.
    lang = json.loads((ASSETS / "lang/en_us.json").read_text(encoding="utf-8"))
    for item in sorted(picks):
        check(("item.%s.%s" % (NAMESPACE, item)) in lang,
              "%s has no lang key, so it ships showing its raw translation key" % item)
        model = ASSETS / "models/item" / (item + ".json")
        if check(model.is_file(), "%s has no item model" % item):
            layer = json.loads(model.read_text())["textures"]["layer0"]
            check(layer == "%s:item/%s" % (NAMESPACE, item),
                  "%s's model points at %r rather than its own texture" % (item, layer))
            check((ASSETS / "textures/item" / (item + ".png")).is_file(),
                  "%s's texture is missing, which renders as the missing-texture checkerboard"
                  % item)
        check(item in recipes,
              "%s is registered but nothing crafts it -- under ADR-0034's sweep there is no stock "
              "recipe to fall back on" % item)

    # The wrench verb ADR-0039 absorbs is two tag entries, not code: Create reads the NeoForge tag
    # and GregTech reads its own. A pick in neither dismantles no machine, and the pack has no
    # other wrench to reach for.
    for tag in ("c/tags/item/tools/wrench.json", "gtceu/tags/item/crafting_tools/wrench.json"):
        path = DATA / tag
        if check(path.is_file(), "%s is missing, so the Pick does not dismantle machines" % tag):
            values = set(json.loads(path.read_text())["values"])
            for item in sorted(picks):
                check(("%s:%s" % (NAMESPACE, item)) in values,
                      "%s is not in %s -- ADR-0039 gives the Pick the wrench's dismantle verb"
                      % (item, tag))

    # Every name KubeJS will scan, lowercase. This is not about tidiness: the validator refuses an
    # uppercase letter with an ERROR, and the world does not load.
    for root in SCANNED:
        for path in sorted(root.rglob("*")):
            if not path.is_file():
                continue
            check(path.name == path.name.lower(),
                  "`%s` has an uppercase letter in its name. KubeJS rejects it -- `Invalid file "
                  "name` -- and that stops a world from loading"
                  % path.relative_to(ROOT).as_posix())

    # The flat-time block tag the jar asks for by name. A tag that does not exist is empty, and an
    # empty one silently reverts every ore to vanilla hardness -- the Factorio number the ADR is
    # about, gone with nothing logged.
    tag_id = re.search(r'fromNamespaceAndPath\([^)]*?"([a-z_]+)"\)',
                       PICK_ITEM.read_text(encoding="utf-8"), re.S)
    if check(tag_id is not None, "no block tag id parsed out of EngineersPick.java"):
        path = ROOT / "kubejs/data" / NAMESPACE / "tags/block" / (tag_id.group(1) + ".json")
        if check(path.is_file(),
                 "EngineersPick asks for the block tag `%s:%s`, which no file defines -- every "
                 "block falls back to vanilla hardness and Factorio's flat 2.0s is gone"
                 % (NAMESPACE, tag_id.group(1))):
            check(json.loads(path.read_text())["values"],
                  "the flat-mining-time tag is empty, which is the same as not existing")

    return report()


def report():
    for failure in failures:
        print("FAIL " + failure)
    if failures:
        return 1
    print("ok   %d hand-written recipe(s), both surfaces admitted and both picks dressed"
          % len(list(PACK.glob("*.json"))))
    return 0


if __name__ == "__main__":
    sys.exit(main())
