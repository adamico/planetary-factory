#!/usr/bin/env python3
"""Assert the machines `machines.js` registers agree with everything that names them.

`docs/testing/what-to-check.md`'s "cross-file references resolve" claim, for the machine
registration seam. A world load proves a machine registered; what it does NOT prove is that
anything referring to it by name is still right, because every failure here is a warning in a
log nobody reads or a silently untranslated string:

  - the `setMaxIOSize` a recipe type registers is the envelope of its routed recipes. The
    corpus fixture in `tests/factorio/test_recipe_extract.py` is the source of truth, and it
    compares the corpus against itself -- it never opens `machines.js`. Without this file,
    editing a registered GUI is a silent widening, which is exactly what that fixture exists to
    prevent (#107, ADR-0025)
  - `data/pack/category-map.json` names a `recipe_type` per machine, and `#87`'s converter emits
    against it. A type the map names and the script does not register is a recipe with nowhere
    to go
  - a machine's display name is a lang key built from its registered id, and the two builders
    put their machines in DIFFERENT namespaces: the tiered builder registers through GregTech's
    registrate (`gtceu:<tier>_<name>`) and the multiblock wrapper through KubeJS's own
    (`kubejs:<name>`). A lang key aimed at the wrong namespace is an untranslated block, and
    nothing logs it
  - GregTech's runtime model provider generates nothing for the `kubejs:` namespace, so the
    multiblock ships an authored blockstate, block model and item model. Every hop of that chain
    -- blockstate to model, model to parent, model to texture -- is a file that must exist, and
    a broken one renders untextured with only a client-side warning

WHAT IT CANNOT PROVE is that the machines behave: that the pattern forms, that the GUI has the
slots the recipe type asked for, or that EMI shows them. That is the world load, and the last of
it is owed to a human (`docs/testing/what-to-check.md`).

Usage: tests/pack/test_machine_assets.py
"""
import importlib.util
import json
import re
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent.parent
MACHINES = ROOT / "kubejs/startup_scripts/machines.js"
CATEGORY_MAP = ROOT / "data/pack/category-map.json"
ASSETS = ROOT / "kubejs/assets"

# The tiered builder prefixes each tier's short name onto the machine's own; the multiblock
# wrapper registers one block under the bare name. Both are read out of a running game (#107).
TIER_PREFIX = {"GTValues.LV": "lv", "GTValues.MV": "mv", "GTValues.HV": "hv"}

failures = []


def check(condition, message):
    if not condition:
        failures.append(message)


def expected_io():
    """The corpus envelope, imported rather than copied, so there is one fixture and not two."""
    path = ROOT / "tests/factorio/test_recipe_extract.py"
    spec = importlib.util.spec_from_file_location("test_recipe_extract", path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module.EXPECTED_IO


def registered_types(text):
    """`GTRecipeTypes.register('name', ...)` and the `setMaxIOSize` that follows it.

    The sizes are written as a named constant indexed four times -- `NAME_IO[0]` through
    `[3]` -- so the constant is resolved here rather than the call being read literally.
    """
    sizes = {name: json.loads(value)
             for name, value in re.findall(r"const (\w+_IO) = (\[[^\]]*\]);", text)}
    types = {}
    for name, call in re.findall(
            r"GTRecipeTypes\.register\('(\w+)'.*?\.setMaxIOSize\(([^)]*)\)", text, re.S):
        constants = {re.match(r"(\w+_IO)\[", arg.strip()).group(1)
                     for arg in call.split(",") if re.match(r"\w+_IO\[", arg.strip())}
        check(len(constants) == 1,
              "recipe type `%s` does not take its four IO sizes from one `*_IO` constant, so "
              "this check cannot read them" % name)
        if len(constants) == 1:
            constant = constants.pop()
            check(constant in sizes, "recipe type `%s` names %s, which is not declared"
                  % (name, constant))
            types[name] = tuple(sizes.get(constant, ()))
    return types


def registered_machines(text):
    """`event.create('name')` with its tiers, or `event.create('name', 'multiblock')`.

    Returns the block ids the game will hold, which is what a lang key has to match.
    """
    ids = {}
    for name, kind in re.findall(r"event\.create\('(\w+)'(?:,\s*'(\w+)')?\)", text):
        if kind == "multiblock":
            ids[name] = ["kubejs:" + name]
            continue
        tail = text.split("event.create('%s'" % name, 1)[1]
        tiers = re.search(r"\.tiers\(([^)]*)\)", tail.split("event.create(")[0])
        check(tiers, "machine `%s` is not a multiblock and declares no tiers" % name)
        ids[name] = ["gtceu:%s_%s" % (TIER_PREFIX[tier.strip()], name)
                     for tier in (tiers.group(1).split(",") if tiers else [])
                     if check(tier.strip() in TIER_PREFIX,
                              "machine `%s` names tier %s, which this check does not know"
                              % (name, tier.strip())) is None and tier.strip() in TIER_PREFIX]
    return ids


def lang_entries():
    entries = {}
    for path in sorted(ASSETS.glob("*/lang/en_us.json")):
        entries[path.parent.parent.name] = json.loads(path.read_text())
    return entries


def gregtech_jar():
    jars = sorted((ROOT / "mods").glob("gtceu-*.jar"))
    return zipfile.ZipFile(jars[-1]) if jars else None


def resolve_assets(block_ids, jar):
    """Walk every authored blockstate to its models and their textures.

    Only the `kubejs:` namespace is authored -- GregTech generates the rest at runtime -- so a
    `gtceu:` id with no file here is correct rather than missing.
    """
    for block_id in block_ids:
        namespace, name = block_id.split(":")
        if namespace != "kubejs":
            continue
        blockstate = ASSETS / namespace / "blockstates" / (name + ".json")
        check(blockstate.exists(),
              "%s registers in the `kubejs:` namespace, which GregTech's runtime model provider "
              "does not serve, and has no authored blockstate at %s"
              % (block_id, blockstate.relative_to(ROOT)))
        if not blockstate.exists():
            continue
        models = {variant["model"] for variant in json.loads(blockstate.read_text())["variants"].values()}
        models.add("%s:item/%s" % (namespace, name))
        seen = set()
        while models:
            model_id = models.pop()
            if model_id in seen:
                continue
            seen.add(model_id)
            model_ns, model_path = model_id.split(":")
            if model_ns != "kubejs":
                # A vanilla or GregTech parent is theirs to ship, not ours to author.
                continue
            model_file = ASSETS / model_ns / "models" / (model_path + ".json")
            check(model_file.exists(), "%s references model %s, which does not exist at %s"
                  % (block_id, model_id, model_file.relative_to(ROOT)))
            if not model_file.exists():
                continue
            model = json.loads(model_file.read_text())
            if "parent" in model:
                models.add(model["parent"] if ":" in model["parent"]
                           else "minecraft:" + model["parent"])
            for texture in model.get("textures", {}).values():
                texture_ns, texture_path = (texture.split(":") if ":" in texture
                                            else ("minecraft", texture))
                if texture_ns != "gtceu" or jar is None:
                    continue
                entry = "assets/gtceu/textures/%s.png" % texture_path
                try:
                    jar.getinfo(entry)
                except KeyError:
                    failures.append("%s references texture %s, which is not in the GregTech jar"
                                    % (model_id, texture))


def main():
    text = MACHINES.read_text()
    types = registered_types(text)
    machines = registered_machines(text)
    envelope = expected_io()
    routes = json.loads(CATEGORY_MAP.read_text())["machines"]
    lang = lang_entries()
    jar = gregtech_jar()
    check(jar is not None, "no gtceu jar in mods/, so texture references cannot be resolved")

    for name, size in types.items():
        check(name in envelope,
              "recipe type `%s` is registered and `tests/factorio/test_recipe_extract.py` has no "
              "envelope for it" % name)
        if name in envelope:
            check(size == envelope[name],
                  "recipe type `%s` registers setMaxIOSize%s, but the corpus envelope is %s -- "
                  "widening a registered GUI is a decision, not a detail (ADR-0025)"
                  % (name, size, envelope[name]))

    for surface, machine in routes.items():
        recipe_type = machine["recipe_type"]
        if recipe_type is None or not recipe_type.startswith("gtceu:"):
            # Not registered yet, or vanilla's -- neither is this script's to declare.
            continue
        registered = recipe_type.split(":", 1)[1]
        check(registered in types,
              "category-map.json routes `%s` to %s, which machines.js does not register"
              % (surface, recipe_type))

    for name, block_ids in machines.items():
        for block_id in block_ids:
            namespace, path = block_id.split(":")
            key = "block.%s.%s" % (namespace, path)
            check(key in lang.get(namespace, {}),
                  "machine `%s` registers as %s and nothing names it: no `%s` in "
                  "kubejs/assets/%s/lang/en_us.json" % (name, block_id, key, namespace))
        resolve_assets(block_ids, jar)

    for failure in failures:
        print("FAIL: " + failure)
    if failures:
        print("\n%d failure(s)" % len(failures))
        return 1
    print("ok   %d recipe type(s), %d machine(s), %d block id(s) named and rendered"
          % (len(types), len(machines), sum(len(ids) for ids in machines.values())))
    return 0


if __name__ == "__main__":
    sys.exit(main())
