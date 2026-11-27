package dev.trickle.item;

import dev.trickle.Trickle;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

import java.util.EnumMap;
import java.util.Map;

public final class ModItems {
    private static final Map<LootBoxTier, Item> LOOT_BOXES = new EnumMap<>(LootBoxTier.class);

    static {
        for (LootBoxTier tier : LootBoxTier.values()) {
            ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Trickle.id(tier.id));
            LOOT_BOXES.put(tier, Registry.register(BuiltInRegistries.ITEM, key,
                    new LootBoxItem(tier, new Item.Properties().setId(key).stacksTo(64).rarity(tier.rarity))));
        }
    }

    public static final Item LOOT_BOX = lootBox(LootBoxTier.COMMON);
    public static final Item RARE_LOOT_BOX = lootBox(LootBoxTier.RARE);
    public static final Item EPIC_LOOT_BOX = lootBox(LootBoxTier.EPIC);

    private ModItems() {}

    public static Item lootBox(LootBoxTier tier) {
        return LOOT_BOXES.get(tier);
    }

    public static void init() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> {
            for (LootBoxTier tier : LootBoxTier.values()) output.accept(lootBox(tier));
        });
    }
}
