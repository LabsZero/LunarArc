package io.lunararcdevs.lunararc.common.mixin.core.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.lunararcdevs.lunararc.common.bridge.MobBridge;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.bukkit.event.entity.EntityTargetEvent.TargetReason;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal.class)
public abstract class OwnerHurtByTargetReasonMixin {
    @WrapOperation(method = "start", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;setTarget(Lnet/minecraft/world/entity/LivingEntity;)V"))
    private void lunararc$ownerHurt(Mob mob, LivingEntity target, Operation<Void> original) {
        ((MobBridge) mob).lunararc$pushTargetReason(TargetReason.TARGET_ATTACKED_OWNER);
        original.call(mob, target);
    }
}
