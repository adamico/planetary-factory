#!/usr/bin/env python3
"""Assert the conversion's data files and its emitted recipes still say what the decisions say.

This is #87's static half -- the "cross-file references resolve" check in
`docs/testing/what-to-check.md`'s terms. It launches no game, so what it can prove is that the
files agree with each other:

  - `item-map.json` covers every item and fluid the corpus names, and nothing else. A missing row
    is the converter's one hard failure (#72); an extra row is a name that left the corpus at a
    regeneration and took its decision with it
  - every row is one of the three legible kinds: a target, an `undecided` with a reason, or a
    `native_mechanic` (#93). Never a bare name
  - every target names a namespace the pack actually ships, and every first-party target is
    registered in `kubejs/startup_scripts/`. A row pointing at an item nobody registers is the
    failure that reaches the player as a recipe missing from JEI
  - every override carries a `reason` and names a recipe in the corpus (ADR-0031) -- the converter
    does not repeat this check, so this is the only place it is made
  - the emitted JSON is exactly what the converter emits today. Generated output is never
    hand-edited (ADR-0026), and this is the assertion that says so
  - every emitted recipe's ingredients and results resolve through the item map, and its `type`
    is a recipe type this pack registers

WHAT IT CANNOT PROVE is that the recipe SHAPE is right: GregTech's codec is Java, the ids of
GregTech's generated material items exist only in a loaded registry, and a wrong shape NPEs at
datapack load rather than reporting anything readable. That is ADR-0026's second check -- one
world load with the generated recipes in place -- and it needs a human.

Usage: tests/factorio/test_recipe_convert.py
"""
import json
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent.parent
STARTUP = ROOT / "kubejs/startup_scripts"

# The namespaces an item-map target may live in: this pack, the game, and the three mods whose
# capabilities ADR-0017 puts on Terra. Mekanism is deliberately absent -- ADR-0035 takes it out
# of the pack, so a row pointing at it would be a row written against a mod that is leaving.
NAMESPACES = {"minecraft", "planetaryfactory", "gtceu", "create", "electroenergetics",
              # GregTech's multiblock builder lands in `kubejs:`, not `gtceu:` -- the tiered
              # builder and the multiblock builder disagree about the namespace, which is why
              # the Oil Refinery's id differs from the Chemical Plant's (#107, machines.js).
              "kubejs",
              # `c:` is the common tag namespace, which belongs to no mod and is how a row
              # survives Almost Unified deciding which mod's item the player actually holds.
              "c",
              # Researchd owns the research-pack item; `planetary_factory:` (an underscore) is the
              # id space its packs are declared in, and is not this pack's item namespace.
              "researchd", "planetary_factory"}


def first_party_items():
    """The `planetaryfactory:` items and machines the startup scripts actually register."""
    items = set(re.findall(r"event\.create\('(planetaryfactory:[a-z0-9_]+)'",
                           (STARTUP / "items.js").read_text()))
    # A block registers an item too, and the chest ladder is a block (#133): its rows would
    # otherwise read as unregistered while sitting three lines away in `blocks.js`.
    items |= set(re.findall(r"event\.create\('(planetaryfactory:[a-z0-9_]+)'",
                            (STARTUP / "blocks.js").read_text()))
    machines = (STARTUP / "machines.js").read_text()
    for name in re.findall(r"event\.create\('([a-z0-9_]+)'\)", machines):
        # KJSTieredMachineBuilder registers through GregTech's registrate, so the ids come out
        # `gtceu:<tier>_<name>` -- the namespace and the tier prefix are both unreachable from
        # the script, which is why they are reconstructed here rather than read.
        for tier in re.findall(r"GTValues\.([A-Z]+)", machines.split(".tiers(", 1)[1].split(")", 1)[0]):
            items.add(f"gtceu:{tier.lower()}_{name}")
    return items


def check_item_map(items, corpus, failures):
    referenced = {e["name"] for r in corpus for e in r["ingredients"] + r["results"]}
    for name in sorted(referenced - set(items)):
        failures.append(f"item map has no row for {name} -- a hard failure, not a skip (#72)")
    for name in sorted(set(items) - referenced):
        failures.append(f"item map row {name} names nothing in the corpus")

    registered = first_party_items()
    for name, row in sorted(items.items()):
        status = row.get("status")
        if status in ("undecided", "native_mechanic", "not_emitted"):
            if not row.get("note"):
                failures.append(f"{name} is {status} with no note saying what decides it")
            # AN UNDECIDED ROW MUST NAME THE TICKET THAT DECIDES IT. Without this, a row can sit
            # `undecided` with a note pointing at prose -- an ADR, a closed ticket, another row --
            # and nothing ever comes back to it. #87 found 40 such rows, a quarter of the map:
            # eight of them cited an ADR that had already been accepted and had decided them.
            # `native_mechanic` and `not_emitted` are terminal and need no ticket; `undecided` is
            # a promise that someone will decide, and a promise needs an owner.
            if status == "undecided" and not isinstance(row.get("ticket"), int):
                failures.append(f"{name} is undecided and names no ticket -- say who decides it, "
                                "or the row is a decision nobody is coming back to")
            continue
        if status is not None:
            failures.append(f"{name} has unknown status {status!r}")
            continue
        for field in ("kind", "target", "source", "note"):
            if not row.get(field):
                failures.append(f"{name} has no {field}")
        if row.get("kind") not in ("item", "tag", "fluid"):
            failures.append(f"{name} has kind {row.get('kind')!r}")
        if row.get("source") not in ("borrowed", "authored"):
            failures.append(f"{name} has source {row.get('source')!r} -- ADR-0031's rule has two")
        target = row.get("target", "")
        namespace = target.split(":", 1)[0]
        if namespace not in NAMESPACES:
            failures.append(f"{name} maps onto {target}, whose namespace the pack does not ship")
        for component, value in (row.get("components") or {}).items():
            for id_ in (component, value):
                if id_.split(":", 1)[0] not in NAMESPACES:
                    failures.append(f"{name} names {id_}, whose namespace the pack does not ship")
        if "blocked_by" in row:
            if not isinstance(row["blocked_by"], int):
                failures.append(f"{name} has blocked_by {row['blocked_by']!r}, not a ticket number")
            if row.get("source") != "authored":
                failures.append(f"{name} is blocked_by a ticket but is not authored -- a borrowed "
                                "item exists already, so nothing can be waiting on it")
        if row.get("source") == "authored" and namespace != "planetaryfactory":
            failures.append(f"{name} is authored but maps onto {target}")
        if namespace == "planetaryfactory" and row.get("kind") == "item" \
                and target not in registered and "blocked_by" not in row:
            failures.append(f"{name} maps onto {target}, which no startup script registers")
        # `blocked_by` is the one escape, and it is narrow: a row whose item is DECIDED but is
        # `planetaryfactory_core`'s to register, naming the ticket that builds it. KubeJS cannot
        # register a furnace with a fuel slot or a chunk-charting block, so without this the map
        # could not record a decision the mod has not caught up with -- and the alternative,
        # leaving the row `undecided`, would say nobody had decided rather than nobody had built.
        if "blocked_by" in row and target in registered:
            failures.append(f"{name} is blocked_by #{row['blocked_by']} and is already "
                            "registered -- drop the field, the ticket landed")
        if target.startswith("gtceu:") and row.get("kind") == "item" \
                and target.endswith("_assembling_machine") and target not in registered:
            failures.append(f"{name} maps onto {target}, which machines.js does not register")


def check_overrides(overrides, corpus, failures):
    names = {r["name"] for r in corpus}
    for name, override in sorted(overrides.items()):
        if not override.get("reason"):
            failures.append(f"override {name} has no reason (ADR-0031)")
        if name not in names:
            failures.append(f"override {name} names no recipe in the corpus")


def check_emitted(items, recipe_types, failures):
    """Every emitted recipe resolves through the item map and onto a recipe type that exists.

    Two shapes reach this directory: GregTech's, and vanilla's furnace for the 1:1 smelts (#91).
    """
    targets = {row["target"] for row in items.values() if "target" in row}
    for path in sorted((ROOT / "kubejs/data/planetaryfactory/recipe").rglob("*.json")):
        recipe = json.loads(path.read_text())
        if recipe.get("type") == "minecraft:smelting":
            named = [recipe["ingredient"].get("item") or recipe["ingredient"].get("tag"),
                     recipe["result"]["id"]]
            for target in named:
                if target not in targets:
                    failures.append(f"{path.name} names {target}, which no item-map row gives")
            if not isinstance(recipe.get("cookingtime"), int) or recipe["cookingtime"] <= 0:
                failures.append(f"{path.name} has cookingtime {recipe.get('cookingtime')!r}")
            if "tag" in recipe["ingredient"] and recipe["result"]["id"].startswith("#"):
                failures.append(f"{path.name} has a tag as its result")
            continue
        if recipe.get("type") not in recipe_types:
            failures.append(f"{path.name} has type {recipe.get('type')!r}, which is not registered")
        if not isinstance(recipe.get("duration"), int) or recipe["duration"] <= 0:
            failures.append(f"{path.name} has duration {recipe.get('duration')!r}")
        for field in ("inputs", "outputs"):
            for capability, contents in recipe.get(field, {}).items():
                for entry in contents:
                    ingredient = entry["content"]["ingredient"]
                    target = ingredient.get("item") or ingredient.get("tag") \
                        or ingredient.get("fluid") or ingredient.get("items")
                    if target not in targets:
                        failures.append(
                            f"{path.name}: {field} names {target}, which no item-map row gives")
                    if capability == "fluid" and "fluid" not in ingredient:
                        failures.append(f"{path.name}: a fluid content holds {ingredient}")


def main():
    corpus = json.loads((ROOT / "data/factorio/recipe.json").read_text())
    items = json.loads((ROOT / "data/pack/item-map.json").read_text())["items"]
    overrides = json.loads((ROOT / "data/pack/recipe-overrides.json").read_text())["recipes"]

    failures = []
    check_item_map(items, corpus, failures)
    check_overrides(overrides, corpus, failures)
    machines = json.loads((ROOT / "data/pack/category-map.json").read_text())["machines"]
    # The types that exist today, read from the map rather than listed here: registering the
    # Chemical Plant should be one edit to one design document (#107), not three.
    recipe_types = {m["recipe_type"] for m in machines.values() if m["recipe_type"]}
    check_emitted(items, recipe_types, failures)

    stale = subprocess.run(
        [sys.executable, str(ROOT / "scripts/factorio-recipe-convert.py"), "--check", "--quiet"],
        capture_output=True, text=True)
    if stale.returncode != 0:
        failures.extend(line for line in stale.stdout.splitlines() if line.strip())

    for number, failure in enumerate(failures, 1):
        print(f"FAIL {number}: {failure}")
    if failures:
        return 1
    decided = sum(1 for row in items.values() if "target" in row)
    print(f"ok   {len(items)} item-map rows ({decided} decided), "
          f"{len(list((ROOT / 'kubejs/data/planetaryfactory/recipe').rglob('*.json')))} "
          "recipes emitted and current")
    return 0


if __name__ == "__main__":
    sys.exit(main())
