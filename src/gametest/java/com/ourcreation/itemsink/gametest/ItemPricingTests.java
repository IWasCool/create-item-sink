package com.ourcreation.itemsink.gametest;

import com.ourcreation.itemsink.Config;
import com.ourcreation.itemsink.ItemSink;
import com.ourcreation.itemsink.block.entity.ItemSinkBlockEntity;
import com.ourcreation.itemsink.compat.ItemSinkConversionRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

@GameTestHolder("item_sink_tests")
@PrefixGameTestTemplate(false)
public class ItemPricingTests {
    @GameTest(template = "empty")
    public static void originalCataloguePricesStayUnchanged(GameTestHelper helper) throws Exception {
        int[] values = {1, 2, 3, 4, 5, 6, 7, 8, 16, 32, 48, 64};
        int[] counts = {1, 2, 3, 4, 5, 6, 7, 1, 1, 2, 3, 1};
        int checked = 0;
        // Snapshot of the effective original prices, independent of the pricing implementation.
        try (var reader = new BufferedReader(new InputStreamReader(ItemPricingTests.class
                .getResourceAsStream("/legacy-prices.csv"), StandardCharsets.UTF_8))) {
            for (String line : reader.lines().toList()) {
                String[] fields = line.split(",");
                ResourceLocation input = ResourceLocation.parse(fields[0]);
                if (!BuiltInRegistries.ITEM.containsKey(input)) {
                    continue;
                }
                int value = Integer.parseInt(fields[1]);
                int tier = 0;
                while (values[tier] != value) {
                    tier++;
                }
                var actual = Config.exchanges().stream().filter(exchange -> exchange.input().equals(input))
                        .findFirst().orElseThrow();
                String coin = tier < 7 ? "spur" : tier == 7 ? "bevel" : tier < 11 ? "sprocket" : "cog";
                helper.assertTrue(actual.output().equals(ResourceLocation.parse("numismatics:" + coin))
                                && actual.count() == counts[tier] && actual.ticks() == 1200 - tier * 100
                                && actual.energyPerTick() == 360,
                        "Original price changed for " + input);
                checked++;
            }
        }
        helper.assertTrue(checked > 100, "Catalogue regression test checked too few installed items");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void orderedSelectorsAndCustomCurrency(GameTestHelper helper) {
        withSettings(() -> {
            Config.PRICING_RULES.set(List.of(
                    " minecraft:diamond_pickaxe -> minecraft:emerald, 7, 2, 10 ",
                    "contains:diamond -> minecraft:emerald, 3, 4, 11",
                    "contains:IRON INGOT -> minecraft:emerald, 4, 5, 12",
                    "#minecraft:coals -> minecraft:emerald, 5, 6, 13",
                    "minecraft:*_planks -> minecraft:emerald, 6, 7, 14",
                    "*_wool -> minecraft:emerald, 8, 8, 15",
                    "namespace:create -> minecraft:emerald, 9, 9, 16",
                    "minecraft:gold?n_apple -> minecraft:emerald, 10, 10, 17"));
            Config.invalidateExchanges();
            assertPayout(helper, Items.DIAMOND_PICKAXE, 7, 2, 10);
            assertPayout(helper, Items.DIAMOND, 3, 4, 11);
            assertPayout(helper, Items.IRON_INGOT, 4, 5, 12);
            assertPayout(helper, Items.CHARCOAL, 5, 6, 13);
            assertPayout(helper, Items.OAK_PLANKS, 6, 7, 14);
            assertPayout(helper, Items.RED_WOOL, 8, 8, 15);
            assertPayout(helper, BuiltInRegistries.ITEM.get(ResourceLocation.parse("create:andesite_alloy")), 9, 9, 16);
            assertPayout(helper, Items.GOLDEN_APPLE, 10, 10, 17);
            var renamed = new ItemStack(Items.FLINT);
            renamed.setHoverName(Component.literal("diamond"));
            var original = Config.exchanges().stream().filter(exchange -> exchange.matches(renamed)).findFirst().orElseThrow();
            helper.assertTrue(original.count() == 1 && original.ticks() == 1200, "Renaming an item changed its price");
            var overview = ItemSinkConversionRecipe.fromConfig();
            helper.assertTrue(overview.size() == 1 && overview.get(0).outputs().stream().anyMatch(stack -> stack.is(Items.EMERALD)),
                    "Direct-rule currency was missing from the JEI overview");

            Config.PRICING_RULES.set(List.of("minecraft:diamond -> minecraft:emerald, 12, 1, 0"));
            Config.invalidateExchanges();
            assertPayout(helper, Items.DIAMOND, 12, 1, 0);
        });
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void fallbackAndReplacingBundledPrices(GameTestHelper helper) {
        withSettings(() -> {
            Config.INCLUDE_BUNDLED_VALUES.set(false);
            currency().set("minecraft:emerald");
            Config.VALUE_1_ITEMS.set(List.of("#item_sink:value_1", "#minecraft:coals", "minecraft:stick"));
            Config.PRICING_RULES.set(List.of("contains:diamond -> value1"));
            Config.invalidateExchanges();
            helper.assertTrue(Config.findExchange(new ItemStack(Items.FLINT)).isEmpty(), "Disabled bundle still supplied a price");
            assertPayout(helper, Items.COAL, 1, 1200, 360);
            assertPayout(helper, Items.STICK, 1, 1200, 360);
            assertPayout(helper, Items.DIAMOND, 1, 1200, 360);

            Config.FALLBACK_VALUE.set("minecraft:emerald, 13, 7, 0");
            Config.invalidateExchanges();
            assertPayout(helper, Items.FLINT, 13, 7, 0);
            assertPayout(helper, Items.COAL, 1, 1200, 360);
            assertPayout(helper, Items.DIAMOND, 1, 1200, 360);
            Config.INCLUDE_BUNDLED_VALUES.set(true);
            Config.invalidateExchanges();
            assertPayout(helper, Items.FLINT, 1, 1200, 360);

            Config.FALLBACK_VALUE.set("value1");
            Config.INCLUDE_BUNDLED_VALUES.set(false);
            Config.invalidateExchanges();
            assertPayout(helper, Items.FLINT, 1, 1200, 360);
        });
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void exclusionsAndCurrenciesAlwaysWin(GameTestHelper helper) {
        withSettings(() -> {
            Config.PRICING_RULES.set(List.of("minecraft:emerald -> minecraft:gold_nugget, 2, 1, 0",
                    "* -> minecraft:emerald, 4, 1, 0"));
            Config.FALLBACK_VALUE.set("minecraft:gold_nugget, 1, 1, 0");
            Config.VALUE_1_ITEMS.set(List.of("minecraft:emerald", "minecraft:gold_nugget", "minecraft:coal"));
            Config.EXCLUDED_ITEMS.set(List.of("#minecraft:coals", "contains:iron ingot", "minecraft:*_planks", "namespace:create"));
            Config.invalidateExchanges();
            for (Item item : List.of(Items.COAL, Items.CHARCOAL, Items.IRON_INGOT, Items.OAK_PLANKS,
                    Items.EMERALD, Items.GOLD_NUGGET,
                    BuiltInRegistries.ITEM.get(ResourceLocation.parse("create:andesite_alloy")))) {
                helper.assertTrue(Config.findExchange(new ItemStack(item)).isEmpty(), "Blocked input accepted: " + item);
            }
            helper.assertTrue(Config.exchanges().stream().noneMatch(exchange -> exchange.input().equals(ResourceLocation.parse("minecraft:air"))),
                    "Catch-all rule priced air");
            assertPayout(helper, Items.FLINT, 4, 1, 0);
        });
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void malformedAndMissingOutputRules(GameTestHelper helper) {
        withSettings(() -> {
            Config.PRICING_RULES.set(List.of(
                    "broken rule", "contains: -> value1", "namespace: -> value1", "# -> value1",
                    "minecraft:flint -> value999", "minecraft:flint -> minecraft:emerald, 0, 2, 0",
                    "minecraft:flint -> minecraft:emerald, 65, 2, 0", "minecraft:flint -> minecraft:emerald, 1, 0, 0",
                    "minecraft:flint -> minecraft:emerald, 1, 2, -1", "minecraft:flint -> minecraft:emerald, 1, 2, 2147483648",
                    "minecraft:flint -> minecraft:air, 1, 2, 0", "minecraft:flint -> , 1, 2, 0",
                    "minecraft:flint -> minecraft:emerald, 2, 1, 0",
                    "minecraft:diamond -> missing_currency:coin, 1, 2, 0",
                    "contains:diamond -> minecraft:emerald, 3, 1, 0",
                    "#missing_mod:absent_tag -> value1"));
            Config.FALLBACK_VALUE.set("invalid fallback");
            Config.invalidateExchanges();
            assertPayout(helper, Items.FLINT, 2, 1, 0);
            helper.assertTrue(Config.findExchange(new ItemStack(Items.DIAMOND)).isEmpty(),
                    "A missing output fell through to a different price");
            assertPayout(helper, Items.DIAMOND_SWORD, 3, 1, 0);
        });
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void customPayoutProcessesAndSplitsStacks(GameTestHelper helper) {
        withSettings(() -> {
            Config.PRICING_RULES.set(List.of("minecraft:flint -> minecraft:emerald, 64, 2, 17"));
            Config.invalidateExchanges();
            var pos = new BlockPos(1, 1, 1);
            helper.setBlock(pos, ItemSink.ITEM_SINK.get());
            var sink = (ItemSinkBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
            helper.assertTrue(sink.getAutomationInventory().insertItem(0, new ItemStack(Items.FLINT, 2), false).isEmpty(),
                    "Rule-based input rejected by automation");
            sink.getInventory().setStackInSlot(1, new ItemStack(Items.EMERALD, 63));
            sink.getEnergyStorage().receiveEnergy(100, false);
            for (int i = 0; i < 2; i++) {
                ItemSinkBlockEntity.serverTick(helper.getLevel(), sink.getBlockPos(), sink.getBlockState(), sink);
            }
            helper.assertTrue(sink.getEnergyStorage().getEnergyStored() == 66, "Custom energy cost not applied");
            helper.assertTrue(sink.getInventory().getStackInSlot(0).getCount() == 1, "Custom processing time/input count not applied");
            helper.assertTrue(sink.getInventory().getStackInSlot(1).getCount() == 64
                    && sink.getInventory().getStackInSlot(2).is(Items.EMERALD)
                    && sink.getInventory().getStackInSlot(2).getCount() == 63, "Payout was lost between output slots");
            ItemSinkBlockEntity.serverTick(helper.getLevel(), sink.getBlockPos(), sink.getBlockState(), sink);
            helper.assertTrue(sink.getEnergyStorage().getEnergyStored() == 66, "Blocked custom payout consumed energy");
        });
        helper.succeed();
    }

    private static void assertPayout(GameTestHelper helper, Item input, int count, int ticks, int energy) {
        var exchange = Config.findExchange(new ItemStack(input)).orElseThrow();
        helper.assertTrue(exchange.output().equals(ResourceLocation.parse("minecraft:emerald"))
                        && exchange.count() == count && exchange.ticks() == ticks && exchange.energyPerTick() == energy,
                "Unexpected payout for " + BuiltInRegistries.ITEM.getKey(input) + ": " + exchange);
    }

    private static ForgeConfigSpec.ConfigValue<String> currency() {
        return Config.SPEC.getValues().get(List.of("item_sink", "values", "value1", "outputItem"));
    }

    private static void withSettings(Runnable check) {
        var rules = Config.PRICING_RULES.get();
        var fallback = Config.FALLBACK_VALUE.get();
        var bundled = Config.INCLUDE_BUNDLED_VALUES.get();
        var exclusions = Config.EXCLUDED_ITEMS.get();
        var items = Config.VALUE_1_ITEMS.get();
        var output = currency().get();
        try {
            check.run();
        } finally {
            Config.PRICING_RULES.set(rules);
            Config.FALLBACK_VALUE.set(fallback);
            Config.INCLUDE_BUNDLED_VALUES.set(bundled);
            Config.EXCLUDED_ITEMS.set(exclusions);
            Config.VALUE_1_ITEMS.set(items);
            currency().set(output);
            Config.invalidateExchanges();
        }
    }
}
