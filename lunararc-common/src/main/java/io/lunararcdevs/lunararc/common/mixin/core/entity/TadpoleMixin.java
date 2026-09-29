package io.lunararcdevs.lunararc.common.mixin.core.entity;

import net.minecraft.world.entity.animal.frog.Tadpole;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Tadpole.class)
public abstract class TadpoleMixin {
    public boolean ageLocked;

    @Inject(method = "ageUp(I)V", at = @At("HEAD"), cancellable = true)
    private void lunararc$ageLocked(int seconds, CallbackInfo ci) {
        if (this.ageLocked) ci.cancel();
    }
}
