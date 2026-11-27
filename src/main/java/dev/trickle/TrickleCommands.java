package dev.trickle;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import dev.trickle.item.LootBoxTier;
import dev.trickle.item.ModItems;

import java.util.Collection;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * /trickle               show your balance
 * /trickle buy [count]   buy common loot boxes
 * /trickle buy common|rare|epic [count]   buy loot boxes of a tier
 * /trickle shop          list loot box prices
 * /trickle give|take|set <players> <amount>   (operators)
 * /trickle reload        reload config/trickle.json (operators)
 */
public final class TrickleCommands {
    private TrickleCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("trickle")
                .executes(TrickleCommands::balance)
                .then(literal("balance").executes(TrickleCommands::balance))
                .then(literal("shop").executes(TrickleCommands::shop))
                .then(buyCommand())
                .then(literal("give").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(argument("players", EntityArgument.players())
                                .then(argument("amount", LongArgumentType.longArg(0))
                                        .executes(ctx -> adjust(ctx, Op.GIVE)))))
                .then(literal("take").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(argument("players", EntityArgument.players())
                                .then(argument("amount", LongArgumentType.longArg(0))
                                        .executes(ctx -> adjust(ctx, Op.TAKE)))))
                .then(literal("set").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(argument("players", EntityArgument.players())
                                .then(argument("amount", LongArgumentType.longArg(0))
                                        .executes(ctx -> adjust(ctx, Op.SET)))))
                .then(literal("reload").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(ctx -> {
                            Trickle.reloadConfig();
                            ctx.getSource().getServer().getPlayerList().getPlayers().forEach(Economy::sync);
                            ctx.getSource().sendSuccess(() -> Component.literal("Trickle config reloaded"), true);
                            return 1;
                        })));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buyCommand() {
        LiteralArgumentBuilder<CommandSourceStack> buy = literal("buy")
                .executes(ctx -> buy(ctx, LootBoxTier.COMMON, 1))
                .then(argument("count", IntegerArgumentType.integer(1, 64))
                        .executes(ctx -> buy(ctx, LootBoxTier.COMMON, IntegerArgumentType.getInteger(ctx, "count"))));
        for (LootBoxTier tier : LootBoxTier.values()) {
            buy.then(literal(tier.commandName())
                    .executes(ctx -> buy(ctx, tier, 1))
                    .then(argument("count", IntegerArgumentType.integer(1, 64))
                            .executes(ctx -> buy(ctx, tier, IntegerArgumentType.getInteger(ctx, "count")))));
        }
        return buy;
    }

    private static int shop(CommandContext<CommandSourceStack> ctx) {
        TrickleConfig cfg = Trickle.config();
        ctx.getSource().sendSuccess(() -> Component.translatable("trickle.msg.shop"), false);
        for (LootBoxTier tier : LootBoxTier.values()) {
            ctx.getSource().sendSuccess(() -> Component.translatable("trickle.msg.shop_line",
                    ModItems.lootBox(tier).getName(new ItemStack(ModItems.lootBox(tier))),
                    tier.cost(cfg), tier.commandName()), false);
        }
        return 1;
    }

    private static int balance(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        long balance = Economy.balance(player);
        ctx.getSource().sendSuccess(() -> Component.translatable("trickle.msg.balance", balance), false);
        shop(ctx);
        return (int) Math.min(Integer.MAX_VALUE, balance);
    }

    private static int buy(CommandContext<CommandSourceStack> ctx, LootBoxTier tier, int count) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        long cost = (long) tier.cost(Trickle.config()) * count;
        Component name = ModItems.lootBox(tier).getName(new ItemStack(ModItems.lootBox(tier)));
        if (!Economy.buyLootBoxes(player, tier, count)) {
            ctx.getSource().sendFailure(Component.translatable("trickle.msg.too_poor",
                    count, name, cost, Economy.balance(player)));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("trickle.msg.bought",
                count, name, cost, Economy.balance(player)), false);
        return count;
    }

    private enum Op { GIVE, TAKE, SET }

    private static int adjust(CommandContext<CommandSourceStack> ctx, Op op) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "players");
        long amount = LongArgumentType.getLong(ctx, "amount");
        for (ServerPlayer p : players) {
            long current = Economy.balance(p);
            long next = switch (op) {
                case GIVE -> current + amount;
                case TAKE -> Math.max(0, current - amount);
                case SET -> amount;
            };
            Economy.setBalance(p, next);
        }
        ctx.getSource().sendSuccess(() -> Component.literal("Updated Trickle balance for "
                + players.size() + " player(s)"), true);
        return players.size();
    }
}
