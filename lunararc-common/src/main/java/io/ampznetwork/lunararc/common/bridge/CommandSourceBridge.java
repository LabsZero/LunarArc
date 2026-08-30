package io.ampznetwork.lunararc.common.bridge;

import net.minecraft.commands.CommandSourceStack;
import org.bukkit.command.CommandSender;

public interface CommandSourceBridge {
    CommandSender lunararc$getBukkitSender(CommandSourceStack stack);
}
