# Update Log

## v0.0.4

- Fixed the energy bar and tooltip showing incorrect values above 32,767 FE.
- Synced server energy capacity and long processing times correctly to the machine screen.
- Removed two unused textures and a redundant item value catalogue from the mod.
- Added packet serialization regression tests; test code and dependencies are excluded from the release jar.

## v0.0.3

- Fixed the missing pickaxe mining tag so the Item Sink mines at pickaxe speed and drops itself when harvested with any pickaxe.
- Removed development tests and generated audit files from the source distribution.

## v0.0.2

- Added two Item Sink Ponder scenes covering manual conversion, FE power, currencies, hopper automation, and blocked outputs.

- Kept the name Create: Item Sink and the release filename `create_item_sink-0.0.2.jar` without loader or Minecraft-version suffixes.

- Added a separate [Forge edition](README.md) for Minecraft 1.20.1, Forge 47.4.x, Java 17, and Create 6.0.x.
- Ported registration, item and FE capabilities, capability invalidation, block interaction, menu networking, client screens, configuration, and NBT persistence to Forge.
- Shared the existing assets, translations, and item values, with automatic conversion to Minecraft 1.20.1 datapack paths and crafting result format.
- Retained optional Numismatics currencies and JEI support, config migration, value overrides, exclusions, and currency loop protection.


- Extracted the Forge edition into a standalone project with local resources, documentation, and a Gradle wrapper.
