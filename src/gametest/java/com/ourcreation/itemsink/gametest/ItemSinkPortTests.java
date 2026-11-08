package com.ourcreation.itemsink.gametest;

import com.mojang.authlib.GameProfile;
import com.ourcreation.itemsink.Config;
import com.ourcreation.itemsink.ItemSink;
import com.ourcreation.itemsink.block.entity.ItemSinkBlockEntity;
import com.ourcreation.itemsink.compat.ItemSinkConversionRecipe;
import com.ourcreation.itemsink.menu.ItemSinkMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.UUID;

/** Runs against the real NeoForge server, Create registry, tags, recipes and config. */
@Mod("item_sink_tests")
@GameTestHolder("item_sink_tests")
@PrefixGameTestTemplate(false)
public class ItemSinkPortTests {
    @GameTest(template = "empty")
    public static void tagsRecipesAndDefaultCurrency(GameTestHelper helper) {
        var flint = Config.exchanges().stream()
                .filter(exchange -> exchange.input().equals(ResourceLocation.parse("minecraft:flint")))
                .findFirst().orElseThrow();
        helper.assertTrue(flint.output().equals(ResourceLocation.parse("numismatics:spur")), "Default currency changed");
        helper.assertTrue(flint.count() == 1 && flint.ticks() == 1200 && flint.energyPerTick() == 360,
                "Default price, processing time or power changed");
        helper.assertTrue(helper.getLevel().getRecipeManager()
                .byKey(ResourceLocation.parse("item_sink:item_sink")).isPresent(), "Crafting recipe did not load");
        if (ModList.get().isLoaded("numismatics")) {
            helper.assertTrue(!flint.createOutput().isEmpty(), "Numismatics currency did not resolve");
            helper.assertTrue(!ItemSinkConversionRecipe.fromConfig().isEmpty(), "JEI conversions did not resolve");
        } else {
            helper.assertTrue(flint.createOutput().isEmpty(), "Missing currency should disable its tier");
            helper.assertTrue(!Config.hasValidExchange(), "Missing default currencies should disable conversions");
            helper.assertTrue(ItemSinkConversionRecipe.fromConfig().isEmpty(), "Missing outputs appeared in JEI");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void survivalMiningAndDrops(GameTestHelper helper) {
        var sink = placeSink(helper);
        var level = helper.getLevel();
        var pos = sink.getBlockPos();
        var state = sink.getBlockState();
        var player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "sink-mining-test"));
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 1.5);
        player.setOnGround(true);

        helper.assertTrue(!state.canHarvestBlock(level, pos, player), "Bare hands can harvest the sink");
        float handProgress = state.getDestroyProgress(player, level, pos);
        for (var pickaxe : List.of(Items.WOODEN_PICKAXE, Items.STONE_PICKAXE, Items.IRON_PICKAXE,
                Items.GOLDEN_PICKAXE, Items.DIAMOND_PICKAXE, Items.NETHERITE_PICKAXE)) {
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(pickaxe));
            helper.assertTrue(state.canHarvestBlock(level, pos, player), "Pickaxe cannot harvest the sink: " + pickaxe);
            helper.assertTrue(state.getDestroyProgress(player, level, pos) > handProgress,
                    "Pickaxe does not speed up mining: " + pickaxe);
        }
        for (var wrongTool : List.of(Items.DIAMOND_AXE, Items.DIAMOND_SHOVEL, Items.DIAMOND_HOE)) {
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(wrongTool));
            helper.assertTrue(!state.canHarvestBlock(level, pos, player), "Wrong tool can harvest the sink: " + wrongTool);
        }

        sink.getInventory().setStackInSlot(0, new ItemStack(Items.FLINT, 3));
        sink.getInventory().setStackInSlot(1, new ItemStack(Items.EMERALD, 2));
        sink.getInventory().setStackInSlot(2, new ItemStack(Items.EMERALD, 5));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_PICKAXE));
        helper.assertTrue(player.gameMode.destroyBlock(pos), "Survival player could not break the sink");
        helper.assertTrue(level.getBlockState(pos).isAir() && level.getBlockEntity(pos) == null,
                "Mined sink or its block entity remained in the world");
        var relativePos = new BlockPos(1, 1, 1);
        helper.assertItemEntityCountIs(ItemSink.ITEM_SINK_ITEM.get(), relativePos, 2, 1);
        helper.assertItemEntityCountIs(Items.FLINT, relativePos, 2, 3);
        helper.assertItemEntityCountIs(Items.EMERALD, relativePos, 2, 7);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void automationAndCapabilityLifecycle(GameTestHelper helper) {
        withEmeraldCurrency(() -> {
            var sink = placeSink(helper);
            var level = helper.getLevel();
            var pos = sink.getBlockPos();
            for (Direction side : Direction.values()) {
                helper.assertTrue(level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side) != null, "Missing item side " + side);
                helper.assertTrue(level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, side) != null, "Missing energy side " + side);
            }
            var inventory = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, (Direction) null);
            helper.assertTrue(inventory.insertItem(0, new ItemStack(Items.FLINT, 2), false).isEmpty(), "Valid input rejected");
            helper.assertTrue(inventory.extractItem(0, 1, false).isEmpty(), "Automation extracted input");
            helper.assertTrue(inventory.insertItem(1, new ItemStack(Items.FLINT), false).getCount() == 1, "Automation inserted output");
            sink.getInventory().setStackInSlot(1, new ItemStack(Items.EMERALD, 2));
            helper.assertTrue(inventory.extractItem(1, 1, false).is(Items.EMERALD), "Automation could not extract currency");
            level.removeBlock(pos, false);
            helper.assertTrue(level.getCapability(Capabilities.ItemHandler.BLOCK, pos, (Direction) null) == null,
                    "Removed block retained its item capability");
            helper.assertTrue(level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, (Direction) null) == null,
                    "Removed block retained its energy capability");
            helper.setBlock(new BlockPos(1, 1, 1), ItemSink.ITEM_SINK.get());
            helper.assertTrue(level.getCapability(Capabilities.ItemHandler.BLOCK, pos, (Direction) null) != null,
                    "Replacement block did not expose an item capability");
        });
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void poweredConversionAndBlockedOutputs(GameTestHelper helper) {
        withEmeraldCurrency(() -> {
            var sink = placeSink(helper);
            sink.getInventory().setStackInSlot(0, new ItemStack(Items.FLINT, 2));
            tick(helper, sink);
            helper.assertTrue(sink.getData().get(0) == 0, "Unpowered machine advanced");
            for (int i = 0; i < 1200; i++) {
                sink.getEnergyStorage().receiveEnergy(10000, false);
                int before = sink.getEnergyStorage().getEnergyStored();
                tick(helper, sink);
                helper.assertTrue(before - sink.getEnergyStorage().getEnergyStored() == 360, "Wrong FE consumption");
            }
            helper.assertTrue(sink.getInventory().getStackInSlot(0).getCount() == 1, "Conversion consumed the wrong input count");
            helper.assertTrue(sink.getInventory().getStackInSlot(1).is(Items.EMERALD)
                    && sink.getInventory().getStackInSlot(1).getCount() == 1, "Conversion produced the wrong currency");
            helper.assertTrue(sink.getData().get(0) == 0, "Progress did not reset after conversion");
            sink.getInventory().setStackInSlot(1, new ItemStack(Items.EMERALD, 64));
            sink.getInventory().setStackInSlot(2, new ItemStack(Items.EMERALD, 64));
            int before = sink.getEnergyStorage().getEnergyStored();
            tick(helper, sink);
            helper.assertTrue(sink.getEnergyStorage().getEnergyStored() == before && sink.getData().get(0) == 0,
                    "Full outputs consumed power or advanced progress");
        });
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void inventoryEnergyAndProgressSurviveSave(GameTestHelper helper) {
        withEmeraldCurrency(() -> {
            var sink = placeSink(helper);
            sink.getInventory().setStackInSlot(0, new ItemStack(Items.FLINT, 3));
            sink.getInventory().setStackInSlot(2, new ItemStack(Items.EMERALD, 7));
            sink.getEnergyStorage().receiveEnergy(10000, false);
            tick(helper, sink);
            CompoundTag saved = sink.saveWithoutMetadata(helper.getLevel().registryAccess());
            var restored = new ItemSinkBlockEntity(sink.getBlockPos(), sink.getBlockState());
            restored.loadWithComponents(saved, helper.getLevel().registryAccess());
            helper.assertTrue(restored.getInventory().getStackInSlot(0).getCount() == 3, "Saved input lost");
            helper.assertTrue(restored.getInventory().getStackInSlot(2).getCount() == 7, "Saved currency lost");
            helper.assertTrue(restored.getEnergyStorage().getEnergyStored() == 9640, "Saved energy lost");
            helper.assertTrue(restored.getData().get(0) == 1 && restored.getData().get(1) == 1200, "Saved progress lost");
        });
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void menuShiftClickAndDistance(GameTestHelper helper) {
        withEmeraldCurrency(() -> {
            var sink = placeSink(helper);
            var player = helper.makeMockPlayer(GameType.SURVIVAL);
            var pos = sink.getBlockPos();
            player.setPos(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            player.getInventory().setItem(9, new ItemStack(Items.FLINT, 2));
            var menu = new ItemSinkMenu(1, player.getInventory(), sink);
            helper.assertTrue(menu.slots.size() == 39 && menu.stillValid(player), "Menu layout or access invalid");
            menu.quickMoveStack(player, 3);
            helper.assertTrue(sink.getInventory().getStackInSlot(0).getCount() == 2, "Shift click did not insert valid input");
            sink.getInventory().setStackInSlot(1, new ItemStack(Items.EMERALD, 4));
            menu.quickMoveStack(player, 1);
            helper.assertTrue(sink.getInventory().getStackInSlot(1).isEmpty(), "Shift click did not extract currency");
            player.setPos(pos.getX() + 20, pos.getY(), pos.getZ());
            helper.assertTrue(!menu.stillValid(player), "Distant player retained menu access");
        });
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void exclusionsOverridesAndCurrencyLoopProtection(GameTestHelper helper) {
        var exclusions = Config.EXCLUDED_ITEMS.get();
        var highTier = Config.VALUE_64_ITEMS.get();
        var lowTier = Config.VALUE_1_ITEMS.get();
        try {
            withEmeraldCurrency(() -> {
                Config.EXCLUDED_ITEMS.set(List.of("minecraft:flint"));
                Config.invalidateExchanges();
                helper.assertTrue(Config.findExchange(new ItemStack(Items.FLINT)).isEmpty(), "Excluded input accepted");
                Config.EXCLUDED_ITEMS.set(exclusions);
                Config.VALUE_64_ITEMS.set(List.of("minecraft:flint"));
                Config.invalidateExchanges();
                var override = Config.exchanges().stream().filter(exchange -> exchange.input()
                        .equals(ResourceLocation.parse("minecraft:flint"))).findFirst().orElseThrow();
                helper.assertTrue(override.output().equals(ResourceLocation.parse("numismatics:cog")), "Explicit override lost to tag");
                Config.VALUE_1_ITEMS.set(List.of("#minecraft:coals"));
                currency().set("minecraft:coal");
                Config.invalidateExchanges();
                helper.assertTrue(Config.findExchange(new ItemStack(Items.COAL)).isEmpty(), "Tag allowed a currency loop");
            });
        } finally {
            Config.EXCLUDED_ITEMS.set(exclusions);
            Config.VALUE_64_ITEMS.set(highTier);
            Config.VALUE_1_ITEMS.set(lowTier);
            Config.invalidateExchanges();
        }
        helper.succeed();
    }

    private static ModConfigSpec.ConfigValue<String> currency() {
        return Config.SPEC.getValues().get(List.of("item_sink", "values", "value1", "outputItem"));
    }

    private static void withEmeraldCurrency(Runnable check) {
        var output = currency();
        String previous = output.get();
        try {
            output.set("minecraft:emerald");
            Config.invalidateExchanges();
            check.run();
        } finally {
            output.set(previous);
            Config.invalidateExchanges();
        }
    }

    private static ItemSinkBlockEntity placeSink(GameTestHelper helper) {
        var relativePos = new BlockPos(1, 1, 1);
        helper.setBlock(relativePos, ItemSink.ITEM_SINK.get());
        return (ItemSinkBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(relativePos));
    }

    private static void tick(GameTestHelper helper, ItemSinkBlockEntity sink) {
        var level = helper.getLevel();
        var pos = sink.getBlockPos();
        ItemSinkBlockEntity.serverTick(level, pos, level.getBlockState(pos), sink);
    }
}
