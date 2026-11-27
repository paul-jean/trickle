package dev.trickle.item;

import dev.trickle.Trickle;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.function.Consumer;

/** A box that rolls its tier's loot table (trickle:<tier id>) when right-clicked. Datapacks can override the tables. */
public class LootBoxItem extends Item {
    private final LootBoxTier tier;
    private final ResourceKey<LootTable> lootTable;

    public LootBoxItem(LootBoxTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
        this.lootTable = ResourceKey.create(Registries.LOOT_TABLE, Trickle.id(tier.id));
    }

    public LootBoxTier tier() {
        return tier;
    }

    /**
     * Set by the client mod: shows the "open this box?" dialog. It is null on a dedicated server.
     */
    public static ClientConfirmHook clientConfirmHook;

    @FunctionalInterface
    public interface ClientConfirmHook {
        void askToOpen(LootBoxItem item, ItemStack stack, InteractionHand hand);
    }

    /**
     * Right-click asks for confirmation first; the client then sends OpenLootBoxPayload and the server opens the box.
     * Sneak + right-click skips the question and opens straight away.
     */
    @Override
    public InteractionResult use(Level level, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);
        if (user.isShiftKeyDown()) {
            if (user instanceof ServerPlayer player) open(player, hand);
            return InteractionResult.SUCCESS;
        }
        if (level.isClientSide() && clientConfirmHook != null) {
            clientConfirmHook.askToOpen(this, stack, hand);
        }
        return InteractionResult.CONSUME;
    }

    /** Opens the box in the given hand: rolls its loot table, gives the items, and uses up one box. */
    public static boolean open(ServerPlayer player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof LootBoxItem box)) return false;
        var serverLevel = player.level();
        LootTable table = serverLevel.getServer().reloadableRegistries().getLootTable(box.lootTable);
        LootParams params = new LootParams.Builder(serverLevel)
                .withParameter(LootContextParams.ORIGIN, player.position())
                .withParameter(LootContextParams.THIS_ENTITY, player)
                .withLuck(player.getLuck())
                .create(LootContextParamSets.GIFT);
        stack.consume(1, player);
        for (ItemStack drop : table.getRandomItems(params)) {
            player.getInventory().placeItemBackInInventory(drop, Prediction.SERVER_ONLY);
        }
        float pitch = switch (box.tier) { case COMMON -> 1.4f; case RARE -> 1.0f; case EPIC -> 0.7f; };
        serverLevel.playSound(null, player.blockPosition(),
                box.tier == LootBoxTier.EPIC ? SoundEvents.UI_TOAST_CHALLENGE_COMPLETE : SoundEvents.PLAYER_LEVELUP,
                SoundSource.PLAYERS, 0.6f, pitch);
        player.sendOverlayMessage(Component.translatable("trickle.msg.opened").withStyle(ChatFormatting.GOLD));
        return true;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("item.trickle.loot_box.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
