package io.ampznetwork.lunararc.neoforge.mixin.bukkit;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import io.ampznetwork.lunararc.common.mod.server.LunarArcServer;
import io.ampznetwork.lunararc.common.server.LunarArcCommandRouter;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.CommandEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = CraftServer.class, remap = false)
public abstract class CraftServerMixin_NeoForge {
    @Redirect(
            method = "dispatchCommand",
            at = @At(
                    value = "INVOKE",
                    target = "Lio/ampznetwork/lunararc/common/server/LunarArcCommandRouter;dispatch(Lorg/bukkit/craftbukkit/CraftServer;Lorg/bukkit/command/CommandSender;Ljava/lang/String;)Z",
                    remap = false),
            remap = false)
    private boolean lunararc$neoForgeCommandEvent(CraftServer server, CommandSender sender, String commandLine) {
        CommandSourceStack source = lunararc$source(sender);
        if (source == null) {
            return LunarArcCommandRouter.dispatch(server, sender, commandLine);
        }

        StringReader reader = new StringReader(commandLine);
        if (reader.canRead() && reader.peek() == '/') {
            reader.skip();
        }
        ParseResults<CommandSourceStack> parse = LunarArcServer.requireMinecraftServer()
                .getCommands().getDispatcher().parse(reader, source);
        CommandEvent event = new CommandEvent(parse);
        if (NeoForge.EVENT_BUS.post(event).isCanceled() || event.getException() != null) {
            return false;
        }

        String rewritten = event.getParseResults().getReader().getString();
        if (rewritten.startsWith("/")) rewritten = rewritten.substring(1);
        return LunarArcCommandRouter.dispatch(server, sender, rewritten);
    }

    private static CommandSourceStack lunararc$source(CommandSender sender) {
        if (sender instanceof CraftEntity craftEntity) {
            return craftEntity.getHandle().createCommandSourceStack();
        }
        if (sender == Bukkit.getConsoleSender()) {
            return LunarArcServer.requireMinecraftServer().createCommandSourceStack();
        }
        return null;
    }
}
