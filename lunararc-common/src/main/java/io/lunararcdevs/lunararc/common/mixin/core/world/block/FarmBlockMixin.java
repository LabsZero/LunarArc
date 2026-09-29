package io.lunararcdevs.lunararc.common.mixin.core.world.block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FarmBlock.class)
public abstract class FarmBlockMixin {
    @WrapOperation(method = "randomTick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private boolean lunararc$moisture(ServerLevel level, BlockPos pos, BlockState state, int flags, Operation<Boolean> original) {
        return CraftEventFactory.handleMoistureChangeEvent(level, pos, state, flags, (p, s, f) -> original.call(level, p, s, f));
    }

    @WrapOperation(method = "turnToDirt", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private static boolean lunararc$fade(Level level, BlockPos pos, BlockState state, Operation<Boolean> original) {
        if (CraftEventFactory.callBlockFadeCancelled(level, pos, state)) return false;
        return original.call(level, pos, state);
    }
}
