package io.lunararcdevs.lunararc.common.mixin.core.world.block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.CoralBlock;
import net.minecraft.world.level.block.CoralFanBlock;
import net.minecraft.world.level.block.CoralPlantBlock;
import net.minecraft.world.level.block.CoralWallFanBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({CoralBlock.class, CoralFanBlock.class, CoralPlantBlock.class, CoralWallFanBlock.class})
public abstract class CoralFadeMixin {
    @WrapOperation(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private boolean lunararc$fade(ServerLevel level, BlockPos pos, BlockState state, int flags, Operation<Boolean> original) {
        if (CraftEventFactory.callBlockFadeCancelled(level, pos, state)) return false;
        return original.call(level, pos, state, flags);
    }
}
