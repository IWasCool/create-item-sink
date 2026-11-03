package com.ourcreation.itemsink.menu;

import net.minecraft.world.inventory.ContainerData;

/** Preserves full integer values over the menu protocol's signed 16-bit data packets. */
final class IntContainerData implements ContainerData {
    private final ContainerData data;

    IntContainerData(ContainerData data) {
        this.data = data;
    }

    @Override
    public int get(int index) {
        int shift = (index & 1) * 16;
        return (data.get(index / 2) >>> shift) & 0xFFFF;
    }

    @Override
    public void set(int index, int value) {
        int dataIndex = index / 2;
        int shift = (index & 1) * 16;
        int mask = 0xFFFF << shift;
        data.set(dataIndex, (data.get(dataIndex) & ~mask) | ((value & 0xFFFF) << shift));
    }

    @Override
    public int getCount() {
        return data.getCount() * 2;
    }
}
