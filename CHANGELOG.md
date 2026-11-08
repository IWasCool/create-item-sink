# Changelog

All versions below are for Minecraft 1.21.1 / NeoForge.

## 1.0.0

- Added ordered pricing rules for exact item IDs, tags, phrases, wildcards, and whole mods.
- Added direct custom payouts, fallback prices, and an option to replace the bundled economy. Existing prices, customized tier lists, currency amounts, FE costs, and processing times remain in use.
- Applied exclusions and currency loop protection to every pricing source.
- Combined input, FE power, processing, belt/funnel automation, currency collection, and blocked outputs into one Ponder tutorial.
- Shows a connected accumulator and copper wire when Create Crafts & Additions is installed. Without that addon, the tutorial keeps the brief FE instruction and omits its power blocks and wire.
- Simplified the JEI input tooltip to **Any Item** and included configured currencies in the overview.
- Preserved 137 locale files, with English fallbacks for new captions and configuration labels.
- Added reproducible source builds, 13 server GameTests with and without Numismatics, translation checks, and catalogue tests.

## 0.0.4

- Refined progression prices for terminals, circuits, mechanisms, and advanced machinery. The HUB Terminal pays 16; the Quantum Targeting Computer remains at 64.
- Separated Tom's Storage terminal values into storage, wireless, crafting, and advanced wireless tiers.
- Preserved all 163 frozen orestone prices, food rewards, existing tier settings, and custom item overrides. Updated the bundled tags and item catalogue.
- Replaced per-item JEI conversions with a compact question-mark overview cycling through available configured currencies.
- Added localized JEI overview strings across all 137 supported locales and allowed longer labels to fit the card.

## 0.0.3

- Added the pickaxe mining tag and corrected machine drops.
- Added missing Ponder captions to non-US-English locales.
- Applied the MIT license and included both license notices in the JAR.

## 0.0.2

- Added Ponder tutorials for manual use, FE power, currencies, automation, and blocked outputs.
- Remastered prices around the existing orestone payouts, preserving all 163 orestone entries and their variants.
- Rebalanced materials, equipment, components, machinery, food, and prepared meals.
- Added expanded CreateCore item values, item tags, datapack support, explicit price overrides, exclusions, and a CreateCore opt-out.
- Added configurable currency items, payout amounts, processing times, and FE costs per tier. Numismatics became optional.
- Added missing-currency warnings, currency loop protection, configuration migration, compact default lists, and cache refreshes on configuration and tag reloads.
- Added 137 locale files, an electric motor sound, and the item value catalogue.
- Standardized registry IDs and configuration under `item_sink`. Worlds using the former `create_item_sink` namespace require manual ID migration.

## 0.0.1

- Added the Item Sink and its currency exchange tiers for Minecraft 1.21.1 / NeoForge.
- Added configurable energy storage, input limits, optional energy requirements, inventory automation, the machine screen, and JEI conversions.
