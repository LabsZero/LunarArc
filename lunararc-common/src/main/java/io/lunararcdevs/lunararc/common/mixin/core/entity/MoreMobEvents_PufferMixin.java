package io.lunararcdevs.lunararc.common.mixin.core.entity;

import io.lunararcdevs.lunararc.common.event.LunarArcMoreEvents;
import net.minecraft.world.entity.animal.Pufferfish;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Pufferfish.class)
public abstract class MoreMobEvents_PufferMixin {

    @Inject(method = "setPuffState", at = @At("HEAD"), cancellable = true, require = 0)
    private void lunararc$puff(int state, CallbackInfo ci) {
        if (!LunarArcMoreEvents.pufferState((Pufferfish) (Object) this, state)) ci.cancel();
    }
}
