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

import net.minecraft.world.level.block.FenceGateBlock;

@Mixin(FenceGateBlock.class)
public abstract class FenceGateBlockMixin {

    @WrapOperation(method = "neighborChanged", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private boolean lunararc$power(Level level, BlockPos pos, BlockState state, int flags, Operation<Boolean> original,
            @Local(argsOnly = true) BlockState old) {
        return LunarArcPaperEvents.redstoneAllows(level, pos, old.getValue(FenceGateBlock.POWERED), state.getValue(FenceGateBlock.POWERED))
                && original.call(level, pos, state, flags);
    }
}
