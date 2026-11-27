package dev.trickle;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/** Server-side settings, stored in config/trickle.json. */
public class TrickleConfig {
    /** Seconds of active play needed to earn one payout. */
    public int secondsPerPayout = 300;
    /** Coins given per payout. */
    public int coinsPerPayout = 1;
    /** Coins a common loot box costs. */
    public int lootBoxCost = 10;
    /** Coins a rare loot box costs. */
    public int rareLootBoxCost = 50;
    /** Coins an epic loot box costs. */
    public int epicLootBoxCost = 150;
    /** Players who haven't moved or looked around for this long stop earning. 0 disables the check. */
    public int afkTimeoutSeconds = 180;
    /** Highest balance a player can hold. 0 means no cap. */
    public long maxBalance = 0;
    /** Tell the player in chat each time they earn coins. */
    public boolean announcePayouts = false;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static TrickleConfig load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("trickle.json");
        TrickleConfig config = new TrickleConfig();
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                TrickleConfig loaded = GSON.fromJson(reader, TrickleConfig.class);
                if (loaded != null) config = loaded;
            } catch (IOException | RuntimeException e) {
                Trickle.LOGGER.error("Could not read {}, using defaults", path, e);
            }
        }
        config.sanitize();
        try (Writer writer = Files.newBufferedWriter(path)) {
            GSON.toJson(config, writer);
        } catch (IOException e) {
            Trickle.LOGGER.warn("Could not write {}", path, e);
        }
        return config;
    }

    private void sanitize() {
        secondsPerPayout = Math.max(1, secondsPerPayout);
        coinsPerPayout = Math.max(0, coinsPerPayout);
        lootBoxCost = Math.max(0, lootBoxCost);
        rareLootBoxCost = Math.max(0, rareLootBoxCost);
        epicLootBoxCost = Math.max(0, epicLootBoxCost);
        afkTimeoutSeconds = Math.max(0, afkTimeoutSeconds);
        maxBalance = Math.max(0, maxBalance);
    }
}
