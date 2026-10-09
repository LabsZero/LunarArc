package io.lunararcdevs.lunararc.common.mixin.core.world;

import io.lunararcdevs.lunararc.common.event.LunarArcDeathCapture;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
public abstract class ServerLevelDeathCaptureMixin {
    @Inject(method = "addFreshEntity", at = @At("HEAD"), cancellable = true, require = 0)
    private void lunararc$captureDeathDrop(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (LunarArcDeathCapture.captureEntity(entity)) cir.setReturnValue(true);
    }
}
