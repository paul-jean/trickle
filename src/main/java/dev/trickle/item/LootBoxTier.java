package dev.trickle.item;

import dev.trickle.TrickleConfig;
import net.minecraft.world.item.Rarity;

import java.util.Locale;

/** The loot box tiers. Each has its own item, loot table (data/trickle/loot_table/<id>.json) and price. */
public enum LootBoxTier {
    COMMON("loot_box", Rarity.UNCOMMON),
    RARE("rare_loot_box", Rarity.RARE),
    EPIC("epic_loot_box", Rarity.EPIC);

    public final String id;
    public final Rarity rarity;

    LootBoxTier(String id, Rarity rarity) {
        this.id = id;
        this.rarity = rarity;
    }

    /** Lowercase name used in commands, e.g. "rare". */
    public String commandName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public int cost(TrickleConfig cfg) {
        return switch (this) {
            case COMMON -> cfg.lootBoxCost;
            case RARE -> cfg.rareLootBoxCost;
            case EPIC -> cfg.epicLootBoxCost;
        };
    }
}
