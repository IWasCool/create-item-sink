# Create: Item Sink

A configurable Create addon that converts unwanted items into currency.

Version **0.0.4** for Minecraft 1.20.1, Forge 47.4.x (47.4.0+), Create 6.0.x and Java 17.

Download the jar from [Releases](https://github.com/IWasCool/create-item-sink-forge/releases) and place it in your client and server's `mods` folders alongside Create.

Create: Numismatics and JEI are optional. The default outputs use Numismatics coins. Without Numismatics, configure installed currency items (such as `minecraft:emerald`) as the tiers' `outputItem` values in `config/item_sink-common.toml`; tiers with missing output items stay disabled.

Right-click the sink to open it. Insert unwanted items and supply Forge Energy to convert them into currency. Hold **W** over the item to view its Ponder tutorials.

Mine the Item Sink with any pickaxe to recover the machine. Its stored items drop separately.

Includes two Ponder tutorials and initial translations for all 143 Minecraft 1.20.1 language options. Translation corrections are welcome.

Build from source with `./gradlew build` or `gradlew.bat build` on Windows. The jar is written to `build/libs/`.

The code is available to view, reuse, modify and port under the [MIT license](LICENSE).
