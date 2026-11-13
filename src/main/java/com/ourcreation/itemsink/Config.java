package com.ourcreation.itemsink;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    private static final ModConfigSpec.Builder COMMON_BUILDER = new ModConfigSpec.Builder();
    private static final ModConfigSpec.Builder ORIGINAL_BUILDER = new ModConfigSpec.Builder();
    private static final ModConfigSpec.Builder KEYWORD_BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue ENERGY_CAPACITY;
    public static final ModConfigSpec.IntValue MAX_ENERGY_INPUT;
    public static final ModConfigSpec.BooleanValue REQUIRE_ENERGY;
    public static final ModConfigSpec.BooleanValue INCLUDE_BUNDLED_VALUES;
    public static final ModConfigSpec.BooleanValue INCLUDE_CREATECORE_VALUES;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> PRICING_RULES;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ORIGINAL_PRICING_RULES;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> CREATECORE_PRICING_RULES;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> DEFAULT_KEYWORD_BLACKLIST;
    public static final ModConfigSpec.ConfigValue<String> FALLBACK_VALUE;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> EXCLUDED_ITEMS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> VALUE_1_ITEMS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> VALUE_2_ITEMS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> VALUE_3_ITEMS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> VALUE_4_ITEMS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> VALUE_5_ITEMS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> VALUE_6_ITEMS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> VALUE_7_ITEMS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> VALUE_8_ITEMS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> VALUE_16_ITEMS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> VALUE_32_ITEMS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> VALUE_48_ITEMS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> VALUE_64_ITEMS;
    private static final List<ValueTier> VALUE_TIERS;
    private static volatile ExchangeIndex exchangeIndex;

    static {
        BUILDER.translation("item_sink.configuration.item_sink").push("item_sink");
        COMMON_BUILDER.translation("item_sink.configuration.item_sink").push("item_sink");
        ORIGINAL_BUILDER.translation("item_sink.configuration.item_sink").push("item_sink")
                .translation("item_sink.configuration.item_sink.values").push("values");
        KEYWORD_BUILDER.translation("item_sink.configuration.item_sink").push("item_sink")
                .translation("item_sink.configuration.item_sink.values").push("values");

        ENERGY_CAPACITY = COMMON_BUILDER
                .translation("item_sink.configuration.item_sink.energyCapacity")
                .comment("Internal FE capacity for the Item Sink.")
                .defineInRange("energyCapacity", 100000, 0, Integer.MAX_VALUE);

        MAX_ENERGY_INPUT = COMMON_BUILDER
                .translation("item_sink.configuration.item_sink.maxEnergyInput")
                .comment("Maximum FE accepted per tick from external energy handlers.")
                .defineInRange("maxEnergyInput", 10000, 0, Integer.MAX_VALUE);

        REQUIRE_ENERGY = COMMON_BUILDER
                .translation("item_sink.configuration.item_sink.requireEnergy")
                .comment("If false, exchanges run without consuming FE.")
                .define("requireEnergy", true);

        BUILDER.translation("item_sink.configuration.item_sink.values").push("values");
        PRICING_RULES = BUILDER
                .translation("item_sink.configuration.item_sink.values.pricingRules")
                .comment("Ordered pricing overrides. First matching rule wins, ahead of tier item lists and configured default rules.",
                        "Format: selector -> tier OR selector -> outputItem, outputCount, ticks, energyPerTick.",
                        "Selectors: minecraft:diamond (exact id), #c:ingots (tag), contains:iron ingot (phrase in item path),",
                        "namespace:create (whole mod), *:raw_* (wildcard in full id), *_ore (wildcard in item path). * matches all items.",
                        "Also accepts keyword:iron ingot (whole path phrase), regex:^iron_ingot$ (path regex), idregex:^create:.* (full id regex).",
                        "Regex uses case-insensitive find; anchor with ^ and $ for a complete match. Patterns compile once per config/tag reload.",
                        "Phrases ignore case and treat spaces as underscores. Names, language and stack NBT do not affect matching.",
                        "Tier names: value1, value2, value3, value4, value5, value6, value7, value8, value16, value32, value48, value64.",
                        "Examples: [\"minecraft:diamond -> value16\", \"contains:ingot -> value4\", \"namespace:create -> minecraft:emerald, 2, 200, 360\"].",
                        "Put specific rules before broad ones. An empty list uses the configured default rules. Missing outputs disable matched inputs.",
                        "Output count: 1..64, ticks: 1..2147483647, energyPerTick: 0..2147483647. Numismatics is not required.",
                        "Exclusions and all configured currency outputs are always blocked, including exact-id rules.",
                        "Invalid entries, including invalid regular expressions, are logged and skipped.")
                .defineListAllowEmpty("pricingRules", List.<String>of(), () -> "", value -> value instanceof String);
        FALLBACK_VALUE = BUILDER
                .translation("item_sink.configuration.item_sink.values.fallbackValue")
                .comment("Price for installed items with no rule or tier-list match. Empty means unpriced items are rejected.",
                        "Use a tier name, such as value1, or a payout, such as minecraft:emerald, 1, 1200, 360.",
                        "Unlike a * rule, this never replaces an existing price. Exclusions and currency protection still apply.")
                .define("fallbackValue", "");
        INCLUDE_BUNDLED_VALUES = BUILDER
                .translation("item_sink.configuration.item_sink.values.includeBundledValues")
                .comment("Enable originalPricingRules and createCorePricingRules beneath your custom rules and tier item lists.",
                        "Set false to build your own economy using pricingRules, custom tier item lists and fallbackValue.",
                        "The default economy is entirely in this config; no bundled value tags or item-value JSON files are loaded.")
                .define("includeBundledValues", true);
        INCLUDE_CREATECORE_VALUES = BUILDER
                .translation("item_sink.configuration.item_sink.values.includeCreateCoreValues")
                .comment("Enable createCorePricingRules, including automatic keyword matching for newly installed items.",
                        "Requires includeBundledValues. Explicit pricingRules and tier lists take priority.")
                .define("includeCreateCoreValues", true);
        ORIGINAL_PRICING_RULES = defineDefaultRules(ORIGINAL_BUILDER, "originalPricingRules",
                "Preserved original prices, including the fixed orestone economy. First matching rule wins.",
                ConfigValueDefaults.ORIGINAL_RULES);
        CREATECORE_PRICING_RULES = defineDefaultRules(KEYWORD_BUILDER, "createCorePricingRules",
                "Ordered item-value rules: reviewed exceptions first, then material, form, machine and food keywords.",
                ConfigValueDefaults.CREATECORE_RULES);
        DEFAULT_KEYWORD_BLACKLIST = KEYWORD_BUILDER
                .translation("item_sink.configuration.item_sink.values.keywordBlacklist")
                .comment("Selectors ignored by createCorePricingRules, similar to Thirst's keyword blacklist.",
                        "Does not override your explicit pricingRules, custom tier lists or originalPricingRules.",
                        "Use excludedItems below to block an item from every pricing source.")
                .defineListAllowEmpty("keywordBlacklist", ConfigValueDefaults.KEYWORD_BLACKLIST, () -> "", value -> value instanceof String);
        EXCLUDED_ITEMS = BUILDER
                .translation("item_sink.configuration.item_sink.values.excludedItems")
                .comment("Items that must never be accepted, even when a pricing rule matches.",
                        "Accepts the same selectors as pricingRules, including keyword:, regex:, idregex:, tags and wildcards.",
                        "Example: [\"minecraft:rotten_flesh\", \"contains:creative\", \"namespace:yourmod\"].")
                .defineListAllowEmpty("excludedItems", List.<String>of(), () -> "", value -> value instanceof String);
        VALUE_1_ITEMS = defineValueItems("value1Items", "Items in the value 1 exchange tier.", List.of());
        VALUE_2_ITEMS = defineValueItems("value2Items", "Items in the value 2 exchange tier.", List.of());
        VALUE_3_ITEMS = defineValueItems("value3Items", "Items in the value 3 exchange tier.", List.of());
        VALUE_4_ITEMS = defineValueItems("value4Items", "Items in the value 4 exchange tier.", List.of());
        VALUE_5_ITEMS = defineValueItems("value5Items", "Items in the value 5 exchange tier.", List.of());
        VALUE_6_ITEMS = defineValueItems("value6Items", "Items in the value 6 exchange tier.", List.of());
        VALUE_7_ITEMS = defineValueItems("value7Items", "Items in the value 7 exchange tier.", List.of());
        VALUE_8_ITEMS = defineValueItems("value8Items", "Items in the value 8 exchange tier.", List.of());
        VALUE_16_ITEMS = defineValueItems("value16Items", "Items in the value 16 exchange tier.", List.of());
        VALUE_32_ITEMS = defineValueItems("value32Items", "Items in the value 32 exchange tier.", List.of());
        VALUE_48_ITEMS = defineValueItems("value48Items", "Items in the value 48 exchange tier.", List.of());
        VALUE_64_ITEMS = defineValueItems("value64Items", "Items in the value 64 exchange tier.", List.of());
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
        COMMON_BUILDER.pop();
        ORIGINAL_BUILDER.pop(2);
        KEYWORD_BUILDER.pop(2);
    }

    public static final ModConfigSpec COMMON_SPEC = COMMON_BUILDER.build();
    public static final ModConfigSpec ITEM_SETTINGS_SPEC = BUILDER.build();
    public static final ModConfigSpec ORIGINAL_VALUES_SPEC = ORIGINAL_BUILDER.build();
    public static final ModConfigSpec KEYWORD_SPEC = KEYWORD_BUILDER.build();
    /** Compatibility alias for callers looking up value tier settings. */
    public static final ModConfigSpec SPEC = ITEM_SETTINGS_SPEC;

    public static boolean ownsSpec(Object spec) {
        return spec == COMMON_SPEC || spec == ITEM_SETTINGS_SPEC
                || spec == ORIGINAL_VALUES_SPEC || spec == KEYWORD_SPEC;
    }

    private static ModConfigSpec.ConfigValue<List<? extends String>> defineDefaultRules(ModConfigSpec.Builder builder, String name, String comment, List<String> defaults) {
        return builder.translation("item_sink.configuration.item_sink.values." + name)
                .comment(comment,
                        "Format and selectors are the same as pricingRules. Every default can be edited or removed here.",
                        "Specific rules precede general keywords. Use regex: for item paths and idregex: for namespace-specific exceptions.",
                        "Unmatched items are rejected unless fallbackValue is configured. Missing currencies disable the matched price.")
                .defineListAllowEmpty(name, defaults, () -> "", value -> value instanceof String);
    }

    private static ModConfigSpec.ConfigValue<List<? extends String>> defineValueItems(String name, String comment, List<String> defaults) {
        return BUILDER.translation("item_sink.configuration.item_sink.values." + name)
                .comment(comment,
                        "Custom selectors assigned to this tier, ahead of configured default prices. Empty uses the default rule lists.",
                        "Accepts item ids, #external_tags, keyword:, regex:, idregex:, contains:, namespace: and wildcards.",
                        "Exact item ids override other tier selectors. pricingRules overrides both.",
                        "These inputs share the output settings from the matching value tier below.")
                .defineListAllowEmpty(name, defaults, () -> "", value -> value instanceof String);
    }

    private static ValueTier defineValueTier(String name, ModConfigSpec.ConfigValue<List<? extends String>> items, String defaultOutput, int defaultCount, int defaultTicks, int defaultEnergyPerTick) {
        BUILDER.translation("item_sink.configuration.item_sink.values." + name).push(name);
        ModConfigSpec.ConfigValue<String> output = BUILDER
                .translation("item_sink.configuration.item_sink.values.outputItem")
                .comment("Currency item produced by this tier.", "Use any item id, for example minecraft:emerald or numismatics:spur.", "If this item is missing, recipes in this tier are disabled until the config is changed or the item-providing mod is installed.")
                .define("outputItem", defaultOutput, value -> value instanceof String string && ResourceLocation.tryParse(string) != null);
        ModConfigSpec.IntValue count = BUILDER
                .translation("item_sink.configuration.item_sink.values.outputCount")
                .comment("Number of output items produced per input item in this tier.")
                .defineInRange("outputCount", defaultCount, 1, 64);
        ModConfigSpec.IntValue ticks = BUILDER
                .translation("item_sink.configuration.item_sink.values.ticks")
                .comment("Processing time in ticks for this tier. 20 ticks = 1 second.")
                .defineInRange("ticks", defaultTicks, 1, Integer.MAX_VALUE);
        ModConfigSpec.IntValue energyPerTick = BUILDER
                .translation("item_sink.configuration.item_sink.values.energyPerTick")
                .comment("FE consumed per tick while processing this tier when requireEnergy is true.")
                .defineInRange("energyPerTick", defaultEnergyPerTick, 0, Integer.MAX_VALUE);
        BUILDER.pop();
        return new ValueTier(name, items, output, count, ticks, energyPerTick);
    }

    /** Migrate only former built-in tag selectors; retain custom selectors and tier settings. */
    static boolean compactLegacyDefaults() {
        List<List<String>> legacy = List.of(ValueDefaults.VALUE_1, ValueDefaults.VALUE_2,
                ValueDefaults.VALUE_3, ValueDefaults.VALUE_4, ValueDefaults.VALUE_5, ValueDefaults.VALUE_6,
                ValueDefaults.VALUE_7, ValueDefaults.VALUE_8, ValueDefaults.VALUE_16, ValueDefaults.VALUE_32,
                ValueDefaults.VALUE_48, ValueDefaults.VALUE_64);
        boolean changed = false;
        for (int i = 0; i < VALUE_TIERS.size(); i++) {
            ValueTier tier = VALUE_TIERS.get(i);
            var items = tier.items();
            List<String> migrated = new ArrayList<>();
            if (!items.get().equals(legacy.get(i))) {
                for (String entry : items.get()) {
                    String normalized = entry.trim().replaceFirst("^#create_item_sink:", "#item_sink:");
                    // This was the unchanged default tag; configured defaults now supply its prices.
                    if (normalized.equals("#item_sink:" + tier.name().replace("value", "value_"))) continue;
                    List<String> replacement = normalized.startsWith("#")
                            ? ConfigValueDefaults.legacySelectors(normalized.substring(1)) : null;
                    if (replacement == null) migrated.add(entry);
                    else if (normalized.matches("#item_sink:value_[0-9]+")) {
                        String suffix = normalized.substring("#item_sink:".length());
                        ConfigValueDefaults.legacySelectors("item_sink:original/" + suffix)
                                .forEach(selector -> migrated.add("bundled:" + selector));
                        ConfigValueDefaults.legacySelectors("item_sink:createcore/" + suffix)
                                .forEach(selector -> migrated.add("corebundled:" + selector));
                    } else replacement.forEach(selector -> migrated.add("bundled:" + selector));
                }
            }
            if (!items.get().equals(migrated)) {
                items.set(List.copyOf(migrated));
                changed = true;
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

    /** Keep tier currency order, then include any additional rule/fallback currencies. */
    public static List<ResourceLocation> configuredCurrencies() {
        return index().currencies();
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
        List<PricingRule> rules = parseRules(PRICING_RULES.get(), "pricingRules");
        List<PricingRule> original = INCLUDE_BUNDLED_VALUES.get()
                ? parseRules(ORIGINAL_PRICING_RULES.get(), "originalPricingRules") : List.of();
        List<PricingRule> automatic = INCLUDE_BUNDLED_VALUES.get() && INCLUDE_CREATECORE_VALUES.get()
                ? parseRules(CREATECORE_PRICING_RULES.get(), "createCorePricingRules") : List.of();
        List<Predicate<ResourceLocation>> exclusions = parseSelectors(EXCLUDED_ITEMS.get(), "excludedItems");
        List<Predicate<ResourceLocation>> keywordBlacklist = parseSelectors(DEFAULT_KEYWORD_BLACKLIST.get(), "keywordBlacklist");
        List<PricingRule> exactTierItems = new ArrayList<>();
        List<PricingRule> otherTierItems = new ArrayList<>();
        for (ValueTier tier : VALUE_TIERS) {
            for (String selector : tier.items().get()) {
                try {
                    PricingRule rule = new PricingRule(ItemSelector.parse(selector), payout(tier));
                    (ItemSelector.isExactItemId(selector) ? exactTierItems : otherTierItems).add(rule);
                } catch (IllegalArgumentException error) {
                    ItemSink.LOGGER.warn("Ignoring Item Sink {} item selector '{}': {}", tier.name(), selector, error.getMessage());
                }
            }
        }
        Payout fallback = null;
        if (!FALLBACK_VALUE.get().isBlank()) {
            try {
                fallback = parsePayout(FALLBACK_VALUE.get());
            } catch (IllegalArgumentException error) {
                ItemSink.LOGGER.warn("Ignoring Item Sink fallbackValue '{}': {}", FALLBACK_VALUE.get(), error.getMessage());
            }
        }
        var outputs = new LinkedHashSet<ResourceLocation>();
        for (ValueTier tier : VALUE_TIERS) outputs.add(payout(tier).output());
        for (List<PricingRule> source : List.of(rules, original, automatic)) {
            source.forEach(rule -> outputs.add(rule.payout().output()));
        }
        if (fallback != null) outputs.add(fallback.output());

        Map<ResourceLocation, Exchange> parsed = new LinkedHashMap<>();
        for (ResourceLocation input : BuiltInRegistries.ITEM.keySet()) {
            if (input.equals(ResourceLocation.parse("minecraft:air")) || outputs.contains(input)
                    || exclusions.stream().anyMatch(selector -> selector.test(input))) continue;
            Exchange match = firstMatch(input, rules);
            if (match == null) match = firstMatch(input, exactTierItems);
            if (match == null) match = firstMatch(input, otherTierItems);
            if (match == null) match = firstMatch(input, original);
            if (match == null && keywordBlacklist.stream().noneMatch(selector -> selector.test(input))) {
                match = firstMatch(input, automatic);
            }
            if (match == null && fallback != null) match = fallback.exchange(input);
            // Keep a matched-but-missing currency disabled, rather than falling through to a different price.
            if (match != null) parsed.put(input, match);
        }
        return new ExchangeIndex(List.copyOf(parsed.values()), Map.copyOf(parsed), List.copyOf(outputs));
    }

    private static Exchange firstMatch(ResourceLocation input, List<PricingRule> rules) {
        for (PricingRule rule : rules) {
            if (rule.selector().test(input)) return rule.payout().exchange(input);
        }
        return null;
    }

    private static List<PricingRule> parseRules(List<? extends String> entries, String setting) {
        List<PricingRule> result = new ArrayList<>();
        for (String entry : entries) {
            try {
                String[] parts = entry.split("->", -1);
                if (parts.length != 2) throw new IllegalArgumentException("expected selector -> tier or selector -> outputItem, count, ticks, FE/t");
                result.add(new PricingRule(ItemSelector.parse(parts[0]), parsePayout(parts[1])));
            } catch (IllegalArgumentException error) {
                ItemSink.LOGGER.warn("Ignoring Item Sink {} rule '{}': {}", setting, entry, error.getMessage());
            }
        }
        return List.copyOf(result);
    }

    private static List<Predicate<ResourceLocation>> parseSelectors(List<? extends String> entries, String setting) {
        List<Predicate<ResourceLocation>> result = new ArrayList<>();
        for (String entry : entries) {
            try {
                result.add(ItemSelector.parse(entry));
            } catch (IllegalArgumentException error) {
                ItemSink.LOGGER.warn("Ignoring Item Sink {} selector '{}': {}", setting, entry, error.getMessage());
            }
        }
        return List.copyOf(result);
    }

    private static Payout parsePayout(String text) {
        String value = text.trim();
        for (ValueTier tier : VALUE_TIERS) {
            if (tier.name().equals(value)) {
                return payout(tier);
            }
        }
        String[] fields = value.split(",", -1);
        if (fields.length != 4) {
            throw new IllegalArgumentException("use a tier name or outputItem, outputCount, ticks, energyPerTick");
        }
        ResourceLocation output = fields[0].isBlank() ? null : ResourceLocation.tryParse(fields[0].trim());
        if (output == null || output.equals(ResourceLocation.parse("minecraft:air"))) {
            throw new IllegalArgumentException("outputItem must be a valid non-air item id");
        }
        int count = Integer.parseInt(fields[1].trim());
        int ticks = Integer.parseInt(fields[2].trim());
        int energyPerTick = Integer.parseInt(fields[3].trim());
        if (count < 1 || count > 64 || ticks < 1 || energyPerTick < 0) {
            throw new IllegalArgumentException("outputCount must be 1..64, ticks >= 1 and energyPerTick >= 0");
        }
        return new Payout(output, count, ticks, energyPerTick);
    }

    private static Payout payout(ValueTier tier) {
        return new Payout(ResourceLocation.parse(tier.output().get()), tier.count().get(), tier.ticks().get(), tier.energyPerTick().get());
    }

    private record Payout(ResourceLocation output, int count, int ticks, int energyPerTick) {
        Exchange exchange(ResourceLocation input) {
            return new Exchange(input, output, count, ticks, energyPerTick);
        }
    }

    private record PricingRule(Predicate<ResourceLocation> selector, Payout payout) {}

    private record ExchangeIndex(List<Exchange> exchanges, Map<ResourceLocation, Exchange> byInput,
                                 List<ResourceLocation> currencies) {}

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
            String name,
            ModConfigSpec.ConfigValue<List<? extends String>> items,
            ModConfigSpec.ConfigValue<String> output,
            ModConfigSpec.IntValue count,
            ModConfigSpec.IntValue ticks,
            ModConfigSpec.IntValue energyPerTick
    ) {
    }

}
