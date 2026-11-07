package com.ourcreation.itemsink.valuescan;

import com.google.gson.GsonBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import com.ourcreation.itemsink.Config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;

/** Development-only registry audit. Never included in the released mod. */
@Mod("item_sink_value_scan")
public final class ValueRegistryScan {
    public ValueRegistryScan(IEventBus bus) {
        if (Boolean.getBoolean("item_sink.scan.validate")) {
            // Tags do not exist yet during FMLLoadCompleteEvent.
            NeoForge.EVENT_BUS.addListener(this::validateTags);
        } else {
            bus.addListener(this::scan);
        }
    }

    private void scan(FMLLoadCompleteEvent event) {
        event.enqueueWork(this::writeScan);
    }

    private void validateTags(TagsUpdatedEvent event) {
        Config.invalidateExchanges();
        writeScan();
    }

    private void writeScan() {
            try {
                var items = new ArrayList<Object>();
                BuiltInRegistries.ITEM.entrySet().stream()
                        .sorted(java.util.Comparator.comparing(entry -> entry.getKey().location().toString()))
                        .forEach(entry -> {
                            Item item = entry.getValue();
                            var row = new LinkedHashMap<String, Object>();
                            row.put("id", entry.getKey().location().toString());
                            row.put("class", item.getClass().getSimpleName());
                            row.put("block", item instanceof BlockItem);
                            row.put("rarity", item.getDefaultInstance().getRarity().name());
                            row.put("stack", item.getDefaultMaxStackSize());
                            var food = item.getDefaultInstance().get(DataComponents.FOOD);
                            row.put("food", food == null ? 0 : food.nutrition());
                            items.add(row);
                        });
                var result = new LinkedHashMap<String, Object>();
                result.put("mods", ModList.get().getMods().stream().map(mod -> mod.getModId()).sorted().toList());
                result.put("items", items);
                Path target = Path.of(System.getProperty("item_sink.scan.output"));
                Files.createDirectories(target.getParent());
                Files.writeString(target, new GsonBuilder().setPrettyPrinting().create().toJson(result));
                if (Boolean.getBoolean("item_sink.scan.validate")) {
                    ValueExchangeChecks.run();
                }
                System.out.println("ITEM_SINK_SCAN_COMPLETE: " + items.size() + " registered items -> " + target);
                System.exit(0);
            } catch (Throwable error) {
                error.printStackTrace();
                System.exit(1);
            }
    }
}
