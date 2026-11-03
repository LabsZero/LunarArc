package io.lunararcdevs.lunararc.common.mixin.core.entity;

import io.lunararcdevs.lunararc.common.event.LunarArcMoreEvents;
import net.minecraft.world.entity.ExperienceOrb;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ExperienceOrb.class)
public abstract class EntityEvents_OrbMixin {
    @Inject(method = "merge", at = @At("HEAD"), cancellable = true, require = 0)
    private void lunararc$merge(ExperienceOrb other, CallbackInfo ci) {
        if (!LunarArcMoreEvents.orbMerge((ExperienceOrb) (Object) this, other)) ci.cancel();
    }
}
