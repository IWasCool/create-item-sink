package com.ourcreation.itemsink.menu;

import com.ourcreation.itemsink.Config;
import com.ourcreation.itemsink.ItemSink;
import com.ourcreation.itemsink.block.entity.ItemSinkBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class ItemSinkMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOT_COUNT = 3;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final BlockPos pos;
    private final ContainerData data;

    public ItemSinkMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, resolveBlockEntity(playerInventory, buffer.readBlockPos()));
    }

    public ItemSinkMenu(int containerId, Inventory playerInventory, ItemSinkBlockEntity blockEntity) {
        this(containerId, playerInventory, blockEntity.getInventory(), blockEntity.getData(), blockEntity.getBlockPos());
    }

    private ItemSinkMenu(int containerId, Inventory playerInventory, BlockEntityLookup lookup) {
        this(containerId, playerInventory, lookup.inventory, lookup.data, lookup.pos);
    }

    private ItemSinkMenu(int containerId, Inventory playerInventory, IItemHandler inventory, ContainerData data, BlockPos pos) {
        super(ItemSink.ITEM_SINK_MENU.get(), containerId);
        this.pos = pos;
        this.data = data;

        addSlot(new InputSlot(inventory, ItemSinkBlockEntity.INPUT_SLOT, 52, 35));
        addSlot(new OutputSlot(inventory, ItemSinkBlockEntity.OUTPUT_SLOT_1, 125, 35));
        addSlot(new OutputSlot(inventory, ItemSinkBlockEntity.OUTPUT_SLOT_2, 143, 35));

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }

        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, 142));
        }

        addDataSlots(data);
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().getBlockEntity(pos) instanceof ItemSinkBlockEntity blockEntity
                && Container.stillValidBlockEntity(blockEntity, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);

        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();

            if (index < MACHINE_SLOT_COUNT) {
                if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(stack, result);
            } else if (Config.findExchange(stack).isPresent()) {
                if (!moveItemStackTo(stack, ItemSinkBlockEntity.INPUT_SLOT, ItemSinkBlockEntity.INPUT_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= PLAYER_INVENTORY_START && index < PLAYER_INVENTORY_END) {
                if (!moveItemStackTo(stack, HOTBAR_START, HOTBAR_END, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= HOTBAR_START && index < HOTBAR_END && !moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, false)) {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (stack.getCount() == result.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, stack);
        }

        return result;
    }

    public int progress() {
        return data.get(0);
    }

    public int maxProgress() {
        return data.get(1);
    }

    public int energy() {
        return data.get(2);
    }

    public int maxEnergy() {
        return data.get(3);
    }

    private static BlockEntityLookup resolveBlockEntity(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof ItemSinkBlockEntity itemSink) {
            return new BlockEntityLookup(itemSink.getInventory(), itemSink.getData(), pos);
        }
        return new BlockEntityLookup(new ItemStackHandler(MACHINE_SLOT_COUNT), new SimpleContainerData(4), pos);
    }

    private record BlockEntityLookup(IItemHandler inventory, ContainerData data, BlockPos pos) {
    }

    private static class InputSlot extends SlotItemHandler {
        private InputSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
            super(itemHandler, index, xPosition, yPosition);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return Config.findExchange(stack).isPresent();
        }
    }

    private static class OutputSlot extends SlotItemHandler {
        private OutputSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
            super(itemHandler, index, xPosition, yPosition);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
