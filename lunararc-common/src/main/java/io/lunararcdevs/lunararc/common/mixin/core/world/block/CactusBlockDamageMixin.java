package io.lunararcdevs.lunararc.common.mixin.core.world.block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CactusBlock.class)
public abstract class CactusBlockDamageMixin {
    @WrapOperation(method = "entityInside", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean lunararc$blockDamage(Entity entity, DamageSource source, float amount, Operation<Boolean> original,
            @Local(argsOnly = true) BlockPos pos, @Local(argsOnly = true) BlockState state) {
        CraftEventFactory.damagingBlockPos = pos;
        CraftEventFactory.damagingBlockState = state;
        try {
            return original.call(entity, source, amount);
        } finally {
            CraftEventFactory.damagingBlockPos = null;
            CraftEventFactory.damagingBlockState = null;
        }
    }
}
