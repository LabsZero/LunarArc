package io.lunararcdevs.lunararc.common.mixin.core.entity;

import io.lunararcdevs.lunararc.common.event.LunarArcMoreEvents;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractHorse.class)
public abstract class MobEvents_HorseEventsMixin {
    @Inject(method = "handleStartJump", at = @At("HEAD"), cancellable = true, require = 0)
    private void lunararc$jump(int power, CallbackInfo ci) {
        if (!LunarArcMoreEvents.horseJump((AbstractHorse) (Object) this, power)) ci.cancel();
    }
}
