package io.lunararcdevs.lunararc.common.mixin.core.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.lunararcdevs.lunararc.common.event.LunarArcPaperEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class LivingEntityKnockbackMixin {

    @WrapOperation(method = "hurt", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;knockback(DDD)V"))
    private void lunararc$knockback(LivingEntity target, double strength, double x, double z, Operation<Void> original,
            @Local(argsOnly = true) DamageSource source) {
        Vec3 before = target.getDeltaMovement();
        original.call(target, strength, x, z);
        if (source.getEntity() != null) LunarArcPaperEvents.knockback(target, source.getEntity(), strength, before);
    }
}
