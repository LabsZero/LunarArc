package io.lunararcdevs.lunararc.common.mixin.core.world.blockentity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.lunararcdevs.lunararc.common.bridge.donor.DonorBlockEntitySupport;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ConduitBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ConduitBlockEntity.class)
public abstract class ConduitBlockEntityMixin {
    @WrapOperation(method = "updateDestroyTarget", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private static boolean lunararc$gateDamage(LivingEntity target, DamageSource source, float amount, Operation<Boolean> original) {
        return DonorBlockEntitySupport.conduitDamage && original.call(target, source, amount);
    }

    @WrapOperation(method = "updateDestroyTarget", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;playSound(Lnet/minecraft/world/entity/player/Player;DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V"))
    private static void lunararc$gateSound(Level level, Player player, double x, double y, double z, SoundEvent sound,
            SoundSource category, float volume, float pitch, Operation<Void> original) {
        if (DonorBlockEntitySupport.conduitDamage) original.call(level, player, x, y, z, sound, category, volume, pitch);
    }
}
