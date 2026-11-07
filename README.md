# Create: Item Sink

A configurable Create addon that converts unwanted items into currency.

Version **1.0.0** for Minecraft **1.21.1**, **NeoForge 21.1.251+**, **Create 6.0.x**, and **Java 21**.

**Source:** [NeoForge branch](https://github.com/IWasCool/create-item-sink-forge/tree/neoforge-1.21.1).
**Download:** [NeoForge 1.0.0 release](https://github.com/IWasCool/create-item-sink-forge/releases/tag/neoforge-v1.0.0).
Place the NeoForge JAR in the client and server `mods` folders alongside Create.

The [Forge 1.20.1 edition](https://github.com/IWasCool/create-item-sink-forge) is a separate build. Use the JAR matching your Minecraft version and loader.

Create: Numismatics and JEI are optional. Default outputs use Numismatics coins. Without Numismatics, configure installed currency items such as `minecraft:emerald` in `config/item_sink-common.toml`. Missing currencies disable their conversions.

1.0.0 introduced ordered pricing rules for exact item IDs, tags, phrases, wildcards, and entire mods, with custom payouts and fallback prices. Existing prices, customized tier lists, output amounts, FE costs, and processing times remain in use. See the [configuration guide](CONFIGURATION.md) and [item value catalogue](docs/item-values.md).

Right-click the Item Sink, insert unwanted items, and supply Forge Energy to convert them into currency. Hold **W** over the item for Ponder tutorials for basic use and Create belts and funnels. With Create Crafts & Additions installed, a third tutorial shows a charged accumulator connected to the sink by a copper wire, with the battery connector set to Pull and the sink connector set to Push. Any pickaxe recovers the machine; stored items drop separately.

JEI shows one compact `? -> currency` overview with an **Any Item** input tooltip. Currency outputs are protected from being sold back into the machine.

Existing Minecraft 1.21.1 language support is preserved across 137 locales. Newer configuration labels and tutorial captions use English fallbacks where a translation is unavailable. Initial translations welcome native review.

## Build and verify

```powershell
.\gradlew.bat build
.\gradlew.bat runGameTestServer
.\gradlew.bat runGameTestServer -PwithoutNumismatics
py -3 tools/check_translations.py build/libs/create_item_sink-1.0.0.jar
```

On Linux and macOS use `bash ./gradlew` instead. The release JAR is `build/libs/create_item_sink-1.0.0.jar`. Dependencies download automatically; local mod JARs are not required. Test code is excluded from the release JAR.

Worlds using the current `item_sink` registry IDs remain compatible. Worlds from the former `create_item_sink` namespace require an ID migration before upgrading.

Available under the [MIT license](LICENSE).
