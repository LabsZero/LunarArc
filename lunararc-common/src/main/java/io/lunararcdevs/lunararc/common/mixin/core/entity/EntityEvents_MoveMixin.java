package io.lunararcdevs.lunararc.common.mixin.core.entity;

import io.lunararcdevs.lunararc.common.event.LunarArcMoreEvents;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class EntityEvents_MoveMixin {
    @Inject(method = "tick", at = @At("RETURN"), require = 0)
    private void lunararc$move(CallbackInfo ci) {
        LunarArcMoreEvents.move((LivingEntity) (Object) this);
    }
}
