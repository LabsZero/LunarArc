package io.ampznetwork.lunararc.common.server;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.ampznetwork.lunararc.common.bridge.CommandSourceStackBridge;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.concurrent.CompletableFuture;


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
        } catch (org.bukkit.command.CommandException exception) {
            throw exception;
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
        return ((CommandSourceStackBridge) (Object) source).lunararc$getBukkitSender();
    }

    private static String normalize(String value) {
        if (value == null) return "";


        return value.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
