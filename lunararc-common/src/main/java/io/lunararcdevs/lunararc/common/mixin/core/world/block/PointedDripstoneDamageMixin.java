package io.lunararcdevs.lunararc.common.mixin.core.world.block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PointedDripstoneBlock.class)
public abstract class PointedDripstoneDamageMixin {
    @WrapOperation(method = "fallOn", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;causeFallDamage(FFLnet/minecraft/world/damagesource/DamageSource;)Z"))
    private boolean lunararc$blockDamage(Entity entity, float distance, float multiplier, DamageSource source, Operation<Boolean> original,
            @Local(argsOnly = true) BlockPos pos, @Local(argsOnly = true) BlockState state) {
        CraftEventFactory.damagingBlockPos = pos;
        CraftEventFactory.damagingBlockState = state;
        try {
            return original.call(entity, distance, multiplier, source);
        } finally {
            CraftEventFactory.damagingBlockPos = null;
            CraftEventFactory.damagingBlockState = null;
        }
    }
}
