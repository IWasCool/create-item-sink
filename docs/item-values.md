# CreateCore item values

The catalogue remasters both the original item prices and the expanded CreateCore prices around the existing orestone payouts. All **163 orestone entries** retain their previous values, including variants and items from other mods. The six base stones—Asurine, Crimsite, Ochrum, Veridium, Scoria, and Scorchia—remain at 8.

The scan inventoried all 195 jars in `createnewworldcore-1.21.1/libs`, inspected their bundled archives, and loaded server mods and their dependencies in a separate development run. It found **16,235 registered items across 80 item namespaces**. Of these, 274 belong to the original catalogue and **15,917 belong to the expansion**; 44 are excluded. Among the registered originals, 205 receive revised prices and 69 retain their previous prices. The other 188 original ids belong to absent/legacy mods and retain optional entries for compatibility. Client utilities and libraries that register no items need no exchange entries. When duplicate mod versions exist in Core's `libs`, the scan selects the newest version.

Every registered item is accounted for in [item-values.csv](item-values.csv), with its price, catalogue membership, and pricing rationale. The `status` column uses `existing` for original entries, `added` for expansion entries, and `excluded` for items without a price; `existing` does not mean that the old price was preserved. [item-value-coverage.json](item-value-coverage.json) lists original and revised-price counts, all frozen orestone values, counts by namespace and tier, the exclusions, and legacy ids missing from this installation. [value-mod-inventory.json](value-mod-inventory.json) records every inspected jar, including bundled mods. [value-registry.json](value-registry.json) is the exact registered-item snapshot used to generate the prices.

## Pricing

Prices use the existing 1, 2, 3, 4, 5, 6, 7, 8, 16, 32, 48, and 64 tiers. The main reference points are:

| Item or category | Value |
| --- | ---: |
| Base orestones | 8, unchanged |
| Copper / iron / gold ingots | 2 / 4 / 6 |
| Zinc / brass ingots | 4 / 6 |
| Emerald / diamond / netherite ingot | 16 / 32 / 64 |
| Ordinary boat / chest boat | 4 / 6 |
| Supplementaries cannon or cannon boat | 8 |
| Basic mechanical components | 2–8 |
| Ordinary machinery | 8–16 |
| Advanced machinery / endgame items | 32 / 64 |
| HUB Terminal / Northstar computer rack | 16 / 8 |
| Northstar Targeting Computer / Quantum Targeting Computer | 48 / 64 |
| Core advanced / radiance, shadow steel, or platinum circuits | 32 / 48 |
| Core reinforced / netherite mechanisms | 48 / 64 |
| Tom's Storage: storage / wireless / crafting / advanced wireless terminals | 8 / 16 / 32 / 64 |

Reviewed progression prices take priority over name and rarity heuristics. The HUB Terminal is a single craft with steel sheets, a display, handles, and an inductive mechanism; its 16-value payout is a quarter of the quantum computer's 64. The quantum upgrade starts with a Targeting Computer and repeats six operations four times, consuming late circuits and dragon's breath. Circuit blanks and schematics remain at 4, unetched boards at 8, assembled electrical parts at 16, advanced assemblies at 32, and late circuits at 48. This leaves room for meaningful upgrades instead of assigning the same price to everything called a terminal, circuit, or mechanism. The generator's `PROGRESSION` table records the reviewed stage and reason for each exception, including legacy KubeJS aliases.

Common natural and decorative blocks stay near 1. Equipment scales with its material, and cannon parts receive prices appropriate to their material and stage of completion. Incomplete processing intermediates receive value 1. Modded wood variants follow the same boat prices. Specific item overrides and repeatable category rules are in [build_item_values.py](../tools/build_item_values.py).

Food production receives a larger reward: raw ingredients generally pay 2–4, cooked foods 8–16, prepared meals 16–32, whole pies 32, and whole feasts 64. Small prepared snacks have their own lower prices. Food rules run before general material rules, so foods such as golden carrots are priced as food. Drinks and placeable food blocks receive explicit prices even when the registry reports no nutrition. Rotten food and poisonous items stay low; nuclear yellowcake does not receive a meal payout.

These are salvage prices, rather than a guarantee of equal value across every crafting recipe. Modded recipe changes, item components, enchantments, backpack contents, tank contents, and other stored data do not change the price for an item id. The CSV makes individual adjustments reviewable.

The catalogue excludes 44 items: Numismatics currency coins and creative-only, debug, or technical items, including infinite power sources and command/structure blocks. Coins must not become inputs that mint more coins. Any item configured as a tier's output is also excluded from the bundled inputs. Explicit user tier entries continue to take priority.

## Configuration

Each tier defaults to a single item tag: `#item_sink:value_1`, `#item_sink:value_2`, `#item_sink:value_3`, `#item_sink:value_4`, `#item_sink:value_5`, `#item_sink:value_6`, `#item_sink:value_7`, `#item_sink:value_8`, `#item_sink:value_16`, `#item_sink:value_32`, `#item_sink:value_48`, and `#item_sink:value_64`. Each tag bundles the original defaults and the additional CreateCore items for that value. Missing optional items do not prevent tags from loading.

In `config/item_sink-common.toml`, keep the default tag and append individual item ids or other item tags:

```toml
[item_sink.values]
# Keep the bundled value 1 items and add sticks.
value1Items = ["#item_sink:value_1", "minecraft:stick"]
# Keep the bundled value 4 items and add everything in the tin ingot tag.
value4Items = ["#item_sink:value_4", "#c:ingots/tin"]
```

Individual item ids take priority over tags, so adding an item id to another tier moves its value. If multiple individual lists contain the same id, the first tier wins; overlapping tags also use the first tier. Exclusions in `item_sink.values.excludedItems` take priority over both. Currency outputs are excluded from tag-derived inputs to prevent currency loops.

When `item_sink-common.toml` is absent, the previous `create_item_sink-common.toml` is copied automatically, and old built-in tag references are renamed to `#item_sink:`. Unchanged old default lists are compacted automatically on config load and receive the remastered prices through the bundled tags; customized lists are retained. Existing configs still receive CreateCore additions automatically. Set `includeCreateCoreValues = false` to disable those additions; the default tier tags then use only their remastered original item groups. Explicit item ids and custom tags remain available. Tier currency, quantity, processing time, and energy settings are unchanged, although an item moved to a different tier uses that tier's settings. Config and datapack tag reloads invalidate the cached lookup table.

### Add items with a datapack

Create `world/datapacks/my_sink_values/pack.mcmeta`:

```json
{
  "pack": {
    "pack_format": 48,
    "description": "Extra Item Sink values"
  }
}
```

Create `world/datapacks/my_sink_values/data/item_sink/tags/item/value_1.json`:

```json
{
  "replace": false,
  "values": [
    "minecraft:stick",
    { "id": "yourmod:your_item", "required": false }
  ]
}
```

Use `value_2.json`, `value_4.json`, or any other tier filename to extend that value instead. `replace: false` keeps the bundled items. Replace the example mod item with your actual item id; `required: false` allows the pack to load when that mod is absent. Run `/reload` after enabling or editing the datapack. Restart the game to refresh JEI's available currencies after changing tags or config.

## JEI overview

The Item Sink catalyst opens one conversion card: a question-mark input labelled **Any Item**, an arrow, and a cycling currency output. Default currencies cycle through **Spur → Bevel → Sprocket → Cog** once each. These icons illustrate the available currencies; they do not promise a fixed payout or let players choose a coin. The input's configured value determines the actual currency, amount, processing time, and energy cost.

Hover over the question mark or input label for the acceptance rules. “Any Item” means any item with a configured exchange; currency outputs, creative/technical items, and exclusions are not accepted by default. The question mark is a drawn symbol, not a registered item, and JEI no longer indexes all priced items as separate sink recipes. Custom currency items replace the defaults in the cycle. Missing currencies and currencies without valid inputs are omitted; if no conversion is available, no overview is registered.

## Rebuild the catalogue

The registry exporter and integration checks are development-only and are excluded from the released jar. They run under `build/value-scan-run` and do not open player saves. The generated mod copies are under `build/value-scan-mods`; the source mod jars are never modified.

The full Core collection currently requires NeoForge 21.1.255 for the scan because its Create Better Networking jar requires that version (Quark requires at least 21.1.252). The production Item Sink build retains its existing NeoForge 21.1.251 setting. The version override below applies only to this development invocation.

```powershell
$env:JAVA_HOME = 'C:/Program Files/Java/jdk-21'
py tools/scan_item_values.py
.\gradlew.bat --no-configuration-cache '-Pneo_version=21.1.255' -I gradle/value-scan.init.gradle runValueScan
py tools/build_item_values.py
```

After adjusting the rules or item overrides, the generator can also use the committed registry snapshot without launching the game:

```powershell
py tools/build_item_values.py --registry docs/value-registry.json
py -m unittest discover -s tools -p "test_item_values.py"
```

Verify the generated values against the real mods, including upgrade behavior with an existing config:

```powershell
py tools/scan_item_values.py --legacy-config run/config/item_sink-common.toml
.\gradlew.bat --no-configuration-cache '-Pneo_version=21.1.255' '-PvalidateValues=true' -I gradle/value-scan.init.gradle runValueScan
.\gradlew.bat build
```

The checks verify the generated original and expanded prices, unchanged orestone prices, representative material and food relationships, progression price differences, the single JEI overview and its currency order, tier processing settings, explicit overrides, exclusions, opt-out, currency guards, and cached lookups. Historical Java default lists remain migration fingerprints; the generated tags define current prices. A successful runtime audit prints both `ITEM_SINK_VALUE_CHECKS_PASSED` and `ITEM_SINK_SCAN_COMPLETE`; check those markers as well as Gradle's exit status because Minecraft startup errors can otherwise return a successful Gradle task.
