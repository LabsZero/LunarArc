package io.lunararcdevs.lunararc.common.mixin.core.server;

import io.lunararcdevs.lunararc.common.bridge.MinecraftServerBridge;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.dedicated.DedicatedServer;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.event.server.ServerCommandEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DedicatedServer.class)
public abstract class DedicatedServerMixin {
    @Inject(method = "getSpawnProtectionRadius", at = @At("HEAD"), cancellable = true)
    private void lunararc$useBukkitSpawnRadius(CallbackInfoReturnable<Integer> cir) {
        CraftServer craftServer = ((MinecraftServerBridge) (Object) this).lunararc$getCraftServer();
        if (craftServer == null) return;
        int configured = craftServer.getBukkitSpawnRadius();
        if (configured >= 0) cir.setReturnValue(configured);
    }

    @Redirect(method = "handleConsoleInputs",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/commands/Commands;performPrefixedCommand(Lnet/minecraft/commands/CommandSourceStack;Ljava/lang/String;)V"),
            require = 0)
    private void lunararc$routeConsoleCommand(Commands commands, CommandSourceStack source, String command) {
        String line = command == null ? "" : command.trim();
        if (line.isEmpty()) return;

        CraftServer craftServer = ((MinecraftServerBridge) (Object) this).lunararc$getCraftServer();
        if (craftServer == null) {
            commands.performPrefixedCommand(source, command);
            return;
        }

        ConsoleCommandSender console = craftServer.getConsoleSender();
        ServerCommandEvent event = new ServerCommandEvent(console, line);
        craftServer.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }

        String routed = event.getCommand() == null ? "" : event.getCommand().trim();
        if (routed.isEmpty()) return;

        try {
            boolean handled = craftServer.dispatchCommand(console, routed);
        } catch (Exception failure) {
            craftServer.getLogger().log(java.util.logging.Level.WARNING,
                    "Unexpected exception while parsing console command \"" + routed + '"', failure);
        }
    }
}
