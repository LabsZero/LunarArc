package io.lunararcdevs.lunararc.common.mixin.core.world.block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.SpreadingSnowyDirtBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SpreadingSnowyDirtBlock.class)
public abstract class SpreadingSnowyDirtBlockMixin {
    @WrapOperation(method = "randomTick", at = @At(value = "INVOKE", ordinal = 0,
            target = "Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean lunararc$fade(ServerLevel level, BlockPos pos, BlockState state, Operation<Boolean> original) {
        if (CraftEventFactory.callBlockFadeCancelled(level, pos, state)) return false;
        return original.call(level, pos, state);
    }

    @WrapOperation(method = "randomTick", at = @At(value = "INVOKE", ordinal = 1,
            target = "Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean lunararc$spread(ServerLevel level, BlockPos target, BlockState state, Operation<Boolean> original,
            @Local(argsOnly = true) BlockPos source) {
        return CraftEventFactory.handleBlockSpreadEvent(level, source, target, state, 3, (p, s, f) -> original.call(level, p, s));
    }
}
