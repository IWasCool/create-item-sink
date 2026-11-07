# Update Log

## v1.0.0 | NeoForge 1.21.1

- Ported Forge 1.0.0 pricing rules: ordered exact IDs, tags, phrases, wildcards, whole-mod selectors, direct custom payouts, fallback values, and optional replacement of bundled prices.
- Preserved the NeoForge 0.0.4 catalogue, tier settings, custom inputs, IDs, and existing world data. Currency protection and exclusions apply to every pricing source.
- Added the short basic-use and Create belt/funnel Ponder tutorials, plus the concise Any Item JEI tooltip.
- Kept all 137 Minecraft 1.21.1 locales, with English fallbacks for new untranslated text.
- Made source builds reproducible with downloaded dependencies and separate server GameTests, including a run without Numismatics.

## v0.0.4 | Progression Values and Compact JEI

- Priced the HUB Terminal at 16 instead of 64; the Quantum Targeting Computer remains at 64. Reviewed electronics and assembly stages now distinguish simple frames and utility terminals from advanced circuits, reinforced mechanisms, and quantum upgrades.
- Separated Tom's Storage terminal prices: storage 8, wireless 16, crafting 32, and netherite-upgraded advanced wireless 64. Late Core circuits pay 48, above advanced circuits at 32; targeting computers pay 48 before the quantum upgrade.
- Preserved the 163 frozen orestone prices, food rewards, existing tier settings, and custom item overrides. Regenerated the bundled tags and auditable item catalogue.
- Replaced thousands of per-item JEI conversions with one question-mark “Any Item → Currency” overview. Its output cycles through available configured currencies (Spur → Bevel → Sprocket → Cog by default); hover text explains item-dependent payouts and exclusions. Missing currencies are omitted.
- Added all five JEI overview strings to all 137 Minecraft 1.21.1 language options, including regional and novelty variants. Every locale now has all 87 translation keys; strict checks reject missing keys. Existing translations and Ponder captions are preserved. These are initial translations awaiting native review, with symbolic technical text retained for Ngiiwa and Tsotsil.
- Kept the bundled Forge compatibility sources aligned and allowed the JEI card to fit longer translated labels and payout explanations.
- Installed and hash-verified 0.0.4 in both the existing Create New World client and server pack. Migrated six recognized legacy client price lists to the bundled tags while preserving other settings. Each profile has exactly one Item Sink jar; rollback copies and verification details are retained locally.

## v0.0.3 | Mining and Translations

- Added the pickaxe mining tag so the sink mines normally and drops itself when harvested with any pickaxe.
- Added the 14 missing Ponder captions to all 136 non-US-English locales using the existing Forge translations.
- Applied the MIT license to this edition and included both license notices in the jar.
- Built the Create New World update from the installed resource baseline to preserve its current item prices; pending price-remaster source changes remain separate.

## v0.0.2 | Item Value Remaster

- Rebuilt the default price scale around the existing orestone payouts, preserving all 163 orestone entries and their variants.
- Raised diamonds to 32, emeralds to 16, and netherite ingots to 64. Ordinary boats pay 4; Supplementaries cannons and cannon boats pay 8 across all wood variants.
- Rebalanced materials, equipment, components, and machinery so common crafted items no longer receive endgame prices just because of their names.
- Increased food payouts, with cooked foods at 8–16, prepared meals at 16–32, and whole feasts at 64, encouraging cooking and automated food production.
- Updated both editions' shared value tags and the item catalogue. Existing custom item prices, currency outputs, tier timings, and FE settings remain configurable; unchanged default lists migrate automatically.

## v0.0.2 | Ponder Tutorials (NeoForge 1.21.1)

- Ported the Forge edition's two Item Sink Ponder scenes to NeoForge 1.21.1, covering manual conversion, FE power, currencies, hopper automation, and blocked outputs.
- Kept existing item IDs, configuration, recipes, translations, and value tags compatible with the installed Create New World modpack.

## v0.0.2 | Forge 1.20.1 Edition

- Added a separate [Forge edition](forge/README.md) for Minecraft 1.20.1, Forge 47.4.x, Java 17, and Create 6.0.x.
- Ported registration, item and FE capabilities, capability invalidation, block interaction, menu networking, client screens, configuration, and NBT persistence to Forge.
- Shared the existing assets, translations, and item values, with automatic conversion to Minecraft 1.20.1 datapack paths and crafting result format.
- Retained optional Numismatics currencies and JEI support, config migration, value overrides, exclusions, and currency loop protection.
- Added six server GameTests covering conversion, automation, energy, persistence, menus, loaded resources, and configuration behavior, including a run without Numismatics.

## v0.0.2 | Expanded Item Values, Languages & Configuration

- Added 15,919 CreateCore item values while preserving existing prices and processing settings.
- Added item tags, datapack support, explicit item overrides, exclusions, and a CreateCore opt-out.
- Added configurable currency items, output amounts, processing times, and FE costs per tier. Numismatics is now optional; Create remains required.
- Added missing-currency warnings, handling for absent optional mods, and currency-loop protection for tag inputs.
- Added language resources for all 137 Minecraft 1.21.1 languages and an electric motor buzz sound.
- Added config migration, compact default lists, and value refreshes on config/datapack reload. Restart to refresh JEI.
- Added an [item value catalogue and configuration guide](docs/item-values.md).
- Standardized IDs and configuration under `item_sink`. Older `create_item_sink` worlds require manual ID migration.

## v0.0.1 | Initial Release

- Added the Item Sink with currency exchange tiers for Minecraft 1.21.1 / NeoForge.
- Added configurable energy storage, input limits, and optional energy requirements.
- Added inventory automation, an energy/progress screen, and JEI conversion information.
