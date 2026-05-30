package io.ampznetwork.lunararc.common.server;

import com.mojang.brigadier.CommandDispatcher;
import io.ampznetwork.lunararc.common.LunarArcPlatform;
import net.minecraft.commands.CommandSourceStack;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.SimpleCommandMap;

import java.util.HashMap;
import java.util.Map;

public class LunarArcCommandMap extends SimpleCommandMap {

    // Brigadier dispatcher set by CommandsMixin once commands are initialized
    private static volatile CommandDispatcher<CommandSourceStack> dispatcher;

    public LunarArcCommandMap(Server server) {
        super(server, new HashMap<>());
    }

    public static void setDispatcher(CommandDispatcher<CommandSourceStack> d) {
        dispatcher = d;
    }

    public static CommandDispatcher<CommandSourceStack> getDispatcher() {
        return dispatcher;
    }

    @Override
    public boolean register(String label, String fallbackPrefix, Command command) {
        boolean registered = super.register(label, fallbackPrefix, command);
        if (registered && dispatcher != null) {
            new BukkitCommandWrapper(command).register(dispatcher);
            syncCommandTreeToPlayers();
        }
        return registered;
    }

    /** Resend the command tree to all online players so tab-complete reflects the new command. */
    private void syncCommandTreeToPlayers() {
        try {
            Server server = LunarArcPlatform.getServer();
            if (server == null) return;
            for (org.bukkit.entity.Player player : server.getOnlinePlayers()) {
                try {
                    player.updateCommands();
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
    }
}
