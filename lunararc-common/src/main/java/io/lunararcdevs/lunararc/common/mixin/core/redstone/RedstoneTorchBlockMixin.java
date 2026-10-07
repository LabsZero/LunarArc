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

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.RedstoneTorchBlock;

@Mixin(RedstoneTorchBlock.class)
public abstract class RedstoneTorchBlockMixin {

    @WrapOperation(method = "tick", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private boolean lunararc$toggle(ServerLevel level, BlockPos pos, BlockState state, int flags, Operation<Boolean> original) {
        boolean lit = state.getValue(RedstoneTorchBlock.LIT);
        return LunarArcPaperEvents.redstoneAllows(level, pos, !lit, lit) && original.call(level, pos, state, flags);
    }
}
