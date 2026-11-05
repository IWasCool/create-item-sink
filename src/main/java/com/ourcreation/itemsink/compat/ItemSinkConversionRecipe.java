package com.ourcreation.itemsink.compat;

import com.ourcreation.itemsink.Config;
import com.ourcreation.itemsink.ItemSink;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.List;

/** A single JEI overview, with one icon for each available currency. */
public record ItemSinkConversionRecipe(ResourceLocation id, List<ItemStack> outputs) {
    public static List<ItemSinkConversionRecipe> fromConfig() {
        var outputs = new LinkedHashMap<ResourceLocation, ItemStack>();

        for (Config.Exchange exchange : Config.exchanges()) {
            if (!BuiltInRegistries.ITEM.containsKey(exchange.input())) {
                continue;
            }
            ItemStack output = exchange.createOutput();
            if (!output.isEmpty()) {
                // Icons represent possible currencies, not a fixed payout for
                // every input. Different tiers can share the same currency.
                outputs.putIfAbsent(exchange.output(), new ItemStack(output.getItem()));
            }
        }

        if (outputs.isEmpty()) {
            return List.of();
        }
        return List.of(new ItemSinkConversionRecipe(
                ResourceLocation.fromNamespaceAndPath(ItemSink.MODID, "item_sink_conversion/overview"),
                Config.configuredCurrencies().stream().map(outputs::get).filter(stack -> stack != null).toList()));
    }
}
