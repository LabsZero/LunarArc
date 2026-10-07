package io.lunararcdevs.lunararc.common.mixin.core.entity;

import io.lunararcdevs.lunararc.common.event.LunarArcMoreEvents;
import net.minecraft.world.entity.monster.Slime;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.world.entity.monster.Slime$SlimeAttackGoal", remap = false)
public abstract class SlimeGoalEvents_TargetMixin {
    @Shadow(remap = true) @Final private Slime slime;

    @Inject(method = "canUse", remap = true, at = @At("RETURN"), cancellable = true)
    private void lunararc$target(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && !LunarArcMoreEvents.slimeTarget(this.slime, this.slime.getTarget())) cir.setReturnValue(false);
    }

    @Inject(method = "canContinueToUse", remap = true, at = @At("RETURN"), cancellable = true)
    private void lunararc$keep(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && !LunarArcMoreEvents.slimeTarget(this.slime, this.slime.getTarget())) cir.setReturnValue(false);
    }
}
