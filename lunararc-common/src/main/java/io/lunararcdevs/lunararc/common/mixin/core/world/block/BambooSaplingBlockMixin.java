package io.lunararcdevs.lunararc.common.mixin.core.world.block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BambooSaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BambooSaplingBlock.class)
public abstract class BambooSaplingBlockMixin {
    @WrapOperation(method = "growBamboo", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private boolean lunararc$spread(Level level, BlockPos target, BlockState state, int flags, Operation<Boolean> original,
            @Local(argsOnly = true) BlockPos source) {
        return CraftEventFactory.handleBlockSpreadEvent(level, source, target, state, flags, (p, s, f) -> original.call(level, p, s, f));
    }
}
