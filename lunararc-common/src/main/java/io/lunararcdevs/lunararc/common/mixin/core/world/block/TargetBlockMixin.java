package io.lunararcdevs.lunararc.common.mixin.core.world.block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.lunararcdevs.lunararc.common.event.LunarArcPaperEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.TargetBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TargetBlock.class)
public abstract class TargetBlockMixin {

    @WrapOperation(method = "updateRedstoneOutput", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/TargetBlock;getRedstoneStrength(Lnet/minecraft/world/phys/BlockHitResult;Lnet/minecraft/world/phys/Vec3;)I"))
    private static int lunararc$targetHit(BlockHitResult hit, Vec3 location, Operation<Integer> original,
            @Local(argsOnly = true) LevelAccessor level, @Local(argsOnly = true) Entity entity) {
        return LunarArcPaperEvents.targetHit(level, entity, hit, original.call(hit, location));
    }
}
