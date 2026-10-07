package io.lunararcdevs.lunararc.common.mixin.core.entity;

import io.lunararcdevs.lunararc.common.event.LunarArcMoreEvents;
import net.minecraft.world.entity.animal.Turtle;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.world.entity.animal.Turtle$TurtleGoHomeGoal", remap = false)
public abstract class MoreMobEvents_TurtleGoHomeMixin {
    @Shadow(remap = true) @Final private Turtle turtle;

    @Inject(method = "canUse", remap = true, at = @At("RETURN"), cancellable = true)
    private void lunararc$home(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && !LunarArcMoreEvents.turtleGoHome(this.turtle)) cir.setReturnValue(false);
    }
}
