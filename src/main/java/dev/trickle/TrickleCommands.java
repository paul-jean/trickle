package dev.trickle;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * /trickle               show your balance
 * /trickle buy [count]   spend coins on loot boxes
 * /trickle give|take|set <players> <amount>   (operators)
 * /trickle reload        reload config/trickle.json (operators)
 */
public final class TrickleCommands {
    private TrickleCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("trickle")
                .executes(TrickleCommands::balance)
                .then(literal("balance").executes(TrickleCommands::balance))
                .then(literal("buy")
                        .executes(ctx -> buy(ctx, 1))
                        .then(argument("count", IntegerArgumentType.integer(1, 64))
                                .executes(ctx -> buy(ctx, IntegerArgumentType.getInteger(ctx, "count")))))
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

    private static int balance(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        long balance = Economy.balance(player);
        ctx.getSource().sendSuccess(() -> Component.translatable("trickle.msg.balance",
                balance, Trickle.config().lootBoxCost), false);
        return (int) Math.min(Integer.MAX_VALUE, balance);
    }

    private static int buy(CommandContext<CommandSourceStack> ctx, int count) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        long cost = (long) Trickle.config().lootBoxCost * count;
        if (!Economy.buyLootBoxes(player, count)) {
            ctx.getSource().sendFailure(Component.translatable("trickle.msg.too_poor",
                    count, cost, Economy.balance(player)));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("trickle.msg.bought",
                count, cost, Economy.balance(player)), false);
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
