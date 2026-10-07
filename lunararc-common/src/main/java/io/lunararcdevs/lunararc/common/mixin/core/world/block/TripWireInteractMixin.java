package io.lunararcdevs.lunararc.common.mixin.core.world.block;

import io.lunararcdevs.lunararc.common.event.LunarArcPaperEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TripWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TripWireBlock.class)
public abstract class TripWireInteractMixin {

    @Inject(method = "entityInside", at = @At("HEAD"), cancellable = true, require = 0)
    private void lunararc$interact(BlockState state, Level level, BlockPos pos, Entity entity, CallbackInfo ci) {
        if (LunarArcPaperEvents.entityInteract(entity, level, pos)) ci.cancel();
    }
}
