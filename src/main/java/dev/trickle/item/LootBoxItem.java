package dev.trickle.item;

import dev.trickle.Trickle;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

/** A box that rolls the trickle:loot_box loot table when right-clicked. Server packs can override that table. */
public class LootBoxItem extends Item {
    public static final RegistryKey<LootTable> LOOT_TABLE =
            RegistryKey.of(RegistryKeys.LOOT_TABLE, Trickle.id("loot_box"));

    public LootBoxItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world instanceof ServerWorld serverWorld && user instanceof ServerPlayerEntity player) {
            LootTable table = serverWorld.getServer().getReloadableRegistries().getLootTable(LOOT_TABLE);
            LootContextParameterSet params = new LootContextParameterSet.Builder(serverWorld)
                    .add(LootContextParameters.ORIGIN, player.getPos())
                    .add(LootContextParameters.THIS_ENTITY, player)
                    .luck(player.getLuck())
                    .build(LootContextTypes.GIFT);
            for (ItemStack drop : table.generateLoot(params)) {
                player.getInventory().offerOrDrop(drop);
            }
            stack.decrementUnlessCreative(1, player);
            world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_PLAYER_LEVELUP,
                    SoundCategory.PLAYERS, 0.6f, 1.4f);
            player.sendMessage(Text.translatable("trickle.msg.opened").formatted(Formatting.GOLD), true);
        }
        return TypedActionResult.success(stack, world.isClient());
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("item.trickle.loot_box.tooltip").formatted(Formatting.GRAY));
    }
}
