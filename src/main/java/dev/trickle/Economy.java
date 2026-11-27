package dev.trickle;

import dev.trickle.data.TrickleState;
import dev.trickle.item.ModItems;
import dev.trickle.net.BalancePayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Pays coins to players who are actively playing, and handles spending them. All of this runs on the server. */
public final class Economy {
    private static final int CHECK_INTERVAL = 20;   // check once a second
    private static final int SYNC_INTERVAL = 100;   // refresh the HUD every 5 seconds

    private record Look(double x, double y, double z, float yaw, float pitch) {
        static Look of(ServerPlayer p) {
            return new Look(p.getX(), p.getY(), p.getZ(), p.getYRot(), p.getXRot());
        }
    }

    private static final Map<UUID, Look> lastLook = new HashMap<>();
    private static final Map<UUID, Long> lastActiveTick = new HashMap<>();
    private static long tick;

    private Economy() {}

    public static void onServerTick(MinecraftServer server) {
        tick++;
        if (tick % CHECK_INTERVAL != 0) return;
        TrickleConfig cfg = Trickle.config();
        TrickleState state = TrickleState.get(server);
        boolean sync = tick % SYNC_INTERVAL == 0;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID id = player.getUUID();
            if (player.isSpectator() || !isActive(player, cfg)) {
                if (sync) sync(player);
                continue;
            }
            TrickleState.Account account = state.account(id);
            account.progressTicks += CHECK_INTERVAL;
            int needed = cfg.secondsPerPayout * 20;
            boolean changed = false;
            while (account.progressTicks >= needed) {
                account.progressTicks -= needed;
                long before = account.balance;
                account.balance = cap(account.balance + cfg.coinsPerPayout, cfg);
                if (account.balance != before) {
                    changed = true;
                    if (cfg.announcePayouts) {
                        player.sendOverlayMessage(Component.translatable("trickle.msg.earned",
                                account.balance - before, account.balance));
                    }
                }
            }
            state.setDirty();
            if (changed || sync) sync(player);
        }
    }

    private static boolean isActive(ServerPlayer player, TrickleConfig cfg) {
        UUID id = player.getUUID();
        Look now = Look.of(player);
        if (!now.equals(lastLook.get(id))) {
            lastLook.put(id, now);
            lastActiveTick.put(id, tick);
        }
        if (cfg.afkTimeoutSeconds == 0) return true;
        long idle = tick - lastActiveTick.getOrDefault(id, tick);
        return idle < cfg.afkTimeoutSeconds * 20L;
    }

    private static long cap(long value, TrickleConfig cfg) {
        return cfg.maxBalance > 0 ? Math.min(value, cfg.maxBalance) : value;
    }

    public static void onJoin(ServerPlayer player) {
        lastLook.put(player.getUUID(), Look.of(player));
        lastActiveTick.put(player.getUUID(), tick);
        sync(player);
    }

    public static void onLeave(ServerPlayer player) {
        lastLook.remove(player.getUUID());
        lastActiveTick.remove(player.getUUID());
    }

    public static long balance(ServerPlayer player) {
        return TrickleState.get(player.level().getServer()).balance(player.getUUID());
    }

    public static void setBalance(ServerPlayer player, long value) {
        TrickleState.get(player.level().getServer()).setBalance(player.getUUID(), cap(value, Trickle.config()));
        sync(player);
    }

    /** Spends coins on loot boxes. Returns false, changing nothing, if the player can't afford them. */
    public static boolean buyLootBoxes(ServerPlayer player, int count) {
        TrickleState state = TrickleState.get(player.level().getServer());
        long cost = (long) Trickle.config().lootBoxCost * count;
        long balance = state.balance(player.getUUID());
        if (balance < cost) return false;
        state.setBalance(player.getUUID(), balance - cost);
        int left = count;
        while (left > 0) {
            int n = Math.min(left, ModItems.LOOT_BOX.getDefaultMaxStackSize());
            player.getInventory().placeItemBackInInventory(new ItemStack(ModItems.LOOT_BOX, n), Prediction.SERVER_ONLY);
            left -= n;
        }
        sync(player);
        return true;
    }

    public static void sync(ServerPlayer player) {
        if (!ServerPlayNetworking.canSend(player, BalancePayload.TYPE)) return; // player lacks the client mod
        TrickleConfig cfg = Trickle.config();
        TrickleState.Account a = TrickleState.get(player.level().getServer()).account(player.getUUID());
        float progress = Math.min(1f, a.progressTicks / (cfg.secondsPerPayout * 20f));
        ServerPlayNetworking.send(player, new BalancePayload(a.balance, progress, cfg.lootBoxCost));
    }
}
