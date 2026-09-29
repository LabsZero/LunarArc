package io.lunararcdevs.lunararc.common.mixin.core.world.block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LiquidBlock.class)
public abstract class LiquidBlockMixin {
    @WrapOperation(method = "shouldSpreadLiquid", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean lunararc$form(Level level, BlockPos pos, BlockState state, Operation<Boolean> original) {
        return CraftEventFactory.handleBlockFormEvent(level, pos, state, 3, null, (p, s, f) -> original.call(level, p, s));
    }
}
