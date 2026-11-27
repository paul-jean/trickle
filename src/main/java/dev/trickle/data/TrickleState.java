package dev.trickle.data;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Coin balances and earning progress for every player, saved with the world. */
public class TrickleState extends PersistentState {
    public static class Account {
        public long balance;
        /** Active-play ticks counted toward the next payout. Kept across logouts. */
        public int progressTicks;
    }

    private final Map<UUID, Account> accounts = new HashMap<>();

    private static final Type<TrickleState> TYPE =
            new Type<>(TrickleState::new, TrickleState::fromNbt, null);

    public static TrickleState get(MinecraftServer server) {
        return server.getWorld(World.OVERWORLD).getPersistentStateManager().getOrCreate(TYPE, "trickle");
    }

    public Account account(UUID id) {
        return accounts.computeIfAbsent(id, k -> new Account());
    }

    public long balance(UUID id) {
        Account a = accounts.get(id);
        return a == null ? 0 : a.balance;
    }

    public void setBalance(UUID id, long value) {
        account(id).balance = Math.max(0, value);
        markDirty();
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        NbtCompound players = new NbtCompound();
        accounts.forEach((id, a) -> {
            NbtCompound tag = new NbtCompound();
            tag.putLong("balance", a.balance);
            tag.putInt("progress", a.progressTicks);
            players.put(id.toString(), tag);
        });
        nbt.put("players", players);
        return nbt;
    }

    private static TrickleState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        TrickleState state = new TrickleState();
        NbtCompound players = nbt.getCompound("players");
        for (String key : players.getKeys()) {
            try {
                NbtCompound tag = players.getCompound(key);
                Account a = new Account();
                a.balance = tag.getLong("balance");
                a.progressTicks = tag.getInt("progress");
                state.accounts.put(UUID.fromString(key), a);
            } catch (IllegalArgumentException ignored) {
                // skip malformed entries
            }
        }
        return state;
    }
}
