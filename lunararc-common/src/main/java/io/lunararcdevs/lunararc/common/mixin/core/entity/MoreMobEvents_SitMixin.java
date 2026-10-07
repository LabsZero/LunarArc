package io.lunararcdevs.lunararc.common.mixin.core.entity;

import io.lunararcdevs.lunararc.common.event.LunarArcMoreEvents;
import net.minecraft.world.entity.TamableAnimal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TamableAnimal.class)
public abstract class MoreMobEvents_SitMixin {

    @Inject(method = "setInSittingPose", at = @At("HEAD"), cancellable = true, require = 0)
    private void lunararc$sit(boolean sitting, CallbackInfo ci) {
        TamableAnimal self = (TamableAnimal) (Object) this;
        if (sitting != self.isInSittingPose() && !LunarArcMoreEvents.toggleSit(self, sitting)) ci.cancel();
    }
}
