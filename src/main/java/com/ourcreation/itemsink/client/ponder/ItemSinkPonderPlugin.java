package com.ourcreation.itemsink.client.ponder;

import com.ourcreation.itemsink.ItemSink;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

public final class ItemSinkPonderPlugin implements PonderPlugin {
    @Override
    public String getModId() {
        return ItemSink.MODID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        helper.forComponents(ItemSink.ITEM_SINK_ITEM.getId())
                .addStoryBoard("item_sink", ItemSinkScenes::basics)
                .addStoryBoard("item_sink_create", ItemSinkScenes::createAutomation);
        if (ItemSinkScenes.hasWireExample()) {
            helper.forComponents(ItemSink.ITEM_SINK_ITEM.getId())
                    .addStoryBoard("item_sink", ItemSinkScenes::power);
        }
    }
}
