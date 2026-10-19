package io.lunararcdevs.lunararc.common.mixin.core.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.lunararcdevs.lunararc.common.bridge.LivingEntityBridge;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus;
import net.minecraft.world.entity.player.Player;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Nautilus (and zombie nautilus) breath effect and feeding report the causes Paper gives them. */
@Mixin(AbstractNautilus.class)
public abstract class NautilusEventsMixin {
    @WrapOperation(method = "applyEffects", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;addEffect(Lnet/minecraft/world/effect/MobEffectInstance;)Z"))
    private boolean lunararc$breathCause(Player rider, MobEffectInstance effect, Operation<Boolean> original) {
        ((LivingEntityBridge) rider).lunararc$pushEffectCause(EntityPotionEffectEvent.Cause.NAUTILUS);
        return original.call(rider, effect);
    }
}
