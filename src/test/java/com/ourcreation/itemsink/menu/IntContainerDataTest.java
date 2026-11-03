package com.ourcreation.itemsink.menu;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundContainerSetDataPacket;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.SimpleContainerData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IntContainerDataTest {
    @Test
    void fullIntegerRangeSurvivesRealContainerPackets() {
        SimpleContainerData server = new SimpleContainerData(4);
        SimpleContainerData client = new SimpleContainerData(4);
        IntContainerData sender = new IntContainerData(server);
        IntContainerData receiver = new IntContainerData(client);
        int[] values = {0, 1, 32767, 32768, 50000, 65535, 65536, 75000, 100000, Integer.MAX_VALUE};

        for (int field = 0; field < 4; field++) {
            for (int value : values) {
                server.set(field, value);
                for (int slot = 0; slot < sender.getCount(); slot++) {
                    transmit(sender, receiver, slot);
                }
                for (int index = 0; index < 4; index++) {
                    assertEquals(server.get(index), client.get(index), "field " + index + ", value " + value);
                }
                assertEquals(value, server.get(field), "sync must not change server values");
            }
        }
    }

    @Test
    void updatesWorkWhenOnlyOneHalfChangesOrValueDecreases() {
        SimpleContainerData server = new SimpleContainerData(4);
        SimpleContainerData client = new SimpleContainerData(4);
        IntContainerData sender = new IntContainerData(server);
        IntContainerData receiver = new IntContainerData(client);
        DataSlot[] slots = new DataSlot[sender.getCount()];
        for (int slot = 0; slot < slots.length; slot++) {
            slots[slot] = DataSlot.forContainer(sender, slot);
        }

        int[] values = {100000, 100001, 165537, 65536, 65535, 32768, 32767, Integer.MAX_VALUE, 0};
        for (int value : values) {
            server.set(2, value);
            for (int slot = 0; slot < slots.length; slot++) {
                if (slots[slot].checkAndClearUpdateFlag()) {
                    transmit(sender, receiver, slot);
                }
            }
            assertEquals(value, client.get(2), "value " + value);
        }
    }

    @Test
    void serverCapacityReplacesClientValueWithoutClampingEnergy() {
        SimpleContainerData server = new SimpleContainerData(4);
        server.set(2, 100000);
        server.set(3, 250000);
        SimpleContainerData client = new SimpleContainerData(4);
        client.set(3, 10000);
        IntContainerData sender = new IntContainerData(server);
        IntContainerData receiver = new IntContainerData(client);

        for (int slot = 0; slot < sender.getCount(); slot++) {
            transmit(sender, receiver, slot);
        }

        assertEquals(100000, client.get(2));
        assertEquals(250000, client.get(3));
        assertEquals(100000, server.get(2));
        assertEquals(250000, server.get(3));
    }

    private static void transmit(IntContainerData sender, IntContainerData receiver, int slot) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            new ClientboundContainerSetDataPacket(1, slot, sender.get(slot)).write(buffer);
            ClientboundContainerSetDataPacket packet = new ClientboundContainerSetDataPacket(buffer);
            receiver.set(packet.getId(), packet.getValue());
        } finally {
            buffer.release();
        }
    }
}
