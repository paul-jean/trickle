package dev.trickle.test;

import dev.trickle.Economy;
import dev.trickle.Trickle;
import dev.trickle.TrickleConfig;
import dev.trickle.item.ModItems;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public class TrickleGameTests {
    private static int countOf(ServerPlayer player, net.minecraft.world.item.Item item) {
        int n = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.is(item)) n += s.getCount();
        }
        return n;
    }

    private static int countOther(ServerPlayer player, net.minecraft.world.item.Item item) {
        int n = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (!s.isEmpty() && !s.is(item)) n += s.getCount();
        }
        return n;
    }

    @GameTest
    public void buyFailsWithoutEnoughCoins(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Economy.setBalance(player, Trickle.config().lootBoxCost - 1);
        helper.assertFalse(Economy.buyLootBoxes(player, 1), "buy should fail when short on coins");
        helper.assertValueEqual(Economy.balance(player), (long) Trickle.config().lootBoxCost - 1, "balance");
        helper.assertValueEqual(countOf(player, ModItems.LOOT_BOX), 0, "loot boxes");
        helper.succeed();
    }

    @GameTest
    public void buySpendsCoinsAndGivesBoxes(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        int cost = Trickle.config().lootBoxCost;
        Economy.setBalance(player, cost * 2L + 5);
        helper.assertTrue(Economy.buyLootBoxes(player, 2), "buy should succeed");
        helper.assertValueEqual(Economy.balance(player), 5L, "balance after buying");
        helper.assertValueEqual(countOf(player, ModItems.LOOT_BOX), 2, "loot boxes");
        helper.succeed();
    }

    @GameTest
    public void openingBoxGivesLoot(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.getAbilities().instabuild = false; // mock players are creative; make the box get used up
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.LOOT_BOX, 3));
        ModItems.LOOT_BOX.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertValueEqual(countOf(player, ModItems.LOOT_BOX), 2, "loot boxes left");
        helper.assertTrue(countOther(player, ModItems.LOOT_BOX) >= 2, "box should give at least 2 items");
        helper.succeed();
    }

    /** Both earning tests run at the same time, so they share one fast-payout setting. */
    private static void useFastPayouts() {
        TrickleConfig cfg = Trickle.config();
        cfg.secondsPerPayout = 1;
        cfg.afkTimeoutSeconds = 1;
    }

    @GameTest(maxTicks = 200)
    public void activePlayersEarnCoins(GameTestHelper helper) {
        useFastPayouts();
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Economy.setBalance(player, 0);
        // keep looking around so the player counts as active
        helper.onEachTick(() -> player.setYRot(player.getYRot() + 5f));
        helper.runAfterDelay(90, () -> {
            long balance = Economy.balance(player);
            helper.assertTrue(balance >= 3, "expected at least 3 coins after 4.5s of play, got " + balance);
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 200)
    public void idlePlayersStopEarning(GameTestHelper helper) {
        useFastPayouts();
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Economy.setBalance(player, 0);
        helper.runAfterDelay(100, () -> {
            long balance = Economy.balance(player);
            // a player who never moves earns at most the first second before being marked idle
            helper.assertTrue(balance <= 1, "idle player should stop earning, got " + balance);
            helper.succeed();
        });
    }
}
