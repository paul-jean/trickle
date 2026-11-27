package dev.trickle.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.trickle.Trickle;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Coin balances and earning progress for every player, saved with the world in data/trickle/accounts.dat. */
public class TrickleState extends SavedData {
    public static class Account {
        public long balance;
        /** Active-play ticks counted toward the next payout. Kept across logouts. */
        public int progressTicks;

        Account() {}

        Account(long balance, int progressTicks) {
            this.balance = balance;
            this.progressTicks = progressTicks;
        }

        static final Codec<Account> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.LONG.optionalFieldOf("balance", 0L).forGetter(a -> a.balance),
                Codec.INT.optionalFieldOf("progress", 0).forGetter(a -> a.progressTicks)
        ).apply(i, Account::new));
    }

    private final Map<UUID, Account> accounts;

    public TrickleState() {
        this(new HashMap<>());
    }

    private TrickleState(Map<UUID, Account> accounts) {
        this.accounts = new HashMap<>(accounts);
    }

    private static final Codec<TrickleState> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, Account.CODEC)
                    .optionalFieldOf("players", Map.of()).forGetter(s -> s.accounts)
    ).apply(i, TrickleState::new));

    // No vanilla data fixer applies to mod data; Fabric API accepts null here.
    private static final SavedDataType<TrickleState> TYPE =
            new SavedDataType<>(Trickle.id("accounts"), TrickleState::new, CODEC, null);

    public static TrickleState get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
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
        setDirty();
    }
}
