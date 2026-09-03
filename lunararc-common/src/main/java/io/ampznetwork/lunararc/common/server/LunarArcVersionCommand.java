package io.ampznetwork.lunararc.common.server;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.VersionCommand;
import org.jetbrains.annotations.NotNull;


public final class LunarArcVersionCommand extends VersionCommand {
    public LunarArcVersionCommand(String name) {
        super(name);
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String currentAlias, @NotNull String[] args) {
        boolean result = super.execute(sender, currentAlias, args);
        if (args.length == 0) {
            sender.sendMessage(ChatColor.GRAY + "LunarArc version " + ChatColor.AQUA
                    + LunarArcVersionInfo.lunarArcVersion());
        }
        return result;
    }
}
