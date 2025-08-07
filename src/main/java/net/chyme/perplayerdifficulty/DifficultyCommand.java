package net.chyme.perplayerdifficulty;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.Difficulty;

import java.util.concurrent.CompletableFuture;

public class DifficultyCommand {

    private static final SuggestionProvider<ServerCommandSource> DIFFICULTY_SUGGESTIONS = (context, builder) -> {
        return CommandSource.suggestMatching(
                new String[]{"peaceful", "easy", "normal", "hard"},
                builder
        );
    };

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("playerdifficulty")
                        .then(CommandManager.literal("set")
                                .then(CommandManager.argument("difficulty", StringArgumentType.word())
                                        .suggests(DIFFICULTY_SUGGESTIONS)
                                        .executes(DifficultyCommand::setDifficulty)
                                )
                        )
                        .then(CommandManager.literal("get")
                                .executes(DifficultyCommand::getDifficulty)
                        )
                        .then(CommandManager.literal("reset")
                                .executes(DifficultyCommand::resetDifficulty)
                        )
                        .executes(DifficultyCommand::showHelp)
        );
    }

    private static int setDifficulty(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();
        String difficultyName = StringArgumentType.getString(context, "difficulty");

        Difficulty difficulty = Difficulty.byName(difficultyName);
        if (difficulty == null) {
            source.sendError(Text.literal("Invalid difficulty! Use: peaceful, easy, normal, or hard"));
            return 0;
        }

        PlayerDifficultyManager.setPlayerDifficulty(player, difficulty);
        source.sendFeedback(() -> Text.literal("Your difficulty has been set to " + difficulty.getName()), false);
        return 1;
    }

    private static int getDifficulty(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();

        Difficulty playerDifficulty = PlayerDifficultyManager.getPlayerDifficulty(player);
        Difficulty serverDifficulty = PlayerDifficultyManager.getServerDifficulty();

        source.sendFeedback(() -> Text.literal("Your difficulty: " + playerDifficulty.getName() +
                " (Server default: " + serverDifficulty.getName() + ")"), false);
        return 1;
    }

    private static int resetDifficulty(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();

        Difficulty serverDifficulty = PlayerDifficultyManager.getServerDifficulty();
        PlayerDifficultyManager.setPlayerDifficulty(player, serverDifficulty);

        source.sendFeedback(() -> Text.literal("Your difficulty has been reset to server default: " +
                serverDifficulty.getName()), false);
        return 1;
    }

    private static int showHelp(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        source.sendFeedback(() -> Text.literal("§6Per-Player Difficulty Commands:§r\n" +
                "§e/playerdifficulty set <difficulty>§r - Set your difficulty (peaceful/easy/normal/hard)\n" +
                "§e/playerdifficulty get§r - Check your current difficulty\n" +
                "§e/playerdifficulty reset§r - Reset to server default difficulty"), false);
        return 1;
    }
}