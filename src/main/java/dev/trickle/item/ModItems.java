package dev.trickle.item;

import dev.trickle.Trickle;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Rarity;

public final class ModItems {
    public static final Item LOOT_BOX = Registry.register(Registries.ITEM, Trickle.id("loot_box"),
            new LootBoxItem(new Item.Settings().maxCount(64).rarity(Rarity.RARE)));

    private ModItems() {}

    public static void init() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(entries -> entries.add(LOOT_BOX));
    }
}
