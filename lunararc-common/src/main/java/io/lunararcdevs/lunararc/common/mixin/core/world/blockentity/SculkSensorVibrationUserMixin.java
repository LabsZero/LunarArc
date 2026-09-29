package io.lunararcdevs.lunararc.common.mixin.core.world.blockentity;

import net.minecraft.world.level.block.entity.SculkSensorBlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.world.level.block.entity.SculkSensorBlockEntity$VibrationUser", remap = false)
public abstract class SculkSensorVibrationUserMixin {
    @Shadow(aliases = {"this$0", "field_44618"}, remap = true) @Final SculkSensorBlockEntity outer;

    @Inject(method = "getListenerRadius", at = @At("HEAD"), cancellable = true, remap = true)
    private void lunararc$rangeOverride(CallbackInfoReturnable<Integer> cir) {
        Integer override = ((io.lunararcdevs.lunararc.common.bridge.world.SculkRangeBridge) this.outer).lunararc$rangeOverride();
        if (override != null) cir.setReturnValue(override);
    }
}
