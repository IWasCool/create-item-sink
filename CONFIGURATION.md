# Item Sink pricing configuration

Start the game once to generate `config/item_sink-common.toml`. Edit the existing entries in that file; do not add a second `[item_sink.values]` table. Use the same configuration on the server and clients. Restart after editing to refresh all client displays, including JEI.

The default configuration keeps the current item prices, currency quantities, processing times, and FE costs. Your existing customized tier lists and settings remain in use. Numismatics is optional: any installed item can be a currency.

## Quick examples

Under `[item_sink.values]`, change `pricingRules` to add overrides. The first matching rule wins, so put specific exceptions before broader categories:

```toml
[item_sink.values]
pricingRules = [
    "minecraft:diamond -> value16",
    "contains:diamond sword -> value32",
    "#c:ingots/iron -> value4",
    "contains:ingot -> value4",
    "*:raw_* -> value2",
    "namespace:create -> value8"
]
fallbackValue = ""
includeBundledValues = true
```

`value16` refers to the configured **value16 tier**. It uses that tier's currency, quantity, time, and energy settings. It does not necessarily produce 16 currency items. Unmatched items keep their existing prices; items without a price are rejected.

For payouts independent of the tiers, write `outputItem, outputCount, ticks, energyPerTick` after the arrow:

```toml
pricingRules = [
    "minecraft:diamond -> minecraft:emerald, 12, 100, 360",
    "contains:ingot -> minecraft:emerald, 2, 200, 120",
    "#minecraft:logs -> minecraft:gold_nugget, 3, 400, 0"
]
```

This pays 12 emeralds per diamond, 2 per ingot, and 3 gold nuggets per log. Each conversion consumes one input item. Processing time is in ticks (20 ticks = 1 second); energy is FE per tick. Count must be 1–64, ticks must be at least 1, and energy must be nonnegative. `requireEnergy = false` under `[item_sink]` disables FE consumption for all conversions.

Direct payouts let you use any quantity in that range and mix currencies without adding a tier. Replace the output IDs with items from any installed currency mod. Missing output items disable the inputs matched by that rule; they do not fall through to a cheaper rule or bundled price. Invalid rule syntax is logged and skipped.

## Selectors

| Selector | Matches |
| --- | --- |
| `minecraft:diamond` | Exactly that registered item |
| `#c:ingots/iron` | Every item in that item tag |
| `contains:iron ingot` | Item paths containing `iron_ingot` |
| `namespace:create` | All items registered by Create |
| `minecraft:*_planks` | Minecraft IDs ending in `_planks` |
| `*:raw_*` | IDs in any namespace whose path starts with `raw_` |
| `*_ore` | Paths ending in `_ore`, in any namespace |
| `gold?n_apple` | Paths matching `gold`, any one character, then `n_apple` |
| `*` | Every registered item, subject to exclusions and currency protection |

`contains:` phrases ignore case and convert spaces to underscores. They search the **registry path**, such as `iron_ingot`, rather than the mod ID or translated display name. Item IDs, namespaces, tags, and wildcards use lowercase registry names. `*` matches any number of characters; `?` matches one. Tags for missing optional mods simply match nothing.

Prices depend on item type. Renaming items, changing game language, enchantments, durability, and stored contents do not change their price.

## Default prices and fallback prices

The bundled catalogue still supplies the existing prices. Default tier input lists such as `value1Items = ["#item_sink:value_1"]` retain those item groups. Leave these lists alone when adding pricing rules.

`fallbackValue` gives a price only to items without a rule or tier-list match:

```toml
fallbackValue = "value1"
# Or use a direct payout:
# fallbackValue = "minecraft:emerald, 1, 1200, 360"
```

Leave it empty to reject unpriced items. A `*` pricing rule overrides existing prices as well; fallback prices preserve them.

To replace the catalogue with your own economy:

```toml
[item_sink.values]
includeBundledValues = false
pricingRules = [
    "contains:diamond -> minecraft:emerald, 8, 200, 360",
    "contains:ingot -> minecraft:emerald, 2, 400, 360"
]
fallbackValue = "minecraft:emerald, 1, 1200, 360"
excludedItems = ["contains:creative", "minecraft:barrier", "minecraft:command_block"]
```

This disables the built-in `#item_sink:value_*`, `#item_sink:original/value_*`, and `#item_sink:createcore/value_*` groups and their automatic additions. Your explicit tier item IDs and custom tags still work. `includeCreateCoreValues = false` by itself keeps the original catalogue and disables only the additional CreateCore values.

## Change the existing tier currencies

Each existing tier has its own output settings:

```toml
[item_sink.values.value4]
outputItem = "minecraft:emerald"
outputCount = 4
ticks = 900
energyPerTick = 360
```

This applies to the bundled value4 items, custom `value4Items`, and rules targeting `value4`. Available tier names are `value1`, `value2`, `value3`, `value4`, `value5`, `value6`, `value7`, `value8`, `value16`, `value32`, `value48`, and `value64`. Configure every tier you want to use when playing without Numismatics, or use direct payouts and disable the bundled catalogue as shown above.

## Priority and exclusions

1. `excludedItems`, air, and every configured currency output are always blocked.
2. `pricingRules` run in list order; the first match wins.
3. Explicit item IDs in the existing tier input lists override tag prices. If multiple tiers list an ID, the first tier wins.
4. Tier tags and automatic CreateCore additions supply the remaining existing prices.
5. `fallbackValue` applies to otherwise unpriced items.

`excludedItems` accepts the same selectors as pricing rules:

```toml
excludedItems = [
    "minecraft:rotten_flesh",
    "#c:ores",
    "contains:creative",
    "namespace:yourmod",
    "*:infinite_*"
]
```

Currency protection covers outputs configured in any tier, pricing rule, or fallback, even if that output is named explicitly as an input elsewhere. This prevents currency conversion loops. To sell an item that was previously a currency, remove it from all output settings first.

Existing configurations are extended with the new settings automatically. Old `create_item_sink-common.toml` files are copied when no current config exists, and unchanged legacy item lists are compacted into their bundled tags. Config reloads and datapack tag reloads rebuild the cached pricing lookup; matching is not repeated every machine tick. JEI shows all currencies actually available from configured conversions, and the question-mark input tooltip reads **Any Item**.
