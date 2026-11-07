# Create: Item Sink

A configurable Create addon that converts unwanted items into currency.

Version **1.0.0** for Minecraft 1.20.1, Forge 47.4.x (47.4.0+), Create 6.0.x and Java 17.

For **Minecraft 1.21.1 / NeoForge**, use the separate [NeoForge 1.0.0 release](https://github.com/IWasCool/create-item-sink-forge/releases/tag/neoforge-v1.0.0) and [source branch](https://github.com/IWasCool/create-item-sink-forge/tree/neoforge-1.21.1).

**Public source:** [IWasCool/create-item-sink-forge](https://github.com/IWasCool/create-item-sink-forge).

Download the jar from [Releases](https://github.com/IWasCool/create-item-sink-forge/releases) and place it in your client and server's `mods` folders alongside Create.

Create: Numismatics and JEI are optional. The default outputs use Numismatics coins. Without Numismatics, configure installed currency items (such as `minecraft:emerald`) as the tiers' `outputItem` values in `config/item_sink-common.toml`; tiers with missing output items stay disabled.

Pricing supports ordered config rules for item IDs, tags, phrases, wildcards, and entire mods. Keep the current prices as defaults or build a custom economy with any installed currency items, payout amounts, processing times, and FE costs. See the [configuration guide](CONFIGURATION.md) for examples.

Right-click the sink to open it. Insert unwanted items and supply Forge Energy to convert them into currency. Hold **W** over the item to view its Ponder tutorials.

Mine the Item Sink with any pickaxe to recover the machine. Its stored items drop separately.

JEI shows one compact `? -> currency` overview, with checkerboard slots and an **Any Item** tooltip over the question mark.

Includes two short Ponder tutorials: basic use and Create automation. Belts sit directly on Create's checkerboard floor, with funnels feeding items into the sink and extracting currency onto an output belt.

Initial translations cover all 143 Minecraft 1.20.1 language options; new Create automation captions fall back to English. Translation corrections are welcome.

Build from source with `./gradlew build` or `gradlew.bat build` on Windows. The release jar is `build/libs/create_item_sink-1.0.0.jar`.

Run the server GameTests with `./gradlew runGameTestServer`, and repeat with `-PwithoutNumismatics` to verify custom currencies without Numismatics. The test mod is excluded from the release jar.

The code is available to view, reuse, modify and port under the [MIT license](LICENSE).
