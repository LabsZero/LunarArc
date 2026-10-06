package io.lunararcdevs.lunararc.common.server;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.lunararcdevs.lunararc.common.bridge.CommandSourceStackBridge;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;


public final class BukkitCommandWrapper {
    private static final Logger LOGGER = LoggerFactory.getLogger("LunarArc");
    private static final ConcurrentMap<String, AtomicInteger> PERMISSION_FAILURES = new ConcurrentHashMap<>();
    private static final int MAX_SUGGESTIONS = 500;
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
        if (command == null) return false;
        try {
            return command.testPermissionSilent(sender(source));
        } catch (Throwable ex) {
            StackTraceElement[] frames = ex.getStackTrace();
            String failureKey = ex.getClass().getName() + "@" + (frames.length > 0 ? frames[0] : "");
            int seen = PERMISSION_FAILURES.computeIfAbsent(failureKey, key -> new AtomicInteger()).incrementAndGet();
            if (seen == 1) {
                LOGGER.warn("Permission check for command '{}' threw; hiding it from this sender. Repeats of this failure are counted, not logged in full.",
                        commandLabel, ex);
            } else if (seen % 100 == 0) {
                LOGGER.warn("Permission check for command '{}' has now thrown {} times ({}); still hiding it.",
                        commandLabel, seen, ex.getClass().getSimpleName());
            }
            return false;
        }
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
            String commandText = input.startsWith("/") ? input.substring(1) : input;
            boolean hasArgumentInput = commandText.length() > label.length();
            String argumentText = hasArgumentInput ? commandText.substring(label.length()) : "";
            if (argumentText.startsWith(" ")) argumentText = argumentText.substring(1);
            String[] args = hasArgumentInput ? argumentText.split(" ", -1) : new String[0];
            completions = command == null ? List.of() : command.tabComplete(sender, label, args);
            if (command != null) completions = LunarArcEssentialsItemBridge.withModdedItems(command, args, completions);
        } catch (org.bukkit.command.CommandException exception) {
            throw exception;
        } catch (Throwable throwable) {
            LOGGER.warn("TabCompleter for '{}' threw while completing '{}'", label, input, throwable);
            completions = List.of();
        }

        int lastSpace = input.lastIndexOf(' ');
        SuggestionsBuilder target = lastSpace >= 0
                ? builder.createOffset(lastSpace + 1)
                : builder;

        String remaining = target.getRemainingLowerCase();
        if (completions == null) return target.buildFuture();
        List<String> matches = new java.util.ArrayList<>();
        for (String completion : completions) {
            if (completion != null && completion.toLowerCase(java.util.Locale.ROOT).startsWith(remaining)) {
                matches.add(completion);
            }
        }
        if (matches.size() > MAX_SUGGESTIONS) {
            matches.sort(String.CASE_INSENSITIVE_ORDER);
            matches = matches.subList(0, MAX_SUGGESTIONS);
        }
        for (String match : matches) target.suggest(match);
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
