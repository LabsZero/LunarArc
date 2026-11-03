package io.lunararcdevs.lunararc.common.mixin.core.entity;

import io.lunararcdevs.lunararc.common.event.LunarArcMoreEvents;
import net.minecraft.world.entity.animal.Sheep;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Sheep.class)
public abstract class MobEvents_SheepEventsMixin {
    @Inject(method = "ate", at = @At("HEAD"), cancellable = true, require = 0)
    private void lunararc$regrow(CallbackInfo ci) {
        if (!LunarArcMoreEvents.sheepRegrow((Sheep) (Object) this)) ci.cancel();
    }
}
