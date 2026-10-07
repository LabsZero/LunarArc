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

import net.minecraft.world.level.block.RedStoneWireBlock;

@Mixin(RedStoneWireBlock.class)
public abstract class RedStoneWireBlockMixin {

    @ModifyExpressionValue(method = "updatePowerStrength", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/RedStoneWireBlock;calculateTargetStrength(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)I"))
    private int lunararc$strength(int target, @Local(argsOnly = true) Level level, @Local(argsOnly = true) BlockPos pos,
            @Local(argsOnly = true) BlockState state) {
        int old = state.getValue(RedStoneWireBlock.POWER);
        return old != target ? LunarArcPaperEvents.redstone(level, pos, old, target) : target;
    }
}
