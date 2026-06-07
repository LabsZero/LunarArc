package io.ampznetwork.lunararc.common.mixin.core.entity;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.craftbukkit.v1_21_R1.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void lunararc$onDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;

        // Don't fire for entities that aren't tracked by the bridge yet
        if (org.bukkit.Bukkit.getEntity(entity.getUUID()) == null) return;

        EntityDamageEvent event = CraftEventFactory.callEntityDamageEvent(entity, source, amount);
        if (event.isCancelled()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "die", at = @At("HEAD"))
    private void lunararc$onDeath(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        try {
            CraftEventFactory.callEntityDeathEvent(entity);
        } catch (Throwable ignored) {
        }
    }
}
