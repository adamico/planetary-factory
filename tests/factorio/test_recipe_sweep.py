#!/usr/bin/env python3
"""Assert ADR-0034's default-deny sweep and its survivor allowlist still agree with the decisions.

This is #143's static half -- the "cross-file references resolve" check in
`docs/testing/what-to-check.md`'s terms. It launches no game, so what it can prove is that the
files agree with each other:

  - `recipes.js` removes by the NEGATION of the allowlist. A sweep that listed removals instead
    would go stale on every version bump, which is the shape ADR-0034 rejected
  - the allowlist is `var` and loads first. ADR-0022: load order is the `// priority:` header and
    not the filename, and a cross-file share has to be `var`. Get either wrong and the sweep dies
    with a `ReferenceError` in a script KubeJS still reports as 0 errors -- neither fact is
    visible to any amount of reading, which is why they are asserted here
  - every survivor entry names a surface, a filter and a decision. Never a bare filter
  - every surface is a machine in `data/pack/category-map.json`, and its filter's recipe type is
    that machine's registered `recipe_type`
  - every machine with a registered `recipe_type` has a survivor. This is the assertion that
    catches #107's Chemical Plant or #135's Centrifuge landing and having its recipes swept in
    silence, which reaches the player as a machine with no recipes and nothing in any log
  - a machine still waiting on its ticket (`recipe_type: null`) has no survivor, so the allowlist
    cannot admit a surface that does not exist yet
  - every recipe the converter emits is covered by a survivor -- otherwise the sweep removes the
    pack's own output
  - #97: nothing the pack emits, and nothing the allowlist admits, is a vanilla grid recipe

WHAT IT CANNOT PROVE is that the sweep removed the right things in a running game: KubeJS filters
are evaluated against a loaded recipe manager that exists only in the JVM. That is #143's world
load -- 0 failed recipes and no JEI route to a removed idiom -- and it needs a human.

Usage: tests/factorio/test_recipe_sweep.py
"""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent.parent
SCRIPTS = ROOT / "kubejs/server_scripts"
EMITTED = ROOT / "kubejs/data/planetaryfactory/recipe"
CATEGORY_MAP = ROOT / "data/pack/category-map.json"

# The two vanilla types #90 and #34 left with no block in the pack to execute them. A survivor on
# either is unreachable by construction, which is #97's rule read backwards (ADR-0034).
GRID_TYPES = {"minecraft:crafting_shaped", "minecraft:crafting_shapeless"}

failures = []


def check(condition, message):
    if not condition:
        failures.append(message)


def survivors():
    """The allowlist entries, read out of the script that declares them.

    Parsed rather than imported: the file is a KubeJS script and its only reader is Rhino. The
    parse is deliberately strict -- an entry it cannot read is a failure, not a skip.
    """
    text = (SCRIPTS / "recipe_survivors.js").read_text()
    body = text.split("var RECIPE_SURVIVORS = [", 1)
    check(len(body) == 2,
          "recipe_survivors.js declares no `var RECIPE_SURVIVORS = [` -- a `const` is file-local "
          "in KubeJS and recipes.js would die with a ReferenceError (ADR-0022)")
    if len(body) != 2:
        return []
    entries = []
    for block in re.findall(r"\{\s*surface:(.*?)\n  \}", body[1], re.S):
        entry = {}
        for key in ("surface", "why"):
            found = re.search(key + r":\s*([\"'])(.*?)\1\s*(?:[,\n]|$)", "surface:" + block, re.S)
            entry[key] = found.group(2) if found else None
        found = re.search(r"type:\s*'([^']*)'", block)
        entry["type"] = found.group(1) if found else None
        entries.append(entry)
    check(entries, "recipe_survivors.js declares no readable survivor entries")
    # An entry this parser cannot read must fail rather than vanish: a skipped entry is a
    # surface silently swept, which is the failure the whole check exists to catch.
    check(len(entries) == body[1].count("\n  {"),
          "recipe_survivors.js has %d entries and %d are readable -- an entry must open with "
          "`surface:` and close on `  }`" % (body[1].count("\n  {"), len(entries)))
    return entries


def main():
    machines = json.loads(CATEGORY_MAP.read_text())["machines"]
    entries = survivors()

    sweep = (SCRIPTS / "recipes.js").read_text()
    priorities = {}
    for name in ("recipe_survivors.js", "recipes.js"):
        header = re.match(r"// priority: (-?\d+)", (SCRIPTS / name).read_text())
        check(header, "%s carries no `// priority:` header, so its load order is arbitrary "
                      "(ADR-0022)" % name)
        priorities[name] = int(header.group(1)) if header else None
    if all(value is not None for value in priorities.values()):
        check(priorities["recipe_survivors.js"] > priorities["recipes.js"],
              "recipe_survivors.js (priority %d) must load before recipes.js (priority %d) -- "
              "KubeJS sorts by the header descending" % (priorities["recipe_survivors.js"],
                                                         priorities["recipes.js"]))
    check("RECIPE_SURVIVORS" in sweep,
          "recipes.js does not read RECIPE_SURVIVORS -- the sweep and the allowlist have parted")
    check(re.search(r"event\.remove\(\s*\{\s*not:", sweep),
          "recipes.js does not remove by the negation of the allowlist (ADR-0034's default-deny)")

    # ADR-0034 as amended by #172: A SURVIVOR NAMES A SURFACE, NOT A MOD. #144 asked whether the
    # allowlist could admit a whole mod's line in one row -- `{ mod: 'powergrid' }` -- and the
    # answer is no: 84 of Create: Power Grid's 112 recipes sit on surfaces no block in this pack
    # executes, so a mod-wide admission would have put 84 entries in EMI that are craftable
    # nowhere. That is the failure ADR-0034 exists to name, and it is a one-word edit away, so it
    # is asserted rather than trusted. The pack's namespace is applied ONCE, in recipes.js, to
    # every entry; a row carrying its own `mod` would escape that.
    check(re.search(r"mod:\s*'planetaryfactory'", sweep),
          "recipes.js does not apply `mod: 'planetaryfactory'` to the survivors. Every survivor "
          "is by definition a recipe the pack authored (ADR-0034 as amended by #172) -- dropping "
          "the namespace admits every other mod's recipes on the same surface")
    check("RECIPE_SURVIVORS.map" in sweep,
          "recipes.js no longer maps EVERY survivor onto the pack namespace -- if entries can opt "
          "out, a survivor can name a foreign mod, which ADR-0034 refuses (#172)")
    allowlist = (SCRIPTS / "recipe_survivors.js").read_text()
    stray = re.search(r"^\s*mod:", allowlist, re.M)
    check(stray is None,
          "recipe_survivors.js declares a `mod:` key. A survivor names a SURFACE and the pack's "
          "own namespace is applied in recipes.js (ADR-0034 as amended by #172); admitting a mod "
          "ships recipes on surfaces nothing in the pack executes")

    by_surface = {}
    for entry in entries:
        name = entry["surface"]
        check(name is not None, "a survivor entry names no surface")
        check(entry["why"], "survivor `%s` names no decision" % name)
        check(entry["type"], "survivor `%s` names no recipe type" % name)
        check(entry["type"] not in GRID_TYPES,
              "survivor `%s` admits a vanilla grid recipe, which no block in the pack executes (#97)"
              % name)
        check(name not in by_surface, "surface `%s` has more than one survivor entry" % name)
        by_surface[name] = entry

    for name, entry in by_surface.items():
        check(name in machines,
              "survivor `%s` names no machine in category-map.json" % name)
        if name in machines:
            check(entry["type"] == machines[name]["recipe_type"],
                  "survivor `%s` admits type %r, but category-map.json registers %r"
                  % (name, entry["type"], machines[name]["recipe_type"]))

    for name, machine in machines.items():
        if machine["recipe_type"] is None:
            check(name not in by_surface,
                  "`%s` has no registered recipe type yet, so it cannot have a survivor" % name)
        else:
            check(name in by_surface,
                  "machine `%s` is registered as %s and has no survivor -- the sweep removes its "
                  "recipes" % (name, machine["recipe_type"]))

    admitted = {entry["type"] for entry in entries}
    for path in sorted(EMITTED.rglob("*.json")):
        emitted_type = json.loads(path.read_text())["type"]
        check(emitted_type not in GRID_TYPES,
              "%s is a vanilla grid recipe (#97)" % path.name)
        check(emitted_type in admitted,
              "%s is emitted as %s, which no survivor admits -- the sweep removes it"
              % (path.name, emitted_type))

    for failure in failures:
        print("FAIL: " + failure)
    if failures:
        print("\n%d failure(s)" % len(failures))
        return 1
    print("ok: %d survivor(s), %d emitted recipe(s)" % (len(entries), len(list(EMITTED.rglob("*.json")))))
    return 0


if __name__ == "__main__":
    sys.exit(main())
