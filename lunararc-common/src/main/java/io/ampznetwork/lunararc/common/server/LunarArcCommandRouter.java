package io.ampznetwork.lunararc.common.server;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_21_R1.CraftServer;
import org.bukkit.craftbukkit.v1_21_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Locale;
import java.util.logging.Level;

/**
 * Single command-routing authority for the LunarArc Bukkit/Paper bridge.
 *
 * <p>The Minecraft client removes the first command introducer before sending a
 * ServerboundChatCommandPacket. That means WorldEdit's user-facing {@code //wand}
 * arrives as packet command {@code /wand}. We therefore remove the Bukkit event
 * introducer exactly once and never blindly strip another slash afterwards.</p>
 *
 * <p>Routing policy:</p>
 * <ol>
 *   <li>Fire PlayerCommandPreprocessEvent exactly once for player packet input.</li>
 *   <li>If Bukkit owns the resulting exact label, execute it through CommandMap.</li>
 *   <li>If Bukkit does not own it and the event did not rewrite it, let the native
 *       Minecraft/NeoForge packet handler continue unchanged.</li>
 *   <li>If a plugin rewrites the command to a non-Bukkit label, execute the rewritten
 *       text through the native Brigadier dispatcher and cancel the stale packet.</li>
 * </ol>
 */
public final class LunarArcCommandRouter {

    private LunarArcCommandRouter() {
    }

    public enum PacketResult {
        /** Leave the original Minecraft packet handler alone. */
        PASS,
        /** The command was handled (or cancelled) and the packet must not continue. */
        CANCEL
    }

    public static PacketResult routePlayerPacket(Server server, Player player, String packetCommand) {
        if (server == null || player == null || packetCommand == null) {
            return PacketResult.PASS;
        }

        // packetCommand has already had Minecraft's command introducer removed by the
        // client. Prefix exactly one slash to reproduce Bukkit's event contract.
        String originalEventMessage = "/" + packetCommand;
        PlayerCommandPreprocessEvent event = new PlayerCommandPreprocessEvent(player, originalEventMessage);
        server.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            return PacketResult.CANCEL;
        }

        String eventMessage = event.getMessage();
        if (eventMessage == null) {
            return PacketResult.CANCEL;
        }

        String routedLine = removeEventIntroducer(eventMessage);
        if (routedLine.isEmpty()) {
            return PacketResult.CANCEL;
        }

        boolean modified = !eventMessage.equals(originalEventMessage);
        CommandMap commandMap = server.getCommandMap();
        String label = labelOf(routedLine);
        Command command = commandMap.getCommand(label);

        if (command != null) {
            // Exact Bukkit ownership. Do not bounce through CraftServer again: doing so
            // creates another normalisation/routing decision and was the source of several
            // duplicate-slash and plugin-command regressions.
            commandMap.dispatch(player, routedLine);
            return PacketResult.CANCEL;
        }

        if (modified) {
            // The packet still contains the old command. Execute the plugin's rewritten
            // command once, using the same central programmatic router, then cancel it.
            dispatch(server, player, routedLine);
            return PacketResult.CANCEL;
        }

        // Native/mod command: Minecraft remains authoritative and handles its own
        // Brigadier context, permissions, command result and feedback.
        return PacketResult.PASS;
    }

    /**
     * Programmatic Bukkit Server#dispatchCommand routing.
     *
     * <p>Bukkit normally supplies a command line without a leading slash. Some plugins
     * still pass one. A slash is stripped only if it is an external introducer; if the
     * slash-bearing token is itself a registered Bukkit label (WorldEdit), it is kept.</p>
     */
    public static boolean dispatch(Server server, CommandSender sender, String commandLine) {
        if (server == null || sender == null || commandLine == null) return false;

        String line = commandLine.trim();
        if (line.isEmpty()) return false;

        CommandMap map = server.getCommandMap();

        // First honour the exact supplied label. This is what preserves WorldEdit labels
        // such as "/wand" after //wand has crossed the client protocol boundary.
        String exactLabel = labelOf(line);
        if (map.getCommand(exactLabel) != null) {
            return map.dispatch(sender, line);
        }

        // If no exact slash-bearing command exists, accept one conventional external
        // command introducer from programmatic callers.
        if (line.startsWith("/")) {
            line = line.substring(1).trim();
            if (line.isEmpty()) return false;
        }

        String label = labelOf(line);
        if (map.getCommand(label) != null) {
            return map.dispatch(sender, line);
        }

        return dispatchNative(server, sender, line);
    }

    public static boolean dispatchNative(Server server, CommandSender sender, String line) {
        if (!(server instanceof CraftServer craftServer)) return false;
        try {
            CommandSourceStack source = source(craftServer, sender);
            return craftServer.getHandle().getCommands().getDispatcher().execute(line, source) > 0;
        } catch (CommandSyntaxException syntax) {
            sender.sendMessage(syntax.getRawMessage().getString());
            return false;
        } catch (Throwable error) {
            server.getLogger().log(Level.SEVERE, "Error dispatching command: " + line, error);
            return false;
        }
    }

    private static CommandSourceStack source(CraftServer server, CommandSender sender) {
        if (sender instanceof CraftPlayer player) {
            return player.getHandle().createCommandSourceStack();
        }
        return server.getHandle().createCommandSourceStack();
    }

    /** Remove exactly the slash required by PlayerCommandPreprocessEvent's contract. */
    static String removeEventIntroducer(String eventMessage) {
        String line = eventMessage.trim();
        if (line.startsWith("/")) {
            line = line.substring(1);
        }
        return line.trim();
    }

    static String labelOf(String line) {
        String trimmed = line == null ? "" : line.trim();
        if (trimmed.isEmpty()) return "";
        int space = trimmed.indexOf(' ');
        String label = space >= 0 ? trimmed.substring(0, space) : trimmed;
        return label.toLowerCase(Locale.ROOT);
    }
}
