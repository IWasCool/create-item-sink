package com.ourcreation.itemsink;

import com.mojang.logging.LogUtils;
import com.ourcreation.itemsink.block.ItemSinkBlock;
import com.ourcreation.itemsink.block.entity.ItemSinkBlockEntity;
import com.ourcreation.itemsink.menu.ItemSinkMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

import java.util.Set;

@Mod(ItemSink.MODID)
public class ItemSink {
    public static final String MODID = "item_sink";
    public static final String BLOCK_NAMESPACE = "item_sink";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(BLOCK_NAMESPACE);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BLOCK_NAMESPACE);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, MODID);

    public static final DeferredBlock<ItemSinkBlock> ITEM_SINK = BLOCKS.register("item_sink", ItemSinkBlock::new);
    public static final DeferredItem<BlockItem> ITEM_SINK_ITEM = ITEMS.registerSimpleBlockItem("item_sink", ITEM_SINK);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ItemSinkBlockEntity>> ITEM_SINK_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("item_sink", () -> new BlockEntityType<>(ItemSinkBlockEntity::new, Set.of(ITEM_SINK.get()), null));

    public static final DeferredHolder<MenuType<?>, MenuType<ItemSinkMenu>> ITEM_SINK_MENU =
            MENU_TYPES.register("item_sink", () -> IMenuTypeExtension.create(ItemSinkMenu::new));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ITEM_SINK_TAB = CREATIVE_MODE_TABS.register("item_sink", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.item_sink"))
            .icon(() -> ITEM_SINK_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> output.accept(ITEM_SINK_ITEM.get()))
            .build());

    public static final DeferredHolder<SoundEvent, SoundEvent> ELECTRIC_MOTOR_BUZZ =
            SOUND_EVENTS.register("electric_motor_buzz", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(MODID, "electric_motor_buzz")));

    public ItemSink(IEventBus modEventBus, ModContainer modContainer) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        MENU_TYPES.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        SOUND_EVENTS.register(modEventBus);
        modEventBus.addListener(this::registerCapabilities);
        modEventBus.addListener(this::onConfigLoaded);
        modEventBus.addListener(this::onConfigReloaded);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(this::onTagsUpdated);
        ConfigMigration.migrate(net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get());
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.COMMON_SPEC, "item_sink/common.toml");
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.ITEM_SETTINGS_SPEC, "item_sink/item_settings.toml");
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.ORIGINAL_VALUES_SPEC, "item_sink/original_values.toml");
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.KEYWORD_SPEC, "item_sink/keyword.toml");
    }

    private void onConfigLoaded(ModConfigEvent.Loading event) {
        if (Config.ownsSpec(event.getConfig().getSpec())) {
            if (event.getConfig().getSpec() == Config.ITEM_SETTINGS_SPEC && Config.compactLegacyDefaults()) {
                Config.ITEM_SETTINGS_SPEC.save();
            }
            Config.invalidateExchanges();
        }
    }

    private void onConfigReloaded(ModConfigEvent.Reloading event) {
        if (Config.ownsSpec(event.getConfig().getSpec())) {
            if (event.getConfig().getSpec() == Config.ITEM_SETTINGS_SPEC && Config.compactLegacyDefaults()) {
                Config.ITEM_SETTINGS_SPEC.save();
            }
            Config.invalidateExchanges();
        }
    }

    private void onTagsUpdated(TagsUpdatedEvent event) {
        Config.invalidateExchanges();
    }

    private void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!Config.hasValidExchange()) {
            event.getEntity().sendSystemMessage(Component.translatable(
                    "message.item_sink.no_valid_outputs", "Create: Numismatics",
                    Component.translatable("item_sink.configuration.item_sink.values.outputItem"),
                    "item_sink/item_settings.toml"
            ));
        }
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ITEM_SINK_BLOCK_ENTITY.get(),
                (blockEntity, side) -> blockEntity.getAutomationInventory()
        );
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ITEM_SINK_BLOCK_ENTITY.get(),
                (blockEntity, side) -> blockEntity.getEnergyStorage()
        );
    }
}
