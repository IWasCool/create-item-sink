package com.ourcreation.itemsink;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.IntValue ENERGY_CAPACITY;
    public static final ForgeConfigSpec.IntValue MAX_ENERGY_INPUT;
    public static final ForgeConfigSpec.BooleanValue REQUIRE_ENERGY;
    public static final ForgeConfigSpec.BooleanValue INCLUDE_CREATECORE_VALUES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> EXCLUDED_ITEMS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> VALUE_1_ITEMS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> VALUE_2_ITEMS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> VALUE_3_ITEMS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> VALUE_4_ITEMS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> VALUE_5_ITEMS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> VALUE_6_ITEMS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> VALUE_7_ITEMS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> VALUE_8_ITEMS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> VALUE_16_ITEMS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> VALUE_32_ITEMS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> VALUE_48_ITEMS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> VALUE_64_ITEMS;
    private static final List<ValueTier> VALUE_TIERS;
    private static volatile ExchangeIndex exchangeIndex;

    static {
        BUILDER.translation("item_sink.configuration.item_sink").push("item_sink");

        ENERGY_CAPACITY = BUILDER
                .translation("item_sink.configuration.item_sink.energyCapacity")
                .comment("Internal FE capacity for the Item Sink.")
                .defineInRange("energyCapacity", 100000, 0, Integer.MAX_VALUE);

        MAX_ENERGY_INPUT = BUILDER
                .translation("item_sink.configuration.item_sink.maxEnergyInput")
                .comment("Maximum FE accepted per tick from external energy handlers.")
                .defineInRange("maxEnergyInput", 10000, 0, Integer.MAX_VALUE);

        REQUIRE_ENERGY = BUILDER
                .translation("item_sink.configuration.item_sink.requireEnergy")
                .comment("If false, exchanges run without consuming FE.")
                .define("requireEnergy", true);

        BUILDER.translation("item_sink.configuration.item_sink.values").push("values");
        INCLUDE_CREATECORE_VALUES = BUILDER
                .translation("item_sink.configuration.item_sink.values.includeCreateCoreValues")
                .comment("Include bundled values for the CreateCore mod collection.",
                        "Your explicit tier lists take priority. Existing configs receive the additions automatically.")
                .define("includeCreateCoreValues", true);
        EXCLUDED_ITEMS = BUILDER
                .translation("item_sink.configuration.item_sink.values.excludedItems")
                .comment("Item ids that must never be accepted, including bundled defaults.",
                        "To move a bundled item to a different tier, add it to that tier's item list instead.")
                .defineListAllowEmpty("excludedItems", List.<String>of(), value -> value instanceof String);
        VALUE_1_ITEMS = defineValueItems("value1Items", "Items in the value 1 exchange tier.", List.of("#item_sink:value_1"));
        VALUE_2_ITEMS = defineValueItems("value2Items", "Items in the value 2 exchange tier.", List.of("#item_sink:value_2"));
        VALUE_3_ITEMS = defineValueItems("value3Items", "Items in the value 3 exchange tier.", List.of("#item_sink:value_3"));
        VALUE_4_ITEMS = defineValueItems("value4Items", "Items in the value 4 exchange tier.", List.of("#item_sink:value_4"));
        VALUE_5_ITEMS = defineValueItems("value5Items", "Items in the value 5 exchange tier.", List.of("#item_sink:value_5"));
        VALUE_6_ITEMS = defineValueItems("value6Items", "Items in the value 6 exchange tier.", List.of("#item_sink:value_6"));
        VALUE_7_ITEMS = defineValueItems("value7Items", "Items in the value 7 exchange tier.", List.of("#item_sink:value_7"));
        VALUE_8_ITEMS = defineValueItems("value8Items", "Items in the value 8 exchange tier.", List.of("#item_sink:value_8"));
        VALUE_16_ITEMS = defineValueItems("value16Items", "Items in the value 16 exchange tier.", List.of("#item_sink:value_16"));
        VALUE_32_ITEMS = defineValueItems("value32Items", "Items in the value 32 exchange tier.", List.of("#item_sink:value_32"));
        VALUE_48_ITEMS = defineValueItems("value48Items", "Items in the value 48 exchange tier.", List.of("#item_sink:value_48"));
        VALUE_64_ITEMS = defineValueItems("value64Items", "Items in the value 64 exchange tier.", List.of("#item_sink:value_64"));
        VALUE_TIERS = List.of(
                defineValueTier("value1", VALUE_1_ITEMS, "numismatics:spur", 1, 1200, 360),
                defineValueTier("value2", VALUE_2_ITEMS, "numismatics:spur", 2, 1100, 360),
                defineValueTier("value3", VALUE_3_ITEMS, "numismatics:spur", 3, 1000, 360),
                defineValueTier("value4", VALUE_4_ITEMS, "numismatics:spur", 4, 900, 360),
                defineValueTier("value5", VALUE_5_ITEMS, "numismatics:spur", 5, 800, 360),
                defineValueTier("value6", VALUE_6_ITEMS, "numismatics:spur", 6, 700, 360),
                defineValueTier("value7", VALUE_7_ITEMS, "numismatics:spur", 7, 600, 360),
                defineValueTier("value8", VALUE_8_ITEMS, "numismatics:bevel", 1, 500, 360),
                defineValueTier("value16", VALUE_16_ITEMS, "numismatics:sprocket", 1, 400, 360),
                defineValueTier("value32", VALUE_32_ITEMS, "numismatics:sprocket", 2, 300, 360),
                defineValueTier("value48", VALUE_48_ITEMS, "numismatics:sprocket", 3, 200, 360),
                defineValueTier("value64", VALUE_64_ITEMS, "numismatics:cog", 1, 100, 360)
        );
        BUILDER.pop();

        BUILDER.pop();
    }

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private static ForgeConfigSpec.ConfigValue<List<? extends String>> defineValueItems(String name, String comment, List<String> defaults) {
        return BUILDER
                .translation("item_sink.configuration.item_sink.values." + name)
                .comment(comment, "Use #item_tag or an item id. The default tag bundles this tier's items.",
                        "Example: " + name + " = [\"#item_sink:" + name.replace("Items", "").replace("value", "value_") + "\", \"minecraft:stick\", \"#forge:ingots/tin\"].",
                        "Individual item ids override tags. Extend the default tag with a datapack to add more items.", "These inputs share the output settings from the matching value tier below.")
                .defineListAllowEmpty(name, defaults, value -> value instanceof String);
    }

    private static ValueTier defineValueTier(String name, ForgeConfigSpec.ConfigValue<List<? extends String>> items, String defaultOutput, int defaultCount, int defaultTicks, int defaultEnergyPerTick) {
        BUILDER.translation("item_sink.configuration.item_sink.values." + name).push(name);
        ForgeConfigSpec.ConfigValue<String> output = BUILDER
                .translation("item_sink.configuration.item_sink.values.outputItem")
                .comment("Currency item produced by this tier.", "Use any item id, for example minecraft:emerald or numismatics:spur.", "If this item is missing, recipes in this tier are disabled until the config is changed or the item-providing mod is installed.")
                .define("outputItem", defaultOutput, value -> value instanceof String string && ResourceLocation.tryParse(string) != null);
        ForgeConfigSpec.IntValue count = BUILDER
                .translation("item_sink.configuration.item_sink.values.outputCount")
                .comment("Number of output items produced per input item in this tier.")
                .defineInRange("outputCount", defaultCount, 1, 64);
        ForgeConfigSpec.IntValue ticks = BUILDER
                .translation("item_sink.configuration.item_sink.values.ticks")
                .comment("Processing time in ticks for this tier. 20 ticks = 1 second.")
                .defineInRange("ticks", defaultTicks, 1, Integer.MAX_VALUE);
        ForgeConfigSpec.IntValue energyPerTick = BUILDER
                .translation("item_sink.configuration.item_sink.values.energyPerTick")
                .comment("FE consumed per tick while processing this tier when requireEnergy is true.")
                .defineInRange("energyPerTick", defaultEnergyPerTick, 0, Integer.MAX_VALUE);
        BUILDER.pop();
        return new ValueTier(items, output, count, ticks, energyPerTick);
    }

    /** Compact unchanged old defaults while preserving every customized tier list. */
    static boolean compactLegacyDefaults() {
        List<List<String>> legacy = List.of(ValueDefaults.VALUE_1, ValueDefaults.VALUE_2,
                ValueDefaults.VALUE_3, ValueDefaults.VALUE_4, ValueDefaults.VALUE_5, ValueDefaults.VALUE_6,
                ValueDefaults.VALUE_7, ValueDefaults.VALUE_8, ValueDefaults.VALUE_16, ValueDefaults.VALUE_32,
                ValueDefaults.VALUE_48, ValueDefaults.VALUE_64);
        boolean changed = false;
        for (int i = 0; i < VALUE_TIERS.size(); i++) {
            var items = VALUE_TIERS.get(i).items();
            if (items.get().equals(legacy.get(i))) {
                items.set(items.getDefault());
                changed = true;
            } else {
                List<String> renamed = items.get().stream()
                        .map(entry -> entry.startsWith("#create_item_sink:")
                                ? "#item_sink:" + entry.substring("#create_item_sink:".length()) : entry)
                        .toList();
                if (!renamed.equals(items.get())) {
                    items.set(renamed);
                    changed = true;
                }
            }
        }
        return changed;
    }

    public static boolean hasValidExchange() {
        for (Exchange exchange : exchanges()) {
            if (BuiltInRegistries.ITEM.getOptional(exchange.input()).isPresent() && !exchange.createOutput().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public static Optional<Exchange> findExchange(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }

        Exchange exchange = index().byInput().get(BuiltInRegistries.ITEM.getKey(stack.getItem()));
        return exchange != null && !exchange.createOutput().isEmpty() ? Optional.of(exchange) : Optional.empty();
    }

    public static List<Exchange> exchanges() {
        return index().exchanges();
    }

    /** Keep overview currency icons in configured tier order. */
    public static List<ResourceLocation> configuredCurrencies() {
        return VALUE_TIERS.stream()
                .map(tier -> ResourceLocation.tryParse(tier.output().get()))
                .filter(output -> output != null)
                .distinct()
                .toList();
    }

    public static synchronized void invalidateExchanges() {
        exchangeIndex = null;
    }

    private static ExchangeIndex index() {
        ExchangeIndex current = exchangeIndex;
        if (current == null) {
            synchronized (Config.class) {
                current = exchangeIndex;
                if (current == null) {
                    current = buildIndex();
                    exchangeIndex = current;
                }
            }
        }
        return current;
    }

    private static ExchangeIndex buildIndex() {
        Map<ResourceLocation, Exchange> parsed = new LinkedHashMap<>();
        var excluded = new HashSet<String>(EXCLUDED_ITEMS.get());
        var outputs = new HashSet<String>();
        for (ValueTier tier : VALUE_TIERS) {
            outputs.add(tier.output().get());
        }
        // Explicit item ids override tag membership, even in a later tier.
        for (ValueTier tier : VALUE_TIERS) {
            for (String entry : tier.items().get()) {
                if (!entry.startsWith("#") && !excluded.contains(entry)) {
                    addExchange(parsed, entry, tier);
                }
            }
        }
        for (ValueTier tier : VALUE_TIERS) {
            for (String entry : tier.items().get()) {
                if (entry.startsWith("#")) {
                    String tagId = entry.substring(1);
                    // Preserve the existing opt-out for the bundled collection.
                    if (!INCLUDE_CREATECORE_VALUES.get() && tagId.matches("item_sink:value_[0-9]+")) {
                        tagId = tagId.replace(":value_", ":original/value_");
                    }
                    addTagExchanges(parsed, tagId, tier, excluded, outputs);
                }
            }
        }
        // Existing customized configs still receive bundled additions without being overwritten.
        if (INCLUDE_CREATECORE_VALUES.get()) {
            int[] values = {1, 2, 3, 4, 5, 6, 7, 8, 16, 32, 48, 64};
            for (int i = 0; i < VALUE_TIERS.size(); i++) {
                addTagExchanges(parsed, "item_sink:createcore/value_" + values[i],
                        VALUE_TIERS.get(i), excluded, outputs);
            }
        }
        return new ExchangeIndex(List.copyOf(parsed.values()), Map.copyOf(parsed));
    }

    private static void addTagExchanges(Map<ResourceLocation, Exchange> parsed, String tagId,
                                        ValueTier tier, java.util.Set<String> excluded, java.util.Set<String> outputs) {
        ResourceLocation location = ResourceLocation.tryParse(tagId);
        if (location == null) {
            ItemSink.LOGGER.warn("Ignoring invalid Item Sink item tag '{}'", tagId);
            return;
        }
        BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM, location)).ifPresent(holders -> {
            for (var holder : holders) {
                String itemId = BuiltInRegistries.ITEM.getKey(holder.value()).toString();
                if (!excluded.contains(itemId) && !outputs.contains(itemId)) {
                    addExchange(parsed, itemId, tier);
                }
            }
        });
    }

    private static void addExchange(Map<ResourceLocation, Exchange> parsed, String itemId, ValueTier tier) {
        ResourceLocation input = ResourceLocation.tryParse(itemId);
        if (input == null) {
            ItemSink.LOGGER.warn("Ignoring invalid Item Sink value item '{}'", itemId);
            return;
        }
        ResourceLocation output = ResourceLocation.tryParse(tier.output().get());
        if (output == null) {
            ItemSink.LOGGER.warn("Ignoring Item Sink value tier with invalid output item '{}'", tier.output().get());
            return;
        }
        parsed.putIfAbsent(input, new Exchange(input, output, tier.count().get(), tier.ticks().get(), tier.energyPerTick().get()));
    }

    private record ExchangeIndex(List<Exchange> exchanges, Map<ResourceLocation, Exchange> byInput) {}

    public record Exchange(ResourceLocation input, ResourceLocation output, int count, int ticks, int energyPerTick) {
        public boolean matches(ItemStack stack) {
            return BuiltInRegistries.ITEM.getOptional(input).isPresent() && stack.is(BuiltInRegistries.ITEM.get(input));
        }

        public ItemStack createOutput() {
            return BuiltInRegistries.ITEM.getOptional(output)
                    .map(item -> new ItemStack(item, count))
                    .orElse(ItemStack.EMPTY);
        }
    }

    private record ValueTier(
            ForgeConfigSpec.ConfigValue<List<? extends String>> items,
            ForgeConfigSpec.ConfigValue<String> output,
            ForgeConfigSpec.IntValue count,
            ForgeConfigSpec.IntValue ticks,
            ForgeConfigSpec.IntValue energyPerTick
    ) {
    }

}
