package com.ourcreation.itemsink.valuescan;

import com.google.gson.JsonParser;
import com.ourcreation.itemsink.Config;
import com.ourcreation.itemsink.compat.ItemSinkConversionRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Tests use the real loaded item registry and the normal NeoForge config. */
final class ValueExchangeChecks {
    private static Map<String, Config.Exchange> defaultExchanges;

    static void run() throws Exception {
        defaultExchanges = Config.exchanges().stream().collect(Collectors.toMap(
                exchange -> exchange.input().toString(), exchange -> exchange));
        int[] tiers = {1, 2, 3, 4, 5, 6, 7, 8, 16, 32, 48, 64};
        var seen = new HashSet<String>();
        int originalItems = 0;
        int registeredOriginals = 0;
        // Audit remastered original items independently of the compact config lists.
        for (int value : tiers) {
            try (var stream = Config.class.getResourceAsStream(
                    "/data/item_sink/tags/item/original/value_" + value + ".json")) {
                check(stream != null, "Original tier tag missing");
                var tag = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
                for (var element : tag.getAsJsonArray("values")) {
                    String id = element.getAsJsonObject().get("id").getAsString();
                    if (seen.add(id)) {
                        originalItems++;
                        if (BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(id))) {
                            check(price(exchange(id)) == value, "Wrong remastered original value for " + id);
                            registeredOriginals++;
                        }
                    }
                }
            }
        }
        int additions = 0;
        try (var stream = Config.class.getResourceAsStream("/data/item_sink/item_values/createcore.json")) {
            check(stream != null, "Bundled catalogue missing from runtime resources");
            var catalogue = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            for (var tier : catalogue.entrySet()) {
                for (var element : tier.getValue().getAsJsonArray()) {
                    String id = element.getAsString();
                    check(seen.add(id), "Duplicate catalogue item " + id);
                    ResourceLocation key = ResourceLocation.parse(id);
                    check(BuiltInRegistries.ITEM.containsKey(key), "Catalogue contains unregistered item " + id);
                    check(price(exchange(id)) == Integer.parseInt(tier.getKey()), "Wrong catalogue value " + id);
                    check(Config.findExchange(new ItemStack(BuiltInRegistries.ITEM.get(key))).isPresent(), "Input not accepted " + id);
                    additions++;
                }
            }
        }
        check(Config.findExchange(ItemStack.EMPTY).isEmpty(), "Empty stack accepted");
        for (String coin : List.of("spur", "bevel", "sprocket", "cog", "crown", "sun")) {
            check(Config.findExchange(stack("numismatics:" + coin)).isEmpty(), "Currency accepted: " + coin);
        }
        check(Config.findExchange(stack("create:creative_motor")).isEmpty(), "Creative motor accepted");
        check(Config.findExchange(stack("minecraft:barrier")).isEmpty(), "Technical item accepted");
        check(Config.exchanges() == Config.exchanges(), "Exchange catalogue is rebuilt on every lookup");
        var recipes = ItemSinkConversionRecipe.fromConfig();
        check(recipes.size() == 1, "JEI must contain exactly one overview, not one recipe per item");
        check(recipes.getFirst().outputs().stream()
                        .map(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()).toList()
                        .equals(List.of("numismatics:spur", "numismatics:bevel", "numismatics:sprocket", "numismatics:cog")),
                "JEI must cycle through each available currency once, from spur to cog");
        check(recipes.getFirst().outputs().stream().allMatch(stack -> stack.getCount() == 1),
                "Overview icons must not imply a fixed payout for every item");
        var dirt = Config.findExchange(stack("minecraft:dirt")).orElseThrow();
        check(dirt.ticks() == 1200 && dirt.energyPerTick() == 360, "Tier processing or energy defaults changed");
        checkBalance();

        var originalOne = Config.VALUE_1_ITEMS.get();
        Config.VALUE_1_ITEMS.set(List.of("#minecraft:logs"));
        Config.invalidateExchanges();
        check(price(Config.findExchange(stack("minecraft:oak_log")).orElseThrow()) == 1,
                "Custom item tag not resolved");
        Config.VALUE_1_ITEMS.set(originalOne);
        Config.invalidateExchanges();

        var original = Config.VALUE_64_ITEMS.get();
        var custom = new ArrayList<String>(original);
        custom.add("minecraft:dirt");
        Config.VALUE_64_ITEMS.set(custom);
        Config.invalidateExchanges();
        check(price(Config.findExchange(stack("minecraft:dirt")).orElseThrow()) == 64, "Configured price does not override bundled default");
        check(ItemSinkConversionRecipe.fromConfig().getFirst().outputs().stream()
                        .map(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()).toList()
                        .equals(List.of("numismatics:spur", "numismatics:bevel", "numismatics:sprocket", "numismatics:cog")),
                "Explicit input overrides must not reorder the displayed currencies");

        Config.EXCLUDED_ITEMS.set(List.of("minecraft:dirt", "minecraft:iron_ingot"));
        Config.invalidateExchanges();
        check(Config.findExchange(stack("minecraft:dirt")).isEmpty(), "Excluded bundled item accepted");
        check(Config.findExchange(stack("minecraft:iron_ingot")).isEmpty(), "Excluded legacy item accepted");
        Config.EXCLUDED_ITEMS.set(List.of());
        Config.VALUE_64_ITEMS.set(original);
        Config.INCLUDE_CREATECORE_VALUES.set(false);
        Config.invalidateExchanges();
        check(Config.findExchange(stack("minecraft:dirt")).isEmpty(), "Pack values cannot be disabled");
        check(price(Config.findExchange(stack("minecraft:iron_ingot")).orElseThrow())
                        == price(exchange("minecraft:iron_ingot")), "Original value lost when pack values disabled");
        Config.INCLUDE_CREATECORE_VALUES.set(true);
        Config.invalidateExchanges();
        check(price(Config.findExchange(stack("minecraft:dirt")).orElseThrow()) == 1, "Pack defaults not restored after cache invalidation");

        // Never advertise a currency when no inputs can be converted.
        Config.EXCLUDED_ITEMS.set(Config.exchanges().stream().map(exchange -> exchange.input().toString()).toList());
        Config.invalidateExchanges();
        check(ItemSinkConversionRecipe.fromConfig().isEmpty(), "JEI advertised currency with no accepted inputs");
        Config.EXCLUDED_ITEMS.set(List.of());
        Config.invalidateExchanges();
        check(ItemSinkConversionRecipe.fromConfig().size() == 1, "JEI overview did not recover after config reset");
        System.out.println("ITEM_SINK_VALUE_CHECKS_PASSED: " + originalItems + " remastered original values, " + additions
                + " registered additions, single JEI overview, config overrides, exclusions, opt-out, currency guards and cached lookups");
    }

    private static void checkBalance() throws Exception {
        try (var reader = Files.newBufferedReader(Path.of(System.getProperty("item_sink.scan.orestoneValues")))) {
            var orestones = JsonParser.parseReader(reader).getAsJsonObject();
            check(orestones.size() > 0, "Frozen orestone baseline is empty");
            for (var entry : orestones.entrySet()) {
                check(price(exchange(entry.getKey())) == entry.getValue().getAsInt(),
                        "Frozen orestone value changed: " + entry.getKey());
            }
        }
        for (String stone : List.of("asurine", "crimsite", "ochrum", "veridium", "scorchia", "scoria")) {
            check(price(exchange("create:" + stone)) == 8, "Orestone anchor changed: " + stone);
            check(exchange("create:" + stone).ticks() == 500,
                    "Orestone processing time changed: " + stone);
        }
        int diamond = price(exchange("minecraft:diamond"));
        check(diamond == 32, "Diamond anchor changed");
        check(price(exchange("minecraft:emerald")) == 16, "Emerald anchor changed");
        check(price(exchange("minecraft:netherite_ingot")) == 64, "Netherite anchor changed");
        check(exchange("minecraft:diamond").ticks() == 300,
                "Repriced items must use the existing settings of their new tier");
        check(price(exchange("minecraft:oak_boat")) == 4, "Basic boat must use its reviewed value");
        check(price(exchange("minecraft:oak_chest_boat")) == 6, "Chest boat must use its reviewed value");
        check(price(exchange("supplementaries:cannon")) == 8, "Basic cannon must use its reviewed value");
        check(price(exchange("supplementaries:cannon_boat_oak")) == 8,
                "Cannon boat must use its reviewed value");
        check(price(exchange("supplementaries:biomesoplenty/cannon_boat_fir")) == 8,
                "Cross-mod cannon boats must match vanilla wood cannon boats");
        check(price(exchange("supplementaries:cannon_boat_oak")) < diamond,
                "Cannon boat must be worth less than a diamond");
        check(price(exchange("create:potato_cannon")) <= diamond, "Potato cannon must not exceed a diamond");
        check(price(exchange("createbigcannons:cannon_carriage")) <= diamond,
                "Cannon carriage must not exceed a diamond");
        check(price(exchange("createbigcannons:empty_autocannon_cartridge"))
                        < price(exchange("createbigcannons:cannon_carriage")),
                "Empty ammunition must not have a machinery payout");
        check(price(exchange("minecraft:bread")) > 2, "Bread payout must reward food production");
        check(price(exchange("minecraft:cooked_beef")) > 4, "Cooking must have a higher payout");
        check(price(exchange("minecraft:cooked_beef")) > price(exchange("minecraft:beef")),
                "Cooked meat must be worth more than raw meat");
        check(price(exchange("farmersdelight:beef_stew")) > 8, "Prepared meals must receive a higher payout");
        check(price(exchange("farmersdelight:beef_stew")) > price(exchange("minecraft:cooked_beef")),
                "Prepared meals must reward extra preparation");
        check(price(exchange("create_new_world_core:hub_terminal")) == 16, "HUB terminal must use the early electronics tier");
        check(price(exchange("northstar:targeting_computer")) == 48, "Targeting computer must exceed ordinary electronics");
        check(price(exchange("create_new_world_core:quantum_targeting_computer")) == 64,
                "Quantum computer must retain the endgame payout");
        check(price(exchange("create_new_world_core:quantum_targeting_computer"))
                        >= 4 * price(exchange("create_new_world_core:hub_terminal")),
                "Quantum computer must be worth substantially more than a HUB terminal");
        for (String circuit : List.of("refined_radiance_circuit", "shadow_steel_circuit", "platinum_circuit")) {
            check(price(exchange("create_new_world_core:" + circuit))
                            > price(exchange("create_new_world_core:advanced_circuit")),
                    "Late circuit must exceed advanced circuit: " + circuit);
        }
        for (String[] stage : List.of(
                new String[]{"storage_terminal", "wireless_terminal"},
                new String[]{"wireless_terminal", "crafting_terminal"},
                new String[]{"crafting_terminal", "adv_wireless_terminal"})) {
            check(price(exchange("toms_storage:" + stage[0])) < price(exchange("toms_storage:" + stage[1])),
                    "Storage terminal prices must follow recipe complexity");
        }
    }

    private static ItemStack stack(String id) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
    }

    private static Config.Exchange exchange(String id) {
        Config.Exchange result = defaultExchanges.get(id);
        check(result != null, "Missing exchange " + id);
        return result;
    }

    private static int price(Config.Exchange exchange) {
        int factor = switch (exchange.output().getPath()) {
            case "spur" -> 1;
            case "bevel" -> 8;
            case "sprocket" -> 16;
            case "cog" -> 64;
            default -> throw new AssertionError("Unexpected output " + exchange.output());
        };
        return factor * exchange.count();
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
