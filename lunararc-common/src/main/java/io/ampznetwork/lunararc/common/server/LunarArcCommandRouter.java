package io.ampznetwork.lunararc.common.server;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Locale;
import java.util.logging.Level;


public final class LunarArcCommandRouter {

    @FunctionalInterface
    public interface PlatformCommandHook {
        HookResult apply(CraftServer server, CommandSender sender, String commandLine);
    }

    public record HookResult(boolean cancelled, String commandLine) {
        public static HookResult pass(String commandLine) {
            return new HookResult(false, commandLine);
        }

        public static HookResult cancel() {
            return new HookResult(true, "");
        }
    }

    private static volatile PlatformCommandHook platformCommandHook = (server, sender, line) -> HookResult.pass(line);

    private LunarArcCommandRouter() {
    }

    public static void installPlatformCommandHook(PlatformCommandHook hook) {
        platformCommandHook = hook == null ? (server, sender, line) -> HookResult.pass(line) : hook;
    }

    public enum PacketResult {

        PASS,

        CANCEL
    }

    public static PacketResult routePlayerPacket(Server server, Player player, String packetCommand) {
        if (server == null || player == null || packetCommand == null) {
            return PacketResult.PASS;
        }


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


            commandMap.dispatch(player, routedLine);
            return PacketResult.CANCEL;
        }

        if (modified) {


            dispatch(server, player, routedLine);
            return PacketResult.CANCEL;
        }


        return PacketResult.PASS;
    }


    public static boolean dispatch(Server server, CommandSender sender, String commandLine) {
        if (server == null || sender == null || commandLine == null) return false;

        String line = commandLine.trim();
        if (line.isEmpty()) return false;

        if (server instanceof CraftServer craftServer) {
            HookResult hookResult = platformCommandHook.apply(craftServer, sender, line);
            if (hookResult == null || hookResult.cancelled()) return false;
            line = hookResult.commandLine() == null ? "" : hookResult.commandLine().trim();
            if (line.isEmpty()) return false;
        }

        CommandMap map = server.getCommandMap();


        String exactLabel = labelOf(line);
        if (map.getCommand(exactLabel) != null) {
            return map.dispatch(sender, line);
        }


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
