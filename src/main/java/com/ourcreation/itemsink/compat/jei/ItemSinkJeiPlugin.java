package com.ourcreation.itemsink.compat.jei;

import com.ourcreation.itemsink.ItemSink;
import com.ourcreation.itemsink.compat.ItemSinkConversionRecipe;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

@JeiPlugin
public class ItemSinkJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(ItemSink.MODID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new ItemSinkJeiCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        List<ItemSinkConversionRecipe> recipes = ItemSinkConversionRecipe.fromConfig();
        ItemSink.LOGGER.info("Registering {} Item Sink conversion overview with JEI", recipes.size());
        registration.addRecipes(ItemSinkJeiCategory.TYPE, recipes);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(ItemSink.ITEM_SINK_ITEM.get(), ItemSinkJeiCategory.TYPE);
    }
}
