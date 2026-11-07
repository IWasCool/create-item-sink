package com.ourcreation.itemsink.block.entity;

import com.ourcreation.itemsink.Config;
import com.ourcreation.itemsink.ItemSink;
import com.ourcreation.itemsink.block.ItemSinkBlock;
import com.ourcreation.itemsink.menu.ItemSinkMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;

public class ItemSinkBlockEntity extends BlockEntity implements MenuProvider {
    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT_1 = 1;
    public static final int OUTPUT_SLOT_2 = 2;
    public static final int SLOT_COUNT = 3;

    private final ItemStackHandler inventory = new ItemStackHandler(SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot != INPUT_SLOT || Config.findExchange(stack).isPresent();
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private final AutomationInventory automationInventory = new AutomationInventory(inventory);
    private final ItemSinkEnergyStorage energyStorage = new ItemSinkEnergyStorage();
    private int progress;
    private int maxProgress;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> maxProgress;
                case 2 -> energyStorage.getEnergyStored();
                case 3 -> energyStorage.getMaxEnergyStored();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> progress = value;
                case 1 -> maxProgress = value;
                case 2 -> energyStorage.setEnergy(value);
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public ItemSinkBlockEntity(BlockPos pos, BlockState blockState) {
        super(ItemSink.ITEM_SINK_BLOCK_ENTITY.get(), pos, blockState);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ItemSinkBlockEntity blockEntity) {
        boolean wasRunning = state.getValue(ItemSinkBlock.LIT);
        int previousProgress = blockEntity.progress;
        boolean running = blockEntity.process();

        if (running) {
            blockEntity.playActiveSounds(level, pos, previousProgress);
        }

        if (wasRunning != running) {
            level.setBlock(pos, state.setValue(ItemSinkBlock.LIT, running), Block.UPDATE_ALL);
        }
    }

    private boolean process() {
        ItemStack input = inventory.getStackInSlot(INPUT_SLOT);
        Config.Exchange exchange = Config.findExchange(input).orElse(null);

        if (exchange == null) {
            resetProgress();
            return false;
        }

        maxProgress = exchange.ticks();
        ItemStack output = exchange.createOutput();
        if (output.isEmpty() || !canInsertOutput(output)) {
            return false;
        }

        if (Config.REQUIRE_ENERGY.get() && energyStorage.getEnergyStored() < exchange.energyPerTick()) {
            return false;
        }

        if (Config.REQUIRE_ENERGY.get()) {
            energyStorage.extractInternal(exchange.energyPerTick());
        }

        progress++;
        setChanged();

        if (progress >= maxProgress) {
            inventory.extractItem(INPUT_SLOT, 1, false);
            insertOutput(output);
            progress = 0;
            setChanged();
        }

        return true;
    }

    private void playActiveSounds(Level level, BlockPos pos, int previousProgress) {
        long tick = level.getGameTime() + Math.floorMod(pos.asLong(), 37);
        if (tick % 20 == 0 || previousProgress > 0 && progress == 0) {
            playSound(level, pos, ItemSink.ELECTRIC_MOTOR_BUZZ.get(), 0.03F, 1.0F);
        }
    }

    private static void playSound(Level level, BlockPos pos, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, pos, sound, SoundSource.BLOCKS, volume, pitch);
    }

    private void resetProgress() {
        if (progress != 0 || maxProgress != 0) {
            progress = 0;
            maxProgress = 0;
            setChanged();
        }
    }

    private boolean canInsertOutput(ItemStack output) {
        return insertOutput(output, true).isEmpty();
    }

    private void insertOutput(ItemStack output) {
        insertOutput(output, false);
    }

    private ItemStack insertOutput(ItemStack output, boolean simulate) {
        ItemStack remainder = inventory.insertItem(OUTPUT_SLOT_1, output.copy(), simulate);
        if (!remainder.isEmpty()) {
            remainder = inventory.insertItem(OUTPUT_SLOT_2, remainder, simulate);
        }
        return remainder;
    }

    public void dropContents(Level level, BlockPos pos) {
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                Block.popResource(level, pos, stack);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", inventory.serializeNBT(registries));
        tag.put("Energy", energyStorage.serializeNBT(registries));
        tag.putInt("Progress", progress);
        tag.putInt("MaxProgress", maxProgress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        if (tag.contains("Energy")) {
            energyStorage.deserializeNBT(registries, tag.get("Energy"));
        }
        progress = tag.getInt("Progress");
        maxProgress = tag.getInt("MaxProgress");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.item_sink.item_sink");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ItemSinkMenu(containerId, playerInventory, this);
    }

    public IItemHandlerModifiable getInventory() {
        return inventory;
    }

    public ContainerData getData() {
        return data;
    }

    public IItemHandler getAutomationInventory() {
        return automationInventory;
    }

    public IEnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    private static class AutomationInventory implements IItemHandler {
        private final ItemStackHandler inventory;

        private AutomationInventory(ItemStackHandler inventory) {
            this.inventory = inventory;
        }

        @Override
        public int getSlots() {
            return SLOT_COUNT;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return inventory.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot != INPUT_SLOT || !inventory.isItemValid(slot, stack)) {
                return stack;
            }
            return inventory.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot == INPUT_SLOT) {
                return ItemStack.EMPTY;
            }
            return inventory.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return inventory.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == INPUT_SLOT && inventory.isItemValid(slot, stack);
        }
    }

    private static class ItemSinkEnergyStorage extends EnergyStorage {
        private ItemSinkEnergyStorage() {
            super(Config.ENERGY_CAPACITY.get(), Config.MAX_ENERGY_INPUT.get(), 0);
        }

        @Override
        public int getMaxEnergyStored() {
            return Config.ENERGY_CAPACITY.get();
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            if (!canReceive()) {
                return 0;
            }

            int received = Math.min(getMaxEnergyStored() - energy, Math.min(Config.MAX_ENERGY_INPUT.get(), maxReceive));
            if (!simulate) {
                energy += received;
            }
            return received;
        }

        private void extractInternal(int amount) {
            energy = Math.max(0, energy - amount);
        }

        private void setEnergy(int value) {
            energy = Math.max(0, Math.min(value, getMaxEnergyStored()));
        }
    }
}
