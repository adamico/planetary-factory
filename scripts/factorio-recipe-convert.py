#!/usr/bin/env python3
"""Convert the extracted Factorio recipes into pack recipe JSON (ADR-0026, #87).

Reads five committed inputs and writes GregTech recipe JSON. Nothing here decides anything:
every judgement lives in one of the data files, so a decision is reviewed as a diff to a
design document rather than as a diff to a script.

  data/factorio/recipe.json        the corpus -- 163 Nauvis pre-launch recipes (#72)
  data/pack/category-map.json      which pack machine crafts a Factorio CATEGORY
  data/pack/subgroup-owner.json    which process crafts a recipe, per shelf and per recipe (#88)
  data/pack/item-map.json          Factorio name -> pack item, tag or fluid (ADR-0026)
  data/pack/recipe-overrides.json  every knowing departure from Factorio, with its reason (ADR-0031)

THE CONVERSION RULE (#126, rewriting ADR-0025's table). Nothing is scaled. Item counts transfer
1:1, one Factorio fluid unit is one millibucket, and `energy_required` seconds become ticks at
x 20. `crafting_speed` belongs to the machine and `EUt` is set at registration (ADR-0029), so
neither appears in an emitted recipe.

WHAT STOPS A RECIPE BEING EMITTED, in the order it is checked:

  1. its category is `!`-routed          -- deliberately not routed (category-map.json)
  2. its process is `not_emitted`        -- out of the corpus's emit scope (subgroup-owner.json)
  3. its process is `native_mechanic`    -- IN scope, supported by a mod mechanic with no recipe
  4. its process has no registered type  -- blocked on the ticket that registers the machine
  5. it touches an `undecided` item-map row -- blocked on the decision that row names
  6. an override says `skip`             -- a departure, with its reason

A Factorio name with NO item-map row at all is none of these: it is a HARD FAILURE (#72), because
a name nobody has looked at must never be quietly skipped.

Usage: scripts/factorio-recipe-convert.py [--check] [--quiet]
  --check  write nothing; exit non-zero if the emitted files on disk differ from what would be
           written. Generated output is never hand-edited (ADR-0026), and this is what says so.
"""
import argparse
import json
import shutil
import sys
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT_DIR = ROOT / "kubejs/data/planetaryfactory/recipe"

# `subgroup-owner.json` names a process as `<owner>:<machine>`; `category-map.json` names the same
# machines and holds their recipe types. The two vocabularies meet here and nowhere else -- the
# recipe type itself, and the ticket that registers one that does not exist yet, are read from the
# map rather than kept in a table of this script's own.
#
# `pack:smelting` is the pack's three furnace tiers (#91, #149) -- `planetaryfactory_core` blocks
# reading vanilla `minecraft:smelting`, which needs no registration. A recipe vanilla's furnace
# cannot express still skips below: the pack's own furnace type is the furnace ticket's, not this
# script's.
MACHINE_OF_PROCESS = {"pack:smelting": "smelting"}

# Vanilla's furnace consumes exactly one item -- `SmeltingRecipe` holds a bare `Ingredient` with
# no count, while its result carries one -- so a `smelting` recipe asking for more has no vanilla
# shape. `stone-brick` is overridden to the vanilla 1:1 shape (cobblestone to stone); `steel-plate`
# is 5 plates to 1 and waits on the pack furnace's own recipe type. Reported, never emitted as 1:1.
VANILLA_SMELTING = "minecraft:smelting"

# Factorio's `crafting` / `advanced-crafting` / `crafting-with-fluid` all collapse to one
# machine, and the Personal Assembler needs the distinction back: it is a filtered view of the
# Assembling Machine's recipes, not a machine with a recipe type (#125's decision 6, CONTEXT.md).
# So the source category rides on the emitted recipe, in GregTech's own `data` compound -- not
# as an item tag, and not by having the Personal Assembler read `data/factorio/recipe.json` at
# runtime, which would make a regenerable build input into a shipped runtime asset.
SOURCE_CATEGORY_KEY = "factorio_category"


def load(path):
    return json.loads((ROOT / path).read_text())


def machine_of(process):
    """The `category-map.json` machine a `subgroup-owner.json` process names."""
    if process in MACHINE_OF_PROCESS:
        return MACHINE_OF_PROCESS[process]
    return process.split(":", 1)[1]


def process_of(recipe, subgroups):
    """The process that crafts this recipe: its own row if it has one, else its shelf's."""
    shelf = subgroups[f"{recipe['group']}/{recipe['subgroup']}"]
    process = shelf.get("per_recipe", {}).get(recipe["name"])
    if process is None:
        process = shelf.get("process") or shelf.get("owner")
    # `owner / process` where the two disagree; the machine is the right-hand side.
    if " / " in process:
        process = process.split(" / ", 1)[1]
    return process


def content(entry, row):
    """One GregTech `Content`: a NeoForge SizedIngredient or SizedFluidIngredient.

    `chance`, `maxChance` and `tierChanceBoost` are all `optionalFieldOf` on Content's codec and
    every Factorio recipe is deterministic, so they are left off rather than written as defaults.
    """
    if row["kind"] == "fluid":
        return {"content": {"ingredient": {"fluid": row["target"]}, "amount": entry["amount"]}}
    if row.get("components"):
        # NeoForge's DataComponentIngredient: one item told apart by the components it carries.
        # The science packs are the case -- one `researchd:research_pack` item, four variants.
        ingredient = {"type": "neoforge:components", "items": row["target"],
                      "components": row["components"]}
    elif row["kind"] == "tag":
        ingredient = {"tag": row["target"]}
    else:
        ingredient = {"item": row["target"]}
    return {"content": {"ingredient": ingredient, "count": entry["amount"]}}


def capability_of(row):
    """GregTech keys its capability maps by the capability's own name."""
    return "fluid" if row["kind"] == "fluid" else "item"


def convert_smelting(recipe, items, override):
    """One vanilla furnace recipe. #91 puts the plates on three furnace tiers, all vanilla."""
    ingredient = items[recipe["ingredients"][0]["name"]]
    result = items[recipe["results"][0]["name"]]
    return {
        "type": VANILLA_SMELTING,
        "ingredient": {"tag": ingredient["target"]} if ingredient["kind"] == "tag"
        else {"item": ingredient["target"]},
        "result": {"id": result["target"], "count": recipe["results"][0]["amount"]},
        # Factorio has no smelting XP and the pack is not going to invent one.
        "experience": 0.0,
        "cookingtime": override.get("duration", round(recipe["energy_required"] * 20)),
    }


def convert(recipe, recipe_type, items, override):
    """One GTRecipe as JSON.

    The shape is read off `GTRecipeSerializer`'s codec in GTCEu 7.0.2: `type` and `duration` are
    the only required fields, every capability map is `optionalFieldOf`, and the capability keys
    are the recipe capabilities' own names (`item`, `fluid`). ADR-0026 exists because a wrong
    shape NPEs in the codec at datapack load rather than reporting anything readable, so this
    writes the minimum the codec asks for and nothing speculative.
    """
    out = {"type": recipe_type}
    for field, entries in (("inputs", recipe["ingredients"]), ("outputs", recipe["results"])):
        caps = {}
        for entry in entries:
            row = items[entry["name"]]
            caps.setdefault(capability_of(row), []).append(content(entry, row))
        if caps:
            out[field] = caps
    out["duration"] = override.get("duration", round(recipe["energy_required"] * 20))
    out["data"] = {SOURCE_CATEGORY_KEY: recipe["category"]}
    return out


def emitted_path(recipe_type, name):
    """Where a GregTech recipe's file has to sit, which is not a free choice (#87).

    GregTech re-registers every GTRecipe the datapack loaded: `RecipeManagerLateMixin` strips
    everything before the first `/` of the id's path and `GTRecipeBuilder.save` puts the recipe
    type's own path back on the front. The round trip closes only for a file already under a
    directory named after its recipe type -- a flat `recipe/copper_cable.json` is loaded as
    `planetaryfactory:copper_cable` and re-registered as `planetaryfactory:assembling/copper_cable`,
    leaving BOTH ids in the recipe manager with identical inputs and outputs. Vanilla types are
    not GTRecipes, are not cloned, and stay flat.
    """
    return "%s/%s" % (recipe_type.split(":", 1)[1], name.replace("-", "_"))


def apply_override(recipe, override):
    if not ({"ingredients", "results"} & set(override)):
        return recipe
    recipe = dict(recipe)
    for field in ("ingredients", "results"):
        if field in override:
            recipe[field] = [dict(e) for e in override[field]]
    return recipe


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true",
                        help="write nothing; fail if the emitted files differ")
    parser.add_argument("--quiet", action="store_true")
    args = parser.parse_args()

    recipes = load("data/factorio/recipe.json")
    routes = load("data/pack/category-map.json")["routes"]
    subgroups = load("data/pack/subgroup-owner.json")["subgroups"]
    items = load("data/pack/item-map.json")["items"]
    overrides = load("data/pack/recipe-overrides.json")["recipes"]
    machines = load("data/pack/category-map.json")["machines"]

    # The overrides file's own invariants -- a `reason` on every entry, and no entry naming a
    # recipe outside the corpus -- are asserted by `tests/factorio/test_recipe_convert.py`.
    failures = []

    emitted, skipped = {}, []
    for recipe in recipes:
        name = recipe["name"]
        override = overrides.get(name, {})
        route = routes.get(recipe["category"])
        if route is None:
            failures.append(f"{name}: category {recipe['category']} is routed by nothing")
            continue
        if route.startswith("!"):
            skipped.append((name, "unrouted category", route[1:]))
            continue

        process = process_of(recipe, subgroups)
        if process in ("not_emitted", "native_mechanic", "undecided"):
            skipped.append((name, process, f"{recipe['group']}/{recipe['subgroup']}"))
            continue
        machine = machine_of(process)
        if machine not in machines:
            failures.append(f"{name}: process {process} names no machine in category-map.json")
            continue
        recipe_type = machines[machine]["recipe_type"]
        if recipe_type is None:
            ticket = machines[machine].get("blocked_by")
            skipped.append((name, "machine not registered",
                            f"{machine}" + (f", #{ticket}" if ticket else ", no ticket")))
            continue

        recipe = apply_override(recipe, override)
        missing = [e["name"] for e in recipe["ingredients"] + recipe["results"]
                   if e["name"] not in items]
        if missing:
            # #72's decision, and the one failure this converter must never soften.
            failures.append(f"{name}: no item-map row for " + ", ".join(sorted(set(missing))))
            continue
        # A `native_mechanic` row is reported under its own name: the capability is fully
        # supported and the recipe is not a cut, so it must never read as a blocked decision (#93).
        for status, label in (("native_mechanic", "native mechanic"),
                              ("not_emitted", "not_emitted item-map row"),
                              ("undecided", "undecided item-map row")):
            blocked = sorted({e["name"] for e in recipe["ingredients"] + recipe["results"]
                              if items[e["name"]].get("status") == status})
            if blocked:
                skipped.append((name, label, ", ".join(blocked)))
                break
        if blocked:
            continue
        on_tag = [e["name"] for e in recipe["results"] if items[e["name"]]["kind"] == "tag"]
        if on_tag:
            failures.append(f"{name}: result {on_tag[0]} maps onto a tag, which cannot be a result")
            continue
        if override.get("skip"):
            skipped.append((name, "override", override["reason"]))
            continue

        if recipe_type == VANILLA_SMELTING:
            oversized = [e for e in recipe["ingredients"] if e["amount"] != 1]
            if oversized:
                skipped.append((name, "no vanilla shape",
                                f"a furnace consumes one item, not {oversized[0]['amount']}"))
                continue
            emitted[name.replace("-", "_")] = convert_smelting(recipe, items, override)
        else:
            emitted[emitted_path(recipe_type, name)] = convert(recipe, recipe_type, items,
                                                              override)

    if failures:
        for failure in failures:
            print("FAIL " + failure)
        return 1

    if args.check:
        written = {p.relative_to(OUT_DIR).with_suffix("").as_posix(): json.loads(p.read_text())
                   for p in OUT_DIR.rglob("*.json")} if OUT_DIR.exists() else {}
        if written != emitted:
            added = sorted(set(emitted) - set(written))
            removed = sorted(set(written) - set(emitted))
            changed = sorted(k for k in set(emitted) & set(written) if emitted[k] != written[k])
            print("FAIL emitted recipes on disk are stale -- re-run "
                  "scripts/factorio-recipe-convert.py")
            for label, names in (("missing", added), ("unexpected", removed), ("changed", changed)):
                if names:
                    print(f"  {label}: " + ", ".join(names))
            return 1
    else:
        if OUT_DIR.exists():
            shutil.rmtree(OUT_DIR)
        OUT_DIR.mkdir(parents=True)
        for stem, body in emitted.items():
            path = OUT_DIR / f"{stem}.json"
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(json.dumps(body, indent=2) + "\n")

    if not args.quiet:
        reasons = Counter(reason for _, reason, _ in skipped)
        print(f"ok   {len(emitted)} recipes emitted, {len(skipped)} not")
        for reason, count in reasons.most_common():
            print(f"     {count:3d} {reason}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
