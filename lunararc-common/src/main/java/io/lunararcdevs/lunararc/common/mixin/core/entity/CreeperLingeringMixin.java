package io.lunararcdevs.lunararc.common.mixin.core.entity;

import io.lunararcdevs.lunararc.common.server.LunarArcServerSettings;
import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Creeper.class)
public abstract class CreeperLingeringMixin {
    @Inject(method = "spawnLingeringCloud", at = @At("HEAD"), cancellable = true, require = 0)
    private void lunararc$noLingering(CallbackInfo ci) {
        if (LunarArcServerSettings.tuning(((Creeper) (Object) this).level()).disableCreeperLingeringEffect) ci.cancel();
    }
}
