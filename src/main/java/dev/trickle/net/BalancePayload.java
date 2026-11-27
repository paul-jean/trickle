package dev.trickle.net;

import dev.trickle.Trickle;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/** Server to client: the player's current balance, the payout progress, and the loot box price. */
public record BalancePayload(long balance, float progress, int lootBoxCost) implements CustomPayload {
    public static final Id<BalancePayload> ID = new Id<>(Trickle.id("balance"));
    public static final PacketCodec<RegistryByteBuf, BalancePayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_LONG, BalancePayload::balance,
            PacketCodecs.FLOAT, BalancePayload::progress,
            PacketCodecs.VAR_INT, BalancePayload::lootBoxCost,
            BalancePayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
