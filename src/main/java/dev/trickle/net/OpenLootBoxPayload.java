package dev.trickle.net;

import dev.trickle.Trickle;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;

/** Client to server: the player confirmed opening the loot box held in this hand. */
public record OpenLootBoxPayload(InteractionHand hand) implements CustomPacketPayload {
    public static final Type<OpenLootBoxPayload> TYPE = new Type<>(Trickle.id("open_loot_box"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenLootBoxPayload> CODEC = ByteBufCodecs.VAR_INT
            .map(i -> new OpenLootBoxPayload(i == 1 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND),
                    p -> p.hand() == InteractionHand.OFF_HAND ? 1 : 0)
            .cast();

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
