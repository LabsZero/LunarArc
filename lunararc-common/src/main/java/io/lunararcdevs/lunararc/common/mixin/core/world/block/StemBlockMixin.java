package io.lunararcdevs.lunararc.common.mixin.core.world.block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Unique;

@Mixin(net.minecraft.world.level.block.StemBlock.class)
public abstract class StemBlockMixin {
    @Unique private boolean lunararc$fruitCancelled;

    @WrapOperation(method = "randomTick", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean lunararc$growFruit(ServerLevel level, BlockPos pos, BlockState state, Operation<Boolean> original) {
        boolean placed = CraftEventFactory.handleBlockGrowEvent(level, pos, state, 3, (p, s, f) -> original.call(level, p, s));
        this.lunararc$fruitCancelled = !placed;
        return placed;
    }

    @WrapOperation(method = "randomTick", at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean lunararc$attachStem(ServerLevel level, BlockPos pos, BlockState state, Operation<Boolean> original) {
        if (this.lunararc$fruitCancelled) {
            this.lunararc$fruitCancelled = false;
            return false;
        }
        return original.call(level, pos, state);
    }
}
