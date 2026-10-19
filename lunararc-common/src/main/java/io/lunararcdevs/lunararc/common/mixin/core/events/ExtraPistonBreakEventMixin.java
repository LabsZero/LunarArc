package io.lunararcdevs.lunararc.common.mixin.core.events;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.lunararcdevs.lunararc.common.event.LunarArcBlockEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** BlockBreakBlockEvent for blocks a moving piston destroys. */
@Mixin(PistonBaseBlock.class)
public abstract class ExtraPistonBreakEventMixin {
    @WrapOperation(method = "moveBlocks", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/piston/PistonBaseBlock;dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;)V"))
    private void lunararc$breakBlockEvent(BlockState state, net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockEntity blockEntity, Operation<Void> original,
            @Local(argsOnly = true, ordinal = 0) BlockPos piston) {
        if (LunarArcBlockEvents.dropAfterBreak(state, level, pos, blockEntity, piston)) return;
        original.call(state, level, pos, blockEntity);
    }
}
