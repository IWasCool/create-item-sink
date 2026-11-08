package com.ourcreation.itemsink;

import com.ourcreation.itemsink.client.ItemSinkScreen;
import com.ourcreation.itemsink.client.ponder.ItemSinkPonderPlugin;
import net.createmod.ponder.foundation.PonderIndex;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = ItemSink.MODID, dist = Dist.CLIENT)
public class ItemSinkClient {
    public ItemSinkClient(IEventBus modEventBus, ModContainer container) {
        modEventBus.addListener(this::setup);
        modEventBus.addListener(this::registerScreens);
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    private void setup(FMLClientSetupEvent event) {
        PonderIndex.addPlugin(new ItemSinkPonderPlugin());
    }

    private void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ItemSink.ITEM_SINK_MENU.get(), ItemSinkScreen::new);
    }
}
