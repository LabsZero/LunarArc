package io.ampznetwork.lunararc.common.server;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.ampznetwork.lunararc.common.LunarArcPlatform;
import io.ampznetwork.lunararc.common.bridge.EntityBridge;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Thin Brigadier facade over Bukkit's authoritative CommandMap.
 * Command ownership, aliases, permissions and execution stay in Bukkit.
 */
public final class BukkitCommandWrapper {
    private final CommandMap commandMap;
    private final String label;

    public BukkitCommandWrapper(CommandMap commandMap, String label) {
        this.commandMap = commandMap;
        this.label = normalize(label);
    }

    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (label.isEmpty()) return;

        dispatcher.register(
                Commands.literal(label)
                        .requires(source -> canUse(source, label))
                        .then(Commands.argument("args", StringArgumentType.greedyString())
                                .suggests(this::suggest)
                                .executes(context -> execute(context, true)))
                        .executes(context -> execute(context, false)));
    }

    private boolean canUse(CommandSourceStack source, String commandLabel) {
        Command command = commandMap.getCommand(commandLabel);
        return command != null && command.testPermissionSilent(sender(source));
    }

    private int execute(CommandContext<CommandSourceStack> context, boolean hasArguments) {
        String line = label;
        if (hasArguments) {
            String args = StringArgumentType.getString(context, "args");
            if (!args.isBlank()) line += " " + args;
        }

        // This Brigadier node exists only because Bukkit already owns this exact label.
        // Execute directly through the authoritative CommandMap so routing cannot recurse.
        return commandMap.dispatch(sender(context.getSource()), line) ? 1 : 0;
    }

    private CompletableFuture<Suggestions> suggest(
            CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder) {
        CommandSender sender = sender(context.getSource());
        String input = context.getInput();
        Command command = commandMap.getCommand(label);

        List<String> completions;
        try {
            String argumentText = input.length() <= label.length()
                    ? ""
                    : input.substring(label.length()).stripLeading();
            String[] args = argumentText.isEmpty()
                    ? new String[0]
                    : argumentText.split(" ", -1);
            completions = command == null ? List.of() : command.tabComplete(sender, label, args);
        } catch (Throwable ignored) {
            completions = List.of();
        }

        int lastSpace = input.lastIndexOf(' ');
        SuggestionsBuilder target = lastSpace >= 0
                ? builder.createOffset(lastSpace + 1)
                : builder;

        String remaining = target.getRemainingLowerCase();
        for (String completion : completions) {
            if (completion != null && completion.toLowerCase(java.util.Locale.ROOT).startsWith(remaining)) {
                target.suggest(completion);
            }
        }
        return target.buildFuture();
    }

    private static CommandSender sender(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            Object bukkit = ((EntityBridge) player).lunararc$getBukkitEntity();
            if (bukkit instanceof CommandSender commandSender) {
                return commandSender;
            }
        }

        Server server = LunarArcPlatform.getServer();
        if (server == null) {
            throw new IllegalStateException("Bukkit server is not initialized");
        }
        return server.getConsoleSender();
    }

    private static String normalize(String value) {
        if (value == null) return "";
        // Preserve a slash that is part of the actual Bukkit label (not the
        // client's command introducer). This is required for WorldEdit // commands.
        return value.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
