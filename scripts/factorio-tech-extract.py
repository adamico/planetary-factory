#!/usr/bin/env python3
"""Extract Factorio's Space Age technology tree as data the pack's researches are built from.

Researchd's tree is ours, but its *shape* does not have to be invented. Factorio has
spent a decade playtesting the order in which a factory learns things, and that ordering
is the one part of the design worth importing wholesale (ADR-0022). What is not worth
importing is Factorio's content: its recipes, its items, its four planets. So this script
takes the topology and the costs, and leaves every Minecraft-specific decision -- icon,
unlocked recipe, planet gating -- to be hand-authored against the extracted file.

The data comes from `factorio --dump-data`, which writes every prototype the game loaded
to `script-output/data-raw-dump.json`. That dump has no mod attribution: nothing in a
prototype records which mod defined it. Since `space-age` hard-depends on `quality` and
`elevated-rails`, a base+SA-only load is impossible, and the filter has to be applied
afterwards. The allowlist is grepped out of the two `technology.lua` files on disk,
anchored on `type = "technology"` so that recipe names appearing inside `effects` cannot
be mistaken for technology names.

Three families of technology are dropped, because Researchd has no concept that fits them
and a hand translation would be invented rather than imported:

  - `max_level = "infinite"`   -- mining productivity and friends, unbounded
  - `count_formula`            -- cost as an expression of the level, not a number
  - `upgrade = true`           -- the levelled bonus chains (robot speed, braking force,
                                  stack size, inserter capacity)

`upgrade` is the load-bearing flag; the other two mostly co-occur with it. A technology
whose name ends in a digit but which carries none of the three is *reported* rather than
filtered, so that a family this misses is visible instead of silent.

Dropping a technology would orphan its children, so prerequisites are re-pointed through
every dropped node to the nearest surviving ancestors. That walk is the only real logic
here, and it is what `tests/factorio/test_tech_extract.py` checks.

Usage:

    scripts/factorio-tech-extract.py            # finds the Steam install and dump
    scripts/factorio-tech-extract.py --dump PATH --factorio-data PATH
"""

import argparse
import json
import re
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent

DEFAULT_DUMP = (
    Path.home()
    / "Library/Application Support/factorio/script-output/data-raw-dump.json"
)
DEFAULT_DATA = (
    Path.home()
    / "Library/Application Support/Steam/steamapps/common/Factorio"
    / "factorio.app/Contents/data"
)

# The mods whose technologies we keep. `quality` and `elevated-rails` also load, because
# space-age requires them, and are filtered back out here: quality is an orthogonal system
# rather than a tier, and elevated rail has no analogue in a Minecraft pack.
#
# `recycler` is Space Age content in every sense but the folder name -- Factorio splits it
# out for packaging, but `recycling` is gated on discovering Fulgora and `holmium-processing`
# depends on it. Excluding it would be an artifact of where the file lives.
SOURCE_MODS = ("base", "space-age", "recycler")

TECH_DECL = re.compile(r'type\s*=\s*"technology"\s*,\s*\n\s*name\s*=\s*"([a-z0-9_-]+)"')
LEVELLED_NAME = re.compile(r"-\d+$")


def allowlist(data_dir):
    """Technology names each mod defines, and which mod defined each one.

    Anchored on the `type = "technology"` line rather than a bare `name =`, because
    `technology.lua` is full of recipe and item names inside `effects` blocks.
    """
    owner = {}
    for mod in SOURCE_MODS:
        # base and space-age keep theirs in prototypes/technology.lua; recycler declares
        # its single technology inline in data.lua, so the whole mod is scanned.
        sources = sorted((data_dir / mod).rglob("*.lua"))
        if not sources:
            sys.exit(f"no lua sources for '{mod}' under {data_dir / mod}")
        found = 0
        for source in sources:
            for name in TECH_DECL.findall(source.read_text(encoding="utf-8", errors="replace")):
                owner.setdefault(name, mod)
                found += 1
        if not found:
            sys.exit(f"'{mod}' declares no technologies -- did the prototype form change?")
    return owner


def locale_names(data_dir):
    """The `[technology-name]` section of each mod's English locale, flattened.

    The dump gives a technology no display name at all: Factorio resolves
    `technology-name.<name>` at draw time. Since every research is hand-written with a
    literal name anyway, resolving here means the reference file reads like the game.
    """
    names = {}
    for mod in SOURCE_MODS:
        for path in sorted((data_dir / mod / "locale" / "en").glob("*.cfg")):
            section = None
            for line in path.read_text(encoding="utf-8", errors="replace").splitlines():
                line = line.strip()
                if line.startswith("[") and line.endswith("]"):
                    section = line[1:-1]
                elif section == "technology-name" and "=" in line and not line.startswith("#"):
                    key, _, value = line.partition("=")
                    names[key.strip()] = value.strip()
    return names


def pf_id(name):
    return "planetary_factory:" + name.replace("-", "_")


def is_pruned(tech):
    """Whether a technology is one of the three families Researchd cannot express."""
    if tech.get("max_level") == "infinite":
        return "infinite"
    if "count_formula" in tech.get("unit", {}):
        return "count_formula"
    if tech.get("upgrade") is True:
        return "upgrade"
    return None


def surviving_parents(name, raw, keep, seen=None):
    """Prerequisites of `name`, with every dropped node walked through to its own parents.

    A pruned technology is transparent rather than terminal: dropping `mining-productivity-2`
    must not orphan whatever depended on it. `seen` guards against a prerequisite reachable
    by two paths being emitted twice, and against a cycle in malformed data.
    """
    if seen is None:
        seen = set()
    out = []
    for parent in raw.get(name, {}).get("prerequisites", []) or []:
        if parent in seen:
            continue
        seen.add(parent)
        if parent in keep:
            out.append(parent)
        elif parent in raw:
            out.extend(surviving_parents(parent, raw, keep, seen))
    return out


def icon_path(tech):
    """The first icon path, whether the prototype uses `icon` or the layered `icons` form.

    Useless as a Minecraft texture -- it survives only as a hint when picking a
    `gtceu:`/`create:` item to stand in for the technology.
    """
    if "icon" in tech:
        return tech["icon"]
    layers = tech.get("icons") or []
    return layers[0].get("icon") if layers else None


RECYCLING_SUFFIX = "-recycling"


def collapse_effects(effects, derivable):
    """Replace Factorio's generated reverse-craft recipes with the rule that made them.

    `recycling` unlocks 314 recipes, an order of magnitude more than any other technology.
    They are not 314 decisions: `recycler/data-updates.lua` generates one per existing
    recipe, returning a quarter of its ingredients. Recording the rule is both smaller and
    truer than recording its output, and it matches how the pack will implement it -- as
    one decision about GregTech maceration, not 314 recipe ids.

    A recipe only collapses if the name it is derived *from* actually exists, or if the
    recipe is hidden -- the hidden ones are the void recyclings (blueprints, fish,
    planners: items that recycle to nothing), whose source prototypes live in tables other
    than `recipe` and `item`. A visible, hand-authored recipe that merely ends in
    `-recycling` is left alone.
    """
    out = []
    collapsed = 0
    for effect in effects:
        recipe = effect.get("recipe")
        if (
            effect.get("type") == "unlock-recipe"
            and recipe
            and recipe.endswith(RECYCLING_SUFFIX)
            and (recipe[: -len(RECYCLING_SUFFIX)] in derivable or effect.get("hidden"))
        ):
            collapsed += 1
            continue
        out.append(effect)
    if collapsed:
        out.append(
            {
                "type": "unlock-recipe-family",
                "rule": "reverse-craft",
                "recipes": collapsed,
            }
        )
    return out


def cost(tech):
    """Cost as either a science-pack bill or a Space Age research trigger.

    Space Age gives many technologies no pack cost at all: instead a `research_trigger`
    fires on crafting an item, mining an entity, capturing a spawner. That is a direct
    analogue of Researchd's `method(ResearchMethodHelper.consumeItem(...))`, and these are
    the technologies that translate into the pack most cleanly, so the discriminator is
    kept explicit rather than inferred from a missing field.
    """
    trigger = tech.get("research_trigger")
    if trigger:
        return "trigger", None, trigger
    unit = tech.get("unit") or {}
    return (
        "packs",
        {
            "count": unit.get("count"),
            "time": unit.get("time"),
            "ingredients": [list(i) for i in unit.get("ingredients", [])],
        },
        None,
    )


def science_packs(dump):
    """The science packs, in Factorio's own order.

    Twelve packs is not enough tedium to deserve a DSL -- each needs a colour, a sorting
    value and a crafting recipe, all hand decisions. What is worth recording is the order,
    which is otherwise reconstructed from memory.

    Science packs are plain `item` prototypes as of 2.1; they were `tool` in earlier
    versions, so a future extraction returning zero packs means this key moved again.
    """
    packs = []
    for name, item in (dump.get("item") or {}).items():
        if not name.endswith("science-pack"):
            continue
        packs.append(
            {
                "name": name,
                "suggested_id": pf_id(name),
                "order": item.get("order", ""),
                "subgroup": item.get("subgroup", ""),
                "hidden": "hidden" in (item.get("flags") or []),
            }
        )
    packs.sort(key=lambda p: (p["order"], p["name"]))
    return packs


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--dump", type=Path, default=DEFAULT_DUMP)
    parser.add_argument("--factorio-data", type=Path, default=DEFAULT_DATA)
    parser.add_argument("--out-dir", type=Path, default=REPO / "data" / "factorio")
    parser.add_argument(
        "--kubejs-out",
        type=Path,
        default=REPO / "kubejs" / "server_scripts" / "factorio_tech_data.js",
    )
    args = parser.parse_args()

    if not args.dump.is_file():
        sys.exit(
            f"no dump at {args.dump}\n"
            "run:  factorio --dump-data --mod-directory <dir with base+SA only>"
        )

    dump = json.loads(args.dump.read_text(encoding="utf-8"))
    raw = dump.get("technology") or {}
    derivable = set(dump.get("recipe") or {}) | set(dump.get("item") or {})
    owner = allowlist(args.factorio_data)
    names = locale_names(args.factorio_data)

    contaminated = sorted(set(raw) - set(owner))
    # Declared in technology.lua but absent from the dump: a later data stage removed or
    # renamed it. Harmless, but it means the allowlist and the game disagree.
    vanished = sorted(set(owner) - set(raw))
    pruned = {}
    keep = set()
    for name in raw:
        if name not in owner:
            continue
        reason = is_pruned(raw[name])
        if reason:
            pruned[name] = reason
        else:
            keep.add(name)

    # A levelled family this missed would silently halve someone's tech tree, so say so.
    suspicious = sorted(
        n for n in keep if LEVELLED_NAME.search(n) and not n.endswith("science-pack")
    )

    techs = []
    for name in sorted(keep):
        tech = raw[name]
        kind, unit, trigger = cost(tech)
        techs.append(
            {
                "name": name,
                "suggested_id": pf_id(name),
                "localised_name": names.get(name, name),
                "source": owner[name],
                "essential": bool(tech.get("essential")),
                "prerequisites": surviving_parents(name, raw, keep),
                "cost_kind": kind,
                "unit": unit,
                "research_trigger": trigger,
                "effects": collapse_effects(tech.get("effects") or [], derivable),
                "icon": icon_path(tech),
            }
        )

    args.out_dir.mkdir(parents=True, exist_ok=True)
    tech_path = args.out_dir / "technology.json"
    tech_path.write_text(json.dumps(techs, indent=2) + "\n", encoding="utf-8")

    packs = science_packs(dump)
    (args.out_dir / "science_packs.json").write_text(
        json.dumps(packs, indent=2) + "\n", encoding="utf-8"
    )

    # KubeJS reads this rather than the JSON: the pack's KubeJS version is pinned, and
    # JsonIO's path semantics move between versions. A generated script has no such
    # dependency, and since it is never hand-edited there is nothing to keep in sync.
    #
    # A plain top-level `var`, not `global.FACTORIO_TECHS`: KubeJS's `global` binding is an
    # unmodifiable Java map, so assigning to it fails and reading back gives undefined. Every
    # script of a type is evaluated against one shared topLevelScope, so a top-level var is
    # what crosses files -- and a `// priority:` header, not the filename, decides which file
    # gets there first. See ADR-0022.
    args.kubejs_out.parent.mkdir(parents=True, exist_ok=True)
    args.kubejs_out.write_text(
        "// priority: 20\n"
        "// GENERATED by scripts/factorio-tech-extract.py -- do not edit.\n"
        "// Regenerate after a Factorio update; hand-authored researches live in researchd.js.\n"
        "// The priority header is load order: KubeJS sorts scripts by it, descending, and does\n"
        "// NOT load them alphabetically. This has to load before factorio_tech_dsl.js reads it.\n"
        "var FACTORIO_TECHS = "
        + json.dumps(techs, indent=2)
        + ";\n",
        encoding="utf-8",
    )

    by_reason = {}
    for reason in pruned.values():
        by_reason[reason] = by_reason.get(reason, 0) + 1

    print(f"dump           {len(raw)} technologies")
    print(f"allowlist      {len(owner)} from {'+'.join(SOURCE_MODS)}")
    print(f"filtered out   {len(contaminated)} not from {'+'.join(SOURCE_MODS)}")
    print(f"pruned         {len(pruned)} " + str(by_reason))
    print(f"kept           {len(techs)}")
    print(f"science packs  {len(packs)}")
    print(f"wrote          {tech_path.relative_to(REPO)}")
    print(f"wrote          {(args.out_dir / 'science_packs.json').relative_to(REPO)}")
    print(f"wrote          {args.kubejs_out.relative_to(REPO)}")
    if not packs:
        print("\nWARNING: no science packs found -- the prototype key moved again")
    if vanished:
        print(
            "\ndeclared in technology.lua but absent from the dump (removed at a later "
            "data stage):\n  " + ", ".join(vanished)
        )
    if suspicious:
        print(
            "\nkept, but named like a levelled chain -- check none is an upgrade family "
            "this missed:\n  " + ", ".join(suspicious)
        )


if __name__ == "__main__":
    main()
