# Create: Item Sink

A configurable Create addon for **Minecraft 1.21.1 / NeoForge** that converts unwanted items into currency.

Version **1.0.1** requires **NeoForge 21.1.256+**, **Create 6.0.x**, and **Java 21**. Built and tested with Create **6.0.10**.

## Install

Download `item_sink-neoforge-1.21.1-1.0.1.jar` from the [1.0.1 release](https://github.com/IWasCool/create-item-sink/releases/tag/neoforge-v1.0.1). Place it in the client and server `mods` folders alongside Create. Replace the previous Item Sink jar so only one edition is installed.

Create: Numismatics and JEI are optional. Default payouts use Numismatics coins. Without Numismatics, configure installed currency items such as `minecraft:emerald` in `config/item_sink-common.toml`. Conversions with missing currency items stay disabled.

## Use

Right-click the Item Sink to open its inventory. Insert unwanted items, supply **FE**, and collect the currency. Belts and funnels can automate input and output. Full output slots pause processing until space is available.

Mine the sink with any pickaxe to recover it. Like the Hub Terminal, a Create wrench rotates it on right-click and picks it up on sneak-right-click. Wrench pickup returns the machine to your inventory, or drops it when your inventory is full. Stored input and currency drop separately.

Hold **W** over the Item Sink for a Ponder tutorial covering use, power, conversion, and belt/funnel automation. If Create Crafts & Additions is installed, the tutorial shows a connected accumulator and copper wire as a power example. That addon is optional.

Pricing rules support exact item IDs, tags, phrases, wildcards, and whole mods. Configure currencies, payout amounts, processing times, and FE costs, or replace the bundled prices with your own economy. Currency outputs cannot be sold back into the sink. Settings live in `config/item_sink-common.toml`; restart after changing configuration.

JEI shows a compact `? -> currency` overview using the **original font-rendered question mark**, with an **Any Item** tooltip.

The supported language set matches Create New World Core: **18 locales**, with all 86 Item Sink translation keys present in each: `de_de`, `en_us`, `es_es`, `es_mx`, `fr_fr`, `id_id`, `it_it`, `ja_jp`, `ko_kr`, `pl_pl`, `pt_br`, `pt_pt`, `ru_ru`, `tr_tr`, `uk_ua`, `vi_vn`, `zh_cn`, and `zh_tw`. Newer captions and labels use English fallbacks where needed. Translation corrections are welcome.

## Compatibility and verification

Verified on assembled **Sable 2.0.5 and 2.0.6** structures after translation and rotation: inventory, FE, and progress survive assembly; normal block-entity ticks convert items; automation capabilities and menu distance checks work; Create wrench pickup returns the sink. The isolated test holds physics still during the short check. Prolonged active flight and client targeting were outside its scope.

The focused native suite passed 15 Item Sink server tests, 16 with the Sable integration enabled, and a client check rendering the JEI category. The working 1.0.1 build uses the original question mark, confirmed by the native client render check. Production builds verify the exact supported language set and complete translation keys.

## Build from source

Use **JDK 25** for Gradle. The build selects **Java 21** for Minecraft. On Windows:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-25'
.\gradlew.bat build
```

On Linux and macOS, use `bash ./gradlew build`. The build produces `build/libs/item_sink-neoforge-1.21.1-1.0.1.jar`. Dependencies download automatically. Add `-PwithJei` to include JEI in a development client run.

Worlds using the current `item_sink` registry IDs remain compatible. Worlds from the former `create_item_sink` namespace require an ID migration before upgrading.

Source: [IWasCool/create-item-sink](https://github.com/IWasCool/create-item-sink). Available under the [MIT license](LICENSE).
