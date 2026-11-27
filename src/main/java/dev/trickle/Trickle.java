package dev.trickle;

import dev.trickle.item.ModItems;
import dev.trickle.net.BalancePayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Trickle implements ModInitializer {
    public static final String MOD_ID = "trickle";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static TrickleConfig config;

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    public static TrickleConfig config() {
        return config;
    }

    public static void reloadConfig() {
        config = TrickleConfig.load();
    }

    @Override
    public void onInitialize() {
        reloadConfig();
        ModItems.init();
        PayloadTypeRegistry.playS2C().register(BalancePayload.ID, BalancePayload.CODEC);

        ServerTickEvents.END_SERVER_TICK.register(Economy::onServerTick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> Economy.onJoin(handler.getPlayer()));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> Economy.onLeave(handler.getPlayer()));
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, env) ->
                TrickleCommands.register(dispatcher));

        LOGGER.info("Trickle loaded: {} coin(s) every {}s, loot box costs {}",
                config.coinsPerPayout, config.secondsPerPayout, config.lootBoxCost);
    }
}
