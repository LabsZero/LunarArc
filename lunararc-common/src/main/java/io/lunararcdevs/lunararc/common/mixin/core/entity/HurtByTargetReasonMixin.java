package io.lunararcdevs.lunararc.common.mixin.core.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.lunararcdevs.lunararc.common.bridge.MobBridge;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.bukkit.event.entity.EntityTargetEvent.TargetReason;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal.class)
public abstract class HurtByTargetReasonMixin {
    @WrapOperation(method = "start", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;setTarget(Lnet/minecraft/world/entity/LivingEntity;)V"))
    private void lunararc$attacked(Mob mob, LivingEntity target, Operation<Void> original) {
        ((MobBridge) mob).lunararc$pushTargetReason(TargetReason.TARGET_ATTACKED_ENTITY);
        original.call(mob, target);
    }

    @WrapOperation(method = "alertOther", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;setTarget(Lnet/minecraft/world/entity/LivingEntity;)V"))
    private void lunararc$alerted(Mob mob, LivingEntity target, Operation<Void> original) {
        ((MobBridge) mob).lunararc$pushTargetReason(TargetReason.TARGET_ATTACKED_NEARBY_ENTITY);
        original.call(mob, target);
    }
}
