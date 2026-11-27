package dev.trickle.net;

import dev.trickle.Trickle;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server to client: the player's current balance, the payout progress, and the loot box price. */
public record BalancePayload(long balance, float progress, int lootBoxCost) implements CustomPacketPayload {
    public static final Type<BalancePayload> TYPE = new Type<>(Trickle.id("balance"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BalancePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, BalancePayload::balance,
            ByteBufCodecs.FLOAT, BalancePayload::progress,
            ByteBufCodecs.VAR_INT, BalancePayload::lootBoxCost,
            BalancePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
