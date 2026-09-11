#!/usr/bin/env python3
"""Assert the hand-written recipe subtrees, and the Engineer's Pick's pack-side files.

`docs/testing/what-to-check.md`'s "cross-file references resolve" claim, for the one subtree of
`kubejs/data/planetaryfactory/recipe/` that no converter generates.

`recipe/assembling/pack/` is ADR-0031's single stated exception: the corpus authors every recipe it contains,
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
    kubejs/data/planetaryfactory/recipe/assembling/pack/README.md` -- and that ERROR stops a world from
    loading. It is asserted here because this subtree is the one place a human writes files under
    `kubejs/` by hand rather than generating them, and a README next to the recipes is the obvious
    thing to reach for.
  - a pick with no model, texture or lang key -- the black-and-magenta cube and a raw translation
    key, neither of which is logged as an error.
  - the steel recipe not consuming the iron pick, which ADR-0039 states in one line and which no
    other file would notice.

ADR-0051 adds the second such subtree, `recipe/assembling/sapling/`, for the same reason and with a
different one behind it: Factorio's `tree-seed` is a recipe costing `wood x2`, and its wild trees drop
no seed at all, so the pack carries that over -- felling drops no sapling and replanting is bought
with logs. The corpus cannot author these either, because Factorio's seed is one generic item and
Minecraft's sapling is a species.

That species-ness is what this check is really for. `#minecraft:oak_logs -> oak_sapling` is only
correct while Terra actually grows oak; a recipe for a species no biome places is a sapling the
player can never plant a second of, and nothing else in the repo compares the two. So the species
list is read out of `kubejs/data/planetaryfactory/worldgen/biome/terra_*.json` rather than typed, and
the recipes are asserted against it in both directions.

The `fellable` block tag is here for the same class of reason: the mod names it with a `TagKey`,
which resolves to an empty tag rather than an error when the JSON is missing, and an empty tag means
no tree in the pack fells with no log line anywhere.

The item ids are read out of `PickTier.java` rather than typed here, so a third tier fails this
check instead of shipping without assets or a recipe.

Usage: tests/factorio/test_pack_recipes.py
"""
import json
import re
import subprocess
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
EMITTED = ROOT / "kubejs/data/planetaryfactory/recipe"
SUBTREE = "assembling/pack"
PACK = EMITTED / SUBTREE
SAPLING_SUBTREE = "assembling/sapling"
SAPLINGS = EMITTED / SAPLING_SUBTREE
BIOMES = ROOT / "kubejs/data/planetaryfactory/worldgen/biome"
FELLABLE_TAG = ROOT / "kubejs/data/planetaryfactory/tags/block/fellable.json"
FELLING = ROOT / "mod/src/main/java/com/planetaryfactory/core/felling/TreeFelling.java"
# Which vanilla tree placement carries which species. Terra's biomes name the placed feature, and
# the feature is what decides whether a sapling recipe has a tree behind it.
PLACEMENT_SPECIES = {
    "trees_plains": ("oak",),
    "trees_birch_and_oak": ("oak", "birch"),
    # 26.1's name for the same selector: oak by default, birch at 0.2, plus fallen logs of each.
    "trees_birch_and_oak_leaf_litter": ("oak", "birch"),
    "trees_savanna": ("acacia",),
    "trees_sparse_jungle": ("jungle",),
    "trees_taiga": ("spruce",),
}
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
TEXTURE_BUILDER = ROOT / "scripts/build-pick-textures.py"
MODS = ROOT / "mods"
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


def texture_resolves(item, layer):
    """That an item model's texture is one that will actually be there at load.

    The two picks are dressed from three different places on purpose, so this cannot be a single
    equality: the Iron Pick wears vanilla's own `minecraft:item/iron_pickaxe` (nothing to copy), and
    the Steel Pick wears GTCEu's Damascus Steel pickaxe flattened into our namespace by
    `scripts/build-pick-textures.py`, because GT's tool art is three greyscale layers that only
    become a material under GregTech's item-colour handler -- which never sees an item that is not
    a GT tool.

    A texture that is not there renders as the black-and-magenta checkerboard with only a
    client-side warning, so each namespace is resolved where it can be: our own against the file,
    a mod's against the jar the pack ships, and vanilla's against nothing -- the client jar is not
    in this repo, and `minecraft:item/iron_pickaxe` is not a name that moves.
    """
    namespace, _, path = layer.partition(":")
    if namespace == NAMESPACE:
        check((ASSETS / "textures" / (path + ".png")).is_file(),
              "%s's model points at %r and that file is missing, which renders as the "
              "missing-texture checkerboard" % (item, layer))
    elif namespace == "minecraft":
        return
    else:
        jars = sorted(MODS.glob("%s-*.jar" % namespace))
        if check(len(jars) == 1,
                 "%s's model points at %r, and mods/ holds %d %s jar(s) to resolve it against"
                 % (item, layer, len(jars), namespace)):
            with zipfile.ZipFile(jars[0]) as jar:
                names = set(jar.namelist())
            check(("assets/%s/textures/%s.png" % (namespace, path)) in names,
                  "%s's model points at %r, which the installed %s jar does not contain"
                  % (item, layer, namespace))


def terra_species():
    """The tree species Terra's own biomes place, read from the biome files."""
    species = set()
    for path in sorted(BIOMES.glob("terra_*.json")):
        text = path.read_text(encoding="utf-8")
        for placement, names in PLACEMENT_SPECIES.items():
            if ('"minecraft:%s"' % placement) in text:
                species.update(names)
    return species


def check_saplings():
    """ADR-0051's sapling recipes: one per species Terra grows, and none for one it does not."""
    grown = terra_species()
    check(bool(grown),
          "no Terra biome places a tree placement this check knows -- either worldgen changed or "
          "PLACEMENT_SPECIES is stale, and either way the sapling recipes are unchecked")
    if not check(SAPLINGS.is_dir(),
                 "%s does not exist -- ADR-0051 replaces the dropped sapling with a recipe, so "
                 "without it felling deletes replanting" % SAPLINGS):
        return

    recipes = {p.stem: json.loads(p.read_text()) for p in sorted(SAPLINGS.glob("*.json"))}
    expected = {"%s_sapling" % name for name in grown}
    check(set(recipes) == expected,
          "recipe/%s/ holds %s; Terra grows %s. A recipe for a species no biome places is a sapling "
          "nobody can plant twice, and a species with no recipe has no way back after a fell"
          % (SAPLING_SUBTREE, sorted(recipes), sorted(expected)))

    types = survivor_types()
    for name, recipe in sorted(recipes.items()):
        where = "%s/%s.json" % (SAPLING_SUBTREE, name)
        species = name[: -len("_sapling")]
        check(recipe["type"] in types.values(),
              "%s is type %r, which recipe_survivors.js does not admit -- ADR-0034's sweep removes "
              "it on load with no error" % (where, recipe["type"]))
        check(recipe.get("data", {}).get("factorio_category") == HAND_CATEGORY,
              "%s is not category %r, so the Personal Assembler will not plan it and a sapling "
              "needs a machine the player has no reason to have built" % (where, HAND_CATEGORY))
        check(items_of(recipe, "outputs") == [("minecraft:%s" % name, 1)],
              "%s does not output one %s" % (where, name))
        inputs = dict(items_of(recipe, "inputs"))
        check(inputs == {"#minecraft:%s_logs" % species: 2},
              "%s takes %s; Factorio's `tree-seed` costs `wood x2`, and the species has to match -- "
              "two birch logs must not buy an oak" % (where, sorted(inputs)))


def check_fellable_tag():
    """The tag ADR-0051's fill reads. A missing one is an empty tag, which fells nothing."""
    if not check(FELLABLE_TAG.is_file(),
                 "%s is missing. The mod names it with a TagKey, which resolves to an EMPTY tag "
                 "rather than an error -- so every tree in the pack silently stops felling"
                 % FELLABLE_TAG.name):
        return
    values = json.loads(FELLABLE_TAG.read_text(encoding="utf-8")).get("values") or []
    check("#minecraft:logs" in values,
          "fellable.json does not carry `#minecraft:logs`, so Terra's own trees do not fell")
    declared = re.search(r'"fellable"', FELLING.read_text(encoding="utf-8"))
    check(declared is not None,
          "TreeFelling.java no longer names the `fellable` tag; the JSON and the TagKey are the two "
          "halves of one lookup and neither fails loudly on its own")


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

    check_saplings()
    check_fellable_tag()

    # The pack-side files each registered pick needs. Every one of these fails silently.
    lang = json.loads((ASSETS / "lang/en_us.json").read_text(encoding="utf-8"))
    for item in sorted(picks):
        check(("item.%s.%s" % (NAMESPACE, item)) in lang,
              "%s has no lang key, so it ships showing its raw translation key" % item)
        model = ASSETS / "models/item" / (item + ".json")
        if check(model.is_file(), "%s has no item model" % item):
            layer = json.loads(model.read_text())["textures"]["layer0"]
            texture_resolves(item, layer)
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

    # The generated half of the Steel Pick's texture. Generated output is never hand-edited here;
    # a GTCEu update that changed its tool art would otherwise leave the pack showing the old one
    # with nothing to say so.
    if check(TEXTURE_BUILDER.is_file(), "scripts/build-pick-textures.py is missing"):
        built = subprocess.run([sys.executable, str(TEXTURE_BUILDER), "--check"],
                               capture_output=True, text=True)
        check(built.returncode == 0,
              "the Steel Pick's texture is stale against the installed GTCEu jar -- re-run "
              "scripts/build-pick-textures.py (%s)" % built.stdout.strip())

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
    print("ok   %d pick recipe(s) and %d sapling recipe(s), every surface admitted, both picks "
          "dressed and the fellable tag resolved"
          % (len(list(PACK.glob("*.json"))), len(list(SAPLINGS.glob("*.json")))))
    return 0


if __name__ == "__main__":
    sys.exit(main())
