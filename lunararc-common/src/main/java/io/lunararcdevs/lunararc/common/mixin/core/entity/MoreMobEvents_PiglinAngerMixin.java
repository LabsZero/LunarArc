package io.lunararcdevs.lunararc.common.mixin.core.entity;

import io.lunararcdevs.lunararc.common.event.LunarArcMoreEvents;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ZombifiedPiglin.class)
public abstract class MoreMobEvents_PiglinAngerMixin {
    @Shadow @Final private static UniformInt PERSISTENT_ANGER_TIME;

    @Inject(method = "startPersistentAngerTimer", at = @At("HEAD"), cancellable = true, require = 0)
    private void lunararc$anger(CallbackInfo ci) {
        ZombifiedPiglin self = (ZombifiedPiglin) (Object) this;
        int anger = LunarArcMoreEvents.piglinAnger(self, PERSISTENT_ANGER_TIME.sample(self.getRandom()));
        ci.cancel();
        if (anger < 0) self.setPersistentAngerTarget(null);
        else self.setRemainingPersistentAngerTime(anger);
    }
}
