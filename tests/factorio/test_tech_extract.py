#!/usr/bin/env python3
"""Check that the extracted Factorio tech tree is still a tree, without launching anything.

The extractor drops 106 of Factorio's 268 technologies -- the infinite ones, the ones
costed by a formula, and the levelled upgrade chains (ADR-0022). Dropping a node whose
children survive would orphan them, so prerequisites are re-pointed through every dropped
node to its nearest surviving ancestors. That walk is the one piece of real logic in the
extractor, and its failure mode is silent: a tree that loads fine and simply omits half
the pack, or one with a research nobody can ever reach.

So the claim under check is narrow and structural -- *the pruned tree is still a valid
tree* -- plus one thing the game reports far too late: a `fromFactorio()` call naming a
technology that does not exist is a research that never appears, with no error anywhere.

What this deliberately does not check is the pack's content: whether an icon is the right
item, whether an unlocked recipe id resolves, whether the costs are fun. That is authoring,
and it is the launch test's job.
"""

import importlib.util
import json
import re
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent.parent
DATA = REPO / "data" / "factorio"
FAILURES = []


def check(condition, message):
    if not condition:
        FAILURES.append(message)


def load_extractor():
    """Import the extractor by path -- its filename is hyphenated, so it is not importable."""
    path = REPO / "scripts" / "factorio-tech-extract.py"
    spec = importlib.util.spec_from_file_location("factorio_tech_extract", path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def check_pruning_rules(x):
    """The three families the extractor drops, and the one it must not."""
    check(x.is_pruned({"max_level": "infinite"}) == "infinite", "infinite tech not pruned")
    check(
        x.is_pruned({"unit": {"count_formula": "1000*(L-2)"}}) == "count_formula",
        "count_formula tech not pruned",
    )
    check(x.is_pruned({"upgrade": True}) == "upgrade", "upgrade tech not pruned")
    check(
        x.is_pruned({"unit": {"count": 50, "time": 5}}) is None,
        "an ordinary tech was pruned",
    )
    check(
        x.is_pruned({"name": "automation-2", "unit": {"count": 50}}) is None,
        "a tech was pruned for its name rather than its flags",
    )


def check_reparenting(x):
    """Dropped nodes are transparent: children re-point through them, not into a void."""
    raw = {
        "root": {"prerequisites": []},
        "gone": {"prerequisites": ["root"], "upgrade": True},
        "child": {"prerequisites": ["gone"]},
    }
    keep = {"root", "child"}
    check(
        x.surviving_parents("child", raw, keep) == ["root"],
        "a child of a dropped tech was not re-pointed to its grandparent",
    )

    # Two dropped nodes deep, and a diamond: the same ancestor reached twice is emitted once.
    raw = {
        "root": {"prerequisites": []},
        "mid-a": {"prerequisites": ["root"], "upgrade": True},
        "mid-b": {"prerequisites": ["root"], "upgrade": True},
        "child": {"prerequisites": ["mid-a", "mid-b"]},
    }
    check(
        x.surviving_parents("child", raw, {"root", "child"}) == ["root"],
        "a diamond through dropped nodes produced a duplicate parent",
    )

    # Malformed data must not hang the extractor.
    raw = {"a": {"prerequisites": ["b"]}, "b": {"prerequisites": ["a"]}}
    x.surviving_parents("a", raw, set())


def check_effect_collapsing(x):
    """Generated reverse-crafts collapse to their rule; hand-authored ones do not."""
    derivable = {"iron-plate"}
    effects = [
        {"type": "unlock-recipe", "recipe": "recycler"},
        {"type": "unlock-recipe", "recipe": "iron-plate-recycling"},
        {"type": "unlock-recipe", "recipe": "raw-fish-recycling", "hidden": True},
        {"type": "unlock-recipe", "recipe": "bespoke-recycling"},
    ]
    out = x.collapse_effects(effects, derivable)
    families = [e for e in out if e["type"] == "unlock-recipe-family"]
    check(len(families) == 1 and families[0]["recipes"] == 2, "reverse-crafts did not collapse")
    kept = {e.get("recipe") for e in out if e["type"] == "unlock-recipe"}
    check(kept == {"recycler", "bespoke-recycling"}, f"wrong effects survived collapsing: {kept}")


def check_tree(techs):
    """The committed tree: connected, acyclic, no dangling or self-referential parents."""
    by_name = {t["name"]: t for t in techs}
    check(len(by_name) == len(techs), "duplicate technology names in technology.json")

    for tech in techs:
        for field in ("name", "suggested_id", "localised_name", "source", "cost_kind"):
            check(tech.get(field), f"{tech['name']}: missing {field}")
        check(
            tech["suggested_id"] == "planetary_factory:" + tech["name"].replace("-", "_"),
            f"{tech['name']}: suggested_id does not follow from the name",
        )
        check(
            tech["cost_kind"] in ("packs", "trigger"),
            f"{tech['name']}: unknown cost_kind {tech['cost_kind']}",
        )
        if tech["cost_kind"] == "packs":
            check(tech.get("unit"), f"{tech['name']}: pack-costed but has no unit")
        else:
            check(
                tech.get("research_trigger"),
                f"{tech['name']}: trigger-costed but has no research_trigger",
            )
        check(
            tech["name"] not in tech["prerequisites"],
            f"{tech['name']}: is its own prerequisite",
        )
        for parent in tech["prerequisites"]:
            check(
                parent in by_name,
                f"{tech['name']}: prerequisite '{parent}' was dropped without re-pointing",
            )

    # Every technology must be reachable from a root, or nothing can ever research it.
    roots = [t["name"] for t in techs if not t["prerequisites"]]
    check(roots, "the tree has no root -- every technology has a prerequisite")
    children = {}
    for tech in techs:
        for parent in tech["prerequisites"]:
            children.setdefault(parent, []).append(tech["name"])
    seen, stack = set(roots), list(roots)
    while stack:
        for child in children.get(stack.pop(), []):
            if child not in seen:
                seen.add(child)
                stack.append(child)
    unreachable = sorted(set(by_name) - seen)
    check(not unreachable, f"unreachable from any root (a cycle): {unreachable[:5]}")


def check_no_pruned_families(techs):
    """Nothing the extractor claims to drop survived into the committed file."""
    for tech in techs:
        unit = tech.get("unit") or {}
        check("count_formula" not in unit, f"{tech['name']}: count_formula survived")
        check(
            not re.search(r"^(mining-productivity|worker-robot-speed|braking-force)-\d+$", tech["name"]),
            f"{tech['name']}: an upgrade chain survived",
        )


def check_generated_js(techs):
    """The generated KubeJS module and the committed JSON are the same data."""
    path = REPO / "kubejs" / "server_scripts" / "factorio_tech_data.js"
    check(path.is_file(), "factorio_tech_data.js was not generated")
    if not path.is_file():
        return
    text = path.read_text(encoding="utf-8")
    check("var FACTORIO_TECHS" in text, "generated js does not define FACTORIO_TECHS")
    body = text[text.index("=") + 1 : text.rindex(";")]
    check(
        json.loads(body) == techs,
        "factorio_tech_data.js is stale -- rerun scripts/factorio-tech-extract.py",
    )


def check_builder_singletons(techs):
    """The DSL must use the plural builder calls, and the data must justify them.

    Researchd's ResearchBuilder holds ONE researchMethod and ONE researchEffect:
    consumePack() delegates to method(), and both overwrite rather than accumulate. Calling
    either in a loop keeps only the last value, which for the 117 multi-pack technologies
    means a research costing a single pack instead of up to eleven. Nothing errors, nothing
    logs -- the tree just becomes trivially cheap.

    consumePacks(ids, count, time) is only equivalent to a per-pack cost because every
    ingredient amount in Factorio's tree is 1. If a future extraction breaks that, the
    plural call silently rounds it away, so the data is asserted alongside the code.
    """
    amounts = {
        amount
        for tech in techs
        if tech["cost_kind"] == "packs"
        for _, amount in (tech.get("unit") or {}).get("ingredients", [])
    }
    check(
        amounts <= {1},
        f"a pack ingredient amount is not 1 ({sorted(amounts)}) -- consumePacks() can no "
        "longer express the cost; use and(...) composition",
    )

    path = REPO / "kubejs" / "server_scripts" / "factorio_tech_dsl.js"
    if not path.is_file():
        check(False, "factorio_tech_dsl.js is missing")
        return
    source = path.read_text(encoding="utf-8")
    body = "\n".join(
        line
        for line in source.splitlines()
        if not line.strip().startswith(("//", "*", "/*"))
    )
    check("consumePacks(" in body, "the DSL does not call consumePacks()")
    check(
        "consumePack(" not in body,
        "the DSL calls consumePack() -- it overwrites, so multi-pack costs are lost",
    )
    check("unlockRecipes(" in body, "the DSL does not call unlockRecipes()")
    check(
        "unlockRecipe(" not in body,
        "the DSL calls unlockRecipe() -- it overwrites, so extra unlocks are lost",
    )
    check(
        "unlockDimension(" not in body,
        "the DSL calls unlockDimension() -- it overwrites, so extra dimensions are lost",
    )

    # consumeItem destroys the items and, like checkItemPresence, reads the player's
    # inventory rather than the Research Lab -- so it buys no automation and costs the
    # player the thing they just proved they could make. Factorio's own research triggers
    # do not consume. The pack uses it nowhere.
    # KubeJS's `global` binding is an unmodifiable Java map: assigning to it fails and
    # reading it back gives undefined, so a DSL built on it registers nothing at all and
    # says nothing about it. Cross-file sharing goes through the shared topLevelScope --
    # a plain top-level var or function -- instead.
    for name in ("factorio_tech_dsl.js", "researchd.js", "factorio_tech_data.js"):
        script = REPO / "kubejs" / "server_scripts" / name
        if not script.is_file():
            continue
        lines = [
            line
            for line in script.read_text(encoding="utf-8").splitlines()
            if not line.strip().startswith(("//", "*", "/*")) and "global." in line
        ]
        check(not lines, f"{name} uses `global.`, which KubeJS cannot assign to")

    for name in ("factorio_tech_dsl.js", "researchd.js"):
        script = REPO / "kubejs" / "server_scripts" / name
        if not script.is_file():
            continue
        lines = [
            line
            for line in script.read_text(encoding="utf-8").splitlines()
            if not line.strip().startswith(("//", "*", "/*"))
            and "consumeItem" in line
        ]
        check(not lines, f"{name} uses consumeItem, which the pack does not use")


def check_declarations(techs):
    """Every fromFactorio() call names a technology that actually exists.

    A typo here is silent in-game: the override sits in a map nothing ever looks up, and
    the research simply never appears.
    """
    path = REPO / "kubejs" / "server_scripts" / "researchd.js"
    if not path.is_file():
        return
    names = {t["name"] for t in techs}
    declared = re.findall(r"fromFactorio\(\s*'([^']+)'", path.read_text(encoding="utf-8"))
    for name in declared:
        check(name in names, f"researchd.js declares '{name}', which is not a known technology")
    check(len(declared) == len(set(declared)), "researchd.js declares a technology twice")


def main():
    tech_path = DATA / "technology.json"
    if not tech_path.is_file():
        sys.exit(f"no {tech_path.relative_to(REPO)} -- run scripts/factorio-tech-extract.py")

    techs = json.loads(tech_path.read_text(encoding="utf-8"))
    packs = json.loads((DATA / "science_packs.json").read_text(encoding="utf-8"))
    extractor = load_extractor()

    check_pruning_rules(extractor)
    check_reparenting(extractor)
    check_effect_collapsing(extractor)
    check_tree(techs)
    check_no_pruned_families(techs)
    check_generated_js(techs)
    check_builder_singletons(techs)
    check_declarations(techs)

    check(packs, "no science packs extracted -- the prototype key moved again")
    check(
        {p["name"] for p in packs} >= {"automation-science-pack", "promethium-science-pack"},
        "the science pack list is missing base or Space Age packs",
    )

    if FAILURES:
        for failure in FAILURES:
            print(f"FAIL  {failure}")
        print(f"\n{len(FAILURES)} failures")
        sys.exit(1)
    print(f"ok  {len(techs)} technologies, {len(packs)} science packs")


if __name__ == "__main__":
    main()
