# Create: Item Sink

A configurable Create addon for **Minecraft 1.21.1 / NeoForge** that converts unwanted items into currency.

Version **1.0.0** requires **NeoForge 21.1.251+**, **Create 6.0.x**, and **Java 21**.

## Install

Download `create_item_sink-neoforge-1.21.1-1.0.0.jar` from the [1.0.0 release](https://github.com/IWasCool/create-item-sink/releases/tag/neoforge-v1.0.0). Place it in the client and server `mods` folders alongside Create.

Create: Numismatics and JEI are optional. Default payouts use Numismatics coins. Without Numismatics, configure installed currency items such as `minecraft:emerald` in `config/item_sink-common.toml`. Conversions with missing currency items stay disabled.

## Use

Right-click the Item Sink to open its inventory. Insert unwanted items, supply **FE**, and collect the currency. Belts and funnels can automate input and output. Full output slots pause processing until space is available. Mine the sink with any pickaxe to recover it; stored items drop separately.

Hold **W** over the Item Sink for a Ponder tutorial covering use, power, conversion, and belt/funnel automation. If Create Crafts & Additions is installed, the tutorial shows a connected accumulator and copper wire as a power example. That addon is optional.

Pricing rules support exact item IDs, tags, phrases, wildcards, and whole mods. Configure currencies, payout amounts, processing times, and FE costs, or replace the bundled prices with your own economy. Currency outputs cannot be sold back into the sink.

See the [configuration guide](CONFIGURATION.md) and [changelog](CHANGELOG.md).

JEI shows a compact `? -> currency` overview with an **Any Item** tooltip. Language resources cover 137 Minecraft 1.21.1 locales; newer captions and labels use English fallbacks where needed. Translation corrections are welcome.

## Build from source

Use Java 21. On Windows:

```powershell
.\gradlew.bat build
```

On Linux and macOS, use `bash ./gradlew build`. The build produces `build/libs/create_item_sink-1.0.0.jar`. Dependencies download automatically.

Worlds using the current `item_sink` registry IDs remain compatible. Worlds from the former `create_item_sink` namespace require an ID migration before upgrading.

Source: [IWasCool/create-item-sink](https://github.com/IWasCool/create-item-sink). Available under the [MIT license](LICENSE).
