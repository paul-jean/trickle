package dev.trickle;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Collection;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/**
 * /trickle               show your balance
 * /trickle buy [count]   spend coins on loot boxes
 * /trickle give|take|set <players> <amount>   (operators)
 * /trickle reload        reload config/trickle.json (operators)
 */
public final class TrickleCommands {
    private TrickleCommands() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("trickle")
                .executes(TrickleCommands::balance)
                .then(literal("balance").executes(TrickleCommands::balance))
                .then(literal("buy")
                        .executes(ctx -> buy(ctx, 1))
                        .then(argument("count", IntegerArgumentType.integer(1, 64))
                                .executes(ctx -> buy(ctx, IntegerArgumentType.getInteger(ctx, "count")))))
                .then(literal("give").requires(s -> s.hasPermissionLevel(2))
                        .then(argument("players", EntityArgumentType.players())
                                .then(argument("amount", LongArgumentType.longArg(0))
                                        .executes(ctx -> adjust(ctx, Op.GIVE)))))
                .then(literal("take").requires(s -> s.hasPermissionLevel(2))
                        .then(argument("players", EntityArgumentType.players())
                                .then(argument("amount", LongArgumentType.longArg(0))
                                        .executes(ctx -> adjust(ctx, Op.TAKE)))))
                .then(literal("set").requires(s -> s.hasPermissionLevel(2))
                        .then(argument("players", EntityArgumentType.players())
                                .then(argument("amount", LongArgumentType.longArg(0))
                                        .executes(ctx -> adjust(ctx, Op.SET)))))
                .then(literal("reload").requires(s -> s.hasPermissionLevel(2))
                        .executes(ctx -> {
                            Trickle.reloadConfig();
                            ctx.getSource().getServer().getPlayerManager().getPlayerList().forEach(Economy::sync);
                            ctx.getSource().sendFeedback(() -> Text.literal("Trickle config reloaded"), true);
                            return 1;
                        })));
    }

    private static int balance(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
        long balance = Economy.balance(player);
        ctx.getSource().sendFeedback(() -> Text.translatable("trickle.msg.balance",
                balance, Trickle.config().lootBoxCost), false);
        return (int) Math.min(Integer.MAX_VALUE, balance);
    }

    private static int buy(CommandContext<ServerCommandSource> ctx, int count) throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
        long cost = (long) Trickle.config().lootBoxCost * count;
        if (!Economy.buyLootBoxes(player, count)) {
            ctx.getSource().sendError(Text.translatable("trickle.msg.too_poor",
                    count, cost, Economy.balance(player)));
            return 0;
        }
        ctx.getSource().sendFeedback(() -> Text.translatable("trickle.msg.bought",
                count, cost, Economy.balance(player)), false);
        return count;
    }

    private enum Op { GIVE, TAKE, SET }

    private static int adjust(CommandContext<ServerCommandSource> ctx, Op op) throws CommandSyntaxException {
        Collection<ServerPlayerEntity> players = EntityArgumentType.getPlayers(ctx, "players");
        long amount = LongArgumentType.getLong(ctx, "amount");
        for (ServerPlayerEntity p : players) {
            long current = Economy.balance(p);
            long next = switch (op) {
                case GIVE -> current + amount;
                case TAKE -> Math.max(0, current - amount);
                case SET -> amount;
            };
            Economy.setBalance(p, next);
        }
        ctx.getSource().sendFeedback(() -> Text.literal("Updated Trickle balance for "
                + players.size() + " player(s)"), true);
        return players.size();
    }
}
