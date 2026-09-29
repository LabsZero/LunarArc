package io.lunararcdevs.lunararc.common.mixin.core.world;

import io.lunararcdevs.lunararc.common.bridge.EntityBridge;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.server.level.ServerLevel$EntityCallbacks", remap = false)
public abstract class ServerLevelEntityCallbacksMixin {
    @Inject(method = "onTrackingStart(Lnet/minecraft/world/entity/Entity;)V", remap = true, at = @At("TAIL"))
    private void lunararc$markValid(Entity entity, CallbackInfo ci) {
        ((EntityBridge) entity).lunararc$setInWorld(true);
    }

    @Inject(method = "onTrackingEnd(Lnet/minecraft/world/entity/Entity;)V", remap = true, at = @At("TAIL"))
    private void lunararc$markInvalid(Entity entity, CallbackInfo ci) {
        ((EntityBridge) entity).lunararc$setInWorld(false);
    }
}
