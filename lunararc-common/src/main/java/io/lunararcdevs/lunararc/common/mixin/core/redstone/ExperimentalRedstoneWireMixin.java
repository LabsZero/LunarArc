package io.lunararcdevs.lunararc.common.mixin.core.redstone;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.lunararcdevs.lunararc.common.event.LunarArcPaperEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RedstoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.ExperimentalRedstoneWireEvaluator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** BlockRedstoneEvent for the experimental (minecart-improvements) redstone wire evaluator, where plugins may change the new power. */
@Mixin(ExperimentalRedstoneWireEvaluator.class)
public abstract class ExperimentalRedstoneWireMixin {
    @WrapOperation(method = "updatePowerStrength", require = 1,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private boolean lunararc$redstoneEvent(Level level, BlockPos pos, BlockState newState, int flags, Operation<Boolean> original) {
        BlockState current = level.getBlockState(pos);
        if (current.hasProperty(RedstoneWireBlock.POWER) && newState.hasProperty(RedstoneWireBlock.POWER)) {
            int oldPower = current.getValue(RedstoneWireBlock.POWER);
            int newPower = newState.getValue(RedstoneWireBlock.POWER);
            if (oldPower != newPower) {
                int adjusted = LunarArcPaperEvents.redstone(level, pos, oldPower, newPower);
                if (adjusted != newPower) newState = newState.setValue(RedstoneWireBlock.POWER, Math.max(0, Math.min(15, adjusted)));
            }
        }
        return original.call(level, pos, newState, flags);
    }
}
