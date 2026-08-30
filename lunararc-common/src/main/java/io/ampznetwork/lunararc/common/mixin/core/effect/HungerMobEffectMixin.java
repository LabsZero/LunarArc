package io.ampznetwork.lunararc.common.mixin.core.effect;

import io.ampznetwork.lunararc.common.bridge.PlayerExhaustionBridge;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Adds Bukkit's HUNGER_EFFECT cause without replacing the vanilla mob-effect tick. */
@Mixin(targets = "net.minecraft.world.effect.HungerMobEffect")
public abstract class HungerMobEffectMixin {
    @Inject(method = "applyEffectTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;causeFoodExhaustion(F)V"), require = 0)
    private void lunararc$hungerExhaustion(net.minecraft.world.entity.LivingEntity entity, int amplifier, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof Player player) {
            ((PlayerExhaustionBridge) player).lunararc$pushExhaustionReason(
                    org.bukkit.event.entity.EntityExhaustionEvent.ExhaustionReason.HUNGER_EFFECT);
        }
    }
}
