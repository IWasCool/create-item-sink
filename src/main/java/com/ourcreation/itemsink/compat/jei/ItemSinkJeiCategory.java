package com.ourcreation.itemsink.compat.jei;

import com.ourcreation.itemsink.ItemSink;
import com.ourcreation.itemsink.compat.ItemSinkConversionRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class ItemSinkJeiCategory implements IRecipeCategory<ItemSinkConversionRecipe> {
    public static final RecipeType<ItemSinkConversionRecipe> TYPE = new RecipeType<>(
            ResourceLocation.fromNamespaceAndPath(ItemSink.MODID, "item_sink_conversion"),
            ItemSinkConversionRecipe.class
    );
    private static final int INPUT_X = 7;
    private static final int SLOT_Y = 7;
    private static final int OUTPUT_X = 89;
    private static final int SLOT_SIZE = 18;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable arrow;

    public ItemSinkJeiCategory(IGuiHelper guiHelper) {
        background = guiHelper.createBlankDrawable(116, 32);
        icon = guiHelper.createDrawableItemStack(new ItemStack(ItemSink.ITEM_SINK_ITEM.get()));
        arrow = guiHelper.getRecipeArrow();
    }

    @Override
    public RecipeType<ItemSinkConversionRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.item_sink.item_sink_conversion");
    }

    @Override
    public int getWidth() {
        return background.getWidth();
    }

    @Override
    public int getHeight() {
        return background.getHeight();
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ItemSinkConversionRecipe recipe, IFocusGroup focuses) {
        // Keep the unknown input visual; do not index the entire item registry as ingredients.
        builder.addOutputSlot(OUTPUT_X + 1, SLOT_Y + 1)
                .setSlotName("currency")
                .addItemStacks(recipe.outputs());
    }

    @Override
    public void draw(ItemSinkConversionRecipe recipe, IRecipeSlotsView recipeSlotsView,
                     GuiGraphics graphics, double mouseX, double mouseY) {
        drawCheckerboardSlot(graphics, INPUT_X, SLOT_Y);
        drawCheckerboardSlot(graphics, OUTPUT_X, SLOT_Y);
        graphics.drawCenteredString(Minecraft.getInstance().font, "?",
                INPUT_X + SLOT_SIZE / 2, SLOT_Y + 5, 0xFFFFFF);
        arrow.draw(graphics, 45, 8);
    }

    private static void drawCheckerboardSlot(GuiGraphics graphics, int x, int y) {
        // Match the reference slot using fills, with no additional texture asset.
        graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0xFF505050);
        for (int row = 0; row < 8; row++) {
            for (int column = 0; column < 8; column++) {
                int color = (row + column) % 2 == 0 ? 0xFF5E5E5E : 0xFF4B4B4B;
                int left = x + 1 + column * 2;
                int top = y + 1 + row * 2;
                graphics.fill(left, top, left + 2, top + 2, color);
            }
        }
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, ItemSinkConversionRecipe recipe,
                           IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
        if (mouseX >= INPUT_X && mouseX < INPUT_X + SLOT_SIZE
                && mouseY >= SLOT_Y && mouseY < SLOT_Y + SLOT_SIZE) {
            tooltip.add(Component.translatable("jei.item_sink.overview.payout"));
        }
    }

    @Override
    public ResourceLocation getRegistryName(ItemSinkConversionRecipe recipe) {
        return recipe.id();
    }
}
