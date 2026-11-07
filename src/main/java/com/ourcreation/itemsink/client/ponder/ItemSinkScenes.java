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
    private static final BlockPos SINK = new BlockPos(2, 2, 2);
    private static final BlockPos CREATE_SINK = new BlockPos(3, 2, 3);
    private static final BlockPos INPUT_BELT_START = new BlockPos(1, 1, 3);
    private static final BlockPos INPUT_BELT_END = new BlockPos(2, 1, 3);
    private static final BlockPos OUTPUT_BELT_START = new BlockPos(4, 1, 3);
    private static final BlockPos OUTPUT_BELT_END = new BlockPos(5, 1, 3);
    private static final BlockPos SOURCE_CHEST = new BlockPos(0, 2, 3);
    private static final BlockPos OUTPUT_CHEST = new BlockPos(6, 2, 3);
    private static final BlockPos CRANK = new BlockPos(0, 1, 1);
    private static final BlockPos BATTERY = new BlockPos(0, 1, 0);
    private static final BlockPos BATTERY_CONNECTOR = BATTERY.above();
    private static final BlockPos SINK_CONNECTOR = SINK.north();

    private ItemSinkScenes() {
    }

    public static boolean hasWireExample() {
        return BuiltInRegistries.BLOCK.containsKey(ResourceLocation.parse("createaddition:modular_accumulator"))
                && BuiltInRegistries.BLOCK.containsKey(ResourceLocation.parse("createaddition:connector"))
                && BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse("createaddition:copper_spool"));
    }

    public static void power(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("item_sink_power", "Powering the Item Sink with a Wire");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.world().showSection(util.select().fromTo(SINK.below(), SINK), Direction.DOWN);
        scene.idle(15);

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
        scene.overlay().showText(70)
                .text("Start with a charged accumulator. It stores Forge Energy (FE).")
                .pointAt(util.vector().centerOf(BATTERY))
                .placeNearTarget()
                .colored(PonderPalette.BLUE)
                .attachKeyFrame();
        scene.idle(80);

        BlockState connector = BuiltInRegistries.BLOCK
                .get(ResourceLocation.parse("createaddition:connector")).defaultBlockState();
        scene.world().setBlock(BATTERY_CONNECTOR, connector, false);
        scene.world().setBlock(SINK_CONNECTOR,
                setProperty(connector, connector.getBlock().getStateDefinition().getProperty("facing"), "south"), false);
        scene.world().showSection(util.select().position(BATTERY_CONNECTOR), Direction.DOWN);
        scene.world().showSection(util.select().position(SINK_CONNECTOR), Direction.SOUTH);
        ItemStack wrench = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("create:wrench")));
        scene.overlay().showControls(util.vector().centerOf(BATTERY_CONNECTOR), Pointing.RIGHT, 40)
                .rightClick().withItem(wrench);
        scene.world().modifyBlock(BATTERY_CONNECTOR,
                state -> setProperty(state, state.getBlock().getStateDefinition().getProperty("mode"), "pull"), false);
        scene.overlay().showText(85)
                .text("Wrench the battery connector to Pull and the sink connector to Push.")
                .pointAt(util.vector().centerOf(SINK_CONNECTOR))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(40);
        scene.overlay().showControls(util.vector().centerOf(SINK_CONNECTOR), Pointing.RIGHT, 40)
                .rightClick().withItem(wrench);
        scene.world().modifyBlock(SINK_CONNECTOR,
                state -> setProperty(state, state.getBlock().getStateDefinition().getProperty("mode"), "push"), false);
        scene.idle(65);

        ItemStack spool = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("createaddition:copper_spool")));
        scene.overlay().showText(90)
                .text("Right-click both connectors with a copper wire spool to join them.")
                .pointAt(util.vector().centerOf(BATTERY_CONNECTOR))
                .placeNearTarget()
                .attachKeyFrame();
        scene.overlay().showControls(util.vector().centerOf(BATTERY_CONNECTOR), Pointing.RIGHT, 35)
                .rightClick().withItem(spool);
        scene.idle(40);
        scene.overlay().showControls(util.vector().centerOf(SINK_CONNECTOR), Pointing.RIGHT, 35)
                .rightClick().withItem(spool);
        // These are the optional mod's real, reciprocal wire nodes, not a drawn power line.
        connectWire(scene, util, BATTERY_CONNECTOR, SINK_CONNECTOR);
        connectWire(scene, util, SINK_CONNECTOR, BATTERY_CONNECTOR);
        scene.idle(90);

        Example example = example();
        insertInput(scene, SINK, example);
        scene.overlay().showText(90)
                .text("The wire fills the sink's FE buffer. That power turns items into currency.")
                .pointAt(util.vector().centerOf(SINK))
                .placeNearTarget()
                .colored(PonderPalette.BLUE)
                .attachKeyFrame();
        // Ponder is client-side: demonstrate charging without ticking a real energy network.
        for (int step = 1; step <= 4; step++) {
            int charge = Config.ENERGY_CAPACITY.get() / 4 * step;
            scene.world().modifyBlockEntity(SINK, ItemSinkBlockEntity.class,
                    sink -> sink.getData().set(2, charge));
            scene.idle(10);
        }
        animateConversion(scene, SINK, example);
        scene.idle(50);
        scene.markAsFinished();
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

    public static void basics(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("item_sink_basics", "Using the Item Sink");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(5);
        scene.world().showSection(util.select().fromTo(SINK.below(), SINK), Direction.DOWN);
        scene.idle(15);

        Example example = example();
        Vec3 front = util.vector().blockSurface(SINK, Direction.SOUTH);
        scene.overlay().showControls(front, Pointing.RIGHT, 50).rightClick().withItem(example.input());
        scene.overlay().showText(60)
                .text("Right-click. Put accepted items in the left slot.")
                .pointAt(front)
                .placeNearTarget()
                .attachKeyFrame();
        moveItem(scene, front.add(-1.5, 0.5, 0), front, example.input(), 15);
        insertInput(scene, SINK, example);
        scene.idle(50);

        showPower(scene, util, SINK, Direction.EAST, 65);
        scene.overlay().showText(65)
                .text("Supply Forge Energy (FE) from a generator or battery.")
                .pointAt(util.vector().blockSurface(SINK, Direction.EAST))
                .placeNearTarget()
                .colored(PonderPalette.BLUE)
                .attachKeyFrame();
        animateConversion(scene, SINK, example);
        scene.idle(40);

        scene.overlay().showControls(front, Pointing.RIGHT, 50).withItem(example.output());
        scene.overlay().showText(60)
                .text("Each item becomes currency. Collect it from the right slots.")
                .pointAt(front)
                .placeNearTarget()
                .colored(PonderPalette.OUTPUT)
                .attachKeyFrame();
        moveItem(scene, front, front.add(-1.5, 0.5, 0), example.output(), 15);
        clearOutput(scene, SINK);
        scene.idle(50);
        scene.markAsFinished();
    }

    public static void createAutomation(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("item_sink_create_automation", "Automating with Belts and Funnels");
        scene.configureBasePlate(0, 0, 7);
        scene.scaleSceneView(0.9F);
        scene.showBasePlate();
        scene.world().setKineticSpeed(util.select().everywhere(), 0);
        scene.world().showSection(util.select().fromTo(0, 1, 1, 6, 2, 3), Direction.DOWN);
        Example example = example();
        scene.world().modifyBlockEntity(SOURCE_CHEST, ChestBlockEntity.class,
                chest -> chest.setItem(0, example.input().copyWithCount(16)));
        scene.idle(15);

        scene.overlay().showText(70)
                .text("Funnels feed items from the belt into the sink.")
                .pointAt(util.vector().centerOf(INPUT_BELT_END.above()))
                .placeNearTarget()
                .colored(PonderPalette.INPUT)
                .attachKeyFrame();
        scene.overlay().showControls(util.vector().blockSurface(CRANK, Direction.WEST), Pointing.RIGHT, 65)
                .rightClick();
        // Shafts and gearboxes drive both belts directly above the floor.
        // Negative rotation moves their items east, from input to output.
        scene.world().setKineticSpeed(util.select().everywhere(), -32);
        scene.effects().rotationDirectionIndicator(CRANK);
        scene.idle(15);

        scene.world().modifyBlockEntity(SOURCE_CHEST, ChestBlockEntity.class,
                chest -> chest.setItem(0, example.input().copyWithCount(15)));
        scene.world().createItemOnBelt(INPUT_BELT_START, Direction.WEST, example.input());
        scene.idle(20);
        scene.world().removeItemsFromBelt(INPUT_BELT_END);
        scene.world().flapFunnel(INPUT_BELT_END.above(), false);
        insertInput(scene, CREATE_SINK, example);
        scene.idle(40);

        showPower(scene, util, CREATE_SINK, Direction.SOUTH, 55);
        scene.overlay().showText(55)
                .text("Supply FE to the sink and rotation to the belts.")
                .pointAt(util.vector().blockSurface(CREATE_SINK, Direction.SOUTH))
                .placeNearTarget()
                .colored(PonderPalette.BLUE);
        animateConversion(scene, CREATE_SINK, example);
        scene.idle(30);

        scene.overlay().showText(70)
                .text("An extracting funnel sends currency onto the output belt.")
                .pointAt(util.vector().centerOf(OUTPUT_BELT_START.above()))
                .placeNearTarget()
                .colored(PonderPalette.OUTPUT)
                .attachKeyFrame();
        clearOutput(scene, CREATE_SINK);
        scene.world().createItemOnBelt(OUTPUT_BELT_START, Direction.WEST, example.output());
        scene.idle(20);
        scene.world().removeItemsFromBelt(OUTPUT_BELT_END);
        scene.world().flapFunnel(OUTPUT_BELT_END.above(), false);
        scene.world().modifyBlockEntity(OUTPUT_CHEST, ChestBlockEntity.class,
                chest -> chest.setItem(0, example.output().copy()));
        scene.effects().indicateSuccess(OUTPUT_CHEST);
        scene.idle(55);
        scene.markAsFinished();
    }

    private static void insertInput(SceneBuilder scene, BlockPos sinkPos, Example example) {
        scene.world().modifyBlockEntity(sinkPos, ItemSinkBlockEntity.class,
                sink -> sink.getInventory().setStackInSlot(ItemSinkBlockEntity.INPUT_SLOT, example.input().copy()));
    }

    private static void clearOutput(SceneBuilder scene, BlockPos sinkPos) {
        scene.world().modifyBlockEntity(sinkPos, ItemSinkBlockEntity.class,
                sink -> sink.getInventory().setStackInSlot(ItemSinkBlockEntity.OUTPUT_SLOT_1, ItemStack.EMPTY));
    }

    private static void showPower(SceneBuilder scene, SceneBuildingUtil util, BlockPos sinkPos,
                                  Direction side, int duration) {
        Vec3 port = util.vector().blockSurface(sinkPos, side);
        scene.overlay().showBigLine(PonderPalette.BLUE,
                port.add(Vec3.atLowerCornerOf(side.getNormal()).scale(1.5)), port, duration);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.BLUE, new Object(),
                new AABB(sinkPos).deflate(0.05), duration);
        scene.world().modifyBlockEntity(sinkPos, ItemSinkBlockEntity.class,
                sink -> sink.getData().set(2, Config.ENERGY_CAPACITY.get()));
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
