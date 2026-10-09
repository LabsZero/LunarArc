package io.lunararcdevs.lunararc.common.mixin.core.entity;

import io.lunararcdevs.lunararc.common.event.LunarArcDeathCapture;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ExperienceOrb.class)
public abstract class ExperienceOrbAwardMixin {
    @Inject(method = "award", at = @At("HEAD"), cancellable = true, require = 0)
    private static void lunararc$captureDeathExperience(ServerLevel level, Vec3 pos, int amount, CallbackInfo ci) {
        if (LunarArcDeathCapture.captureExperience(amount)) ci.cancel();
    }
}
