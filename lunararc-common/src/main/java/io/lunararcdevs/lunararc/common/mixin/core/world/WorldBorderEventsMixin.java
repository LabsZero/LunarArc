package io.lunararcdevs.lunararc.common.mixin.core.world;

import io.lunararcdevs.lunararc.common.event.LunarArcPaperEvents;
import net.minecraft.world.level.border.WorldBorder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldBorder.class)
public abstract class WorldBorderEventsMixin {

    @Inject(method = "setSize", at = @At("HEAD"), cancellable = true, require = 0)
    private void lunararc$size(double size, CallbackInfo ci) {
        WorldBorder self = (WorldBorder) (Object) this;
        double[] before = {size};
        LunarArcPaperEvents.borderBounds(self, size, 0L, false, change -> {
            if (change[3] == 1) self.lerpSizeBetween(change[0], change[1], (long) change[2]);
            else if (change[1] != size) self.setSize(change[1]);
            else before[0] = Double.NaN;
        });
        if (!Double.isNaN(before[0])) ci.cancel();
    }

    @Inject(method = "lerpSizeBetween", at = @At("HEAD"), cancellable = true, require = 0)
    private void lunararc$lerp(double from, double to, long time, CallbackInfo ci) {
        WorldBorder self = (WorldBorder) (Object) this;
        boolean[] unchanged = {false};
        LunarArcPaperEvents.borderBounds(self, to, time, true, change -> {
            if (change[0] == from && change[1] == to && (long) change[2] == time) unchanged[0] = true;
            else self.lerpSizeBetween(change[0], change[1], (long) change[2]);
        });
        if (!unchanged[0]) ci.cancel();
    }

    @Inject(method = "setCenter", at = @At("HEAD"), cancellable = true, require = 0)
    private void lunararc$center(double x, double z, CallbackInfo ci) {
        double[] center = LunarArcPaperEvents.borderCenter((WorldBorder) (Object) this, x, z);
        if (center == null) {
            ci.cancel();
        } else if (center[0] != x || center[1] != z) {
            ci.cancel();
            LunarArcPaperEvents.withBorderReentry(() -> ((WorldBorder) (Object) this).setCenter(center[0], center[1]));
        }
    }
}
