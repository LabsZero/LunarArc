package io.lunararcdevs.lunararc.common.mixin.core.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.lunararcdevs.lunararc.common.bridge.LivingEntityBridge;
import net.minecraft.world.entity.TamableAnimal;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Feeding a tamed animal (wolf, nautilus, ...) heals with Bukkit's EATING reason; 26.x moved this into TamableAnimal#feed. */
@Mixin(TamableAnimal.class)
public abstract class TamableAnimalFeedEventsMixin {
    @WrapOperation(method = "feed", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/TamableAnimal;heal(F)V"))
    private void lunararc$eatingHeal(TamableAnimal self, float amount, Operation<Void> original) {
        ((LivingEntityBridge) self).lunararc$pushHealReason(EntityRegainHealthEvent.RegainReason.EATING, false);
        original.call(self, amount);
    }
}
