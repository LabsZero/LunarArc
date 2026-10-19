package io.lunararcdevs.lunararc.common.mixin.core.events;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.lunararcdevs.lunararc.common.event.LunarArcBlockEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** FluidLevelChangeEvent: a plugin may cancel or change the block a flowing fluid is about to place. */
@Mixin(FlowingFluid.class)
public abstract class ExtraFluidLevelEventMixin {
    @WrapOperation(method = "tick", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean lunararc$levelChange(ServerLevel level, BlockPos pos, BlockState state, Operation<Boolean> original) {
        BlockState result = LunarArcBlockEvents.fluidLevelChange(level, pos, state);
        if (result == null) return false;
        return original.call(level, pos, result);
    }
}
