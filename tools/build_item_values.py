"""Remaster the item economy around frozen orestone prices and rewarding food production."""
import argparse
import collections
import csv
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
TIERS = (1, 2, 3, 4, 5, 6, 7, 8, 16, 32, 48, 64)
MATERIALS = {"copper": 2, "iron": 4, "gold": 6, "golden": 6, "zinc": 4,
             "andesite_alloy": 4, "brass": 6, "steel": 8, "electrum": 8,
             "silver": 5, "lead": 4, "tin": 3, "nickel": 4, "aluminum": 3,
             "aluminium": 3, "bronze": 6, "platinum": 16, "diamond": 32,
             "emerald": 16, "netherite": 64, "nethersteel": 16, "uranium": 8, "thorium": 8,
             "tungsten": 8, "titanium": 8, "amethyst": 4, "redstone": 3,
             "lapis": 3, "quartz": 2, "coal": 2, "charcoal": 2,
             "ruby": 16, "sapphire": 16, "spectral_ruby": 16, "accelerite": 48,
             "shadow_steel": 32, "refined_radiance": 32}
ORESTONE_VALUES = json.loads((ROOT / "tools/orestone_values.json").read_text(encoding="utf-8"))
CURRENCY = {"numismatics:" + coin for coin in ("spur", "bevel", "sprocket", "cog", "crown", "sun")}
TECHNICAL = {"minecraft:" + item for item in (
    "air", "barrier", "light", "structure_void", "structure_block", "jigsaw", "debug_stick",
    "command_block", "chain_command_block", "repeating_command_block", "command_block_minecart",
    "bedrock", "end_portal_frame", "knowledge_book", "spawner", "trial_spawner", "vault")}
MANUAL = {
    "item_sink:item_sink": 8,
    "minecraft:armadillo_scute": 5, "minecraft:turtle_scute": 5,
    "minecraft:netherite_scrap": 16, "minecraft:ancient_debris": 16,
    "minecraft:mace": 48, "minecraft:heavy_core": 48, "minecraft:breeze_rod": 8,
    "minecraft:wind_charge": 2, "minecraft:trial_key": 16, "minecraft:ominous_trial_key": 32,
    "minecraft:experience_bottle": 8, "minecraft:enchanted_book": 16,
    "minecraft:shield": 8, "minecraft:bow": 8, "minecraft:crossbow": 16,
    "minecraft:fishing_rod": 4, "minecraft:shears": 4,
    "minecraft:bucket": 4, "minecraft:water_bucket": 4, "minecraft:lava_bucket": 6,
    "minecraft:milk_bucket": 4, "minecraft:powder_snow_bucket": 4,
    "minecraft:clay_ball": 1, "minecraft:brick": 2, "minecraft:nether_brick": 2,
    "minecraft:paper": 1, "minecraft:book": 2, "minecraft:writable_book": 4,
    "minecraft:written_book": 4, "minecraft:map": 4, "minecraft:filled_map": 4,
    "minecraft:compass": 8, "minecraft:recovery_compass": 16, "minecraft:clock": 8,
    "minecraft:arrow": 1, "minecraft:spectral_arrow": 2, "minecraft:tipped_arrow": 2,
    "minecraft:potion": 4, "minecraft:splash_potion": 5, "minecraft:lingering_potion": 8,
    "minecraft:firework_rocket": 2, "minecraft:firework_star": 2,
    "minecraft:end_crystal": 16, "minecraft:conduit": 32, "minecraft:dragon_egg": 64,
    "minecraft:cake": 32, "minecraft:fermented_spider_eye": 3, "minecraft:phantom_membrane": 5,
    "minecraft:glistering_melon_slice": 5, "minecraft:disc_fragment_5": 2,
    "minecraft:netherite_upgrade_smithing_template": 32,
    "create:rose_quartz": 6, "create:iron_sheet": 4, "create:copper_sheet": 2,
    "create:golden_sheet": 6, "create:brass_sheet": 6, "create:brass_nugget": 1,
    "create:zinc_nugget": 1, "create:copper_nugget": 1, "create:wheat_flour": 3,
    "create:dough": 4, "create:blaze_cake_base": 8, "create:cinder_flour": 2,
    "create:sand_paper": 2, "create:red_sand_paper": 2,
    "create:shaft": 2, "create:cogwheel": 2, "create:large_cogwheel": 4,
    "create:andesite_casing": 4, "create:brass_casing": 6, "create:copper_casing": 4,
    "create:railway_casing": 8, "create:gearbox": 8, "create:vertical_gearbox": 8,
    "create:water_wheel": 8, "create:large_water_wheel": 16, "create:windmill_bearing": 16,
    "create:steam_engine": 16, "create:mechanical_press": 16, "create:mechanical_mixer": 16,
    "create:deployer": 16, "create:mechanical_crafter": 16, "create:crushing_wheel": 16,
    "create:mechanical_drill": 8, "create:mechanical_saw": 8, "create:mechanical_harvester": 8,
    "create:belt_connector": 4, "create:track": 8,
    "create:cardboard": 1, "create:pulp": 1, "create:cardboard_block": 2,
    "createaddition:electric_motor": 16, "createaddition:alternator": 16,
    "createaddition:rolling_mill": 16, "createaddition:tesla_coil": 16,
    "explorerscompass:explorerscompass": 16, "naturescompass:naturescompass": 8,
    "create_new_world_core:black_diamond": 48, "create_new_world_core:iron_ring": 4,
    "create_new_world_core:survival_guide": 1,
    "minecraft:diamond": 32, "minecraft:emerald": 16, "minecraft:netherite_ingot": 64,
    "minecraft:golden_apple": 32, "minecraft:enchanted_golden_apple": 64,
    "minecraft:golden_carrot": 16, "minecraft:ominous_bottle": 8,
    "minecraft:nether_star": 64, "minecraft:beacon": 64, "minecraft:elytra": 48,
    "minecraft:trident": 32, "minecraft:totem_of_undying": 32,
    "minecraft:heart_of_the_sea": 32, "minecraft:dragon_breath": 16,
    "minecraft:ender_pearl": 8, "minecraft:ender_eye": 16,
    "minecraft:nautilus_shell": 8, "minecraft:blaze_rod": 8,
    "minecraft:leather": 3, "minecraft:rabbit_hide": 2, "minecraft:rabbit_foot": 8,
    "minecraft:slime_ball": 3, "minecraft:ghast_tear": 8, "minecraft:blaze_powder": 4,
    "minecraft:magma_cream": 4, "minecraft:amethyst_shard": 4,
    "minecraft:flint": 1, "minecraft:feather": 1, "minecraft:egg": 2,
    "minecraft:wheat": 2, "minecraft:sugar": 2, "minecraft:sugar_cane": 1,
    "minecraft:honeycomb": 3, "minecraft:glowstone_dust": 2, "minecraft:gunpowder": 2,
    "minecraft:bone": 1, "minecraft:string": 1, "minecraft:glow_ink_sac": 2, "minecraft:ink_sac": 1,
    "minecraft:bread": 8, "minecraft:baked_potato": 8, "minecraft:pumpkin_pie": 16,
    "minecraft:honey_bottle": 8, "minecraft:cookie": 4, "minecraft:dried_kelp": 2,
    "minecraft:rotten_flesh": 1, "minecraft:poisonous_potato": 1,
    "minecraft:spider_eye": 1, "minecraft:pufferfish": 1,
    "farmersdelight:rotten_tomato": 1, "createnuclear:yellowcake": 1,
    "farmersdelight:rice": 3, "farmersdelight:milk_bottle": 4,
    "farmersdelight:apple_cider": 16, "farmersdelight:melon_juice": 16,
    "farmersdelight:hot_cocoa": 16, "farmersdelight:horse_feed": 16,
    "farmersdelight:dog_food": 16, "farmersdelight:tomato_sauce": 8,
    "farmersdelight:apple_pie": 32, "farmersdelight:chocolate_pie": 32,
    "farmersdelight:sweet_berry_cheesecake": 32,
    "farmersdelight:roast_chicken_block": 64, "farmersdelight:stuffed_pumpkin_block": 64,
    "farmersdelight:honey_glazed_ham_block": 64, "farmersdelight:shepherds_pie_block": 64,
    "farmersdelight:rice_roll_medley_block": 64, "farmersdelight:gleaming_salad_block": 64,
    "create:builders_tea": 16, "create:blaze_cake": 16,
    "create:shadow_steel": 32, "create:refined_radiance": 32,
    "create:polished_rose_quartz": 8,
    "create:potato_cannon": 16, "create:electron_tube": 6, "create:precision_mechanism": 16,
    "createaddition:chocolate_cake": 32, "createaddition:honey_cake": 32,
    "createaddition:cake_base": 8, "createaddition:cake_base_baked": 16,
    "create_enchantment_industry:experience_cake_slice": 16,
    "create_enchantment_industry:experience_cake": 64,
    "create_enchantment_industry:hyper_experience_bottle": 32,
    "createnuclear:reactor_alarm": 8, "createnuclear:reactor_blueprint_item": 4,
    "createnuclear:reactor_casing": 16, "createnuclear:reactor_cooler": 32,
    "createnuclear:reactor_fluid_input": 16, "createnuclear:reactor_frame": 32,
    "createnuclear:reactor_output": 16, "createnuclear:reactor_rod_input": 16,
    "supplementaries:netherite_door": 16, "supplementaries:netherite_trapdoor": 16,
    "quark:golden_apple_crate": 64,
}


# Reviewed recipe stages take priority over broad name/rarity heuristics. These
# are salvage tiers, not ingredient sums: preserve headroom for late assemblies
# instead of pricing every terminal, circuit, or mechanism alike.
PROGRESSION = {
    "create_new_world_core:hub_terminal": (16, "HUB terminal: single craft using an inductive mechanism and steel sheets"),
    "create_new_world_core:control_module": (16, "control module: one four-step electrical assembly loop"),
    "create_new_world_core:inductive_mechanism": (16, "inductive mechanism: early electrical sequenced assembly"),
    "create_new_world_core:steel_mechanism": (16, "steel mechanism: early industrial assembly"),
    "create_new_world_core:calculation_mechanism": (32, "calculation mechanism: intermediate computing assembly"),
    "create_new_world_core:sealed_mechanism": (32, "sealed mechanism: four assembly loops with chemical processing parts"),
    "create_new_world_core:integrated_mechanism": (32, "integrated mechanism: five assembly loops with circuits and regulator tubes"),
    "create_new_world_core:reinforced_mechanism": (48, "reinforced mechanism: five assembly loops upgrading a hardened precision mechanism"),
    "create_new_world_core:netherite_mechanism": (64, "netherite mechanism: reinforced assembly upgraded with molten netherite"),
    "create_new_world_core:optical_unit": (32, "optical unit: six assembly loops with optical parts, engraving, and filling"),
    "create_new_world_core:improved_circuit": (16, "improved circuit: three electrical, engraving, and focusing loops"),
    "create_new_world_core:integrated_circuit": (16, "integrated circuit: intermediate electrical component"),
    "create_new_world_core:advanced_circuit": (32, "advanced circuit: four assembly loops using improved circuits and lunar sapphire"),
    "create_new_world_core:platinum_circuit": (48, "platinum circuit: late advanced-material circuit"),
    "create_new_world_core:refined_radiance_circuit": (48, "radiance circuit: four focusing loops consuming advanced circuits and refined radiance"),
    "create_new_world_core:shadow_steel_circuit": (48, "shadow steel circuit: four focusing loops consuming advanced circuits and shadow steel"),
    "create_new_world_core:quantum_targeting_computer": (64, "quantum computer: four six-step loops upgrading a targeting computer with late circuits and dragon breath"),
    "northstar:computer_rack": (8, "computer rack: simple iron-sheet frame, crafted two at a time"),
    "northstar:circuit": (16, "circuit: five basic circuit assembly loops"),
    "northstar:advanced_circuit": (32, "advanced circuit: five assembly loops upgrading a basic circuit"),
    "northstar:circuit_engraver": (32, "circuit engraver: titanium-sheet precision machinery"),
    "northstar:targeting_computer": (48, "targeting computer: sequenced computing assembly used by the quantum upgrade"),
    "toms_storage:storage_terminal": (8, "storage terminal: planks, comparators, glass, and a chest"),
    "toms_storage:wireless_terminal": (16, "wireless terminal: utility electronics with a spyglass and ender pearl"),
    "toms_storage:crafting_terminal": (32, "crafting terminal: storage terminal upgraded with four diamonds"),
    "toms_storage:adv_wireless_terminal": (64, "advanced wireless terminal: netherite ingot and smithing-template upgrade"),
    "numismatics:bank_terminal": (16, "bank terminal: single craft with a precision mechanism and electron tube"),
    "numismaticsutils:portable_bank_terminal": (32, "portable bank terminal: bank terminal upgraded with brass sheets and wireless controls"),
    "powergrid:empty_circuit": (4, "empty circuit: blank substrate, before etching and assembly"),
    "powergrid:unetched_circuit": (8, "unetched circuit: partially processed circuit board"),
    "powergrid:circuit_schematic": (4, "circuit schematic: reusable circuit design, not a completed circuit"),
    "powergrid:integrated_circuit": (16, "integrated circuit: single mechanical craft using redstone, quartz, lapis, and gold nuggets"),
}


def legacy_values():
    text = (ROOT / "src/main/java/com/ourcreation/itemsink/ValueDefaults.java").read_text()
    result = {}
    duplicates = []
    for tier, body in re.findall(r"VALUE_(\d+) = List\.of\((.*?)\n    \);", text, re.S):
        for item in re.findall(r'"([^"\n]+)"', body):
            if item in result:
                duplicates.append({"id": item, "effective_tier": result[item], "later_tier": int(tier)})
            result.setdefault(item, int(tier))
    return result, duplicates


def has(path, *words):
    return any(re.search(r"(?:^|_)" + re.escape(word) + r"(?:_|$)", path) for word in words)


RAW_FOODS = set("""apple carrot potato beetroot sweet_berries glow_berries chorus_fruit melon_slice
    beef porkchop mutton chicken rabbit cod salmon tropical_fish cabbage cabbage_leaf tomato onion
    pumpkin_slice bacon minced_beef chicken_cuts cod_slice salmon_slice mutton_chops ham
    anglerfish bass blobfish bushmeat catfish clam_meat crab_meat drumstick duck lizard_tail
    mammoth_meat morsel piranha venison martian_strawberry ancient_fruit crab_leg angler_fish
    bluefish bluefish_slice dates dodo herring herring_slice tuna_chunk tuna_slice""".split())


def floor_tier(value):
    return max(tier for tier in TIERS if tier <= min(64, max(1, value)))


def classify(row, legacy):
    item = row["id"]
    namespace, full_path = item.split(":", 1)
    path = full_path.rsplit("/", 1)[-1]
    # Exclusion and fixed anchors precede every pricing rule, including old defaults.
    if has(path, "creative", "debug") or item in TECHNICAL or item in CURRENCY:
        return None, "currency or creative-only technical item"
    if item in ORESTONE_VALUES:
        return ORESTONE_VALUES[item], "frozen orestone value"
    progression_id = "create_new_world_core:" + path if namespace == "kubejs" else item
    if progression_id in PROGRESSION:
        return PROGRESSION[progression_id]
    if item in MANUAL:
        return MANUAL[item], "reviewed remaster override"
    if namespace == "kubejs" and "create_new_world_core:" + path in MANUAL:
        return MANUAL["create_new_world_core:" + path], "same remastered Core progression item"
    if namespace == "createdeco" and path.startswith("decal_"):
        return 2, "decorative hazard sign"
    if has(path, "incomplete", "unprocessed", "unfinished", "unbored", "partially_formed"):
        return 1, "unfinished processing intermediate"
    # Food must precede materials: golden food and chocolate are not metal parts.
    nutrition = row.get("food", 0)
    if nutrition:
        if has(path, "rotten", "poisonous", "poisoned"):
            return 1, "spoiled or harmful food"
        if path in RAW_FOODS:
            return (4 if nutrition >= 4 else 3 if nutrition >= 3 else 2), "raw food or farm produce"
        if has(path, "wheat_dough", "raw_pasta", "pie_crust"):
            return 4, "prepared cooking ingredient"
        if has(path, "cooked", "smoked", "fried_egg", "beef_patty"):
            return (16 if nutrition >= 8 else 8), "cooked food production reward"
        if has(path, "cookie", "candy"):
            return 4, "crafted snack production reward"
        return (32 if nutrition >= 10 else 16 if nutrition >= 6 else 8), "prepared food production reward"
    if has(path, "cannon_boat", "cannon_raft") or row.get("class") == "CannonBoatItem":
        return 8, "reviewed cannon boat, below diamond value"
    if has(path, "boat", "raft") or row.get("class") in {"BoatItem", "ChestBoatItem"}:
        return (6 if has(path, "chest") or row.get("class") == "ChestBoatItem" else 4), "wooden transport"
    if namespace == "createbigcannons":
        if has(path, "nugget"):
            return 1, "cannon metal nugget"
        if has(path, "cartridge", "shell", "projectile", "shot", "round", "fuze", "propellant", "gunpowder"):
            return (1 if has(path, "empty", "sheet", "spent") else 4), "ammunition or propellant"
        if has(path, "mould", "mold", "cast") and not has(path, "cast_iron"):
            return 2, "reusable casting tool"
        if has(path, "cannon", "autocannon", "breech", "carriage", "recoil", "mortar"):
            return (16 if has(path, "steel", "nethersteel") else 4 if has(path, "log") else 8), "cannon equipment salvage"
    if has(path, "cannon", "autocannon"):
        return 8, "ordinary cannon salvage, below diamond value"
    if namespace in {"buddycards", "createbuddycards", "collectorsalbum"}:
        if has(path, "medal", "trophy", "completion", "legendary"):
            return 32, "collection reward"
        if has(path, "gilded", "foil", "rare", "epic"):
            return 8, "special collectible"
        if has(path, "pack", "booster", "binder", "album"):
            return 4, "collection pack or storage"
        if "card" in path and not row["block"]:
            return 2, "collectible card"
    if path.endswith("spawn_egg") or "smithing_template" in path or "music_disc" in path:
        return 16, "special collectible or template"
    if "backpack" in path or namespace == "travelersbackpack" and path not in {"hose", "sleeping_bag", "backpack_tank"}:
        return 16, "crafted backpack"
    if has(path, "jetpack", "spacesuit", "space_suit", "oxygen_tank"):
        return 32, "advanced wearable equipment"
    if has(path, "quantum", "nether_star", "reactor", "warhead", "nuclear_bomb", "rocket_engine"):
        return 64, "endgame machinery or material"
    if has(path, "teleporter", "enrichment", "fuel_rod", "turbine", "thruster", "plutonium", "fusion"):
        return 32, "advanced machinery or nuclear processing"
    if has(path, "motor", "engine", "generator", "pumpjack", "distillation", "refinery", "computer", "radar"):
        return 16, "complete industrial machine"
    if has(path, "controller", "processor", "mechanism", "circuit", "bearing", "transmission", "battery", "inverter", "transformer", "terminal", "wireless", "storage_inventory"):
        return (32 if has(path, "advanced", "reinforced", "integrated", "refined_radiance", "shadow_steel") else 16), "control or processing assembly"
    material = next((value for name, value in sorted(MATERIALS.items(), key=lambda pair: -len(pair[0]))
                     if has(path, name)), None)
    if material is not None:
        if has(path, "coin", "coinstack"):
            nugget = floor_tier(material // 9)
            return (floor_tier(nugget * 4) if has(path, "coinstack") else nugget), "decorative coin made from nuggets"
        if has(path, "nugget"):
            return floor_tier(material // 9), "metal nugget"
        if has(path, "netherite_scrap"):
            return (8 if has(path, "dirty", "crushed") else 16), "netherite scrap processing intermediate"
        if path.endswith("_block") and (path.removesuffix("_block") in MATERIALS
                                        or path.startswith(("raw_", "enriched_", "charged_"))):
            return floor_tier(material * 9), "compressed material (conservative tier)"
        if has(path, "sword", "axe", "pickaxe", "shovel", "hoe", "helmet", "chestplate", "leggings", "boots", "knife", "spear", "rapier", "cutlass", "glaive", "warglaive", "sai", "katana", "claymore", "longsword", "twinblade", "greathammer", "greataxe", "chakram", "scythe", "halberd"):
            return floor_tier(material * (2 if has(path, "chestplate", "leggings", "helmet", "pickaxe", "axe", "sword") else 1)), "equipment material and construction"
        if path.endswith("_ore"):
            return floor_tier(material * 2), "unprocessed mineral deposit"
        return material, "material priced against fixed orestone anchors"
    if has(path, "gear", "gears", "gearbox", "cogwheel", "shaft", "pipe", "wire", "cable", "tube", "sheet", "rod", "plate", "screw", "casing", "capacitor", "track", "bogey", "connector", "rotor", "propeller", "tank", "vault"):
        return (2 if has(path, "shaft", "pipe", "wire", "cable", "rod", "screw") else 8 if has(path, "gearbox", "bogey", "tank", "vault") else 4), "processed mechanical component"
    if has(path, "shell", "cartridge", "projectile", "bullet", "round", "propellant"):
        return 2, "basic ammunition or propellant"
    if has(path, "workbench", "table", "bench", "bed", "couch", "sofa", "desk", "cabinet", "stove", "cooking_pot", "skillet"):
        return 4, "crafted furniture or workstation"
    if namespace in {"chipped", "biomesoplenty", "quark", "mcwfences", "mcwbridges", "handcrafted", "createdeco"}:
        return (2 if has(path, "planks", "door", "trapdoor", "fence", "bridge", "gate", "ladder") else 1), "decorative or natural material"
    if has(path, "sapling", "seeds", "seed", "leaves", "flower", "grass", "moss", "dirt", "sand", "gravel", "cobblestone", "stone", "limestone", "log", "wood", "coral", "kelp", "fungus", "mushroom"):
        return 1, "common natural material"
    if has(path, "planks", "slab", "stairs", "wall", "fence", "door", "trapdoor", "wool", "carpet", "glass", "terracotta", "concrete", "torch", "button", "pressure_plate"):
        return (2 if has(path, "planks", "door", "trapdoor", "fence") else 1), "common construction material"
    if namespace in {"bosses_of_mass_destruction", "block_factorys_bosses", "nexus"} and item in legacy:
        return legacy[item], "rare legacy progression reward"
    if row["rarity"] == "EPIC":
        return 32, "epic item"
    if row["rarity"] == "RARE":
        return 16, "rare item"
    if row["rarity"] == "UNCOMMON":
        return 8, "uncommon item"
    if namespace.startswith("create") or namespace in {"aeronautics", "aeroworks", "offroad", "powergrid", "petrochem", "northstar", "gadgetsngizmos", "railways", "bits_n_bobs", "toms_storage", "struts", "simulated", "tracks", "linearbearing"}:
        return (4 if row["block"] else 2), "general crafted part or utility"
    if not row["block"] and row["stack"] == 1:
        return 4, "basic utility or equipment"
    return (2 if row["block"] else 1), "common utility block or raw item"


def generate_tags(additions, original_values):
    """Write optional item entries so absent mods never break the tier tags."""
    root = ROOT / "src/main/resources/data/item_sink/tags/item"
    for tier in TIERS:
        original = sorted(item for item, value in original_values.items() if value == tier)
        for group, items in (("original", original), ("createcore", additions[str(tier)])):
            path = root / group / f"value_{tier}.json"
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(json.dumps({"replace": False, "values": [
                {"id": item, "required": False} for item in items
            ]}, indent=2) + "\n", encoding="utf-8")
        (root / f"value_{tier}.json").write_text(json.dumps({
            "replace": False,
            "values": [f"#item_sink:original/value_{tier}",
                       f"#item_sink:createcore/value_{tier}"]
        }, indent=2) + "\n", encoding="utf-8")


def generate(registry_path):
    registry = json.loads(registry_path.read_text(encoding="utf-8"))
    # Keep historical registry snapshots reproducible after the block ID rename.
    for row in registry["items"]:
        if row["id"] == "create_item_sink:item_sink":
            row["id"] = "item_sink:item_sink"
    registry["items"].sort(key=lambda row: row["id"])
    legacy, duplicates = legacy_values()
    registered = {row["id"]: row for row in registry["items"]}
    if len(registered) != len(registry["items"]):
        raise ValueError("Registry contains duplicate item ids")
    # Historical Java lists identify old configs; current prices live in the tags.
    # Keep absent legacy mods supported using the same remaster rules.
    original_values = {}
    for item in legacy:
        row = registered.get(item, {"id": item, "block": False, "rarity": "COMMON", "stack": 64, "food": 0})
        tier, _ = classify(row, legacy)
        if tier is not None:
            original_values[item] = tier
    additions = {str(tier): [] for tier in TIERS}
    audit = []
    namespace_counts = collections.defaultdict(collections.Counter)
    for row in registry["items"]:
        item = row["id"]
        tier, reason = classify(row, legacy)
        status = "excluded" if tier is None else "existing" if item in legacy else "added"
        if status == "added":
            additions[str(tier)].append(item)
        namespace_counts[item.split(":")[0]][status] += 1
        audit.append({"id": item, "value": tier if tier is not None else "", "status": status, "reason": reason})
    for values in additions.values():
        values.sort()
    output = ROOT / "src/main/resources/data/item_sink/item_values/createcore.json"
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(additions, indent=2) + "\n", encoding="utf-8")
    generate_tags(additions, original_values)
    docs = ROOT / "docs"
    docs.mkdir(exist_ok=True)
    with (docs / "item-values.csv").open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=("id", "value", "status", "reason"))
        writer.writeheader()
        writer.writerows(audit)
    report = {
        "registered_items": len(registry["items"]), "legacy_unique_items": len(legacy),
        "added_items": sum(map(len, additions.values())),
        "original_registered_items": sum(row["status"] == "existing" for row in audit),
        "rebalanced_registered_original_items": sum(row["status"] == "existing" and row["value"] != legacy[row["id"]] for row in audit),
        "preserved_registered_items": sum(row["status"] == "existing" and row["value"] == legacy[row["id"]] for row in audit),
        "frozen_orestone_items": len(ORESTONE_VALUES),
        "frozen_orestone_values": ORESTONE_VALUES,
        "excluded_items": [row["id"] for row in audit if row["status"] == "excluded"],
        "historical_legacy_duplicates": duplicates,
        "legacy_ids_absent_from_scan": sorted(set(legacy) - {row["id"] for row in registry["items"]}),
        "added_tier_counts": {tier: len(items) for tier, items in additions.items()},
        "namespaces": dict(sorted(namespace_counts.items())), "loaded_mods": registry["mods"],
    }
    (docs / "item-value-coverage.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    shutil_target = docs / "value-registry.json"
    shutil_target.write_text(json.dumps(registry, indent=2) + "\n", encoding="utf-8")
    print(f"Priced {report['added_items']} additional items across {len(namespace_counts)} namespaces; "
          f"remastered {report['rebalanced_registered_original_items']} registered original prices; "
          f"froze {report['frozen_orestone_items']} orestone prices; "
          f"excluded {len(report['excluded_items'])} currency/technical items.")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--registry", type=Path, default=ROOT / "build/value-registry.json")
    generate(parser.parse_args().registry)
