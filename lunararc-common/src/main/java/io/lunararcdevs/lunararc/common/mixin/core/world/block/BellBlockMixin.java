package io.lunararcdevs.lunararc.common.mixin.core.world.block;

import io.lunararcdevs.lunararc.common.event.LunarArcPaperEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BellBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BellBlock.class)
public abstract class BellBlockMixin {

    @Inject(method = "attemptToRing(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)Z",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void lunararc$ring(Entity entity, Level level, BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        Direction facing = direction != null ? direction : level.getBlockState(pos).getValue(BellBlock.FACING);
        if (level.getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.BellBlockEntity
                && !LunarArcPaperEvents.bellRing(level, pos, facing, entity)) {
            cir.setReturnValue(false);
        }
    }
}
