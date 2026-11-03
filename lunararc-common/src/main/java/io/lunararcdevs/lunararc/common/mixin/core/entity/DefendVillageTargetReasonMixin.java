package io.lunararcdevs.lunararc.common.mixin.core.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.lunararcdevs.lunararc.common.bridge.MobBridge;
import net.minecraft.world.entity.LivingEntity;

import org.bukkit.event.entity.EntityTargetEvent.TargetReason;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(net.minecraft.world.entity.ai.goal.target.DefendVillageTargetGoal.class)
public abstract class DefendVillageTargetReasonMixin {
    @WrapOperation(method = "start", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/IronGolem;setTarget(Lnet/minecraft/world/entity/LivingEntity;)V"))
    private void lunararc$defend(net.minecraft.world.entity.animal.IronGolem mob, LivingEntity target, Operation<Void> original) {
        ((MobBridge) mob).lunararc$pushTargetReason(TargetReason.DEFEND_VILLAGE);
        original.call(mob, target);
    }
}
