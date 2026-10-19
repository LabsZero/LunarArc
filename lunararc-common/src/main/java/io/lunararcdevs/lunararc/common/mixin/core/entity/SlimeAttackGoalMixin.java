package io.lunararcdevs.lunararc.common.mixin.core.entity;
import io.lunararcdevs.lunararc.common.bridge.entity.SlimeBridge;
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(targets = "net.minecraft.world.entity.monster.cubemob.AbstractCubeMob$CubeMobAttackGoal", remap = false)
public abstract class SlimeAttackGoalMixin {
    @Shadow(remap = true) @Final private AbstractCubeMob cubeMob;
    @Inject(method="canUse", remap=true, at=@At("HEAD"), cancellable=true)
    private void lunararc$paperCanWander(CallbackInfoReturnable<Boolean> cir) {
        if (this.cubeMob instanceof SlimeBridge bridge && !bridge.lunararc$canWander()) cir.setReturnValue(false);
    }
    @Inject(method="canContinueToUse", remap=true, at=@At("HEAD"), cancellable=true)
    private void lunararc$paperCanContinueWandering(CallbackInfoReturnable<Boolean> cir) {
        if (this.cubeMob instanceof SlimeBridge bridge && !bridge.lunararc$canWander()) cir.setReturnValue(false);
    }
}
