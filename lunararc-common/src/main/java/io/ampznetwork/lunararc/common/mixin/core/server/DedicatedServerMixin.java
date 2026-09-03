package io.ampznetwork.lunararc.common.mixin.core.server;

import io.ampznetwork.lunararc.common.LunarArcDebug;
import io.ampznetwork.lunararc.common.bridge.MinecraftServerBridge;
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

    /**
     * Console input, routed through Bukkit the way CraftBukkit routes it.
     *
     * <p>Vanilla drains its queued console lines straight into brigadier, which is all a vanilla
     * server has. On a Bukkit server the console is a {@link ConsoleCommandSender} and the line
     * goes to {@code CraftServer.dispatchCommand}, so it reaches the Bukkit command map first and
     * falls through to brigadier only when no Bukkit command claims the label. Without that hop
     * three things a plugin is entitled to expect never happen: {@link ServerCommandEvent} is
     * never fired, so command loggers, restart wrappers and permission plugins never see console
     * commands; the {@code commands.yml} aliases are never consulted; and a Bukkit command only
     * answers at the console at all because the command map happens to have mirrored it into
     * brigadier at startup, which a command registered later has not been.</p>
     *
     * <p>This is the same redirect Arclight applies, on the same call in the same method, and the
     * event-then-dispatch shape is CraftBukkit's own {@code dispatchServerCommand}. Vanilla
     * behaviour is preserved exactly when Bukkit is not up yet, which is the case for anything
     * typed before the compatibility layer finishes starting.</p>
     */
    @Redirect(method = "handleConsoleInputs",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/commands/Commands;performPrefixedCommand(Lnet/minecraft/commands/CommandSourceStack;Ljava/lang/String;)V"),
            require = 0)
    private void lunararc$routeConsoleCommand(Commands commands, CommandSourceStack source, String command) {
        String line = command == null ? "" : command.trim();
        if (LunarArcDebug.COMMAND) {
            LunarArcDebug.command("console input reached handleConsoleInputs: '%s'", line);
        }
        if (line.isEmpty()) return;

        CraftServer craftServer = ((MinecraftServerBridge) (Object) this).lunararc$getCraftServer();
        if (craftServer == null) {
            if (LunarArcDebug.COMMAND) {
                LunarArcDebug.command("no CraftServer yet; '%s' goes straight to vanilla", line);
            }
            commands.performPrefixedCommand(source, command);
            return;
        }

        ConsoleCommandSender console = craftServer.getConsoleSender();
        ServerCommandEvent event = new ServerCommandEvent(console, line);
        craftServer.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            if (LunarArcDebug.COMMAND) {
                LunarArcDebug.command("ServerCommandEvent cancelled '%s'", line);
            }
            return;
        }

        String routed = event.getCommand() == null ? "" : event.getCommand().trim();
        if (routed.isEmpty()) return;

        try {
            boolean handled = craftServer.dispatchCommand(console, routed);
            if (LunarArcDebug.COMMAND) {
                LunarArcDebug.command("dispatched '%s' -> %s", routed, handled);
            }
        } catch (Exception failure) {
            craftServer.getLogger().log(java.util.logging.Level.WARNING,
                    "Unexpected exception while parsing console command \"" + routed + '"', failure);
        }
    }
}
