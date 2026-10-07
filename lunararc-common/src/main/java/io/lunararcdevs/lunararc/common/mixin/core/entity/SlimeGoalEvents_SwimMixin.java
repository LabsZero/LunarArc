package io.lunararcdevs.lunararc.common.mixin.core.entity;

import io.lunararcdevs.lunararc.common.event.LunarArcMoreEvents;
import net.minecraft.world.entity.monster.Slime;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.world.entity.monster.Slime$SlimeFloatGoal", remap = false)
public abstract class SlimeGoalEvents_SwimMixin {
    @Shadow(remap = true) @Final private Slime slime;

    @Inject(method = "canUse", remap = true, at = @At("RETURN"), cancellable = true)
    private void lunararc$swim(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && !LunarArcMoreEvents.slimeSwim(this.slime)) cir.setReturnValue(false);
    }
}
