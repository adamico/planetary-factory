#!/usr/bin/env python3
"""Assert the pack's smelting recipe type stays the thing #155 registered it to be.

`docs/testing/what-to-check.md`'s "cross-file references resolve" claim, for the recipe type that
makes the furnace ladder expressible.

This exists for one failure in particular, and it is the ticket's headline trap. GregTech's
`GTRecipeType.proxyRecipes` converts vanilla `SmeltingRecipe` **specifically**, reading only the
single vanilla ingredient. A pack recipe class that was assignable to it would have its count
silently dropped -- `5 iron_plate -> 1 steel_plate` becoming `1 iron_plate -> 1 steel_plate`, with
the wrong output, no error and no log line. Nothing at build time reports it, and in a running game
it looks like a working recipe.

The natural place for that assertion is a Java unit test. It cannot live there: the mod's test
source set has no Minecraft on its classpath by design (`mod/build.gradle`), so `SmeltingRecipe`
the vanilla class is not nameable from it. So the assertion is made against the source text, the
way `tests/factorio/test_recipe_convert.py` reads `PFBlocks.java` for the same reason.

Two of #155's acceptance criteria are deliberately NOT here, and that is a recorded decision rather
than an omission: the count-bearing `matches` and the serializer's JSON/network round trip both
need `Ingredient` and `ItemStack`, which means Minecraft on the classpath. They land as GameTests
with the harness in #156. What can be checked without a world is checked here.
"""

import json
import pathlib
import re
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[2]
MOD = ROOT / "mod/src/main/java/com/planetaryfactory/core"
RECIPE = MOD / "recipes/SmeltingRecipe.java"
REGISTRY = MOD / "recipes/PFRecipes.java"
CATEGORY_MAP = ROOT / "data/pack/category-map.json"

PACK_SMELTING = "planetaryfactory:smelting"

# Reading a recipe off vanilla's type, as opposed to using it to ask an item its burn time --
# `ItemStack.getBurnTime(RecipeType.SMELTING)` is fuel bookkeeping and is fine.
VANILLA_LOOKUPS = (
    "getRecipeFor(RecipeType.SMELTING",
    "createCheck(RecipeType.SMELTING",
    "getAllRecipesFor(RecipeType.SMELTING",
)


class SmeltingType(unittest.TestCase):
    def test_the_pack_recipe_is_not_a_vanilla_smelting_recipe(self):
        source = RECIPE.read_text(encoding="utf-8")
        declaration = re.search(r"public\s+record\s+SmeltingRecipe\b[^{]*\{", source, re.DOTALL)
        self.assertIsNotNone(declaration, "the pack recipe's declaration has moved")
        header = declaration.group(0)
        self.assertNotIn("extends", header,
                         "the pack recipe must implement Recipe directly -- anything assignable "
                         "to vanilla's SmeltingRecipe has its count dropped by GT's proxyRecipes")
        self.assertIn("implements Recipe<SingleRecipeInput>", header)
        self.assertNotIn("AbstractCookingRecipe", source)
        self.assertNotIn("import net.minecraft.world.item.crafting.SmeltingRecipe", source)

    def test_the_pack_recipe_carries_a_count(self):
        source = RECIPE.read_text(encoding="utf-8")
        self.assertIn("int count", source, "the count is the whole reason this type exists")
        self.assertRegex(source, r'[Ff]ieldOf\("count"',
                         "the count has to survive the JSON codec")
        self.assertRegex(source, r"SmeltingRecipe::count",
                         "the count has to survive the network codec too -- a stream codec that "
                         "drops it desyncs the client's recipe book from the server's recipe")
        self.assertIn("input.item().getCount() >= count", source,
                      "matching has to need the whole count, or the smelt is not m:n")

    def test_nothing_in_the_mod_reads_vanillas_smelting_type_for_recipes(self):
        # ADR-0034's sweep leaves vanilla `minecraft:smelting` with no live recipe, so a read of it
        # would be a read of nothing -- and a recipe re-admitted onto it later would carry vanilla's
        # cook time and get no tier scaling, which is the reason #155 dropped the dual read.
        for path in sorted(MOD.rglob("*.java")):
            source = path.read_text(encoding="utf-8")
            for lookup in VANILLA_LOOKUPS:
                with self.subTest(file=path.name, lookup=lookup):
                    self.assertNotIn(lookup, source)

    def test_the_registered_id_is_the_one_the_data_files_name(self):
        # The Java side and the data side name this type independently, and nothing else compares
        # them: a rename on one side ships a furnace that finds no recipes at all.
        registry = REGISTRY.read_text(encoding="utf-8")
        path = re.search(r'SMELTING\s*=\s*"([a-z0-9_]+)"', registry)
        self.assertIsNotNone(path, "PFRecipes no longer names the type's registry path")
        self.assertEqual(PACK_SMELTING, f"planetaryfactory:{path.group(1)}")

        machines = json.loads(CATEGORY_MAP.read_text(encoding="utf-8"))["machines"]
        self.assertEqual(PACK_SMELTING, machines["smelting"]["recipe_type"])


if __name__ == "__main__":
    unittest.main(verbosity=2)
