"""Regression checks for the Item Sink economy and generated catalogue.

Run with: py -m unittest discover -s tools -p test_item_values.py -v
"""
import collections
import csv
import hashlib
import json
import re
import unittest
from pathlib import Path

import build_item_values as values


ROOT = Path(__file__).resolve().parents[1]
TAG_ROOT = ROOT / "src/main/resources/data/item_sink/tags/item"
HISTORICAL_DEFAULTS_HASH = "1bf611eb4baf78cccd3c52bf8467b0bb41c7fee98ac77e1168c0102585b24e31"
ORESTONE_NAMES = "asurine|crimsite|ochrum|veridium|scoria|scorchia"


def load_json(path):
    return json.loads(path.read_text(encoding="utf-8"))


class EconomyChecks(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.registry = load_json(ROOT / "docs/value-registry.json")
        cls.rows = {row["id"]: row for row in cls.registry["items"]}
        with (ROOT / "docs/item-values.csv").open(encoding="utf-8", newline="") as handle:
            cls.audit_rows = list(csv.DictReader(handle))
        cls.audit = {row["id"]: row for row in cls.audit_rows}
        cls.prices = {
            row["id"]: int(row["value"])
            for row in cls.audit_rows if row["value"]
        }
        cls.legacy, _ = values.legacy_values()

    def test_historical_defaults_still_recognize_existing_configs(self):
        projects = [ROOT]
        if (ROOT / "forge").is_dir():
            projects.append(ROOT / "forge")
        for project in projects:
            text = (project / "src/main/java/com/ourcreation/itemsink/ValueDefaults.java").read_text()
            with self.subTest(project=str(project)):
                self.assertEqual(hashlib.sha256(text.encode()).hexdigest(), HISTORICAL_DEFAULTS_HASH)

    def test_every_orestone_and_variant_keeps_its_original_price(self):
        anchors = load_json(ROOT / "tools/orestone_values.json")
        self.assertEqual(len(anchors), 163)
        self.assertEqual(
            hashlib.sha256(json.dumps(anchors, sort_keys=True, separators=(",", ":")).encode()).hexdigest(),
            "9aa463008e1bc4c0d90ee599612d2f192360fe85afd14c5d4c3e359833e33b6f",
        )
        actual_ids = {
            item for item in self.rows
            if re.search(r"(?:^|_)(?:" + ORESTONE_NAMES + r")(?:_|$)", item.split(":", 1)[1])
        }
        self.assertEqual(actual_ids, set(anchors))
        for item, original_price in anchors.items():
            with self.subTest(item=item):
                self.assertEqual(self.prices[item], original_price)
                self.assertEqual(values.classify(self.rows[item], self.legacy)[0], original_price)

    def test_diamond_is_more_valuable_than_boats_and_cannons(self):
        self.assertEqual(self.prices["minecraft:diamond"], 32)
        self.assertEqual(self.prices["minecraft:emerald"], 16)
        self.assertEqual(self.prices["minecraft:netherite_ingot"], 64)
        self.assertEqual(self.prices["minecraft:oak_boat"], 4)
        self.assertEqual(self.prices["minecraft:oak_chest_boat"], 6)
        cannon_boats = [item for item, row in self.rows.items() if row["class"] == "CannonBoatItem"]
        self.assertGreaterEqual(len(cannon_boats), 9)
        self.assertIn("supplementaries:cannon_boat_oak", cannon_boats)
        for item in cannon_boats:
            with self.subTest(item=item):
                self.assertEqual(self.prices[item], 8)
        for item in ("create:potato_cannon", "supplementaries:cannon", "createbigcannons:cannon_carriage"):
            with self.subTest(item=item):
                self.assertLessEqual(self.prices[item], 16)
                self.assertLess(self.prices[item], self.prices["minecraft:diamond"])

    def test_cooking_and_prepared_meals_reward_production(self):
        for raw, cooked in (
            ("minecraft:beef", "minecraft:cooked_beef"),
            ("minecraft:potato", "minecraft:baked_potato"),
            ("minecraft:cod", "minecraft:cooked_cod"),
            ("farmersdelight:bacon", "farmersdelight:cooked_bacon"),
            ("naturalist:bass", "naturalist:cooked_bass"),
        ):
            with self.subTest(raw=raw, cooked=cooked):
                self.assertGreater(self.prices[cooked], self.prices[raw])
                self.assertGreaterEqual(self.prices[cooked], 8)
                self.assertLessEqual(self.prices[raw], 4)
        self.assertGreaterEqual(self.prices["minecraft:bread"], 8)
        self.assertGreaterEqual(self.prices["minecraft:golden_carrot"], 16)
        self.assertGreaterEqual(self.prices["create:chocolate_glazed_berries"], 16)
        for item in ("farmersdelight:beef_stew", "farmersdelight:hamburger", "minecraft:rabbit_stew"):
            with self.subTest(item=item):
                self.assertGreaterEqual(self.prices[item], 32)
        for item in ("farmersdelight:roast_chicken_block", "farmersdelight:stuffed_pumpkin_block"):
            with self.subTest(item=item):
                self.assertGreaterEqual(self.prices[item], 48)

    def test_harmful_food_and_technical_food_names_do_not_get_meal_prices(self):
        for item in ("minecraft:rotten_flesh", "minecraft:poisonous_potato", "createnuclear:yellowcake"):
            with self.subTest(item=item):
                self.assertEqual(self.prices[item], 1)
        for item in ("create:blaze_cake", "create:blaze_cake_base"):
            with self.subTest(item=item):
                self.assertLess(self.prices[item], self.prices["farmersdelight:beef_stew"])
        # A crop name on a collectible is not a food production recipe.
        self.assertLess(self.prices["collectorsalbum:common_golden_carrot_card"], self.prices["minecraft:golden_carrot"])

    def test_materials_and_storage_have_a_consistent_order(self):
        for item, price in {
            "minecraft:copper_ingot": 2,
            "minecraft:iron_ingot": 4,
            "minecraft:gold_ingot": 6,
            "create:zinc_ingot": 4,
            "create:brass_ingot": 6,
        }.items():
            with self.subTest(item=item):
                self.assertEqual(self.prices[item], price)
        for material, unit in (
            ("copper", "copper_ingot"), ("iron", "iron_ingot"),
            ("gold", "gold_ingot"), ("diamond", "diamond"),
            ("emerald", "emerald"), ("netherite", "netherite_ingot"),
            ("coal", "coal"),
        ):
            with self.subTest(material=material):
                unit_price = self.prices["minecraft:" + unit]
                block_price = self.prices["minecraft:" + material + "_block"]
                self.assertGreaterEqual(block_price, unit_price)
                self.assertLessEqual(block_price, 9 * unit_price)

    def test_small_material_forms_and_reactor_parts_have_sensible_salvage(self):
        # Create Deco presses one nugget into one coin; four coins form a stack.
        self.assertEqual(self.prices["createdeco:netherite_coin"], 7)
        self.assertEqual(self.prices["createdeco:netherite_coinstack"], 16)
        self.assertLessEqual(self.prices["createdeco:netherite_coin"], self.prices["createdeco:netherite_nugget"])
        self.assertLessEqual(self.prices["createdeco:netherite_coinstack"], 4 * self.prices["createdeco:netherite_coin"])
        # Crushing and milling scrap must not turn it into full ingot salvage.
        for item in (
            "createmetalwork:crushed_netherite_scrap", "createmetalwork:dirty_netherite_scrap_dust",
            "createmetalwork:netherite_scrap_dust",
        ):
            with self.subTest(item=item):
                self.assertGreaterEqual(self.prices[item], 8)
                self.assertLessEqual(self.prices[item], self.prices["minecraft:netherite_scrap"])
                self.assertLess(self.prices[item], self.prices["minecraft:netherite_ingot"])
        self.assertEqual(self.prices["createmetalwork:netherite_scrap_dust"], 16)
        # A reactor casing only needs a brass casing and a steel ingot.
        self.assertLess(self.prices["createnuclear:reactor_casing"], self.prices["minecraft:diamond"])
        for item in ("createnuclear:reactor_core", "createnuclear:reactor_controller"):
            with self.subTest(item=item):
                self.assertEqual(self.prices[item], 64)

    def test_terminals_and_computers_follow_recipe_progression(self):
        for item, expected in {
            "create_new_world_core:hub_terminal": 16,
            "northstar:computer_rack": 8,
            "northstar:targeting_computer": 48,
            "create_new_world_core:quantum_targeting_computer": 64,
            "toms_storage:storage_terminal": 8,
            "toms_storage:wireless_terminal": 16,
            "toms_storage:crafting_terminal": 32,
            "toms_storage:adv_wireless_terminal": 64,
            "numismatics:bank_terminal": 16,
            "numismaticsutils:portable_bank_terminal": 32,
        }.items():
            with self.subTest(item=item):
                self.assertEqual(self.prices[item], expected)
        self.assertGreaterEqual(self.prices["create_new_world_core:quantum_targeting_computer"],
                                4 * self.prices["create_new_world_core:hub_terminal"])

    def test_late_circuits_and_assemblies_exceed_their_earlier_stages(self):
        for stages in (
            ("create_new_world_core:improved_circuit", "create_new_world_core:advanced_circuit",
             "create_new_world_core:refined_radiance_circuit", "create_new_world_core:quantum_targeting_computer"),
            ("create_new_world_core:inductive_mechanism", "create_new_world_core:reinforced_mechanism",
             "create_new_world_core:netherite_mechanism"),
            ("powergrid:empty_circuit", "powergrid:unetched_circuit", "powergrid:integrated_circuit"),
        ):
            # Rules also cover newer Core ids absent from historical scans.
            prices = [values.classify(self.rows.get(item, {"id": item}), self.legacy)[0] for item in stages]
            with self.subTest(stages=stages):
                self.assertTrue(all(left < right for left, right in zip(prices, prices[1:])))
        for item in self.rows:
            if item.startswith("create_new_world_core:incomplete_"):
                with self.subTest(item=item):
                    self.assertEqual(self.prices[item], 1)
        for item, (price, reason) in values.PROGRESSION.items():
            with self.subTest(item=item):
                self.assertEqual(values.classify({"id": item}, self.legacy), (price, reason))
                if item.startswith("create_new_world_core:"):
                    old_id = item.replace("create_new_world_core:", "kubejs:")
                    self.assertEqual(values.classify({"id": old_id}, self.legacy), (price, reason))

    def test_material_names_on_signs_and_processed_forms_do_not_break_prices(self):
        # This "diamond" is the shape of a printed hazard symbol on iron sheet.
        self.assertLessEqual(self.prices["createdeco:decal_fire_diamond"], self.prices["create:iron_sheet"])
        self.assertGreaterEqual(self.prices["create:polished_rose_quartz"], self.prices["create:rose_quartz"])
        for material in ("shadow_steel", "refined_radiance"):
            with self.subTest(material=material):
                self.assertEqual(self.prices["createqol:" + material + "_block"], 64)
                self.assertGreaterEqual(self.prices["createqol:" + material + "_block"], self.prices["create:" + material])

    def test_currency_and_creative_items_are_excluded_even_if_historically_listed(self):
        for item in sorted(values.CURRENCY | values.TECHNICAL):
            if item not in self.rows:
                continue
            with self.subTest(item=item):
                self.assertNotIn(item, self.prices)
                self.assertIsNone(values.classify(self.rows[item], self.legacy)[0])
        for item in self.rows:
            if "creative" in item.split(":", 1)[1]:
                with self.subTest(item=item):
                    self.assertNotIn(item, self.prices)
        self.assertNotIn("numismatics:bevel", self.prices)
        for item in ("numismatics:bevel", "create:creative_motor", "minecraft:barrier"):
            with self.subTest(historical_override=item):
                historical = dict(self.legacy, **{item: 64})
                self.assertIsNone(values.classify(self.rows[item], historical)[0])

    def test_audit_covers_every_registered_item_once(self):
        self.assertEqual(len(self.rows), len(self.registry["items"]))
        self.assertEqual(len(self.audit), len(self.audit_rows))
        self.assertEqual(set(self.rows), set(self.audit))
        self.assertTrue(all(price in values.TIERS for price in self.prices.values()))
        for item, row in self.rows.items():
            with self.subTest(item=item):
                price, _ = values.classify(row, self.legacy)
                self.assertEqual(self.prices.get(item), price)

    def test_generated_tags_are_optional_complete_and_unambiguous(self):
        seen = collections.defaultdict(list)
        group_prices = {}
        for group in ("original", "createcore"):
            group_prices[group] = {}
            for tier in values.TIERS:
                tag = load_json(TAG_ROOT / group / f"value_{tier}.json")
                self.assertFalse(tag["replace"])
                for entry in tag["values"]:
                    item = entry["id"]
                    self.assertIs(entry["required"], False)
                    seen[item].append((group, tier))
                    group_prices[group][item] = tier
                parent = load_json(TAG_ROOT / f"value_{tier}.json")
                self.assertIn(f"#item_sink:{group}/value_{tier}", parent["values"])
        self.assertEqual({item: tiers for item, tiers in seen.items() if len(tiers) != 1}, {})
        generated = {item: entries[0][1] for item, entries in seen.items() if item in self.rows}
        self.assertEqual(generated, self.prices)
        catalogue = load_json(ROOT / "src/main/resources/data/item_sink/item_values/createcore.json")
        additions = {}
        for tier, items in catalogue.items():
            for item in items:
                self.assertNotIn(item, additions)
                additions[item] = int(tier)
        self.assertEqual(additions, group_prices["createcore"])
        self.assertTrue(set(additions) <= set(self.rows))


if __name__ == "__main__":
    unittest.main()
