package com.ourcreation.itemsink.client;

import com.ourcreation.itemsink.menu.ItemSinkMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Optional;

public class ItemSinkScreen extends AbstractContainerScreen<ItemSinkMenu> {
    private static final ResourceLocation BACKGROUND = texture("background_classic");
    private static final ResourceLocation INVENTORY = texture("base_inventory");
    private static final ResourceLocation SLOT = texture("base_slot");
    private static final ResourceLocation PROGRESS_EMPTY = texture("base_progress_empty");
    private static final ResourceLocation PROGRESS_FILLED = texture("base_progress_filled");
    private static final ResourceLocation ENERGY_EMPTY = texture("base_energy_storage_empty");
    private static final ResourceLocation ENERGY_FILLED = texture("base_energy_storage_filled");
    private static final int ENERGY_X = 13;
    private static final int ENERGY_Y = 23;
    private static final int ENERGY_WIDTH = 18;
    private static final int ENERGY_HEIGHT = 44;
    private static final int PROGRESS_X = 84;
    private static final int PROGRESS_Y = 36;
    private static final int PROGRESS_WIDTH = 24;
    private static final int PROGRESS_HEIGHT = 16;

    public ItemSinkScreen(ItemSinkMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = 176;
        imageHeight = 166;
        titleLabelX = 68;
        titleLabelY = 7;
        inventoryLabelX = 7;
        inventoryLabelY = 72;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
        renderWidgetTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        guiGraphics.blit(BACKGROUND, x, y, 0, 0, imageWidth, imageHeight, 176, 166);
        guiGraphics.blit(INVENTORY, x + 7, y + 83, 0, 0, 162, 76, 162, 76);

        drawSlot(guiGraphics, x + 52, y + 35);
        drawSlot(guiGraphics, x + 125, y + 35);
        drawSlot(guiGraphics, x + 143, y + 35);

        drawEnergy(guiGraphics, x + ENERGY_X, y + ENERGY_Y);
        drawProgress(guiGraphics, x + PROGRESS_X, y + PROGRESS_Y);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Longer translated block names must fit within the panel.
        int titleWidth = font.width(title);
        float scale = Math.min(1.0F, (imageWidth - 12.0F) / Math.max(1, titleWidth));
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(imageWidth / 2.0F, titleLabelY, 0);
        guiGraphics.pose().scale(scale, scale, 1.0F);
        guiGraphics.drawString(font, title, -titleWidth / 2, 0, 0x404040, false);
        guiGraphics.pose().popPose();
        guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
    }

    private void drawSlot(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.blit(SLOT, x - 1, y - 1, 0, 0, 18, 18, 18, 18);
    }

    private void drawEnergy(GuiGraphics guiGraphics, int x, int y) {
        int height = 44;
        guiGraphics.blit(ENERGY_EMPTY, x, y, 0, 0, 18, height, 18, height);
        int maxEnergy = Math.max(1, menu.maxEnergy());
        int filled = Math.min(height, Math.max(0, (int) ((long) menu.energy() * height / maxEnergy)));
        if (filled > 0) {
            guiGraphics.blit(ENERGY_FILLED, x, y + height - filled, 0, height - filled, 18, filled, 18, height);
        }
    }

    private void drawProgress(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.blit(PROGRESS_EMPTY, x, y, 0, 0, 24, 16, 24, 16);
        int maxProgress = Math.max(1, menu.maxProgress());
        int width = Math.min(24, Math.max(0, (int) ((long) menu.progress() * 24 / maxProgress)));
        if (width > 0) {
            guiGraphics.blit(PROGRESS_FILLED, x, y, 0, 0, width, 16, 24, 16);
        }
    }

    private void renderWidgetTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (isHovering(ENERGY_X, ENERGY_Y, ENERGY_WIDTH, ENERGY_HEIGHT, mouseX, mouseY)) {
            guiGraphics.renderTooltip(
                    font,
                    Component.translatable("gui.item_sink.energy", menu.energy(), menu.maxEnergy()),
                    mouseX,
                    mouseY
            );
            return;
        }

        if (isHovering(PROGRESS_X, PROGRESS_Y, PROGRESS_WIDTH, PROGRESS_HEIGHT, mouseX, mouseY)) {
            int maxProgress = Math.max(1, menu.maxProgress());
            int progress = Math.min(menu.progress(), maxProgress);
            int percent = (int) ((long) progress * 100 / maxProgress);
            guiGraphics.renderTooltip(
                    font,
                    List.of(
                            Component.translatable("gui.item_sink.progress", progress, maxProgress),
                            Component.translatable("gui.item_sink.complete", percent)
                    ),
                    Optional.empty(),
                    mouseX,
                    mouseY
            );
        }
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath("item_sink", "textures/gui/" + name + ".png");
    }
}
