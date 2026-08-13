package io.ampznetwork.lunararc.common.server;

import com.mojang.brigadier.CommandDispatcher;
import io.ampznetwork.lunararc.common.LunarArcPlatform;
import net.minecraft.commands.CommandSourceStack;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/** Bukkit is the source of truth; Brigadier mirrors labels from this map. */
public final class LunarArcCommandMap extends SimpleCommandMap {
    private static volatile CommandDispatcher<CommandSourceStack> dispatcher;

    public LunarArcCommandMap(Server server) {
        super(server, new HashMap<>());
    }

    public static void setDispatcher(CommandDispatcher<CommandSourceStack> value) {
        dispatcher = value;
    }

    public static CommandDispatcher<CommandSourceStack> getDispatcher() {
        return dispatcher;
    }

    @Override
    public boolean register(String label, String fallbackPrefix, Command command) {
        boolean result = super.register(label, fallbackPrefix, command);
        syncCommand(command, label, fallbackPrefix);
        syncCommandTreeToPlayers();
        return result;
    }

    public void unregisterPlugin(Plugin plugin) {
        if (plugin == null) return;
        Set<Command> removed = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        getKnownCommands().entrySet().removeIf(entry -> {
            Command command = entry.getValue();
            if (command instanceof PluginCommand pluginCommand && pluginCommand.getPlugin() == plugin) {
                removed.add(command);
                return true;
            }
            return false;
        });
        for (Command command : removed) command.unregister(this);
        syncCommandTreeToPlayers();
    }

    public void syncToBrigadier(CommandDispatcher<CommandSourceStack> target) {
        if (target == null) return;
        dispatcher = target;

        Set<String> labels = new LinkedHashSet<>(getKnownCommands().keySet());
        for (String knownLabel : labels) {
            registerMirror(target, knownLabel);
        }
    }

    private void syncCommand(Command command, String requestedLabel, String fallbackPrefix) {
        CommandDispatcher<CommandSourceStack> target = dispatcher;
        if (target == null) return;

        Set<String> candidates = new LinkedHashSet<>();
        candidates.add(normalize(requestedLabel));
        candidates.add(normalize(command.getName()));
        for (String alias : command.getAliases()) candidates.add(normalize(alias));

        String prefix = normalize(fallbackPrefix);
        if (!prefix.isEmpty()) {
            candidates.add(prefix + ":" + normalize(requestedLabel));
            candidates.add(prefix + ":" + normalize(command.getName()));
            for (String alias : command.getAliases()) {
                candidates.add(prefix + ":" + normalize(alias));
            }
        }

        // Register only labels that Bukkit actually accepted into knownCommands.
        for (String candidate : candidates) {
            if (!candidate.isEmpty() && getKnownCommands().containsKey(candidate)) {
                registerMirror(target, candidate);
            }
        }
    }

    private void registerMirror(CommandDispatcher<CommandSourceStack> target, String label) {
        String normalized = normalize(label);
        if (normalized.isEmpty() || getCommand(normalized) == null) return;

        /*
         * Never replace a command node supplied by Minecraft/NeoForge/a mod. Brigadier's
         * register() merges/replaces equal literal roots, and blindly mirroring Bukkit
         * labels here could therefore turn a native command such as /clear into a Bukkit
         * facade. Paper keeps the native dispatcher authoritative and performs command
         * conflict/namespace routing above it; LunarArc does the same in
         * LunarArcCommandRouter.
         *
         * A node already present for a non-native Bukkit label is also safe to leave in
         * place: BukkitCommandWrapper resolves the current Command from CommandMap at
         * execution time, so plugin reload/re-registration does not require replacing it.
         */
        if (target.getRoot().getChild(normalized) != null) return;

        new BukkitCommandWrapper(this, normalized).register(target);
    }

    private void syncCommandTreeToPlayers() {
        Server server = LunarArcPlatform.getServer();
        if (server == null) return;
        for (org.bukkit.entity.Player player : server.getOnlinePlayers()) {
            try {
                player.updateCommands();
            } catch (Throwable ignored) {
            }
        }
    }

    private static String normalize(String value) {
        if (value == null) return "";
        // A leading slash can be part of a Bukkit command label. WorldEdit uses
        // labels such as "/wand" so the client command //wand reaches Brigadier
        // as /wand. Never strip that label slash while mirroring Bukkit commands.
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
