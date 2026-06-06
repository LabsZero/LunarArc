package io.ampznetwork.lunararc.common.mixin.core.server;

import net.minecraft.server.dedicated.DedicatedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// NeoForge overrides getServerModName() in DedicatedServer (not MinecraftServer),
// so we must also target this class with max priority to win the injection ordering.
@Mixin(value = DedicatedServer.class, priority = Integer.MAX_VALUE)
public abstract class DedicatedServerMixin {

    @Inject(method = "getServerModName", at = @At("HEAD"), cancellable = true)
    private void lunararc$getServerModName(CallbackInfoReturnable<String> cir) {
        cir.setReturnValue("paper");
    }
}
