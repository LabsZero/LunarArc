package io.lunararcdevs.lunararc.common.mixin.core.server;

import io.lunararcdevs.lunararc.common.event.LunarArcServerSideEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.rcon.RconConsoleSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** RemoteServerCommandEvent: lets plugins see, change or cancel commands arriving over RCON. */
@Mixin(DedicatedServer.class)
public abstract class ServerSideRconCommandMixin {
    @Shadow @Final private RconConsoleSource rconConsoleSource;

    @Inject(method = "runCommand", at = @At("HEAD"), cancellable = true, require = 0)
    private void lunararc$remoteCommand(String command, CallbackInfoReturnable<String> cir) {
        if (command == null || command.isBlank()) {
            cir.setReturnValue("");
            return;
        }
        DedicatedServer self = (DedicatedServer) (Object) this;
        this.rconConsoleSource.prepareForCommand();
        self.executeBlocking(() -> {
            CommandSourceStack stack = this.rconConsoleSource.createCommandSourceStack();
            String routed = LunarArcServerSideEvents.remoteCommand(stack, command);
            if (routed == null) return;
            self.getCommands().performPrefixedCommand(stack, routed);
        });
        cir.setReturnValue(this.rconConsoleSource.getCommandResponse());
    }
}
