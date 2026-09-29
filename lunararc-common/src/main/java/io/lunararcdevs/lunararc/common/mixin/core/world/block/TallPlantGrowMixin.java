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

@Mixin({net.minecraft.world.level.block.SugarCaneBlock.class, net.minecraft.world.level.block.CactusBlock.class})
public abstract class TallPlantGrowMixin {
    @WrapOperation(method = "randomTick", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean lunararc$grow(ServerLevel level, BlockPos pos, BlockState state, Operation<Boolean> original) {
        return CraftEventFactory.handleBlockGrowEvent(level, pos, state, 3, (p, s, f) -> original.call(level, p, s));
    }
}
