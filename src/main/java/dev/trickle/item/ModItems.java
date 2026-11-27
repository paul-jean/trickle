package dev.trickle.item;

import dev.trickle.Trickle;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

public final class ModItems {
    private static final ResourceKey<Item> LOOT_BOX_KEY = ResourceKey.create(Registries.ITEM, Trickle.id("loot_box"));

    public static final Item LOOT_BOX = Registry.register(BuiltInRegistries.ITEM, LOOT_BOX_KEY,
            new LootBoxItem(new Item.Properties().setId(LOOT_BOX_KEY).stacksTo(64).rarity(Rarity.RARE)));

    private ModItems() {}

    public static void init() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
                .register(output -> output.accept(LOOT_BOX));
    }
}
