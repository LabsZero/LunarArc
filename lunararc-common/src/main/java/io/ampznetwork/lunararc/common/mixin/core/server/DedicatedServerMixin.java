package io.ampznetwork.lunararc.common.mixin.core.server;

import io.ampznetwork.lunararc.common.bridge.MinecraftServerBridge;
import net.minecraft.server.dedicated.DedicatedServer;
import org.bukkit.craftbukkit.CraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
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
}