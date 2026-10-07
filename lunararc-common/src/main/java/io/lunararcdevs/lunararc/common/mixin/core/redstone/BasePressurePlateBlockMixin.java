package io.lunararcdevs.lunararc.common.mixin.core.redstone;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.lunararcdevs.lunararc.common.event.LunarArcPaperEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.world.level.block.BasePressurePlateBlock;

@Mixin(BasePressurePlateBlock.class)
public abstract class BasePressurePlateBlockMixin {

    @ModifyExpressionValue(method = "checkPressed", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/BasePressurePlateBlock;getSignalStrength(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)I"))
    private int lunararc$signal(int signal, @Local(argsOnly = true) Level level, @Local(argsOnly = true) BlockPos pos,
            @Local(argsOnly = true) int current) {
        return (current > 0) != (signal > 0) ? LunarArcPaperEvents.redstone(level, pos, current, signal) : signal;
    }
}
