package com.ourcreation.itemsink.client.ponder;

import com.ourcreation.itemsink.Config;
import com.ourcreation.itemsink.block.ItemSinkBlock;
import com.ourcreation.itemsink.block.entity.ItemSinkBlockEntity;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class ItemSinkScenes {
    private static final BlockPos SINK = new BlockPos(3, 2, 3);
    private static final BlockPos INPUT_BELT_START = new BlockPos(1, 1, 3);
    private static final BlockPos INPUT_BELT_END = new BlockPos(2, 1, 3);
    private static final BlockPos OUTPUT_BELT_START = new BlockPos(4, 1, 3);
    private static final BlockPos OUTPUT_BELT_END = new BlockPos(5, 1, 3);
    private static final BlockPos SOURCE_CHEST = new BlockPos(0, 2, 3);
    private static final BlockPos OUTPUT_CHEST = new BlockPos(6, 2, 3);
    private static final BlockPos CRANK = new BlockPos(0, 1, 1);
    private static final BlockPos BATTERY = new BlockPos(1, 1, 0);
    private static final BlockPos BATTERY_CONNECTOR = BATTERY.above();
    private static final BlockPos SINK_CONNECTOR = SINK.north();

    private ItemSinkScenes() {}

    public static boolean hasWireExample() {
        return BuiltInRegistries.BLOCK.containsKey(ResourceLocation.parse("createaddition:modular_accumulator"))
                && BuiltInRegistries.BLOCK.containsKey(ResourceLocation.parse("createaddition:connector"))
                && BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse("createaddition:copper_spool"));
    }

    public static void tutorial(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        boolean wired = hasWireExample();
        scene.title(wired ? "item_sink_complete" : "item_sink_complete_fe", "Using and Automating the Item Sink");
        scene.configureBasePlate(0, 0, 7);
        scene.scaleSceneView(0.9F);
        scene.showBasePlate();
        scene.world().setKineticSpeed(util.select().everywhere(), 0);
        scene.world().showSection(util.select().fromTo(SINK.below(), SINK), Direction.DOWN);
        scene.idle(15);

        Example example = example();
        Vec3 front = util.vector().blockSurface(SINK, Direction.SOUTH);
        scene.overlay().showControls(front, Pointing.RIGHT, 50).rightClick().withItem(example.input());
        scene.overlay().showText(65)
                .text("Right-click the sink. Put unwanted items in its left input slot.")
                .pointAt(front).placeNearTarget().attachKeyFrame();
        moveItem(scene, front.add(-1.5, 0.5, 0), front, example.input(), 15);
        insertInput(scene, SINK, example);
        scene.idle(65);

        if (wired) {
            installPowerExample(scene, util);
        } else {
            Vec3 port = util.vector().blockSurface(SINK, Direction.NORTH);
            scene.overlay().showBigLine(PonderPalette.BLUE, port.add(0, 0, -1.5), port, 80);
        }

        scene.overlay().showText(80)
                .text("The connected source fills the FE buffer. The sink lights up as it uses that energy to make currency.")
                .pointAt(util.vector().centerOf(SINK)).placeNearTarget()
                .colored(PonderPalette.BLUE).attachKeyFrame();
        // Ponder is client-side. Animate the stored FE alongside the real displayed connection.
        for (int step = 1; step <= 4; step++) {
            int charge = Config.ENERGY_CAPACITY.get() / 4 * step;
            scene.world().modifyBlockEntity(SINK, ItemSinkBlockEntity.class, sink -> sink.getData().set(2, charge));
            if (wired) {
                int remaining = 1000000 - charge;
                scene.world().modifyBlockEntityNBT(util.select().position(BATTERY), BlockEntity.class,
                        tag -> tag.getCompound("EnergyContent").putInt("energy", remaining));
            }
            scene.idle(10);
        }
        animateConversion(scene, SINK, example);
        scene.idle(30);

        scene.world().showSection(util.select().fromTo(0, 1, 3, 2, 2, 3), Direction.DOWN);
        scene.world().showSection(util.select().fromTo(4, 1, 3, 6, 2, 3), Direction.DOWN);
        scene.world().showSection(util.select().fromTo(0, 1, 1, 4, 1, 2), Direction.DOWN);
        scene.world().modifyBlockEntity(SOURCE_CHEST, ChestBlockEntity.class,
                chest -> chest.setItem(0, example.input().copyWithCount(16)));
        scene.idle(10);
        scene.overlay().showText(80)
                .text("Turn the belts. Funnels feed accepted items from the input chest into the sink.")
                .pointAt(util.vector().centerOf(INPUT_BELT_END.above())).placeNearTarget()
                .colored(PonderPalette.INPUT).attachKeyFrame();
        scene.overlay().showControls(util.vector().blockSurface(CRANK, Direction.WEST), Pointing.RIGHT, 50).rightClick();
        scene.world().setKineticSpeed(util.select().everywhere(), -32);
        scene.effects().rotationDirectionIndicator(CRANK);
        scene.world().createItemOnBelt(INPUT_BELT_START, Direction.WEST, example.input());
        scene.idle(20);
        scene.world().removeItemsFromBelt(INPUT_BELT_END);
        scene.world().flapFunnel(INPUT_BELT_END.above(), false);
        scene.world().modifyBlockEntity(SOURCE_CHEST, ChestBlockEntity.class,
                chest -> chest.setItem(0, example.input().copyWithCount(15)));
        insertInput(scene, SINK, example);
        scene.idle(75);

        scene.overlay().showText(80)
                .text("The extracting funnel puts currency on the output belt. Collect it in the chest.")
                .pointAt(util.vector().centerOf(OUTPUT_BELT_START.above())).placeNearTarget()
                .colored(PonderPalette.OUTPUT).attachKeyFrame();
        animateConversion(scene, SINK, example);
        clearOutput(scene, SINK);
        scene.world().createItemOnBelt(OUTPUT_BELT_START, Direction.WEST, example.output());
        scene.idle(20);
        scene.world().removeItemsFromBelt(OUTPUT_BELT_END);
        scene.world().flapFunnel(OUTPUT_BELT_END.above(), false);
        scene.world().modifyBlockEntity(OUTPUT_CHEST, ChestBlockEntity.class,
                chest -> chest.setItem(0, example.output().copy()));
        scene.effects().indicateSuccess(OUTPUT_CHEST);
        scene.idle(50);

        scene.overlay().showText(85)
                .text("Keep power supplied and the outputs clear. A full machine pauses until there is room again.")
                .pointAt(util.vector().centerOf(SINK)).placeNearTarget()
                .colored(PonderPalette.RED).attachKeyFrame();
        scene.world().modifyBlockEntity(SINK, ItemSinkBlockEntity.class, sink -> {
            sink.getInventory().setStackInSlot(ItemSinkBlockEntity.INPUT_SLOT, example.input().copy());
            sink.getInventory().setStackInSlot(ItemSinkBlockEntity.OUTPUT_SLOT_1, example.output().copyWithCount(64));
            sink.getInventory().setStackInSlot(ItemSinkBlockEntity.OUTPUT_SLOT_2, example.output().copyWithCount(64));
        });
        scene.overlay().showOutline(PonderPalette.RED, new Object(), util.select().position(SINK), 85);
        scene.idle(100);
        clearOutput(scene, SINK);
        scene.world().modifyBlockEntity(SINK, ItemSinkBlockEntity.class,
                sink -> sink.getInventory().setStackInSlot(ItemSinkBlockEntity.OUTPUT_SLOT_2, ItemStack.EMPTY));
        animateConversion(scene, SINK, example);
        scene.idle(50);
        scene.markAsFinished();
    }

    private static void installPowerExample(SceneBuilder scene, SceneBuildingUtil util) {
        scene.world().setBlock(BATTERY, BuiltInRegistries.BLOCK
                .get(ResourceLocation.parse("createaddition:modular_accumulator")).defaultBlockState(), false);
        scene.world().modifyBlockEntityNBT(util.select().position(BATTERY), BlockEntity.class, tag -> {
            CompoundTag charge = new CompoundTag();
            charge.putInt("energy", 1000000);
            tag.put("EnergyContent", charge);
            tag.putInt("EnergyCapacity", 1000000);
            tag.putInt("Size", 1);
            tag.putInt("Height", 1);
            tag.remove("Uninitialized");
        });
        scene.world().showSection(util.select().position(BATTERY), Direction.DOWN);
        // FACING points at the supporting block, not away from it. Never use the north-facing default.
        BlockState connector = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("createaddition:connector")).defaultBlockState();
        scene.world().setBlock(BATTERY_CONNECTOR,
                setProperty(connector, connector.getBlock().getStateDefinition().getProperty("facing"), "down"), false);
        scene.world().setBlock(SINK_CONNECTOR,
                setProperty(connector, connector.getBlock().getStateDefinition().getProperty("facing"), "south"), false);
        scene.world().showSection(util.select().position(BATTERY_CONNECTOR), Direction.DOWN);
        scene.world().showSection(util.select().position(SINK_CONNECTOR), Direction.SOUTH);
        scene.overlay().showText(90)
                .text("Attach connectors to the charged accumulator and the sink. Wrench the battery to Pull and the sink to Push.")
                .pointAt(util.vector().centerOf(BATTERY_CONNECTOR)).placeNearTarget()
                .colored(PonderPalette.BLUE).attachKeyFrame();
        ItemStack wrench = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("create:wrench")));
        scene.overlay().showControls(util.vector().centerOf(BATTERY_CONNECTOR), Pointing.RIGHT, 35).rightClick().withItem(wrench);
        scene.world().modifyBlock(BATTERY_CONNECTOR,
                state -> setProperty(state, state.getBlock().getStateDefinition().getProperty("mode"), "pull"), false);
        scene.idle(35);
        scene.overlay().showControls(util.vector().centerOf(SINK_CONNECTOR), Pointing.RIGHT, 35).rightClick().withItem(wrench);
        scene.world().modifyBlock(SINK_CONNECTOR,
                state -> setProperty(state, state.getBlock().getStateDefinition().getProperty("mode"), "push"), false);
        scene.idle(70);

        scene.overlay().showText(80)
                .text("Use a copper wire spool on both connectors to link the battery to the sink.")
                .pointAt(util.vector().centerOf(SINK_CONNECTOR)).placeNearTarget().attachKeyFrame();
        ItemStack spool = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("createaddition:copper_spool")));
        scene.overlay().showControls(util.vector().centerOf(BATTERY_CONNECTOR), Pointing.RIGHT, 30).rightClick().withItem(spool);
        scene.idle(35);
        scene.overlay().showControls(util.vector().centerOf(SINK_CONNECTOR), Pointing.RIGHT, 30).rightClick().withItem(spool);
        connectWire(scene, util, BATTERY_CONNECTOR, SINK_CONNECTOR);
        connectWire(scene, util, SINK_CONNECTOR, BATTERY_CONNECTOR);
        scene.idle(65);
    }

    private static <T extends Comparable<T>> BlockState setProperty(BlockState state, Property<T> property, String value) {
        return state.setValue(property, property.getValue(value).orElseThrow());
    }

    private static void connectWire(SceneBuilder scene, SceneBuildingUtil util, BlockPos from, BlockPos to) {
        // Create Crafts & Additions uses this saved node format on both supported loaders.
        scene.world().modifyBlockEntityNBT(util.select().position(from), BlockEntity.class, tag -> {
            CompoundTag node = new CompoundTag();
            node.putInt("id", 0);
            node.putInt("other", 0);
            node.putInt("type", 0); // Copper
            node.putInt("x", to.getX() - from.getX());
            node.putInt("y", to.getY() - from.getY());
            node.putInt("z", to.getZ() - from.getZ());
            ListTag nodes = new ListTag();
            nodes.add(node);
            tag.put("nodes", nodes);
        });
    }

    private static void insertInput(SceneBuilder scene, BlockPos sinkPos, Example example) {
        scene.world().modifyBlockEntity(sinkPos, ItemSinkBlockEntity.class,
                sink -> sink.getInventory().setStackInSlot(ItemSinkBlockEntity.INPUT_SLOT, example.input().copy()));
    }

    private static void clearOutput(SceneBuilder scene, BlockPos sinkPos) {
        scene.world().modifyBlockEntity(sinkPos, ItemSinkBlockEntity.class,
                sink -> sink.getInventory().setStackInSlot(ItemSinkBlockEntity.OUTPUT_SLOT_1, ItemStack.EMPTY));
    }

    private static void animateConversion(SceneBuilder scene, BlockPos sinkPos, Example example) {
        // Ponder runs on the client, where the real machine ticker is disabled.
        // Animate a shortened example without changing the player's config or world.
        scene.world().modifyBlock(sinkPos, state -> state.setValue(ItemSinkBlock.LIT, true), false);
        for (int progress = 1; progress <= 6; progress++) {
            int currentProgress = progress;
            scene.world().modifyBlockEntity(sinkPos, ItemSinkBlockEntity.class, sink -> {
                sink.getData().set(1, 6);
                sink.getData().set(0, currentProgress);
            });
            scene.idle(5);
        }
        scene.world().modifyBlockEntity(sinkPos, ItemSinkBlockEntity.class, sink -> {
            sink.getInventory().setStackInSlot(ItemSinkBlockEntity.INPUT_SLOT, ItemStack.EMPTY);
            sink.getInventory().setStackInSlot(ItemSinkBlockEntity.OUTPUT_SLOT_1, example.output().copy());
            sink.getData().set(0, 0);
        });
        scene.world().modifyBlock(sinkPos, state -> state.setValue(ItemSinkBlock.LIT, false), false);
        scene.effects().indicateSuccess(sinkPos);
    }

    private static void moveItem(SceneBuilder scene, Vec3 from, Vec3 to, ItemStack stack, int ticks) {
        ElementLink<EntityElement> item = scene.world().createItemEntity(from, Vec3.ZERO, stack.copy());
        scene.world().modifyEntity(item, entity -> entity.setNoGravity(true));
        for (int tick = 1; tick <= ticks; tick++) {
            Vec3 position = from.lerp(to, (double) tick / ticks);
            scene.world().modifyEntity(item, entity -> entity.setPos(position));
            scene.idle(1);
        }
        scene.world().modifyEntity(item, Entity::discard);
    }

    private static Example example() {
        ItemStack flint = new ItemStack(Items.FLINT);
        Config.Exchange exchange = Config.findExchange(flint).orElseGet(() -> Config.exchanges().stream()
                .filter(candidate -> !candidate.createOutput().isEmpty())
                .findFirst().orElse(null));
        if (exchange != null) {
            return new Example(new ItemStack(BuiltInRegistries.ITEM.get(exchange.input())), exchange.createOutput());
        }
        // Keep the guide usable before the pack has configured an available currency.
        return new Example(flint, new ItemStack(Items.EMERALD));
    }

    private record Example(ItemStack input, ItemStack output) {
    }
}
