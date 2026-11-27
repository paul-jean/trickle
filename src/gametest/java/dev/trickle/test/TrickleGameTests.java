package dev.trickle.test;

import dev.trickle.Economy;
import dev.trickle.Trickle;
import dev.trickle.TrickleConfig;
import dev.trickle.item.LootBoxItem;
import dev.trickle.item.LootBoxTier;
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
        helper.assertFalse(Economy.buyLootBoxes(player, LootBoxTier.COMMON, 1), "buy should fail when short on coins");
        helper.assertValueEqual(Economy.balance(player), (long) Trickle.config().lootBoxCost - 1, "balance");
        helper.assertValueEqual(countOf(player, ModItems.LOOT_BOX), 0, "loot boxes");
        helper.succeed();
    }

    @GameTest
    public void buySpendsCoinsAndGivesBoxes(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        int cost = Trickle.config().lootBoxCost;
        Economy.setBalance(player, cost * 2L + 5);
        helper.assertTrue(Economy.buyLootBoxes(player, LootBoxTier.COMMON, 2), "buy should succeed");
        helper.assertValueEqual(Economy.balance(player), 5L, "balance after buying");
        helper.assertValueEqual(countOf(player, ModItems.LOOT_BOX), 2, "loot boxes");
        helper.succeed();
    }

    @GameTest
    public void openingBoxGivesLoot(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.getAbilities().instabuild = false; // mock players are creative; make the box get used up
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.LOOT_BOX, 3));
        LootBoxItem.open(player, InteractionHand.MAIN_HAND);
        helper.assertValueEqual(countOf(player, ModItems.LOOT_BOX), 2, "loot boxes left");
        helper.assertTrue(countOther(player, ModItems.LOOT_BOX) >= 2, "box should give at least 2 items");
        helper.succeed();
    }

    @GameTest
    public void rightClickWaitsForConfirmation(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.getAbilities().instabuild = false;
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.LOOT_BOX, 2));
        // plain right-click on the server does nothing: the client asks first and then sends OpenLootBoxPayload
        ModItems.LOOT_BOX.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertValueEqual(countOf(player, ModItems.LOOT_BOX), 2, "boxes after unconfirmed right-click");
        helper.assertValueEqual(countOther(player, ModItems.LOOT_BOX), 0, "loot after unconfirmed right-click");
        // sneak + right-click skips the question
        player.setShiftKeyDown(true);
        ModItems.LOOT_BOX.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertValueEqual(countOf(player, ModItems.LOOT_BOX), 1, "boxes after sneak right-click");
        helper.assertTrue(countOther(player, ModItems.LOOT_BOX) >= 2, "sneak right-click should open the box");
        helper.succeed();
    }

    @GameTest
    public void confirmingWithoutABoxDoesNothing(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(net.minecraft.world.item.Items.DIRT, 1));
        helper.assertFalse(LootBoxItem.open(player, InteractionHand.MAIN_HAND), "should refuse when not holding a box");
        helper.assertValueEqual(countOf(player, net.minecraft.world.item.Items.DIRT), 1, "dirt untouched");
        helper.succeed();
    }

    @GameTest
    public void tiersHaveTheirOwnPrices(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        TrickleConfig cfg = Trickle.config();
        Economy.setBalance(player, LootBoxTier.EPIC.cost(cfg) - 1);
        helper.assertFalse(Economy.buyLootBoxes(player, LootBoxTier.EPIC, 1), "epic box should be unaffordable");
        helper.assertTrue(Economy.buyLootBoxes(player, LootBoxTier.RARE, 1), "rare box should be affordable");
        helper.assertValueEqual(Economy.balance(player),
                (long) LootBoxTier.EPIC.cost(cfg) - 1 - LootBoxTier.RARE.cost(cfg), "balance after rare box");
        helper.assertValueEqual(countOf(player, ModItems.RARE_LOOT_BOX), 1, "rare loot boxes");
        helper.assertValueEqual(countOf(player, ModItems.EPIC_LOOT_BOX), 0, "epic loot boxes");
        helper.succeed();
    }

    @GameTest
    public void rareBoxGivesMoreLoot(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.getAbilities().instabuild = false;
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.RARE_LOOT_BOX, 1));
        LootBoxItem.open(player, InteractionHand.MAIN_HAND);
        helper.assertValueEqual(countOf(player, ModItems.RARE_LOOT_BOX), 0, "rare boxes left");
        // 3-4 rolls plus a guaranteed enchanted diamond piece (identical rolls may merge into one stack)
        helper.assertTrue(countOther(player, ModItems.RARE_LOOT_BOX) >= 4, "rare box should give at least 4 items, got " + describe(player));
        helper.assertTrue(hasEnchantedDiamondGear(player), "rare box should include enchanted diamond gear");
        helper.succeed();
    }

    @GameTest
    public void epicBoxGivesJackpotLoot(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.getAbilities().instabuild = false;
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.EPIC_LOOT_BOX, 1));
        LootBoxItem.open(player, InteractionHand.MAIN_HAND);
        helper.assertValueEqual(countOf(player, ModItems.EPIC_LOOT_BOX), 0, "epic boxes left");
        // a jackpot item, an enchanted piece of gear, and 2-3 more rolls (identical rolls may merge)
        helper.assertTrue(countOther(player, ModItems.EPIC_LOOT_BOX) >= 4, "epic box should give at least 4 items, got " + describe(player));
        helper.assertTrue(stacksOther(player, ModItems.EPIC_LOOT_BOX) >= 3, "epic box should give at least 3 stacks, got " + describe(player));
        helper.assertTrue(hasEnchantedDiamondGear(player) || has(player, net.minecraft.world.item.Items.BOW),
                "epic box should include enchanted gear");
        helper.succeed();
    }

    @GameTest
    public void stackCountsComeFromLootTable(GameTestHelper helper) {
        // Opening many common boxes should produce some stacks bigger than 1 (bread is 3-8),
        // which proves the loot table's count modifiers are applied.
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.getAbilities().instabuild = false;
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.LOOT_BOX, 10));
        for (int i = 0; i < 10; i++) LootBoxItem.open(player, InteractionHand.MAIN_HAND);
        boolean bigStack = false;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (!s.is(ModItems.LOOT_BOX) && s.getCount() > 1) bigStack = true;
        }
        helper.assertTrue(bigStack, "expected some stacks larger than 1 from 10 common boxes");
        helper.succeed();
    }

    private static int stacksOther(ServerPlayer player, net.minecraft.world.item.Item item) {
        int n = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (!s.isEmpty() && !s.is(item)) n++;
        }
        return n;
    }

    private static String describe(ServerPlayer player) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (!s.isEmpty()) b.append(s.getCount()).append("x").append(s.getItem()).append(s.isEnchanted() ? "*" : "").append(", ");
        }
        return b.toString();
    }

    private static boolean has(ServerPlayer player, net.minecraft.world.item.Item item) {
        return countOf(player, item) > 0;
    }

    private static boolean hasEnchantedDiamondGear(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem()).getPath();
            if (id.startsWith("diamond_") && !id.equals("diamond_block") && s.isEnchanted()) return true;
        }
        return false;
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
