package io.lunararcdevs.lunararc.common.mixin.core.world.block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ConcretePowderBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ConcretePowderBlock.class)
public abstract class ConcretePowderBlockMixin {
    @WrapOperation(method = "onLand", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private boolean lunararc$form(Level level, BlockPos pos, BlockState state, int flags, Operation<Boolean> original) {
        return CraftEventFactory.handleBlockFormEvent(level, pos, state, flags, null, (p, s, f) -> original.call(level, p, s, f));
    }
}
